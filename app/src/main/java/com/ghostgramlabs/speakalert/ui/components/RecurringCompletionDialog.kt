package com.ghostgramlabs.speakalert.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun RecurringCompletionDialog(
    onDismiss: () -> Unit,
    onMarkTodayAsDone: () -> Unit,
    onStopCompletely: () -> Unit,
    onEditSchedule: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "This is a recurring reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "What would you like to do?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Option 1: Mark today as done
                Button(
                    onClick = onMarkTodayAsDone,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Done, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(horizontalAlignment = androidx.compose.ui.Alignment.Start) {
                         Text(
                             "Mark today as done", 
                             style = MaterialTheme.typography.titleSmall
                         )
                         Text(
                             "Next occurrence still scheduled", 
                             style = MaterialTheme.typography.bodySmall.copy(
                                 fontWeight = FontWeight.Normal
                             )
                         )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Option 2: Stop completely
                OutlinedButton(
                    onClick = onStopCompletely,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                         contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                     Spacer(modifier = Modifier.width(12.dp))
                     Column(horizontalAlignment = androidx.compose.ui.Alignment.Start) {
                         Text(
                             "Stop this reminder completely", 
                             style = MaterialTheme.typography.titleSmall
                         )
                         Text(
                             "Move to Completed tab", 
                             style = MaterialTheme.typography.bodySmall.copy(
                                 fontWeight = FontWeight.Normal
                             )
                         )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                
                // Option 3: Edit schedule
                TextButton(
                    onClick = onEditSchedule,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit schedule")
                }
                
                // Option 4: Cancel
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }
            }
        }
    }
}
