package com.github.damontecres.stashapp.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.leanback.widget.SparseArrayObjectAdapter
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.StashGridFragment2
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection

abstract class StashFragmentPagerAdapter(private val items: MutableList<PagerEntry>, fm: FragmentManager) :
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

    abstract fun getFragment(position: Int): StashGridFragment2

    data class PagerEntry(val title: String, val dataType: DataType, val sortAndDirection: SortAndDirection = dataType.defaultSort) {
        constructor(dataType: DataType) : this(StashApplication.getApplication().getString(dataType.pluralStringId), dataType)

        fun getFindFilter(): FindFilterType {
            return FindFilterType(
                sort = Optional.present(sortAndDirection.sort),
                direction = Optional.present(sortAndDirection.direction),
            )
        }
    }
}
