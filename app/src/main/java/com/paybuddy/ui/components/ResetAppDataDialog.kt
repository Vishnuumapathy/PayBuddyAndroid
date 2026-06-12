package com.paybuddy.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ResetAppDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean
) {
    var resetText by remember { mutableStateOf("") }
    val isConfirmEnabled = resetText == "RESET" && !isLoading

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reset App Data",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "This will permanently delete all your PayBuddy data (Customers, Sales, Payments, etc.). This action CANNOT be undone.",
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Type RESET to confirm:",
                    color = Color(0xFFFB7185),
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = resetText,
                    onValueChange = { resetText = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text("RESET", color = Color.Gray) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFB7185),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(top = 16.dp),
                        color = Color(0xFFFB7185)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = isConfirmEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFB7185),
                    contentColor = Color.White,
                    disabledContainerColor = Color.Gray
                )
            ) {
                Text("Confirm Reset")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}
