package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.isNotNullOrBlank
import com.github.damontecres.stashapp.views.TabbedGridTitleView
import com.github.damontecres.stashapp.views.models.TabbedGridViewModel
import com.google.android.material.tabs.TabLayout

/**
 * A [Fragment] that displays multiple tabs
 */
abstract class TabbedFragment(val tabKey: String) : Fragment(R.layout.tabbed_grid_view) {
    protected val viewModel by activityViewModels<TabbedGridViewModel>()

    private lateinit var titleView: TabbedGridTitleView
    private lateinit var tabLayout: LeanbackTabLayout
    private lateinit var adapter: StashFragmentPagerAdapter
    private var currentTabPosition = 0

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val rememberTab =
            preferences.getBoolean(getString(R.string.pref_key_ui_remember_tab), false)
        val rememberTabKey = getString(R.string.pref_key_ui_remember_tab) + ".$tabKey"
        val rememberedTabIndex = if (rememberTab) preferences.getInt(rememberTabKey, 0) else 0

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
        tabLayout = view.findViewById<LeanbackTabLayout>(R.id.tab_layout)

        adapter = getPagerAdapter(childFragmentManager)
        adapter.fragmentCreatedListener = { fragment, position ->
            if (fragment is StashGridFragment) {
                fragment.titleView = tabLayout
            }
        }
        viewPager.adapter = adapter
        tabLayout.setupWithViewPager(viewPager)
        if (savedInstanceState == null && tabLayout.childCount > 0) {
            tabLayout.getChildAt(0).requestFocus()
        }

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    for (i in 0..<tabLayout.tabCount) {
                        if (tab == tabLayout.getTabAt(i)) {
                            Log.v(TAG, "onTabSelected: currentTabPosition=$i")
                            currentTabPosition = i
                            if (rememberTab) {
                                preferences.edit {
                                    putInt(rememberTabKey, i)
                                }
                            }
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

        if (rememberTab) {
            val tabIndex = if (rememberedTabIndex < tabLayout.tabCount) rememberedTabIndex else 0
            val tab = tabLayout.getTabAt(tabIndex)
            tabLayout.selectTab(tab, true)
            tab?.view?.requestFocus()
        }
    }

//    override fun onStart() {
//        super.onStart()
//        if (tabLayout.selectedTabPosition >= 0) {
//            tabLayout.getChildAt(tabLayout.selectedTabPosition)?.requestFocus()
//        }
//    }

    open fun getTitleText(): String? {
        return null
    }

    abstract fun getPagerAdapter(fm: FragmentManager): StashFragmentPagerAdapter

    companion object {
        private const val TAG = "TabbedFragment"
    }
}
