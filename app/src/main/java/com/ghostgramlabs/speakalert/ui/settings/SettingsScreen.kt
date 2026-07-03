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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.openPlayStoreListing
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
import com.ghostgramlabs.speakalert.ui.components.SystemTimePickerDialog
import com.ghostgramlabs.speakalert.ui.components.shouldUseSystemDateTimePickers
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
    val toneAutoStopLabel = if (loopTimeoutMinutes == 0) stringResource(R.string.set_infinite) else stringResource(R.string.set_minutes_short, loopTimeoutMinutes)
    val toneOnlySubtitle = stringResource(R.string.set_tone_only_desc, toneAutoStopLabel, defaultSnoozeDuration)
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
            if (pickedUri == null) context.getString(R.string.set_toast_default_tone) else context.getString(R.string.set_toast_tone_updated),
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
                context.getString(R.string.set_toast_private_on)
            } else {
                context.getString(R.string.set_private_bt)
            },
            Toast.LENGTH_LONG
        ).show()
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_cd_settings)) },
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
                title = stringResource(R.string.set_appearance),
                icon = "UI",
                initiallyExpanded = true
            ) {
                Text(stringResource(R.string.set_app_theme), style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themes = listOf(
                        stringResource(R.string.theme_system),
                        stringResource(R.string.theme_light),
                        stringResource(R.string.theme_dark)
                    )
                    themes.forEachIndexed { index, name ->
                        SnoozeOptionChip(
                            text = name,
                            isSelected = themeMode == index,
                            onClick = {
                                viewModel.setThemeMode(index)
                                val msg = when(index) {
                                    1 -> context.getString(R.string.set_toast_theme_light)
                                    2 -> context.getString(R.string.set_toast_theme_dark)
                                    else -> context.getString(R.string.set_toast_theme_system)
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.bodyMedium)
                val currentLangTag = com.ghostgramlabs.speakalert.util.AppLocale.currentTag()
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.ghostgramlabs.speakalert.util.AppLocale.supported.forEach { (tag, label) ->
                        SnoozeOptionChip(
                            text = label,
                            isSelected = currentLangTag == tag,
                            onClick = { com.ghostgramlabs.speakalert.util.AppLocale.set(tag) }
                        )
                    }
                }
            }

            // ============================================================
            // SECTION 1: PLAYBACK (includes Test Reminder at bottom)
            // ============================================================
            CollapsibleSettingsSection(
                title = stringResource(R.string.set_playback),
                icon = "Play",
                initiallyExpanded = true
            ) {
                SwitchRow(
                    text = stringResource(R.string.set_autoplay),
                    description = if (toneOnlyMode) {
                        stringResource(R.string.set_disabled_tone)
                    } else {
                        stringResource(R.string.set_autoplay_desc)
                    },
                    checked = autoPlayEnabled,
                    onCheckedChange = {
                        viewModel.setAutoPlayEnabled(it)
                        Toast.makeText(context, if (it) context.getString(R.string.set_toast_autoplay_on) else context.getString(R.string.set_toast_autoplay_off), Toast.LENGTH_SHORT).show()
                    },
                    enabled = !toneOnlyMode
                )

                if (autoPlayEnabled && !toneOnlyMode) {
                    SwitchRow(
                        text = stringResource(R.string.set_unlock_only),
                        description = stringResource(R.string.set_unlock_only_desc),
                        checked = autoPlayOnUnlockOnly,
                        onCheckedChange = { 
                            viewModel.setAutoPlayOnUnlockOnly(it)
                            Toast.makeText(context, if (it) context.getString(R.string.set_toast_lock_off) else context.getString(R.string.set_toast_lock_on), Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                
                SwitchRow(
                    text = stringResource(R.string.set_speak_text),
                    description = if (toneOnlyMode) {
                        stringResource(R.string.set_disabled_tone)
                    } else {
                        stringResource(R.string.set_speak_text_desc)
                    },
                    checked = speakTextIfNoVoice,
                    onCheckedChange = {
                        viewModel.setSpeakTextIfNoVoice(it)
                        Toast.makeText(
                            context,
                            if (it) context.getString(R.string.set_toast_speak_on) else context.getString(R.string.set_toast_speak_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    enabled = !toneOnlyMode
                )

                val ttsLanguageMode by viewModel.ttsLanguageMode.collectAsState()
                Text(
                    text = stringResource(R.string.set_spoken_language),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = stringResource(R.string.set_spoken_language_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val ttsOptions = listOf(
                        stringResource(R.string.tts_auto),
                        stringResource(R.string.tts_device),
                        stringResource(R.string.tts_english)
                    )
                    ttsOptions.forEachIndexed { index, name ->
                        SnoozeOptionChip(
                            text = name,
                            isSelected = ttsLanguageMode == index,
                            onClick = {
                                viewModel.setTtsLanguageMode(index)
                                val msg = when (index) {
                                    1 -> context.getString(R.string.set_toast_tts_device)
                                    2 -> context.getString(R.string.set_toast_tts_english)
                                    else -> context.getString(R.string.set_toast_tts_auto)
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                SwitchRow(
                    text = stringResource(R.string.set_private),
                    description = if (toneOnlyMode) {
                        stringResource(R.string.set_disabled_tone)
                    } else {
                        stringResource(R.string.set_private_desc)
                    },
                    checked = privatePlaybackEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && needsBluetoothConnectPermission(context)) {
                            bluetoothPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            viewModel.setPrivatePlaybackEnabled(enabled)
                            Toast.makeText(
                                context,
                                if (enabled) context.getString(R.string.set_toast_private_on) else context.getString(R.string.set_toast_private_off),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = !toneOnlyMode
                )

                SwitchRow(
                    text = stringResource(R.string.set_tone_only),
                    description = toneOnlySubtitle,
                    checked = toneOnlyMode,
                    onCheckedChange = {
                        viewModel.setToneOnlyMode(it)
                        Toast.makeText(
                            context,
                            if (it) context.getString(R.string.set_toast_tone_on) else context.getString(R.string.set_toast_tone_off),
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
                                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, context.getString(R.string.set_select_tone))
                            }
                        )
                    },
                    onUseDefault = {
                        viewModel.setToneOnlyAlertToneUri(null)
                        Toast.makeText(context, context.getString(R.string.set_toast_default_tone), Toast.LENGTH_SHORT).show()
                    }
                )

                SwitchRow(
                    text = stringResource(R.string.set_fullscreen),
                    description = if (fullScreenAccessGranted) {
                        stringResource(R.string.set_fullscreen_desc_granted)
                    } else {
                        stringResource(R.string.set_fullscreen_desc_denied)
                    },
                    checked = fullScreenAlertEnabled,
                    onCheckedChange = {
                        viewModel.setFullScreenAlertEnabled(it)
                        if (it && !fullScreenAccessGranted) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.set_allow_fullscreen),
                                Toast.LENGTH_SHORT
                            ).show()
                            FullScreenIntentSupport.openSettings(context)
                        }
                        Toast.makeText(
                            context,
                            if (it) context.getString(R.string.set_toast_fs_on) else context.getString(R.string.set_toast_fs_off),
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
                                text = stringResource(R.string.set_fs_off_title),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.set_fs_off_msg),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = { FullScreenIntentSupport.openSettings(context) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(stringResource(R.string.set_open_perm))
                            }
                        }
                    }
                }

                if (toneOnlyMode) {
                    Text(
                        text = stringResource(R.string.set_fs_note),
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
                    Text(stringResource(R.string.set_volume), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(60.dp))
                    val volumeCd = stringResource(R.string.set_cd_volume, (appVolume * 100).toInt())
                    Slider(
                        value = appVolume,
                        onValueChange = { viewModel.setAppVolume(it) },
                        valueRange = 0f..1f,
                        steps = 9,
                        enabled = !toneOnlyMode,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = volumeCd },
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
                        text = stringResource(R.string.set_tone_volume_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                 
                // Loop Auto-Stop (compact inline)
                Text(stringResource(R.string.set_loop_duration), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                Text(
                    stringResource(R.string.set_loop_inline_desc),
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
                            text = stringResource(R.string.set_minutes_short, mins),
                            isSelected = loopTimeoutMinutes == mins,
                            onClick = {
                                viewModel.setLoopTimeoutMinutes(mins)
                                Toast.makeText(context, context.getString(R.string.set_toast_loop, mins), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Infinite option
                    SnoozeOptionChip(
                        text = stringResource(R.string.set_infinite),
                        isSelected = loopTimeoutMinutes == 0,
                        onClick = { 
                            viewModel.setLoopTimeoutMinutes(0)
                            Toast.makeText(context, context.getString(R.string.set_toast_loop_infinite), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    var showCustomLoopDialog by remember { mutableStateOf(false) }
                    SnoozeOptionChip(
                        text = if (isCustom) stringResource(R.string.set_minutes_short, loopTimeoutMinutes) else stringResource(R.string.set_custom),
                        isSelected = isCustom,
                        onClick = { showCustomLoopDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    if (showCustomLoopDialog) {
                        CustomDurationDialog(
                            title = stringResource(R.string.set_loop_duration),
                            initialValue = if (isCustom) loopTimeoutMinutes else 20,
                            maxMinutes = 1440,
                            description = stringResource(R.string.set_loop_duration_desc),
                            onDismiss = { showCustomLoopDialog = false },
                            onSave = {
                                viewModel.setLoopTimeoutMinutes(it)
                                Toast.makeText(context, context.getString(R.string.set_toast_loop, it), Toast.LENGTH_SHORT).show()
                                showCustomLoopDialog = false
                            }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Snooze Duration (moved from Timing)
                Text(stringResource(R.string.set_default_snooze), style = MaterialTheme.typography.bodyMedium)
                Text(
                    stringResource(R.string.set_snooze_inline_desc),
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
                            text = stringResource(R.string.set_minutes_short, mins),
                            isSelected = !isCustom && defaultSnoozeDuration == mins,
                            onClick = {
                                viewModel.setDefaultSnoozeDuration(mins)
                                Toast.makeText(context, context.getString(R.string.set_toast_snooze, mins), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    var showCustomSnoozeDialog by remember { mutableStateOf(false) }
                    SnoozeOptionChip(
                        text = if (isCustom) stringResource(R.string.set_minutes_short, defaultSnoozeDuration) else stringResource(R.string.set_custom),
                        isSelected = isCustom,
                        onClick = { showCustomSnoozeDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    if (showCustomSnoozeDialog) {
                        CustomDurationDialog(
                            title = stringResource(R.string.set_custom_snooze),
                            initialValue = if (isCustom) defaultSnoozeDuration else 20,
                            maxMinutes = 240,
                            description = stringResource(R.string.set_snooze_desc),
                            onDismiss = { showCustomSnoozeDialog = false },
                            onSave = {
                                viewModel.setDefaultSnoozeDuration(it)
                                Toast.makeText(context, context.getString(R.string.set_toast_snooze, it), Toast.LENGTH_SHORT).show()
                                showCustomSnoozeDialog = false
                            }
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Default follow-up check
                Text(stringResource(R.string.set_default_followup), style = MaterialTheme.typography.bodyMedium)
                Text(
                    if (defaultFollowUpMinutes == 0) {
                        stringResource(R.string.set_followup_off_desc)
                    } else {
                        stringResource(R.string.set_followup_on_desc, defaultFollowUpMinutes)
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
                            if (minutes == 0) context.getString(R.string.set_toast_followup_off) else context.getString(R.string.set_toast_followup, minutes),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.padding(top = 4.dp)
                )

                // How many times a follow-up check repeats before it stops on its own.
                val followUpMaxRepeats by viewModel.followUpMaxRepeats.collectAsState()
                Text(
                    text = stringResource(R.string.set_followup_repeat),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = stringResource(R.string.set_followup_repeat_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 3, 5, 10).forEach { count ->
                        SnoozeOptionChip(
                            text = "$count×",
                            isSelected = followUpMaxRepeats == count,
                            onClick = {
                                viewModel.setFollowUpMaxRepeats(count)
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.set_toast_followup_repeat, count),
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Quiet Time (moved from Timing)
                SwitchRow(
                    text = stringResource(R.string.set_quiet),
                    description = if (quietTimeEnabled) "${formatTime(startHour, startMinute)} - ${formatTime(endHour, endMinute)}" else stringResource(R.string.set_quiet_desc),
                    checked = quietTimeEnabled,
                    onCheckedChange = {
                        viewModel.setQuietTimeEnabled(it)
                        Toast.makeText(context, if (it) context.getString(R.string.set_toast_quiet_on) else context.getString(R.string.set_toast_quiet_off), Toast.LENGTH_SHORT).show()
                    }
                )
                
                if (quietTimeEnabled) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TimePickerButton(
                            label = stringResource(R.string.set_start),
                            hour = startHour,
                            minute = startMinute,
                            onTimeSelected = { h, m ->
                                viewModel.setQuietTimeStart(h, m)
                                Toast.makeText(context, context.getString(R.string.set_toast_quiet_start, formatTime(h, m)), Toast.LENGTH_SHORT).show()
                            }
                        )

                        TimePickerButton(
                            label = stringResource(R.string.set_end),
                            hour = endHour,
                            minute = endMinute,
                            onTimeSelected = { h, m ->
                                viewModel.setQuietTimeEnd(h, m)
                                Toast.makeText(context, context.getString(R.string.set_toast_quiet_end, formatTime(h, m)), Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                // Test Reminder Button (moved here from separate section)
                OutlinedButton(
                    onClick = {
                        viewModel.scheduleTestReminder()
                        Toast.makeText(context, context.getString(R.string.set_toast_test), Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.set_test_reminder))
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
                            contentDescription = stringResource(R.string.set_cd_granted),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(R.string.set_all_granted), style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    // Expanded error state with fix buttons
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = stringResource(R.string.set_cd_action_required),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(stringResource(R.string.set_some_needed), style = MaterialTheme.typography.titleSmall)
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            PermissionRow(
                                name = stringResource(R.string.set_notifications),
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
                                name = stringResource(R.string.set_exact_alarms),
                                granted = false,
                                onFix = {
                                    val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                    context.startActivity(intent)
                                }
                            )
                        }
                        
                        if (!hasMicPerm) {
                            PermissionRow(
                                name = stringResource(R.string.set_microphone),
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
                title = stringResource(R.string.set_reliability),
                icon = "Safe",
                initiallyExpanded = true
            ) {
                val persistUntilDone by viewModel.persistUntilDone.collectAsState()
                SwitchRow(
                    text = stringResource(R.string.set_persist),
                    description = stringResource(R.string.set_persist_desc),
                    checked = persistUntilDone,
                    onCheckedChange = {
                        viewModel.setPersistUntilDone(it)
                        Toast.makeText(
                            context,
                            if (it) context.getString(R.string.set_toast_persist_on) else context.getString(R.string.set_toast_persist_off),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )

                SwitchRow(
                    text = stringResource(R.string.set_dnd),
                    description = when {
                        !dndBypassEnabled -> stringResource(R.string.set_dnd_desc_off)
                        dndPolicyAccessGranted -> stringResource(R.string.set_dnd_desc_active)
                        else -> stringResource(R.string.set_dnd_desc_needed)
                    },
                    checked = dndBypassEnabled,
                    onCheckedChange = {
                        viewModel.setDndBypassEnabled(it)
                        NotificationHelper(context).refreshChannels(dndBypassEnabled = it)
                        Toast.makeText(
                            context,
                            if (it) context.getString(R.string.set_toast_dnd_on) else context.getString(R.string.set_toast_dnd_off),
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
                                    context.getString(R.string.set_dnd_unavailable),
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
                        Text(stringResource(R.string.set_grant_dnd))
                    }
                    Text(
                        stringResource(R.string.set_dnd_note, APP_DISPLAY_NAME),
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
                                text = stringResource(R.string.set_battery_guide),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (batteryOptimizationEnabled) {
                                    stringResource(R.string.set_battery_desc)
                                } else {
                                    stringResource(R.string.set_battery_desc_ok, APP_DISPLAY_NAME)
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
                title = stringResource(R.string.set_wear),
                icon = "Wear",
                initiallyExpanded = false
            ) {
                val notificationsReady =
                    appNotificationStatus.appNotificationsEnabled && appNotificationStatus.reminderChannelEnabled
                val statusText = when {
                    isCheckingWearStatus -> stringResource(R.string.set_wear_checking)
                    wearConnectionInfo.isConnected && wearConnectionInfo.connectedNodeCount > 1 -> {
                        stringResource(R.string.set_wear_watches, wearConnectionInfo.connectedNodeCount)
                    }
                    wearConnectionInfo.isConnected -> stringResource(R.string.set_wear_connected)
                    else -> stringResource(R.string.set_wear_none)
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
                            text = stringResource(R.string.set_wear_supported, APP_DISPLAY_NAME),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (wearConnectionInfo.isConnected && !notificationsReady) {
                            Text(
                                text = if (!appNotificationStatus.appNotificationsEnabled) {
                                    stringResource(R.string.set_wear_notif_off)
                                } else {
                                    stringResource(R.string.set_wear_channel_muted, APP_DISPLAY_NAME)
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
                                Text(stringResource(R.string.set_open_notif))
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
                                    Text(stringResource(R.string.set_open_channel))
                                }
                            }
                        } else if (wearConnectionInfo.isConnected) {
                            Text(
                                text = stringResource(R.string.set_wear_enabled),
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
                            Text(stringResource(R.string.set_refresh))
                        }
                    }
                }
            }

            // SECTION 6: HELP & GUIDE
            // ============================================================
            CollapsibleSettingsSection(
                title = stringResource(R.string.set_app_guide),
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
                                stringResource(R.string.set_app_guide),
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
                                    context.getString(R.string.set_toast_no_email),
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
                            stringResource(R.string.set_contact_support),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.set_contact_support_desc),
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
                                    context.getString(R.string.set_toast_no_rating),
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
                            stringResource(R.string.set_rate, APP_DISPLAY_NAME),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            stringResource(R.string.set_rate_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null)
                }
            }

            // ============================================================
            // SECTION 6: MORE APPS FROM THE DEVELOPER
            // ============================================================
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.set_more_apps_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(R.string.set_more_apps_desc, APP_DISPLAY_NAME),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    MoreAppRow(
                        name = stringResource(R.string.app_pettibox_name),
                        description = stringResource(R.string.app_pettibox_desc),
                        packageName = "com.ghostgramlabs.pettibox",
                        context = context
                    )
                    MoreAppRow(
                        name = stringResource(R.string.app_directserve_name),
                        description = stringResource(R.string.app_directserve_desc),
                        packageName = "com.ghostgramlabs.directserve",
                        context = context
                    )
                }
            }

            // Debug Section - ONLY for Debug builds (hidden from most users)
            if (com.ghostgramlabs.speakalert.BuildConfig.DEBUG) {
                CollapsibleSettingsSection(
                    title = stringResource(R.string.set_developer),
                    icon = "Dev",
                    initiallyExpanded = false
                ) {
                    SwitchRow(
                        text = stringResource(R.string.set_debug_logging),
                        description = stringResource(R.string.set_debug_desc),
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
                            Text(stringResource(R.string.set_send_logs))
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
                    Text(stringResource(R.string.set_fix))
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
                    contentDescription = if (isExpanded) stringResource(R.string.set_cd_collapse) else stringResource(R.string.set_cd_expand),
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

@Composable
private fun MoreAppRow(
    name: String,
    description: String,
    packageName: String,
    context: android.content.Context
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!openPlayStoreListing(context, packageName)) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.set_toast_no_store),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null)
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
        val onLabel = stringResource(R.string.state_on)
        val offLabel = stringResource(R.string.state_off)
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
                    stateDescription = if (checked) onLabel else offLabel
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
                text = stringResource(R.string.set_tone_sound),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = if (enabled) {
                    stringResource(R.string.set_tone_sound_active, currentToneLabel)
                } else {
                    stringResource(R.string.set_tone_sound_hint)
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
                    Text(stringResource(R.string.set_choose_tone))
                }
                if (hasCustomTone) {
                    OutlinedButton(
                        onClick = onUseDefault,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(stringResource(R.string.set_use_default))
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
    if (uriString.isNullOrBlank()) return context.getString(R.string.set_default_alarm_tone)
    val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return context.getString(R.string.set_default_alarm_tone)
    val ringtone = runCatching { RingtoneManager.getRingtone(context, uri) }.getOrNull()
    return ringtone?.getTitle(context) ?: context.getString(R.string.set_tone_unavailable)
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
        Text(stringResource(R.string.set_label_time, label, formatTime(hour, minute)))
    }

    if (showPicker) {
        if (shouldUseSystemDateTimePickers()) {
            SystemTimePickerDialog(
                initialHour = hour,
                initialMinute = minute,
                is24Hour = android.text.format.DateFormat.is24HourFormat(context),
                onDismiss = { showPicker = false },
                onConfirm = { h, m ->
                    onTimeSelected(h, m)
                    showPicker = false
                },
            )
        } else {
            val timeState = rememberTimePickerState(
                initialHour = hour,
                initialMinute = minute,
                is24Hour = android.text.format.DateFormat.is24HourFormat(context)
            )
            AlertDialog(
                onDismissRequest = { showPicker = false },
                title = { Text(stringResource(R.string.set_select_label_time, label)) },
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    Button(
                        onClick = {
                            onTimeSelected(timeState.hour, timeState.minute)
                            showPicker = false
                        }
                    ) {
                        Text(stringResource(R.string.action_apply))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPicker = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
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
                text = stringResource(R.string.set_desc_max, description, maxMinutes),
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
                label = { Text(stringResource(R.string.set_minutes)) },
                supportingText = { Text(stringResource(R.string.set_max_allowed, maxMinutes)) },
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
                Text(stringResource(R.string.action_save))
            }
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val amPm = if (hour < 12) "AM" else "PM"
    return String.format("%d:%02d %s", h, minute, amPm)
}
