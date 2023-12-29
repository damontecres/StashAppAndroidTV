package com.github.damontecres.stashapp.presenters

import android.content.Context
import android.view.ViewGroup
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.paging.PagingDataAdapter
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.github.damontecres.stashapp.api.FindScenesQuery
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.createApolloClient
import com.github.damontecres.stashapp.data.CountAndList

class ScenePagingSource(val context: Context, val pageSize: Int) :
    PagingSource<Int, SlimSceneData>() {

    private suspend fun fetchPage(page: Int): CountAndList<SlimSceneData> {
        val apolloClient = createApolloClient(context)
        if (apolloClient != null) {
            val results = apolloClient.query(
                FindScenesQuery(
                    filter = Optional.present(
                        FindFilterType(
                            per_page = Optional.present(pageSize),
                            page = Optional.present(page),
                            sort = Optional.present("date"),
                            direction = Optional.present(SortDirectionEnum.DESC)
                        )
                    )
                )
            ).execute()
            val scenes = results.data?.findScenes?.scenes?.map { it.slimSceneData }.orEmpty()
            val count = results.data?.findScenes?.count ?: -1
            return CountAndList(count, scenes)
        }
        return CountAndList(-1, listOf())
    }

    override suspend fun load(
        params: LoadParams<Int>
    ): LoadResult<Int, SlimSceneData> {
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

    override fun getRefreshKey(state: PagingState<Int, SlimSceneData>): Int? {
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

//class SceneAdapter(
//    val presenter: ScenePresenter,
//    diffCallback: DiffUtil.ItemCallback<SlimSceneData>
//) : PagingDataAdapter<SlimSceneData>(diffCallback) {
//
//}

object SceneComparator : DiffUtil.ItemCallback<SlimSceneData>() {
    override fun areItemsTheSame(oldItem: SlimSceneData, newItem: SlimSceneData): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SlimSceneData, newItem: SlimSceneData): Boolean {
        return oldItem == newItem
    }
}