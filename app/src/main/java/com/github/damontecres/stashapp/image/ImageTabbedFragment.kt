package com.github.damontecres.stashapp.image

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.TabbedFragment
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.views.TabbedGridTitleView

class ImageTabbedFragment : TabbedFragment() {
    private val imageViewModel: ImageViewModel by activityViewModels<ImageViewModel>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val titleBar = view.findViewById<TabbedGridTitleView>(R.id.browse_title_group)
        titleBar.setBackgroundColor(requireContext().getColor(R.color.transparent_black_50))

        val title = view.findViewById<TextView>(R.id.grid_title)
        imageViewModel.image.observe(viewLifecycleOwner) { newImage ->
            title.text = newImage.title
        }
    }

    override fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter {
        val tabs =
            listOf(
                StashFragmentPagerAdapter.PagerEntry("Overlay", null),
                StashFragmentPagerAdapter.PagerEntry("Details", null),
            )
        return object : StashFragmentPagerAdapter(tabs, fm) {
            override fun getFragment(position: Int): Fragment {
                return when (position) {
                    0 -> {
                        ImageOverlayFragment()
                    }

                    1 -> {
                        ImageDetailsFragment()
                    }

                    else -> throw IllegalStateException("No fragment for position $position")
                }
            }
        }
    }
}
