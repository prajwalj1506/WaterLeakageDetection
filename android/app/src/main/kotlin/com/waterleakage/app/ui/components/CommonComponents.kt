package com.waterleakage.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.waterleakage.app.ui.theme.*

@Composable
fun LeakBadge(
    prediction: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    val isLeak = prediction.equals("Leak", ignoreCase = true)
    val bgColor = if (isLeak) StatusLeakRedContainer else StatusNormalGreenContainer
    val borderColor = if (isLeak) StatusLeakRedBorder else StatusNormalGreenBorder
    val contentColor = if (isLeak) StatusLeakRed else StatusNormalGreen
    val icon = if (isLeak) Icons.Default.Warning else Icons.Default.CheckCircle
    val text = if (isLeak) "LEAK DETECTED" else "NO LEAK"

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(if (large) 12.dp else 8.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (large) 14.dp else 10.dp,
                vertical = if (large) 8.dp else 4.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (large) 20.dp else 14.dp)
            )
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (large) 15.sp else 12.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ProbabilityGauge(
    probability: Double,
    modifier: Modifier = Modifier
) {
    val pct = (probability * 100).coerceIn(0.0, 100.0)
    val isLeak = probability >= 0.5
    val barColor = if (isLeak) StatusLeakRed else StatusNormalGreen

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Leak Probability",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Text(
                text = String.format("%.1f%%", pct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(SurfaceVariantDark)
        ) {
            // Threshold line at 50%
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.Center)
                    .background(Color.White.copy(alpha = 0.3f))
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (pct / 100.0).toFloat())
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0% (Safe)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = "50% Threshold", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(text = "100% (Critical)", style = MaterialTheme.typography.labelSmall, color = TextMuted)
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    color: Color = PrimaryCyan,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
        }
    }
}

