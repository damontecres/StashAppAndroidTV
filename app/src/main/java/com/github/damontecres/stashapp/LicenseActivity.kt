package com.github.damontecres.stashapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView
import com.github.damontecres.stashapp.util.concatIfNotBlank
import java.io.BufferedReader

class LicenseActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.license)

        val attributions = resources.getTextArray(R.array.stash_license_attributions)
        val licenseText = assets.open("LICENSE").bufferedReader().use(BufferedReader::readText)

        val text = concatIfNotBlank(getString(R.string.license_separator), *attributions, licenseText)

        findViewById<TextView>(R.id.license_text).text = text
    }
}
