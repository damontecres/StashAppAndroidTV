package com.github.damontecres.stashapp.filter

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.FilterListActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SaveFilterInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.filter.output.FilterWriter
import com.github.damontecres.stashapp.filter.picker.BooleanPickerFragment
import com.github.damontecres.stashapp.filter.picker.HierarchicalMultiCriterionFragment
import com.github.damontecres.stashapp.filter.picker.IntPickerFragment
import com.github.damontecres.stashapp.filter.picker.MultiCriterionFragment
import com.github.damontecres.stashapp.filter.picker.StringPickerFragment
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.MutationEngine
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.util.putDataType
import com.github.damontecres.stashapp.util.putFilterArgs
import kotlinx.coroutines.launch
import kotlin.reflect.full.declaredMemberProperties

class CreateFilterStep0 : CreateFilterActivity.CreateFilterGuidedStepFragment() {
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queryEngine = QueryEngine(StashServer.requireCurrentServer())
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        val filter = viewModel.filter.value!!

        val params =
            SceneFilterType::class.declaredMemberProperties.mapNotNull { param ->
                val obj = param.get(filter) as Optional<*>
                if (obj.getOrNull() != null) {
                    param.name to obj.getOrNull()!!
                } else {
                    null
                }
            }.sortedBy { it.first }
        val text =
            params.joinToString("\n") {
                "${it.first}: ${it.second}"
            }

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

        val options =
            SceneFilterOptions.map {
                createAction(it.nameStringId)
            }.sortedBy { it.title.toString() }
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(FILTER_OPTIONS)
                .hasNext(true)
                .subActions(options)
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

    private fun createAction(
        @StringRes nameId: Int,
    ): GuidedAction {
        return GuidedAction.Builder(requireContext())
            .id(nameId.toLong())
            .hasNext(true)
            .title(nameId)
            .build()
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == SUBMIT) {
            val objectFilter = viewModel.filter.value!!
            val filterArgs =
                FilterArgs(
                    dataType = DataType.SCENE,
                    objectFilter = objectFilter,
                )
            val filterNameAction = findActionById(FILTER_NAME)
            viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                if (filterNameAction.description.isNotNullOrBlank()) {
                    // Save it
                    val filterWriter = FilterWriter(QueryEngine(StashServer.requireCurrentServer()))
                    val findFilter =
                        filterArgs.findFilter ?: StashFindFilter(
                            null,
                            filterArgs.dataType.defaultSort,
                        )
                    val objectFilterMap = filterWriter.convertSceneFilterType(objectFilter)
                    val mutationEngine = MutationEngine(StashServer.requireCurrentServer())
                    val newSavedFilter =
                        mutationEngine.saveFilter(
                            SaveFilterInput(
                                mode = DataType.SCENE.filterMode,
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

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        val curVal = viewModel.filter.value!!
        val filterOption = SceneFilterOptionsMap[action.id.toInt()]!!
        when (filterOption.type) {
            IntCriterionInput::class -> {
                requireActivity().supportFragmentManager.commit {
                    addToBackStack("picker")
                    replace(
                        android.R.id.content,
                        IntPickerFragment(
                            getString(filterOption.nameStringId),
                            filterOption as FilterOption<SceneFilterType, IntCriterionInput>,
                        ),
                    )
                }
            }

            Boolean::class -> {
                filterOption as FilterOption<SceneFilterType, Boolean>
                nextStep(BooleanPickerFragment(filterOption))
            }

            StringCriterionInput::class -> {
                filterOption as FilterOption<SceneFilterType, StringCriterionInput>
                nextStep(StringPickerFragment(filterOption))
            }

            MultiCriterionInput::class -> {
                filterOption as FilterOption<SceneFilterType, MultiCriterionInput>
                val value = filterOption.getter(curVal)
                val ids =
                    value.getOrNull()?.value?.getOrNull()
                        .orEmpty() + value.getOrNull()?.excludes?.getOrNull().orEmpty()
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val items =
                        queryEngine.getByIds(filterOption.dataType!!, ids).associateBy { it.id }
                    nextStep(MultiCriterionFragment(filterOption.dataType, filterOption, items))
                }
            }

            HierarchicalMultiCriterionInput::class -> {
                filterOption as FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>
                val value = filterOption.getter(curVal)
                val ids =
                    value.getOrNull()?.value?.getOrNull()
                        .orEmpty() + value.getOrNull()?.excludes?.getOrNull().orEmpty()
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val items =
                        queryEngine.getByIds(filterOption.dataType!!, ids).associateBy { it.id }
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
        return false
    }

    companion object {
        private val TAG = CreateFilterStep0::class.simpleName

        private const val SUBMIT = -1_000L
        private const val FILTER_NAME = -2_000L
        private const val FILTER_OPTIONS = -3_000L
    }
}
