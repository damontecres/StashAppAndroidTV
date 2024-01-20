package com.github.damontecres.stashapp

import android.content.Context
import androidx.core.content.edit
import com.github.damontecres.stashapp.api.ConfigurationQuery

/**
 * Represents configuration that users have set server-side
 *
 * Configuration is loaded into a SharedPreferences and made available throughout the app
 */
class ServerPreferences(context: Context) {
    val preferences =
        context.getSharedPreferences(
            context.packageName + "_server_preferences",
            Context.MODE_PRIVATE,
        )

    val trackActivity get() = preferences.getBoolean(PREF_TRACK_ACTIVITY, false)

    /**
     * Update the local preferences from the server configuration
     */
    fun updatePreferences(config: ConfigurationQuery.Configuration?) {
        if (config != null) {
            val ui = config.ui as Map<String, *>
            preferences.edit {
                putBoolean(
                    PREF_TRACK_ACTIVITY,
                    (ui.getCaseInsensitive(PREF_TRACK_ACTIVITY) as Boolean?) ?: false,
                )
            }
        }
    }

    companion object {
        const val PREF_TRACK_ACTIVITY = "trackActivity"
    }
}
