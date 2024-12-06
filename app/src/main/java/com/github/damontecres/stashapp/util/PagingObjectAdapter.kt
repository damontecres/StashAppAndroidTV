package com.github.damontecres.stashapp.util

import android.util.Log
import androidx.leanback.widget.ObjectAdapter
import androidx.leanback.widget.PresenterSelector
import com.github.damontecres.stashapp.suppliers.StashPagingSource
import com.google.common.cache.CacheBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

/**
 * An [ObjectAdapter] to paginate requests to the server
 */
class PagingObjectAdapter(
    private val source: StashPagingSource<*, *, *, *>,
    private val pageSize: Int = 25,
    private val scope: CoroutineScope,
    presenterSelector: PresenterSelector,
) : ObjectAdapter(presenterSelector) {
    private var totalCount = -1

    private val cachedPages =
        CacheBuilder.newBuilder()
            .maximumSize(8)
            .build<Int, Page>()

    private val mutex = Mutex()

    suspend fun init() {
        totalCount = source.getCount()
    }

    override fun size(): Int {
        if (totalCount < 0) {
            throw IllegalStateException("Must call init() first!")
        }
        return totalCount
    }

    override fun get(position: Int): Any? {
        val pageNumber = position / pageSize + 1
        val page = cachedPages.getIfPresent(pageNumber)
        if (page != null) {
            return page.items[position % pageSize]
        }
        if (DEBUG) Log.v(TAG, "get: position=$position")
        fetchPage(position, pageNumber)
        return null
    }

    /**
     * Prepare to jump to a position by preloading the necessary page
     */
    suspend fun prepareForJump(position: Int) {
        // Fetch multiple pages?
        val pageNumber = position / pageSize + 1
        fetchPage(position, pageNumber).join()
    }

    /**
     * Notify the adapter of the current UI position which allows for prefetching pages if needed
     */
    fun updatePosition(position: Int) {
        val pageNumber = position / pageSize + 1
        val minPage = cachedPages.asMap().keys.minOrNull()
        val maxPage = cachedPages.asMap().keys.maxOrNull()
        if (DEBUG) Log.v(TAG, "updatePosition: position=$position, minPage=$minPage, maxPage=$maxPage")
        val toFetch =
            if (maxPage != null && pageNumber + 2 > maxPage) {
                (maxPage + 1).coerceAtMost(totalCount / pageSize + 1)
            } else if (minPage != null && pageNumber - 2 < minPage) {
                (minPage - 1).coerceAtLeast(0)
            } else {
                null
            }
        if (toFetch != null) {
            scope.launchIO {
                try {
                    mutex.lock()
                    if (cachedPages.getIfPresent(toFetch) == null) {
                        val items = source.fetchPage(toFetch, pageSize)
                        cachedPages.put(toFetch, Page(toFetch, items))
                    }
                } finally {
                    mutex.unlock()
                }
            }
        }
    }

    private fun fetchPage(
        position: Int,
        pageNumber: Int,
    ): Job {
        return scope.launchIO {
            try {
                mutex.lock()
                if (cachedPages.getIfPresent(pageNumber) == null) {
                    if (DEBUG) Log.v(TAG, "get: fetching $pageNumber")
                    val items = source.fetchPage(pageNumber, pageSize)
                    cachedPages.put(pageNumber, Page(pageNumber, items))
                    withContext(Dispatchers.Main) {
                        notifyItemRangeChanged(position / pageSize * pageSize, pageSize)
                    }
                }
            } finally {
                mutex.unlock()
            }
        }
    }

    companion object {
        private const val TAG = "PagingObjectAdapter"

        private const val DEBUG = false
    }

    private data class Page(val number: Int, val items: List<Any>)
}
