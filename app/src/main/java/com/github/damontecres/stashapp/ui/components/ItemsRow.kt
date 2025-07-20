package com.github.damontecres.stashapp.ui.components

import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.LocalGlobalContext
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.isPlayKeyUp
import com.github.damontecres.stashapp.ui.util.getPlayDestinationForItem
import kotlinx.parcelize.Parcelize

@Composable
fun <T : StashData> ItemsRow(
    title: String,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    cardOnFocus: (isFocused: Boolean, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    focusPair: FocusPair? = null,
    additionalContent: (LazyListScope.() -> Unit)? = null,
) = ItemsRow(
    title = title,
    items = items,
    uiConfig = uiConfig,
    itemOnClick = itemOnClick,
    longClicker = longClicker,
    cardOnFocus = cardOnFocus,
    modifier = modifier,
    focusPair = focusPair,
    itemContent = { uiConfig, item, itemOnClick, longClicker, getFilterAndPosition, modifier ->
        StashCard(
            uiConfig = uiConfig,
            item = item,
            itemOnClick = { itemOnClick.onClick(item, null) },
            longClicker = longClicker,
            getFilterAndPosition = null,
            modifier = modifier,
        )
    },
    additionalContent = additionalContent,
)

@Composable
fun <T : StashData> ItemsRow(
    title: String,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    longClicker: LongClicker<Any>? = null,
    additionalContent: (LazyListScope.() -> Unit)? = null,
) = ItemsRow(
    title = title,
    items = items,
    uiConfig = uiConfig,
    itemOnClick = itemOnClick,
    longClicker = longClicker ?: LongClicker { _, _ -> },
    cardOnFocus = { _, _ -> },
    modifier = modifier,
    focusPair = null,
    itemContent = { uiConfig, item, itemOnClick, longClicker, getFilterAndPosition, modifier ->
        StashCard(
            uiConfig = uiConfig,
            item = item,
            itemOnClick = { itemOnClick.onClick(item, null) },
            longClicker = longClicker,
            getFilterAndPosition = null,
            modifier = modifier,
        )
    },
    additionalContent = additionalContent,
)

@Composable
fun <T : StashData> ItemsRow(
    title: String,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    cardOnFocus: (isFocused: Boolean, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    focusPair: FocusPair? = null,
    additionalContent: (LazyListScope.() -> Unit)? = null,
    itemContent: @Composable (
        uiConfig: ComposeUiConfig,
        item: T,
        itemOnClick: ItemOnClicker<Any>,
        longClicker: LongClicker<Any>,
        getFilterAndPosition: ((item: Any) -> FilterAndPosition)?,
        modifier: Modifier,
    ) -> Unit,
) {
    val navigationManager = LocalGlobalContext.current.navigationManager
    val firstFocus = remember { FocusRequester() }
    var focusedIndex by remember { mutableIntStateOf(focusPair?.column ?: 0) }
    val state = rememberLazyListState()
    val server = LocalGlobalContext.current.server
    Column(
        modifier = modifier,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        LazyRow(
            modifier =
                Modifier
                    .padding(top = 8.dp)
                    .focusRestorer(focusPair?.focusRequester ?: firstFocus)
                    .onKeyEvent {
                        if (isPlayKeyUp(it)) {
                            val destination =
                                getPlayDestinationForItem(server, items[focusedIndex], null)
                            return@onKeyEvent if (destination != null) {
                                navigationManager.navigate(destination)
                                true
                            } else {
                                false
                            }
                        }
                        return@onKeyEvent false
                    },
            state = state,
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val cardModifier =
                    if (index == 0 && focusPair == null) {
                        Modifier
                            .focusRequester(firstFocus)
                    } else {
                        if (focusPair != null && focusPair.column == index) {
                            Log.d("SceneDetails", "Setting focusPair on $index")
                            Modifier
                                .focusRequester(focusPair.focusRequester)
                        } else {
                            Modifier
                        }
                    }.onFocusChanged {
                        if (it.isFocused) {
                            Log.d("SceneDetails", "focused on $index")
                            focusedIndex = index
                        }
                        cardOnFocus.invoke(it.isFocused, index)
                    }

                itemContent.invoke(
                    uiConfig,
                    item,
                    itemOnClick,
                    longClicker,
                    null,
                    cardModifier,
                )
            }
            additionalContent?.invoke(this)
        }
    }
}

@Parcelize
data class RowColumn(
    val row: Int,
    val column: Int,
) : Parcelable

data class FocusPair(
    val row: Int,
    val column: Int,
    val focusRequester: FocusRequester,
)
