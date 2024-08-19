package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.annotation.LayoutRes
import androidx.fragment.app.FragmentActivity
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.TabbedGridTitleView
import com.github.damontecres.stashapp.views.models.TabbedGridViewModel
import com.google.android.material.tabs.TabLayout

abstract class TabbedGridFragmentActivity(
    @LayoutRes layoutId: Int = R.layout.tabbed_grid_view,
) : FragmentActivity(layoutId) {
    protected val viewModel by viewModels<TabbedGridViewModel>()

    private lateinit var titleView: TabbedGridTitleView
    private lateinit var adapter: StashFragmentPagerAdapter
    private var currentTabPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        titleView = findViewById<TabbedGridTitleView>(R.id.browse_title_group)
        val gridTitle = findViewById<TextView>(R.id.grid_title)
        viewModel.title.observe(this) {
            gridTitle.text = it
        }
        val title = getTitleText()
        if (title.isNotNullOrBlank()) {
            viewModel.title.value = getTitleText()
        }

        val viewPager = findViewById<LeanbackViewPager>(R.id.view_pager)
        val tabLayout = findViewById<LeanbackTabLayout>(R.id.tab_layout)

        adapter = getPagerAdapter()
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
        if (tabLayout.childCount > 0) {
            tabLayout.getChildAt(0).requestFocus()
        }

        for (i in 0..<adapter.count) {
            val fragment = adapter.getItem(i)
            if (fragment is StashGridFragment) {
                fragment.sortButtonEnabled = true
            }
        }

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

    /**
     * Get an optional text to set on the grid title
     */
    open fun getTitleText(): String? {
        return null
    }

    /**
     * Get the tab pager adapter, typical a ListFragmentPagerAdapter
     */
    abstract fun getPagerAdapter(): StashFragmentPagerAdapter

    companion object {
        private const val TAG = "TabbedGridFragmentActivity"
    }
}
