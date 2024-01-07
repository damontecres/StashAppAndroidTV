package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.MovieData
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    val id: String,
    val name: String,
    val aliases: String?,
    val studioId: String?,
    val front_image_path: String?,
    val back_image_path: String?,
) : Parcelable

fun fromMovieData(movie: MovieData): Movie {
    return Movie(
        movie.id,
        movie.name,
        movie.aliases,
        movie.studio?.id,
        movie.front_image_path,
        movie.back_image_path,
    )
}
