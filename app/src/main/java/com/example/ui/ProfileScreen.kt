package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.profile.UserProfile

@Composable
internal fun ProfileScreen(viewModel: FoodCalorieViewModel, state: FoodCalorieUiState) {
    var editing by rememberSaveable { mutableStateOf(state.showProfileOnboarding) }
    var confirmDelete by remember { mutableStateOf(false) }
    if (editing || state.showProfileOnboarding) {
        ProfileWizard(
            initial = state.profile, onboarding = state.showProfileOnboarding,
            busy = state.isSavingProfile, saveError = state.profileError,
            onSkip = { if (state.showProfileOnboarding) viewModel.skipProfileOnboarding() else editing = false },
            onSave = { profile -> viewModel.saveProfile(profile) { if (it) editing = false } }
        )
        return
    }
    BackHandler { if (!state.isSavingProfile) viewModel.closeProfile() }
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::closeProfile) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back to meal") }
                Text("YOUR SPACE", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp)
            }
            Text(if (state.profile.name.isBlank()) "A little more you." else "Hello, ${state.profile.name}.", style = MaterialTheme.typography.displaySmall)
            Text("Better context. More thoughtful suggestions. Always your choice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick=viewModel::openHistory, modifier=Modifier.fillMaxWidth()) { Text("Your history & saved recipes") }
            PersonalizationChoice(state.profile.personalizationEnabled, !state.isSavingProfile, viewModel::setPersonalization)
            state.profileError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.isSavingProfile) LinearProgressIndicator(Modifier.fillMaxWidth())
            Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Your profile", style = MaterialTheme.typography.titleLarge)
                    ProfileDetails(state.profile)
                    OutlinedButton(onClick = { editing = true }, enabled = !state.isSavingProfile, modifier = Modifier.fillMaxWidth().testTag("edit_profile")) { Text("Edit your details") }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.Shield, null, tint = MaterialTheme.colorScheme.primary)
                Text("Encrypted on this device. Excluded from backups. Your name stays here; other details are sent to Google only while personalization is on.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            TextButton(onClick = { confirmDelete = true }, enabled = !state.isSavingProfile, modifier = Modifier.testTag("delete_profile")) {
                Text("Delete saved profile", color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false }, title = { Text("Delete your profile?") },
        text = { Text("This removes saved details from this device, turns personalization off, and clears existing advice and chat. Already-sent AI requests cannot be recalled.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteProfile() }, modifier = Modifier.testTag("confirm_delete_profile")) { Text("Delete profile") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep profile") } }
    )
}

@Composable
private fun PersonalizationChoice(enabled: Boolean, interactive: Boolean, onChange: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Personalize answers", style = MaterialTheme.typography.titleMedium)
                    Text(if (enabled) "On · profile allowed in AI guidance" else "Off · general meal guidance", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.testTag("personalization_status"))
                }
                Switch(checked = enabled, onCheckedChange = onChange, enabled = interactive, modifier = Modifier.testTag("personalization_toggle").semantics { contentDescription = "Personalize answers" })
            }
            Text("On shares your saved age, body details, goals, preferences and health notes with Google for insights/chat. Your name stays local; photos never use the profile. Off stops future sharing, but cannot recall sent requests.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Changing the profile clears old advice/chat. Guidance is not diagnosis or treatment.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProfileDetails(profile: UserProfile) {
    val body = listOfNotNull(profile.age?.let { "$it years" }, profile.heightCm?.let { "$it cm" }, profile.weightKg?.let { "$it kg" }).joinToString(" · ")
    Text(if (body.isBlank()) "Body details · not added" else body, style = MaterialTheme.typography.bodyMedium)
    for ((label, text) in listOf("Activity" to profile.activity, "Goal" to profile.goal, "Preferences & allergies" to profile.preferences, "Health notes" to profile.healthNotes)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text.ifBlank { "Not added" }, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileWizard(initial: UserProfile, onboarding: Boolean, busy: Boolean, saveError: String?, onSkip: () -> Unit, onSave: (UserProfile) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf(initial.name) }
    var age by remember { mutableStateOf(initial.age?.toString() ?: "") }
    var height by remember { mutableStateOf(initial.heightCm?.toString() ?: "") }
    var weight by remember { mutableStateOf(initial.weightKg?.toString() ?: "") }
    var activity by remember { mutableStateOf(initial.activity) }
    var goal by remember { mutableStateOf(initial.goal) }
    var preferences by remember { mutableStateOf(initial.preferences) }
    var health by remember { mutableStateOf(initial.healthNotes) }
    var enabled by remember { mutableStateOf(initial.personalizationEnabled) }
    var error by remember { mutableStateOf<String?>(null) }
    fun draft(): UserProfile {
        val parsedAge = if (age.isBlank()) null else age.toIntOrNull() ?: throw IllegalArgumentException("Use whole years for age.")
        val parsedHeight = if (height.isBlank()) null else height.toDoubleOrNull() ?: throw IllegalArgumentException("Use centimeters for height.")
        val parsedWeight = if (weight.isBlank()) null else weight.toDoubleOrNull() ?: throw IllegalArgumentException("Use kilograms for weight.")
        return UserProfile(name, parsedAge, parsedHeight, parsedWeight, activity, goal, preferences, health, enabled).validated()
    }
    BackHandler(enabled = !busy) { if (step > 0) { step--; error = null } else onSkip() }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (error ?: saveError)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(onClick = {
                    keyboard?.hide()
                    runCatching { draft() }.onSuccess { if (step == 3) onSave(it) else { step++; error = null } }
                        .onFailure { error = it.message }
                }, enabled = !busy, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("profile_next")) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    else Text(if (step == 3) { if (onboarding) "Save & get started" else "Save profile" } else "Continue")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                if (step > 0) IconButton(onClick = { step--; error = null }, enabled = !busy) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous step") }
                else Text("KalC", style = MaterialTheme.typography.headlineMedium)
                TextButton(onClick = onSkip, enabled = !busy, modifier = Modifier.testTag("skip_profile")) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp)); Text(if (onboarding) "Skip" else "Cancel")
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("${step + 1} OF 4 · ${listOf("YOUR INTENT", "YOUR EVERYDAY", "YOUR PREFERENCES", "YOUR CHOICE")[step]}", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(progress = { (step + 1) / 4f }, modifier = Modifier.fillMaxWidth())
            AnimatedContent(targetState = step, transitionSpec = { (slideInHorizontally { it / 8 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 8 } + fadeOut()) }, label = "profile_step") { page ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 24.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(listOf("Good food.\nMore you.", "A little context.", "What matters to you?", "Your profile.\nYour call.")[page], style = MaterialTheme.typography.displaySmall)
                    when (page) {
                        0 -> {
                            Text("A few optional details can make meal guidance more relevant. Skip anything. Personalization starts off.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ProfileField(name, { name = it.take(60) }, "First name (optional, stays on device)", "profile_name")
                            Text("What would you like to work toward?", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (choice in listOf("Eat more balanced meals", "Build strength", "Manage my weight")) {
                                    FilterChip(selected = goal == choice, onClick = { goal = if (goal == choice) "" else choice }, label = { Text(choice) })
                                }
                            }
                            ProfileField(goal, { goal = it.take(500) }, "Your goal, in your words (optional)", "profile_goal", lines = 2)
                        }
                        1 -> {
                            Text("Use only the details you want to share. These do not change measured meal calories.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ProfileField(age, { age = it.take(3) }, "Age in years (optional)", "profile_age", KeyboardType.Number)
                            ProfileField(height, { height = it.take(7) }, "Height in cm (optional)", "profile_height", KeyboardType.Decimal)
                            ProfileField(weight, { weight = it.take(7) }, "Weight in kg (optional)", "profile_weight", KeyboardType.Decimal)
                            Text("Your usual activity", style = MaterialTheme.typography.titleMedium)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (choice in listOf("Mostly sitting", "Moderately active", "Very active")) {
                                    FilterChip(selected = activity == choice, onClick = { activity = if (activity == choice) "" else choice }, label = { Text(choice) })
                                }
                            }
                        }
                        2 -> {
                            Text("Optional notes help avoid unsuitable food suggestions. Share only what you are comfortable sending to AI when you enable personalization.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            ProfileField(preferences, { preferences = it.take(500) }, "Food preferences & allergies (optional)", "profile_preferences", lines = 3)
                            ProfileField(health, { health = it.take(500) }, "Health conditions or notes (optional)", "profile_health", lines = 3)
                            Text("AI cannot diagnose or prescribe treatment. Use professional guidance for medical decisions.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        3 -> {
                            Text("Save the profile on this device. Decide separately whether AI may use it.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            PersonalizationChoice(enabled, !busy) { enabled = it }
                            val preview = runCatching { draft() }.getOrNull()
                            preview?.let { ProfileDetails(it) }
                            Text("You can edit, turn off, or delete your profile in the You section anytime. Skipping discards this draft.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileField(value: String, onChange: (String) -> Unit, label: String, tag: String, type: KeyboardType = KeyboardType.Text, lines: Int = 1) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth().testTag(tag),
        keyboardOptions = KeyboardOptions(keyboardType = type), singleLine = lines == 1, minLines = lines,
        shape = RoundedCornerShape(14.dp))
}
