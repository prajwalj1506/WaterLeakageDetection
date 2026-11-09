package com.waterleakage.app.ui.screens.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.waterleakage.app.ui.components.ProbabilityGauge
import com.waterleakage.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionResultScreen(
    viewModel: PredictionResultViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onAddAnother: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Prediction Result",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadReading() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = BackgroundOffWhite
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SkyBluePrimary)
            }
        } else if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = StatusLeakRed, modifier = Modifier.size(48.dp))
                    Text("Failed to load result", style = MaterialTheme.typography.titleMedium, color = StatusLeakRed)
                    Text(state.errorMessage!!, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Button(
                        onClick = { viewModel.loadReading() },
                        colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary)
                    ) {
                        Text("Retry")
                    }
                }
            }
        } else {
            val r = state.reading!!
            val isLeak = r.finalPrediction.equals("Leak", ignoreCase = true)
            val heroBg = if (isLeak) StatusLeakRedContainer else StatusNormalGreenContainer
            val heroBorder = if (isLeak) StatusLeakRedBorder else StatusNormalGreenBorder
            val heroColor = if (isLeak) StatusLeakRed else StatusNormalGreen

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hero Prediction Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = heroBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, heroBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isLeak) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = heroColor,
                            modifier = Modifier.size(52.dp)
                        )

                        Text(
                            text = if (isLeak) "LEAK DETECTED" else "NO LEAK DETECTED",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroColor,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "Pipe ID: ${r.pipeId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Probability Gauge
                        ProbabilityGauge(probability = r.averageProbability)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isLeak)
                                "Average probability is ≥ 50%. The ensemble identifies an anomalous pressure condition."
                            else
                                "Average probability is < 50%. Pipeline pressure is within expected nominal operating limits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Four Individual Models Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Model Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                color = SkyBlueLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Avg: ${String.format("%.1f%%", r.averageProbability * 100)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SkyBlueDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = SurfaceBorder)

                        ModelRow(
                            modelName = "Logistic Regression",
                            prob = r.probLogisticRegression,
                            pred = r.predLogisticRegression
                        )
                        ModelRow(
                            modelName = "Decision Tree",
                            prob = r.probDecisionTree,
                            pred = r.predDecisionTree
                        )
                        ModelRow(
                            modelName = "Random Forest",
                            prob = r.probRandomForest,
                            pred = r.predRandomForest
                        )
                        ModelRow(
                            modelName = "Support Vector Machine",
                            prob = r.probSvm,
                            pred = r.predSvm
                        )
                    }
                }

                // Inspection Input Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Reading Metadata",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        HorizontalDivider(color = SurfaceBorder)

                        InfoRow("Pressure", "${r.pressure} bar")
                        InfoRow("Refill Status", if (r.refill == 1) "Active (1)" else "Inactive (0)")
                        InfoRow("Coordinates", "${r.latitude}, ${r.longitude}")
                        InfoRow("Reading Timestamp", r.readingTimestamp.take(19).replace('T', ' '))
                        InfoRow("Database Row", "#${r.id}")
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToHistory,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SurfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = SurfaceWhite,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View History", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onAddAnother,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBluePrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Next", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ModelRow(
    modelName: String,
    prob: Double,
    pred: String
) {
    val isLeak = pred.equals("Leak", ignoreCase = true)
    val badgeBg = if (isLeak) StatusLeakRedContainer else StatusNormalGreenContainer
    val badgeBorder = if (isLeak) StatusLeakRedBorder else StatusNormalGreenBorder
    val badgeColor = if (isLeak) StatusLeakRed else StatusNormalGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = modelName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )
            Text(
                text = "Probability: ${String.format("%.1f%%", prob * 100)}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Surface(
            color = badgeBg,
            border = BorderStroke(1.dp, badgeBorder),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = if (isLeak) "LEAK" else "NORMAL",
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = TextPrimary)
    }
}
