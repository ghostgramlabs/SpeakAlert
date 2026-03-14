package com.ghostgramlabs.speakalert.ui.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.ghostgramlabs.speakalert.ui.components.PremiumHeaderCard
import com.ghostgramlabs.speakalert.ui.components.PremiumScreenBackground
import com.ghostgramlabs.speakalert.util.APP_DISPLAY_NAME
import com.ghostgramlabs.speakalert.util.BatteryOptimizationSupport

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptimizationGuideScreen(
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var optimizationEnabled by remember {
        mutableStateOf(BatteryOptimizationSupport.isBatteryOptimizationEnabled(context))
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                optimizationEnabled = BatteryOptimizationSupport.isBatteryOptimizationEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("Battery Optimization Guide") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            PremiumHeaderCard(
                title = "Keep reminders reliable",
                subtitle = "Some phones pause or stop background alarms unless $APP_DISPLAY_NAME is exempt from battery restrictions.",
                eyebrow = "Battery Optimization Guide"
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (optimizationEnabled) Icons.Default.BatteryAlert else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (optimizationEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                    Text(
                        text = "Some devices stop background alarms to save battery. This can prevent reminders from triggering.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Menu names can vary by MIUI, HyperOS, EMUI, Android version, and phone model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (optimizationEnabled) {
                            "Battery optimization is still enabled for $APP_DISPLAY_NAME."
                        } else {
                            "$APP_DISPLAY_NAME is already allowed to run without battery optimization."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (optimizationEnabled) {
                        Button(
                            onClick = {
                                val opened = BatteryOptimizationSupport.requestIgnoreBatteryOptimizations(context)
                                if (!opened) {
                                    Toast.makeText(
                                        context,
                                        "Battery settings are not available on this device.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                optimizationEnabled = BatteryOptimizationSupport.isBatteryOptimizationEnabled(context)
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Allow $APP_DISPLAY_NAME")
                        }
                    }
                }
            }

            Text(
                text = "Xiaomi / POCO / Redmi (MIUI / HyperOS)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            BatteryGuideStep(number = "Step 1", text = "Open Settings")
            BatteryGuideStep(number = "Step 2", text = "Tap Battery")
            BatteryGuideStep(number = "Step 3", text = "Tap Battery Optimization")
            BatteryGuideStep(number = "Step 4", text = "Find $APP_DISPLAY_NAME")
            BatteryGuideStep(number = "Step 5", text = "Select No Restrictions")
            BatteryGuideStep(number = "Optional", text = "Lock the app in the recent apps screen.")

            Text(
                text = "Huawei (EMUI)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            BatteryGuideStep(number = "Step 1", text = "Open Settings")
            BatteryGuideStep(number = "Step 2", text = "Open Apps or App launch")
            BatteryGuideStep(number = "Step 3", text = "Find $APP_DISPLAY_NAME")
            BatteryGuideStep(number = "Step 4", text = "Turn off Manage automatically")
            BatteryGuideStep(number = "Step 5", text = "Allow auto-launch, secondary launch, and run in background")
            BatteryGuideStep(number = "Optional", text = "Also check Battery optimization and set $APP_DISPLAY_NAME to Don't allow.")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Why this matters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Phones from Xiaomi, POCO, Redmi, Huawei, and similar brands can aggressively stop apps in the background. Use the manufacturer family and software version as a guide; exact paths can differ by device model.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onNavigateUp,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Back to Settings")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
        }
    }
}

@Composable
private fun BatteryGuideStep(
    number: String,
    text: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = number,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
