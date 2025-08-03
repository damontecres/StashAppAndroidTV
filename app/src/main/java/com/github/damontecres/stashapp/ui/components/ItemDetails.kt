package com.github.damontecres.stashapp.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.ui.ComposeUiConfig
import com.github.damontecres.stashapp.ui.FontAwesome
import com.github.damontecres.stashapp.ui.compat.Button
import com.github.damontecres.stashapp.ui.components.scene.AddRemove
import com.github.damontecres.stashapp.ui.pages.DialogParams
import com.github.damontecres.stashapp.ui.pages.SearchForDialog
import com.github.damontecres.stashapp.ui.pages.SearchForParams
import com.github.damontecres.stashapp.ui.titleCount
import com.github.damontecres.stashapp.util.isNotNullOrBlank

@Composable
fun ItemDetails(
    uiConfig: ComposeUiConfig,
    imageUrl: String?,
    tableRows: List<TableRow>,
    itemOnClick: ItemOnClicker<Any>,
    longClicker: LongClicker<Any>,
    modifier: Modifier = Modifier,
    favorite: Boolean? = null,
    favoriteClick: (() -> Unit)? = null,
    rating100: Int? = null,
    rating100Click: ((rating100: Int) -> Unit)? = null,
    basicItemInfo: BasicItemInfo? = null,
    tags: List<TagData>? = null,
    onEdit: ((EditItem) -> Unit)? = null,
    editableTypes: Set<DataType>? = null,
    bodyContent: (LazyListScope.() -> Unit)? = null,
) {
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf<DialogParams?>(null) }
    var searchForDataType by remember { mutableStateOf<SearchForParams?>(null) }

    Row(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        if (imageUrl.isNotNullOrBlank()) {
            AsyncImage(
                modifier =
                    Modifier
                        .padding(12.dp)
                        .fillMaxWidth(.5f)
                        .fillMaxHeight(),
                model =
                    ImageRequest
                        .Builder(LocalContext.current)
                        .data(imageUrl)
                        .crossfade(false)
                        .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
            )
        }
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 16.dp),
            modifier = Modifier.padding(12.dp),
        ) {
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier,
                ) {
                    if (favorite != null && favoriteClick != null) {
                        val color = if (favorite) Color.Red else Color.LightGray

                        ProvideTextStyle(MaterialTheme.typography.displayLarge.copy(color = color)) {
                            Button(
                                onClick = favoriteClick,
                            ) {
                                Text(
                                    text = stringResource(R.string.fa_heart),
                                    fontFamily = FontAwesome,
                                )
                            }
                        }
                    }
                    if (uiConfig.readOnlyModeDisabled && onEdit != null && editableTypes != null) {
                        EditButton(
                            onClick = {
                                showDialog =
                                    DialogParams(
                                        false,
                                        context.getString(R.string.stashapp_actions_edit),
                                        buildEditList(context, editableTypes) {
                                            searchForDataType = it
                                        },
                                    )
                            },
                        )
                    }
                }
            }
            if (rating100Click != null) {
                item {
                    Rating100(
                        rating100 = rating100 ?: 0,
                        uiConfig = uiConfig,
                        onRatingChange = rating100Click,
                        enabled = true,
                        modifier =
                            Modifier
                                .height(ratingBarHeight)
                                .padding(start = 0.dp),
                    )
                }
            }
            items(tableRows) { row ->
                TableRowComposable(row)
            }

            if (!tags.isNullOrEmpty()) {
                item {
                    ItemsRow(
                        title = titleCount(R.string.stashapp_tags, tags),
                        items = tags,
                        uiConfig = uiConfig,
                        itemOnClick = itemOnClick,
                        longClicker = longClicker,
                        modifier =
                            Modifier
                                .padding(top = 12.dp)
                                .animateItem(),
                    )
                }
            }

            bodyContent?.invoke(this)

            basicItemInfo?.let {
                item {
                    ItemDetailsFooter(
                        id = it.id,
                        createdAt = it.createdAt?.toString(),
                        updatedAt = it.updatedAt?.toString(),
                        modifier =
                            Modifier
                                .padding(top = 32.dp)
                                .fillMaxWidth(),
                    )
                }
            }
        }
    }

    showDialog?.let { params ->
        DialogPopup(
            showDialog = true,
            title = params.title,
            dialogItems = params.items,
            onDismissRequest = { showDialog = null },
            waitToLoad = params.fromLongClick,
        )
    }
    SearchForDialog(
        show = searchForDataType != null,
        dataType = searchForDataType?.dataType ?: DataType.TAG,
        onItemClick = { item ->
            onEdit?.invoke(EditItem(item.id, searchForDataType!!.dataType, AddRemove.ADD))
            searchForDataType = null
        },
        onDismissRequest = { searchForDataType = null },
        dialogTitle = showDialog?.title,
        dismissOnClick = false,
        uiConfig = uiConfig,
    )
}

data class BasicItemInfo(
    val id: String,
    val createdAt: Any?,
    val updatedAt: Any?,
)

@Composable
fun ItemDetailsFooter(
    id: String,
    createdAt: String?,
    updatedAt: String?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.Start),
    ) {
        if (createdAt != null && createdAt.length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_created_at),
                createdAt.substring(0..<10),
            )
        }
        if (updatedAt != null && updatedAt.length >= 10) {
            TitleValueText(
                stringResource(R.string.stashapp_updated_at),
                updatedAt.substring(0..<10),
            )
        }
        TitleValueText(stringResource(R.string.id), id)
    }
}

data class EditItem(
    val id: String,
    val dataType: DataType,
    val action: AddRemove,
)

fun buildEditList(
    context: Context,
    editableTypes: Set<DataType>,
    onClick: (SearchForParams) -> Unit,
) = buildList {
    if (DataType.TAG in editableTypes) {
        add(
            DialogItem(
                context.getString(R.string.add_tag),
                DataType.TAG.iconStringId,
            ) {
                onClick.invoke(SearchForParams(DataType.TAG))
            },
        )
    }
    if (DataType.PERFORMER in editableTypes) {
        add(
            DialogItem(
                context.getString(R.string.add_performer),
                DataType.PERFORMER.iconStringId,
            ) {
                onClick.invoke(SearchForParams(DataType.PERFORMER))
            },
        )
    }

    if (DataType.STUDIO in editableTypes) {
        add(
            DialogItem(
                context.getString(R.string.set_studio),
                DataType.STUDIO.iconStringId,
            ) {
                onClick.invoke(SearchForParams(DataType.STUDIO))
            },
        )
    }
    if (DataType.GROUP in editableTypes) {
        add(
            DialogItem(
                context.getString(R.string.add_group),
                DataType.GROUP.iconStringId,
            ) {
                onClick.invoke(SearchForParams(DataType.GROUP))
            },
        )
    }
    if (DataType.GALLERY in editableTypes) {
        add(
            DialogItem(
                context.getString(R.string.add_gallery),
                DataType.GALLERY.iconStringId,
            ) {
                onClick.invoke(SearchForParams(DataType.GALLERY))
            },
        )
    }
}
