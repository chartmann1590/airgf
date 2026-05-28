package com.airgf.app.core.util

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.roundToInt

object BitmapDescriber {

    fun describe(bitmap: Bitmap): String {
        val width = bitmap.width
        val height = bitmap.height
        val orientation = when {
            width > height * 1.2 -> "landscape"
            height > width * 1.2 -> "portrait"
            else -> "square"
        }
        val dominantColors = dominantColorNames(bitmap)
        val brightness = averageBrightness(bitmap)
        val brightnessLabel = when {
            brightness < 0.3f -> "dark/low-light"
            brightness > 0.7f -> "bright/well-lit"
            else -> "moderately lit"
        }
        val colorVariance = colorVariance(bitmap)
        val contentHint = when {
            colorVariance < 0.02f -> "a solid or mostly uniform color"
            colorVariance < 0.08f -> "low detail, possibly a simple graphic or close-up"
            else -> "a detailed photograph or complex image"
        }

        return buildString {
            append("The user shared a $orientation photo (${width}x${height}). ")
            append("It appears $brightnessLabel and looks like $contentHint. ")
            append("Dominant colors: ${dominantColors.joinToString(", ")}.")
        }
    }

    private fun averageBrightness(bitmap: Bitmap): Float {
        val step = maxOf(1, (bitmap.width * bitmap.height) / 2000)
        var total = 0f
        var count = 0
        for (y in 0 until bitmap.height step maxOf(1, bitmap.height / 40)) {
            for (x in 0 until bitmap.width step maxOf(1, bitmap.width / 40)) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                total += 0.299f * r + 0.587f * g + 0.114f * b
                count++
            }
        }
        return if (count > 0) total / count else 0.5f
    }

    private fun colorVariance(bitmap: Bitmap): Float {
        var sumR = 0f; var sumG = 0f; var sumB = 0f
        var sumR2 = 0f; var sumG2 = 0f; var sumB2 = 0f
        var count = 0
        for (y in 0 until bitmap.height step maxOf(1, bitmap.height / 30)) {
            for (x in 0 until bitmap.width step maxOf(1, bitmap.width / 30)) {
                val pixel = bitmap.getPixel(x, y)
                val r = Color.red(pixel) / 255f
                val g = Color.green(pixel) / 255f
                val b = Color.blue(pixel) / 255f
                sumR += r; sumG += g; sumB += b
                sumR2 += r * r; sumG2 += g * g; sumB2 += b * b
                count++
            }
        }
        if (count == 0) return 0f
        val varR = sumR2 / count - (sumR / count) * (sumR / count)
        val varG = sumG2 / count - (sumG / count) * (sumG / count)
        val varB = sumB2 / count - (sumB / count) * (sumB / count)
        return (varR + varG + varB) / 3f
    }

    private fun dominantColorNames(bitmap: Bitmap): List<String> {
        val buckets = mutableMapOf<String, Int>()
        for (y in 0 until bitmap.height step maxOf(1, bitmap.height / 30)) {
            for (x in 0 until bitmap.width step maxOf(1, bitmap.width / 30)) {
                val pixel = bitmap.getPixel(x, y)
                val name = colorName(Color.red(pixel), Color.green(pixel), Color.blue(pixel))
                buckets[name] = (buckets[name] ?: 0) + 1
            }
        }
        return buckets.entries.sortedByDescending { it.value }.take(3).map { it.key }
    }

    private fun colorName(r: Int, g: Int, b: Int): String {
        val brightness = (0.299 * r + 0.587 * g + 0.114 * b)
        if (brightness < 30) return "black"
        if (brightness > 225 && maxOf(r, g, b) - minOf(r, g, b) < 30) return "white"
        if (maxOf(r, g, b) - minOf(r, g, b) < 25) return "gray"

        val max = maxOf(r, g, b)
        return when (max) {
            r -> when {
                g > b + 40 && g > 100 -> "yellow/orange"
                g > b + 20 -> "orange"
                b > g + 20 -> "pink/magenta"
                else -> "red"
            }
            g -> when {
                b > r + 20 -> "teal/cyan"
                r > b + 20 -> "yellow-green"
                else -> "green"
            }
            b -> when {
                r > g + 20 -> "purple"
                g > r + 20 -> "cyan"
                else -> "blue"
            }
            else -> "mixed"
        }
    }
}
