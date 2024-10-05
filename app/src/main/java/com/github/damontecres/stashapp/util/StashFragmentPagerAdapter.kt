package com.github.damontecres.stashapp.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.DataType

/**
 * A [FragmentStatePagerAdapter] to show various tabs for data types
 */
class StashFragmentPagerAdapter(
    private val items: List<PagerEntry>,
    fm: FragmentManager,
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Fragment {
        return items[position].createFragment.invoke()
    }

    override fun getPageTitle(position: Int): CharSequence {
        return items[position].title
    }

    /**
     * Represents a tab with an title
     */
    data class PagerEntry(val title: String, val createFragment: () -> Fragment) {
        constructor(dataType: DataType, createFragment: () -> Fragment) : this(
            StashApplication.getApplication().getString(dataType.pluralStringId),
            createFragment,
        )
    }

    companion object {
        private const val TAG = "StashFragmentPagerAdapter"
    }
}
