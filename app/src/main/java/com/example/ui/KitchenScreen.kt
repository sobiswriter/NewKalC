package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.R
import com.example.data.kitchen.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KitchenScreen(viewModel: FoodCalorieViewModel) {
    val c=viewModel.kitchen
    val s by c.state.collectAsState()
    val listState=remember(s.page) { LazyListState() }
    val app by viewModel.uiState.collectAsState()
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var captureCookedId by rememberSaveable { mutableStateOf<String?>(null) }
    var plateMode by rememberSaveable { mutableStateOf(false) }
    var mediaError by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var deletion by remember { mutableStateOf<String?>(null) }
    var viewingPhoto by remember { mutableStateOf<Bitmap?>(null) }
    val handleImage: (Uri)->Unit = { uri ->
        val asPlate=plateMode; val cookedTarget=captureCookedId
        scope.launch {
        try {
            val bitmap=withContext(Dispatchers.IO) { KitchenImages.decode(context,uri) }
            if(asPlate)viewModel.scanCookedPlate(bitmap,cookedTarget) else c.addPhoto(bitmap)
        } catch(e: Exception) { mediaError=e.message?:"Could not open the image." }
        finally { if(uri.toString()==cameraUri) { cameraUri?.let { saved -> runCatching { context.contentResolver.delete(Uri.parse(saved),null,null) } }; cameraUri=null } }
    } }
    val camera=rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success -> if(success)cameraUri?.let { handleImage(Uri.parse(it)) } }
    val gallery=rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris -> uris.take(if(plateMode)1 else 3).forEach(handleImage) }
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(!granted)mediaError="Notifications are off. The saved timer remains visible in cooking mode; background reminders will not appear." }
    val launchCamera: ()->Unit = {
        try {
            val directory=File(context.cacheDir,"kitchen-camera").apply { mkdirs() }
            val file=File.createTempFile("capture-",".jpg",directory)
            val uri=FileProvider.getUriForFile(context,"${context.packageName}.kitchen.files",file)
            cameraUri=uri.toString(); camera.launch(uri)
        } catch(e: Exception) { mediaError="Camera unavailable. You can choose a photo from your gallery." }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted)launchCamera() else mediaError="Camera permission was denied. Use gallery or manual entry." }
    val takePhoto: (Boolean,String?)->Unit = { plate,id -> plateMode=plate;captureCookedId=id
        if(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)launchCamera() else permission.launch(Manifest.permission.CAMERA)
    }
    val pickPhotos: (Boolean,String?)->Unit = { plate,id -> plateMode=plate;captureCookedId=id;gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val back: ()->Unit = { c.go(when(s.page) { "ingredients"->"home";"pantry"->"ingredients";"brief"->"pantry";"results"->"brief";"detail"->"results";"prepare","cook","chat","finish"->"detail";"historyDetail"->"history";else->"home" }) }
    BackHandler { if(s.page=="home")viewModel.openKalc() else back() }
    Scaffold(topBar={ TopAppBar(title={ Text(if(s.page.startsWith("history"))"Your kitchen memory" else "Make a meal",style=MaterialTheme.typography.titleLarge) },
        navigationIcon={ IconButton(onClick={ if(s.page=="home")viewModel.openKalc() else back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack,"Back") } },
        actions={ IconButton(onClick={ c.go("history") }) { Icon(Icons.Default.History,"History") }; IconButton(onClick={ viewModel.openApiKeyDialog() }) { Icon(Icons.Default.Key,"AI settings") } }) },
        bottomBar={ NavigationBar {
            NavigationBarItem(selected=false,onClick=viewModel::openKalc,icon={Icon(Icons.Default.Restaurant,"KalC")},label={Text("KalC")})
            NavigationBarItem(selected=true,onClick={c.go("home")},icon={Icon(Icons.Default.SoupKitchen,"Make a meal")},label={Text("Make a meal")})
            NavigationBarItem(selected=false,onClick=viewModel::openProfile,icon={Icon(Icons.Default.PersonOutline,"You")},label={Text("You")})
        } }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("kitchen_content"),state=listState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item { Text(if(app.aiProvider==com.example.data.auth.AiProvider.VERTEX) { if(app.isVertexUnlocked)"VERTEX AI · CONNECTED" else "VERTEX AI · LOCKED" } else "GOOGLE AI STUDIO",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            if(s.busy!=null)item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(s.busy!!,Modifier.semantics { liveRegion=LiveRegionMode.Polite }) } }
            if(s.error!=null || mediaError!=null || s.notice!=null)item { Surface(color=if(s.error!=null || mediaError!=null)MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(s.error?:mediaError?:s.notice!!,Modifier.semantics { liveRegion=LiveRegionMode.Polite }); TextButton(onClick={ c.dismiss();mediaError=null }) { Text("Dismiss") } }
            } }
            when(s.page) {
                "home" -> {
                    item { KitchenTitle("Good things\nstart here.","From what you have to something you love. Photograph your ingredients, then make the final call on what is there.") }
                    item { EditorialImage("Your kitchen",0); Caption("Ingredient illustration · your photos stay separate") }
                    item { Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) { Button(onClick={takePhoto(false,null)},Modifier.weight(1f).testTag("ingredient_camera"),enabled=s.busy==null) { Text("Take photo") }; OutlinedButton(onClick={pickPhotos(false,null)},Modifier.weight(1f),enabled=s.photos.size<3 && s.busy==null) { Text("Add photos") } } }
                    items(s.photos.size) { n -> Row(verticalAlignment=Alignment.CenterVertically) { Image(s.photos[n].asImageBitmap(),"Ingredient photo ${n+1}",Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)).clickable {viewingPhoto=s.photos[n]},contentScale=ContentScale.Crop); Text("Photo ${n+1} of 3",Modifier.weight(1f).padding(12.dp)); TextButton(onClick={c.removePhoto(n)}) {Text("Remove")} } }
                    item { Button(onClick=c::recognize,enabled=s.photos.isNotEmpty() && s.busy==null,modifier=Modifier.fillMaxWidth().testTag("recognize_ingredients")) { Text("Find my ingredients") }
                        TextButton(onClick={c.go("ingredients")},modifier=Modifier.fillMaxWidth()) { Text("Add ingredients by hand") } }
                    if(s.session!=null)item { OutlinedButton(onClick=c::resume,Modifier.fillMaxWidth()) { Text("Resume ${s.session!!.recipe.summary.title}") } }
                    item { Text("Recent kitchen memories",style=MaterialTheme.typography.titleLarge) }
                    items(s.historyRows.take(3),key={"recent-${it.id}"}) { row -> HistoryRowCard(row) {c.openHistory(row.id)} }
                    item { TextButton(onClick={c.go("history")}) {Text("See all history & saved recipes")} }
                }
                "ingredients" -> {
                    item { KitchenTitle("Here is what\nwe have.","Correct names, amounts and preparation state. Unknown quantities are allowed, but cannot qualify a recipe as ready. Merge duplicate views yourself before confirming.") }
                    items(s.pantry.size) { n -> val i=s.pantry[n]; Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) { Text(i.name,style=MaterialTheme.typography.titleMedium); Text("${amount(i)} · ${i.state}${if(i.useSoon)" · Use soon" else ""}"); Row { TextButton(onClick={editing=n}) { Text("Edit") }; TextButton(onClick={c.editIngredient(n,null)}) { Text("Remove") } } }
                    } }
                    item { OutlinedButton(onClick={editing=-1},Modifier.fillMaxWidth().testTag("add_ingredient")) {Text("Add ingredient")}; Button(onClick=c::confirmPantry,Modifier.fillMaxWidth().testTag("confirm_ingredients"),enabled=s.pantry.isNotEmpty()) {Text("Confirm ingredients")} }
                }
                "pantry" -> {
                    item { KitchenTitle("The little\nextras.","Confirm only what you actually have. No spices, oil or equipment are assumed.") }
                    items(listOf(KitchenIngredient("oil",15.0,"ml","ready"),KitchenIngredient("salt",1.0,"tsp","ready"),KitchenIngredient("cumin",1.0,"tsp","dry"),KitchenIngredient("ginger",10.0,"g","raw"),KitchenIngredient("coconut milk",200.0,"ml","ready"))) { ingredient ->
                        CheckRow("${ingredient.name} · ${amount(ingredient)}",s.pantry.any {it.key==ingredient.key}) {c.staple(ingredient,it)}
                    }
                    item { TextButton(onClick={c.go("ingredients")}) {Text("Edit amounts or add other staples")}; Text("Equipment available",style=MaterialTheme.typography.titleLarge) }
                    items(listOf("hob","oven","microwave","blender")) { tool -> CheckRow(tool.replaceFirstChar { it.uppercase() },tool in s.brief.equipment) { checked -> c.updateBrief(s.brief.copy(equipment=if(checked)s.brief.equipment+tool else s.brief.equipment-tool)) } }
                    item { Button(onClick={c.go("brief")},Modifier.fillMaxWidth()) {Text("Set the mood")}; if(s.selected!=null)OutlinedButton(onClick={c.go("detail")},Modifier.fillMaxWidth()) {Text("Review selected recipe")} }
                }
                "brief" -> {
                    item { KitchenTitle("Your meal,\nyour way.","A few choices, then something worth cooking. Today's restrictions apply whether saved-profile personalization is on or off.") }
                    item { Text("People eating: ${s.brief.servings}"); Row { OutlinedButton(onClick={c.updateBrief(s.brief.copy(servings=s.brief.servings-1))},enabled=s.brief.servings>1) {Text("−")}; Spacer(Modifier.width(12.dp)); OutlinedButton(onClick={c.updateBrief(s.brief.copy(servings=s.brief.servings+1))},enabled=s.brief.servings<12) {Text("+")} } }
                    item { ChoiceChips("Total time, including prep",listOf(10,15,30,45,90,120).map { "$it min" },"${s.brief.minutes} min") { c.updateBrief(s.brief.copy(minutes=it.substringBefore(" ").toInt())) } }
                    item { ChoiceChips("Cooking style",listOf("Surprise me","Indian-inspired","Mediterranean-inspired","East Asian-inspired","Latin American-inspired","Middle Eastern-inspired","European-inspired"),s.brief.style) { c.updateBrief(s.brief.copy(style=it)) } }
                    item { ChoiceChips("Heat preference",listOf("Mild","Medium","Hot"),s.brief.heat) { c.updateBrief(s.brief.copy(heat=it)) }; Text("Foods to avoid for everyone eating",style=MaterialTheme.typography.titleLarge) }
                    items(listOf("egg","dairy","peanut","nuts","gluten","fish","shellfish","soy","sesame","vegetarian","vegan")) { avoid -> CheckRow(avoid.replaceFirstChar {it.uppercase()},avoid in s.brief.avoid) { checked -> c.updateBrief(s.brief.copy(avoid=if(checked)s.brief.avoid+avoid else s.brief.avoid-avoid)) } }
                    item { var extra by remember { mutableStateOf("") }; OutlinedTextField(extra,{extra=it.take(80)},label={Text("Another food to avoid")},modifier=Modifier.fillMaxWidth()); TextButton(onClick={if(extra.isNotBlank() && s.brief.avoid.size<20) { c.updateBrief(s.brief.copy(avoid=(s.brief.avoid+extra.trim()).distinct()));extra="" }}) {Text("Add avoidance")}; s.brief.avoid.filterNot {it in listOf("egg","dairy","peanut","nuts","gluten","fish","shellfish","soy","sesame","vegetarian","vegan")}.forEach { item -> TextButton(onClick={c.updateBrief(s.brief.copy(avoid=s.brief.avoid-item))}) {Text("Remove: $item")} } }
                    item { if(app.profile.personalizationEnabled) { CheckRow("Use my enabled profile for this cooking session",s.brief.profileConsent) { c.updateBrief(s.brief.copy(profileConsent=it)) }; Caption("Shares relevant saved goals, preferences, age and health notes with your selected AI provider for recipe guidance. Name and body measurements stay out of cooking requests. Check explicit avoidances above.") }
                        else Caption("Personalization is off. Suggestions stay general; today's explicit avoidances still apply. Change the master setting in You.")
                        Button(onClick={c.suggest()},Modifier.fillMaxWidth().testTag("find_recipes"),enabled=s.busy==null) {Text("Find my recipes")} }
                }
                "results" -> {
                    item { KitchenTitle("Dinner,\ndecided.","${s.brief.servings} people · ${s.brief.minutes} minute everyday brief. Choose one meal; stock is not reserved for every suggestion at once.") }
                    item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { Button(onClick={c.grab(false)},enabled=s.busy==null,modifier=Modifier.weight(1f)) {Text("Quick Grab")}; OutlinedButton(onClick={c.grab(true)},enabled=s.busy==null,modifier=Modifier.weight(1f)) {Text("Healthy Grab")} }; Caption(if(s.brief.profileConsent && app.profile.personalizationEnabled)"Suggestions use your permitted cooking profile; this shortcut picks a ready option with a variety of plant ingredients." else "Healthy Grab is a general balanced ready pick, not the lowest calorie score.") }
                    item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { RecipeGroup.entries.forEach { group -> FilterChip(selected=s.group==group,onClick={c.setGroup(group)},label={Text("${group.label} (${c.matches().count {it.group==group}})")}) } }; Caption(if(s.group==RecipeGroup.EFFORT)"Longer projects may exceed your everyday time brief. Premium effort, no paid tier." else "Ready claims require enough confirmed ingredients, equipment and time.") }
                    val matches=c.matches().filter {it.group==s.group}
                    if(matches.isEmpty())item { Text("No suitable options in this group. Review quantities, restrictions or time; no ingredients will be invented.") }
                    items(matches,key={it.recipe.id}) { match -> RecipeCard(match.recipe,match.missing.takeIf{it.isNotEmpty()}?.joinToString {amount(it)+" "+it.name}?:"No extra ingredients required") {c.openRecipe(match.recipe)} }
                    item { OutlinedButton(onClick={c.suggest(true)},enabled=s.busy==null && s.recipes.size<18,modifier=Modifier.fillMaxWidth()) {Text("More ideas")}; TextButton(onClick={c.go("ingredients")}) {Text("Edit pantry")}; TextButton(onClick={c.go("brief")}) {Text("Change the brief")} }
                }
                "detail" -> s.selected?.let { recipe ->
                    val match=RecipeMatcher.match(recipe.summary,s.pantry,s.brief)
                    item { KitchenTitle(recipe.summary.title,recipe.summary.description); EditorialImage(recipe.summary.title); Caption("Representative illustration · the finished dish may differ") }
                    item { Text("${recipe.summary.totalMinutes} min total · ${recipe.summary.activeMinutes} min active · ${recipe.summary.effort}\n${recipe.summary.style} · ${s.brief.servings} portions",style=MaterialTheme.typography.bodyLarge); Text("Why this fits",style=MaterialTheme.typography.titleLarge); Text(recipe.summary.balancedReason.ifBlank {"Uses ${recipe.summary.ingredients.filter {ingredient->s.pantry.any {it.key==ingredient.key}}.take(4).joinToString {it.name}}"}) }
                    if(match.blocked.isNotEmpty())item {Text(match.blocked.joinToString(),color=MaterialTheme.colorScheme.error)}
                    if(match.missing.isNotEmpty())item { Text("Before you start",style=MaterialTheme.typography.titleLarge); match.missing.forEach { ingredient -> Text("• ${amount(ingredient)} ${ingredient.name} (${ingredient.state})") }; Caption("An exact shopping list, not available stock. Add what you obtained and confirm amounts before starting."); TextButton(onClick={c.go("ingredients")}) {Text("Update pantry")} }
                    item { Text("Ingredients",style=MaterialTheme.typography.titleLarge); recipe.summary.ingredients.forEach { ingredient -> val scaled=RecipeMatcher.scaled(ingredient,recipe.summary.servings,s.brief.servings); Column(Modifier.fillMaxWidth().padding(vertical=10.dp)) {Text(ingredient.name,style=MaterialTheme.typography.titleMedium);Text("${amount(scaled)} · ${ingredient.state}${if(ingredient.optional)" · optional" else ""}",color=MaterialTheme.colorScheme.onSurfaceVariant)};HorizontalDivider() }; Caption("Allergen declarations: ${recipe.summary.allergens.joinToString().ifBlank {"not specified"}}. Check labels and cross-contact; an AI recipe is not an allergy guarantee.") }
                    item { RecipeNutrition(recipe,s.brief.servings); Text("The method",style=MaterialTheme.typography.titleLarge); if(recipe.summary.servings!=s.brief.servings)Caption("Original method is for ${recipe.summary.servings} servings. Use scaled amounts above and step quantities in cooking mode; pan size and timings may need adjustment.") }
                    items(recipe.steps.size) { n ->
                        OutlinedCard(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {
                            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                Text("STEP ${n+1}",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                                Text(recipe.steps[n].title,style=MaterialTheme.typography.titleMedium)
                                Text(recipe.steps[n].instruction,style=MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    item { recipe.notes.forEach {Text(it)}; Button(onClick=c::prepareCooking,enabled=s.busy==null,modifier=Modifier.fillMaxWidth()) {Text("Start cooking")}; OutlinedButton(onClick=c::saveRecipe,enabled=s.busy==null,modifier=Modifier.fillMaxWidth()) {Text("Save recipe / my version")}; TextButton(onClick={c.go("chat")}) {Text("Ask about this recipe / rescue mode")} }
                }
                "prepare" -> s.selected?.let { recipe ->
                    val match=RecipeMatcher.match(recipe.summary,s.pantry,s.brief)
                    item { KitchenTitle("Before the first chop.","Confirm the amounts and preparation state you actually have. This does not buy ingredients or cook dry beans for you.") }
                    items(recipe.summary.ingredients.size) { index ->
                        val ingredient=RecipeMatcher.scaled(recipe.summary.ingredients[index],recipe.summary.servings,s.brief.servings)
                        if(!ingredient.optional) CheckRow("${amount(ingredient)} ${ingredient.name} · ${ingredient.state}",match.missing.none {it.key==ingredient.key && it.state==ingredient.state}) {c.confirmCookingIngredient(index,it)}
                    }
                    item { Text("Equipment",style=MaterialTheme.typography.titleLarge) }
                    items(recipe.summary.equipment) { tool -> CheckRow(tool,tool in s.brief.equipment) {checked->c.updateBrief(s.brief.copy(equipment=if(checked)(s.brief.equipment+tool).distinct() else s.brief.equipment-tool))} }
                    if(match.blocked.isNotEmpty()) item { Text(match.blocked.joinToString(),color=MaterialTheme.colorScheme.error);TextButton(onClick={c.go("brief")}) {Text("Review food restrictions")} }
                    item { Button(onClick=c::startCooking,enabled=s.busy==null && match.missing.isEmpty() && match.blocked.isEmpty(),modifier=Modifier.fillMaxWidth().testTag("begin_cooking")) {Text("Begin cooking")}; Caption("Confirm every required item above to continue.") }
                }
                "cook" -> s.session?.let { session ->
                    val step=session.recipe.steps[session.step]
                    item { var keepAwake by remember {mutableStateOf(false)}; val view=LocalView.current; DisposableEffect(keepAwake) {view.keepScreenOn=keepAwake;onDispose {view.keepScreenOn=false}}; CheckRow("Keep screen awake while cooking",keepAwake) {keepAwake=it} }
                    item { Caption("${session.recipe.summary.title} · STEP ${session.step+1} OF ${session.recipe.steps.size}"); LinearProgressIndicator(progress={ (session.step+1).toFloat()/session.recipe.steps.size },modifier=Modifier.fillMaxWidth()); KitchenTitle(step.title,step.instruction) }
                    item { session.recipe.summary.ingredients.filter {it.key in step.ingredientKeys}.forEach {val scaled=RecipeMatcher.scaled(it,session.recipe.summary.servings,session.servings);Text("${amount(scaled)} ${it.name}")} }
                    item { var now by remember {mutableLongStateOf(System.currentTimeMillis())}; LaunchedEffect(session.timerEndsAt) {while(session.timerEndsAt!=null) {now=System.currentTimeMillis();delay(1000)}}
                        session.timerEndsAt?.let {val seconds=((it-now)/1000).coerceAtLeast(0); Text(if(seconds==0L)"Timer ended · check your food" else String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60),style=MaterialTheme.typography.displaySmall); TextButton(onClick={c.timer(0)}) {Text("Cancel timer")} }
                        if(step.timerMinutes>0)OutlinedButton(onClick={c.timer(step.timerMinutes);if(Build.VERSION.SDK_INT>=33)notifications.launch(Manifest.permission.POST_NOTIFICATIONS)},enabled=s.busy==null) {Text("Start ${step.timerMinutes} minute timer")}
                        Caption("Timer progress is saved. Android may delay background reminders; notifications require permission. A timer does not verify safe doneness.") }
                    item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick={c.step(-1)},enabled=session.step>0 && s.busy==null,modifier=Modifier.weight(1f)) {Text("Previous")}; Button(onClick={if(session.step==session.recipe.steps.lastIndex)c.go("finish") else c.step(1)},enabled=s.busy==null,modifier=Modifier.weight(1f)) {Text(if(session.step==session.recipe.steps.lastIndex)"Finish" else "Next step")} }; TextButton(onClick={c.go("chat")}) {Text("Ask / rescue this step")} }
                }
                "finish" -> s.session?.let { session ->
                    item { KitchenTitle(if(session.confirmedCooked)"Made by you." else "Did you cook this?",session.recipe.summary.title) }
                    item { var note by remember(session.id) {mutableStateOf(session.note)}; OutlinedTextField(note,{note=it.take(2000)},label={Text("Changes, taste, texture or notes (optional)")},readOnly=session.confirmedCooked,modifier=Modifier.fillMaxWidth(),minLines=3)
                        if(!session.confirmedCooked) { Button(onClick={c.confirmCooked(note)},enabled=s.busy==null,modifier=Modifier.fillMaxWidth().testTag("confirm_cooked")) {Text("Confirm cooked")}; TextButton(onClick={c.go("cook")}) {Text("Not yet · return to cooking")} }
                        else { Caption("Cooked does not mean eaten or a daily intake entry. Recipe estimates and plate estimates stay separate."); OutlinedButton(onClick=c::saveCookedExplicitly,enabled=s.busy==null,modifier=Modifier.fillMaxWidth()) {Text("Save cooked meal & recipe")}; Button(onClick={takePhoto(true,session.id)},Modifier.fillMaxWidth()) {Text("Scan & review in KalC")}; TextButton(onClick={pickPhotos(true,session.id)}) {Text("Choose a finished-plate photo")}; TextButton(onClick={c.go("history")}) {Text("Done for now · open history")} }
                    }
                }
                "chat" -> s.selected?.let { recipe ->
                    item { KitchenTitle("A little help,\nright here.","${recipe.summary.title} · Step ${(s.session?.takeIf {it.recipe.version==recipe.version}?.step?:0)+1}. Questions and rescue tips stay with this recipe.") }
                    items(s.messages.size) { n -> val message=s.messages[n]; Surface(color=if(message.first)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,shape=RoundedCornerShape(16.dp)) {Text(message.second,Modifier.fillMaxWidth().padding(16.dp))} }
                    item { var question by remember {mutableStateOf("")}; OutlinedTextField(question,{question=it.take(1000)},label={Text("Ask about this recipe or request a swap")},modifier=Modifier.fillMaxWidth(),minLines=2); Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {Button(onClick={c.ask(question);question=""},enabled=question.isNotBlank() && s.busy==null) {Text("Ask AI")}; OutlinedButton(onClick={c.propose(question)},enabled=question.isNotBlank() && s.busy==null) {Text("Propose change")}}; Caption("A chat answer does not change the recipe. Propose change builds a complete version for you to review and accept.") }
                    s.proposed?.let { proposal -> item { Text("Proposed: ${proposal.summary.title}",style=MaterialTheme.typography.titleLarge); Text(proposal.summary.description); Text("${proposal.summary.totalMinutes} min · ${proposal.summary.servings} servings"); proposal.summary.ingredients.forEach {Text("${amount(it)} ${it.name} · ${it.state}")}; proposal.steps.forEachIndexed {n,step->Text("${n+1}. ${step.title}: ${step.instruction}")}; proposal.notes.forEach {Text(it)}; Button(onClick=c::acceptProposal) {Text("Accept new recipe version")}; TextButton(onClick=c::declineProposal) {Text("Keep original")} } }
                }
                "history" -> {
                    item { KitchenTitle("Your kitchen,\nremembered.","Complete meal scans, confirmed cooked meals and saved recipes. Reopen them offline without asking AI again.") }
                    item { CheckRow("Automatically save local history",s.historyEnabled,c::setHistoryEnabled); Caption("Off stops new automatic saves; explicit Save still works. Existing entries stay until deleted. About ${s.storageBytes/1024/1024} MB on this device. No cloud sync; uninstall or clear-data can remove it.") }
                    item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf(null to "All",HistoryKind.SCANNED to "Scanned",HistoryKind.COOKED to "Cooked",HistoryKind.RECIPE to "Recipes").forEach {(kind,label)->FilterChip(selected=s.historyKind==kind,onClick={c.refreshHistory(kind)},label={Text(label)})} } }
                    items(s.historyRows,key={it.id}) { row -> HistoryRowCard(row) {c.openHistory(row.id)} }
                    if(s.historyRows.isEmpty())item {Text("Your first saved meal will appear here. Sample meals are not automatically added.")}
                    if(s.hasMoreHistory)item {TextButton(onClick={c.refreshHistory(more=true)}) {Text("Load older entries")}}
                    item { TextButton(onClick={deletion="all"}) {Text("Clear local history",color=MaterialTheme.colorScheme.error)} }
                }
                "historyDetail" -> s.historyRecord?.let { record ->
                    item { KitchenTitle(record.title,"${record.kind.name.lowercase().replaceFirstChar{it.uppercase()}} · ${DateFormat.getDateTimeInstance().format(Date(record.createdAt))}") }
                    c.history.photo(record)?.let { photo -> item { AsyncImage(photo,"Saved meal photo",Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(20.dp)).clickable {scope.launch { runCatching {withContext(Dispatchers.IO) {KitchenImages.decode(context,Uri.fromFile(photo))}}.onSuccess {viewingPhoto=it}.onFailure {mediaError="This saved photo could not be opened."} }},contentScale=ContentScale.Crop) } }
                    item { var note by remember(record.id,record.note) {mutableStateOf(record.note)}; OutlinedTextField(note,{note=it.take(2000)},label={Text("Your notes: changes, taste or texture")},modifier=Modifier.fillMaxWidth(),minLines=2); TextButton(onClick={c.saveHistoryNote(note)},enabled=s.busy==null && note!=record.note) {Text("Save notes")} }
                    record.meal?.let { meal -> item { Text("Saved plate estimate",style=MaterialTheme.typography.titleLarge); Text("${meal.total.calories} kcal · ${meal.total.protein} g protein · ${meal.total.carbs} g carbs · ${meal.total.fat} g fat"); meal.foods.forEach {Text("${it.name} · ${it.weightGrams.toInt()} g · ${it.calories} kcal")}; Text(meal.uncertaintyExplanation); record.insights?.let {Text(it.headline);(it.positives+it.concerns+it.suggestions).forEach {Text("• $it")}}; if(record.personalizedInsights)Caption("Historical personalized insight. Opening it does not send profile data or enable new personalization."); Button(onClick={viewModel.loadHistoryScan(record)},Modifier.fillMaxWidth()) {Text("Open saved meal in KalC")} } }
                    record.recipe?.let { recipe -> item { Text("Saved recipe version",style=MaterialTheme.typography.titleLarge);Text("${recipe.summary.title} · ${record.servings} servings");Caption("Version ${recipe.version.take(8)}. Saved availability is not today's pantry.");OutlinedButton(onClick=c::openSavedRecipe,Modifier.fillMaxWidth()) {Text("Full recipe / make again")}; if(record.kind==HistoryKind.COOKED) {Button(onClick={takePhoto(true,record.id)},Modifier.fillMaxWidth()) {Text("Scan this cooked meal")}; TextButton(onClick={pickPhotos(true,record.id)}) {Text("Choose finished-plate photo")}} } }
                    items(s.linkedScans,key={it.id}) { scan -> HistoryRowCard(HistoryRow(scan.id,scan.kind,scan.title,scan.createdAt,scan.linkedCookedId)) {c.openHistory(scan.id)} }
                    if(record.linkedCookedId!=null)item { TextButton(onClick={c.openHistory(record.linkedCookedId)}) {Text("Open linked cooked recipe")}; OutlinedButton(onClick={c.review(record)},enabled=s.busy==null) {Text(if(record.review==null)"Review how it turned out" else "Refresh cooking review")} }
                    record.review?.let { review -> item { Text("Cooking review",style=MaterialTheme.typography.titleLarge); Text("What appears to have worked",style=MaterialTheme.typography.titleMedium);review.observations.forEach {Text("• $it")};Text("What remains uncertain",style=MaterialTheme.typography.titleMedium);review.uncertain.forEach {Text("• $it")};Text("Next time",style=MaterialTheme.typography.titleMedium);review.nextTime.forEach {Text("• $it")};Caption("AI feedback is an estimate. Taste, internal temperature and safe doneness cannot be verified from a photo.") } }
                    item { if(record.imagePath!=null)TextButton(onClick={deletion="photo"}) {Text("Remove photo, keep information")};TextButton(onClick={deletion="entry"}) {Text("Delete this entry",color=MaterialTheme.colorScheme.error)} }
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    viewingPhoto?.let {photo -> FullScreenImageViewerDialog(photo,null,"Meal photo",{viewingPhoto=null})}
    editing?.let { index -> IngredientDialog(if(index>=0)s.pantry.getOrNull(index) else null,onDismiss={editing=null}) { ingredient -> c.editIngredient(index,ingredient);editing=null } }
    deletion?.let { kind -> AlertDialog(onDismissRequest={deletion=null},title={Text(if(kind=="all")"Clear local history?" else if(kind=="photo")"Remove this photo?" else "Delete this entry?")},text={Text(if(kind=="all")"Removes all saved meal, recipe and review entries and their history photos on this device. Current cooking progress is separate." else if(kind=="photo")"Keeps the meal information and recipe while deleting its saved photo." else "Deletes this record and its photo. Linked scans are kept as standalone scans; separately saved recipes remain.")},confirmButton={TextButton(onClick={c.deleteHistory(if(kind=="all")null else s.historyRecord?.id,kind=="photo");deletion=null}) {Text("Delete")}},dismissButton={TextButton(onClick={deletion=null}) {Text("Keep")}}) }
}

@Composable private fun KitchenTitle(title: String, subtitle: String) { Text(title,style=MaterialTheme.typography.headlineLarge); Spacer(Modifier.height(10.dp)); Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant) }
@Composable private fun Caption(text: String) { Text(text,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(vertical=8.dp)) }
@Composable private fun CheckRow(label: String, checked: Boolean, change: (Boolean)->Unit) { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Checkbox(checked,change);TextButton(onClick={change(!checked)},modifier=Modifier.weight(1f)) { Text(label,modifier=Modifier.fillMaxWidth()) } } }
@Composable private fun ChoiceChips(label: String, choices: List<String>, selected: String, choose: (String)->Unit) {Text(label,style=MaterialTheme.typography.titleMedium);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {choices.forEach {FilterChip(selected=it==selected,onClick={choose(it)},label={Text(it)})}}}
fun amount(i: KitchenIngredient) = if(i.quantity==null)"Amount unknown" else "${String.format(Locale.ROOT,"%.1f",i.quantity).removeSuffix(".0")} ${i.unit}"
@Composable private fun RecipeCard(recipe: RecipeSummary, missing: String, open: ()->Unit) { OutlinedCard(onClick=open,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)) {Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {Text("${recipe.style} · ${recipe.effort}",style=MaterialTheme.typography.labelSmall);Text(recipe.title,style=MaterialTheme.typography.titleLarge);Text(recipe.description);Text("${recipe.totalMinutes} min total · ${recipe.activeMinutes} min active"); Text(missing,color=MaterialTheme.colorScheme.primary)}} }
@Composable private fun HistoryRowCard(row: HistoryRow, open: ()->Unit) { OutlinedCard(onClick=open,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)) {Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {Text(row.title,style=MaterialTheme.typography.titleMedium);Caption("${row.kind.name.lowercase()} · ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(Date(row.createdAt))}${if(row.linkedCookedId!=null)" · linked to cooking" else ""}")}} }
@Composable private fun EditorialImage(title: String, forcedTile: Int? = null) {
    val image=ImageBitmap.imageResource(R.drawable.recipe_editorial)
    var fullImage by remember {mutableStateOf<Bitmap?>(null)}
    val lower=title.lowercase(); val tile=forcedTile?:when {"biryani" in lower->6;"flatbread" in lower->7;"omelette" in lower->5;"curry" in lower && "chickpea" in lower->4;"egg" in lower->3;"salad" in lower || "bowl" in lower->2;"chickpea" in lower->1;else->0}
    Canvas(Modifier.testTag("recipe_illustration").fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)).clickable {fullImage=Bitmap.createBitmap(image.asAndroidBitmap(),tile%4*(image.width/4),tile/4*(image.height/2),image.width/4,image.height/2)}) { val tw=image.width/4; val th=image.height/2
        val ratio=size.width/size.height
        val sw=minOf(tw,(th*ratio).toInt()).coerceAtLeast(1)
        val sh=minOf(th,(tw/ratio).toInt()).coerceAtLeast(1)
        drawImage(image,IntOffset(tile%4*tw+(tw-sw)/2,tile/4*th+(th-sh)/2),IntSize(sw,sh),IntOffset.Zero,IntSize(size.width.toInt(),size.height.toInt())) }
    fullImage?.let { FullScreenImageViewerDialog(it,null,"Recipe illustration",{fullImage=null}) }
}
@Composable private fun IngredientDialog(original: KitchenIngredient?, onDismiss: ()->Unit, save: (KitchenIngredient)->Unit) {
    var name by remember {mutableStateOf(original?.name?:"")};var quantity by remember {mutableStateOf(original?.quantity?.toString()?:"")};var unit by remember {mutableStateOf(original?.unit?:"g")};var state by remember {mutableStateOf(original?.state?:"unknown")};var soon by remember {mutableStateOf(original?.useSoon?:false)};var error by remember {mutableStateOf<String?>(null)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Confirm an ingredient")},text={ Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {OutlinedTextField(name,{name=it.take(100)},label={Text("Ingredient name")},modifier=Modifier.testTag("ingredient_name"));OutlinedTextField(quantity,{quantity=it.take(12)},label={Text("Usable amount (blank if unknown)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),modifier=Modifier.testTag("ingredient_quantity"));ChoiceChips("Unit",KitchenIngredient.units,unit) {unit=it};ChoiceChips("Preparation state",KitchenIngredient.states,state) {state=it};CheckRow("Use soon (your choice, not an expiry prediction)",soon) {soon=it};error?.let {Text(it,color=MaterialTheme.colorScheme.error)}} },confirmButton={TextButton(onClick={runCatching {require(quantity.isBlank() || quantity.toDoubleOrNull()!=null) {"Enter a valid amount."};KitchenIngredient(name,quantity.toDoubleOrNull(),unit,state,useSoon=soon).validated()}.onSuccess(save).onFailure {error=it.message}},modifier=Modifier.testTag("save_ingredient")) {Text("Save ingredient")}},dismissButton={TextButton(onClick=onDismiss) {Text("Cancel")}})
}
