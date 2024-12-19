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
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * An [ObjectAdapter] to paginate requests to the server
 */
class PagingObjectAdapter(
    private val source: StashPagingSource<*, *, *, *>,
    private val pageSize: Int = 25,
    private val scope: CoroutineScope,
    presenterSelector: PresenterSelector,
    cacheSize: Long = 8,
) : ObjectAdapter(presenterSelector) {
    private var totalCount = -1

    private val cachedPages =
        CacheBuilder
            .newBuilder()
            .maximumSize(cacheSize)
            .build<Int, Page>()

    private val mutex = Mutex()

    suspend fun init(firstPage: Int = 1) {
        if (totalCount < 0) {
            totalCount = source.getCount()
            fetchPage(0, firstPage, false).join()
            if (DEBUG) Log.v(TAG, "init notify $totalCount")
            notifyItemRangeInserted(0, totalCount)
        }
    }

    override fun size(): Int = totalCount

    override fun get(position: Int): Any? {
        val pageNumber = position / pageSize + 1
        val page = cachedPages.getIfPresent(pageNumber)
        if (DEBUG) Log.v(TAG, "get: position=$position")
        if (page != null) {
            return if (page.items.size > position % pageSize) {
                page.items[position % pageSize]
            } else {
                null
            }
        }
        if (DEBUG) Log.v(TAG, "get: need to fetch page $pageNumber for position=$position")
        fetchPage(position, pageNumber, true)
        return null
    }

    /**
     * Prefetch data for the specified position
     */
    fun prefetch(position: Int): Job {
        // Fetch multiple pages?
        val pageNumber = position / pageSize + 1
        return fetchPage(position, pageNumber, false)
    }

    /**
     * Notify the adapter of the current UI position which allows for prefetching pages if needed
     */
    fun maybePrefetch(position: Int) {
        val pageNumber = position / pageSize + 1
        val minPage = cachedPages.asMap().keys.minOrNull()
        val maxPage = cachedPages.asMap().keys.maxOrNull()
        if (DEBUG) {
            Log.v(
                TAG,
                "maybePrefetch: position=$position, minPage=$minPage, maxPage=$maxPage",
            )
        }
        val toFetch =
            if (maxPage != null && pageNumber + 2 > maxPage) {
                (maxPage + 1).coerceAtMost(totalCount / pageSize + 1)
            } else if (minPage != null && pageNumber - 2 < minPage) {
                (minPage - 1).coerceAtLeast(0)
            } else {
                null
            }
        if (toFetch != null) {
            fetchPage(position, toFetch, false)
        }
    }

    /**
     * Fetch a page of data and optionally call [notifyItemRangeChanged]
     */
    private fun fetchPage(
        position: Int,
        pageNumber: Int,
        notifyChange: Boolean,
    ): Job =
        scope.launchIO {
            mutex.withLock {
                if (cachedPages.getIfPresent(pageNumber) == null) {
                    if (DEBUG) Log.v(TAG, "fetchPage: $pageNumber")
                    val items = source.fetchPage(pageNumber, pageSize)
                    cachedPages.put(pageNumber, Page(pageNumber, items))
                    if (notifyChange) {
                        withContext(Dispatchers.Main) {
                            if (DEBUG) {
                                Log.v(
                                    TAG,
                                    "fetchPage: notify start=${position / pageSize * pageSize}, items.size=${items.size}",
                                )
                            }
                            notifyItemRangeChanged(position / pageSize * pageSize, items.size)
                        }
                    }
                }
            }
        }

    companion object {
        private const val TAG = "PagingObjectAdapter"

        private const val DEBUG = false
    }

    private data class Page(
        val number: Int,
        val items: List<Any>,
    )
}
