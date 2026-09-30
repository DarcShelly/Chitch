package com.chitch.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.chitch.app.data.entity.RepeatType

/** Shared repeat-rule picker, used by standalone task, routine task, and habit task forms. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepeatTypeDropdown(
    value: RepeatType,
    onValueChange: (RepeatType) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Repeat"
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            readOnly = true,
            value = repeatTypeLabel(value),
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RepeatType.entries.forEach { repeat ->
                DropdownMenuItem(
                    text = { Text(repeatTypeLabel(repeat)) },
                    onClick = {
                        onValueChange(repeat)
                        expanded = false
                    }
                )
            }
        }
    }
}
