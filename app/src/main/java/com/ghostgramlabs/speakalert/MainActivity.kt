package com.ghostgramlabs.speakalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.isSystemInDarkTheme
import com.ghostgramlabs.speakalert.ui.theme.VoiceReminderTheme
import androidx.activity.enableEdgeToEdge
import com.ghostgramlabs.speakalert.ui.navigation.VoiceReminderNavGraph
import com.ghostgramlabs.speakalert.util.BatteryOptimizationSupport
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
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
            val themeMode by app.container.settingsRepository.themeMode.collectAsState(initial = 0)
            val batteryOptimizationPromptShown by settingsRepository
                .batteryOptimizationPromptShown
                .collectAsState(initial = false)
            val coroutineScope = rememberCoroutineScope()
            var showBatteryOptimizationDialog by rememberSaveable { mutableStateOf(false) }
            
            val isDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true  // Dark
                else -> isSystemInDarkTheme() // System
            }

            LaunchedEffect(batteryOptimizationPromptShown) {
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
                        startAddEdit = openAddEdit
                    )

                    if (showBatteryOptimizationDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showBatteryOptimizationDialog = false
                                coroutineScope.launch {
                                    settingsRepository.setBatteryOptimizationPromptShown(true)
                                }
                            },
                            title = {
                                Text("Allow SpeakAlert to run in background")
                            },
                            text = {
                                Text(
                                    "Some phones (especially Xiaomi/POCO) stop apps from running in the background to save battery. This can prevent reminders from triggering. Please allow SpeakAlert to ignore battery optimization for reliable reminders."
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showBatteryOptimizationDialog = false
                                        coroutineScope.launch {
                                            settingsRepository.setBatteryOptimizationPromptShown(true)
                                        }
                                        BatteryOptimizationSupport.requestIgnoreBatteryOptimizations(this@MainActivity)
                                    }
                                ) {
                                    Text("Allow")
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showBatteryOptimizationDialog = false
                                        coroutineScope.launch {
                                            settingsRepository.setBatteryOptimizationPromptShown(true)
                                        }
                                    }
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
