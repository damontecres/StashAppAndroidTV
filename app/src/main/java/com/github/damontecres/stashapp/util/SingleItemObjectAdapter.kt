package com.github.damontecres.stashapp.util

import androidx.leanback.widget.ObjectAdapter
import com.github.damontecres.stashapp.presenters.StashPresenter

class SingleItemObjectAdapter<T : Any>(presenter: StashPresenter<T>) : ObjectAdapter(presenter) {
    var item: T? = null
        set(newValue) {
            field = newValue
            notifyChanged()
        }

    override fun size(): Int {
        return 1
    }

    override fun get(position: Int): Any? {
        if (position != 0) {
            throw IllegalArgumentException()
        }
        return item
    }
}
