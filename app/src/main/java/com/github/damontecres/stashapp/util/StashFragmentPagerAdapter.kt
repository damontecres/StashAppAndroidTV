package com.github.damontecres.stashapp.util

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.DataType

/**
 * A [FragmentStatePagerAdapter] to show various tabs for data types
 */
abstract class StashFragmentPagerAdapter(
    private val items: List<PagerEntry>,
    fm: FragmentManager,
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    var fragmentCreatedListener: ((Fragment, Int) -> Unit)? = null

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Fragment {
        val newFragment = getFragment(position)
        Log.v(TAG, "newFragment for $position")
        fragmentCreatedListener?.invoke(newFragment, position)
        return newFragment
    }

    override fun getPageTitle(position: Int): CharSequence {
        return items[position].title
    }

    abstract fun getFragment(position: Int): Fragment

    /**
     * Represents a tab with an title
     */
    data class PagerEntry(val title: String) {
        constructor(dataType: DataType) : this(
            StashApplication.getApplication().getString(dataType.pluralStringId),
        )
    }

    companion object {
        private const val TAG = "StashFragmentPagerAdapter"
    }
}
