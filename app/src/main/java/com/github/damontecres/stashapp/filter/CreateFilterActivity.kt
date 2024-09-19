package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.StashDataFilter
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.getFilterArgs
import com.github.damontecres.stashapp.views.getString

class CreateFilterActivity : FragmentActivity(R.layout.frame_layout) {
    private val viewModel by viewModels<CreateFilterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val dataType = intent.getDataType()
            val startingFilter = intent.getFilterArgs(INTENT_STARTING_FILTER)
            viewModel.initialize(
                dataType,
                startingFilter?.objectFilter,
                startingFilter?.findFilter,
            ) {
                // This occurs after the items are loaded for labels
                GuidedStepSupportFragment.addAsRoot(
                    this,
                    CreateFilterStep(),
                    android.R.id.content,
                )
            }
        }
    }

    companion object {
        private const val TAG = "CreateFilterActivity"
        const val INTENT_STARTING_FILTER = "$TAG.startingFilter"
    }

    open class CreateFilterGuidedStepFragment : GuidedStepSupportFragment() {
        protected val viewModel by activityViewModels<CreateFilterViewModel>()

        fun nextStep(step: GuidedStepSupportFragment) {
            add(requireActivity().supportFragmentManager, step, android.R.id.content)
        }

        override fun onProvideTheme(): Int {
            return R.style.Theme_StashAppAndroidTV_GuidedStep
        }

        /**
         * Create a [GuidedAction] for a [CriterionModifier]
         */
        protected fun modifierAction(modifier: CriterionModifier): GuidedAction {
            return GuidedAction.Builder(requireContext())
                .id(MODIFIER_OFFSET + modifier.ordinal)
                .hasNext(false)
                .title(modifier.getString(requireContext()))
                .build()
        }

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
        ) {
            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(GuidedAction.ACTION_ID_FINISH)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_actions_save))
                    .build(),
            )

            if (viewModel.getValue(filterOption) != null) {
                actions.add(
                    GuidedAction.Builder(requireContext())
                        .id(ACTION_ID_REMOVE)
                        .hasNext(true)
                        .title(getString(R.string.stashapp_actions_remove))
                        .build(),
                )
            }

            actions.add(
                GuidedAction.Builder(requireContext())
                    .id(GuidedAction.ACTION_ID_CANCEL)
                    .hasNext(true)
                    .title(getString(R.string.stashapp_actions_cancel))
                    .build(),
            )
        }

        companion object {
            const val MODIFIER_OFFSET = 3_000_000L
            const val ACTION_ID_REMOVE = -234L
        }
    }
}
