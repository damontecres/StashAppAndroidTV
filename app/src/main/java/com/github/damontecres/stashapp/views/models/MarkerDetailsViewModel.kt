package com.github.damontecres.stashapp.views.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.github.damontecres.stashapp.data.Marker

class MarkerDetailsViewModel(private val state: SavedStateHandle) : ViewModel() {
    val seconds = MutableLiveData<Double>()

    val marker: LiveData<Marker> = state.getLiveData("marker")

    fun setMarker(newMarker: Marker) {
        state["marker"] = newMarker
    }
}
