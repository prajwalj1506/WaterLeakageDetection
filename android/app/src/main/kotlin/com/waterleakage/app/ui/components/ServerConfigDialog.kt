package com.waterleakage.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waterleakage.app.data.remote.ApiClient
import com.waterleakage.app.data.repository.WaterLeakageRepository
import com.waterleakage.app.ui.theme.*
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
    var isAutoDetecting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSuccess by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(16.dp),
        title = {
            Text(
                text = "Server Connection",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "Select your connection mode or let the app probe and connect automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Instant Auto-Detect Button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isAutoDetecting = true
                            statusMessage = "Probing USB, Wi-Fi, and Emulator..."
                            isSuccess = false
                            val detected = ApiClient.autoDetectWorkingServer(context)
                            isAutoDetecting = false
                            if (detected != null) {
                                urlText = detected
                                isSuccess = true
                                statusMessage = "Connected to $detected"
                                Toast.makeText(context, "Connected to $detected", Toast.LENGTH_SHORT).show()
                                onSaved()
                            } else {
                                statusMessage = "Server not reachable. Ensure run_backend.bat is running."
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SkyBluePrimary,
                        contentColor = Color.White
                    ),
                    enabled = !isAutoDetecting && !isChecking
                ) {
                    if (isAutoDetecting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Probing Endpoints...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Detect Active Server", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(color = SurfaceBorder)

                // Quick Presets
                Text(
                    text = "PRESETS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { urlText = ApiClient.USB_BASE_URL },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (urlText == ApiClient.USB_BASE_URL) SkyBlueLight else SurfaceWhite,
                            contentColor = if (urlText == ApiClient.USB_BASE_URL) SkyBlueDark else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Cable, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("USB Cable", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("127.0.0.1", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    OutlinedButton(
                        onClick = { urlText = ApiClient.WIFI_BASE_URL },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (urlText == ApiClient.WIFI_BASE_URL) SkyBlueLight else SurfaceWhite,
                            contentColor = if (urlText == ApiClient.WIFI_BASE_URL) SkyBlueDark else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Wi-Fi LAN", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(".29.251", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    OutlinedButton(
                        onClick = { urlText = ApiClient.EMULATOR_BASE_URL },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (urlText == ApiClient.EMULATOR_BASE_URL) SkyBlueLight else SurfaceWhite,
                            contentColor = if (urlText == ApiClient.EMULATOR_BASE_URL) SkyBlueDark else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Emulator", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text("10.0.2.2", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }

                // Custom URL Input
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        statusMessage = null
                    },
                    label = { Text("Server Base URL") },
                    placeholder = { Text("http://127.0.0.1:8000/") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite,
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedLabelColor = SkyBluePrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (statusMessage != null) {
                    Text(
                        text = statusMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSuccess) StatusNormalGreen else StatusLeakRed,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isChecking) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = SkyBluePrimary,
                        trackColor = SkyBlueLight
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    coroutineScope.launch {
                        isChecking = true
                        statusMessage = "Testing connection..."
                        isSuccess = false
                        ApiClient.setBaseUrl(context, urlText)
                        val repo = WaterLeakageRepository(context)
                        val result = repo.checkHealth()
                        isChecking = false
                        when (result) {
                            is com.waterleakage.app.data.model.ApiResult.Success -> {
                                Toast.makeText(context, "Connected successfully", Toast.LENGTH_SHORT).show()
                                onSaved()
                                onDismiss()
                            }
                            is com.waterleakage.app.data.model.ApiResult.Error -> {
                                statusMessage = "Connection failed: ${result.message}"
                            }
                            else -> {}
                        }
                    }
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SkyBluePrimary,
                    contentColor = Color.White
                ),
                enabled = !isChecking && !isAutoDetecting
            ) {
                Text("Test & Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel")
            }
        }
    )
}
