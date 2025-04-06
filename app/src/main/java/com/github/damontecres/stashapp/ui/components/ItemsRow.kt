package com.github.damontecres.stashapp.ui.components

import android.os.Parcelable
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.cards.StashCard
import com.github.damontecres.stashapp.ui.util.ifElse
import kotlinx.parcelize.Parcelize

@Composable
fun <T : StashData> ItemsRow(
    @StringRes title: Int,
    rowNum: Int,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    cardOnFocus: (isFocused: Boolean, row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
    focusPair: FocusPair? = null,
) = ItemsRow(
    title = stringResource(title),
    rowNum = rowNum,
    items = items,
    uiConfig = uiConfig,
    itemOnClick = itemOnClick,
    longClicker = longClicker,
    cardOnFocus = cardOnFocus,
    modifier = modifier,
    focusPair = focusPair,
)

@Composable
fun <T : StashData> ItemsRow(
    title: String,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    modifier: Modifier = Modifier,
    longClicker: LongClicker<Any>? = null,
) = ItemsRow(
    title = title,
    rowNum = 0,
    items = items,
    uiConfig = uiConfig,
    itemOnClick = itemOnClick,
    longClicker = longClicker ?: LongClicker { _, _ -> },
    cardOnFocus = { _, _, _ -> },
    modifier = modifier,
    focusPair = null,
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun <T : StashData> ItemsRow(
    title: String,
    rowNum: Int,
    items: List<T>,
    uiConfig: ComposeUiConfig,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    cardOnFocus: (isFocused: Boolean, row: Int, column: Int) -> Unit,
    modifier: Modifier = Modifier,
    focusPair: FocusPair? = null,
) {
    val firstFocus = remember { FocusRequester() }
    val state = rememberLazyListState()
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
                    .focusRestorer {
                        Log.v(
                            "SceneDetails",
                            "focusPair?.focusRequester=${focusPair?.focusRequester}",
                        )
                        focusPair?.focusRequester ?: firstFocus
                    },
            state = state,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                val cardModifier =
                    if (index == 0) {
                        Modifier
                            .focusRequester(firstFocus)
                    } else {
                        Modifier
                    }.ifElse(
                        focusPair != null && focusPair.row == rowNum && focusPair.column == index,
                        { Modifier.focusRequester(focusPair!!.focusRequester) },
                    ).onFocusChanged {
                        cardOnFocus.invoke(it.isFocused, rowNum, index)
                    }

                StashCard(
                    uiConfig = uiConfig,
                    item = item,
                    itemOnClick = { itemOnClick.onClick(item, null) },
                    longClicker = longClicker,
                    getFilterAndPosition = null,
                    modifier = cardModifier,
                )
            }
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
