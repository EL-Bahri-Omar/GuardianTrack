package com.guardian.track.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardian.track.ui.components.*
import com.guardian.track.ui.theme.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(uiState.alertMessage) {
        uiState.alertMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAlertMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Fixed Header (Hero) ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colors.primary.copy(alpha = 0.25f),
                                colors.secondary.copy(alpha = 0.10f),
                                colors.background
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title area
                    Column {
                        Text(
                            text = "GuardianTrack",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.onBackground
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (uiState.isServiceRunning) SuccessGreen else AlertRed
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isServiceRunning) "Surveillance Active"
                                else "Surveillance Stopped",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    // Service toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surfaceVariant.copy(alpha = 0.5f))
                            .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
                            .clickable {
                                if (uiState.isServiceRunning) viewModel.stopService()
                                else viewModel.startService()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isServiceRunning) Icons.Default.Shield
                                else Icons.Default.ShieldMoon,
                                contentDescription = null,
                                tint = if (uiState.isServiceRunning) colors.primary else colors.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = if (uiState.isServiceRunning) "Protection is ON"
                                else "Tap to activate",
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.onSurface
                            )
                        }
                        Icon(
                            imageVector = if (uiState.isServiceRunning) Icons.Default.Stop
                            else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (uiState.isServiceRunning) AlertRed else SuccessGreen
                        )
                    }
                }
            }

            // ── Scrollable Body ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                // ── Circular Magnitude ──
                if (uiState.isServiceRunning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Accelerometer",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularMagnitudeIndicator(
                            magnitude = uiState.sensorMagnitude,
                            threshold = uiState.sensitivityThreshold
                        )
                    }
                }

                // ── Floating Glass Chips ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassChip(
                        icon = if (uiState.batteryLevel > 20) Icons.Default.BatteryFull
                        else Icons.Default.BatteryAlert,
                        label = "Battery",
                        value = "${uiState.batteryLevel}%",
                        isActive = uiState.batteryLevel > 15,
                        activeColor = SuccessGreen,
                        inactiveColor = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    GlassChip(
                        icon = if (uiState.isGpsEnabled) Icons.Default.LocationOn
                        else Icons.Default.LocationOff,
                        label = "GPS",
                        value = if (uiState.isGpsEnabled) "Active" else "Off",
                        isActive = uiState.isGpsEnabled,
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Alert Pill Button ──
                FuturisticAlertButton(
                    isLoading = uiState.isAlertSending,
                    onClick = { viewModel.triggerManualAlert() }
                )

                // ── Recent Incidents Timeline ──
                if (uiState.recentIncidents.isNotEmpty()) {
                    Text(
                        text = "Recent Incidents",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )

                    Column {
                        uiState.recentIncidents.forEachIndexed { index, incident ->
                            TimelineIncidentItem(
                                incident = incident,
                                isLast = index == uiState.recentIncidents.lastIndex
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Snackbar overlay
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FuturisticAlertButton(
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "alertPulse")
    val colors = MaterialTheme.colorScheme

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(if (!isLoading) scale else 1f)
            .drawBehind {
                drawRoundRect(
                    color = colors.primary.copy(alpha = glowAlpha * 0.3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(60f),
                    size = size.copy(
                        width = size.width + 16f,
                        height = size.height + 16f
                    ),
                    topLeft = Offset(-8f, -8f)
                )
            }
    ) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(colors.primary, colors.secondary, GradientPurple)
                        ),
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Text(
                        text = if (isLoading) "SENDING..." else "SEND ALERT",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
