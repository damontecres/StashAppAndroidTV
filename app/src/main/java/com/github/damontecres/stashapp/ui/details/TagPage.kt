package com.github.damontecres.stashapp.ui.details

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
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
import com.github.damontecres.stashapp.ui.TabbedFilterGrid
import com.github.damontecres.stashapp.util.QueryEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

        private val _uiState = MutableLiveData<TagUiState>(TagUiState.Loading)
        val uiState: LiveData<TagUiState> get() = _uiState

        suspend fun fetchTag(tagId: String) {
            val tag = queryEngine.getTags(listOf(tagId)).firstOrNull()
            _uiState.value =
                if (tag != null) {
                    TagUiState.Success(tag)
                } else {
                    TagUiState.Error("No Tag with id=$tagId")
                }
        }
    }

@Suppress("ktlint:standard:function-naming")
@Composable
fun TagPage(
    tagId: String,
    itemOnClick: (Any) -> Unit,
    viewModel: TagViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.observeAsState().value

    LaunchedEffect(Unit) {
        viewModel.fetchTag(tagId)
    }

    when (val s = uiState) {
        is TagUiState.Loading -> {
            Text(text = "Loading...")
        }

        is TagUiState.Error -> {
            Text(text = "Error: ${s.message}")
        }

        is TagUiState.Success -> {
            val tabs =
                listOf(
                    stringResource(DataType.SCENE.pluralStringId),
                    stringResource(DataType.GALLERY.pluralStringId),
                    stringResource(DataType.IMAGE.pluralStringId),
                    stringResource(DataType.MARKER.pluralStringId),
                    stringResource(DataType.PERFORMER.pluralStringId),
                    stringResource(R.string.stashapp_sub_tags),
                )
            TabbedFilterGrid(
                name = s.tag.name,
                tabs = tabs,
                contentProvider = { index ->
                    getPagingSource(index, s.tag)
                },
                itemOnClick = itemOnClick,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .animateContentSize(),
            )
        }

        null -> throw IllegalStateException()
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
