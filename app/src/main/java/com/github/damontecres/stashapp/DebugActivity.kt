package com.github.damontecres.stashapp

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashClient
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.plugin.CompanionPlugin
import kotlinx.coroutines.launch

class DebugActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.frame_layout)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, DebugFragment())
                .commitNow()
        }
    }

    class DebugFragment : Fragment(R.layout.debug) {
        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)

            val prefTable = view.findViewById<TableLayout>(R.id.preferences_table)
            val serverPrefTable = view.findViewById<TableLayout>(R.id.server_prefs_table)
            val formatSupportedTable = view.findViewById<TableLayout>(R.id.supported_formats_table)
            val otherTable = view.findViewById<TableLayout>(R.id.other_table)
            val logTextView = view.findViewById<TextView>(R.id.logs)

            val prefManager = PreferenceManager.getDefaultSharedPreferences(requireContext()).all
            prefManager.keys.sorted().forEach {
                val row = createRow(it, prefManager[it].toString())
                prefTable.addView(row)
            }
            prefTable.isStretchAllColumns = true

            val serverPrefs = StashServer.requireCurrentServer().serverPreferences
            val serverPrefsRaw = serverPrefs.preferences.all
            serverPrefsRaw.keys.sorted().forEach {
                val row = createRow(it, serverPrefsRaw[it].toString())
                serverPrefTable.addView(row)
            }
            serverPrefTable.isStretchAllColumns = true

            val codecs = CodecSupport.getSupportedCodecs(requireContext())
            formatSupportedTable.addView(
                createRow(
                    "Video Codecs",
                    codecs.videoCodecs.sorted().joinToString(", "),
                ),
            )
            formatSupportedTable.addView(
                createRow(
                    "Audio Codecs",
                    codecs.audioCodecs.sorted().joinToString(", "),
                ),
            )
            formatSupportedTable.addView(
                createRow(
                    "Container Formats",
                    codecs.containers.sorted().joinToString(", "),
                ),
            )
            formatSupportedTable.isStretchAllColumns = true

            val server = StashServer.getCurrentStashServer(requireContext())
            if (server != null) {
                otherTable.addView(
                    createRow(
                        "Current server URL",
                        server.url,
                    ),
                )
                otherTable.addView(
                    createRow(
                        "Current server API Key",
                        server.apiKey,
                    ),
                )
                otherTable.addView(
                    createRow(
                        "Current server URL (resolved endpoint)",
                        StashClient.cleanServerUrl(server.url),
                    ),
                )
                otherTable.addView(
                    createRow(
                        "Current server URL (root)",
                        StashClient.getServerRoot(server.url),
                    ),
                )
            }
            otherTable.addView(
                createRow(
                    "User-Agent",
                    StashClient.createUserAgent(requireContext()),
                ),
            )
            otherTable.isStretchAllColumns = true

            viewLifecycleOwner.lifecycleScope.launch(
                StashCoroutineExceptionHandler {
                    Toast.makeText(
                        requireContext(),
                        "Exception getting logs: ${it.message}",
                        Toast.LENGTH_LONG,
                    )
                },
            ) {
                val logs = CompanionPlugin.getLogCatLines(true).joinToString("\n")
                logTextView.text = logs
            }

            view.findViewById<Button>(R.id.button_test_saved_filters).setOnClickListener {
                testSavedFilters()
            }
        }

        private fun testSavedFilters() {
            val table = requireView().findViewById<TableLayout>(R.id.debug_test_table)
            table.removeAllViews()
            table.addView(createRow("Data Type", "ID", "Name", "Result"))

            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                try {
                    val server = StashServer.requireCurrentServer()
                    val queryEngine = QueryEngine(server)
                    val filterParser = FilterParser(server.serverPreferences.serverVersion)
                    DataType.entries.forEach { dataType ->
                        val filters = queryEngine.getSavedFilters(dataType)
                        filters.forEach { filter ->
                            try {
                                filter.toFilterArgs(filterParser)
                                table.addView(
                                    createRow(
                                        dataType.name,
                                        filter.id,
                                        filter.name,
                                        "Success",
                                    ),
                                )
                            } catch (ex: Exception) {
                                Log.w(TAG, "Test saved filter failed: id=${filter.id}", ex)
                                table.addView(
                                    createRow(
                                        dataType.name,
                                        filter.id,
                                        filter.name,
                                        ex.message?.ifBlank { ex.cause?.message },
                                    ),
                                )
                            }
                        }
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Exception during test", ex)
                }
            }
        }

        private fun createRow(
            key: String,
            vararg values: String?,
        ): TableRow {
            val row = TableRow(requireContext())
            row.layoutParams =
                TableLayout.LayoutParams(
                    TableLayout.LayoutParams.MATCH_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT,
                )
            row.gravity = Gravity.CENTER_HORIZONTAL
//            val lp = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT)
//            row.layoutParams = lp

            val keyView = TextView(requireContext())
            keyView.text = key
            keyView.textSize = TABLE_TEXT_SIZE
            keyView.setTextColor(Color.WHITE)
            keyView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            keyView.setPadding(5, 3, 5, 3)
//            keyView.isSingleLine=false
//            keyView.maxLines=8
//            keyView.maxWidth=400

            row.addView(keyView)

            val isApiKey = key.contains("apikey", true) || key.contains("api key", true)

            values.forEach { value ->
                val valueView = TextView(requireContext())
                valueView.text =
                    if (isApiKey && value != null
                    ) {
                        value.take(4) + "..." + value.takeLast(8)
                    } else {
                        value
                    }
                if (isApiKey) {
                    valueView.typeface = Typeface.MONOSPACE
                }
                valueView.textSize = TABLE_TEXT_SIZE
                valueView.setTextColor(Color.WHITE)
                valueView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                valueView.setPadding(15, 3, 5, 3)
//            valueView.isSingleLine=false
//            valueView.maxLines=8
//            valueView.maxWidth=400
                row.addView(valueView)
            }

            return row
        }

        companion object {
            private const val TAG = "DebugFragment"
            private const val TABLE_TEXT_SIZE = 12F
        }
    }
}
