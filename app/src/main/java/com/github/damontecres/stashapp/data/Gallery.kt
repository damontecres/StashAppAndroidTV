package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.util.name
import kotlinx.parcelize.Parcelize

@Parcelize
data class Gallery(
    val id: String,
    val name: String?,
    val date: String?,
) : Parcelable

fun GalleryData.toGallery(): Gallery = Gallery(id, name, date)
