package com.github.damontecres.stashapp.filter

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
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
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.views.dialog.ConfirmationDialogFragment
import com.github.damontecres.stashapp.views.formatNumber
import kotlinx.coroutines.launch

/**
 * The first step to create a new filter
 *
 * Assumes that [CreateFilterViewModel.initialize] has been called already
 */
class CreateFilterStep : CreateFilterGuidedStepFragment() {
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryEngine = QueryEngine(StashServer.requireCurrentServer())
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val text =
            filterSummary(
                viewModel.dataType.value!!,
                viewModel.dataType.value!!.filterType,
                viewModel.objectFilter.value!!,
                viewModel::lookupIds,
            ).ifBlank { "No filters set" }
        val typeStr = getString(viewModel.dataType.value!!.stringId)
        return GuidanceStylist.Guidance(
            "Create $typeStr filter",
            text,
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    private fun updateActions(actions: MutableList<GuidedAction>) {
        actions.add(
            GuidedAction
                .Builder(requireContext())
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
            GuidedAction
                .Builder(requireContext())
                .id(SORT_OPTION)
                .hasNext(true)
                .title(getString(R.string.sort_by))
                .description(sortDesc)
                .build(),
        )

        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(FILTER_OPTIONS)
                .hasNext(true)
                .title(getString(R.string.stashapp_filters))
                .build(),
        )
        val count = viewModel.resultCount.value ?: -1
        val countStr = formatNumber(count, viewModel.abbreviateCounters)
        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(SUBMIT)
                .hasNext(true)
                .title("Submit without saving")
                .description(if (count >= 0) "$countStr results" else "Querying...")
                .build(),
        )
        if (readOnlyModeDisabled()) {
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(SAVE_SUBMIT)
                    .hasNext(true)
                    .enabled(false)
                    .title("Save and submit")
                    .description(getString(R.string.save_and_submit_no_name_desc))
                    .build(),
            )
        }
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        updateActions(actions)
    }

    override fun onResume() {
        super.onResume()
        val refreshActions = mutableListOf<GuidedAction>()
        updateActions(refreshActions)
        actions = refreshActions
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.updateCount()
            viewModel.resultCount.observe(viewLifecycleOwner) { count ->
                val countStr = formatNumber(count, viewModel.abbreviateCounters)
                if (count >= 0) {
                    findActionById(SUBMIT).description = "$countStr results"
                    notifyActionChanged(findActionPositionById(SUBMIT))
                } else {
                    findActionById(SUBMIT).description = "Querying..."
                    notifyActionChanged(findActionPositionById(SUBMIT))
                }
            }
        }
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val dataType = viewModel.dataType.value!!
        if (action.id == FILTER_OPTIONS) {
            nextStep(CreateObjectFilterStep())
        } else if (action.id == SORT_OPTION) {
            nextStep(CreateFindFilterFragment(dataType, viewModel.findFilter.value!!))
        } else if (action.id == SUBMIT || action.id == SAVE_SUBMIT) {
            // Ready to load the filter!
            val filterNameAction = findActionById(FILTER_NAME)
            val objectFilter = viewModel.objectFilter.value!!
            val filterArgs =
                FilterArgs(
                    dataType = dataType,
                    name = filterNameAction.description?.toString()?.ifBlank { null },
                    findFilter = viewModel.findFilter.value,
                    objectFilter = objectFilter,
                ).withResolvedRandom()
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                // If there is a name, try to save it to the server
                if (action.id == SAVE_SUBMIT && filterArgs.name.isNotNullOrBlank()) {
                    val queryEngine = QueryEngine(viewModel.server.value!!)
                    // Save it
                    val filterWriter =
                        FilterWriter(dataType) { dataType, ids ->
                            queryEngine
                                .getByIds(dataType, ids)
                                .associate { it.id to extractTitle(it) }
                        }
                    val findFilter =
                        filterArgs.findFilter ?: StashFindFilter(
                            null,
                            filterArgs.dataType.defaultSort,
                        )
                    val objectFilterMap = filterWriter.convertFilter(objectFilter)
                    val existingId =
                        viewModel.getSavedFilterId(filterNameAction.description?.toString())
                    val newFilterInput =
                        SaveFilterInput(
                            id = Optional.presentIfNotNull(existingId),
                            mode = dataType.filterMode,
                            name = filterNameAction.description.toString(),
                            find_filter =
                                Optional.presentIfNotNull(
                                    findFilter.toFindFilterType(1, 40),
                                ),
                            object_filter = Optional.presentIfNotNull(objectFilterMap),
                            ui_options = Optional.absent(),
                        )
                    if (existingId.isNotNullOrBlank()) {
                        // Filter exists, so prompt for confirmation
                        ConfirmationDialogFragment.show(
                            childFragmentManager,
                            getString(
                                R.string.stashapp_dialogs_overwrite_filter_confirm,
                                filterNameAction.description.toString(),
                            ),
                        ) {
                            viewLifecycleOwner.lifecycleScope.launch(
                                StashCoroutineExceptionHandler(
                                    autoToast = true,
                                ),
                            ) {
                                saveAndFinish(filterArgs, newFilterInput)
                            }
                        }
                    } else {
                        saveAndFinish(filterArgs, newFilterInput)
                    }
                } else {
                    // Just show the results without saving
                    saveAndFinish(filterArgs, null)
                }
            }
        }
    }

    private suspend fun saveAndFinish(
        filterArgs: FilterArgs,
        newFilterInput: SaveFilterInput?,
    ) {
        if (newFilterInput != null) {
            val mutationEngine = MutationEngine(StashServer.requireCurrentServer())
            val newSavedFilter = mutationEngine.saveFilter(newFilterInput)
            Log.i(TAG, "New SavedFilter: ${newSavedFilter.id}")
        }
        finishGuidedStepSupportFragments()
        val intent =
            Intent(requireContext(), FilterListActivity::class.java)
                .putDataType(filterArgs.dataType)
                .putFilterArgs(FilterListActivity.INTENT_FILTER_ARGS, filterArgs)
        requireContext().startActivity(intent)
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == FILTER_NAME) {
            val submitAction = findActionById(SAVE_SUBMIT)
            val id = viewModel.getSavedFilterId(action.description?.toString())
            submitAction.isEnabled = action.description.isNotNullOrBlank()
            submitAction.description =
                if (submitAction.isEnabled && id.isNotNullOrBlank()) {
                    getString(R.string.save_and_submit_overwrite)
                } else if (submitAction.isEnabled) {
                    getString(R.string.stashapp_actions_save_filter)
                } else {
                    getString(R.string.save_and_submit_no_name_desc)
                }
            notifyActionChanged(findActionPositionById(SAVE_SUBMIT))
        }
        return GuidedAction.ACTION_ID_NEXT
    }

    companion object {
        private val TAG = CreateFilterStep::class.simpleName

        private const val SAVE_SUBMIT = -1L
        private const val SUBMIT = -2L
        private const val FILTER_NAME = -3L
        private const val FILTER_OPTIONS = -4L
        private const val SORT_OPTION = -5L
    }
}
