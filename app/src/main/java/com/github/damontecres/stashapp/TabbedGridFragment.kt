package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.TabbedGridTitleView
import com.github.damontecres.stashapp.views.models.TabbedGridViewModel
import com.google.android.material.tabs.TabLayout

abstract class TabbedGridFragment : Fragment(R.layout.tabbed_grid_view) {
    private val viewModel by activityViewModels<TabbedGridViewModel>()

    private lateinit var titleView: TabbedGridTitleView
    private lateinit var adapter: StashFragmentPagerAdapter
    private lateinit var sortButton: Button
    private lateinit var sortButtonManager: SortButtonManager
    private var currentTabPosition = 0

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        titleView = view.findViewById<TabbedGridTitleView>(R.id.browse_title_group)
        val gridTitle = view.findViewById<TextView>(R.id.grid_title)
        viewModel.title.observe(viewLifecycleOwner) {
            gridTitle.text = it
        }

        val viewPager = view.findViewById<LeanbackViewPager>(R.id.view_pager)
        val tabLayout = view.findViewById<LeanbackTabLayout>(R.id.tab_layout)

        adapter = getPagerAdapter(childFragmentManager)
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
        if (tabLayout.childCount > 0) {
            tabLayout.getChildAt(0).requestFocus()
        }

        for (i in 0..<adapter.count) {
            val fragment = adapter.getItem(i) as StashGridFragment

            fragment.sortButtonEnabled = true

            fragment.lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        fragment.setTitleView(titleView)
                    }
                },
            )
        }

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    for (i in 0..<tabLayout.tabCount) {
                        if (tab == tabLayout.getTabAt(i)) {
                            currentTabPosition = i
                            val fragment =
                                adapter.getItem(i) as StashGridFragment

                            Log.v(
                                TAG,
                                "Got tab ${tab.text}, currentSortAndDirection=${fragment.currentSortAndDirection}",
                            )
                            sortButtonManager.setUpSortButton(
                                sortButton,
                                fragment.dataType,
                                fragment.currentSortAndDirection,
                            )
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

        sortButton = view.findViewById<Button>(R.id.sort_button)
        sortButtonManager =
            SortButtonManager {
                val fragment = adapter.getItem(currentTabPosition) as StashGridFragment
                fragment.refresh(it)
            }
        val startFragment = adapter.getItem(0) as StashGridFragment
        sortButtonManager.setUpSortButton(
            sortButton,
            startFragment.dataType,
            startFragment.currentSortAndDirection,
        )
    }

    abstract fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter

    companion object {
        private const val TAG = "TabbedGridFragment"
    }
}
