package com.ghostgramlabs.speakalert.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsState()
    val autoPlayOnUnlockOnly by viewModel.autoPlayOnUnlockOnly.collectAsState()
    val defaultSnoozeDuration by viewModel.defaultSnoozeDuration.collectAsState()
    
    // Quiet Time state
    val quietTimeEnabled by viewModel.quietTimeEnabled.collectAsState()
    val startHour by viewModel.quietTimeStartHour.collectAsState()
    val startMinute by viewModel.quietTimeStartMinute.collectAsState()
    val endHour by viewModel.quietTimeEndHour.collectAsState()
    val endMinute by viewModel.quietTimeEndMinute.collectAsState()

    val speakTextIfNoVoice by viewModel.speakTextIfNoVoice.collectAsState()
    val debugLoggingEnabled by viewModel.debugLoggingEnabled.collectAsState()
    val appVolume by viewModel.appVolume.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ============================================================
            // SECTION 0: APPEARANCE
            // ============================================================
            val themeMode by viewModel.themeMode.collectAsState()
            CollapsibleSettingsSection(
                title = "Appearance",
                icon = "🎨",
                initiallyExpanded = true
            ) {
                Text("App Theme", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf("System", "Light", "Dark")
                    themes.forEachIndexed { index, name ->
                        SnoozeOptionChip(
                            text = name,
                            isSelected = themeMode == index,
                            onClick = { 
                                viewModel.setThemeMode(index)
                                val msg = when(index) {
                                    1 -> "Light mode set"
                                    2 -> "Dark mode set"
                                    else -> "Following system theme"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ============================================================
            // SECTION 1: PLAYBACK (includes Test Reminder at bottom)
            // ============================================================
            CollapsibleSettingsSection(
                title = "Playback",
                icon = "🔊",
                initiallyExpanded = true
            ) {
                SwitchRow(
                    text = "Auto-play audio",
                    description = "Play sound automatically when reminder fires",
                    checked = autoPlayEnabled,
                    onCheckedChange = { 
                        viewModel.setAutoPlayEnabled(it)
                        Toast.makeText(context, if (it) "Auto-play on" else "Auto-play off", Toast.LENGTH_SHORT).show()
                    }
                )

                if (autoPlayEnabled) {
                    SwitchRow(
                        text = "Only when unlocked",
                        description = "Prevent playback on lock screen",
                        checked = autoPlayOnUnlockOnly,
                        onCheckedChange = { 
                            viewModel.setAutoPlayOnUnlockOnly(it)
                            Toast.makeText(context, if (it) "Lock screen playback off" else "Lock screen playback on", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                SwitchRow(
                    text = "Text-to-Speech",
                    description = "Read text reminders aloud",
                    checked = speakTextIfNoVoice,
                    onCheckedChange = { 
                        viewModel.setSpeakTextIfNoVoice(it) 
                        Toast.makeText(context, if (it) "TTS on" else "TTS off", Toast.LENGTH_SHORT).show()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                
                // Volume Slider
                Row(
                    verticalAlignment = Alignment.CenterVertically, 
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volume", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(60.dp))
                    Slider(
                        value = appVolume,
                        onValueChange = { viewModel.setAppVolume(it) },
                        valueRange = 0f..1f,
                        steps = 9,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "App Volume: ${(appVolume * 100).toInt()}%" },
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                    Text(
                         "${(appVolume * 100).toInt()}%",
                         style = MaterialTheme.typography.bodyMedium,
                         modifier = Modifier.padding(start = 8.dp).width(40.dp)
                    )
                }
                
                // Loop Auto-Stop (compact inline)
                val loopTimeoutMinutes by viewModel.loopTimeoutMinutes.collectAsState()
                Text("Loop duration", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                Text(
                    "How long a looping reminder notification repeats before stopping",
                    style = MaterialTheme.typography.bodySmall, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(5, 10, 15)
                    val isCustom = loopTimeoutMinutes != 0 && loopTimeoutMinutes !in presets
                    
                    presets.forEach { mins ->
                        SnoozeOptionChip(
                            text = "${mins}m",
                            isSelected = loopTimeoutMinutes == mins,
                            onClick = { 
                                viewModel.setLoopTimeoutMinutes(mins)
                                Toast.makeText(context, "Loop duration: ${mins} min", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Infinite option
                    SnoozeOptionChip(
                        text = "∞",
                        isSelected = loopTimeoutMinutes == 0,
                        onClick = { 
                            viewModel.setLoopTimeoutMinutes(0)
                            Toast.makeText(context, "Loop duration: Infinite", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    var showCustomLoopDialog by remember { mutableStateOf(false) }
                    SnoozeOptionChip(
                        text = if (isCustom) "${loopTimeoutMinutes}m" else "•••",
                        isSelected = isCustom,
                        onClick = { showCustomLoopDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (showCustomLoopDialog) {
                        CustomDurationDialog(
                            title = "Loop Duration",
                            initialValue = if (isCustom) loopTimeoutMinutes else 20,
                            maxMinutes = 1440,
                            description = "Set how long the reminder loop continues",
                            onDismiss = { showCustomLoopDialog = false },
                            onSave = { 
                                viewModel.setLoopTimeoutMinutes(it)
                                Toast.makeText(context, "Loop duration: $it min", Toast.LENGTH_SHORT).show()
                                showCustomLoopDialog = false
                            }
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Snooze Duration (moved from Timing)
                Text("Default snooze", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "How long the reminder is delayed when you tap Snooze",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf(2, 5, 10, 15)
                    val isCustom = defaultSnoozeDuration !in presets
                    
                    presets.forEach { mins ->
                        SnoozeOptionChip(
                            text = "${mins}m",
                            isSelected = !isCustom && defaultSnoozeDuration == mins,
                            onClick = { 
                                viewModel.setDefaultSnoozeDuration(mins)
                                Toast.makeText(context, "Snooze: ${mins} min", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
                    SnoozeOptionChip(
                        text = if (isCustom) "${defaultSnoozeDuration}m" else "•••",
                        isSelected = isCustom,
                        onClick = { showCustomSnoozeDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (showCustomSnoozeDialog) {
                        CustomDurationDialog(
                            title = "Custom Snooze",
                            initialValue = if (isCustom) defaultSnoozeDuration else 20,
                            maxMinutes = 240,
                            description = "Set how long the reminder is delayed",
                            onDismiss = { showCustomSnoozeDialog = false },
                            onSave = { 
                                viewModel.setDefaultSnoozeDuration(it)
                                Toast.makeText(context, "Snooze: $it min", Toast.LENGTH_SHORT).show()
                                showCustomSnoozeDialog = false
                            }
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Quiet Time (moved from Timing)
                SwitchRow(
                    text = "Quiet hours",
                    description = if (quietTimeEnabled) "${formatTime(startHour, startMinute)} – ${formatTime(endHour, endMinute)}" else "Silence reminders during set hours",
                    checked = quietTimeEnabled,
                    onCheckedChange = { 
                        viewModel.setQuietTimeEnabled(it)
                        Toast.makeText(context, if (it) "Quiet hours on" else "Quiet hours off", Toast.LENGTH_SHORT).show()
                    }
                )
                
                if (quietTimeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimePickerButton(
                            label = "Start",
                            hour = startHour,
                            minute = startMinute,
                            context = context,
                            onTimeSelected = { h, m -> 
                                viewModel.setQuietTimeStart(h, m)
                                Toast.makeText(context, "Quiet starts at ${formatTime(h, m)}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                        TimePickerButton(
                            label = "End",
                            hour = endHour,
                            minute = endMinute,
                            context = context,
                            onTimeSelected = { h, m -> 
                                viewModel.setQuietTimeEnd(h, m)
                                Toast.makeText(context, "Quiet ends at ${formatTime(h, m)}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Test Reminder Button (moved here from separate section)
                OutlinedButton(
                    onClick = {
                        viewModel.scheduleTestReminder()
                        Toast.makeText(context, "Test reminder in 10 seconds", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Test playback")
                }
            }



            // ============================================================
            // SECTION 3: PERMISSIONS (compact - show details only if issues)
            // ============================================================
            val hasMicPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                context, Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            
            val hasNotifPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true
            
            val canScheduleAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
            } else true
            
            val allGranted = hasMicPerm && hasNotifPerm && canScheduleAlarms
            
            // Show Permissions section only if there are issues, or as a compact card if all good
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (allGranted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) 
                                     else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                if (allGranted) {
                    // Compact success state
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Permission granted",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("All permissions granted", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    // Expanded error state with fix buttons
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = "Action required",
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Some permissions needed", style = MaterialTheme.typography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionRow(
                                name = "Notifications",
                                granted = false,
                                onFix = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                        
                        if (!canScheduleAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            PermissionRow(
                                name = "Exact Alarms",
                                granted = false,
                                onFix = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    context.startActivity(intent)
                                }
                            )
                        }
                        
                        if (!hasMicPerm) {
                            PermissionRow(
                                name = "Microphone",
                                granted = false,
                                onFix = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }

            // ============================================================
            // SECTION 4: HELP
            // ============================================================
            var showHelpDialog by remember { mutableStateOf(false) }
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showHelpDialog = true }
                        .padding(16.dp)
                        .semantics { role = androidx.compose.ui.semantics.Role.Button },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = "Help icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "How to use SpeakAlert",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Tips and tricks for getting the most out of the app",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Help",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (showHelpDialog) {
                HelpDialog(onDismiss = { showHelpDialog = false })
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.requestAppReview(context) }
                        .padding(16.dp)
                        .semantics { role = androidx.compose.ui.semantics.Role.Button },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating star",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enjoying SpeakAlert? Rate us",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Takes just a few seconds on Play Store",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Play Store",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Debug Section - ONLY for Debug builds (hidden from most users)
            if (com.ghostgramlabs.speakalert.BuildConfig.DEBUG) {
                CollapsibleSettingsSection(
                    title = "Developer",
                    icon = "🔧",
                    initiallyExpanded = false
                ) {
                    SwitchRow(
                        text = "Debug Logging",
                        description = "Record detailed logs",
                        checked = debugLoggingEnabled,
                        onCheckedChange = { viewModel.setDebugLoggingEnabled(it) }
                    )
                    
                    if (debugLoggingEnabled) {
                        OutlinedButton(
                            onClick = { viewModel.sendLogs(context) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send Logs")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PermissionRow(
    name: String,
    granted: Boolean,
    onFix: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, style = MaterialTheme.typography.bodyMedium)
        if (granted) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            TextButton(onClick = onFix) {
                Text("Fix")
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsSection(
    title: String,
    icon: String,
    initiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = icon,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotationAngle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = content
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(
    text: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
            )
        )
    }
}

@Composable
private fun TimePickerButton(
    label: String,
    hour: Int,
    minute: Int,
    context: android.content.Context,
    onTimeSelected: (Int, Int) -> Unit
) {
    OutlinedButton(
        onClick = {
            android.app.TimePickerDialog(
                context,
                { _, h, m -> onTimeSelected(h, m) },
                hour,
                minute,
                false
            ).show()
        }
    ) {
        Text("$label: ${formatTime(hour, minute)}")
    }
}

@Composable
private fun CustomDurationDialog(
    title: String,
    initialValue: Int,
    maxMinutes: Int,
    description: String,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var value by remember { mutableStateOf(initialValue.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { 
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column {
                Text(
                    "$description (Max ${maxMinutes}m)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.filter { c -> c.isDigit() } },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = value.toIntOrNull() ?: 10
                    onSave(mins.coerceIn(1, maxMinutes))
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", h, minute, amPm)
}
