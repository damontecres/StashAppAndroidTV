package com.github.damontecres.stashapp.ui.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.prefs.PreferencesContent
import com.github.damontecres.stashapp.util.StashServer

@SuppressLint("RestrictedApi")
@Composable
fun SettingsPage(
    server: StashServer,
    navigationManager: NavigationManager,
    preferenceScreenOption: PreferenceScreenOption,
    uiConfig: ComposeUiConfig,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
    ) {
        val newModifier =
            if (isTvDevice) {
                Modifier
                    .fillMaxWidth(.4f)
                    .align(Alignment.TopEnd)
            } else {
                Modifier
            }.background(MaterialTheme.colorScheme.secondaryContainer)
        PreferencesContent(
            server,
            navigationManager,
            uiConfig.preferences,
            preferenceScreenOption,
            newModifier,
        )
    }
}
