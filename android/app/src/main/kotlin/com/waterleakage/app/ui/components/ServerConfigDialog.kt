package com.waterleakage.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.waterleakage.app.data.remote.ApiClient
import com.waterleakage.app.data.repository.WaterLeakageRepository
import com.waterleakage.app.ui.theme.PrimaryCyan
import com.waterleakage.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun ServerConfigDialog(
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var urlText by remember { mutableStateOf(ApiClient.getBaseUrl(context)) }
    var isChecking by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Backend Server") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Specify the FastAPI server address. Use 10.0.2.2 for Android Emulator, or your PC's Wi-Fi IP for physical devices.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                OutlinedTextField(
                    value = urlText,
                    onValueChange = { urlText = it },
                    label = { Text("Server Base URL") },
                    placeholder = { Text("http://10.0.2.2:8000/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { urlText = "http://10.0.2.2:8000/" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Emulator", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { urlText = "http://localhost:8000/" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Localhost", style = MaterialTheme.typography.labelSmall)
                    }
                }

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = PrimaryCyan
                    )
                }

                if (isChecking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isChecking = true
                        statusMessage = "Testing connection..."
                        ApiClient.setBaseUrl(context, urlText)
                        val repo = WaterLeakageRepository(context)
                        val result = repo.checkHealth()
                        isChecking = false
                        when (result) {
                            is com.waterleakage.app.data.model.ApiResult.Success -> {
                                Toast.makeText(context, "Connected to backend successfully!", Toast.LENGTH_SHORT).show()
                                onSaved()
                                onDismiss()
                            }
                            is com.waterleakage.app.data.model.ApiResult.Error -> {
                                statusMessage = "Connection failed: ${result.message}"
                            }
                            else -> {}
                        }
                    }
                }
            ) {
                Text("Test & Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

