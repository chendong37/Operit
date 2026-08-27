package com.ai.assistance.operit.integrations.auth

import android.content.Context
import android.content.Intent
import com.ai.assistance.operit.data.preferences.ExternalHttpApiPreferences
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

/**
 * Shared authentication gate for exported automation entry points.
 *
 * The same user-managed bearer token protects HTTP, Intent chat, and Intent workflow
 * integrations. Keeping one credential avoids a second hidden secret and makes rotation
 * invalidate every external automation channel at once.
 */
object ExternalIntegrationAuthenticator {
    const val EXTRA_AUTH_TOKEN = "auth_token"

    suspend fun isAuthorized(context: Context, intent: Intent): Boolean {
        val expected =
            ExternalHttpApiPreferences.getInstance(context.applicationContext)
                .getConfig()
                .bearerToken
                .trim()
        val provided = intent.getStringExtra(EXTRA_AUTH_TOKEN)?.trim().orEmpty()
        if (expected.isEmpty() || provided.isEmpty()) {
            return false
        }
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            provided.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
