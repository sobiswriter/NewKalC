package com.example.ui

import android.graphics.Bitmap
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MealAnalysisResult
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CollapsibleMealHeader(
    meal: MealAnalysisResult,
    isCollapsed: Boolean,
    imageBitmap: Bitmap?,
    imageResId: Int?,
    title: String,
    onAdjustClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val total = meal.total
    Column(
        modifier = Modifier.fillMaxWidth().animateContentSize().testTag("analysis_header_card"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!isCollapsed) {
            Box(
                modifier = Modifier.fillMaxWidth().height(208.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClickLabel = "Inspect meal photo", onClick = onImageClick)
                    .testTag("meal_image_expanded_box")
            ) {
                if (imageBitmap != null) {
                    Image(imageBitmap.asImageBitmap(), "Meal photograph", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else if (imageResId != null) {
                    Image(painterResource(imageResId), "Sample meal photograph", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp)
                ) {
                    Text(
                        if (imageResId != null) "SAMPLE MEAL" else "YOUR MEAL",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = CircleShape, color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, null, modifier = Modifier.padding(10.dp).size(20.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ON YOUR PLATE", style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("ESTIMATED ENERGY", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(total.calories.toString(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("kcal", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(top = 18.dp))
                        }
                    }
                    TextButton(onClick = onAdjustClick, modifier = Modifier.testTag(if (isCollapsed) "collapsed_adjust_button" else "hero_adjust_button")) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Adjust")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.18f))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MealMacro("Protein", total.protein)
                    MealMacro("Carbs", total.carbs)
                    MealMacro("Fat", total.fat)
                }
            }
        }
        if (!isCollapsed) {
            Text(
                "${(meal.overallConfidence * 100).roundToInt()}% recognition confidence · Portions are estimates",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (meal.uncertaintyExplanation.isNotBlank()) {
                Text(meal.uncertaintyExplanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MealMacro(label: String, grams: Double) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("${grams}g", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}
