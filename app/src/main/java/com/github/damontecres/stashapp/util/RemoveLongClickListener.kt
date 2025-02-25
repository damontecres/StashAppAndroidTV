package com.github.damontecres.stashapp.util

import android.util.Log
import android.widget.Toast
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.filter.extractTitle
import com.github.damontecres.stashapp.presenters.StashPresenter
import com.github.damontecres.stashapp.presenters.StashPresenter.PopUpItem.Companion.REMOVE_POPUP_ITEM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun <T : StashData> createRemoveLongClickListener(
    scope: () -> CoroutineScope,
    rowManager: ListRowManager<T>,
): StashPresenter.LongClickCallBack<T> =
    StashPresenter
        .LongClickCallBack<T>(
            StashPresenter.PopUpItem.DEFAULT to StashPresenter.PopUpAction { cardView, _ -> cardView.performClick() },
        ).addAction(REMOVE_POPUP_ITEM, { readOnlyModeDisabled() }) { cardView, item ->
            if (readOnlyModeDisabled()) {
                scope.invoke().launch(StashCoroutineExceptionHandler(autoToast = true)) {
                    Log.v(
                        "RemoveLongClickListener",
                        "Removing id=${item.id} (${item::class.simpleName})",
                    )
                    if (rowManager.remove(item)) {
                        val name = extractTitle(item)
                        Toast.makeText(cardView.context, "Removed '$name'", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
