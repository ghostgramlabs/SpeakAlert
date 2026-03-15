package com.ghostgramlabs.speakalert

import android.os.Bundle
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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import com.ghostgramlabs.speakalert.ui.theme.VoiceReminderTheme
import com.ghostgramlabs.speakalert.ui.navigation.VoiceReminderNavGraph
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.BatteryOptimizationSupport
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
            var showBatteryOptimizationDialog by rememberSaveable { mutableStateOf(false) }
            var showWhatsNewSheet by rememberSaveable { mutableStateOf(false) }
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
                                        BatteryOptimizationSupport.requestIgnoreBatteryOptimizations(this@MainActivity)
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
                                        text = "SpeakAlert now includes faster ways to add reminders, stronger playback options, and more control over alert behavior.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Home screen widgets",
                                        description = "Add Quick Reminder or Upcoming Reminders from your phone's Widgets screen for faster access."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Audio file selection",
                                        description = "Choose an existing audio file instead of recording a new voice note when that fits better."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Lock-screen full-screen alert",
                                        description = "Turn it on in Settings for a stronger alert experience when reminders fire."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Follow-up and missed reminders",
                                        description = "Use follow-up checks and review missed reminders later without losing track."
                                    )
                                    WhatsNewFeatureCard(
                                        title = "Tone-only sound selection",
                                        description = "Choose a sound for Tone-only mode in Settings. If it is unavailable later, SpeakAlert falls back to the default alarm tone."
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
