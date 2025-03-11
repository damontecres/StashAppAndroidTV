package com.github.damontecres.stashapp.playback

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.apollographql.apollo.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.DataSupplierOverride
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.util.AlphabetSearchUtils

class PlaylistViewModel : ViewModel() {
    private val _filterArgs = MutableLiveData<FilterArgs>()
    val filterArgs: LiveData<FilterArgs> = _filterArgs

    fun setFilter(filter: FilterArgs) {
        if (filter.dataType == DataType.SCENE) {
            val objectFilter =
                AlphabetSearchUtils.findNullAndFilter(
                    filter.objectFilter as SceneFilterType? ?: SceneFilterType(),
                )
            // Playlist cannot contain scenes with no files, so modify the filter
            val newObjectFilter =
                objectFilter.copy(
                    AND =
                        Optional.present(
                            SceneFilterType(
                                file_count =
                                    Optional.present(
                                        IntCriterionInput(
                                            modifier = CriterionModifier.GREATER_THAN,
                                            value = 0,
                                        ),
                                    ),
                            ),
                        ),
                )
            _filterArgs.value =
                filter.copy(
                    objectFilter = newObjectFilter,
                    override = DataSupplierOverride.Playlist,
                )
        } else {
            _filterArgs.value = filter.copy(override = DataSupplierOverride.Playlist)
        }
    }
}
