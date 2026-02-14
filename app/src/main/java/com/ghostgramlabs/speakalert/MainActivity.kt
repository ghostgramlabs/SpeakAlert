package com.ghostgramlabs.speakalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.isSystemInDarkTheme
import com.ghostgramlabs.speakalert.ui.theme.VoiceReminderTheme
import androidx.activity.enableEdgeToEdge
import com.ghostgramlabs.speakalert.ui.navigation.VoiceReminderNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Read intent extras for notification tap handling
        val reminderId = intent.getLongExtra("reminderId", -1L)
        val autoplay = intent.getBooleanExtra("autoplay", false)
        
        setContent {
            val app = applicationContext as VoiceReminderApp
            val themeMode by app.container.settingsRepository.themeMode.collectAsState(initial = 0)
            
            val isDarkTheme = when (themeMode) {
                1 -> false // Light
                2 -> true  // Dark
                else -> isSystemInDarkTheme() // System
            }

            VoiceReminderTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    VoiceReminderNavGraph(
                        startReminderId = if (reminderId != -1L) reminderId else null,
                        autoplay = autoplay
                    )
                }
            }
        }
    }
}
