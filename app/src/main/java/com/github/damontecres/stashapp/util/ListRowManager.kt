package com.github.damontecres.stashapp.util

import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.DiffCallback
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.SparseArrayObjectAdapter
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.GalleryData
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.MovieData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.StudioData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.data.DataType

/**
 * Manages a [ListRow] handling adding/removing items, or even the adding/remove the row itself
 */
class ListRowManager<T>(
    dataType: DataType,
    rowModifier: SparseArrayRowModifier,
    private val adapter: ArrayObjectAdapter,
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
    private val diffCallback: DiffCallback<*>
    private val idExtractor: (Any?) -> String

    init {
        addRowCallback = rowModifier
        removeRowCallback = rowModifier
        when (dataType) {
            DataType.SCENE -> {
                diffCallback = SceneDiffCallback
                idExtractor = { item -> (item as SlimSceneData).id }
            }
            DataType.MOVIE -> {
                diffCallback = MovieDiffCallback
                idExtractor = { item -> (item as MovieData).id }
            }
            DataType.MARKER -> {
                diffCallback = MarkerDiffCallback
                idExtractor = { item -> (item as MarkerData).id }
            }
            DataType.PERFORMER -> {
                diffCallback = PerformerDiffCallback
                idExtractor = { item -> (item as PerformerData).id }
            }
            DataType.STUDIO -> {
                diffCallback = StudioDiffCallback
                idExtractor = { item -> (item as StudioData).id }
            }
            DataType.TAG -> {
                diffCallback = TagDiffCallback
                idExtractor = { item -> (item as TagData).id }
            }
            DataType.IMAGE -> {
                diffCallback = ImageDiffCallback
                idExtractor = { item -> (item as ImageData).id }
            }
            DataType.GALLERY -> {
                diffCallback = GalleryDiffCallback
                idExtractor = { item -> (item as GalleryData).id }
            }
        }
    }

    /**
     * Add an item to the row
     * @return true if the item existed and was removed
     */
    suspend fun remove(item: T): Boolean {
        val currentIds = adapter.unmodifiableList<T>().map { idExtractor(it) }.toMutableList()
        if (currentIds.remove(idExtractor(item))) {
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
        val currentIds = adapter.unmodifiableList<T>().map { idExtractor(it) }.toMutableList()
        if (!currentIds.contains(id)) {
            val wasEmpty = adapter.isEmpty()
            currentIds.add(id)
            val results = setItemsCallback.setIds(currentIds)
            adapter.setItems(results, diffCallback)
            if (wasEmpty) {
                addRowCallback.addRow(ListRow(HeaderItem(name), adapter))
            }
            return results.firstOrNull { idExtractor(it) == id } as T
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
    class SparseArrayRowModifier(private val adapter: SparseArrayObjectAdapter, private val position: Int) : RowAdder, RowRemover {
        override fun addRow(row: ListRow) {
            adapter.set(position, row)
        }

        override fun removeRow() {
            adapter.clear(position)
        }
    }
}
