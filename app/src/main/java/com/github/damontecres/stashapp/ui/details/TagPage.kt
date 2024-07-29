package com.github.damontecres.stashapp.ui.details

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Tab
import androidx.tv.material3.TabRow
import androidx.tv.material3.Text
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.GalleryFilterType
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.ImageFilterType
import com.github.damontecres.stashapp.api.type.PerformerFilterType
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.SceneMarkerFilterType
import com.github.damontecres.stashapp.api.type.TagFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.GalleryDataSupplier
import com.github.damontecres.stashapp.suppliers.ImageDataSupplier
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.ui.ResolvedFilter
import com.github.damontecres.stashapp.ui.ResolvedFilterGrid
import com.github.damontecres.stashapp.ui.ResolvedFilterState
import com.github.damontecres.stashapp.util.QueryEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed class TagUiState {
    data class Success(val tag: TagData) : TagUiState()

    data object Loading : TagUiState()

    data class Error(val message: String, val cause: Exception? = null) : TagUiState()
}

@HiltViewModel
class TagViewModel
    @Inject
    constructor(
        @ApplicationContext context: Context,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val queryEngine = QueryEngine(context)

        val uiState =
            savedStateHandle
                .getStateFlow<String?>("id", null)
                .map { id ->
                    if (id == null) {
                        TagUiState.Error("TagId cannot be null")
                    } else {
                        val tag = queryEngine.getTags(listOf(id)).firstOrNull()
                        if (tag != null) {
                            TagUiState.Success(tag)
                        } else {
                            TagUiState.Error("No Tag with id=$id")
                        }
                    }
                }.stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(5_000),
                    initialValue = TagUiState.Loading,
                )
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun TagPage(
    itemOnClick: (Any) -> Unit,
    viewModel: TagViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is TagUiState.Loading -> {
            Text(text = "Loading...")
        }

        is TagUiState.Error -> {
            Text(text = "Error: ${s.message}")
        }

        is TagUiState.Success -> {
            LaunchedEffect(Unit) {
            }
            TagDetails(
                s.tag,
                itemOnClick,
                Modifier
                    .fillMaxSize()
                    .animateContentSize(),
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Suppress("ktlint:standard:function-naming")
@Composable
fun TagDetails(
    tag: TagData,
    itemOnClick: (Any) -> Unit,
    modifier: Modifier,
) {
    val tabs =
        listOf(
            stringResource(DataType.SCENE.pluralStringId),
            stringResource(DataType.GALLERY.pluralStringId),
            stringResource(DataType.IMAGE.pluralStringId),
            stringResource(DataType.MARKER.pluralStringId),
            stringResource(DataType.PERFORMER.pluralStringId),
            stringResource(R.string.stashapp_sub_tags),
        )
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        Column {
            ProvideTextStyle(MaterialTheme.typography.headlineLarge) {
                Text(
                    text = tag.name,
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally),
                )
            }
            // https://developer.android.com/reference/kotlin/androidx/tv/material3/package-summary#TabRow(kotlin.Int,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,kotlin.Function0,kotlin.Function2,kotlin.Function1)
            // TODO center tabs?
            TabRow(
                selectedTabIndex = selectedTabIndex,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth()
                        .focusRestorer(),
            ) {
                tabs.forEachIndexed { index, tab ->
                    key(index) {
                        Tab(
                            selected = index == selectedTabIndex,
                            onFocus = { selectedTabIndex = index },
                            modifier =
                                Modifier
                                    .align(Alignment.CenterHorizontally),
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.titleMedium) {
                                Text(
                                    text = tab,
                                    modifier =
                                        Modifier.padding(
                                            horizontal = 16.dp,
                                            vertical = 6.dp,
                                        ),
                                )
                            }
                        }
                    }
                }
            }
            val resolvedFilter =
                ResolvedFilterState.Success(ResolvedFilter(DataType.TAG), getPagingSource(selectedTabIndex, tag))
            ResolvedFilterGrid(
                resolvedFilter,
                showHeader = false,
                itemOnClick = itemOnClick,
                contentPadding = PaddingValues(top = 16.dp),
                modifier = Modifier,
            )
        }
    }
}

private fun getPagingSource(
    index: Int,
    tag: TagData,
    includeSubTags: Boolean = false,
): StashPagingSource<out Query.Data, Any, out Query.Data> {
    val depth = Optional.present(if (includeSubTags) -1 else 0)

    val dataSupplier =
        when (index) {
            0 -> {
                SceneDataSupplier(
                    SceneFilterType(
                        tags =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(tag.id)),
                                    modifier = CriterionModifier.INCLUDES_ALL,
                                    depth = depth,
                                ),
                            ),
                    ),
                )
            }

            1 -> {
                GalleryDataSupplier(
                    GalleryFilterType(
                        tags =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(tag.id)),
                                    modifier = CriterionModifier.INCLUDES,
                                    depth = depth,
                                ),
                            ),
                    ),
                )
            }

            2 -> {
                ImageDataSupplier(
                    ImageFilterType(
                        tags =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(tag.id)),
                                    modifier = CriterionModifier.INCLUDES,
                                    depth = depth,
                                ),
                            ),
                    ),
                )
            }

            3 -> {
                MarkerDataSupplier(
                    SceneMarkerFilterType(
                        tags =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(tag.id)),
                                    modifier = CriterionModifier.INCLUDES_ALL,
                                    depth = depth,
                                ),
                            ),
                    ),
                )
            }

            4 -> {
                PerformerDataSupplier(
                    PerformerFilterType(
                        tags =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(tag.id)),
                                    modifier = CriterionModifier.INCLUDES_ALL,
                                    depth = depth,
                                ),
                            ),
                    ),
                )
            }

            5 -> {
                TagDataSupplier(
                    DataType.TAG.asDefaultFindFilterType,
                    TagFilterType(
                        parents =
                            Optional.present(
                                HierarchicalMultiCriterionInput(
                                    value = Optional.present(listOf(tag.id)),
                                    modifier = CriterionModifier.INCLUDES_ALL,
                                    depth = depth,
                                ),
                            ),
                    ),
                )
            }

            else -> throw IllegalStateException("selectedTabIndex=$index")
        } as StashPagingSource.DataSupplier<*, Any, *>

    return StashPagingSource(
        StashApplication.getApplication(),
        25, // TODO
        dataSupplier = dataSupplier,
        useRandom = false,
    )
}
