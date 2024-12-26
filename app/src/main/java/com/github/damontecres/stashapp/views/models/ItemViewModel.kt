package com.github.damontecres.stashapp.views.models

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.getDestination
import kotlinx.coroutines.launch

abstract class ItemViewModel<T : StashData> : ViewModel() {
    private val _item = EqualityMutableLiveData<T>()
    val item: LiveData<T?> = _item

    abstract suspend fun fetch(
        queryEngine: QueryEngine,
        id: String,
    ): T?

    fun init(args: Bundle) {
        val id = args.getDestination<Destination.Item>().id
        viewModelScope.launch(StashCoroutineExceptionHandler()) {
            val queryEngine = QueryEngine(StashServer.requireCurrentServer())
            _item.value = fetch(queryEngine, id)
        }
    }
}
