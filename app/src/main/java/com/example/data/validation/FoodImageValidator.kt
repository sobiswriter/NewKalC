package com.example.data.validation

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.FaceDetector
import com.example.data.model.AmbiguousFoodException
import com.example.data.model.NonFoodException
import kotlin.math.abs
import kotlin.math.sqrt

sealed interface ValidationOutcome {
    object Valid : ValidationOutcome
    data class Rejected(val message: String, val reason: String) : ValidationOutcome
    data class Ambiguous(val message: String, val confidence: Double) : ValidationOutcome
}

object FoodImageValidator {

    /**
     * Fast on-device heuristic validation gate that runs before any network/API call.
     * Rejects human portraits/selfies, blank/solid images, and unusable corrupted images.
     */
    fun preValidateBitmap(bitmap: Bitmap): ValidationOutcome {
        // 1. Check dimensions
        if (bitmap.width < 50 || bitmap.height < 50) {
            return ValidationOutcome.Rejected(
                message = "Please provide a clear image of food or a meal to analyze.",
                reason = "Image resolution is too low."
            )
        }

        // 2. Check for blank or solid color images (e.g. solid black/white screenshot)
        if (isSolidOrBlankImage(bitmap)) {
            return ValidationOutcome.Rejected(
                message = "No recognizable food was found in this image.",
                reason = "Blank or uniform image detected."
            )
        }

        // 3. Human Face Detection (Mandatory Food Validation Gate rejection)
        if (detectHumanFaces(bitmap) > 0) {
            return ValidationOutcome.Rejected(
                message = "Please provide a clear image of food or a meal to analyze.",
                reason = "Human photo detected — no food recognized."
            )
        }

        return ValidationOutcome.Valid
    }

    /**
     * Uses Android's built-in FaceDetector to identify human portraits/faces.
     */
    private fun detectHumanFaces(bitmap: Bitmap): Int {
        return try {
            val maxDim = 512
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val (w, h) = if (ratio > 1f) maxDim to (maxDim / ratio).toInt() else (maxDim * ratio).toInt() to maxDim
                Bitmap.createScaledBitmap(bitmap, w.coerceAtLeast(2), h.coerceAtLeast(2), true)
            } else {
                bitmap
            }

            // FaceDetector requires even width and RGB_565 config
            val evenWidth = if (scaled.width % 2 != 0) scaled.width - 1 else scaled.width
            val evenHeight = scaled.height
            if (evenWidth < 16 || evenHeight < 16) return 0

            val rgb565 = Bitmap.createBitmap(evenWidth, evenHeight, Bitmap.Config.RGB_565)
            val canvas = Canvas(rgb565)
            canvas.drawBitmap(scaled, 0f, 0f, null)

            val detector = FaceDetector(evenWidth, evenHeight, 2)
            val faces = arrayOfNulls<FaceDetector.Face>(2)
            val count = detector.findFaces(rgb565, faces)
            count
        } catch (_: Throwable) {
            0
        }
    }

    /**
     * Checks if image is basically a solid color (e.g. black screen, white page, uniform solid background).
     */
    private fun isSolidOrBlankImage(bitmap: Bitmap): Boolean {
        return try {
            val samples = 16
            val stepX = (bitmap.width / (samples + 1)).coerceAtLeast(1)
            val stepY = (bitmap.height / (samples + 1)).coerceAtLeast(1)

            val colors = mutableListOf<Int>()
            for (i in 1..samples) {
                for (j in 1..samples) {
                    val x = (i * stepX).coerceIn(0, bitmap.width - 1)
                    val y = (j * stepY).coerceIn(0, bitmap.height - 1)
                    colors.add(bitmap.getPixel(x, y))
                }
            }

            if (colors.isEmpty()) return true

            var sumR = 0.0
            var sumG = 0.0
            var sumB = 0.0
            colors.forEach { c ->
                sumR += Color.red(c)
                sumG += Color.green(c)
                sumB += Color.blue(c)
            }
            val avgR = sumR / colors.size
            val avgG = sumG / colors.size
            val avgB = sumB / colors.size

            var variance = 0.0
            colors.forEach { c ->
                val dr = Color.red(c) - avgR
                val dg = Color.green(c) - avgG
                val db = Color.blue(c) - avgB
                variance += (dr * dr + dg * dg + db * db)
            }
            variance /= colors.size

            // If standard deviation across pixels is under 15, it's essentially a blank/solid surface
            val stdDev = sqrt(variance)
            stdDev < 15.0
        } catch (_: Throwable) {
            false
        }
    }
}
