package com.github.damontecres.stashapp.views

import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import androidx.leanback.widget.RowPresenter

/**
 * An OnItemViewClickedListener which delegates clicks based on the class of the item, falling back to a default implementation if available
 */
class ClassOnItemViewClickedListener(
    private val defaultListener: OnItemViewClickedListener? = null,
) : OnItemViewClickedListener {
    private val classMap = mutableMapOf<Class<*>, OnItemViewClickedListener>()

    fun addListenerForClass(
        klass: Class<*>,
        listener: OnItemViewClickedListener,
    ): ClassOnItemViewClickedListener {
        classMap[klass] = listener
        return this
    }

    fun <T> addListenerForClass(
        klass: Class<T>,
        listener: SimpleOnItemViewClickedListener<T>,
    ): ClassOnItemViewClickedListener {
        classMap[klass] = listener
        return this
    }

    override fun onItemClicked(
        itemViewHolder: Presenter.ViewHolder?,
        item: Any,
        rowViewHolder: RowPresenter.ViewHolder?,
        row: Row?,
    ) {
        val listener = classMap[item.javaClass] ?: defaultListener
        if (listener != null) {
            listener.onItemClicked(itemViewHolder, item, rowViewHolder, row)
        } else {
            throw IllegalStateException("Item is a '${item.javaClass}', but there is no listener for this class nor a default listener")
        }
    }

    /**
     * A simplified, typed OnItemViewClickedListener
     */
    fun interface SimpleOnItemViewClickedListener<T> : OnItemViewClickedListener {
        override fun onItemClicked(
            itemViewHolder: Presenter.ViewHolder?,
            item: Any,
            rowViewHolder: RowPresenter.ViewHolder?,
            row: Row?,
        ) {
            onItemClicked(item as T)
        }

        fun onItemClicked(item: T)
    }
}
