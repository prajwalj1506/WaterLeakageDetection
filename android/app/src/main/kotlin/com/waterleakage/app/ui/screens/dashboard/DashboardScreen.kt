package com.waterleakage.app.ui.screens.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waterleakage.app.data.model.ReadingResponse
import com.waterleakage.app.ui.components.LeakBadge
import com.waterleakage.app.ui.components.MetricStatCard
import com.waterleakage.app.ui.components.ServerConfigDialog
import com.waterleakage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToDetails: (Int) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var showServerDialog by remember { mutableStateOf(false) }

    if (showServerDialog) {
        ServerConfigDialog(
            onDismiss = { showServerDialog = false },
            onSaved = { viewModel.loadDashboard() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Water Leakage Detection",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "4-Model Ensemble Pipeline",
                            style = MaterialTheme.typography.labelSmall,
                            color = SkyBluePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary
                        )
                    }
                    IconButton(onClick = { showServerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server Settings",
                            tint = if (state.isServerOnline) StatusNormalGreen else StatusWarning
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Reading", fontWeight = FontWeight.SemiBold) },
                containerColor = SkyBluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(14.dp)
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Server Connection Banner
            item {
                ServerStatusBanner(
                    isOnline = state.isServerOnline,
                    connectionType = state.connectionType,
                    activeUrl = state.activeUrl,
                    isAutoConnecting = state.isAutoConnecting,
                    onAutoConnectClick = { viewModel.autoConnect() },
                    onConfigureClick = { showServerDialog = true }
                )
            }

            // Error banner if any
            if (state.errorMessage != null) {
                item {
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
                            Column {
                                Text("Backend Connection Error", fontWeight = FontWeight.Bold, color = StatusLeakRed)
                                Text(state.errorMessage!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Metric Stat Cards Grid
            item {
                val summary = state.summary
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Total Readings",
                            value = summary?.totalReadings?.toString() ?: "0",
                            subtitle = "Logged tests",
                            accentColor = SkyBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "Leaks Detected",
                            value = summary?.leaksDetected?.toString() ?: "0",
                            subtitle = if (summary != null) "${summary.leakRatePercentage}% leak rate" else "0%",
                            accentColor = StatusLeakRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "Normal Pipes",
                            value = summary?.normalReadings?.toString() ?: "0",
                            subtitle = "No leak condition",
                            accentColor = StatusNormalGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "ML Models",
                            value = "4 Models",
                            subtitle = "LR, DT, RF, SVM",
                            accentColor = SkyBlueDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Reading History Quick Action Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reading History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Browse and filter all stored pipe analyses",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        FilledTonalButton(
                            onClick = onNavigateToHistory,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SkyBlueLight,
                                contentColor = SkyBlueDark
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("View All", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Recent Readings Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Predictions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text("See More", color = SkyBluePrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Recent Readings List
            val recentList = state.summary?.recentReadings ?: emptyList()
            if (recentList.isEmpty() && !state.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SurfaceBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDamage,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No readings logged yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap 'New Reading' below to enter pipe sensor values.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(recentList) { reading ->
                    RecentReadingItem(
                        reading = reading,
                        onClick = { onNavigateToDetails(reading.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
            }
        }
    }
}

@Composable
fun ServerStatusBanner(
    isOnline: Boolean,
    connectionType: String,
    activeUrl: String,
    isAutoConnecting: Boolean,
    onAutoConnectClick: () -> Unit,
    onConfigureClick: () -> Unit
) {
    Surface(
        color = SurfaceWhite,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isOnline) StatusNormalGreen else StatusLeakRed,
                    modifier = Modifier.size(10.dp)
                ) {}
                Column {
                    Text(
                        text = if (isOnline) "Backend Online ($connectionType)" else "Backend Offline",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isOnline) StatusNormalGreen else StatusLeakRed
                    )
                    Text(
                        text = activeUrl,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!isOnline) {
                    FilledTonalButton(
                        onClick = onAutoConnectClick,
                        enabled = !isAutoConnecting,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = SkyBlueLight,
                            contentColor = SkyBlueDark
                        )
                    ) {
                        if (isAutoConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(12.dp), strokeWidth = 1.5.dp, color = SkyBlueDark)
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text("Auto-Connect", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(
                    onClick = onConfigureClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Change", fontSize = 12.sp, color = SkyBluePrimary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RecentReadingItem(
    reading: ReadingResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = reading.pipeId,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Pressure: ${reading.pressure} bar  |  Refill: ${if (reading.refill == 1) "Active" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Probability: ${String.format("%.1f%%", reading.averageProbability * 100)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reading.averageProbability >= 0.5) StatusLeakRed else StatusNormalGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LeakBadge(prediction = reading.finalPrediction)
        }
    }
}
