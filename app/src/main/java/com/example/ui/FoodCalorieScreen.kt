package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ChatMessage
import com.example.data.model.DetectedFoodItem
import com.example.data.model.MealAnalysisResult
import com.example.data.model.NutritionInsights
import com.example.data.nutrition.NutritionDatabase
import com.example.ui.theme.ColorCalories
import com.example.ui.theme.ColorCarbs
import com.example.ui.theme.ColorFat
import com.example.ui.theme.ColorProtein
import kotlinx.coroutines.launch
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
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Determine if header should be collapsed based on scroll
    val isHeaderCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }

    // Media pickers
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

    // Feedback banner updates
    LaunchedEffect(uiState.feedbackBanner) {
        val banner = uiState.feedbackBanner
        if (banner != null) {
            snackbarHostState.showSnackbar(banner)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainFoodTopAppBar(
                viewModel = viewModel,
                isApiKeyConfigured = uiState.isApiKeyConfigured,
                onOpenGallery = {
                    galleryLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                onOpenCamera = { cameraLauncher.launch(null) }
            )
        },
        bottomBar = {
            SmartNavigationBottomBar(
                activeTab = uiState.activeNavTab,
                onTabSelected = { tabIndex ->
                    viewModel.setActiveNavTab(tabIndex)
                    if (tabIndex == 1) {
                        // Scroll directly to insights
                        coroutineScope.launch {
                            listState.animateScrollToItem(1)
                        }
                    }
                },
                onAdjustMealClick = { viewModel.openAdjustMealSheet() }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val meal = uiState.activeMealResult

            if (meal != null) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    // 1. Food Analysis Header (Hero Summary)
                    item(key = "header") {
                        CollapsibleMealHeader(
                            meal = meal,
                            isCollapsed = isHeaderCollapsed,
                            imageBitmap = uiState.currentImageBitmap,
                            imageResId = uiState.currentImageResId,
                            title = uiState.currentImageTitle,
                            onAdjustClick = { viewModel.openAdjustMealSheet() },
                            onImageClick = {
                                galleryLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        )
                    }

                    // 2. What's In Your Meal Section (Compact Expandable Items)
                    item(key = "food_breakdown") {
                        WhatIsInYourMealSection(
                            foods = meal.foods,
                            expandedFoodId = uiState.expandedFoodId,
                            onToggleExpand = { foodId -> viewModel.toggleFoodExpansion(foodId) },
                            onAddFoodClick = { viewModel.openAddFoodSheet() },
                            onAdjustFoodClick = { viewModel.openAdjustMealSheet() }
                        )
                    }

                    // 3. AI Nutrition Insights Section
                    item(key = "insights") {
                        AiNutritionInsightsSection(
                            insights = uiState.nutritionInsights,
                            isGenerating = uiState.isGeneratingInsights,
                            isExpanded = uiState.insightsExpanded,
                            onToggleExpand = { viewModel.toggleInsightsExpansion() },
                            onRefreshInsights = { viewModel.refreshInsights() },
                            onAskAiClick = { viewModel.openAskAiSheet() }
                        )
                    }

                    // 4. Compact Secondary Action Strip
                    item(key = "footer_actions") {
                        MealFooterActions(
                            onAdjustMealClick = { viewModel.openAdjustMealSheet() },
                            onAskAiClick = { viewModel.openAskAiSheet() },
                            onViewJsonClick = { viewModel.openJsonDialog() }
                        )
                    }
                }
            } else {
                // Empty / Loading state
                EmptyOrLoadingView(
                    analysisState = uiState.analysisState,
                    onPickImage = {
                        galleryLauncher.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    },
                    onUseCamera = { cameraLauncher.launch(null) }
                )
            }

            // Floating mini-banner when analyzing
            AnimatedVisibility(
                visible = uiState.analysisState is AnalysisUiState.Analyzing,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                val analyzingStep = (uiState.analysisState as? AnalysisUiState.Analyzing)?.stepMessage
                    ?: "Analyzing food..."
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = analyzingStep,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet: Adjust Meal (Editing Surface)
    if (uiState.showAdjustMealSheet && uiState.activeMealResult != null) {
        AdjustMealBottomSheet(
            meal = uiState.activeMealResult!!,
            naturalLanguageCorrection = uiState.naturalLanguageCorrection,
            onNaturalLanguageChange = { viewModel.onNaturalLanguageCorrectionChanged(it) },
            onApplyNaturalLanguage = { viewModel.applyNaturalLanguageCorrection() },
            onUpdateWeight = { foodId, newWeight -> viewModel.updateFoodWeight(foodId, newWeight) },
            onRemoveFood = { foodId -> viewModel.removeFoodItem(foodId) },
            onAddFoodClick = { viewModel.openAddFoodSheet() },
            onRecalculateDone = { viewModel.recalculateMeal() },
            onDismiss = { viewModel.closeAdjustMealSheet() }
        )
    }

    // Modal Bottom Sheet: Add Food
    if (uiState.showAddFoodSheet) {
        AddFoodBottomSheet(
            onAddFood = { name, grams ->
                viewModel.addFoodItem(name, grams)
                viewModel.closeAddFoodSheet()
            },
            onDismiss = { viewModel.closeAddFoodSheet() }
        )
    }

    // Modal Bottom Sheet: Ask AI About This Meal
    if (uiState.showAskAiSheet) {
        AskAiBottomSheet(
            meal = uiState.activeMealResult,
            chatMessages = uiState.chatMessages,
            questionInput = uiState.currentQuestionInput,
            isAsking = uiState.isAskingQuestion,
            onQuestionChange = { viewModel.onQuestionInputChanged(it) },
            onSendQuestion = { q -> viewModel.askQuestion(q) },
            onDismiss = { viewModel.closeAskAiSheet() }
        )
    }

    // Dialog: Raw JSON (Developer / Details)
    if (uiState.showJsonDialog && uiState.activeMealResult != null) {
        RawJsonDialog(
            jsonText = uiState.activeMealResult!!.toFormattedJson(),
            onDismiss = { viewModel.closeJsonDialog() }
        )
    }

    // Dialog: Nutrition Database Explorer
    if (uiState.showDatabaseDialog) {
        NutritionDatabaseDialog(
            onDismiss = { viewModel.closeDatabaseDialog() },
            onSelectItem = { ref ->
                viewModel.addFoodItem(ref.displayName, 100.0)
                viewModel.closeDatabaseDialog()
            }
        )
    }

    // Dialog: API Key Settings
    if (uiState.showApiKeyDialog) {
        ApiKeySettingsDialog(
            currentMaskedKey = uiState.currentApiKeyMasked,
            isConfigured = uiState.isApiKeyConfigured,
            isTesting = uiState.isTestingApiKey,
            isStartup = uiState.isStartupPrompt,
            onSaveKey = { key, onDone ->
                viewModel.saveAndValidateApiKey(key, onDone)
            },
            onClearKey = { viewModel.clearApiKey() },
            onDismiss = { viewModel.closeApiKeyDialog() }
        )
    }
}

// ==========================================
// 1. TOP APP BAR
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFoodTopAppBar(
    viewModel: FoodCalorieViewModel,
    isApiKeyConfigured: Boolean,
    onOpenGallery: () -> Unit,
    onOpenCamera: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showSamplePresetsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Food Calorie AI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = CircleShape,
                        color = if (isApiKeyConfigured) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(8.dp)
                    ) {}
                }
                Text(
                    text = "Vision · Portion · Nutrition DB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(
                onClick = onOpenCamera,
                modifier = Modifier.testTag("camera_button")
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Take food photo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onOpenGallery,
                modifier = Modifier.testTag("gallery_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Upload food photo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.testTag("more_options_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options"
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Sample Food Dishes") },
                        leadingIcon = {
                            Icon(Icons.Default.Restaurant, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            showSamplePresetsDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("API Key & AI Settings") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            viewModel.openApiKeyDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("View Raw JSON") },
                        leadingIcon = {
                            Icon(Icons.Default.Code, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            viewModel.openJsonDialog()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Nutrition Database") },
                        leadingIcon = {
                            Icon(Icons.Default.Storage, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            viewModel.openDatabaseDialog()
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Re-Analyze Photo") },
                        leadingIcon = {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                        },
                        onClick = {
                            showMenu = false
                            viewModel.retryAnalysis()
                        }
                    )
                }
            }
        }
    )

    if (showSamplePresetsDialog) {
        AlertDialog(
            onDismissRequest = { showSamplePresetsDialog = false },
            title = { Text("Choose a Sample Meal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    viewModel.samplePresets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectSamplePreset(context, preset)
                                    showSamplePresetsDialog = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = preset.drawableResId),
                                    contentDescription = preset.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSamplePresetsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

// ==========================================
// 2. COLLAPSIBLE FOOD ANALYSIS HEADER
// ==========================================

@Composable
private fun CollapsibleMealHeader(
    meal: MealAnalysisResult,
    isCollapsed: Boolean,
    imageBitmap: Bitmap?,
    imageResId: Int?,
    title: String,
    onAdjustClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val total = meal.total
    val confidencePct = (meal.overallConfidence * 100).roundToInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("analysis_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (!isCollapsed) {
                // EXPANDED STATE
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Meal Photo Thumbnail
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onImageClick() }
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = "Analyzed food photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (imageResId != null) {
                            Image(
                                painter = painterResource(id = imageResId),
                                contentDescription = "Analyzed food photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Main Total Calorie Callout
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Estimated ${total.calories} kcal",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = ColorCalories
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        // Confidence pill
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                confidencePct >= 80 -> Color(0xFFDCFCE7)
                                confidencePct >= 65 -> Color(0xFFFEF3C7)
                                else -> Color(0xFFFEE2E2)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = when {
                                        confidencePct >= 80 -> Color(0xFF16A34A)
                                        confidencePct >= 65 -> Color(0xFFD97706)
                                        else -> Color(0xFFDC2626)
                                    },
                                    modifier = Modifier.size(6.dp)
                                ) {}
                                Text(
                                    text = "$confidencePct% Confidence (±12% variance)",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = when {
                                        confidencePct >= 80 -> Color(0xFF166534)
                                        confidencePct >= 65 -> Color(0xFF92400E)
                                        else -> Color(0xFF991B1B)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Full Macro Split Badges (Protein, Carbs, Fat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MacroPill(
                        label = "Protein",
                        amount = "${total.protein}g",
                        color = ColorProtein,
                        modifier = Modifier.weight(1f)
                    )
                    MacroPill(
                        label = "Carbs",
                        amount = "${total.carbs}g",
                        color = ColorCarbs,
                        modifier = Modifier.weight(1f)
                    )
                    MacroPill(
                        label = "Fat",
                        amount = "${total.fat}g",
                        color = ColorFat,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Visual uncertainty note
                if (meal.uncertaintyExplanation.isNotBlank()) {
                    Text(
                        text = meal.uncertaintyExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                // COLLAPSED STATE: Compact single-row representation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Estimated ${total.calories} kcal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorCalories
                        )
                        Text(
                            text = "${total.protein}P · ${total.carbs}C · ${total.fat}F",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    FilledTonalButton(
                        onClick = onAdjustClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("collapsed_adjust_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Adjust", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPill(
    label: String,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ==========================================
// 3. WHAT'S IN YOUR MEAL (COMPACT EXPANDABLE ROWS)
// ==========================================

@Composable
private fun WhatIsInYourMealSection(
    foods: List<DetectedFoodItem>,
    expandedFoodId: String?,
    onToggleExpand: (String) -> Unit,
    onAddFoodClick: () -> Unit,
    onAdjustFoodClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("what_is_in_your_meal_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "What's in your meal (${foods.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                onClick = onAddFoodClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("add_food_header_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Food", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                foods.forEachIndexed { index, food ->
                    val isExpanded = food.id == expandedFoodId

                    CompactFoodItemRow(
                        food = food,
                        isExpanded = isExpanded,
                        onToggle = { onToggleExpand(food.id) },
                        onAdjustPortion = onAdjustFoodClick
                    )

                    if (index < foods.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactFoodItemRow(
    food: DetectedFoodItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAdjustPortion: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("food_row_${food.id}")
    ) {
        // Compact collapsed row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = food.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${food.weightGrams.roundToInt()} g · ${food.quantityDescription}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${food.calories} kcal",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorCalories
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Expanded details
        if (isExpanded) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Macro row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            text = "Protein: ${food.protein}g",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorProtein,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Carbs: ${food.carbs}g",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorCarbs,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Fat: ${food.fat}g",
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorFat,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Prep: ${food.cookingMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Portion reasoning: ${food.visualCues}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Source: ${food.nutritionReference.source} (~${food.nutritionReference.caloriesPer100g.roundToInt()} kcal/100g)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(
                            onClick = onAdjustPortion,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adjust in Meal", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. AI NUTRITION INSIGHTS SECTION
// ==========================================

@Composable
private fun AiNutritionInsightsSection(
    insights: NutritionInsights?,
    isGenerating: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRefreshInsights: () -> Unit,
    onAskAiClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ai_nutrition_insights_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "AI Nutrition Insights",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    IconButton(
                        onClick = onRefreshInsights,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh insights",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (insights != null) {
                // Headline Takeaway
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = insights.headline,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Concise Highlights: Positives
                if (insights.positives.isNotEmpty()) {
                    Text(
                        text = "Positives",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    insights.positives.take(if (isExpanded) 4 else 1).forEach { pos ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = pos,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Concerns / Calorie density watchpoint
                if (insights.concerns.isNotEmpty()) {
                    Text(
                        text = "Calorie & Balance Watch",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    insights.concerns.take(if (isExpanded) 3 else 1).forEach { con ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = con,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Actionable Suggestions (Visible when expanded or 1 shown)
                if (insights.suggestions.isNotEmpty()) {
                    Text(
                        text = "Suggestions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    insights.suggestions.take(if (isExpanded) 3 else 1).forEach { sug ->
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .padding(top = 2.dp)
                            )
                            Text(
                                text = sug,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle Expand / Collapse
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onToggleExpand,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isExpanded) "Show Less" else "Show More Insights",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    TextButton(
                        onClick = onAskAiClick,
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ask AI About This", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Text(
                    text = "Analyzing nutrition pointers based on current meal components...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ==========================================
// 5. FOOTER QUICK ACTIONS
// ==========================================

@Composable
private fun MealFooterActions(
    onAdjustMealClick: () -> Unit,
    onAskAiClick: () -> Unit,
    onViewJsonClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onAdjustMealClick,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("adjust_meal_action_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Adjust Meal", fontWeight = FontWeight.SemiBold)
        }

        OutlinedButton(
            onClick = onAskAiClick,
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .testTag("ask_ai_action_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Ask AI", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ==========================================
// 6. SMART NAVIGATION BOTTOM BAR (MEAL | INSIGHTS | ASK AI)
// ==========================================

@Composable
private fun SmartNavigationBottomBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    onAdjustMealClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Segmented tab bar
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavigationTabItem(
                    title = "Meal",
                    icon = Icons.Default.Restaurant,
                    isSelected = activeTab == 0,
                    onClick = { onTabSelected(0) },
                    modifier = Modifier.weight(1f)
                )
                NavigationTabItem(
                    title = "Insights",
                    icon = Icons.Default.Lightbulb,
                    isSelected = activeTab == 1,
                    onClick = { onTabSelected(1) },
                    modifier = Modifier.weight(1f)
                )
                NavigationTabItem(
                    title = "Ask AI",
                    icon = Icons.AutoMirrored.Filled.Chat,
                    isSelected = activeTab == 2,
                    onClick = { onTabSelected(2) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Quick Adjust Floating Action
            FilledTonalButton(
                onClick = onAdjustMealClick,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier.testTag("quick_adjust_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Adjust meal portions",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Edit", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NavigationTabItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        label = "tab_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tab_content"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

// ==========================================
// 7. ADJUST MEAL BOTTOM SHEET (EDITING SURFACE)
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdjustMealBottomSheet(
    meal: MealAnalysisResult,
    naturalLanguageCorrection: String,
    onNaturalLanguageChange: (String) -> Unit,
    onApplyNaturalLanguage: () -> Unit,
    onUpdateWeight: (String, Double) -> Unit,
    onRemoveFood: (String) -> Unit,
    onAddFoodClick: () -> Unit,
    onRecalculateDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val total = meal.total

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Adjust Meal",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Live Total: ${total.calories} kcal · ${total.protein}P · ${total.carbs}C · ${total.fat}F",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorCalories,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Natural Language Quick Correction Bar
            OutlinedTextField(
                value = naturalLanguageCorrection,
                onValueChange = onNaturalLanguageChange,
                placeholder = { Text("e.g. 'Rice to 200g', 'Remove salad', 'Add 1 egg'") },
                trailingIcon = {
                    if (naturalLanguageCorrection.isNotBlank()) {
                        IconButton(onClick = onApplyNaturalLanguage) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Apply correction",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("natural_language_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable list of items to edit
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(meal.foods, key = { it.id }) { item ->
                    MealItemEditRow(
                        item = item,
                        onWeightChanged = { newWeight -> onUpdateWeight(item.id, newWeight) },
                        onRemove = { onRemoveFood(item.id) }
                    )
                }

                item {
                    OutlinedButton(
                        onClick = onAddFoodClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .testTag("add_food_inside_adjust_sheet"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Food / Side Dish")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Recalculate & Done Button
            Button(
                onClick = onRecalculateDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("recalculate_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Recalculate & Done",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MealItemEditRow(
    item: DetectedFoodItem,
    onWeightChanged: (Double) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top: Name + Calories + Trash
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${item.calories} kcal (${item.protein}P · ${item.carbs}C · ${item.fat}F)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove ${item.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Portion Stepper: [-] 180 g [+]
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = { onWeightChanged((item.weightGrams - 25.0).coerceAtLeast(10.0)) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }

                    Text(
                        text = "${item.weightGrams.roundToInt()} g",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.widthIn(min = 60.dp),
                        textAlign = TextAlign.Center
                    )

                    FilledTonalButton(
                        onClick = { onWeightChanged(item.weightGrams + 25.0) },
                        shape = CircleShape,
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }

                // Quick preset tags (50g, 100g, 200g)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(50, 100, 200).forEach { presetGrams ->
                        FilterChip(
                            selected = item.weightGrams.roundToInt() == presetGrams,
                            onClick = { onWeightChanged(presetGrams.toDouble()) },
                            label = { Text("${presetGrams}g", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 8. ADD FOOD BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddFoodBottomSheet(
    onAddFood: (String, Double) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedGrams by remember { mutableStateOf(100.0) }

    val verifiedFoods = remember {
        listOf(
            "Boiled Egg" to 50.0,
            "Cucumber Raita" to 80.0,
            "Whole Wheat Roti" to 40.0,
            "Fresh Green Salad" to 50.0,
            "Grilled Chicken Breast" to 150.0,
            "Steamed Basmati Rice" to 150.0,
            "Paneer Bhurji" to 100.0,
            "Yellow Dal Tadka" to 150.0,
            "Greek Yogurt" to 150.0,
            "Avocado Slices" to 50.0
        )
    }

    val filteredList = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            verifiedFoods
        } else {
            verifiedFoods.filter { it.first.contains(searchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Add Food to Meal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search food or side dish...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_food_search_input"),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Verified Nutrition Database Options",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { (foodName, defaultGrams) ->
                    val resolved = NutritionDatabase.resolveNutrition(foodName)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddFood(foodName, defaultGrams)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = foodName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Serving: ${defaultGrams.roundToInt()}g · ~${((defaultGrams / 100.0) * resolved.caloriesPer100g).roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { onAddFood(foodName, defaultGrams) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text("+ Add")
                            }
                        }
                    }
                }

                // If user entered a custom food not in list
                if (searchQuery.isNotBlank() && filteredList.none { it.first.equals(searchQuery, ignoreCase = true) }) {
                    item {
                        Button(
                            onClick = { onAddFood(searchQuery.trim(), 100.0) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Text("Add \"${searchQuery.trim()}\" (100g)")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. ASK AI ABOUT THIS MEAL BOTTOM SHEET
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AskAiBottomSheet(
    meal: MealAnalysisResult?,
    chatMessages: List<ChatMessage>,
    questionInput: String,
    isAsking: Boolean,
    onQuestionChange: (String) -> Unit,
    onSendQuestion: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val total = meal?.total

    val promptSuggestions = remember {
        listOf(
            "Why is this high in calories?",
            "Is this good after a workout?",
            "What contributes most to fat?",
            "Is this enough protein for lunch?",
            "What could I add to make this healthier?"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Ask AI About This Meal",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (total != null) {
                        Text(
                            text = "Context: ${total.calories} kcal · ${total.protein}P · ${total.carbs}C · ${total.fat}F",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Horizontal Prompt Suggestions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                promptSuggestions.forEach { suggestion ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onSendQuestion(suggestion) }
                    ) {
                        Text(
                            text = suggestion,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chat Message List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                if (isAsking) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "AI is reasoning about your meal...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = questionInput,
                    onValueChange = onQuestionChange,
                    placeholder = { Text("Ask about calories, macros, substitutions...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ask_ai_input_field"),
                    shape = RoundedCornerShape(20.dp),
                    maxLines = 3
                )

                IconButton(
                    onClick = { onSendQuestion(questionInput) },
                    enabled = questionInput.isNotBlank() && !isAsking,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            if (questionInput.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        )
                        .testTag("ask_ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send question",
                        tint = if (questionInput.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

// ==========================================
// 10. DEVELOPER DETAILS DIALOGS (RAW JSON & DB)
// ==========================================

@Composable
private fun RawJsonDialog(
    jsonText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Code, contentDescription = null)
                Text("Meal Analysis JSON")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .verticalScroll(rememberScrollState())
                    .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = jsonText,
                    color = Color(0xFFF8FAFC),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Meal Analysis JSON", jsonText))
                    Toast.makeText(context, "Copied JSON to clipboard", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy JSON")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun NutritionDatabaseDialog(
    onDismiss: () -> Unit,
    onSelectItem: (com.example.data.model.NutritionReference) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val allItems = remember { NutritionDatabase.getAllReferences() }

    val filtered = remember(query) {
        if (query.isBlank()) allItems
        else allItems.filter { it.displayName.contains(query, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nutrition Database Explorer") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search database...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered) { ref ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectItem(ref) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = ref.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${ref.caloriesPer100g.roundToInt()} kcal/100g · P:${ref.proteinPer100g}g C:${ref.carbsPer100g}g F:${ref.fatPer100g}g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

// ==========================================
// 11. API KEY SETTINGS DIALOG
// ==========================================

@Composable
private fun ApiKeySettingsDialog(
    currentMaskedKey: String,
    isConfigured: Boolean,
    isTesting: Boolean,
    isStartup: Boolean,
    onSaveKey: (String, (Boolean, String) -> Unit) -> Unit,
    onClearKey: () -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Key, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(if (isStartup) "Welcome to Food Calorie AI" else "Google AI Studio API Key")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isStartup) {
                        "To analyze custom food photos in real-time with Gemini 3.8 Flash, enter your Google AI Studio API key. (Sample meals work offline immediately!)"
                    } else {
                        "Enter your Google AI Studio API key to enable Gemini 3.8 Flash vision and conversational nutrition analysis."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isConfigured) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFDCFCE7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                            Text(
                                text = "Active Key: $currentMaskedKey",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Paste AI Studio API Key (AIzaSy...)") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusText != null) {
                    Text(
                        text = statusText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveKey(keyInput) { success, msg ->
                        statusText = msg
                        if (success) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                enabled = keyInput.isNotBlank() && !isTesting
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Save & Validate")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isStartup) "Use Sample Meals" else "Close")
            }
        }
    )
}

// ==========================================
// 12. EMPTY OR INITIAL STATE VIEW
// ==========================================

@Composable
private fun EmptyOrLoadingView(
    analysisState: AnalysisUiState,
    onPickImage: () -> Unit,
    onUseCamera: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (analysisState) {
            is AnalysisUiState.Analyzing -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Text(
                        text = analysisState.stepMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
            is AnalysisUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = analysisState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onPickImage) { Text("Try Another Photo") }
                        OutlinedButton(onClick = onUseCamera) { Text("Camera") }
                    }
                }
            }
            else -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Text(
                        text = "Upload or photograph a meal to get started",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onPickImage) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Choose Photo")
                        }
                        OutlinedButton(onClick = onUseCamera) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera")
                        }
                    }
                }
            }
        }
    }
}
