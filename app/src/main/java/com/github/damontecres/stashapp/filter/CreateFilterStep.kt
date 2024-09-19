package com.github.damontecres.stashapp.filter

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.FilterListActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SaveFilterInput
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs
import kotlinx.coroutines.launch

/**
 * The first step to create a new filter
 *
 * Assumes that [CreateFilterViewModel.initialize] has been called already
 */
class CreateFilterStep : CreateFilterActivity.CreateFilterGuidedStepFragment() {
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryEngine = QueryEngine(StashServer.requireCurrentServer())
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val text =
            filterSummary(
                viewModel.dataType.value!!.filterType,
                viewModel.objectFilter.value!!,
                viewModel::lookupIds,
            )
        return GuidanceStylist.Guidance(
            "Create filter",
            text,
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(FILTER_NAME)
                .hasNext(false)
                .title(getString(R.string.stashapp_filter_name))
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT)
                .descriptionEditable(true)
                .build(),
        )

        val sortDesc =
            findFilterSummary(
                requireContext(),
                viewModel.dataType.value!!,
                viewModel.findFilter.value!!,
            )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(SORT_OPTION)
                .hasNext(true)
                .title(getString(R.string.sort_by))
                .description(sortDesc)
                .build(),
        )

        actions.add(
            GuidedAction.Builder(requireContext())
                .id(FILTER_OPTIONS)
                .hasNext(true)
                .title(getString(R.string.stashapp_filters))
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(SUBMIT)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_submit))
                .build(),
        )
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val dataType = viewModel.dataType.value!!
        if (action.id == FILTER_OPTIONS) {
            nextStep(CreateObjectFilterStep())
        } else if (action.id == SORT_OPTION) {
            nextStep(CreateFindFilterFragment(dataType, viewModel.findFilter.value!!))
        } else if (action.id == SUBMIT) {
            // Ready to load the filter!
            val filterNameAction = findActionById(FILTER_NAME)
            val objectFilter = viewModel.objectFilter.value!!
            val filterArgs =
                FilterArgs(
                    dataType = dataType,
                    name = filterNameAction.description?.toString()?.ifBlank { null },
                    findFilter = viewModel.findFilter.value,
                    objectFilter = objectFilter,
                )
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                // If there is a name, try to save it to the server
                if (filterArgs.name.isNotNullOrBlank()) {
                    // Save it
                    val filterWriter = FilterWriter(QueryEngine(StashServer.requireCurrentServer()))
                    val findFilter =
                        filterArgs.findFilter ?: StashFindFilter(
                            null,
                            filterArgs.dataType.defaultSort,
                        )
                    val objectFilterMap = filterWriter.convertFilter(objectFilter)
                    val mutationEngine = MutationEngine(StashServer.requireCurrentServer())
                    val newSavedFilter =
                        mutationEngine.saveFilter(
                            SaveFilterInput(
                                mode = dataType.filterMode,
                                name = filterNameAction.description.toString(),
                                find_filter =
                                    Optional.presentIfNotNull(
                                        findFilter.toFindFilterType(1, 40),
                                    ),
                                object_filter = Optional.presentIfNotNull(objectFilterMap),
                                ui_options = Optional.absent(),
                            ),
                        )
                    Log.i(TAG, "New SavedFilter: ${newSavedFilter.id}")
                }
                // Finish & start the filter list activity
                finishGuidedStepSupportFragments()
                val intent =
                    Intent(requireContext(), FilterListActivity::class.java)
                        .putDataType(filterArgs.dataType)
                        .putFilterArgs(FilterListActivity.INTENT_FILTER_ARGS, filterArgs)
                requireContext().startActivity(intent)
            }
        }
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == FILTER_NAME) {
            val submitAction = findActionById(SUBMIT)
            if (action.description.isNotNullOrBlank()) {
                submitAction.title = "Save & submit"
            } else {
                submitAction.title = "Submit"
            }
            notifyActionChanged(findActionPositionById(SUBMIT))
        }
        return GuidedAction.ACTION_ID_NEXT
    }

    companion object {
        private val TAG = CreateFilterStep::class.simpleName

        private const val SUBMIT = -1L
        private const val FILTER_NAME = -2L
        private const val FILTER_OPTIONS = -3L
        private const val SORT_OPTION = -4L
    }
}
