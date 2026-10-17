package com.github.damontecres.stashapp.ui.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.ui.DefaultMaterial3Theme
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer

@Composable
fun LicenseInfoPage(modifier: Modifier = Modifier) {
    DefaultMaterial3Theme {
        val libraries by produceLibraries()

        LibrariesContainer(libraries, modifier)
    }
}
