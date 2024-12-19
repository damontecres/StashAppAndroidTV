package com.github.damontecres.stashapp.util

import androidx.leanback.widget.ObjectAdapter
import com.github.damontecres.stashapp.presenters.StashPresenter

class SingleItemObjectAdapter<T : Any>(
    presenter: StashPresenter<T>,
) : ObjectAdapter(presenter) {
    constructor(presenter: StashPresenter<T>, item: T) : this(presenter) {
        this.item = item
    }

    var item: T? = null
        set(newValue) {
            field = newValue
            notifyChanged()
        }

    override fun size(): Int = if (item == null) 0 else 1

    override fun get(position: Int): Any? {
        if (position != 0) {
            throw IllegalArgumentException()
        }
        return item
    }
}
