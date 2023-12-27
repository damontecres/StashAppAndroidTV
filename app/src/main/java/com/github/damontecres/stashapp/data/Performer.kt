package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.GenderEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class Performer(
    var id: Long,
    var name: String,
    var disambiguation: String?,
    var gender: GenderEnum?,
) : Parcelable

fun performerFromPerformerData(p: PerformerData): Performer{
    return Performer(
        id=p.id.toLong(),
        name=p.name,
        disambiguation = p.disambiguation,
        gender = p.gender
    )
}