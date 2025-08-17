package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.getDestination
import io.noties.markwon.Markwon

class UpdateChangelogFragment : Fragment(R.layout.changelog) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val release = requireArguments().getDestination<Destination.ReleaseChangelog>().release
        val changelogText =
            "# ${release.version}\n\n${release.body}"
                // Convert PR urls to number
                .replace(
                    Regex("https://github.com/damontecres/StashAppAndroidTV/pull/(\\d+)"),
                    "#$1",
                )
                // Remove the last line for full changelog since its just a link
                .replace(Regex("\\*\\*Full Changelog\\*\\*.*"), "")
        val textView = view.findViewById<TextView>(R.id.changelog_text)
        val markdown = Markwon.create(requireContext())
        markdown.setMarkdown(textView, changelogText)
    }
}
