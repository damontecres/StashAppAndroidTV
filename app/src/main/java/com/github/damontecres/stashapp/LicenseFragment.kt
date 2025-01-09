package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.github.damontecres.stashapp.util.concatIfNotBlank
import java.io.BufferedReader

/**
 * Show various licenses for third party libraries included in the app
 */
class LicenseFragment : Fragment(R.layout.license) {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val attributions = resources.getTextArray(R.array.stash_license_attributions)
        val licenseText =
            resources.assets
                .open("LICENSE")
                .bufferedReader()
                .use(BufferedReader::readText)
        val otherLicenses =
            resources.assets
                .list("licenses")!!
                .map {
                    resources.assets
                        .open("licenses/$it")
                        .bufferedReader()
                        .use(BufferedReader::readText)
                }.toTypedArray()

        val text =
            concatIfNotBlank(
                getString(R.string.license_separator),
                *attributions,
                licenseText,
                *otherLicenses,
            )

        view.findViewById<TextView>(R.id.license_text).text = text
    }
}
