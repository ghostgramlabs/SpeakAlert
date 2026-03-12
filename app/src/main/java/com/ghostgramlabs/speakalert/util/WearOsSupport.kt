package com.ghostgramlabs.speakalert.util

import android.content.Context
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.TimeUnit

data class WearOsConnectionInfo(
    val isConnected: Boolean,
    val connectedNodeCount: Int = 0
)

object WearOsSupport {

    fun getConnectionInfo(context: Context): WearOsConnectionInfo {
        return try {
            val nodes = Tasks.await(
                Wearable.getNodeClient(context).connectedNodes,
                2,
                TimeUnit.SECONDS
            )
            WearOsConnectionInfo(
                isConnected = nodes.isNotEmpty(),
                connectedNodeCount = nodes.size
            )
        } catch (_: Exception) {
            WearOsConnectionInfo(isConnected = false, connectedNodeCount = 0)
        }
    }
}
