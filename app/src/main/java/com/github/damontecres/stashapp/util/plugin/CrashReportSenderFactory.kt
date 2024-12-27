package com.github.damontecres.stashapp.util.plugin

import android.content.Context
import android.util.Log
import com.github.damontecres.stashapp.api.RunPluginTaskMutation
import com.github.damontecres.stashapp.util.StashServer
import com.google.auto.service.AutoService
import kotlinx.coroutines.runBlocking
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
                StashServer.findConfiguredStashServer(context)?.let { server ->
                    val client = server.apolloClient
                    val mutation =
                        RunPluginTaskMutation(
                            plugin_id = CompanionPlugin.PLUGIN_ID,
                            task_name = CompanionPlugin.CRASH_TASK_NAME,
                            args_map = mapOf(CompanionPlugin.CRASH_TASK_NAME to errorContent.toJSON()),
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
    }

    companion object {
        const val TAG = "CrashReportSenderFactory"
    }
}
