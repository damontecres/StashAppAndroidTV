package com.github.damontecres.stashapp.suppliers

import android.util.Log
import com.apollographql.apollo3.api.Query

/**
 * Wraps a StashPagingSource to allow for accessing any item position in the backing filter
 *
 * This class keeps one page of data available in memory and then only fetches a different page if required
 */
class StashSparseFilterFetcher<T : Query.Data, D : Any>(
    val source: StashPagingSource<T, D, *>,
    val pageSize: Int = 25,
) {
    private var firstPage = true
    private var currentPageData: List<D>? = null
    private var currentPage = 0
    private var currentPageStart = 0

    private var listeners = mutableListOf<Listener<D>>()

    private suspend fun loadDataFor(position: Int = 0) {
        // Pages are 1-indexed
        val pageToLoad = position / pageSize + 1
        Log.v(TAG, "Loading dating for pos=$position, page=$pageToLoad")
        val pageData = source.fetchPage(pageToLoad, pageSize)

        listeners.forEach { it.onPageLoad(firstPage, pageToLoad, pageData) }
        currentPage = pageToLoad
        currentPageData = pageData
        currentPageStart = (currentPage - 1) * pageSize
        Log.v(TAG, "New currentPage=$currentPage, currentPageStart=$currentPageStart")

        firstPage = false
    }

    suspend fun get(position: Int): D? {
        Log.v(TAG, "Requested $position, currentPage=$currentPage, currentPageStart=$currentPageStart")
        if (position < 0) {
            return null
        }
        if (currentPageData == null) {
            loadDataFor(position)
        }
        val listPos = position - currentPageStart
        return if (listPos in 0..<pageSize) {
            if (currentPageData != null && listPos < currentPageData!!.size) {
                currentPageData!![listPos]
            } else {
                null
            }
        } else {
            loadDataFor(position)
            get(position)
        }
    }

    fun addListener(listener: Listener<D>) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener<D>) {
        listeners.remove(listener)
    }

    companion object {
        const val TAG = "StashFilterPager"
    }

    fun interface Listener<D : Any> {
        fun onPageLoad(
            firstPageLoaded: Boolean,
            pageNum: Int,
            page: List<D>,
        )
    }
}
