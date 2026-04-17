package com.guardian.track.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.ui.theme.*

@Composable
fun IncidentCard(
    incident: Incident,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (incident.type) {
        IncidentType.FALL -> Triple(Icons.Default.Warning, FallColor, "Fall Detected")
        IncidentType.BATTERY -> Triple(Icons.Default.BatteryAlert, BatteryColor, "Battery Critical")
        IncidentType.MANUAL -> Triple(Icons.Default.TouchApp, ManualColor, "Manual Alert")
    }
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f))
                .drawBehind {
                    drawCircle(
                        color = color.copy(alpha = 0.3f),
                        radius = size.minDimension / 2 + 4f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = incident.formattedDateTime,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (incident.latitude != 0.0 || incident.longitude != 0.0) {
                Text(
                    text = "📍 %.7f, %.7f".format(incident.latitude, incident.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
        }

        GlowingStatusChip(
            label = if (incident.isSynced) "Synced" else "Pending",
            color = if (incident.isSynced) SyncedColor else PendingColor
        )
    }
}

@Composable
fun TimelineIncidentItem(
    incident: Incident,
    isLast: Boolean = false,
    modifier: Modifier = Modifier
) {
    val (icon, color, label) = when (incident.type) {
        IncidentType.FALL -> Triple(Icons.Default.Warning, FallColor, "Fall Detected")
        IncidentType.BATTERY -> Triple(Icons.Default.BatteryAlert, BatteryColor, "Battery Critical")
        IncidentType.MANUAL -> Triple(Icons.Default.TouchApp, ManualColor, "Manual Alert")
    }
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(color)
                    .drawBehind {
                        drawCircle(
                            color = color.copy(alpha = 0.4f),
                            radius = size.minDimension / 2 + 6f
                        )
                    }
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(56.dp)
                        .background(colors.outline)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                GlowingStatusChip(
                    label = if (incident.isSynced) "Synced" else "Pending",
                    color = if (incident.isSynced) SyncedColor else PendingColor
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = incident.formattedDateTime,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
            if (incident.latitude != 0.0 || incident.longitude != 0.0) {
                Text(
                    text = "📍 %.7f, %.7f".format(incident.latitude, incident.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
