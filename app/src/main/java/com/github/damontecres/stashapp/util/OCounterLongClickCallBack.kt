package com.github.damontecres.stashapp.util

import android.view.View
import android.view.View.OnClickListener
import android.view.View.OnLongClickListener
import android.widget.Toast
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.OCounter
import com.github.damontecres.stashapp.presenters.PopupOnLongClickListener
import com.github.damontecres.stashapp.presenters.StashPresenter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

fun oCounterAction(
    scope: CoroutineScope,
    id: String,
    action: suspend (String) -> Unit,
): StashPresenter.PopUpAction<OCounter> =
    StashPresenter.PopUpAction<OCounter> { cardView, item ->
        scope.launch(
            StashCoroutineExceptionHandler(
                Toast.makeText(
                    cardView.context,
                    cardView.context.getString(R.string.failed_update),
                    Toast.LENGTH_SHORT,
                ),
            ),
        ) {
            action(id)
        }
    }

fun createOCounterLongClickCallBack(
    dataType: DataType,
    id: String,
    mutationEngine: MutationEngine,
    scope: CoroutineScope,
    callBack: (newOCounter: OCounter) -> Unit,
): StashPresenter.LongClickCallBack<OCounter> =
    StashPresenter
        .LongClickCallBack<OCounter>()
        .addAction(
            StashPresenter.PopUpItem(1000L, R.string.increment),
            { readOnlyModeDisabled() },
            oCounterAction(scope, id) {
                callBack(
                    if (dataType == DataType.SCENE) {
                        mutationEngine.incrementOCounter(id)
                    } else {
                        mutationEngine.incrementImageOCounter(id)
                    },
                )
            },
        ).addAction(
            StashPresenter.PopUpItem(1001L, R.string.decrement),
            { readOnlyModeDisabled() },
            oCounterAction(scope, id) {
                callBack(
                    if (dataType == DataType.SCENE) {
                        mutationEngine.decrementOCounter(id)
                    } else {
                        mutationEngine.decrementImageOCounter(id)
                    },
                )
            },
        ).addAction(
            StashPresenter.PopUpItem(1002L, R.string.reset),
            { readOnlyModeDisabled() },
            oCounterAction(scope, id) {
                callBack(
                    if (dataType == DataType.SCENE) {
                        mutationEngine.resetOCounter(id)
                    } else {
                        mutationEngine.resetImageOCounter(id)
                    },
                )
            },
        )

/**
 * Handles modifying the O-Counter for a scene/image
 */
class OCounterLongClickCallBack(
    dataType: DataType,
    id: String,
    mutationEngine: MutationEngine,
    scope: CoroutineScope,
    callBack: (newOCounter: OCounter) -> Unit,
) : OnClickListener,
    OnLongClickListener {
    val longClickCallback =
        createOCounterLongClickCallBack(dataType, id, mutationEngine, scope, callBack)

    override fun onClick(v: View) {
        val fake = OCounter("", 0)
        val items = longClickCallback.getPopUpItems(fake)
        if (items.isNotEmpty()) {
            longClickCallback.onItemLongClick(v, fake, items[0])
        }
    }

    override fun onLongClick(v: View): Boolean {
        val fake = OCounter("", 0)
        val items = longClickCallback.getPopUpItems(fake)
        if (items.isNotEmpty()) {
            PopupOnLongClickListener(
                items.map {
                    it.text
                },
            ) { _, _, position, _ ->
                longClickCallback.onItemLongClick(v, fake, items[position])
            }.onLongClick(v)
        }
        return true
    }
}
