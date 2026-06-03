package com.appriyo.amarsavings.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.appriyo.amarsavings.util.formatTaka
import com.appriyo.amarsavings.util.parseTakaAmount
import kotlinx.coroutines.delay

@Composable
fun GoalDialog(
    currentGoal: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember {
        mutableStateOf(if (currentGoal > 0) currentGoal.toString() else "")
    }
    val parsed = inputText.parseTakaAmount()
    val isValid = parsed > 0
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = if (currentGoal > 0) "Update Savings Goal" else "Set Savings Goal",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How much do you want to save?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it.filter { c -> c.isDigit() } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    prefix = { Text("৳", fontWeight = FontWeight.SemiBold) },
                    placeholder = { Text("100,000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    supportingText = if (isValid) {
                        { Text("Goal: ${parsed.formatTaka()}", color = MaterialTheme.colorScheme.primary) }
                    } else null
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(parsed) },
                enabled = isValid,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save Goal", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}