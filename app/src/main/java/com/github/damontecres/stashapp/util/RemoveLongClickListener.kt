package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.StashApplication
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpItem.Companion.GO_TO_POPUP_ITEM
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpItem.Companion.REMOVE_POPUP_ITEM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A [StashPresenter.LongClickCallBack] which shows a 'Go To' or 'Remove' popups
 */
class RemoveLongClickListener<T : StashData>(
    private val scope: () -> CoroutineScope,
    private val rowManager: ListRowManager<T>,
    private val extraPopupItems: List<StashPresenter.PopUpItem> = emptyList(),
    private val extraPopupHandler: ((context: Context, item: T, popUpItem: StashPresenter.PopUpItem) -> Unit)? = null,
) : StashPresenter.LongClickCallBack<T> {
    override fun getPopUpItems(
        context: Context,
        item: T,
    ): List<StashPresenter.PopUpItem> =
        if (readOnlyModeDisabled()) {
            listOf(GO_TO_POPUP_ITEM, *extraPopupItems.toTypedArray(), REMOVE_POPUP_ITEM)
        } else {
            listOf(GO_TO_POPUP_ITEM, *extraPopupItems.toTypedArray())
        }

    override fun onItemLongClick(
        context: Context,
        item: T,
        popUpItem: StashPresenter.PopUpItem,
    ) {
        when (popUpItem.id) {
            StashPresenter.PopUpItem.DEFAULT_ID -> {
                StashApplication.navigationManager.navigate(Destination.fromStashData(item))
            }
            StashPresenter.PopUpItem.REMOVE_ID -> {
                if (readOnlyModeDisabled()) {
                    scope.invoke().launch(StashCoroutineExceptionHandler(autoToast = true)) {
                        Log.v(TAG, "Removing id=${item.id} (${item::class.simpleName})")
                        if (rowManager.remove(item)) {
                            val name = extractTitle(item)
                            Toast.makeText(context, "Removed '$name'", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            else -> extraPopupHandler!!.invoke(context, item, popUpItem)
        }
    }

    companion object {
        private const val TAG = "RemoveLongClickListener"
    }
}
