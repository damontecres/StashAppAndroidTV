package com.github.damontecres.stashapp.filter

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.leanback.app.GuidedStepSupportFragment
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.getDestination

class CreateFilterFragment : Fragment(R.layout.frame) {
    private val viewModel by activityViewModels<CreateFilterViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dest = requireArguments().getDestination<Destination.CreateFilter>()
        val startingFilter = dest.startingFilter
        viewModel.initialize(
            dest.dataType,
            startingFilter?.objectFilter,
            startingFilter?.findFilter,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.ready.observe(viewLifecycleOwner) { ready ->
            if (ready) {
                GuidedStepSupportFragment.add(
                    childFragmentManager,
                    CreateFilterStep(),
                    R.id.frame_content,
                )
            }
        }
    }

    companion object {
        private const val TAG = "CreateFilterFragment"
    }
}
