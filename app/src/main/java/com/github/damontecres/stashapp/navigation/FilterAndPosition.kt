package com.github.damontecres.stashapp.navigation

import com.github.damontecres.stashapp.suppliers.FilterArgs

data class FilterAndPosition(
    val filter: FilterArgs,
    val position: Int,
)
