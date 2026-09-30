package com.chitch.app.ui.components

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * Image + file attachments, backed by the system document picker (Storage Access
 * Framework) since scoped storage means there's no stable raw filesystem path — a
 * persistable content:// URI is the closest thing to a durable "link to a file on
 * device". See clarification #8 in chat.
 */
@Composable
fun AttachmentsSection(
    imageUri: String?,
    fileUris: List<String>,
    onImagePicked: (String?) -> Unit,
    onFilesAdded: (List<String>) -> Unit,
    onRemoveFile: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            onImagePicked(uri.toString())
        }
    }

    val filesPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        uris.forEach { uri ->
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (uris.isNotEmpty()) onFilesAdded(uris.map { it.toString() })
    }

    Column(modifier = modifier) {
        Text("Background image", style = MaterialTheme.typography.labelLarge)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { imagePicker.launch(arrayOf("image/*")) },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = Uri.parse(imageUri),
                        contentDescription = "Selected image",
                        modifier = Modifier.fillMaxWidth().size(64.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Filled.Image, contentDescription = "Pick an image")
                }
            }
            if (imageUri != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .clickable { onImagePicked(null) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove image",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Text("Files", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(onClick = { filesPicker.launch(arrayOf("*/*")) }) {
                Icon(Icons.Filled.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Add files", modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (fileUris.isNotEmpty()) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                fileUris.forEach { uri ->
                    AssistChip(
                        onClick = {},
                        label = { Text(displayNameFor(context, uri), maxLines = 1) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove file",
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onRemoveFile(uri) }
                            )
                        },
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}

private fun displayNameFor(context: android.content.Context, uriString: String): String {
    return try {
        val uri = Uri.parse(uriString)
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            } ?: uri.lastPathSegment ?: uriString
    } catch (e: Exception) {
        uriString
    }
}
