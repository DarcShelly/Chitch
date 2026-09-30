package com.chitch.app.ui.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * A full-bleed photo with a bottom-weighted dark scrim, meant to sit as the first
 * child in a Box so text drawn after it stays legible regardless of the photo.
 * Caller must give its container an explicit size (this fills whatever it's given).
 */
@Composable
fun ImageBackdrop(imageUri: String) {
    AsyncImage(
        model = Uri.parse(imageUri),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.10f),
                    1f to Color.Black.copy(alpha = 0.72f)
                )
            )
    )
}
