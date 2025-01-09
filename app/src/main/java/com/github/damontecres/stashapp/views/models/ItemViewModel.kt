package com.github.damontecres.stashapp.views.models

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import kotlinx.coroutines.launch

/**
 * Base [ViewModel] for simple [StashData] items
 */
abstract class ItemViewModel<T : StashData> : ViewModel() {
    private val _item = EqualityMutableLiveData<T>()
    val item: LiveData<T?> = _item

    /**
     * Fetch the item for the given id
     */
    abstract suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): T?

    /**
     * Initialize the [ViewModel] by fetching the item in the background and updating it
     */
    fun init(args: Bundle) {
        val id = args.getDestination<Destination.Item>().id
        viewModelScope.launch(StashCoroutineExceptionHandler(true)) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            _item.value = fetch(queryEngine, id)
        }
    }

    fun update(item: T) {
        _item.value = item
    }
}
