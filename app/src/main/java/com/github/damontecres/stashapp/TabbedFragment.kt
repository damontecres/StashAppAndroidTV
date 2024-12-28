package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.util.DefaultKeyEventCallback
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.TabbedGridViewModel
import com.google.android.material.tabs.TabLayout

/**
 * A [Fragment] that displays multiple tabs
 */
abstract class TabbedFragment(
    val tabKey: String,
) : Fragment(R.layout.tabbed_grid_view),
    DefaultKeyEventCallback {
    private lateinit var viewPager: LeanbackViewPager
    protected val serverViewModel by activityViewModels<ServerViewModel>()
    protected val tabViewModel by viewModels<TabbedGridViewModel>()

    private lateinit var tabLayout: LeanbackTabLayout
    private lateinit var adapter: StashFragmentPagerAdapter
    private var currentTabPosition = 0

    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val rememberTab =
            preferences.getBoolean(getString(R.string.pref_key_ui_remember_tab), false)
        val rememberTabKey = getString(R.string.pref_key_ui_remember_tab) + ".$tabKey"
        val rememberedTabIndex = if (rememberTab) preferences.getInt(rememberTabKey, 0) else 0

        var firstTime = true

        tabViewModel.tabs.observe(this) { pages ->
            adapter = StashFragmentPagerAdapter(pages, childFragmentManager)
            adapter.fragmentCreatedListener = { fragment, position ->
                if (fragment is StashGridControlsFragment) {
                    fragment.titleView = tabLayout
                }
                fragments[position] = fragment
            }
            viewPager.adapter = adapter
            if (savedInstanceState == null && tabLayout.childCount > 0 && firstTime) {
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

            if (firstTime && rememberTab) {
                val tabIndex =
                    if (rememberedTabIndex < tabLayout.tabCount) rememberedTabIndex else 0
                val tab = tabLayout.getTabAt(tabIndex)
                tabLayout.selectTab(tab, true)
                tab?.view?.requestFocus()
            }
            firstTime = false
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        val gridTitle = view.findViewById<TextView>(R.id.grid_title)
        tabViewModel.title.observe(viewLifecycleOwner) {
            gridTitle.text = it
        }

        viewPager = view.findViewById(R.id.view_pager)
        tabLayout = view.findViewById(R.id.tab_layout)
        tabLayout.setupWithViewPager(viewPager)
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent,
    ): Boolean {
        val tabIndex = currentTabPosition
        val fragment = fragments[tabIndex]
        if (fragment != null && fragment is KeyEvent.Callback && fragment.onKeyUp(keyCode, event)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    companion object {
        private const val TAG = "TabbedFragment"
    }
}
