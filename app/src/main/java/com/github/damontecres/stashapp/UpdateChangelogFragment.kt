package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.UpdateChecker
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

class UpdateChangelogFragment : Fragment(R.layout.changelog) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
            val release = UpdateChecker.getLatestRelease(requireActivity())
            if (release != null) {
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
    }
}
