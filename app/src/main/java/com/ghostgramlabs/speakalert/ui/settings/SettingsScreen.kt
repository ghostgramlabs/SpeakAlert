package com.ghostgramlabs.speakalert.ui.settings

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.media.RingtoneManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.app.NotificationManagerCompat
import com.ghostgramlabs.speakalert.alarm.NotificationHelper
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.BatteryOptimizationSupport
import com.ghostgramlabs.speakalert.util.FullScreenIntentSupport
import com.ghostgramlabs.speakalert.util.WearOsConnectionInfo
import com.ghostgramlabs.speakalert.util.WearOsSupport
import com.ghostgramlabs.speakalert.util.normalizeLocalizedDigitsOrNull
import com.ghostgramlabs.speakalert.util.toLocalizedIntOrNull
import com.ghostgramlabs.speakalert.ui.components.PremiumScreenBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateUp: () -> Unit,
    onOpenBatteryOptimizationGuide: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    var fullScreenAccessGranted by remember { mutableStateOf(FullScreenIntentSupport.canUseFullScreenIntent(context)) }
    var dndPolicyAccessGranted by remember { mutableStateOf(readDndPolicyAccessGranted(context)) }
    var batteryOptimizationEnabled by remember {
        mutableStateOf(BatteryOptimizationSupport.isBatteryOptimizationEnabled(context))
    }
    var wearConnectionInfo by remember { mutableStateOf(WearOsConnectionInfo(isConnected = false)) }
    var isCheckingWearStatus by remember { mutableStateOf(true) }
    var wearRefreshTick by remember { mutableIntStateOf(0) }
    var appNotificationStatus by remember { mutableStateOf(readAppNotificationStatus(context)) }

    LaunchedEffect(context, wearRefreshTick) {
        isCheckingWearStatus = true
        appNotificationStatus = readAppNotificationStatus(context)
        dndPolicyAccessGranted = readDndPolicyAccessGranted(context)
        wearConnectionInfo = withContext(Dispatchers.IO) {
            WearOsSupport.getConnectionInfo(context)
        }
        isCheckingWearStatus = false
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    fullScreenAccessGranted = FullScreenIntentSupport.canUseFullScreenIntent(context)
                    dndPolicyAccessGranted = readDndPolicyAccessGranted(context)
                    batteryOptimizationEnabled = BatteryOptimizationSupport.isBatteryOptimizationEnabled(context)
                wearRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    val autoPlayEnabled by viewModel.autoPlayEnabled.collectAsState()
    val autoPlayOnUnlockOnly by viewModel.autoPlayOnUnlockOnly.collectAsState()
    val defaultSnoozeDuration by viewModel.defaultSnoozeDuration.collectAsState()
    val defaultFollowUpMinutes by viewModel.defaultFollowUpMinutes.collectAsState()
    val loopTimeoutMinutes by viewModel.loopTimeoutMinutes.collectAsState()
    
    // Quiet Time state
    val quietTimeEnabled by viewModel.quietTimeEnabled.collectAsState()
    val startHour by viewModel.quietTimeStartHour.collectAsState()
    val startMinute by viewModel.quietTimeStartMinute.collectAsState()
    val endHour by viewModel.quietTimeEndHour.collectAsState()
    val endMinute by viewModel.quietTimeEndMinute.collectAsState()

    val speakTextIfNoVoice by viewModel.speakTextIfNoVoice.collectAsState()
    val privatePlaybackEnabled by viewModel.privatePlaybackEnabled.collectAsState()
    val dndBypassEnabled by viewModel.dndBypassEnabled.collectAsState()
    val toneOnlyMode by viewModel.toneOnlyMode.collectAsState()
    val toneOnlyAlertToneUri by viewModel.toneOnlyAlertToneUri.collectAsState()
    val fullScreenAlertEnabled by viewModel.fullScreenAlertEnabled.collectAsState()
    val debugLoggingEnabled by viewModel.debugLoggingEnabled.collectAsState()
    val appVolume by viewModel.appVolume.collectAsState()
    val toneAutoStopLabel = if (loopTimeoutMinutes == 0) "Infinite" else "${loopTimeoutMinutes}m"
    val toneOnlySubtitle = "Plays a simple alarm tone instead of voice/TTS for maximum reliability. Auto-stop: $toneAutoStopLabel. Snooze: ${defaultSnoozeDuration}m."
    val tonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pickedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        viewModel.setToneOnlyAlertToneUri(pickedUri?.toString())
        Toast.makeText(
            context,
            if (pickedUri == null) "Using default alarm tone" else "Tone updated",
            Toast.LENGTH_SHORT
        ).show()
    }
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.setPrivatePlaybackEnabled(true)
        Toast.makeText(
            context,
            if (granted) {
                "Private playback on"
            } else {
                "Private playback on. Bluetooth routing needs nearby devices permission."
            },
            Toast.LENGTH_LONG
        ).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                )
            )
        }
    ) { paddingValues ->
        PremiumScreenBackground(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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
                icon = "UI",
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
                icon = "Play",
                initiallyExpanded = true
            ) {
                SwitchRow(
                    text = "Auto-play reminder audio",
                    description = if (toneOnlyMode) {
                        "Disabled while Tone-only mode is on"
                    } else {
                        "Automatically plays voice notes and audio files when a reminder fires"
                    },
                    checked = autoPlayEnabled,
                    onCheckedChange = { 
                        viewModel.setAutoPlayEnabled(it)
                        Toast.makeText(context, if (it) "Auto-play on" else "Auto-play off", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !toneOnlyMode
                )

                if (autoPlayEnabled && !toneOnlyMode) {
                    SwitchRow(
                        text = "Only when phone is unlocked",
                        description = "Prevent playback on lock screen",
                        checked = autoPlayOnUnlockOnly,
                        onCheckedChange = { 
                            viewModel.setAutoPlayOnUnlockOnly(it)
                            Toast.makeText(context, if (it) "Lock screen playback off" else "Lock screen playback on", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                SwitchRow(
                    text = "Speak typed reminders automatically",
                    description = if (toneOnlyMode) {
                        "Disabled while Tone-only mode is on"
                    } else {
                        "Only for text-only reminders with no voice note or audio file"
                    },
                    checked = speakTextIfNoVoice,
                    onCheckedChange = { 
                        viewModel.setSpeakTextIfNoVoice(it) 
                        Toast.makeText(
                            context,
                            if (it) "Automatic spoken text on" else "Automatic spoken text off",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = !toneOnlyMode
                )

                SwitchRow(
                    text = "Private playback",
                    description = if (toneOnlyMode) {
                        "Disabled while Tone-only mode is on"
                    } else {
                        "Prefer hearing aids, Bluetooth, wired headphones, or earpiece instead of the phone speaker. May vary by device."
                    },
                    checked = privatePlaybackEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && needsBluetoothConnectPermission(context)) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            viewModel.setPrivatePlaybackEnabled(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) "Private playback on" else "Private playback off",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !toneOnlyMode
                )

                SwitchRow(
                    text = "Tone-only mode",
                    description = toneOnlySubtitle,
                    checked = toneOnlyMode,
                    onCheckedChange = {
                        viewModel.setToneOnlyMode(it)
                        Toast.makeText(
                            context,
                            if (it) "Tone-only mode on" else "Tone-only mode off",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                ToneSelectionRow(
                    currentToneLabel = resolveToneOnlyToneLabel(context, toneOnlyAlertToneUri),
                    hasCustomTone = !toneOnlyAlertToneUri.isNullOrBlank(),
                    enabled = toneOnlyMode,
                    onChooseTone = {
                        val existingUri = toneOnlyAlertToneUri
                            ?.takeIf { it.isNotBlank() }
                            ?.let(Uri::parse)
                            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        tonePickerLauncher.launch(
                            Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select tone")
                            }
                        )
                    },
                    onUseDefault = {
                        viewModel.setToneOnlyAlertToneUri(null)
                        Toast.makeText(context, "Using default alarm tone", Toast.LENGTH_SHORT).show()
                    }
                )

                SwitchRow(
                    text = "Lock-screen full-screen alert",
                    description = if (fullScreenAccessGranted) {
                        "Show reminder actions over the lock screen when a reminder fires"
                    } else {
                        "Needs Android full-screen alert access to appear over the lock screen"
                    },
                    checked = fullScreenAlertEnabled,
                    onCheckedChange = {
                        viewModel.setFullScreenAlertEnabled(it)
                        if (it && !fullScreenAccessGranted) {
                            Toast.makeText(
                                context,
                                "Allow full-screen alerts in system settings",
                                Toast.LENGTH_SHORT
                            ).show()
                            FullScreenIntentSupport.openSettings(context)
                        }
                        Toast.makeText(
                            context,
                            if (it) "Lock-screen full-screen alert on" else "Lock-screen full-screen alert off",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                if (fullScreenAlertEnabled && !fullScreenAccessGranted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Full-screen access is still off",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Android 14+ requires a separate system permission for full-screen reminder alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { FullScreenIntentSupport.openSettings(context) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Open permission settings")
                            }
                        }
                    }
                }

                if (toneOnlyMode) {
                    Text(
                        text = "You can still tap Play reminder or Speak reminder from the notification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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
                        enabled = !toneOnlyMode,
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
                         color = if (toneOnlyMode) {
                             MaterialTheme.colorScheme.onSurfaceVariant
                         } else {
                             MaterialTheme.colorScheme.onSurface
                         },
                         modifier = Modifier.padding(start = 8.dp).width(40.dp)
                    )
                }
                if (toneOnlyMode) {
                    Text(
                        text = "In Tone-only mode, volume is controlled by your device alarm/notification volume.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                 
                // Loop Auto-Stop (compact inline)
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
                        text = "Infinite",
                        isSelected = loopTimeoutMinutes == 0,
                        onClick = { 
                            viewModel.setLoopTimeoutMinutes(0)
                            Toast.makeText(context, "Loop duration: Infinite", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    
                    var showCustomLoopDialog by remember { mutableStateOf(false) }
                    SnoozeOptionChip(
                        text = if (isCustom) "${loopTimeoutMinutes}m" else "Custom",
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
                        text = if (isCustom) "${defaultSnoozeDuration}m" else "Custom",
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

                // Default follow-up check
                Text("Default follow-up check", style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (defaultFollowUpMinutes == 0) {
                        "Off — new reminders are created with no follow-up. You can still set one per reminder."
                    } else {
                        "New reminders start with a ${defaultFollowUpMinutes}m follow-up that re-alerts if not marked done. Existing reminders are unchanged."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                com.ghostgramlabs.speakalert.ui.components.FollowUpDurationPicker(
                    currentMinutes = defaultFollowUpMinutes,
                    onChange = { minutes ->
                        viewModel.setDefaultFollowUpMinutes(minutes)
                        Toast.makeText(
                            context,
                            if (minutes == 0) "Default follow-up off" else "Default follow-up: ${minutes}m",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Quiet Time (moved from Timing)
                SwitchRow(
                    text = "Quiet hours",
                    description = if (quietTimeEnabled) "${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}" else "Silence reminders during set hours",
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
                            onTimeSelected = { h, m -> 
                                viewModel.setQuietTimeStart(h, m)
                                Toast.makeText(context, "Quiet starts at ${formatTime(h, m)}", Toast.LENGTH_SHORT).show()
                            }
                        )
                        
                        TimePickerButton(
                            label = "End",
                            hour = endHour,
                            minute = endMinute,
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
                    Text("Test reminder")
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
            // SECTION 4: RELIABILITY
            // ============================================================
            CollapsibleSettingsSection(
                title = "Reliability",
                icon = "Safe",
                initiallyExpanded = true
            ) {
                SwitchRow(
                    text = "Alert even during DND",
                    description = when {
                        !dndBypassEnabled -> "Follows your phone's sound mode. DND or routines may silence reminders."
                        dndPolicyAccessGranted -> "Active: reminders use alarm-priority sound and DND bypass."
                        else -> "On, but Android DND access is needed before bypass works."
                    },
                    checked = dndBypassEnabled,
                    onCheckedChange = {
                        viewModel.setDndBypassEnabled(it)
                        NotificationHelper(context).refreshChannels(dndBypassEnabled = it)
                        Toast.makeText(
                            context,
                            if (it) "DND alert bypass on" else "DND alert bypass off",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                if (dndBypassEnabled && !dndPolicyAccessGranted) {
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    "Android DND access settings are not available on this device.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Grant Android DND access")
                    }
                    Text(
                        "This does not control your phone's DND mode. It only lets $APP_DISPLAY_NAME request permission to play important reminders through it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenBatteryOptimizationGuide() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryAlert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Battery Optimization Guide",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (batteryOptimizationEnabled) {
                                    "Battery optimization can delay or stop reminders on some phones."
                                } else {
                                    "$APP_DISPLAY_NAME is already allowed to run in the background."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
            }

            // ============================================================
            // SECTION 5: WEAR OS
            // ============================================================
            CollapsibleSettingsSection(
                title = "Wear OS",
                icon = "Wear",
                initiallyExpanded = false
            ) {
                val notificationsReady =
                    appNotificationStatus.appNotificationsEnabled && appNotificationStatus.reminderChannelEnabled
                val statusText = when {
                    isCheckingWearStatus -> "Checking Wear OS connection..."
                    wearConnectionInfo.isConnected && wearConnectionInfo.connectedNodeCount > 1 -> {
                        "${wearConnectionInfo.connectedNodeCount} Wear OS watches connected"
                    }
                    wearConnectionInfo.isConnected -> "Wear OS watch connected"
                    else -> "No Wear OS watch connected"
                }

                val statusColor = if (wearConnectionInfo.isConnected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Watch,
                                contentDescription = null,
                                tint = statusColor
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (isCheckingWearStatus) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                            }
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = statusColor
                            )
                        }

                        Text(
                            text = "$APP_DISPLAY_NAME watch reminders are supported on Wear OS watches only.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (wearConnectionInfo.isConnected && !notificationsReady) {
                            Text(
                                text = if (!appNotificationStatus.appNotificationsEnabled) {
                                    "Phone app notifications are off. Turn them on so reminders can sync to watch."
                                } else {
                                    "$APP_DISPLAY_NAME reminder channel is muted. Enable it to send reminders to watch."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open notification settings")
                            }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            putExtra(Settings.EXTRA_CHANNEL_ID, NotificationHelper.CHANNEL_ID)
                                        }
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open reminder channel")
                                }
                            }
                        } else if (wearConnectionInfo.isConnected) {
                            Text(
                                text = "Phone notifications are enabled. If reminders are still missing on watch, enable notification sync in your Wear OS companion app.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { wearRefreshTick++ },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Refresh status")
                        }
                    }
                }
            }

            // SECTION 6: HELP & GUIDE
            // ============================================================
            CollapsibleSettingsSection(
                title = "App guide",
                icon = "Help",
                initiallyExpanded = true
            ) {
                var showHelpDialog by remember { mutableStateOf(false) }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHelpDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Help,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "App guide",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                    }
                }
                
                if (showHelpDialog) {
                    HelpDialog(onDismiss = { showHelpDialog = false })
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!openSupportEmail(context)) {
                                Toast.makeText(
                                    context,
                                    "Unable to open email on this device.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Contact support",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Email feedback, questions, or playback issues",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // ============================================================
            // SECTION 5: RATE & REVIEW
            // ============================================================
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!openAppRating(context)) {
                                Toast.makeText(
                                    context,
                                    "Unable to open the rating page on this device.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Rate $APP_DISPLAY_NAME",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "If you enjoy the app, please leave a review!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // Debug Section - ONLY for Debug builds (hidden from most users)
            if (com.ghostgramlabs.speakalert.BuildConfig.DEBUG) {
                CollapsibleSettingsSection(
                    title = "Developer",
                    icon = "Dev",
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
}

private data class AppNotificationStatus(
    val appNotificationsEnabled: Boolean,
    val reminderChannelEnabled: Boolean
)

private fun readAppNotificationStatus(context: android.content.Context): AppNotificationStatus {
    val appEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val channelEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        val reminderChannel = manager?.getNotificationChannel(NotificationHelper.CHANNEL_ID)
        reminderChannel?.importance != android.app.NotificationManager.IMPORTANCE_NONE
    } else {
        true
    }
    return AppNotificationStatus(
        appNotificationsEnabled = appEnabled,
        reminderChannelEnabled = channelEnabled
    )
}

private fun readDndPolicyAccessGranted(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.app.NotificationManager::class.java)
    return manager?.isNotificationPolicyAccessGranted == true
}

@Composable
private fun PermissionRow(
    name: String,
    granted: Boolean,
    onFix: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (granted) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            } else {
                TextButton(onClick = onFix) {
                    Text("Fix")
                }
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
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    Icon(
                        imageVector = settingsSectionIcon(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content
                )
            }
        }
    }
}

private fun settingsSectionIcon(icon: String): ImageVector {
    return when (icon) {
        "UI" -> Icons.Default.Palette
        "Play" -> Icons.Default.PlayArrow
        "Safe" -> Icons.Default.VerifiedUser
        "Wear" -> Icons.Default.Watch
        "Help" -> Icons.Default.Help
        "Dev" -> Icons.Default.Build
        else -> Icons.Default.Settings
    }
}

private fun openAppRating(context: android.content.Context): Boolean {
    val packageName = context.packageName
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    ).apply {
        setPackage("com.android.vending")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    ).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    return when {
        marketIntent.resolveActivity(context.packageManager) != null -> {
            context.startActivity(marketIntent)
            true
        }
        webIntent.resolveActivity(context.packageManager) != null -> {
            context.startActivity(webIntent)
            true
        }
        else -> false
    }
}

private fun openSupportEmail(context: android.content.Context): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:ghostgramlabs@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, "$APP_DISPLAY_NAME support")
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: Exception) {
        false
    }
}

private fun needsBluetoothConnectPermission(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) !=
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

@Composable
private fun SwitchRow(
    text: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    enabled = enabled,
                    onValueChange = onCheckedChange
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) {
                    stateDescription = if (checked) "On" else "Off"
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val contentColor = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(text, style = MaterialTheme.typography.bodyLarge, color = contentColor)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

@Composable
private fun ToneSelectionRow(
    currentToneLabel: String,
    hasCustomTone: Boolean,
    enabled: Boolean,
    onChooseTone: () -> Unit,
    onUseDefault: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Tone-only alert sound",
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = if (enabled) {
                    "$currentToneLabel. If it becomes unavailable, SpeakAlert falls back to the default alarm tone."
                } else {
                    "Enable Tone-only mode to choose a sound. SpeakAlert will use the default alarm tone until then."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onChooseTone,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Choose tone")
                }
                if (hasCustomTone) {
                    OutlinedButton(
                        onClick = onUseDefault,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Use default")
                    }
                }
            }
        }
    }
}

private fun resolveToneOnlyToneLabel(
    context: android.content.Context,
    uriString: String?
): String {
    if (uriString.isNullOrBlank()) return "Default alarm tone"
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return "Default alarm tone"
    val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull()
    return ringtone?.getTitle(context) ?: "Selected tone unavailable, using default alarm tone"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerButton(
    label: String,
    hour: Int,
    minute: Int,
    onTimeSelected: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = { showPicker = true }
    ) {
        Text("$label: ${formatTime(hour, minute)}")
    }

    if (showPicker) {
        val timeState = rememberTimePickerState(
            initialHour = hour,
            initialMinute = minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context)
        )
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("Select $label time") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                Button(
                    onClick = {
                        onTimeSelected(timeState.hour, timeState.minute)
                        showPicker = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$description (Max ${maxMinutes}m)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = value,
                onValueChange = { newValue ->
                    val normalized = newValue.normalizeLocalizedDigitsOrNull()
                    if (normalized != null && normalized.length <= maxMinutes.toString().length) {
                        value = normalized
                    }
                },
                label = { Text("Minutes") },
                supportingText = { Text("Max allowed: $maxMinutes min") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    val mins = value.toLocalizedIntOrNull() ?: 10
                    onSave(mins.coerceIn(1, maxMinutes))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Save")
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("Cancel")
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", h, minute, amPm)
}
