package com.github.damontecres.stashapp.filter

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.getDataType
import com.github.damontecres.stashapp.util.getFilterArgs

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
}
