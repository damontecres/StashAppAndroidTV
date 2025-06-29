package com.github.damontecres.stashapp.util.plugin

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.RunPluginTaskMutation
import com.github.damontecres.stashapp.api.type.PackageSpecInput
import com.github.damontecres.stashapp.api.type.PackageType
import com.github.damontecres.stashapp.data.JobResult
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * Class for interacting with the server-side companion plugin
 */
class CompanionPlugin {
    companion object {
        private const val TAG = "CompanionPlugin"

        const val PLUGIN_ID = "stashAppAndroidTvCompanion"
        private const val SOURCE_URL =
            "https://stashapp.github.io/CommunityScripts/stable/index.yml"

        const val CRASH_TASK_NAME = "crash_report"
        const val LOGCAT_TASK_NAME = "logcat"

        suspend fun installPlugin(mutationEngine: MutationEngine): String =
            mutationEngine.installPackage(
                PackageType.Plugin,
                PackageSpecInput(id = PLUGIN_ID, sourceURL = SOURCE_URL),
            )

        fun getLogCatLines(verbose: Boolean): List<String> {
            val lineCount = if (verbose) 500 else 200
            val args =
                buildList {
                    add("logcat")
                    add("-d")
                    add("-t")
                    add(lineCount.toString())
                    if (verbose) {
                        addAll(THIRD_PARTY_TAGS)
                        add("*:V")
                    } else {
                        add("-s")
                        addAll(LOGCAT_TAGS)
                        addAll(THIRD_PARTY_TAGS)
                        add("*:E")
                    }
                }
            val process = ProcessBuilder().command(args).redirectErrorStream(true).start()
            val logLines = mutableListOf<String>()
            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var count = 0

                while (count < lineCount) {
                    val line = reader.readLine()
                    if (line != null) {
                        logLines.add(line)
                    } else {
                        break
                    }
                    count++
                }
            } finally {
                process.destroy()
            }
            return logLines
        }

        suspend fun sendLogCat(
            context: Context,
            server: StashServer,
            verbose: Boolean,
        ) = withContext(Dispatchers.IO + StashCoroutineExceptionHandler(autoToast = true)) {
            try {
                val lines = getLogCatLines(verbose)
                val sb = StringBuilder("** LOGCAT START **\n")
                sb.append(lines.joinToString("\n"))
                sb.append("\n** LOGCAT END **")

                val result = sendLogMessage(server, sb.toString(), false)
                withContext(Dispatchers.Main) {
                    when (result) {
                        is JobResult.Success -> {
                            val msg =
                                buildString {
                                    if (verbose) {
                                        append("Verbose logs sent!")
                                    } else {
                                        append("Logs sent!")
                                    }
                                    append(" Check the server's log page.")
                                }

                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }

                        is JobResult.NotFound -> {
                            Toast.makeText(context, "Error sending logs", Toast.LENGTH_LONG).show()
                        }

                        is JobResult.Failure -> {
                            Toast
                                .makeText(
                                    context,
                                    "Error sending logs: ${result.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    }
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Error sending logs", ex)
                withContext(Dispatchers.Main) {
                    Toast
                        .makeText(
                            context,
                            "Error sending logs: ${ex.message}",
                            Toast.LENGTH_LONG,
                        ).show()
                }
            }
        }

        suspend fun sendLogMessage(
            server: StashServer,
            message: String,
            isError: Boolean,
            convertNewLines: Boolean = true,
        ): JobResult =
            withContext(Dispatchers.IO + StashCoroutineExceptionHandler()) {
                if (server.serverPreferences.companionPluginInstalled) {
                    val userAgent = StashClient.createUserAgent(StashApplication.getApplication())
                    val ips = getIps().joinToString(", ")
                    val header = "Log from $ips - $userAgent\n"
                    val msg =
                        if (convertNewLines) {
                            (header + message).replace(Regex("\n"), "<br>")
                        } else {
                            header + message
                        }

                    val taskName = if (isError) CRASH_TASK_NAME else LOGCAT_TASK_NAME
                    val mutationEngine = MutationEngine(server)
                    val mutation =
                        RunPluginTaskMutation(
                            plugin_id = PLUGIN_ID,
                            task_name = taskName,
                            args_map = mapOf(taskName to msg),
                        )
                    val jobId = mutationEngine.executeMutation(mutation).data?.runPluginTask
                    return@withContext if (jobId.isNotNullOrBlank()) {
                        QueryEngine(server).waitForJob(jobId)
                    } else {
                        JobResult.NotFound
                    }
                } else {
                    JobResult.Failure("Companion plugin not installed")
                }
            }

        private fun getIps(): List<String> =
            try {
                val ips = mutableListOf<String>()
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val addresses = interfaces.nextElement().inetAddresses
                    while (addresses.hasMoreElements()) {
                        val address = addresses.nextElement()
                        if (address.isSiteLocalAddress && address is Inet4Address && address.hostAddress != null) {
                            ips.add(address.hostAddress!!)
                        }
                    }
                }
                ips
            } catch (ex: Exception) {
                Log.w(TAG, "Error getting IP address", ex)
                listOf()
            }
    }
}
