package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.filter.picker.HierarchicalMultiCriterionFragment
import com.github.damontecres.stashapp.filter.picker.IntPickerFragment
import com.github.damontecres.stashapp.filter.picker.MultiCriterionFragment
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
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
                if (obj != Optional.Absent) {
                    param.name to obj.getOrNull()
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
    }

    override fun onSubGuidedActionClicked(action: GuidedAction): Boolean {
        val curVal = viewModel.filter.value!!
        val filterOption = SceneFilterOptionsMap[action.id.toInt()]!!
        when (filterOption.type) {
            IntCriterionInput::class -> {
                requireActivity().supportFragmentManager.beginTransaction()
                    .addToBackStack("picker")
                    .replace(
                        android.R.id.content,
                        IntPickerFragment(
                            getString(filterOption.nameStringId),
                            filterOption as FilterOption<SceneFilterType, IntCriterionInput>,
                        ),
                    )
                    .commit()
            }
        }
        when (filterOption.nameStringId) {
            R.string.stashapp_tags -> {
                val tagIds =
                    curVal.tags.getOrNull()?.value?.getOrNull()
                        .orEmpty() + curVal.tags.getOrNull()?.excludes?.getOrNull().orEmpty()
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val items = queryEngine.getTags(tagIds).associateBy { it.id }
                    nextStep(
                        HierarchicalMultiCriterionFragment(
                            DataType.TAG,
                            filterOption as FilterOption<SceneFilterType, HierarchicalMultiCriterionInput>,
                            items,
                        ),
                    )
                }
            }

            R.string.stashapp_performers -> {
                val performerIds =
                    curVal.performers.getOrNull()?.value?.getOrNull()
                        .orEmpty() + curVal.performers.getOrNull()?.excludes?.getOrNull().orEmpty()
                viewLifecycleOwner.lifecycleScope.launch(StashCoroutineExceptionHandler()) {
                    val items =
                        queryEngine.findPerformers(performerIds = performerIds)
                            .associateBy { it.id }
                    nextStep(
                        MultiCriterionFragment(
                            DataType.PERFORMER,
                            filterOption as FilterOption<SceneFilterType, MultiCriterionInput>,
                            items,
                        ),
                    )
                }
            }
        }
        return false
    }

    companion object {
        private const val SUBMIT = -1L
        private const val FILTER_NAME = -2L
        private const val FILTER_OPTIONS = -2L
    }
}
