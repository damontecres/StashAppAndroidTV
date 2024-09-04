package com.github.damontecres.stashapp.suppliers

import com.apollographql.apollo.api.Query
import com.github.damontecres.stashapp.api.CountMoviesQuery
import com.github.damontecres.stashapp.api.FindMovieTagsQuery
import com.github.damontecres.stashapp.api.FindMoviesQuery
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.TagData
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

/**
 * A DataSupplier that returns the tags for a movie
 */
class MovieTagDataSupplier(private val movieId: String) :
    StashPagingSource.DataSupplier<FindMovieTagsQuery.Data, TagData, FindMovieTagsQuery.Data> {
    override val dataType: DataType
        get() = DataType.TAG

    override fun createQuery(filter: FindFilterType?): Query<FindMovieTagsQuery.Data> {
        return FindMovieTagsQuery(movieId)
    }

    override fun getDefaultFilter(): FindFilterType {
        return DataType.TAG.asDefaultFindFilterType
    }

    override fun createCountQuery(filter: FindFilterType?): Query<FindMovieTagsQuery.Data> {
        return createQuery(filter)
    }

    override fun parseCountQuery(data: FindMovieTagsQuery.Data): Int {
        return if (data.findMovie != null) {
            1
        } else {
            0
        }
    }

    override fun parseQuery(data: FindMovieTagsQuery.Data): List<TagData> {
        return data.findMovie?.tags?.map { it.tagData }.orEmpty()
    }
}
