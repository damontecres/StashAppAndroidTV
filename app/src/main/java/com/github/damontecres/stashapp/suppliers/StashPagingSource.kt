package com.github.damontecres.stashapp.suppliers

import android.util.Log
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
 */
class StashPagingSource<T : Query.Data, D : StashData, S : Any, C : Query.Data>(
    private val queryEngine: QueryEngine,
    private val dataSupplier: DataSupplier<T, D, C>,
    private val transform: DataTransform<D, S>,
) {
    constructor(
        queryEngine: QueryEngine,
        dataSupplier: DataSupplier<T, D, C>,
    ) : this(
        queryEngine,
        dataSupplier,
        DataTransform { _, _, item -> item as S },
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
         * Get the default find filter
         */
        fun getDefaultFilter(): FindFilterType

        fun createCountQuery(filter: FindFilterType?): Query<C>

        fun parseCountQuery(data: C): Int
    }

    suspend fun fetchPage(
        page: Int,
        loadSize: Int,
    ): List<S> =
        withContext(Dispatchers.IO) {
            val filter =
                dataSupplier.getDefaultFilter().copy(
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

    suspend fun getCount(): Int =
        withContext(Dispatchers.IO) {
            if (count != null) {
                return@withContext count!!
            }
            val query = dataSupplier.createCountQuery(dataSupplier.getDefaultFilter())
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
