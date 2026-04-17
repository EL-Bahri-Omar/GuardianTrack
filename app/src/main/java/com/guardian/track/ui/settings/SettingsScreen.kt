package com.guardian.track.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guardian.track.ui.theme.*

// ── Reusable circular icon wrapper ──
@Composable
private fun CircularIconBox(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (uiState.showAddContactDialog) {
        FuturisticAddContactDialog(
            onDismiss = { viewModel.hideAddContactDialog() },
            onConfirm = { name, phone -> viewModel.addContact(name, phone) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Fixed Header ──
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 8.dp)
            )

            // ── Scrollable Content ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // ── Detection ──
                SettingsSectionHeader("Detection")

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularIconBox(
                                icon = Icons.Default.Speed,
                                tint = colors.primary
                            )
                            Text(
                                "Sensitivity",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                        }
                        Text(
                            text = "%.1f m/s²".format(uiState.sensitivityThreshold),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )
                    }

                    // Custom slider with circle thumb
                    val interactionSource = remember { MutableInteractionSource() }
                    Slider(
                        value = uiState.sensitivityThreshold,
                        onValueChange = { viewModel.updateSensitivity(it) },
                        valueRange = 3f..30f,
                        modifier = Modifier.fillMaxWidth(),
                        interactionSource = interactionSource,
                        thumb = {
                            // Filled circle thumb
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                                    .border(2.dp, Color.White, CircleShape)
                            )
                        },
                        track = { sliderState ->
                            SliderDefaults.Track(
                                sliderState = sliderState,
                                modifier = Modifier.height(6.dp),
                                colors = SliderDefaults.colors(
                                    activeTrackColor = colors.primary,
                                    inactiveTrackColor = colors.surfaceVariant
                                )
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensitive", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                        Text("Less sensitive", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    }
                }

                // ── Appearance ──
                SettingsSectionHeader("Appearance")

                NeonToggleRow(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Switch between light and dark theme",
                    checked = uiState.isDarkMode,
                    onCheckedChange = { viewModel.updateDarkMode(it) },
                    activeColor = colors.secondary
                )

                // ── Communication ──
                SettingsSectionHeader("Communication")

                var editingNumber by remember { mutableStateOf(false) }
                var tempNumber by remember(uiState.emergencyNumber) {
                    mutableStateOf(uiState.emergencyNumber)
                }

                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingNumber = !editingNumber }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularIconBox(
                            icon = Icons.Default.Phone,
                            tint = colors.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Emergency Number",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.onSurface
                            )
                            Text(
                                text = if (uiState.emergencyNumber.isBlank()) "Not configured" else uiState.emergencyNumber,
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Icon(
                            if (editingNumber) Icons.Default.KeyboardArrowUp else Icons.Default.Edit,
                            null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp)
                        )
                    }

                    if (editingNumber) {
                        UnderlineTextField(
                            value = tempNumber,
                            onValueChange = { tempNumber = it },
                            placeholder = "Enter phone number",
                            keyboardType = KeyboardType.Phone,
                            onDone = {
                                viewModel.updateEmergencyNumber(tempNumber)
                                editingNumber = false
                            }
                        )
                    }
                }

                HorizontalDivider(color = colors.outline, thickness = 0.5.dp)

                NeonToggleRow(
                    icon = Icons.Default.Sms,
                    title = "SMS Alerts",
                    subtitle = "Send real-time SMS alerts to emergency contacts",
                    checked = !uiState.isSmsSimulation,
                    onCheckedChange = { viewModel.updateSmsSimulation(!it) },
                    activeColor = SuccessGreen
                )

                if (uiState.isSmsSimulation) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.08f))
                            .border(1.dp, colors.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularIconBox(
                            icon = Icons.Default.Info,
                            tint = colors.primary
                        )
                        Text(
                            "Simulation mode — SMS replaced by notifications",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = colors.primary
                        )
                    }
                }


                // ── Emergency Contacts ──
                SettingsSectionHeader("Emergency Contacts")

                if (uiState.contacts.isEmpty()) {
                    Text(
                        "No contacts added",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column {
                        uiState.contacts.forEach { contact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Contact avatar
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = colors.primary,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        contact.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteContact(contact.id) }) {
                                    Icon(Icons.Default.Close, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = colors.outline, thickness = 0.5.dp)
                        }
                    }
                }

                TextButton(onClick = { viewModel.showAddContactDialog() }) {
                    Icon(Icons.Default.PersonAdd, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Contact", fontWeight = FontWeight.SemiBold)
                }

                // ── About ──
                SettingsSectionHeader("About")
                Row(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularIconBox(
                        icon = Icons.Default.Shield,
                        tint = colors.primary
                    )
                    Column {
                        Text(
                            "GuardianTrack",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.onSurface
                        )
                        Text(
                            "v1.0 — Personal Safety",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }

                // Bottom spacer
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.ExtraBold,
        color = colors.primary,
        letterSpacing = 2.sp
    )
}

@Composable
private fun NeonToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    activeColor: Color = MaterialTheme.colorScheme.primary
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) activeColor else MaterialTheme.colorScheme.surfaceVariant,
        label = "trackColor"
    )
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularIconBox(
            icon = icon,
            tint = if (checked) activeColor else colors.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .width(52.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .drawBehind {
                    drawRoundRect(
                        color = trackColor,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        size = size
                    )
                    if (checked) {
                        drawRoundRect(
                            color = activeColor.copy(alpha = 0.3f),
                            cornerRadius = CornerRadius(14.dp.toPx()),
                            size = Size(size.width + 8f, size.height + 8f),
                            topLeft = Offset(-4f, -4f)
                        )
                    }
                }
                .clickable { onCheckedChange(!checked) }
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}

@Composable
private fun UnderlineTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(start = 48.dp)) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = colors.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal
            ),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Column {
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                color = colors.onSurfaceVariant,
                                fontSize = 15.sp
                            )
                        }
                        innerTextField()
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(colors.primary, colors.secondary)
                                )
                            )
                    )
                }
            }
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDone) {
            Text("Save", color = colors.primary, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FuturisticAddContactDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                "Add Contact",
                color = colors.onSurface,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        cursorColor = colors.primary,
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outline,
                        cursorColor = colors.primary,
                        focusedTextColor = colors.onSurface,
                        unfocusedTextColor = colors.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, phone) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) { Text("Save", color = colors.primary, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = colors.onSurfaceVariant) }
        }
    )
}
