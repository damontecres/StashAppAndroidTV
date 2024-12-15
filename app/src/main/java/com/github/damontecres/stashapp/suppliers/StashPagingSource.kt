package com.github.damontecres.stashapp.suppliers

import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashData
import com.github.damontecres.stashapp.suppliers.StashPagingSource.DataTransform
import com.github.damontecres.stashapp.util.QueryEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A PagingSource for Stash
 *
 * @property context
 * @property pageSize how many items per page
 * @property dataSupplier how to query and parse data
 */
class StashPagingSource<T : Query.Data, D : StashData, S : Any, C : Query.Data>(
    private val queryEngine: QueryEngine,
    private val pageSize: Int,
    private val dataSupplier: DataSupplier<T, D, C>,
    showToasts: Boolean = false,
    private val useRandom: Boolean = true,
    private val sortByOverride: String? = null,
    private val transform: DataTransform<D, S>,
) : PagingSource<Int, S>() {
    constructor(
        queryEngine: QueryEngine,
        pageSize: Int,
        dataSupplier: DataSupplier<T, D, C>,
        showToasts: Boolean = false,
        useRandom: Boolean = true,
        sortByOverride: String? = null,
    ) : this(
        queryEngine,
        pageSize,
        dataSupplier,
        showToasts,
        useRandom,
        sortByOverride,
        DataTransform { page, index, item -> item as S },
    )

    private var listeners = mutableListOf<Listener<S>>()

    private var count: Int? = null

    interface DataSupplier<T : Query.Data, D : StashData, C : Query.Data> {
        val dataType: DataType

        /**
         * Create query with the given filter
         *
         * @param filter the filter to use
         */
        fun createQuery(filter: FindFilterType?): Query<T>

        /**
         * Parse the data returned from the query created by [createQuery]
         *
         * @param data the Query's data object
         * @return The list of data along with the total count
         */
        fun parseQuery(data: T): List<D>

        /**
         * Get the default filter
         *
         * By default, this sorts by name ascending
         */
        fun getDefaultFilter(): FindFilterType

        fun createCountQuery(filter: FindFilterType?): Query<C>

        fun parseCountQuery(data: C): Int
    }

    private fun createFindFilter(): FindFilterType {
        var filter =
            dataSupplier.getDefaultFilter()
        if (!sortByOverride.isNullOrBlank()) {
            filter = filter.copy(sort = Optional.present(sortByOverride))
        }
        return queryEngine.updateFilter(filter, useRandom)!!
    }

    suspend fun fetchPage(
        page: Int,
        loadSize: Int,
    ): List<S> =
        withContext(Dispatchers.IO) {
            val filter =
                createFindFilter().copy(
                    per_page = Optional.present(loadSize),
                    page = Optional.present(page),
                )
            if (DEBUG) Log.v(TAG, "page=$page, loadSize=$loadSize")
            val query = dataSupplier.createQuery(filter)
            val queryResult = queryEngine.executeQuery(query)
            if (queryResult.data != null) {
                val data =
                    dataSupplier
                        .parseQuery(queryResult.data!!)
                        .mapIndexed { index, item ->
                            transform.transform(page, index, item)
                        }
                withContext(Dispatchers.Main) {
                    listeners.forEach { it.onPageFetch(page, data) }
                }
                return@withContext data
            } else {
                return@withContext listOf()
            }
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, S> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                // Start refresh at page 1 if undefined.
                val pageNum = (params.key ?: 1).toInt()
                val loadSize = params.loadSize
                val results = fetchPage(pageNum, loadSize)

                val itemsBefore =
                    if (pageNum > 0) ((pageNum - 1) * pageSize).coerceAtMost(getCount()) else 0
                val itemsAfter =
                    (getCount() - ((pageNum - 1) * pageSize + results.size)).coerceAtLeast(0)
                val nextPageNum = if (itemsAfter > 0) pageNum + (loadSize / pageSize) else null
                if (DEBUG) {
                    Log.v(
                        TAG,
                        "load: pageNum=$pageNum, loadSize=$loadSize, results.size=${results.size}, " +
                            "nextPageNum=$nextPageNum, itemsBefore=$itemsBefore, itemsAfter=$itemsAfter",
                    )
                }

                LoadResult.Page(
                    data = results,
                    // Only a previous page if current page is 2+
                    prevKey = if (pageNum > 1) pageNum - 1 else null,
                    nextKey = nextPageNum,
                    itemsBefore = itemsBefore,
                    itemsAfter = itemsAfter,
                )
            } catch (e: QueryEngine.QueryException) {
                LoadResult.Error(e)
            }
        }

    override val jumpingSupported: Boolean
        get() = true

    override fun getRefreshKey(state: PagingState<Int, S>): Int? {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
        if (DEBUG) {
            Log.v(
                TAG,
                "getRefreshKey: state.anchorPosition=${state.anchorPosition}, pageSize=$pageSize",
            )
        }
        return state.anchorPosition?.let { anchorPosition ->
//            val anchorPage = state.closestPageToPosition(anchorPosition)
//            Log.v(
//                TAG,
//                "getRefreshKey: state.anchorPosition=${state.anchorPosition}, anchorPage.prevKey=${anchorPage?.prevKey}, anchorPage.nextKey=${anchorPage?.nextKey}, anchorPage.itemsBefore=${anchorPage?.itemsBefore}, anchorPage.itemsAfter=${anchorPage?.itemsAfter}",
//            )
//            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
            anchorPosition / pageSize + 1
        }
    }

    suspend fun getCount(): Int =
        withContext(Dispatchers.IO) {
            if (count != null) {
                return@withContext count!!
            }
            val query = dataSupplier.createCountQuery(createFindFilter())
            val queryResult = queryEngine.executeQuery(query).data
            count =
                if (queryResult != null) {
                    dataSupplier.parseCountQuery(queryResult)
                } else {
                    INVALID_COUNT
                }
            return@withContext count!!
        }

    fun addListener(listener: Listener<S>) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener<S>) {
        listeners.remove(listener)
    }

    fun interface Listener<D : Any> {
        /**
         * Called on the Main thread whenever a page of data is fetched, but before it is loaded
         */
        fun onPageFetch(
            pageNum: Int,
            page: List<D>,
        )
    }

    companion object {
        private const val TAG = "StashPagingSource"
        const val INVALID_COUNT = -1
        const val UNSUPPORTED_COUNT = -2

        private const val DEBUG = false
    }

    fun interface DataTransform<D, S> {
        fun transform(
            page: Int,
            index: Int,
            item: D,
        ): S
    }
}
