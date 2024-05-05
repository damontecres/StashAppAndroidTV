package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.CountMoviesQuery
import com.github.damontecres.stashapp.api.FindMoviesQuery
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.MovieFilterType
import com.github.damontecres.stashapp.data.DataType

class MovieDataSupplier(
    private val findFilter: FindFilterType?,
    private val movieFilter: MovieFilterType?,
) :
    StashPagingSource.DataSupplier<FindMoviesQuery.Data, MovieData, CountMoviesQuery.Data> {
    constructor(movieFilter: MovieFilterType? = null) : this(
        DataType.MOVIE.asDefaultFindFilterType,
        movieFilter,
    )

    override val dataType: DataType get() = DataType.PERFORMER

    override fun createQuery(filter: FindFilterType?): Query<FindMoviesQuery.Data> {
        return FindMoviesQuery(
            filter = filter,
            movie_filter = movieFilter,
        )
    }

    override fun getDefaultFilter(): FindFilterType {
        return findFilter ?: DataType.MOVIE.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<CountMoviesQuery.Data> {
        return CountMoviesQuery(filter, movieFilter)
    }

    override fun parseCountQuery(data: CountMoviesQuery.Data): Int {
        return data.findMovies.count
    }

    override fun parseQuery(data: FindMoviesQuery.Data): List<MovieData> {
        return data.findMovies.movies.map { it.movieData }
    }
}
