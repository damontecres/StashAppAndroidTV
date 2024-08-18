package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.views.SortButtonManager
import com.github.damontecres.stashapp.views.TabbedGridTitleView
import com.google.android.material.tabs.TabLayout

abstract class TabbedGridFragmentActivity : FragmentActivity(R.layout.tabbed_grid_view) {
    private lateinit var titleView: TabbedGridTitleView
    private lateinit var adapter: StashFragmentPagerAdapter
    private lateinit var sortButton: Button
    private lateinit var sortButtonManager: SortButtonManager
    private var currentTabPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView = findViewById<TabbedGridTitleView>(R.id.browse_title_group)
        findViewById<TextView>(R.id.grid_title).text = getTitleText()
        val viewPager = findViewById<LeanbackViewPager>(R.id.view_pager)
        val tabLayout = findViewById<LeanbackTabLayout>(R.id.tab_layout)

        adapter = getPagerAdapter()
        viewPager.adapter = adapter
//            TabLayoutMediator(tabLayout, viewPager, true) { tab, position ->
//                tab.text = adapter.items[position].title
//            }.attach()
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

        sortButton = findViewById<Button>(R.id.sort_button)
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

    /**
     * Get an optional text to set on the grid title
     */
    abstract fun getTitleText(): CharSequence?

    /**
     * Get the tab pager adapter, typical a ListFragmentPagerAdapter
     */
    abstract fun getPagerAdapter(): StashFragmentPagerAdapter

    companion object {
        private const val TAG = "TabbedGridFragmentActivity"
    }
}
