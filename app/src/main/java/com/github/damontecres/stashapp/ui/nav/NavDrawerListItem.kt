package com.github.damontecres.stashapp.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.NavigationDrawerItem
import androidx.tv.material3.NavigationDrawerScope
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.isElementVisible
import com.github.damontecres.stashapp.ui.util.ifElse
import com.github.damontecres.stashapp.ui.util.playSoundOnFocus

@Composable
fun NavigationDrawerScope.NavDrawerListItem(
    page: DrawerPage,
    selectedScreen: DrawerPage?,
    initialFocus: FocusRequester,
    composeUiConfig: ComposeUiConfig,
    drawerOpen: Boolean,
    onClick: () -> Unit,
    onVisible: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationDrawerItem(
        modifier =
            modifier
                .ifElse(
                    selectedScreen == page,
                    Modifier
                        .focusRequester(initialFocus),
                ).isElementVisible { onVisible(it) }
                .playSoundOnFocus(composeUiConfig.playSoundOnFocus),
        selected = selectedScreen == page && drawerOpen,
        onClick = onClick,
        leadingContent = {
            if (page != DrawerPage.SettingPage) {
                val color =
                    if (selectedScreen == page) {
                        MaterialTheme.colorScheme.border
                    } else {
                        Color.Unspecified
                    }
                Text(
                    stringResource(id = page.iconString),
                    fontFamily = FontAwesome,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            // Centers the icon for some reason
                            .padding(top = 4.dp),
                    color = color,
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.vector_settings),
                    contentDescription = null,
                )
            }
        },
    ) {
        Text(
            text = stringResource(id = page.name),
            modifier =
                Modifier.ifElse(
                    isNotTvDevice,
                    Modifier.clickable(onClick = onClick),
                ),
        )
    }
}
