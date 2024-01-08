package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.FindMoviesQuery
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.data.CountAndList
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPagingSource

class MovieDataSupplier(
    private val findFilter: FindFilterType?,
    private val movieFilter: MovieFilterType?,
) :
    StashPagingSource.DataSupplier<FindMoviesQuery.Data, MovieData> {
    override val dataType: DataType get() = DataType.PERFORMER

    override fun createQuery(filter: FindFilterType?): Query<FindMoviesQuery.Data> {
        return FindMoviesQuery(
            filter = filter,
            movie_filter = movieFilter,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: FindFilterType()
    }

    override fun parseQuery(data: FindMoviesQuery.Data?): CountAndList<MovieData> {
        val count = data?.findMovies?.count ?: -1
        val performers =
            data?.findMovies?.movies?.map {
                it.movieData
            }.orEmpty()
        return CountAndList(count, performers)
    }
}
