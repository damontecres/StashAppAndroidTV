package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.filter.picker.IntPickerFragment
import kotlin.reflect.full.declaredMemberProperties

class CreateFilterStep0 : CreateFilterActivity.CreateFilterGuidedStepFragment() {
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
            }
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
        when (action.id.toInt()) {
            R.string.stashapp_performer_count -> {
                val filterOption =
                    SceneFilterOptionsMap[R.string.stashapp_performer_count]
                        as FilterOption<SceneFilterType, IntCriterionInput>
                requireActivity().supportFragmentManager.beginTransaction()
                    .addToBackStack("picker")
                    .replace(
                        android.R.id.content,
                        IntPickerFragment(getString(filterOption.nameStringId), filterOption),
                    )
                    .commit()
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
