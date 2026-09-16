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
import androidx.compose.ui.res.stringResource
import com.ghostgramlabs.speakalert.R

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

    val noBatterySettings = stringResource(R.string.bog_no_battery_settings)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bog_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.action_back))
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
                title = stringResource(R.string.bog_header),
                subtitle = stringResource(R.string.bog_header_sub, APP_DISPLAY_NAME),
                eyebrow = stringResource(R.string.bog_title)
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
                        text = stringResource(R.string.bog_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.bog_menu_vary),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (optimizationEnabled) {
                            stringResource(R.string.bog_status_enabled, APP_DISPLAY_NAME)
                        } else {
                            stringResource(R.string.bog_status_exempt, APP_DISPLAY_NAME)
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
                                        noBatterySettings,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                optimizationEnabled = BatteryOptimizationSupport.isBatteryOptimizationEnabled(context)
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(stringResource(R.string.bog_allow_app, APP_DISPLAY_NAME))
                        }
                    }
                }
            }

            Text(
                text = stringResource(R.string.bog_brand_xiaomi),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            BatteryGuideStep(number = stringResource(R.string.bog_step, 1), text = stringResource(R.string.bog_open_settings))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 2), text = stringResource(R.string.bog_xiaomi_battery))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 3), text = stringResource(R.string.bog_xiaomi_optimization))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 4), text = stringResource(R.string.bog_find_app, APP_DISPLAY_NAME))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 5), text = stringResource(R.string.bog_xiaomi_no_restrictions))
            BatteryGuideStep(number = stringResource(R.string.bog_optional), text = stringResource(R.string.bog_xiaomi_opt))

            Text(
                text = stringResource(R.string.bog_brand_huawei),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            BatteryGuideStep(number = stringResource(R.string.bog_step, 1), text = stringResource(R.string.bog_open_settings))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 2), text = stringResource(R.string.bog_huawei_apps))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 3), text = stringResource(R.string.bog_find_app, APP_DISPLAY_NAME))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 4), text = stringResource(R.string.bog_huawei_manage_off))
            BatteryGuideStep(number = stringResource(R.string.bog_step, 5), text = stringResource(R.string.bog_huawei_allow))
            BatteryGuideStep(number = stringResource(R.string.bog_optional), text = stringResource(R.string.bog_huawei_opt, APP_DISPLAY_NAME))

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
                        text = stringResource(R.string.bog_why_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.bog_why_body),
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
                Text(stringResource(R.string.bog_back_settings))
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
