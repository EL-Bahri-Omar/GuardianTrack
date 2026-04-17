package com.guardian.track.ui.history

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardian.track.domain.model.Incident
import com.guardian.track.domain.model.IncidentType
import com.guardian.track.ui.components.GlowingStatusChip
import com.guardian.track.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Fixed Header ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
                if (uiState.incidents.isNotEmpty()) {
                    Text(
                        text = "${uiState.incidents.size} incident(s)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // ── Scrollable Content ──
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colors.primary)
                }
            } else if (uiState.incidents.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = colors.primary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "All Clear",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No incidents recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(
                        items = uiState.incidents,
                        key = { it.id }
                    ) { incident ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteIncident(incident.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    targetValue = when (dismissState.targetValue) {
                                        SwipeToDismissBoxValue.EndToStart -> AlertRed.copy(alpha = 0.2f)
                                        else -> Color.Transparent
                                    },
                                    label = "dismissBg"
                                )
                                val iconScale by animateFloatAsState(
                                    targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0.5f,
                                    label = "iconScale"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = AlertRed,
                                        modifier = Modifier.scale(iconScale)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            content = {
                                MinimalIncidentRow(incident = incident)
                            }
                        )
                    }
                }
            }
        }

        // ── Floating Export Button ──
        if (uiState.incidents.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = { viewModel.exportToCsv() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
                    .border(1.dp, colors.outline, RoundedCornerShape(16.dp)),
                containerColor = colors.surfaceVariant,
                contentColor = colors.primary,
                shape = RoundedCornerShape(16.dp),
                icon = {
                    if (uiState.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = colors.primary
                        )
                    } else {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                },
                text = {
                    Text(
                        text = if (uiState.isExporting) "Exporting..." else "Export History Data",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
        )
    }
}

@Composable
private fun MinimalIncidentRow(incident: Incident) {
    val (icon, color, label) = when (incident.type) {
        IncidentType.FALL -> Triple(Icons.Default.Warning, FallColor, "Fall")
        IncidentType.BATTERY -> Triple(Icons.Default.BatteryAlert, BatteryColor, "Battery")
        IncidentType.MANUAL -> Triple(Icons.Default.TouchApp, ManualColor, "Manual")
    }
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.background)
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = colors.onBackground
            )
            Text(
                text = incident.formattedDateTime,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }

        if (incident.latitude != 0.0 || incident.longitude != 0.0) {
            Text(
                text = "%.7f, %.7f".format(incident.latitude, incident.longitude),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(end = 12.dp)
            )
        }

        GlowingStatusChip(
            label = if (incident.isSynced) "Synced" else "Pending",
            color = if (incident.isSynced) SyncedColor else PendingColor
        )
    }

    HorizontalDivider(
        color = colors.outline,
        thickness = 0.5.dp,
        modifier = Modifier.padding(start = 34.dp)
    )
}
