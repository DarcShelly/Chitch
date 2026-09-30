package com.chitch.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ActiveItemCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    imageUri: String? = null,
    description: String? = null,
    isCompleted: Boolean = false,
    onClick: (() -> Unit)? = null,
    onToggleComplete: (() -> Unit)? = null
) {
    val cardModifier = modifier.fillMaxWidth().height(if (description != null) 132.dp else 112.dp)
    val shape = RoundedCornerShape(20.dp)
    // A background photo needs its own scrim for legible text, so the card itself
    // goes transparent and lets the image + gradient (drawn first, below) show through.
    val effectiveContentColor = if (imageUri != null) Color.White else contentColor
    val colors = if (imageUri != null) {
        CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = effectiveContentColor)
    } else {
        CardDefaults.cardColors(containerColor = containerColor, contentColor = effectiveContentColor)
    }

    val content: @Composable () -> Unit = {
        Box(modifier = Modifier.fillMaxSize()) {
            imageUri?.let { ImageBackdrop(it) }
            Column(
                modifier = Modifier
                    .align(if (imageUri != null) Alignment.BottomStart else Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = if (isCompleted) Icons.Filled.CheckCircle else icon,
                    contentDescription = if (onToggleComplete != null) "Toggle complete" else null,
                    tint = effectiveContentColor,
                    modifier = if (onToggleComplete != null) {
                        Modifier.clickable(onClick = onToggleComplete)
                    } else {
                        Modifier
                    }
                )
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = effectiveContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isCompleted) "Completed" else subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = effectiveContentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = effectiveContentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }

    if (onClick != null) {
        Card(onClick = onClick, modifier = cardModifier, shape = shape, colors = colors) { content() }
    } else {
        Card(modifier = cardModifier, shape = shape, colors = colors) { content() }
    }
}
