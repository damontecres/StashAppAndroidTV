package com.github.damontecres.stashapp.util.plugin

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.RunPluginTaskMutation
import com.github.damontecres.stashapp.di.server.ServerRepository.Companion.PREF_STASH_API_KEY
import com.github.damontecres.stashapp.di.server.ServerRepository.Companion.PREF_STASH_URL
import com.github.damontecres.stashapp.di.server.StashApi
import com.github.damontecres.stashapp.di.server.StashServer
import com.google.auto.service.AutoService
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.acra.config.CoreConfiguration
import org.acra.data.CrashReportData
import org.acra.sender.ReportSender
import org.acra.sender.ReportSenderException
import org.acra.sender.ReportSenderFactory

@AutoService(ReportSenderFactory::class)
class CrashReportSenderFactory : ReportSenderFactory {
    override fun create(
        context: Context,
        config: CoreConfiguration,
    ): ReportSender = CrashReportSender()

    override fun enabled(config: CoreConfiguration): Boolean = true

    class CrashReportSender : ReportSender {
        override fun send(
            context: Context,
            errorContent: CrashReportData,
        ) {
            Log.w(TAG, "Sending crash report")
            try {
                findConfiguredStashServer(context)?.let { server ->
                    val client = StashApi.createApolloClient(server, OkHttpClient())
                    val mutation =
                        RunPluginTaskMutation(
                            plugin_id = CompanionPluginService.PLUGIN_ID,
                            task_name = CompanionPluginService.CRASH_TASK_NAME,
                            args_map = mapOf(CompanionPluginService.CRASH_TASK_NAME to errorContent.toJSON()),
                        )
                    runBlocking {
                        val response = client.mutation(mutation).execute()
                        if (response.exception != null) {
                            throw ReportSenderException("Exception", response.exception!!)
                        } else if (response.hasErrors()) {
                            throw ReportSenderException(response.errors.toString())
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error while sending crash report", ex)
                throw ReportSenderException("Error while sending crash report ", ex)
            }
        }

        fun findConfiguredStashServer(context: Context): StashServer? {
            val manager = PreferenceManager.getDefaultSharedPreferences(context)
            val url = manager.getString(PREF_STASH_URL, null)
            val apiKey = manager.getString(PREF_STASH_API_KEY, null)
            return url?.let { StashServer(url, apiKey) }
        }
    }

    companion object {
        const val TAG = "CrashReportSenderFactory"
    }
}
