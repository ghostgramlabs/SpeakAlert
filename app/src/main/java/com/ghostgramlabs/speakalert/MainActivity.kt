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
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.BatteryOptimizationSupport
import com.ghostgramlabs.speakalert.util.FullScreenIntentSupport
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.ghostgramlabs.speakalert.util.AppLocale.wrapContext(newBase))
    }

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
            var showRatingPrompt by rememberSaveable { mutableStateOf(false) }
            var currentOpenCount by rememberSaveable { mutableStateOf(-1) }
            var ratingEvaluated by rememberSaveable { mutableStateOf(false) }
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

            // Count this app open once per process launch to pace the rating prompt.
            LaunchedEffect(Unit) {
                if (currentOpenCount < 0) {
                    currentOpenCount = settingsRepository.incrementAppOpenCount()
                }
            }

            // Ask for a rating occasionally once the user has some history with the app,
            // but only on a plain home launch and never while another prompt is showing.
            LaunchedEffect(allowHomeStartupOverlays, currentOpenCount) {
                if (ratingEvaluated) return@LaunchedEffect
                if (!allowHomeStartupOverlays) return@LaunchedEffect
                if (!shouldOfferWhatsNew) return@LaunchedEffect
                if (currentOpenCount < 0) return@LaunchedEffect
                if (settingsRepository.ratingPromptDecided.first()) {
                    ratingEvaluated = true
                    return@LaunchedEffect
                }
                val lastOpen = settingsRepository.ratingPromptLastOpen.first()
                val eligible = currentOpenCount >= 4 && (currentOpenCount - lastOpen) >= 3
                ratingEvaluated = true
                if (eligible && Random.nextInt(100) < 50) {
                    settingsRepository.setRatingPromptLastOpen(currentOpenCount)
                    showRatingPrompt = true
                }
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
                                    text = stringResource(R.string.batt_prompt_title, APP_DISPLAY_NAME),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.batt_prompt_message, APP_DISPLAY_NAME),
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
                                                getString(R.string.batt_toast_unavailable),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(stringResource(R.string.action_allow))
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
                                    Text(stringResource(R.string.action_later))
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
                                        text = stringResource(R.string.wn_title, currentVersionName),
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.wn_intro),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    WhatsNewFeatureCard(
                                        title = stringResource(R.string.wn_lang_title),
                                        description = stringResource(R.string.wn_lang_desc)
                                    )
                                    WhatsNewFeatureCard(
                                        title = stringResource(R.string.wn_buttons_title),
                                        description = stringResource(R.string.wn_buttons_desc)
                                    )
                                    WhatsNewFeatureCard(
                                        title = stringResource(R.string.wn_spoken_title),
                                        description = stringResource(R.string.wn_spoken_desc)
                                    )
                                    WhatsNewFeatureCard(
                                        title = stringResource(R.string.wn_persist_title),
                                        description = stringResource(R.string.wn_persist_desc)
                                    )
                                    WhatsNewFeatureCard(
                                        title = stringResource(R.string.wn_followup_title),
                                        description = stringResource(R.string.wn_followup_desc)
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
                                    Text(stringResource(R.string.action_continue))
                                }
                            }
                        }
                    }

                    if (showRatingPrompt) {
                        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                        ModalBottomSheet(
                            onDismissRequest = { showRatingPrompt = false },
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
                                    text = stringResource(R.string.rate_prompt_title, APP_DISPLAY_NAME),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.rate_prompt_message, APP_DISPLAY_NAME),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        showRatingPrompt = false
                                        coroutineScope.launch {
                                            settingsRepository.setRatingPromptDecided(true)
                                        }
                                        openPlayStoreListing(
                                            this@MainActivity,
                                            this@MainActivity.packageName
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(stringResource(R.string.rate_prompt_positive))
                                }
                                OutlinedButton(
                                    onClick = { showRatingPrompt = false },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp)
                                ) {
                                    Text(stringResource(R.string.rate_prompt_later))
                                }
                                TextButton(
                                    onClick = {
                                        showRatingPrompt = false
                                        coroutineScope.launch {
                                            settingsRepository.setRatingPromptDecided(true)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.rate_prompt_never))
                                }
                            }
                        }
                    }

                    if (showFullScreenRecoveryDialog) {
                        AlertDialog(
                            onDismissRequest = { showFullScreenRecoveryDialog = false },
                            title = {
                                Text(stringResource(R.string.fsr_title))
                            },
                            text = {
                                Text(
                                    stringResource(R.string.fsr_message, APP_DISPLAY_NAME)
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showFullScreenRecoveryDialog = false
                                        FullScreenIntentSupport.openSettings(this@MainActivity)
                                    }
                                ) {
                                    Text(stringResource(R.string.action_open_settings))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showFullScreenRecoveryDialog = false }
                                ) {
                                    Text(stringResource(R.string.action_later))
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

/** Opens a Google Play listing, preferring the Play Store app and falling back to the browser. */
internal fun openPlayStoreListing(context: android.content.Context, packageName: String): Boolean {
    val marketIntent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("market://details?id=$packageName")
    ).apply {
        setPackage("com.android.vending")
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val webIntent = android.content.Intent(
        android.content.Intent.ACTION_VIEW,
        android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    ).apply {
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return when {
        marketIntent.resolveActivity(context.packageManager) != null -> {
            context.startActivity(marketIntent); true
        }
        webIntent.resolveActivity(context.packageManager) != null -> {
            context.startActivity(webIntent); true
        }
        else -> false
    }
}

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
