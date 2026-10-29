package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import com.example.data.gemini.GeminiFoodService
import com.example.data.kitchen.*
import com.example.data.profile.UserProfile
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class KitchenUiState(
    val page: String = "home", val pantry: List<KitchenIngredient> = emptyList(),
    val confirmed: Boolean = false, val photos: List<Bitmap> = emptyList(),
    val brief: MealBrief = MealBrief(), val recipes: List<RecipeSummary> = emptyList(),
    val selected: CookbookRecipe? = null, val proposed: CookbookRecipe? = null,
    val session: CookingSession? = null, val group: RecipeGroup = RecipeGroup.NOW,
    val busy: String? = null, val error: String? = null, val notice: String? = null,
    val messages: List<Pair<Boolean,String>> = emptyList(),
    val historyRows: List<HistoryRow> = emptyList(), val historyKind: HistoryKind? = null,
    val historyRecord: HistoryRecord? = null, val linkedScans: List<HistoryRecord> = emptyList(),
    val historyEnabled: Boolean = true, val storageBytes: Long = 0, val hasMoreHistory: Boolean = false
)

class KitchenController(
    private val context: Context, private val scope: CoroutineScope, service: GeminiFoodService,
    private val apiKey: () -> String, private val profile: () -> UserProfile,
    private val accessError: (Throwable) -> Unit
) {
    val history = HistoryRepository.get(context)
    private val ai by lazy { KitchenAiService(service) }
    private val mutable = MutableStateFlow(KitchenUiState())
    val state = mutable.asStateFlow()
    private var job: Job? = null
    private var revision = 0L
    private val cookbookCache = mutableMapOf<String,CookbookRecipe>()
    init {
        scope.launch {
            try {
                val session=withContext(Dispatchers.IO) { history.session() }
                mutable.update { it.copy(session=session) }
            } catch(e: Exception) { mutable.update { it.copy(error="Saved cooking progress could not be read. Your history is still available.") } }
        }
        scope.launch { history.changes.collect { refreshHistory() } }
    }
    private fun cancel() { revision++; job?.cancel(); mutable.update { it.copy(busy=null) } }
    private fun task(label: String, work: suspend () -> Unit) {
        cancel(); val version=revision
        mutable.update { it.copy(busy=label,error=null,notice=null) }
        job=scope.launch {
            try { work() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { if(version==revision) { accessError(e); mutable.update { it.copy(error=e.message?.take(350)?:"Something went wrong. Please retry.") } } }
            finally { if(version==revision) mutable.update { it.copy(busy=null) } }
        }
    }
    private suspend fun <T> io(work: suspend () -> T): T = withContext(Dispatchers.IO) { work() }
    fun go(page: String) { cancel(); mutable.update { it.copy(page=page,error=null,notice=null) }; if(page=="history" || page=="home") refreshHistory() }
    fun dismiss() { mutable.update { it.copy(error=null,notice=null) } }
    fun setGroup(group: RecipeGroup) { mutable.update { it.copy(group=group) } }
    fun addPhoto(bitmap: Bitmap) { cancel(); mutable.update { it.copy(photos=(it.photos+bitmap).take(3),confirmed=false,recipes=emptyList(),error=null) } }
    fun removePhoto(index: Int) { cancel(); mutable.update { it.copy(photos=it.photos.filterIndexed { n,_ -> n!=index }) } }
    fun recognize() {
        val images=state.value.photos
        if(images.isEmpty()) { mutable.update { it.copy(error="Add an ingredient photo first, or enter ingredients by hand.") }; return }
        task("Looking at your ingredients…") {
            val ingredients=io { ai.recognize(images,apiKey()) }
            currentCoroutineContext().ensureActive()
            mutable.update { it.copy(pantry=ingredients,confirmed=false,recipes=emptyList(),page="ingredients") }
        }
    }
    fun editIngredient(index: Int, ingredient: KitchenIngredient?) {
        cancel(); mutable.update { state -> state.copy(pantry=state.pantry.toMutableList().apply {
            if(index in indices) { if(ingredient==null) removeAt(index) else set(index,ingredient.validated()) }
            else if(ingredient!=null && size<35) add(ingredient.validated())
        },confirmed=false,recipes=emptyList()) }
    }
    fun confirmPantry() {
        if(state.value.pantry.isEmpty()) { mutable.update { it.copy(error="Add at least one ingredient.") }; return }
        mutable.update { it.copy(confirmed=true,page="pantry",error=null) }
    }
    fun staple(ingredient: KitchenIngredient, checked: Boolean) {
        cancel(); mutable.update { it.copy(pantry=it.pantry.filterNot { old -> old.key==ingredient.key } + if(checked)listOf(ingredient) else emptyList(),recipes=emptyList()) }
    }
    fun updateBrief(brief: MealBrief) { cancel(); mutable.update { it.copy(brief=brief.validated(),recipes=emptyList(),messages=emptyList(),proposed=null) } }
    fun invalidateProfile() {
        cancel(); mutable.update { it.copy(brief=it.brief.copy(profileConsent=false),recipes=emptyList(),messages=emptyList(),proposed=null,notice="Profile or provider changed. Review your cooking preferences before new suggestions.") }
    }
    fun matches(): List<RecipeMatch> = state.value.let { s -> s.recipes.map { RecipeMatcher.match(it,s.pantry,s.brief) }.filter { it.blocked.isEmpty() && (it.group==RecipeGroup.EFFORT || it.withinTime) } }
    fun suggest(more: Boolean = false) {
        val s=state.value
        if(!s.confirmed) { mutable.update { it.copy(page="ingredients",error="Confirm your pantry first.") }; return }
        task("Finding possibilities for your kitchen…") {
            val suggestions=io { ai.suggest(s.pantry,s.brief,profile(),apiKey(),if(more)s.recipes.map { it.title } else emptyList()) }
            currentCoroutineContext().ensureActive()
            mutable.update { it.copy(recipes=(if(more)it.recipes else emptyList())+suggestions,page="results") }
        }
    }
    fun grab(healthy: Boolean) {
        val pick=if(healthy)RecipeMatcher.healthy(matches()) else RecipeMatcher.quick(matches())
        if(pick==null) { mutable.update { it.copy(error="No ready recipe fits these quantities, time and restrictions. Review your pantry or brief.") }; return }
        openRecipe(pick.recipe)
    }
    fun openRecipe(summary: RecipeSummary) {
        cookbookCache[summary.id]?.let { recipe -> mutable.update { it.copy(selected=recipe,proposed=null,messages=emptyList(),page="detail") }; return }
        task("Opening your cookbook…") {
            val recipe=io { ai.detail(summary,apiKey()) }
            currentCoroutineContext().ensureActive()
            cookbookCache[summary.id]=recipe
            mutable.update { it.copy(selected=recipe,proposed=null,messages=emptyList(),page="detail") }
        }
    }
    fun saveRecipe() {
        val s=state.value; val recipe=s.selected?:return
        task("Saving this recipe…") {
            io { history.save(HistoryRecord(id="recipe-${recipe.version}",kind=HistoryKind.RECIPE,title=recipe.summary.title,recipe=recipe,servings=s.brief.servings)) }
            mutable.update { it.copy(notice="Recipe saved on this device. Available offline in History → Recipes.") }
        }
    }
    fun startCooking() {
        val s=state.value; val recipe=s.selected?:return
        val match=RecipeMatcher.match(recipe.summary,s.pantry,s.brief)
        if(!s.confirmed) { mutable.update { it.copy(error="Confirm today's ingredient amounts before starting.",page="ingredients") }; return }
        if(match.blocked.isNotEmpty()) { mutable.update { it.copy(error=match.blocked.joinToString()) }; return }
        if(match.missing.isNotEmpty()) { mutable.update { it.copy(error="Confirm the missing ingredients before starting. You can read the method meanwhile.") }; return }
        val session=CookingSession(recipe=recipe,servings=s.brief.servings,brief=s.brief.copy(profileConsent=false))
        task("Saving your cooking session…") {
            io { history.saveSession(session) }
            mutable.update { it.copy(session=session,page="cook") }
        }
    }
    fun resume() { state.value.session?.let { session -> mutable.update { s -> s.copy(selected=session.recipe,brief=(session.brief?:s.brief).copy(servings=session.servings,profileConsent=false),page=if(session.confirmedCooked)"finish" else "cook") } } }
    fun step(delta: Int) {
        val session=state.value.session?:return
        val next=(session.step+delta).coerceIn(0,session.recipe.steps.lastIndex)
        val updated=session.copy(step=next)
        task("Saving progress…") { io { history.saveSession(updated) }; mutable.update { it.copy(session=updated) } }
    }
    fun timer(minutes: Int) {
        val session=state.value.session?:return
        val end=if(minutes==0)null else System.currentTimeMillis()+minutes*60000L
        val updated=session.copy(timerEndsAt=end)
        task("Saving timer…") {
            io { history.saveSession(updated); CookingTimer.schedule(context,end,session.recipe.summary.title) }
            mutable.update { it.copy(session=updated) }
        }
    }
    fun confirmCooked(note: String) {
        val session=state.value.session?:return
        task("Saving your cooked meal…") {
            val updated=session.copy(confirmedCooked=true,note=note.take(2000))
            io {
                if(history.enabled) {
                    history.save(HistoryRecord(id=session.id,kind=HistoryKind.COOKED,title=session.recipe.summary.title,recipe=session.recipe,servings=session.servings,note=updated.note))
                    history.save(HistoryRecord(id="recipe-${session.recipe.version}",kind=HistoryKind.RECIPE,title=session.recipe.summary.title,recipe=session.recipe,servings=session.servings))
                }
                history.saveSession(updated); CookingTimer.schedule(context,null,"")
            }
            mutable.update { it.copy(session=updated,page="finish",notice=if(history.enabled)"Cooked meal and recipe saved locally." else "Cooking confirmed. Automatic history is off; use Save cooked meal to keep it in the library.") }
        }
    }
    fun saveCookedExplicitly() {
        val session=state.value.session?.takeIf { it.confirmedCooked }?:return
        task("Saving cooked meal…") { io {
            history.save(HistoryRecord(id=session.id,kind=HistoryKind.COOKED,title=session.recipe.summary.title,recipe=session.recipe,servings=session.servings,note=session.note))
            history.save(HistoryRecord(id="recipe-${session.recipe.version}",kind=HistoryKind.RECIPE,title=session.recipe.summary.title,recipe=session.recipe,servings=session.servings))
        }; mutable.update { it.copy(notice="Cooked meal and recipe saved.") } }
    }
    fun ask(question: String) {
        val s=state.value; val recipe=s.selected?:return
        task("Thinking through your cooking question…") {
            val step=s.session?.takeIf {it.recipe.version==recipe.version}?.step?:0
            val answer=io { ai.chat(recipe,s.brief,step,question,profile(),apiKey()) }
            currentCoroutineContext().ensureActive()
            mutable.update { it.copy(messages=(it.messages+listOf(true to question,false to answer)).takeLast(24)) }
        }
    }
    fun propose(request: String) {
        val recipe=state.value.selected?:return
        task("Checking a revised version…") {
            val proposal=io { ai.revise(recipe,request,apiKey(),state.value.brief,profile()) }
            currentCoroutineContext().ensureActive()
            require(RecipeMatcher.match(proposal.summary,state.value.pantry,state.value.brief).blocked.isEmpty()) { "This proposal conflicts with your food restrictions or equipment. Try a different change." }
            mutable.update { it.copy(proposed=proposal) }
        }
    }
    fun acceptProposal() { val proposed=state.value.proposed?:return; mutable.update { it.copy(selected=proposed,proposed=null,session=null,messages=emptyList(),page="detail",notice="New recipe version selected. Review quantities and missing items before cooking.") } }
    fun declineProposal() { mutable.update { it.copy(proposed=null) } }
    fun refreshHistory(kind: HistoryKind? = state.value.historyKind, more: Boolean = false) {
        scope.launch {
            try {
                val offset=if(more)state.value.historyRows.size else 0
                val (rows,enabled,size)=io { Triple(history.rows(kind,offset),history.enabled,history.storageBytes()) }
                mutable.update { it.copy(historyRows=(if(more)it.historyRows else emptyList())+rows,historyKind=kind,hasMoreHistory=rows.size==30,historyEnabled=enabled,storageBytes=size) }
            } catch(e: Exception) { mutable.update { it.copy(error="Could not read local history. Please retry.") } }
        }
    }
    fun openHistory(id: String) {
        task("Opening saved information…") {
            val record=io { history.get(id) }?:error("This entry was deleted.")
            val linked=io { history.linked(id) }
            mutable.update { it.copy(historyRecord=record,linkedScans=linked,page="historyDetail") }
        }
    }
    fun openSavedRecipe() { val record=state.value.historyRecord?:return; record.recipe?.let { recipe -> mutable.update { it.copy(selected=recipe,brief=it.brief.copy(servings=record.servings),page="detail",messages=emptyList()) } } }
    fun saveHistoryNote(note: String) {
        val record=state.value.historyRecord?:return
        task("Saving your notes…") { val updated=io {history.save(record.copy(note=note.take(2000)))};mutable.update {it.copy(historyRecord=updated,notice="Notes saved on this device.")} }
    }
    fun setHistoryEnabled(enabled: Boolean) { task("Saving history choice…") { io { history.setEnabled(enabled) } } }
    fun deleteHistory(id: String?, photoOnly: Boolean = false) {
        task("Updating local history…") {
            io { if(id==null)history.clear() else if(photoOnly)history.removePhoto(id) else history.delete(id) }
            if(photoOnly && id!=null) { val record=io { history.get(id) }; mutable.update { it.copy(historyRecord=record) } }
            else mutable.update { it.copy(page="history",historyRecord=null) }
            refreshHistory()
        }
    }
    fun review(scan: HistoryRecord) {
        mutable.update {it.copy(historyRecord=scan,linkedScans=emptyList(),page="historyDetail")}
        task("Reviewing this cooking result…") {
            val cooked=io { scan.linkedCookedId?.let { history.get(it) } }?:error("Save and link a cooked meal first.")
            val review=io { ai.review(cooked,scan,apiKey()) }
            currentCoroutineContext().ensureActive()
            val updated=io { history.save(scan.copy(review=review)) }
            mutable.update { it.copy(historyRecord=updated,page="historyDetail",notice="Cooking review saved. Photo observations are estimates.") }
        }
    }
}
