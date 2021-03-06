package dev.shreyansh.tmdb.utils

import android.content.Context
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Scale
import com.github.ajalt.timberkt.e
import com.github.ajalt.timberkt.i
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

@Composable
fun getColorsFromImageOrTheme(
    context: Context = LocalContext.current,
    themeColors: Colors = MaterialTheme.colors,
    backdropUrl: String,
    posterUrl: String
): DominantColors {
    var dominantColor by remember {
        mutableStateOf(
            DominantColors(
                listOf(
                    themeColors.primaryVariant,
                    themeColors.secondary
                ).random(),
                listOf(
                    themeColors.primaryVariant,
                    themeColors.secondary
                ).random(),
                listOf(
                    themeColors.primaryVariant,
                    themeColors.secondary
                ).random(),
                0.0f
            )
        )
    }
    LaunchedEffect(key1 = Unit) {
        calculateSwatchesInImage(context = context, backdropUrl, themeColors.background)?.let {
            dominantColor = it
        }
    }
    return dominantColor
}

suspend fun calculateSwatchesInImage(
    context: Context,
    imageUrl: String,
    surfaceColor: Color
): DominantColors? {
    val r = ImageRequest.Builder(context)
        .data(imageUrl)
        // We scale the image to cover 128px x 128px (i.e. min dimension == 128px)
        .size(128).scale(Scale.FILL)
        // Disable hardware bitmaps, since Palette uses Bitmap.getPixels()
        .allowHardware(false)
        .build()

    val bitmap = when (val result = Coil.execute(r)) {
        is SuccessResult -> result.drawable.toBitmap()
        else -> null
    }

    return bitmap?.let {
        withContext(Dispatchers.Default) {
            val palette = Palette.Builder(bitmap)
                // Disable any bitmap resizing in Palette. We've already loaded an appropriately
                // sized bitmap through Coil
                .resizeBitmapArea(0)
                // Clear any built-in filters. We want the unfiltered dominant color
                .clearFilters()
                // We reduce the maximum color count down to 8
                .maximumColorCount(8)
                .generate()

            var dominantColor: DominantColors? = null
            var c1: Color? = null
            var c2: Color? = null
            var c3: Color? = null
            palette.vibrantSwatch?.let { swatch ->
                if (Color(swatch.rgb).contrastAgainst(surfaceColor) >= ContrastValue) {
                    i { "C1 from vibrant" }
                    c1 = Color(swatch.rgb)
                }
                if (Color(swatch.titleTextColor).contrastAgainst(surfaceColor) >= ContrastValue) {
                    i { "C2 from vibrant" }
                    c2 = Color(swatch.titleTextColor)
                }
                if (Color(swatch.bodyTextColor).contrastAgainst(surfaceColor) >= ContrastValue) {
                    i { "C3 from vibrant" }
                    c3 = Color(swatch.bodyTextColor)
                }
            } ?: palette.swatches
                .sortedByDescending { swatch -> swatch.population }
                // Then we want to find the first valid color
                .firstOrNull { swatch ->
                    Color(swatch.rgb).contrastAgainst(surfaceColor) >= ContrastValue &&
                            Color(swatch.titleTextColor).contrastAgainst(surfaceColor) >= ContrastValue &&
                            Color(swatch.bodyTextColor).contrastAgainst(surfaceColor) >= ContrastValue
                }
                ?.let { swatch ->
                    e { "All colors from other swatches" }
                    c1 = Color(swatch.rgb)
                    c2 = Color(swatch.titleTextColor)
                    c3 = Color(swatch.bodyTextColor)
                }
            if (c1 != null && c2 != null) {
                val validLuminance = when {
                    c1!!.luminance() != 0.0f -> c1!!.luminance()
                    c2!!.luminance() != 0.0f -> c2!!.luminance()
                    else -> c3!!.luminance()
                }
                dominantColor = DominantColors(c1!!, c2!!, c3!!, validLuminance)
            } else
                e { "Using theme colors" }

            dominantColor
        }
    }
}

fun Color.contrastAgainst(background: Color): Float {
    val fg = if (alpha < 1f) compositeOver(background) else this

    val fgLuminance = fg.luminance() + 0.05f
    val bgLuminance = background.luminance() + 0.05f

    return max(fgLuminance, bgLuminance) / min(fgLuminance, bgLuminance)
}

const val ContrastValue = 3f