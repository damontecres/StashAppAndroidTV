package com.github.damontecres.stashapp.suppliers

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType
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
class StashPagingSource<T : Query.Data, D : Any>(
    private val context: Context,
    private val pageSize: Int,
    private val dataSupplier: DataSupplier<T, D>,
    showToasts: Boolean = false,
    private val useRandom: Boolean = true,
    private val sortByOverride: String? = null,
) :
    PagingSource<Int, D>() {
    private val queryEngine = QueryEngine(context, showToasts)

    private var listeners = mutableListOf<Listener<D>>()

    interface DataSupplier<T : Query.Data, D : Any> {
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
        fun parseQuery(data: T?): CountAndList<D>

        /**
         * Get the default filter
         *
         * By default, this sorts by name ascending
         */
        fun getDefaultFilter(): FindFilterType
    }

    interface DataCountSupplier<T : Query.Data, D : Any, C : Query.Data> : DataSupplier<T, D> {
        fun createCountQuery(filter: FindFilterType?): Query<C>

        fun parseCountQuery(data: C?): Int
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
    ): CountAndList<D> =
        withContext(Dispatchers.IO) {
            val filter =
                createFindFilter().copy(
                    per_page = Optional.present(loadSize),
                    page = Optional.present(page),
                )
            val query = dataSupplier.createQuery(filter)
            val queryResult = queryEngine.executeQuery(query)
            val data = dataSupplier.parseQuery(queryResult.data)
            withContext(Dispatchers.Main) {
                listeners.forEach { it.onPageFetch(page, data) }
            }
            return@withContext data
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, D> =
        withContext(Dispatchers.IO) {
            try {
                // Start refresh at page 1 if undefined.
                val pageNum = (params.key ?: 1).toInt()
                // Round requested loadSize down to a multiple of pageSize
                val loadSize = params.loadSize / pageSize * pageSize
                val results = fetchPage(pageNum, loadSize)
                if (results.count == INVALID_COUNT) {
                    return@withContext LoadResult.Error(RuntimeException("Invalid count"))
                }
                // If the total fetched results is less than the total number of items, then there is a next page
                // Advance the page by the number of requested items
                val nextPageNum =
                    if (results.count != UNSUPPORTED_COUNT &&
                        pageSize * pageNum < results.count
                    ) {
                        pageNum + (params.loadSize / pageSize)
                    } else if (results.count == UNSUPPORTED_COUNT &&
                        results.list.isNotEmpty()
                    ) {
                        pageNum + (params.loadSize / pageSize)
                    } else {
                        null
                    }

                return@withContext LoadResult.Page(
                    data = results.list,
                    // Only a previous page if current page is 2+
                    prevKey = if (pageNum > 1) pageNum - 1 else null,
                    nextKey = nextPageNum,
                )
            } catch (e: QueryEngine.QueryException) {
                return@withContext LoadResult.Error(e)
            }
        }

    override fun getRefreshKey(state: PagingState<Int, D>): Int? {
        // Try to find the page key of the closest page to anchorPosition from
        // either the prevKey or the nextKey; you need to handle nullability
        // here.
        //  * prevKey == null -> anchorPage is the first page.
        //  * nextKey == null -> anchorPage is the last page.
        //  * both prevKey and nextKey are null -> anchorPage is the
        //    initial page, so return null.
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    suspend fun <Z : Query.Data> getCount(): Int =
        withContext(Dispatchers.IO) {
            if (dataSupplier is DataCountSupplier<*, *, *>) {
                dataSupplier as DataCountSupplier<*, *, Z>
                val query = dataSupplier.createCountQuery(createFindFilter())
                val queryResult = queryEngine.executeQuery(query).data
                return@withContext dataSupplier.parseCountQuery(queryResult)
            } else {
                return@withContext INVALID_COUNT
            }
        }

    fun addListener(listener: Listener<D>) {
        listeners.add(listener)
    }

    fun removeListener(listener: Listener<D>) {
        listeners.remove(listener)
    }

    fun interface Listener<D : Any> {
        /**
         * Called on the Main thread whenever a page of data is fetched, but before it is loaded
         */
        fun onPageFetch(
            pageNum: Int,
            page: CountAndList<D>,
        )
    }

    companion object {
        const val INVALID_COUNT = -1
        const val UNSUPPORTED_COUNT = -2
    }
}
