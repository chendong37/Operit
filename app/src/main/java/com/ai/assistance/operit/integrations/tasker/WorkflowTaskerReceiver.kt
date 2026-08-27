package com.ai.assistance.operit.integrations.tasker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ai.assistance.operit.integrations.auth.ExternalIntegrationAuthenticator
import com.ai.assistance.operit.util.AppLogger
import com.ai.assistance.operit.data.repository.WorkflowRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Authenticated BroadcastReceiver for custom Intent workflow triggers.
 *
 * Tasker uses [WorkflowTaskerRunner] through the Tasker plugin library and does not pass
 * through this receiver. Keeping those paths separate prevents the public custom-Intent
 * surface from bypassing authentication or breaking Tasker's own configuration protocol.
 */
class WorkflowTaskerReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WorkflowTaskerReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action.isNullOrBlank()) {
            return
        }

        AppLogger.d(TAG, "Received workflow trigger broadcast for action: $action. Checking for matching workflows.")

        // Use goAsync to allow async work
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (!ExternalIntegrationAuthenticator.isAuthorized(context, intent)) {
                    AppLogger.w(TAG, "Rejected workflow trigger with missing or invalid auth_token")
                    return@launch
                }
                val sanitizedIntent = Intent(intent).apply {
                    removeExtra(ExternalIntegrationAuthenticator.EXTRA_AUTH_TOKEN)
                }
                val repository = WorkflowRepository(context.applicationContext)
                // New method to find and trigger workflows based on the intent's content (action, extras, etc.)
                repository.triggerWorkflowsByIntentEvent(sanitizedIntent)
                AppLogger.d(TAG, "Finished processing intent trigger.")
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error processing intent trigger for workflows", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

/**
 * BroadcastReceiver for boot completed event
 * 
 * Re-schedules all enabled workflows after device reboot
 */
class WorkflowBootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WorkflowBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        AppLogger.d(TAG, "Device booted, rescheduling workflows")

        // Use goAsync to allow async work
        val pendingResult = goAsync()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = WorkflowRepository(context.applicationContext)
                val result = repository.getAllWorkflows()
                
                result.getOrNull()?.forEach { workflow ->
                    if (workflow.enabled) {
                        repository.scheduleWorkflow(workflow.id)
                        AppLogger.d(TAG, "Rescheduled workflow: ${workflow.name}")
                    }
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Error rescheduling workflows after boot", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
