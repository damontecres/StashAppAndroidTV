package com.github.damontecres.stashapp.util

import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

abstract class ListFragmentPagerAdapter(
    val items: List<String>,
    fm: FragmentManager,
) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    final override fun getCount(): Int = items.size

    final override fun getPageTitle(position: Int): CharSequence = items[position]
}
