package com.github.damontecres.stashapp.ui.pages

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.github.damontecres.stashapp.PreferenceScreenOption
import com.github.damontecres.stashapp.navigation.NavigationManager
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.compat.isTvDevice
import com.github.damontecres.stashapp.ui.components.prefs.PreferencesBasicContent
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
        modifier = modifier,
    ) {
        val newModifier =
            if (isTvDevice) {
                Modifier
                    .fillMaxWidth(.4f)
                    .align(Alignment.TopEnd)
            } else {
                Modifier
            }
        when (preferenceScreenOption) {
            PreferenceScreenOption.BASIC ->
                PreferencesBasicContent(
                    server,
                    navigationManager,
                    uiConfig.preferences,
                    newModifier,
                )
            PreferenceScreenOption.ADVANCED -> TODO()
            PreferenceScreenOption.USER_INTERFACE -> TODO()
        }
    }
}
