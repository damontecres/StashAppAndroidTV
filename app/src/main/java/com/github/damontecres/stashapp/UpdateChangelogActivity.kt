package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import io.noties.markwon.Markwon

class UpdateChangelogActivity : FragmentActivity() {
    companion object {
        const val INTENT_CHANGELOG = "changelog"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            setContentView(R.layout.activity_main)
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.main_browse_fragment, UpdateChangelogFragment())
                .commitNow()
        }
    }

    class UpdateChangelogFragment : Fragment(R.layout.changelog) {
        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)
            if (savedInstanceState == null) {
                val changelogText = requireActivity().intent.getStringExtra(INTENT_CHANGELOG)!!
                val textView = view.findViewById<TextView>(R.id.changelog_text)
                val markdown = Markwon.create(requireContext())
                markdown.setMarkdown(textView, changelogText)
            }
        }
    }
}
