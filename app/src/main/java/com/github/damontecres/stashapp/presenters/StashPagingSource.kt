package com.github.damontecres.stashapp.presenters

import android.content.Context
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.recyclerview.widget.DiffUtil
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.createApolloClient
import com.github.damontecres.stashapp.data.CountAndList

class StashPagingSource<T : Query.Data, D : Any>(
    private val context: Context,
    private val pageSize: Int,
    private val sortKey: String?,
    private val dataSupplier: DataSupplier<T, D>
) :
    PagingSource<Int, D>() {

    interface DataSupplier<T : Query.Data, D : Any> {
        fun createQuery(filter: FindFilterType): Query<T>

        fun parseQuery(data: T?): CountAndList<D>
    }

    private suspend fun fetchPage(page: Int): CountAndList<D> {
        val apolloClient = createApolloClient(context)
        if (apolloClient != null) {
            val query = dataSupplier.createQuery(
                FindFilterType(
                    per_page = Optional.present(pageSize),
                    page = Optional.present(page),
                    sort = Optional.present(sortKey),
                    direction = Optional.present(SortDirectionEnum.DESC)
                )
            )
            val results = apolloClient.query(query).execute()
            return dataSupplier.parseQuery(results.data)
        }
        return CountAndList(-1, listOf())
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, D> {
        try {
            // Start refresh at page 1 if undefined.
            val pageNum = (params.key ?: 1).toInt()
            val results = fetchPage(pageNum)
            if (results.count < 0) {
                return LoadResult.Error(RuntimeException("Invalid count"))
            }
            val nextPageNum = if (pageSize * pageNum < results.count) pageNum + 1 else null

            return LoadResult.Page(
                data = results.list,
                prevKey = if (pageNum > 1) pageNum - 1 else null,
                nextKey = nextPageNum
            )
        } catch (e: ApolloException) {
            return LoadResult.Error(e)
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
}
