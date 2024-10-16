package com.github.damontecres.stashapp.filter

import android.os.Bundle
import android.text.InputType
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.util.isNotNullOrBlank

class CreateFindFilterFragment(
    val dataType: DataType,
    private var currFindFilter: StashFindFilter,
) : CreateFilterGuidedStepFragment() {
    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            getString(R.string.sort_by),
            "",
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        val sortOptions =
            dataType.sortOptions
                .mapIndexed { index, sortOption ->
                    GuidedAction.Builder(requireContext())
                        .id(SORT_OFFSET + index)
                        .hasNext(false)
                        .title(getString(sortOption.nameStringId))
                        .build()
                }
                .sortedBy { it.title.toString() }

        val currSortOption = currFindFilter.sortAndDirection?.sort
        val sortDesc =
            if (currSortOption != null) {
                getString(currSortOption.nameStringId)
            } else {
                null
            }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(SORT)
                .hasNext(true)
                .subActions(sortOptions)
                .title(getString(R.string.sort_by))
                .description(sortDesc)
                .build(),
        )

        val directionOptions =
            buildList {
                add(
                    GuidedAction.Builder(requireContext())
                        .id(DIRECTION_OFFSET + SortDirectionEnum.ASC.ordinal)
                        .hasNext(false)
                        .title(getString(R.string.stashapp_ascending))
                        .build(),
                )
                add(
                    GuidedAction.Builder(requireContext())
                        .id(DIRECTION_OFFSET + SortDirectionEnum.DESC.ordinal)
                        .hasNext(false)
                        .title(getString(R.string.stashapp_descending))
                        .build(),
                )
            }

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(DIRECTION)
                .hasNext(true)
                .subActions(directionOptions)
                .title(getString(R.string.stashapp_config_ui_image_wall_direction))
                .description(getDirectionString(currFindFilter.sortAndDirection?.direction))
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(QUERY)
                .hasNext(true)
                .title(getString(R.string.stashapp_component_tagger_noun_query))
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT)
                .editDescription(currFindFilter.q)
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_FINISH)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_save))
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_cancel))
                .build(),
        )
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        if (action.id >= SORT_OFFSET) {
            val newSort = dataType.sortOptions[(action.id - SORT_OFFSET).toInt()]
            currFindFilter = currFindFilter.withSort(newSort.key)
            findActionById(SORT).description = getString(newSort.nameStringId)
            notifyActionChanged(findActionPositionById(SORT))
        } else if (action.id >= DIRECTION_OFFSET) {
            val newDirection = SortDirectionEnum.entries[(action.id - DIRECTION_OFFSET).toInt()]
            currFindFilter = currFindFilter.withDirection(newDirection, dataType)
            findActionById(DIRECTION).description = getDirectionString(newDirection)
            notifyActionChanged(findActionPositionById(DIRECTION))
        }
        return true
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_FINISH) {
            val newQuery = findActionById(QUERY).description?.toString()
            val newValue =
                if (newQuery.isNotNullOrBlank()) {
                    currFindFilter.copy(q = newQuery)
                } else {
                    currFindFilter
                }
            viewModel.findFilter.value = newValue
            parentFragmentManager.popBackStack()
        } else if (action.id == GuidedAction.ACTION_ID_CANCEL) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun getDirectionString(direction: SortDirectionEnum?): String? {
        return when (direction) {
            SortDirectionEnum.ASC -> getString(R.string.stashapp_ascending)
            SortDirectionEnum.DESC -> getString(R.string.stashapp_descending)
            else -> null
        }
    }

    companion object {
        private const val TAG = "SortPickerFragment"
        private const val DIRECTION = 2L
        private const val SORT = 3L
        private const val QUERY = 4L

        private const val DIRECTION_OFFSET = 1_000L
        private const val SORT_OFFSET = 2_000L
    }
}
