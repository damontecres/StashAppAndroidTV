package com.github.damontecres.stashapp.ui.nav

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
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
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(id = page.iconString),
                        fontFamily = FontAwesome,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        color = color,
                        modifier = Modifier,
                    )
                }
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
