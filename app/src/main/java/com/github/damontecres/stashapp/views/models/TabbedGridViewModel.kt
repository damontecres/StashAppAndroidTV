package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.util.StashFragmentPagerAdapter.PagerEntry

class TabbedGridViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val title: MutableLiveData<CharSequence?> = savedStateHandle.getLiveData("title", null)

    val tabs: MutableLiveData<List<PagerEntry>> = MutableLiveData()
}
