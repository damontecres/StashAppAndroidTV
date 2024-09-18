package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidedAction
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.CriterionModifier
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
            viewModel.initialize(dataType, startingFilter?.objectFilter, startingFilter?.findFilter)

            GuidedStepSupportFragment.addAsRoot(
                this,
                CreateFilterStep0(),
                android.R.id.content,
            )
        }
    }

//    override fun onPause() {
//        super.onPause()
//        if (isFinishing) {
//            val intent = Intent(this, FilterListActivity::class.java)
//            startActivity(intent)
//        }
//    }

    companion object {
        private const val TAG = "CreateFilterActivity"
        const val INTENT_STARTING_FILTER = "$TAG.startingFilter"

        const val MODIFIER_OFFSET = 3_000_000L
    }

    open class CreateFilterGuidedStepFragment : GuidedStepSupportFragment() {
        protected val viewModel by activityViewModels<CreateFilterViewModel>()

        fun nextStep(step: GuidedStepSupportFragment) {
            add(requireActivity().supportFragmentManager, step, android.R.id.content)
        }

        override fun onProvideTheme(): Int {
            return R.style.Theme_StashAppAndroidTV_GuidedStep
        }

        protected fun modifierAction(modifier: CriterionModifier): GuidedAction {
            return GuidedAction.Builder(requireContext())
                .id(MODIFIER_OFFSET + modifier.ordinal)
                .hasNext(false)
                .title(modifier.getString(requireContext()))
                .build()
        }
    }
}
