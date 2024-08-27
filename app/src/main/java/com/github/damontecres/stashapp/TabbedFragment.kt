package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.TabbedGridTitleView
import com.github.damontecres.stashapp.views.models.TabbedGridViewModel
import com.google.android.material.tabs.TabLayout

/**
 * A [Fragment]-only equivalent to [TabbedGridFragmentActivity]
 */
abstract class TabbedFragment : Fragment(R.layout.tabbed_grid_view) {
    protected val viewModel by activityViewModels<TabbedGridViewModel>()

    private lateinit var titleView: TabbedGridTitleView
    private lateinit var adapter: StashFragmentPagerAdapter
    private var currentTabPosition = 0

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        titleView = view.findViewById(R.id.browse_title_group)
        val gridTitle = view.findViewById<TextView>(R.id.grid_title)
        viewModel.title.observe(viewLifecycleOwner) {
            gridTitle.text = it
        }
        val title = getTitleText()
        if (title.isNotNullOrBlank()) {
            viewModel.title.value = getTitleText()
        }

        val viewPager = view.findViewById<LeanbackViewPager>(R.id.view_pager)
        val tabLayout = view.findViewById<LeanbackTabLayout>(R.id.tab_layout)

        adapter = getPagerAdapter(childFragmentManager)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    for (i in 0..<tabLayout.tabCount) {
                        if (tab == tabLayout.getTabAt(i)) {
                            currentTabPosition = i
                        }
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    // no-op
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    // no-op
                }
            },
        )
    }

    open fun getTitleText(): String? {
        return null
    }

    abstract fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter
}
