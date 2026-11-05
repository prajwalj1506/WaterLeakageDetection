package com.waterleakage.app.ui.screens.add

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waterleakage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReadingScreen(
    viewModel: AddReadingViewModel,
    onNavigateBack: () -> Unit,
    onReadingSubmitted: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.fetchCurrentLocation()
        }
    }

    // Trigger navigation when reading is successfully submitted
    LaunchedEffect(state.submittedReading) {
        state.submittedReading?.let { reading ->
            onReadingSubmitted(reading.id)
            viewModel.resetForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Pipe Reading", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Intro hint
            Text(
                text = "Enter sensor values below. The four ML models will evaluate pressure and refill status to detect potential pipeline leaks.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            // Error Banner
            if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = StatusLeakRedContainer),
                    border = BorderStroke(1.dp, StatusLeakRedBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusLeakRed)
                        Text(
                            text = state.errorMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = StatusLeakRed
                        )
                    }
                }
            }

            // Primary Sensor Features Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. Sensor Input Features (Used by ML)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryCyan
                    )

                    OutlinedTextField(
                        value = state.pipeId,
                        onValueChange = { viewModel.onPipeIdChanged(it) },
                        label = { Text("Pipe / Tank Identifier") },
                        placeholder = { Text("e.g. PIPE-104") },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = state.pressure,
                        onValueChange = { viewModel.onPressureChanged(it) },
                        label = { Text("Water Pressure (bar)") },
                        placeholder = { Text("e.g. 52.3 (Normal: 45–65)") },
                        leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        supportingText = {
                            Text("Normal pressure is 45-65. Values <45 or >65 frequently indicate leaks.")
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Refill Switch Toggle
                    Surface(
                        color = SurfaceVariantDark,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Refill Status: ${if (state.refill == 1) "Active (1)" else "Inactive (0)"}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (state.refill == 1) "Tank is currently undergoing refill" else "Normal operation without active refill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                            Switch(
                                checked = state.refill == 1,
                                onCheckedChange = { checked -> viewModel.onRefillChanged(if (checked) 1 else 0) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryCyan,
                                    checkedTrackColor = PrimaryCyanVariant
                                )
                            )
                        }
                    }
                }
            }

            // Location & Timestamp Metadata Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "2. Metadata & Inspection Location",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = SecondaryIndigo
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = state.latitude,
                            onValueChange = { viewModel.onLatitudeChanged(it) },
                            label = { Text("Latitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = state.longitude,
                            onValueChange = { viewModel.onLongitudeChanged(it) },
                            label = { Text("Longitude") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (state.isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Acquiring GPS fix...")
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Current GPS Coordinates")
                        }
                    }

                    // Timestamp editor
                    OutlinedTextField(
                        value = state.timestamp,
                        onValueChange = { viewModel.onTimestampChanged(it) },
                        label = { Text("Reading Timestamp (ISO 8601)") },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.updateTimestampToNow() }) {
                                Icon(Icons.Default.Update, contentDescription = "Set to Now")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Submit Button
            Button(
                onClick = { viewModel.submitReading() },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryCyan,
                    contentColor = Color(0xFF001F2A)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF001F2A),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Running 4-Model ML Inference...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Icon(Icons.Default.Analytics, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Run ML Leak Prediction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
