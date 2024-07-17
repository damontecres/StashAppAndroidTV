package com.github.damontecres.stashapp

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.playback.CodecSupport
import com.github.damontecres.stashapp.util.ServerPreferences

class DebugActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.frame_layout)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.frame_fragment, DebugFragment())
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

            val prefManager = PreferenceManager.getDefaultSharedPreferences(requireContext()).all
            prefManager.keys.sorted().forEach {
                val row = createRow(it, prefManager[it].toString())
                prefTable.addView(row)
            }
            prefTable.isStretchAllColumns = true

            val serverPrefs = ServerPreferences(requireContext()).preferences.all
            serverPrefs.keys.sorted().forEach {
                val row = createRow(it, serverPrefs[it].toString())
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
        }

        private fun createRow(
            key: String,
            value: String?,
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

            val valueView = TextView(requireContext())
            valueView.text = if (key.contains("apikey", true)) "*****" else value
            valueView.textSize = TABLE_TEXT_SIZE
            valueView.setTextColor(Color.WHITE)
            valueView.textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
            valueView.setPadding(15, 3, 5, 3)
//            valueView.isSingleLine=false
//            valueView.maxLines=8
//            valueView.maxWidth=400

            row.addView(keyView)
            row.addView(valueView)

            return row
        }

        companion object {
            private const val TABLE_TEXT_SIZE = 12F
        }
    }
}
