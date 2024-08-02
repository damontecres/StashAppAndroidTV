package com.github.damontecres.stashapp.util.plugin

import android.content.Context
import android.util.Log
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.RunPluginTaskMutation
import com.github.damontecres.stashapp.api.type.PluginArgInput
import com.github.damontecres.stashapp.api.type.PluginValueInput
import com.github.damontecres.stashapp.util.StashClient
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
    ): ReportSender {
        return CrashReportSender()
    }

    override fun enabled(config: CoreConfiguration): Boolean {
        return true
    }

    class CrashReportSender : ReportSender {
        override fun send(
            context: Context,
            errorContent: CrashReportData,
        ) {
            Log.w(TAG, "Sending crash report")
            try {
                val client = StashClient.getApolloClient(context)
                val mutation =
                    RunPluginTaskMutation(
                        plugin_id = CompanionPlugin.PLUGIN_ID,
                        task_name = CompanionPlugin.CRASH_TASK_NAME,
                        args =
                            listOf(
                                PluginArgInput(
                                    key = CompanionPlugin.CRASH_TASK_NAME,
                                    value =
                                        Optional.present(
                                            PluginValueInput(
                                                str = Optional.present(errorContent.toJSON()),
                                            ),
                                        ),
                                ),
                            ),
                    )
                runBlocking {
                    val response = client.mutation(mutation).execute()
                    if (response.hasErrors()) {
                        throw ReportSenderException(response.errors.toString())
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
