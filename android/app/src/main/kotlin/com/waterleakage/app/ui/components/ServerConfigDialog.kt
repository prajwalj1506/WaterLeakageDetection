package com.waterleakage.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cable
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waterleakage.app.data.model.ApiResult
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Connect automatically over Wi-Fi, Mobile Hotspot, USB Cable, or Android Emulator.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Instant Auto-Detect Button (discovers Wi-Fi LAN, Hotspot, USB & Emulator)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isAutoDetecting = true
                            statusMessage = "Scanning Wi-Fi, Hotspot, USB & Emulator..."
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
                                statusMessage = "Server not found automatically. Enter the LAN URL printed by run_backend.bat or switch to Mobile Hotspot if college Wi-Fi isolates devices."
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
                        Text("Scanning Network...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    } else {
                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Detect Active Server", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                HorizontalDivider(color = SurfaceBorder)

                // Quick Presets
                Text(
                    text = "QUICK PRESETS",
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
                        onClick = {
                            urlText = ApiClient.HOTSPOT_BASE_URL
                            statusMessage = null
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (urlText == ApiClient.HOTSPOT_BASE_URL) SkyBlueLight else SurfaceWhite,
                            contentColor = if (urlText == ApiClient.HOTSPOT_BASE_URL) SkyBlueDark else TextPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.WifiTethering, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("PC Hotspot", fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            Text(".137.1", fontSize = 9.sp, color = TextMuted)
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            urlText = ApiClient.USB_BASE_URL
                            statusMessage = null
                        },
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
                        onClick = {
                            urlText = ApiClient.EMULATOR_BASE_URL
                            statusMessage = null
                        },
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

                // Custom IP / URL Input
                OutlinedTextField(
                    value = urlText,
                    onValueChange = {
                        urlText = it
                        statusMessage = null
                    },
                    label = { Text("Server IP or Base URL") },
                    placeholder = { Text("e.g. 10.17.4.111 or http://192.168.1.5:8000/") },
                    supportingText = {
                        Text(
                            text = "Enter the PC IP shown by run_backend.bat (port 8000 is added automatically).",
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    },
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
                        val repo = WaterLeakageRepository(context)
                        val result = repo.testAndSaveUrl(urlText)
                        isChecking = false
                        when (result) {
                            is ApiResult.Success -> {
                                urlText = result.data
                                Toast.makeText(context, "Connected to ${result.data}", Toast.LENGTH_SHORT).show()
                                onSaved()
                                onDismiss()
                            }
                            is ApiResult.Error -> {
                                statusMessage = result.message
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
