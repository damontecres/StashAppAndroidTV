package com.github.damontecres.stashapp.util.plugin

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.api.RunPluginTaskMutation
import com.github.damontecres.stashapp.api.type.PackageSpecInput
import com.github.damontecres.stashapp.api.type.PackageType
import com.github.damontecres.stashapp.data.JobResult
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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
                PackageSpecInput(PLUGIN_ID, SOURCE_URL),
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
            verbose: Boolean,
        ) = withContext(Dispatchers.IO + StashCoroutineExceptionHandler(autoToast = true)) {
            try {
                val lines = getLogCatLines(verbose)
                val sb = StringBuilder("** LOGCAT START **\n")
                sb.append(lines.joinToString("\n"))
                sb.append("\n** LOGCAT END **")
                // Avoid individual lines being logged server-side
                val logcat = sb.replace(Regex("\n"), "<newline>")

                val server = StashServer.requireCurrentServer()
                val mutationEngine = MutationEngine(server)
                val mutation =
                    RunPluginTaskMutation(
                        plugin_id = PLUGIN_ID,
                        task_name = LOGCAT_TASK_NAME,
                        args_map = mapOf(LOGCAT_TASK_NAME to logcat),
                    )
                val jobId = mutationEngine.executeMutation(mutation).data?.runPluginTask
                if (jobId.isNotNullOrBlank()) {
                    val result = QueryEngine(server).waitForJob(jobId)
                    withContext(Dispatchers.Main) {
                        if (result is JobResult.Success) {
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
                        } else if (result is JobResult.NotFound) {
                            Toast.makeText(context, "Error sending logs", Toast.LENGTH_LONG).show()
                        } else if (result is JobResult.Failure) {
                            Toast
                                .makeText(
                                    context,
                                    "Error sending logs: ${result.message}",
                                    Toast.LENGTH_LONG,
                                ).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "Error sending logs", Toast.LENGTH_LONG).show()
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
    }
}
