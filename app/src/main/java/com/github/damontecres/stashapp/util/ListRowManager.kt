package com.github.damontecres.stashapp.util

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.SinglePresenterSelector
import androidx.leanback.widget.SparseArrayObjectAdapter
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.StashPresenter
import kotlinx.coroutines.CoroutineScope

/**
 * Manages a [ListRow] handling adding/removing items, or even the adding/remove the row itself
 */
class ListRowManager<T : StashData>(
    dataType: DataType,
    rowModifier: SparseArrayRowModifier,
    val adapter: ArrayObjectAdapter,
    rowHeaderName: String? = null,
    private val setItemsCallback: SetIdsForItemCallback<T>,
) {
    var name: String =
        rowHeaderName ?: StashApplication.getApplication().getString(dataType.pluralStringId)
        set(name) {
            field = name
            if (adapter.isNotEmpty()) {
                addRowCallback.addRow(ListRow(HeaderItem(name), adapter))
            }
        }
    private val addRowCallback: RowAdder
    private val removeRowCallback: RowRemover
    private val diffCallback = StashDiffCallback

    init {
        addRowCallback = rowModifier
        removeRowCallback = rowModifier
    }

    /**
     * Add an item to the row
     * @return true if the item existed and was removed
     */
    suspend fun remove(item: T): Boolean {
        val currentIds = adapter.unmodifiableList<T>().map { it.id }.toMutableList()
        if (currentIds.remove(item.id)) {
            val results = setItemsCallback.setIds(currentIds)
            if (results.isEmpty()) {
                removeRowCallback.removeRow()
                adapter.clear()
            } else {
                adapter.setItems(results, diffCallback)
            }
            return true
        }
        return false
    }

    /**
     * Add an item to the row if it doesn't already exist
     * @return the object for the ID or null if it exists already
     */
    suspend fun add(id: String): T? {
        val currentIds = adapter.unmodifiableList<T>().map { it.id }.toMutableList()
        if (!currentIds.contains(id)) {
            val wasEmpty = adapter.isEmpty()
            currentIds.add(id)
            val results = setItemsCallback.setIds(currentIds)
            adapter.setItems(results, diffCallback)
            if (wasEmpty) {
                addRowCallback.addRow(ListRow(HeaderItem(name), adapter))
            }
            return results.firstOrNull { it.id == id }
        }
        return null
    }

    /**
     * Set/replace the items in the row
     */
    fun setItems(items: List<T>) {
        if (items.isEmpty()) {
            adapter.clear()
            removeRowCallback.removeRow()
        } else {
            val wasEmpty = adapter.isEmpty()
            adapter.setItems(items, diffCallback)
            if (wasEmpty) {
                addRowCallback.addRow(ListRow(HeaderItem(name), adapter))
            }
        }
    }

    /**
     * Remove all items
     */
    fun clear() {
        setItems(listOf())
    }

    /**
     * Set the ids for something on a parent object.
     *
     * Returns the items on the parent object, typically the items represented by the IDs.
     *
     * For example wrapping [MutationEngine.setTagsOnScene] to set a list of tag IDs on a scene and return the [TagData] objects.
     *
     * @return the items from the parent object
     */
    fun interface SetIdsForItemCallback<T> {
        suspend fun setIds(ids: List<String>): List<T>
    }

    fun interface RowAdder {
        fun addRow(row: ListRow)
    }

    fun interface RowRemover {
        fun removeRow()
    }

    /**
     * A [RowAdder]/[RowRemover] that adds or removes at the specified position in the specified adapter
     */
    class SparseArrayRowModifier(
        private val adapter: SparseArrayObjectAdapter,
        private val position: Int,
    ) : RowAdder,
        RowRemover {
        override fun addRow(row: ListRow) {
            adapter.set(position, row)
        }

        override fun removeRow() {
            adapter.clear(position)
        }
    }
}

fun <T : StashData> configRowManager(
    scope: CoroutineScope,
    rowManager: ListRowManager<T>,
    presenter: (StashPresenter.LongClickCallBack<T>) -> StashPresenter<T>,
) {
    rowManager.adapter.presenterSelector =
        SinglePresenterSelector(presenter.invoke(RemoveLongClickListener(scope, rowManager)))
}
