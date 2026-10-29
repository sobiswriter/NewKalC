package com.example.data.kitchen

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.util.AtomicFile
import com.example.data.model.MealAnalysisResult
import com.example.data.model.NutritionInsights
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.UUID

enum class HistoryKind { SCANNED, COOKED, RECIPE }
data class HistoryRecord(
    val id: String = UUID.randomUUID().toString(), val kind: HistoryKind, val title: String,
    val createdAt: Long = System.currentTimeMillis(), val meal: MealAnalysisResult? = null,
    val originalMeal: MealAnalysisResult? = null, val insights: NutritionInsights? = null,
    val personalizedInsights: Boolean = false, val recipe: CookbookRecipe? = null,
    val servings: Int = 1, val note: String = "", val review: CookingReview? = null,
    val linkedCookedId: String? = null, val imagePath: String? = null
)
data class HistoryRow(val id: String, val kind: HistoryKind, val title: String, val createdAt: Long, val linkedCookedId: String?)

class HistoryRepository private constructor(context: Context) {
    private val folder = File(context.noBackupFilesDir, "kitchen").apply { check(exists() || mkdirs()) }
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val recordAdapter = moshi.adapter(HistoryRecord::class.java)
    private val sessionAdapter = moshi.adapter(CookingSession::class.java)
    private val sessionFile = AtomicFile(File(folder, "cooking-session.json"))
    private val helper = object : SQLiteOpenHelper(context, File(folder, "history.db").absolutePath, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE history(id TEXT PRIMARY KEY, kind TEXT NOT NULL, title TEXT NOT NULL, created INTEGER NOT NULL, linked TEXT, payload TEXT NOT NULL)")
            db.execSQL("CREATE INDEX history_date ON history(created DESC)")
            db.execSQL("CREATE INDEX history_kind ON history(kind, created DESC)")
            db.execSQL("CREATE INDEX history_link ON history(linked)")
            db.execSQL("CREATE TABLE settings(name TEXT PRIMARY KEY, value TEXT NOT NULL)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
    private val changed = MutableStateFlow(0L)
    val changes = changed.asStateFlow()
    val enabled: Boolean get() = helper.readableDatabase.rawQuery("SELECT value FROM settings WHERE name='enabled'", null).use { !it.moveToFirst() || it.getString(0) != "false" }
    fun setEnabled(value: Boolean) {
        helper.writableDatabase.insertWithOnConflict("settings", null, ContentValues().apply { put("name", "enabled"); put("value", value.toString()) }, SQLiteDatabase.CONFLICT_REPLACE)
        changed.value++
    }
    fun rows(kind: HistoryKind? = null, offset: Int = 0, limit: Int = 30): List<HistoryRow> {
        require(offset >= 0 && limit in 1..100)
        return helper.readableDatabase.query("history", arrayOf("id", "kind", "title", "created", "linked"),
            if(kind == null) "linked IS NULL" else "kind=?", kind?.let { arrayOf(it.name) }, null, null, "created DESC", "$offset,$limit").use { c ->
            buildList { while(c.moveToNext()) add(HistoryRow(c.getString(0), HistoryKind.valueOf(c.getString(1)), c.getString(2), c.getLong(3), c.getString(4))) }
        }
    }
    fun get(id: String): HistoryRecord? = helper.readableDatabase.rawQuery("SELECT payload FROM history WHERE id=?", arrayOf(id)).use {
        if(it.moveToFirst()) requireNotNull(recordAdapter.fromJson(it.getString(0))) else null
    }
    fun linked(id: String): List<HistoryRecord> = helper.readableDatabase.rawQuery("SELECT payload FROM history WHERE linked=? ORDER BY created DESC", arrayOf(id)).use {
        buildList { while(it.moveToNext()) add(requireNotNull(recordAdapter.fromJson(it.getString(0)))) }
    }
    @Synchronized fun save(record: HistoryRecord, image: Bitmap? = null): HistoryRecord {
        require(record.title.length in 1..200 && record.note.length <= 2000)
        record.recipe?.validated()
        val old = get(record.id)
        var newPhoto: File? = null
        try {
            val photo = if(image != null) {
                newPhoto = File(folder, "${UUID.randomUUID()}.jpg")
                val ratio = minOf(1.0, 1000.0 / maxOf(image.width, image.height))
                val scaled = Bitmap.createScaledBitmap(image, maxOf(1,(image.width*ratio).toInt()), maxOf(1,(image.height*ratio).toInt()), true)
                try { newPhoto!!.outputStream().use { check(scaled.compress(Bitmap.CompressFormat.JPEG, 80, it)) } }
                finally { if(scaled !== image) scaled.recycle() }
                newPhoto!!.name
            } else record.imagePath ?: old?.imagePath
            val saved = record.copy(imagePath = photo)
            val values = ContentValues().apply {
                put("id", saved.id); put("kind", saved.kind.name); put("title", saved.title); put("created", saved.createdAt)
                put("linked", saved.linkedCookedId); put("payload", recordAdapter.toJson(saved))
            }
            check(helper.writableDatabase.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1L) { "History could not be saved. Free device storage and retry." }
            if(photo != old?.imagePath) old?.imagePath?.let { safePhoto(it)?.delete() }
            changed.value++
            return saved
        } catch(e: Exception) { newPhoto?.delete(); throw e }
    }
    fun photo(record: HistoryRecord): File? = record.imagePath?.let(::safePhoto)?.takeIf { it.exists() }
    private fun safePhoto(name: String): File? = if(Regex("[a-f0-9-]+\\.jpg").matches(name)) File(folder, name) else null
    @Synchronized fun removePhoto(id: String) {
        val record = get(id) ?: return
        val old = record.imagePath
        save(record.copy(imagePath = null))
        // save preserves an existing photo on metadata updates; clear it deliberately here.
        helper.writableDatabase.execSQL("UPDATE history SET payload=? WHERE id=?", arrayOf(recordAdapter.toJson(record.copy(imagePath = null)), id))
        old?.let { safePhoto(it)?.delete() }
        changed.value++
    }
    @Synchronized fun delete(id: String) {
        val record = get(id) ?: return
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            linked(id).forEach { scan ->
                val detached = scan.copy(linkedCookedId = null)
                db.execSQL("UPDATE history SET linked=NULL,payload=? WHERE id=?", arrayOf(recordAdapter.toJson(detached), scan.id))
            }
            check(db.delete("history", "id=?", arrayOf(id)) == 1)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
        record.imagePath?.let { safePhoto(it)?.delete() }; changed.value++
    }
    @Synchronized fun clear() {
        helper.writableDatabase.delete("history", null, null)
        folder.listFiles()?.filter { it.extension == "jpg" }?.forEach { it.delete() }
        changed.value++
    }
    @Synchronized fun saveSession(session: CookingSession) {
        val stream = sessionFile.startWrite()
        try { stream.write(sessionAdapter.toJson(session).toByteArray()); sessionFile.finishWrite(stream) }
        catch(e: Exception) { sessionFile.failWrite(stream); throw e }
    }
    @Synchronized fun session(): CookingSession? = if(!sessionFile.baseFile.exists()) null else
        sessionFile.openRead().bufferedReader().use { sessionAdapter.fromJson(it.readText())?.also { s -> s.recipe.validated() } }
    fun storageBytes(): Long = folder.listFiles()?.sumOf { it.length() } ?: 0
    companion object {
        @Volatile private var instance: HistoryRepository? = null
        fun get(context: Context): HistoryRepository = instance ?: synchronized(this) {
            instance ?: HistoryRepository(context.applicationContext).also { instance = it }
        }
    }
}
