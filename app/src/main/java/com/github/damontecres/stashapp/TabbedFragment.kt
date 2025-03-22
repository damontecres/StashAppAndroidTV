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
import com.github.damontecres.stashapp.util.DelegateKeyEventCallback
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.animateToInvisible
import com.github.damontecres.stashapp.util.animateToVisible
import com.github.damontecres.stashapp.views.models.ServerViewModel
import com.github.damontecres.stashapp.views.models.TabbedGridViewModel
import com.google.android.material.tabs.TabLayout

/**
 * A [Fragment] that displays multiple tabs
 */
abstract class TabbedFragment(
    private val tabKey: String,
) : Fragment(R.layout.tabbed_grid_view),
    DelegateKeyEventCallback,
    StashGridControlsFragment.HeaderVisibilityListener {
    private lateinit var viewPager: LeanbackViewPager
    protected val serverViewModel by activityViewModels<ServerViewModel>()
    protected val tabViewModel by viewModels<TabbedGridViewModel>()

    private lateinit var tabLayout: LeanbackTabLayout
    private lateinit var adapter: StashFragmentPagerAdapter
    private var currentTabPosition = 0
    private var firstTime = true

    private val fragments = mutableMapOf<Int, Fragment>()

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

        tabViewModel.tabs.observe(viewLifecycleOwner) { pages ->
            adapter = StashFragmentPagerAdapter(pages, childFragmentManager)
            adapter.fragmentCreatedListener = { fragment, position ->
                if (fragment is StashGridControlsFragment) {
                    fragment.headerVisibilityListener = this@TabbedFragment
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

        val gridTitle = view.findViewById<TextView>(R.id.grid_title)
        tabViewModel.title.observe(viewLifecycleOwner) {
            gridTitle.text = it
        }

        viewPager = view.findViewById(R.id.view_pager)
        tabLayout = view.findViewById(R.id.tab_layout)
        tabLayout.setupWithViewPager(viewPager)
        // Hide tabLayout if needed
        fragments.values.forEach {
            if (it is StashGridControlsFragment) {
                it.headerVisibilityListener = this
                if (!it.headerShowing) {
                    Log.v(TAG, "Making tabLayout gone")
                    tabLayout.visibility = View.GONE
                }
            }
        }
    }

    override val keyEventDelegate: KeyEvent.Callback?
        get() {
            val tabIndex = currentTabPosition
            val fragment = fragments[tabIndex]
            return if (fragment != null && fragment is KeyEvent.Callback) {
                fragment
            } else {
                null
            }
        }

    override fun onHeaderVisibilityChanged(
        fragment: StashGridControlsFragment,
        headerShowing: Boolean,
    ) {
        if (fragment == fragments[currentTabPosition]) {
//            Log.v(TAG, "onHeaderVisibilityChanged: headerShowing=$headerShowing")
            if (headerShowing) {
                tabLayout.animateToVisible()
            } else {
                tabLayout.animateToInvisible(View.GONE)
            }
        }
    }

    companion object {
        private const val TAG = "TabbedFragment"
    }
}
