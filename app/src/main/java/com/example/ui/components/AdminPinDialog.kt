package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun AdminPinDialog(
    correctPin: String,
    onDismiss: () -> Unit,
    onAuthenticated: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    fun checkAndSubmit() {
        if (enteredPin == correctPin) {
            showError = false
            onAuthenticated()
        } else {
            showError = true
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Admin Authentication", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Enter Admin Security PIN to access administrative tools, cash verification, and member management.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 8) {
                            enteredPin = it
                            showError = false
                        }
                    },
                    label = { Text("Admin PIN") },
                    placeholder = { Text("Default: 1234") },
                    singleLine = true,
                    isError = showError,
                    supportingText = {
                        if (showError) {
                            Text(
                                "Incorrect PIN. Default PIN is 1234.",
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Text("Default initial PIN is 1234")
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    keyboardActions = KeyboardActions(onDone = { checkAndSubmit() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { checkAndSubmit() },
                modifier = Modifier.testTag("admin_pin_submit_button")
            ) {
                Text("Unlock Admin Mode")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
