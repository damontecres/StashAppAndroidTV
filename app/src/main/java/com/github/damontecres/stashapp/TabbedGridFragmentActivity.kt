package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.viewpager.widget.PagerAdapter

abstract class TabbedGridFragmentActivity : FragmentActivity(R.layout.tabbed_grid_view) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            findViewById<TextView>(R.id.grid_title).text = getTitleText()
            val viewPager = findViewById<LeanbackViewPager>(R.id.view_pager)
            val tabLayout = findViewById<LeanbackTabLayout>(R.id.tab_layout)

            viewPager.adapter = getPagerAdapter()
            tabLayout.setupWithViewPager(viewPager)
            if (tabLayout.childCount > 0) {
                tabLayout.getChildAt(0).requestFocus()
            }
        }
    }

    /**
     * Get an optional text to set on the grid title
     */
    abstract fun getTitleText(): CharSequence?

    /**
     * Get the tab pager adapter, typical a ListFragmentPagerAdapter
     */
    abstract fun getPagerAdapter(): PagerAdapter
}
