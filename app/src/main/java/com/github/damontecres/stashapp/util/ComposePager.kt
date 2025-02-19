package com.github.damontecres.stashapp.util

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ComposePager(
    val filter: FilterArgs,
    private val source: StashPagingSource<*, *, StashData, *>,
    private val scope: CoroutineScope,
    private val pageSize: Int = 25,
    cacheSize: Long = 8,
) {
    private var items by mutableStateOf(ItemList<StashData>(0, pageSize, mapOf()))
    private var totalCount by mutableIntStateOf(-1)
    private val mutex = Mutex()
    private val cachedPages =
        CacheBuilder
            .newBuilder()
            .maximumSize(cacheSize)
            .build<Int, List<StashData>>()

    suspend fun init() {
        totalCount = source.getCount()
    }

    operator fun get(position: Int): StashData? {
        if (position in 0..<totalCount) {
            val item = items[position]
            if (item == null) {
                fetchPage(position)
            }
            return item
        } else {
            throw IndexOutOfBoundsException("$position of $totalCount")
        }
    }

    fun size(): Int = totalCount

    private fun fetchPage(position: Int): Job =
        scope.launchIO {
            mutex.withLock {
                val pageNumber = position / pageSize + 1
                if (cachedPages.getIfPresent(pageNumber) == null) {
                    if (DEBUG) Log.v(TAG, "fetchPage: $pageNumber")
                    val data = source.fetchPage(pageNumber, pageSize)
                    cachedPages.put(pageNumber, data)
                    items = ItemList(totalCount, pageSize, cachedPages.asMap())
                }
            }
        }

    companion object {
        private const val TAG = "ComposePager"
        private const val DEBUG = false
    }
}

class ItemList<T : StashData>(
    val size: Int,
    val pageSize: Int,
    val pages: Map<Int, List<T>>,
) {
    operator fun get(position: Int): T? {
        val page = position / pageSize + 1
        val data = pages[page]
        if (data != null) {
            return data[position % pageSize]
        } else {
            return null
        }
    }
}
