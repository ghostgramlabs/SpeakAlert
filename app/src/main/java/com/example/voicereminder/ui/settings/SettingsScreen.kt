package com.example.voicereminder.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voicereminder.ui.AppViewModelProvider
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.example.voicereminder.alarm.ReminderAlarmReceiver
import android.app.AlarmManager
import com.example.voicereminder.data.model.ReminderEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsState()
    val autoPlayOnUnlockOnly by viewModel.autoPlayOnUnlockOnly.collectAsState()
    val speakTextIfNoVoice by viewModel.speakTextIfNoVoice.collectAsState()
    val appVolume by viewModel.appVolume.collectAsState()
    val defaultSnoozeDuration by viewModel.defaultSnoozeDuration.collectAsState()
    val debugLoggingEnabled by viewModel.debugLoggingEnabled.collectAsState()
    
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

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
                .padding(16.dp)
        ) {
            Text("Playback", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            SwitchRow(
                text = "Auto-play voice reminder",
                description = "Play audio immediately when reminder fires",
                checked = autoPlayEnabled,
                onCheckedChange = { viewModel.setAutoPlayEnabled(it) }
            )

            if (autoPlayEnabled) {
                SwitchRow(
                    text = "Only when unlocked",
                    description = "Prevent playback if screen is locked",
                    checked = autoPlayOnUnlockOnly,
                    onCheckedChange = { viewModel.setAutoPlayOnUnlockOnly(it) }
                )
            }
            
            SwitchRow(
                text = "Speak text reminders aloud",
                description = "If a reminder has no voice recording, its text will be spoken using Text-to-Speech.",
                checked = speakTextIfNoVoice,
                onCheckedChange = { viewModel.setSpeakTextIfNoVoice(it) }
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text("Reminder Volume", style = MaterialTheme.typography.bodyLarge)
            Text("App-specific volume level", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Row(
                verticalAlignment = Alignment.CenterVertically, 
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Slider(
                    value = appVolume,
                    onValueChange = { viewModel.setAppVolume(it) },
                    valueRange = 0f..1f,
                    steps = 9,
                    modifier = Modifier.weight(1f)
                )
                Text(
                     "${(appVolume * 100).toInt()}%",
                     style = MaterialTheme.typography.bodyMedium,
                     modifier = Modifier.padding(start = 12.dp).width(40.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Quiet Time", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Silence reminders during specific hours. Missed reminders will be saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            val quietTimeEnabled by viewModel.quietTimeEnabled.collectAsState()
            val startHour by viewModel.quietTimeStartHour.collectAsState()
            val startMinute by viewModel.quietTimeStartMinute.collectAsState()
            val endHour by viewModel.quietTimeEndHour.collectAsState()
            val endMinute by viewModel.quietTimeEndMinute.collectAsState()
            
            SwitchRow(
                text = "Enable Quiet Time",
                description = if (quietTimeEnabled) "Muting reminders from ${formatTime(startHour, startMinute)} to ${formatTime(endHour, endMinute)}" else "Mute notifications at night",
                checked = quietTimeEnabled,
                onCheckedChange = { viewModel.setQuietTimeEnabled(it) }
            )
            
            if (quietTimeEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TimePickerButton(
                        label = "Start Time",
                        hour = startHour,
                        minute = startMinute,
                        context = context,
                        onTimeSelected = { h, m -> viewModel.setQuietTimeStart(h, m) }
                    )
                    
                    TimePickerButton(
                        label = "End Time",
                        hour = endHour,
                        minute = endMinute,
                        context = context,
                        onTimeSelected = { h, m -> viewModel.setQuietTimeEnd(h, m) }
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Snooze Defaults", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Default snooze duration (minutes)", style = MaterialTheme.typography.bodyMedium)
            
            Row(modifier = Modifier.padding(top = 8.dp)) {
                listOf(5, 10, 15, 30).forEach { mins ->
                    FilterChip(
                        selected = defaultSnoozeDuration == mins,
                        onClick = { viewModel.setDefaultSnoozeDuration(mins) },
                        label = { Text("$mins m") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Permissions & Reliability", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            // Check all permissions
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
            
            // Status Summary Card - Neutral styling (not alarming)
            Text(
                "These permissions help reminders work reliably.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (allGranted) Icons.Filled.Check else Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (allGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            if (allGranted) "All permissions granted" else "Some permissions needed",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            if (allGranted) "Reminders will work reliably" else "Tap items below to adjust",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Microphone Permission
            ListItem(
                headlineContent = { Text("Microphone") },
                supportingContent = { Text(if (hasMicPerm) "Allowed" else "Required for voice recording") },
                trailingContent = {
                    if (hasMicPerm) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    } else {
                        Button(onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        }) {
                            Text("Allow")
                        }
                    }
                }
            )
            
            // Notification Permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ListItem(
                    headlineContent = { Text("Notifications") },
                    supportingContent = { Text(if (hasNotifPerm) "Allowed" else "Required for alerts") },
                    trailingContent = {
                        if (hasNotifPerm) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            Button(onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            }) {
                                Text("Allow")
                            }
                        }
                    }
                )
            }
            
            // Exact Alarms (Android 12+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ListItem(
                   headlineContent = { Text("Exact Alarms") },
                   supportingContent = { Text(if (canScheduleAlarms) "Allowed" else "Required for precise timing") },
                   trailingContent = {
                       if (canScheduleAlarms) {
                           Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                       } else {
                           Button(onClick = {
                               val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                               context.startActivity(intent)
                           }) {
                               Text("Fix")
                           }
                       }
                   }
                )
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Test Button
            Button(
                onClick = {
                    viewModel.scheduleTestReminder()
                    Toast.makeText(
                        context,
                        "Test reminder scheduled! Will fire in 10 seconds.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                enabled = true,
                modifier = Modifier.fillMaxWidth()
            ) {
                 Text("Test Reminder in 10s")
            }
            
            Divider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Help & Instructions
            Text("Help & Instructions", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            
            var showHelpDialog by remember { mutableStateOf(false) }
            
            OutlinedButton(
                onClick = { showHelpDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("View App Features Guide")
            }
            
            // Troubleshooting Section (Debug Only)
            if (com.example.voicereminder.BuildConfig.SHOW_DEBUG_OPTIONS) {
                Divider(modifier = Modifier.padding(vertical = 16.dp))
                
                Text("Troubleshooting (Debug Only)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                
                ListItem(
                    headlineContent = { Text("Enable Debug Logging") },
                    supportingContent = { Text("Log app activity to file") },
                    trailingContent = {
                        Switch(
                            checked = debugLoggingEnabled,
                            onCheckedChange = { viewModel.setDebugLoggingEnabled(it) }
                        )
                    }
                )
                
                if (debugLoggingEnabled) {
                    OutlinedButton(
                        onClick = { viewModel.sendLogs(context) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text("Send Logs to Support")
                    }
                    Text(
                         "If app is not working, enable logs and send to ghostgramlabs@gmail.com",
                         style = MaterialTheme.typography.bodySmall,
                         color = MaterialTheme.colorScheme.onSurfaceVariant,
                         modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }
            }
            
            if (showHelpDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpDialog = false },
                    title = { 
                        Text(
                            "Voice Reminder Guide",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        ) 
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HelpCard(
                                icon = "🎙️",
                                title = "Voice-First Reminders",
                                items = listOf(
                                    "Tap mic to record instantly",
                                    "Play from home cards",
                                    "Edits preserve your recordings"
                                )
                            )
                            
                            HelpCard(
                                icon = "📝",
                                title = "Text Alternative",
                                items = listOf(
                                    "Type instead of speaking",
                                    "Auto text-to-speech option",
                                    "Smart labels or custom names"
                                )
                            )
                            
                            HelpCard(
                                icon = "📅",
                                title = "Smart Scheduling",
                                items = listOf(
                                    "See \"Today • 6:30 PM\" headers",
                                    "Daily, Weekly, Monthly repeats",
                                    "Custom intervals (every 2 days)"
                                )
                            )
                            
                            HelpCard(
                                icon = "🔔",
                                title = "Playback & Actions",
                                items = listOf(
                                    "Auto-play when reminder fires",
                                    "\"Only unlocked\" privacy mode",
                                    "Play, Snooze, Done from notification"
                                )
                            )
                            
                            HelpCard(
                                icon = "📬",
                                title = "Missed Reminders",
                                items = listOf(
                                    "Stays visible while playing",
                                    "Auto-completes after playback",
                                    "Review all missed in inbox"
                                )
                            )
                            
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💡", style = MaterialTheme.typography.headlineSmall)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            "Pro Tips",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            "Test with the button above • Grant all permissions • Disable battery optimization",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showHelpDialog = false }) {
                            Text("Got it!", fontWeight = FontWeight.SemiBold)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SwitchRow(
    text: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun HelpCard(icon: String, title: String, items: List<String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    icon,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            items.forEach { item ->
                Row(
                    modifier = Modifier.padding(start = 8.dp, top = 2.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        "• ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
    cal.set(java.util.Calendar.MINUTE, minute)
    val sdf = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
    return sdf.format(cal.time)
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
                false // 12-hour format usually preferred by users
            ).show()
        },
        modifier = Modifier.width(160.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall)
            Text(
                formatTime(hour, minute),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
