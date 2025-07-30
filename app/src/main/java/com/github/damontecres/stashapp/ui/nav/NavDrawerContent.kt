package com.github.damontecres.stashapp.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import com.github.damontecres.stashapp.ui.components.LongClicker
import com.github.damontecres.stashapp.ui.pages.FilterPage
import com.github.damontecres.stashapp.ui.pages.GalleryPage
import com.github.damontecres.stashapp.ui.pages.GroupPage
import com.github.damontecres.stashapp.ui.pages.MainPage
import com.github.damontecres.stashapp.ui.pages.MarkerPage
import com.github.damontecres.stashapp.ui.pages.PerformerPage
import com.github.damontecres.stashapp.ui.pages.SceneDetailsPage
import com.github.damontecres.stashapp.ui.pages.SearchPage
import com.github.damontecres.stashapp.ui.pages.StudioPage
import com.github.damontecres.stashapp.ui.pages.TagPage
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun NavDrawerContent(
    navManager: NavigationManagerCompose,
    server: StashServer,
    destination: Destination,
    composeUiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    onUpdateTitle: ((AnnotatedString) -> Unit)? = null,
) {
    when (destination) {
        Destination.Main -> {
            MainPage(
                server = server,
                uiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.Filter -> {
            FilterPage(
                server = server,
                navigationManager = navManager,
                initialFilter = destination.filterArgs,
                scrollToNextPage = destination.scrollToNextPage,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                uiConfig = composeUiConfig,
                onUpdateTitle = onUpdateTitle,
                modifier = modifier,
            )
        }

        is Destination.Search -> {
            SearchPage(
                server = server,
                navigationManager = navManager,
                uiConfig = composeUiConfig,
                itemOnClick = itemOnClick,
                longClicker = longClicker,
                modifier = modifier,
            )
        }

        is Destination.MarkerDetails ->
            MarkerPage(
                server = server,
                uiConfig = composeUiConfig,
                markerId = destination.markerId,
                itemOnClick = itemOnClick,
                onUpdateTitle = onUpdateTitle,
            )

        is Destination.Item -> {
            when (destination.dataType) {
                DataType.SCENE ->
                    SceneDetailsPage(
                        modifier = modifier,
                        server = server,
                        sceneId = destination.id,
                        itemOnClick = itemOnClick,
                        uiConfig = composeUiConfig,
                        playOnClick = { position, mode ->
                            navManager.navigate(
                                Destination.Playback(
                                    destination.id,
                                    position,
                                    mode,
                                ),
                            )
                        },
                        onUpdateTitle = onUpdateTitle,
                    )

                DataType.PERFORMER ->
                    PerformerPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )

                DataType.TAG ->
                    TagPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubTags = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )

                DataType.STUDIO ->
                    StudioPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubStudios = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )

                DataType.GALLERY ->
                    GalleryPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )

                DataType.GROUP ->
                    GroupPage(
                        modifier = modifier,
                        server = server,
                        id = destination.id,
                        includeSubGroups = false,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        uiConfig = composeUiConfig,
                        onUpdateTitle = onUpdateTitle,
                    )

                DataType.MARKER ->
                    MarkerPage(
                        server = server,
                        uiConfig = composeUiConfig,
                        markerId = destination.id,
                        itemOnClick = itemOnClick,
                        onUpdateTitle = onUpdateTitle,
                    )

                else -> FragmentView(navManager, destination, modifier)
            }
        }

        else -> {
            FragmentView(navManager, destination, modifier)
        }
    }
}
