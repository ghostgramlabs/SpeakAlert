package com.ghostgramlabs.speakalert

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.ghostgramlabs.speakalert.ui.theme.VoiceReminderTheme
import com.ghostgramlabs.speakalert.ui.navigation.VoiceReminderNavGraph
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.BatteryOptimizationSupport
import com.ghostgramlabs.speakalert.util.FullScreenIntentSupport
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Read intent extras for notification tap handling
        val reminderId = intent.getLongExtra("reminderId", -1L)
        val autoplay = intent.getBooleanExtra("autoplay", false)
        val openAddEdit = intent.getBooleanExtra("openAddEdit", false)
        
        setContent {
            val app = applicationContext as VoiceReminderApp
            val settingsRepository = app.container.settingsRepository
            val currentVersionName = BuildConfig.VERSION_NAME
            val themeMode by app.container.settingsRepository.themeMode.collectAsState(initial = 0)
            val fullScreenAlertEnabled by settingsRepository.fullScreenAlertEnabled.collectAsState(initial = false)
            val startupPromptState by produceState<StartupPromptState?>(
                initialValue = null,
                key1 = settingsRepository
            ) {
                settingsRepository.batteryOptimizationPromptShown
                    .combine(settingsRepository.lastWhatsNewVersionShown) { batteryPromptShown, lastVersionShown ->
                        StartupPromptState(
                            batteryOptimizationPromptShown = batteryPromptShown,
                            lastWhatsNewVersionShown = lastVersionShown
                        )
                    }
                    .collect { value = it }
            }
            val coroutineScope = rememberCoroutineScope()
            val lifecycleOwner = LocalLifecycleOwner.current
            var showBatteryOptimizationDialog by rememberSaveable { mutableStateOf(false) }
            var showWhatsNewSheet by rememberSaveable { mutableStateOf(false) }
            var showFullScreenRecoveryDialog by rememberSaveable { mutableStateOf(false) }
            var fullScreenAccessGranted by rememberSaveable {
                mutableStateOf(FullScreenIntentSupport.canUseFullScreenIntent(this@MainActivity))
            }
            val startupPromptsLoaded = startupPromptState != null
            val batteryOptimizationPromptShown = startupPromptState?.batteryOptimizationPromptShown ?: false
            val lastWhatsNewVersionShown = startupPromptState?.lastWhatsNewVersionShown
            val shouldOfferWhatsNew = reminderId == -1L && !autoplay && !openAddEdit
            val needsWhatsNew =
                startupPromptsLoaded &&
                    shouldOfferWhatsNew &&
                    lastWhatsNewVersionShown != currentVersionName
            val allowHomeStartupOverlays =
                startupPromptsLoaded &&
                    !needsWhatsNew &&
                    !showWhatsNewSheet &&
                    !showBatteryOptimizationDialog &&
                    batteryOptimizationPromptShown
            
            val isDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true  // Dark
                else -> isSystemInDarkTheme() // System
            }

            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        fullScreenAccessGranted =
                            FullScreenIntentSupport.canUseFullScreenIntent(this@MainActivity)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(startupPromptsLoaded, needsWhatsNew) {
                if (!startupPromptsLoaded || !needsWhatsNew) return@LaunchedEffect
                showWhatsNewSheet = true
            }

            LaunchedEffect(startupPromptsLoaded, batteryOptimizationPromptShown, needsWhatsNew, showWhatsNewSheet) {
                if (!startupPromptsLoaded) return@LaunchedEffect
                if (needsWhatsNew || showWhatsNewSheet) return@LaunchedEffect
                if (batteryOptimizationPromptShown) return@LaunchedEffect
                if (!BatteryOptimizationSupport.isBatteryOptimizationEnabled(this@MainActivity)) {
                    settingsRepository.setBatteryOptimizationPromptShown(true)
                    return@LaunchedEffect
                }
                showBatteryOptimizationDialog = true
            }

            LaunchedEffect(
                allowHomeStartupOverlays,
                shouldOfferWhatsNew,
                fullScreenAlertEnabled,
                fullScreenAccessGranted
            ) {
                if (!allowHomeStartupOverlays || !shouldOfferWhatsNew) {
                    showFullScreenRecoveryDialog = false
                    return@LaunchedEffect
                }
                // Auto Backup can restore the in-app toggle after reinstall while Android
                // resets the special full-screen access. Prompt for recovery instead of
                // leaving the feature silently broken until the user toggles it manually.
                showFullScreenRecoveryDialog =
                    fullScreenAlertEnabled && !fullScreenAccessGranted
            }

            VoiceReminderTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceReminderNavGraph(
                        startReminderId = if (reminderId != -1L) reminderId else null,
                        autoplay = autoplay,
                        startAddEdit = openAddEdit,
                        allowHomeStartupOverlays = allowHomeStartupOverlays
                    )

                    if (showBatteryOptimizationDialog) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = {
                                showBatteryOptimizationDialog = false
                                coroutineScope.launch {
                                    settingsRepository.setBatteryOptimizationPromptShown(true)
                                }
                            },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            dragHandle = { BottomSheetDefaults.DragHandle() }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .navigationBarsPadding()
                                    .padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Allow $APP_DISPLAY_NAME to run in background",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Some phones, especially Xiaomi and POCO models, stop apps from running in the background to save battery. This can prevent reminders from triggering. Allow $APP_DISPLAY_NAME to ignore battery optimization for more reliable reminders.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        showBatteryOptimizationDialog = false
                                        coroutineScope.launch {
                                            settingsRepository.setBatteryOptimizationPromptShown(true)
                                        }
                                        val opened = BatteryOptimizationSupport.requestIgnoreBatteryOptimizations(this@MainActivity)
                                        if (!opened) {
                                            Toast.makeText(
                                                this@MainActivity,
                                                "Battery settings are not available on this device.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Allow")
                                }
                                OutlinedButton(
                                    onClick = {
                                        showBatteryOptimizationDialog = false
                                        coroutineScope.launch {
                                            settingsRepository.setBatteryOptimizationPromptShown(true)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Later")
                                }
                            }
                        }
                    }

                    if (showWhatsNewSheet) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = {
                                showWhatsNewSheet = false
                                coroutineScope.launch {
                                    settingsRepository.setLastWhatsNewVersionShown(currentVersionName)
                                }
                            },
                            sheetState = sheetState,
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            dragHandle = { BottomSheetDefaults.DragHandle() }
                        ) {
                            val whatsNewScrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                                    .navigationBarsPadding()
                                    .padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .verticalScroll(whatsNewScrollState),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "What's new in $currentVersionName",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "SpeakAlert now gives you stronger repeating reminders, clearer save protection, and more control over how alerts play.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Repeat until a date and time",
                                        description = "Custom repeats can now end at an exact date and time, or after a set number of occurrences."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Follow-up checks keep asking",
                                        description = "Follow-up reminders repeat every selected interval until the reminder is marked done."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Duplicate reminders",
                                        description = "Use the copy button on a reminder card to create a new reminder with the same audio, repeat, and follow-up settings."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Save protection",
                                        description = "The reminder editor now has a visible Save action at the top and warns before leaving with unsaved changes."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "DND alerts and private playback",
                                        description = "Alert even during DND is clearer about Android permission, and earpiece playback can switch near your ear when supported."
                                    )
                                }
                                Button(
                                    onClick = {
                                        showWhatsNewSheet = false
                                        coroutineScope.launch {
                                            settingsRepository.setLastWhatsNewVersionShown(currentVersionName)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text("Continue")
                                }
                            }
                        }
                    }

                    if (showFullScreenRecoveryDialog) {
                        AlertDialog(
                            onDismissRequest = { showFullScreenRecoveryDialog = false },
                            title = {
                                Text("Allow full-screen alerts again")
                            },
                            text = {
                                Text(
                                    "Lock-screen reminder alerts are still turned on in $APP_DISPLAY_NAME, but Android full-screen access is off. This can happen after reinstall or restore."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showFullScreenRecoveryDialog = false
                                        FullScreenIntentSupport.openSettings(this@MainActivity)
                                    }
                                ) {
                                    Text("Open settings")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showFullScreenRecoveryDialog = false }
                                ) {
                                    Text("Later")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private data class StartupPromptState(
    val batteryOptimizationPromptShown: Boolean,
    val lastWhatsNewVersionShown: String?
)

@Composable
private fun WhatsNewFeatureCard(
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
