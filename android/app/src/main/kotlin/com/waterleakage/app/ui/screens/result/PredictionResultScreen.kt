package com.waterleakage.app.ui.screens.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import com.waterleakage.app.data.model.ReadingResponse
import com.waterleakage.app.ui.components.LeakBadge
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
                title = { Text("Prediction Results", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadReading() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh")
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
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryCyan)
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
                    Button(onClick = { viewModel.loadReading() }) { Text("Retry") }
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
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(2.dp, heroBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = if (isLeak) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = heroColor,
                            modifier = Modifier.size(56.dp)
                        )

                        Text(
                            text = if (isLeak) "LEAK DETECTED" else "NO LEAK DETECTED",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = heroColor,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = "Pipe ID: ${r.pipeId}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Large Probability Gauge
                        ProbabilityGauge(probability = r.averageProbability)

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isLeak)
                                "Average probability is ≥ 50%. The models indicate an abnormal pressure fluctuation."
                            else
                                "Average probability is < 50%. System pressure is within expected nominal operating limits.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                // Four Individual Models Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
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
                                text = "4-Model Individual Breakdown",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Surface(
                                color = SurfaceVariantDark,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Avg: ${String.format("%.1f%%", r.averageProbability * 100)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = heroColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Divider(color = SurfaceBorder)

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
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(16.dp),
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

                        InfoRow("Pressure", "${r.pressure} bar")
                        InfoRow("Refill Status", if (r.refill == 1) "Active (1)" else "Inactive (0)")
                        InfoRow("Coordinates", "${r.latitude}, ${r.longitude}")
                        InfoRow("Reading Timestamp", r.readingTimestamp)
                        InfoRow("Saved to SQLite", "ID #${r.id} at ${r.createdAt.take(19)}")
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
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View History")
                    }

                    Button(
                        onClick = onAddAnother,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan, contentColor = Color(0xFF001F2A))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Next")
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
                color = TextSecondary
            )
        }

        Surface(
            color = if (isLeak) StatusLeakRedContainer else StatusNormalGreenContainer,
            border = BorderStroke(1.dp, if (isLeak) StatusLeakRedBorder else StatusNormalGreenBorder),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = pred,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

