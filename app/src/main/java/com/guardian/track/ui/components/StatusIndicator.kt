package com.guardian.track.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.guardian.track.ui.theme.*

val GlassShape = RoundedCornerShape(24.dp)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(GlassShape)
            .background(colors.surfaceVariant.copy(alpha = 0.3f))
            .border(1.dp, colors.outline, GlassShape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GlassChip(
    icon: ImageVector,
    label: String,
    value: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = SuccessGreen,
    inactiveColor: Color = AlertRed
) {
    val statusColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        label = "chipColor"
    )
    val glowColor = statusColor.copy(alpha = 0.2f)
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.4f))
            .border(1.dp, colors.outline, RoundedCornerShape(20.dp))
            .drawBehind {
                drawCircle(
                    color = glowColor,
                    radius = 60f,
                    center = Offset(40f, size.height / 2)
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = statusColor,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )
        }
    }
}

@Composable
fun CircularMagnitudeIndicator(
    magnitude: Float,
    threshold: Float,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val progress by animateFloatAsState(
        targetValue = (magnitude / (threshold * 1.5f)).coerceIn(0f, 1f),
        animationSpec = tween(300),
        label = "circularProgress"
    )

    val arcColor by animateColorAsState(
        targetValue = when {
            magnitude > threshold -> AlertRed
            magnitude > threshold * 0.7f -> WarningAmber
            else -> Primary
        },
        label = "arcColor"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (magnitude > threshold * 0.5f) 0.4f else 0.15f,
        label = "glow"
    )
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.85f)
                .drawBehind {
                    drawCircle(
                        color = arcColor.copy(alpha = glowAlpha),
                        radius = this.size.minDimension / 2
                    )
                }
        )

        Canvas(modifier = Modifier.size(size)) {
            drawArc(
                color = colors.surfaceVariant,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
        }

        Canvas(modifier = Modifier.size(size)) {
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(arcColor.copy(alpha = 0.3f), arcColor)
                ),
                startAngle = 135f,
                sweepAngle = 270f * progress,
                useCenter = false,
                style = Stroke(width = 12f, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%.1f".format(magnitude),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )
            Text(
                text = "m/s²",
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GlowingStatusChip(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .drawBehind {
                drawRoundRect(
                    color = color.copy(alpha = glowAlpha * 0.3f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f),
                    size = this.size
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

private val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

@Composable
private fun Canvas(modifier: Modifier, onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}
