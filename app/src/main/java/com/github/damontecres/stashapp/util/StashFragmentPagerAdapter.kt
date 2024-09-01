package com.github.damontecres.stashapp.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.widget.SparseArrayObjectAdapter
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.data.DataType

abstract class StashFragmentPagerAdapter(private val items: List<PagerEntry>, fm: FragmentManager) :
    FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fragments = SparseArrayObjectAdapter()

    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Fragment {
        val fragment = fragments.lookup(position) as Fragment?
        if (fragment != null) {
            return fragment
        } else {
            val newFragment = getFragment(position)
            fragments.set(position, newFragment)
            return newFragment
        }
    }

    override fun getPageTitle(position: Int): CharSequence {
        return items[position].title
    }

    fun getItems(): List<Fragment> {
        return (0..<count).map(::getItem)
    }

    abstract fun getFragment(position: Int): Fragment

    data class PagerEntry(val title: String, val dataType: DataType?) {
        constructor(dataType: DataType) : this(StashApplication.getApplication().getString(dataType.pluralStringId), dataType)
    }
}
