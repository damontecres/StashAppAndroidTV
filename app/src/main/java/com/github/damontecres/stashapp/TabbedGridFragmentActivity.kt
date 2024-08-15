package com.github.damontecres.stashapp

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.FragmentActivity
import androidx.leanback.tab.LeanbackTabLayout
import androidx.leanback.tab.LeanbackViewPager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.github.damontecres.stashapp.FilterListActivity.SortByArrayAdapter
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.util.ServerPreferences
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter
import com.github.damontecres.stashapp.util.getMaxMeasuredWidth
import com.github.damontecres.stashapp.util.getRandomSort
import com.github.damontecres.stashapp.views.FontSpan
import com.github.damontecres.stashapp.views.TabbedGridTitleView
import com.google.android.material.tabs.TabLayout

abstract class TabbedGridFragmentActivity : FragmentActivity(R.layout.tabbed_grid_view2) {
    private lateinit var titleView: TabbedGridTitleView
    private lateinit var adapter: StashFragmentPagerAdapter
    private lateinit var sortButton: Button
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
            val fragment = adapter.getItem(i) as StashGridFragment2
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
                                adapter.getItem(i) as StashGridFragment2

                            Log.v(
                                TAG,
                                "Got tab ${tab.text}, currentSortAndDirection=${fragment.currentSortAndDirection}",
                            )
                            setUpSortButton(
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
        val startFragment = adapter.getItem(0) as StashGridFragment2
        setUpSortButton(startFragment.dataType, startFragment.currentSortAndDirection)
    }

    override fun onStart() {
        super.onStart()
    }

    private fun setUpSortButton(
        dataType: DataType,
        sortAndDirection: SortAndDirection,
    ) {
        val listPopUp =
            ListPopupWindow(
                this,
                null,
                android.R.attr.listPopupWindowStyle,
            )
        val serverVersion = ServerPreferences(this).serverVersion
        // Resolve the strings, then sort
        val sortOptions =
            dataType.sortOptions
                .filter { serverVersion.isAtLeast(it.requiresVersion) }
                .map {
                    Pair(
                        it.key,
                        getString(it.nameStringId),
                    )
                }.sortedBy { it.second }
        val resolvedNames = sortOptions.map { it.second }

        val currentDirection = sortAndDirection.direction
        val currentKey = sortAndDirection.sort
        val isRandom = currentKey?.startsWith("random") ?: false
        val index =
            if (isRandom) {
                sortOptions.map { it.first }.indexOf("random")
            } else {
                sortOptions.map { it.first }.indexOf(currentKey)
            }
        setSortButtonText(
            currentDirection,
            if (index >= 0) sortOptions[index].second else null,
            isRandom,
        )
        Log.v(
            TAG,
            "index=$index, currentKey=$currentKey, currentDirection=$currentDirection",
        )
        val adapter =
            SortByArrayAdapter(
                this,
                resolvedNames,
                index,
                currentDirection,
            )
        listPopUp.setAdapter(adapter)
        listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
        listPopUp.anchorView = sortButton

        listPopUp.width = getMaxMeasuredWidth(this, adapter)
        listPopUp.isModal = true

        listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
            val newSortBy = sortOptions[position].first
            listPopUp.dismiss()

            val currentDirection = sortAndDirection.direction
            val currentKey = sortAndDirection.sort
            val newDirection =
                if (newSortBy == currentKey && currentDirection != null) {
                    if (currentDirection == SortDirectionEnum.ASC) SortDirectionEnum.DESC else SortDirectionEnum.ASC
                } else {
                    currentDirection ?: SortDirectionEnum.DESC
                }
            val resolvedNewSortBy =
                if (newSortBy.startsWith("random")) {
                    getRandomSort()
                } else {
                    newSortBy
                }
            Log.v(
                TAG,
                "New sort for position $currentTabPosition: resolvedNewSortBy=$resolvedNewSortBy, newDirection=$newDirection",
            )

            val newSortAndDirection = SortAndDirection(resolvedNewSortBy, newDirection)
            (this@TabbedGridFragmentActivity.adapter.getItem(currentTabPosition) as StashGridFragment2)
                .refresh(newSortAndDirection)
            setUpSortButton(dataType, newSortAndDirection)
        }

        sortButton.setOnClickListener {
            val currentDirection = sortAndDirection.direction
            val currentKey = sortAndDirection.sort
            val index = sortOptions.map { it.first }.indexOf(currentKey)
            adapter.currentIndex = index
            adapter.currentDirection = currentDirection

            listPopUp.show()
            listPopUp.listView?.requestFocus()
        }
    }

    private fun setSortButtonText(
        currentDirection: SortDirectionEnum?,
        sortBy: CharSequence?,
        isRandom: Boolean,
    ) {
        val directionString =
            when (currentDirection) {
                SortDirectionEnum.ASC -> getString(R.string.fa_caret_up)
                SortDirectionEnum.DESC -> getString(R.string.fa_caret_down)
                SortDirectionEnum.UNKNOWN__ -> null
                null -> null
            }
        if (isRandom) {
            sortButton.text = getString(R.string.stashapp_random)
        } else if (directionString != null && sortBy != null) {
            SpannableString(directionString + " " + sortBy).apply {
                val start = 0
                val end = 1
                setSpan(
                    FontSpan(StashApplication.getFont(R.font.fa_solid_900)),
                    start,
                    end,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE,
                )
                sortButton.text = this
            }
        } else if (sortBy != null) {
            sortButton.text = sortBy
        } else {
            sortButton.text = getString(R.string.sort_by)
        }
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
