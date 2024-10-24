package com.github.damontecres.stashapp.playback

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.FilterArgs

class PlaylistViewModel : ViewModel() {
    private val _filterArgs = MutableLiveData<FilterArgs>()
    val filterArgs: LiveData<FilterArgs> = _filterArgs

    fun setFilter(filter: FilterArgs) {
        if (filter.dataType == DataType.SCENE) {
            val objectFilter = filter.objectFilter as SceneFilterType? ?: SceneFilterType()
            val newObjectFilter =
                if (objectFilter.file_count.getOrNull() == null) {
                    // Playlist cannot contain scenes with no files, so modify the filter if necessary
                    objectFilter.copy(
                        file_count =
                            Optional.present(
                                IntCriterionInput(
                                    modifier = CriterionModifier.GREATER_THAN,
                                    value = 0,
                                ),
                            ),
                    )
                } else {
                    // TODO it is possible the filter includes scenes without a file which will crash
                    objectFilter
                }
            _filterArgs.value = filter.copy(objectFilter = newObjectFilter)
        } else {
            _filterArgs.value = filter
        }
    }
}
