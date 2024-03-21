package com.github.damontecres.stashapp

import androidx.fragment.app.FragmentActivity
import androidx.leanback.tab.LeanbackTabLayout

open class TabbedGridFragmentActivity : FragmentActivity(R.layout.tabbed_grid_view) {
    override fun onStart() {
        super.onStart()

        val layout = findViewById<LeanbackTabLayout>(R.id.tab_layout)
        if (layout.childCount > 0) {
            layout.getChildAt(0).requestFocus()
        }
    }
}
