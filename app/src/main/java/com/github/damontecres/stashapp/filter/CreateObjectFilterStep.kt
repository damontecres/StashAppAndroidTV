package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.filter.picker.BooleanPickerFragment
import com.github.damontecres.stashapp.filter.picker.HierarchicalMultiCriterionFragment
import com.github.damontecres.stashapp.filter.picker.IntPickerFragment
import com.github.damontecres.stashapp.filter.picker.MultiCriterionFragment
import com.github.damontecres.stashapp.filter.picker.RatingPickerFragment
import com.github.damontecres.stashapp.filter.picker.StringPickerFragment
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import kotlinx.coroutines.launch

class CreateObjectFilterStep : CreateFilterActivity.CreateFilterGuidedStepFragment() {
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryEngine = QueryEngine(StashServer.requireCurrentServer())
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val text =
            filterSummary(
                viewModel.dataType.value!!.filterType,
                viewModel.filter.value!!,
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
        getFilterOptions(viewModel.dataType.value!!)
            .mapIndexed { index, filterOption ->
                filterOption as FilterOption<StashDataFilter, Any>
                val value = viewModel.getValue(filterOption)
                val description =
                    if (value != null) {
                        filterSummary(filterOption.name, value, viewModel::lookupIds)
                    } else {
                        null
                    }
                createAction(index, filterOption.nameStringId, description)
            }
            .sortedBy { it.title.toString() }
            .forEach {
                actions.add(it)
            }
    }

    override fun onCreateButtonActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(SUBMIT)
                .hasNext(true)
                .title(getString(R.string.stashapp_actions_submit))
                .build(),
        )
    }

    private fun createAction(
        index: Int,
        @StringRes nameId: Int,
        description: String?,
    ): GuidedAction {
        return GuidedAction.Builder(requireContext())
            .id(index.toLong())
            .hasNext(true)
            .title(nameId)
            .description(description)
            .build()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        val dataType = viewModel.dataType.value!!

        if (action.id == SUBMIT) {
            parentFragmentManager.popBackStack()
        } else {
            val filterOption = getFilterOptions(dataType)[action.id.toInt()]
            when (action.id.toInt()) {
                R.string.stashapp_rating -> {
                    filterOption as FilterOption<StashDataFilter, IntCriterionInput>
                    nextStep(RatingPickerFragment(filterOption))
                }

                else ->
                    when (filterOption.type) {
                        IntCriterionInput::class -> {
                            filterOption as FilterOption<StashDataFilter, IntCriterionInput>
                            nextStep(IntPickerFragment(filterOption))
                        }

                        Boolean::class -> {
                            filterOption as FilterOption<StashDataFilter, Boolean>
                            nextStep(BooleanPickerFragment(filterOption))
                        }

                        StringCriterionInput::class -> {
                            filterOption as FilterOption<StashDataFilter, StringCriterionInput>
                            nextStep(StringPickerFragment(filterOption))
                        }

                        MultiCriterionInput::class -> {
                            filterOption as FilterOption<StashDataFilter, MultiCriterionInput>
                            val value = viewModel.getValue(filterOption)
                            val ids =
                                value?.value?.getOrNull().orEmpty() +
                                    value?.excludes?.getOrNull().orEmpty()
                            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                                val items =
                                    queryEngine.getByIds(filterOption.dataType!!, ids)
                                        .associateBy { it.id }
                                nextStep(
                                    MultiCriterionFragment(
                                        filterOption.dataType,
                                        filterOption,
                                        items,
                                    ),
                                )
                            }
                        }

                        HierarchicalMultiCriterionInput::class -> {
                            filterOption as FilterOption<StashDataFilter, HierarchicalMultiCriterionInput>
                            val value = viewModel.getValue(filterOption)
                            val ids =
                                value?.value?.getOrNull().orEmpty() +
                                    value?.excludes?.getOrNull().orEmpty()
                            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                                val items =
                                    queryEngine.getByIds(filterOption.dataType!!, ids)
                                        .associateBy { it.id }
                                nextStep(
                                    HierarchicalMultiCriterionFragment(
                                        filterOption.dataType,
                                        filterOption,
                                        items,
                                    ),
                                )
                            }
                        }

                        else -> TODO()
                    }
            }
        }
    }

    companion object {
        private val TAG = CreateObjectFilterStep::class.simpleName

        private const val SUBMIT = GuidedAction.ACTION_ID_OK
    }
}
