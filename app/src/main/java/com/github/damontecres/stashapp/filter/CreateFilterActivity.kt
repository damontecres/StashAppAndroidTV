package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.util.getFilterArgs

class CreateFilterActivity : FragmentActivity(R.layout.frame_layout) {
    private val viewModel by viewModels<CreateFilterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val filter =
                intent.getFilterArgs(INTENT_STARTING_FILTER)?.objectFilter as SceneFilterType?
                    ?: SceneFilterType()
            viewModel.filter.value = filter

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
    }

    open class CreateFilterGuidedStepFragment : GuidedStepSupportFragment() {
        protected val viewModel by activityViewModels<CreateFilterViewModel>()

        fun nextStep(step: GuidedStepSupportFragment) {
            add(requireActivity().supportFragmentManager, step, android.R.id.content)
        }

        override fun onProvideTheme(): Int {
            return R.style.Theme_StashAppAndroidTV_GuidedStep
        }
    }
}
