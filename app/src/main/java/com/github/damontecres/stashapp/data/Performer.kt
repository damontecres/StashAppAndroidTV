package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.api.type.GenderEnum

data class Performer(
    var id: Long,
    var name: String,
    var disambiguation: String?,
    var gender: GenderEnum?,
)
