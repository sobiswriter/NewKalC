package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DetectedFoodItem
import com.example.data.model.MealAnalysisResult
import com.example.data.nutrition.NutritionDatabase
import com.example.ui.theme.ColorCalories
import com.example.ui.theme.ColorCarbs
import com.example.ui.theme.ColorFat
import com.example.ui.theme.ColorProtein
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodCalorieScreen(
    viewModel: FoodCalorieViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddFoodDialog by remember { mutableStateOf(false) }

    // Media Pickers
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.onImageSelectedFromUri(context, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            viewModel.onImageCapturedFromCamera(bitmap)
        }
    }

    LaunchedEffect(uiState.feedbackBanner) {
        uiState.feedbackBanner?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Food Calorie AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                text = "Gemini 3.8 Flash • Nutrition DB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    // Optimized status badge on top right
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (uiState.isApiKeyConfigured) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            )
                            .clickable { viewModel.openApiKeyDialog(isStartup = false) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("api_key_status_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "API Key",
                                tint = if (uiState.isApiKeyConfigured) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (uiState.isApiKeyConfigured) "AI Live" else "Set Key",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isApiKeyConfigured) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("main_food_calorie_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Food Image Input Card (Optimized layout preventing text wrapping)
            item {
                OptimizedImageInputSection(
                    uiState = uiState,
                    samplePresets = viewModel.samplePresets,
                    onSelectPreset = { preset -> viewModel.selectSamplePreset(context, preset) },
                    onUploadClick = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onCameraClick = {
                        cameraLauncher.launch()
                    },
                    onReanalyze = {
                        viewModel.retryAnalysis()
                    }
                )
            }

            // 2. Error Card (if any error occurred)
            if (uiState.analysisState is AnalysisUiState.Error) {
                item {
                    val errorMsg = (uiState.analysisState as AnalysisUiState.Error).message
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analysis_error_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Analysis Notice",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { viewModel.retryAnalysis() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Retry (3.8 Flash)", fontSize = 12.sp, maxLines = 1)
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.useOfflineAnalysisForCurrentImage() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Offline Est.", fontSize = 12.sp, maxLines = 1)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            TextButton(
                                onClick = { viewModel.openApiKeyDialog(isStartup = false) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Check / Change Google AI Studio API Key", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. Analyzing Loading State
            if (uiState.analysisState is AnalysisUiState.Analyzing) {
                item {
                    val stepMessage = (uiState.analysisState as AnalysisUiState.Analyzing).stepMessage
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("analyzing_progress_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Analyzing Food with Gemini 3.8 Flash",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = stepMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Tab Selector (Responsive & clearly legible)
            item {
                TabRow(
                    selectedTabIndex = uiState.activeTabIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .testTag("output_tab_row")
                ) {
                    Tab(
                        selected = uiState.activeTabIndex == 0,
                        onClick = { viewModel.setActiveTab(0) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Meal", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = uiState.activeTabIndex == 1,
                        onClick = { viewModel.setActiveTab(1) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = uiState.activeTabIndex == 2,
                        onClick = { viewModel.setActiveTab(2) },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Database", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    )
                }
            }

            // Content according to selected Tab
            val activeMeal = uiState.activeMealResult
            if (activeMeal != null) {
                when (uiState.activeTabIndex) {
                    0 -> {
                        // Tab 0: Visual Meal Breakdown & Human Correction
                        item {
                            MealTotalsCard(
                                meal = activeMeal,
                                modifier = Modifier.testTag("meal_totals_card")
                            )
                        }

                        // Uncertainty Explanation Callout
                        item {
                            UncertaintyExplanationBanner(
                                explanation = activeMeal.uncertaintyExplanation,
                                confidence = activeMeal.overallConfidence
                            )
                        }

                        // Human Correction Section
                        item {
                            HumanCorrectionSection(
                                correctionText = uiState.naturalLanguageCorrection,
                                onTextChanged = { viewModel.onNaturalLanguageCorrectionChanged(it) },
                                onApply = { viewModel.applyNaturalLanguageCorrection() },
                                onQuickAction = { command -> viewModel.applyNaturalLanguageCorrection(command) }
                            )
                        }

                        // Food Items Header & Add Button
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Identified Foods (${activeMeal.foods.size})",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                OutlinedButton(
                                    onClick = { showAddFoodDialog = true },
                                    modifier = Modifier.testTag("add_food_button"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Item", fontSize = 12.sp)
                                }
                            }
                        }

                        // Items list
                        items(activeMeal.foods, key = { it.id }) { food ->
                            DetectedFoodItemCard(
                                food = food,
                                onWeightChange = { newWeight ->
                                    viewModel.updateFoodWeight(food.id, newWeight)
                                },
                                onRemove = {
                                    viewModel.removeFoodItem(food.id)
                                }
                            )
                        }
                    }

                    1 -> {
                        // Tab 1: Structured JSON Response View
                        item {
                            JsonOutputView(
                                jsonString = activeMeal.toFormattedJson(),
                                onCopyClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Food Calorie JSON", activeMeal.toFormattedJson())
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }

                    2 -> {
                        // Tab 2: Nutrition Database Reference Explorer
                        item {
                            NutritionDatabaseExplorer(
                                onSelectToAdd = { foodName, grams ->
                                    viewModel.addFoodItem(foodName, grams)
                                    viewModel.setActiveTab(0)
                                }
                            )
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Please choose or upload a food image above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Startup & Settings API Key Prompt Dialog
    if (uiState.showApiKeyDialog) {
        GoogleAiStudioApiKeyDialog(
            isStartup = uiState.isStartupPrompt,
            isTesting = uiState.isTestingApiKey,
            currentMaskedKey = uiState.currentApiKeyMasked,
            onDismiss = { viewModel.closeApiKeyDialog() },
            onSaveKey = { key ->
                viewModel.saveAndValidateApiKey(key) { _, _ -> }
            },
            onClearKey = {
                viewModel.clearApiKey()
            }
        )
    }

    if (showAddFoodDialog) {
        AddFoodCustomDialog(
            onDismiss = { showAddFoodDialog = false },
            onConfirm = { name, grams ->
                viewModel.addFoodItem(name, grams)
                showAddFoodDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// Sub-Components (Screen-Optimized)
// -------------------------------------------------------------

@Composable
fun OptimizedImageInputSection(
    uiState: FoodCalorieUiState,
    samplePresets: List<SampleFoodPreset>,
    onSelectPreset: (SampleFoodPreset) -> Unit,
    onUploadClick: () -> Unit,
    onCameraClick: () -> Unit,
    onReanalyze: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("image_input_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Food Photo Input",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Gemini 3.8 Flash Vision",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Dedicated full-width action buttons row (prevents awkward text wrapping)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCameraClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("camera_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Camera", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Button(
                    onClick = onUploadClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("upload_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick Test Presets Carousel
            Text(
                text = "Quick Sample Meals:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                samplePresets.forEach { preset ->
                    val isSelected = uiState.currentImageResId == preset.drawableResId
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.title, fontSize = 12.sp, maxLines = 1) },
                        leadingIcon = {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            } else {
                                Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Food Image Preview Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.currentImageBitmap != null) {
                    Image(
                        bitmap = uiState.currentImageBitmap.asImageBitmap(),
                        contentDescription = "Active food meal",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (uiState.currentImageResId != null) {
                    Image(
                        painter = painterResource(id = uiState.currentImageResId),
                        contentDescription = "Active food meal",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Overlay label badge on bottom left
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.currentImageTitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Re-analyze trigger on top right
                IconButton(
                    onClick = onReanalyze,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.90f))
                        .testTag("reanalyze_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Re-analyze with Gemini 3.8 Flash",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MealTotalsCard(
    meal: MealAnalysisResult,
    modifier: Modifier = Modifier
) {
    val total = meal.total
    val totalMacroGrams = (total.protein + total.carbs + total.fat).coerceAtLeast(1.0)
    val proteinPercent = (total.protein / totalMacroGrams).toFloat()
    val carbsPercent = (total.carbs / totalMacroGrams).toFloat()
    val fatPercent = (total.fat / totalMacroGrams).toFloat()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "TOTAL ESTIMATED CALORIES",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${total.calories}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = ColorCalories
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                }

                // Confidence Chip
                val confidencePct = (meal.overallConfidence * 100).roundToInt()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (confidencePct >= 80) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.tertiaryContainer
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (confidencePct >= 80) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$confidencePct% Confidence",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (confidencePct >= 80) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Proportional Macro Progress Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
            ) {
                if (proteinPercent > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(proteinPercent)
                            .height(8.dp)
                            .background(ColorProtein)
                    )
                }
                if (carbsPercent > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(carbsPercent)
                            .height(8.dp)
                            .background(ColorCarbs)
                    )
                }
                if (fatPercent > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(fatPercent)
                            .height(8.dp)
                            .background(ColorFat)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Macro Pills Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroPill(
                    title = "Protein",
                    amount = "${total.protein}g",
                    color = ColorProtein,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    title = "Carbs",
                    amount = "${total.carbs}g",
                    color = ColorCarbs,
                    modifier = Modifier.weight(1f)
                )
                MacroPill(
                    title = "Fat",
                    amount = "${total.fat}g",
                    color = ColorFat,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MacroPill(
    title: String,
    amount: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun UncertaintyExplanationBanner(
    explanation: String,
    confidence: Double
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("uncertainty_banner"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Uncertainty explanation",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(18.dp)
                    .padding(top = 1.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Explainable Estimation & Uncertainty:",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HumanCorrectionSection(
    correctionText: String,
    onTextChanged: (String) -> Unit,
    onApply: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("human_correction_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Human Correction (AI guesses → You correct)",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Type adjustments (e.g., 'Noodles to 180g', 'Remove salad', 'Add 1 boiled egg'):",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = correctionText,
                    onValueChange = onTextChanged,
                    placeholder = { Text("e.g. Biryani to 180g or Remove salad", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("correction_input_field"),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onApply,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("apply_correction_button")
                ) {
                    Text("Apply")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Quick Suggestion Chips:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                QuickActionChip("Biryani 250g -> 180g") { onQuickAction("Chicken Biryani 250 g -> 180 g") }
                QuickActionChip("Noodles to 180g") { onQuickAction("Noodles to 180g") }
                QuickActionChip("Remove salad") { onQuickAction("Remove salad") }
                QuickActionChip("+ Add 1 Boiled Egg (50g)") { onQuickAction("Add 1 boiled egg") }
                QuickActionChip("+ Add Roti (35g)") { onQuickAction("Add roti 35g") }
            }
        }
    }
}

@Composable
fun QuickActionChip(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun DetectedFoodItemCard(
    food: DetectedFoodItem,
    onWeightChange: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("food_item_card_${food.name.replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Name + Remove Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = food.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Cooking Method: ${food.cookingMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.testTag("remove_item_${food.name.replace(" ", "_")}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Portion Estimation & Stepper Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ESTIMATED PORTION",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${food.weightGrams.roundToInt()} g",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Steppers [-10g] [+10g]
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onWeightChange(food.weightGrams - 10.0) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease 10g", modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${food.weightGrams.roundToInt()}g",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { onWeightChange(food.weightGrams + 10.0) },
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase 10g", modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Slider for quick portion adjustments
            Slider(
                value = food.weightGrams.toFloat(),
                onValueChange = { onWeightChange(it.toDouble()) },
                valueRange = 10f..600f,
                steps = 58,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            // Visual Cues & Confidence Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Visual cues: ${food.visualCues}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Conf: ${(food.confidence * 100).roundToInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Nutrition DB Source of Truth Indicator
            Text(
                text = "Nutrition Source: ${food.nutritionReference.source} (~${food.nutritionReference.caloriesPer100g.roundToInt()} kcal / 100g)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Item Calories & Macros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Text(
                    text = "${food.calories} kcal",
                    fontWeight = FontWeight.Bold,
                    color = ColorCalories,
                    fontSize = 13.sp
                )
                Text(
                    text = "P: ${food.protein}g",
                    fontWeight = FontWeight.SemiBold,
                    color = ColorProtein,
                    fontSize = 13.sp
                )
                Text(
                    text = "C: ${food.carbs}g",
                    fontWeight = FontWeight.SemiBold,
                    color = ColorCarbs,
                    fontSize = 13.sp
                )
                Text(
                    text = "F: ${food.fat}g",
                    fontWeight = FontWeight.SemiBold,
                    color = ColorFat,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun JsonOutputView(
    jsonString: String,
    onCopyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("json_output_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Clean Structured JSON",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Contract matching the specification",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onCopyClick,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("copy_json_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy JSON",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Text(
                    text = jsonString,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun NutritionDatabaseExplorer(
    onSelectToAdd: (String, Double) -> Unit
) {
    val allReferences = remember { NutritionDatabase.getAllReferences() }
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) allReferences
        else allReferences.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.foodKey.contains(searchQuery, ignoreCase = true)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("database_explorer_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Nutrition Reference Database",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Separation rule: AI detects food + amount; Nutrition source supplies verified 100g values.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search foods (Noodles, Biryani, Salmon, Rice, Egg...)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("db_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            filteredList.forEach { ref ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ref.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Per 100g: ${ref.caloriesPer100g.roundToInt()} kcal | P: ${ref.proteinPer100g}g | C: ${ref.carbsPer100g}g | F: ${ref.fatPer100g}g",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Source: ${ref.source}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { onSelectToAdd(ref.displayName, 100.0) },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("+ Add", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddFoodCustomDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var weightInput by remember { mutableStateOf("100") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Food Item", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    text = "Enter food name and portion in grams. The system will look up nutrition automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name (e.g. Boiled Egg, Noodles, Naan)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Estimated Portion (grams)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val grams = weightInput.toDoubleOrNull() ?: 100.0
                    if (foodName.isNotBlank()) {
                        onConfirm(foodName.trim(), grams)
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// -------------------------------------------------------------
// Google AI Studio API Key Dialog (Startup & Settings)
// -------------------------------------------------------------

@Composable
fun GoogleAiStudioApiKeyDialog(
    isStartup: Boolean,
    isTesting: Boolean,
    currentMaskedKey: String,
    onDismiss: () -> Unit,
    onSaveKey: (String) -> Unit,
    onClearKey: () -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isStartup) "Welcome to Food Calorie AI" else "Google AI Studio API Key",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "To start real-time AI image analysis with Gemini 3.8 Flash, add your Google AI Studio API key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "How to get a free API key:",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "1. Go to aistudio.google.com/app/apikey\n2. Create a key and paste it below\n3. Enjoy Gemini 3.8 Flash vision recognition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                context.startActivity(intent)
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Open Google AI Studio", fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Google AI Studio API Key") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input_field"),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    if (clipText.isNotBlank()) {
                                        apiKeyInput = clipText.trim()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste from clipboard")
                            }
                        }
                    }
                )

                if (currentMaskedKey.isNotEmpty() && currentMaskedKey != "Not Set") {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Current saved key: $currentMaskedKey",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (isTesting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verifying with Gemini 3.8 Flash...", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveKey(apiKeyInput) },
                enabled = apiKeyInput.isNotBlank() && !isTesting,
                modifier = Modifier.testTag("save_api_key_button")
            ) {
                Text("Start AI Services")
            }
        },
        dismissButton = {
            Row {
                if (currentMaskedKey.isNotEmpty() && currentMaskedKey != "Not Set") {
                    TextButton(
                        onClick = onClearKey,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear Key")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(if (isStartup) "Use Demo Mode" else "Cancel")
                }
            }
        }
    )
}
