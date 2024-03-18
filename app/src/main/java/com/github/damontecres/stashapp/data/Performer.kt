package com.github.damontecres.stashapp.data

import android.os.Parcelable
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.type.GenderEnum
import kotlinx.parcelize.Parcelize

@Parcelize
data class Performer(
    val id: String,
    val name: String,
    val disambiguation: String?,
    val gender: GenderEnum?,
) : Parcelable {
    constructor(p: PerformerData) : this(
        id = p.id,
        name = p.name,
        disambiguation = p.disambiguation,
        gender = p.gender,
    )
}
