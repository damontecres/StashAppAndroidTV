package com.github.damontecres.stashapp.ui.components

import androidx.core.util.Consumer
import androidx.core.util.Predicate
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpItem

class LongClicker<T>(
    vararg actions: Pair<PopUpItem, Consumer<T>>,
) {
    private val actions = mutableMapOf<PopUpItem, Consumer<T>>()
    private val filters = mutableMapOf<PopUpItem, Predicate<T>>()

    init {
        this.actions.putAll(actions)
        actions.forEach { filters[it.first] = Predicate { true } }
    }

    fun addAction(
        popUpItem: PopUpItem,
        filter: Predicate<T> = Predicate { true },
        action: Consumer<T>,
    ): LongClicker<T> {
        actions[popUpItem] = action
        filters[popUpItem] = filter
        return this
    }

    fun getPopUpItems(item: T): List<PopUpItem> = actions.keys.filter { filters[it]!!.test(item) }.sortedBy { it.id }

    fun onItemLongClick(
        item: T,
        popUpItem: PopUpItem,
    ) {
        actions[popUpItem]!!.accept(item)
    }

    companion object {
        fun default() =
            LongClicker<Any>(
                PopUpItem.DEFAULT to
                    Consumer<Any> {
                        StashApplication.navigationManager.navigate(Destination.fromStashData(it as StashData))
                    },
            )
    }
}
