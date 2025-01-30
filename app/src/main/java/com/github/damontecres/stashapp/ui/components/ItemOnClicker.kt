package com.github.damontecres.stashapp.ui.components

import com.github.damontecres.stashapp.navigation.FilterAndPosition

fun interface ItemOnClicker<T> {
    fun onClick(
        item: T,
        filterAndPosition: FilterAndPosition,
    )
}
