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
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "4-Model ML Probability Ensemble",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryCyan
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadDashboard() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { showServerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Server Settings",
                            tint = if (state.isServerOnline) StatusNormalGreen else StatusWarningYellow
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Reading", fontWeight = FontWeight.SemiBold) },
                containerColor = PrimaryCyan,
                contentColor = Color(0xFF001F2A)
            )
        },
        containerColor = BackgroundDark
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
                            title = "TOTAL READINGS",
                            value = summary?.totalReadings?.toString() ?: "0",
                            subtitle = "Logged pipe tests",
                            color = PrimaryCyan,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "LEAKS DETECTED",
                            value = summary?.leaksDetected?.toString() ?: "0",
                            subtitle = if (summary != null) "${summary.leakRatePercentage}% leak rate" else "0%",
                            color = StatusLeakRed,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricStatCard(
                            title = "NORMAL PIPES",
                            value = summary?.normalReadings?.toString() ?: "0",
                            subtitle = "No leak condition",
                            color = StatusNormalGreen,
                            modifier = Modifier.weight(1f)
                        )
                        MetricStatCard(
                            title = "ML MODELS",
                            value = "4 Models",
                            subtitle = "LR, DT, RF, SVM",
                            color = SecondaryIndigo,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Quick Actions Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reading History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Browse and filter all stored pipe analyses",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        FilledTonalButton(
                            onClick = onNavigateToHistory,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = SurfaceVariantDark,
                                contentColor = PrimaryCyan
                            )
                        ) {
                            Text("View All")
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
                        Text("See More", color = PrimaryCyan)
                    }
                }
            }

            // Recent Readings List
            val recentList = state.summary?.recentReadings ?: emptyList()
            if (recentList.isEmpty() && !state.isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                        shape = RoundedCornerShape(16.dp),
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
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No readings logged yet",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap '+ New Reading' below to enter pipe sensor values.",
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
    onConfigureClick: () -> Unit
) {
    Surface(
        color = SurfaceDark,
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isOnline) StatusNormalGreen else StatusLeakRed,
                    modifier = Modifier.size(10.dp)
                ) {}
                Text(
                    text = if (isOnline) "Backend Online (FastAPI + SQLite)" else "Backend Offline",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isOnline) StatusNormalGreen else StatusLeakRed
                )
            }
            TextButton(
                onClick = onConfigureClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Change IP", fontSize = 12.sp, color = PrimaryCyan)
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
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(14.dp),
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
                    text = "Pressure: ${reading.pressure} bar • Refill: ${if (reading.refill == 1) "Active" else "Off"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Text(
                    text = "Avg Prob: ${String.format("%.1f%%", reading.averageProbability * 100)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reading.averageProbability >= 0.5) StatusLeakRed else StatusNormalGreen,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LeakBadge(prediction = reading.finalPrediction)
        }
    }
}

