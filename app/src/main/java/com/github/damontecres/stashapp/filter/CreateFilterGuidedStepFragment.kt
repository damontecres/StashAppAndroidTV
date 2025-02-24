package com.github.damontecres.stashapp.filter

import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidedAction
import androidx.leanback.widget.GuidedActionsStylist
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.views.getString

abstract class CreateFilterGuidedStepFragment : GuidedStepSupportFragment() {
    protected val viewModel: CreateFilterViewModel by activityViewModels()

    fun nextStep(step: GuidedStepSupportFragment) {
        add(requireActivity().supportFragmentManager, step, R.id.root_fragment)
    }

    override fun onProvideTheme(): Int = R.style.Theme_StashAppAndroidTV_GuidedStep

    override fun onCreateActionsStylist(): GuidedActionsStylist = StashGuidedActionsStylist()

    /**
     * Create a [GuidedAction] for a [CriterionModifier]
     */
    protected fun modifierAction(modifier: CriterionModifier): GuidedAction =
        GuidedAction
            .Builder(requireContext())
            .id(MODIFIER_OFFSET + modifier.ordinal)
            .hasNext(false)
            .title(modifier.getString(requireContext()))
            .build()

    /**
     * Enable or disable the "finish" [GuidedAction].
     *
     * The step must define a [GuidedAction] with ID=[GuidedAction.ACTION_ID_FINISH] or this will throw an exception.
     */
    protected fun enableFinish(enabled: Boolean) {
        findActionById(GuidedAction.ACTION_ID_FINISH).isEnabled = enabled
        notifyActionChanged(findActionPositionById(GuidedAction.ACTION_ID_FINISH))
    }

    /**
     * Handle cancel & remove actions
     */
    protected fun <T : Any> onStandardActionClicked(
        action: GuidedAction,
        filterOption: FilterOption<StashDataFilter, T>,
    ) {
        if (action.id == GuidedAction.ACTION_ID_CANCEL) {
            parentFragmentManager.popBackStack()
        } else if (action.id == ACTION_ID_REMOVE) {
            viewModel.updateFilter(filterOption, null)
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Add save, remove, & cancel actions
     */
    protected fun <T : Any> addStandardActions(
        actions: MutableList<GuidedAction>,
        filterOption: FilterOption<StashDataFilter, T>,
        finishEnabled: Boolean? = null,
    ) {
        val currVal = viewModel.getValue(filterOption)

        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_FINISH)
                .hasNext(true)
                .title(getString(com.github.damontecres.stashapp.R.string.stashapp_actions_save))
                .enabled(finishEnabled ?: (currVal != null))
                .build(),
        )

        if (currVal != null) {
            actions.add(
                GuidedAction
                    .Builder(requireContext())
                    .id(ACTION_ID_REMOVE)
                    .hasNext(true)
                    .title(getString(com.github.damontecres.stashapp.R.string.stashapp_actions_remove))
                    .build(),
            )
        }

        actions.add(
            GuidedAction
                .Builder(requireContext())
                .id(GuidedAction.ACTION_ID_CANCEL)
                .hasNext(true)
                .title(getString(com.github.damontecres.stashapp.R.string.stashapp_actions_cancel))
                .build(),
        )
    }

    fun CriterionModifier.hasTwoValues(): Boolean = this == CriterionModifier.BETWEEN || this == CriterionModifier.NOT_BETWEEN

    fun CriterionModifier.isNullModifier(): Boolean = this == CriterionModifier.IS_NULL || this == CriterionModifier.NOT_NULL

    companion object {
        const val MODIFIER_OFFSET = 3_000_000L
        const val ACTION_ID_REMOVE = -234L
    }
}
