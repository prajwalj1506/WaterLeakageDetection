package com.waterleakage.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WarningAmber
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
    val icon = if (isLeak) Icons.Default.WarningAmber else Icons.Default.Check
    val text = if (isLeak) "LEAK" else "NORMAL"

    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(if (large) 10.dp else 6.dp),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = if (large) 12.dp else 8.dp,
                vertical = if (large) 6.dp else 3.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(if (large) 16.dp else 12.dp)
            )
            Text(
                text = text,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                fontSize = if (large) 13.sp else 11.sp,
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
    val barColor = if (isLeak) StatusLeakRed else SkyBluePrimary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ensemble Probability",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = String.format("%.1f%%", pct),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SurfaceVariantLight)
        ) {
            // 50% Threshold marker
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .align(Alignment.Center)
                    .background(TextMuted.copy(alpha = 0.5f))
            )

            // Probability fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = (pct / 100.0).toFloat())
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "0% Safe", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
            Text(text = "50% Cutoff", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
            Text(text = "100% Leak", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 10.sp)
        }
    }
}

@Composable
fun MetricStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    accentColor: Color = SkyBluePrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}
