package com.github.damontecres.stashapp.filter

import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SaveFilterInput
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.readOnlyModeDisabled
import com.github.damontecres.stashapp.views.dialog.ConfirmationDialogFragment
import com.github.damontecres.stashapp.views.formatNumber
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

/**
 * The first step to create a new filter
 *
 * Assumes that [CreateFilterViewModel.initialize] has been called already
 */
class CreateFilterStep : CreateFilterGuidedStepFragment() {
    private val serverViewModel: ServerViewModel by activityViewModels()

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val text =
            filterSummary(
                requireContext(),
                viewModel.dataType.value!!,
                viewModel.dataType.value!!.filterType,
                viewModel.objectFilter.value!!,
                serverViewModel.requireServer().serverPreferences.ratingsAsStars,
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
                .descriptionEditInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
                .descriptionEditable(true)
                .description(viewModel.filterName.value)
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
        val countStr =
            formatNumber(
                count,
                serverViewModel.requireServer().serverPreferences.abbreviateCounters,
            )
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
            val submitAction =
                GuidedAction
                    .Builder(requireContext())
                    .id(SAVE_SUBMIT)
                    .hasNext(true)
                    .title("Save and submit")
                    .build()
            updateSaveAndSubmit(submitAction, viewModel.filterName.value)
            actions.add(submitAction)
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
                val countStr =
                    formatNumber(
                        count,
                        serverViewModel.requireServer().serverPreferences.abbreviateCounters,
                    )
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
            val filterName = viewModel.filterName.value
            val objectFilter = viewModel.objectFilter.value!!
            val filterArgs =
                FilterArgs(
                    dataType = dataType,
                    name = filterName,
                    findFilter = viewModel.findFilter.value,
                    objectFilter = objectFilter,
                ).withResolvedRandom()
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                // If there is a name, try to save it to the server
                if (action.id == SAVE_SUBMIT && filterName.isNotNullOrBlank()) {
                    val queryEngine = QueryEngine(serverViewModel.requireServer())
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
                    val existingId = viewModel.getSavedFilterId(filterName)
                    val newFilterInput =
                        SaveFilterInput(
                            id = Optional.presentIfNotNull(existingId),
                            mode = dataType.filterMode,
                            name = filterName,
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
                                filterName,
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
            val mutationEngine = MutationEngine(serverViewModel.requireServer())
            val newSavedFilter = mutationEngine.saveFilter(newFilterInput)
            Log.i(TAG, "New SavedFilter: ${newSavedFilter.id}")
        }

        serverViewModel.navigationManager.goBack()
        serverViewModel.navigationManager.navigate(Destination.Filter(filterArgs))
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        if (action.id == FILTER_NAME) {
            val filterName = action.description?.toString()?.ifBlank { null }
            viewModel.filterName.value = filterName
            val submitAction = findActionById(SAVE_SUBMIT)
            if (submitAction != null) {
                // In read-only, this action doesn't exist
                updateSaveAndSubmit(submitAction, filterName)
                notifyActionChanged(findActionPositionById(SAVE_SUBMIT))
            }
        }
        return GuidedAction.ACTION_ID_NEXT
    }

    private fun updateSaveAndSubmit(
        submitAction: GuidedAction,
        filterName: String?,
    ) {
        val id = viewModel.getSavedFilterId(filterName)
        submitAction.isEnabled = filterName.isNotNullOrBlank()
        submitAction.description =
            if (submitAction.isEnabled && id.isNotNullOrBlank()) {
                getString(R.string.save_and_submit_overwrite)
            } else if (submitAction.isEnabled) {
                getString(R.string.stashapp_actions_save_filter)
            } else {
                getString(R.string.save_and_submit_no_name_desc)
            }
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
