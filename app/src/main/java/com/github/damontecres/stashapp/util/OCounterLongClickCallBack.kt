package com.github.damontecres.stashapp.util

import android.content.Context
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

/**
 * Handles modifying the O-Counter for a scene
 */
class OCounterLongClickCallBack(
    private val dataType: DataType,
    private val id: String,
    private val mutationEngine: MutationEngine,
    private val scope: CoroutineScope,
    private val callBack: (newOCounter: OCounter) -> Unit,
) : StashPresenter.LongClickCallBack<OCounter>,
    OnClickListener,
    OnLongClickListener {
    override fun getPopUpItems(
        context: Context,
        item: OCounter,
    ): List<StashPresenter.PopUpItem> =
        listOf(
            StashPresenter.PopUpItem(1000L, context.getString(R.string.increment)),
            StashPresenter.PopUpItem(1001L, context.getString(R.string.decrement)),
            StashPresenter.PopUpItem(1002L, context.getString(R.string.reset)),
        )

    override fun onItemLongClick(
        context: Context,
        item: OCounter,
        popUpItem: StashPresenter.PopUpItem,
    ) {
        scope.launch(
            StashCoroutineExceptionHandler(
                Toast.makeText(
                    context,
                    context.getString(R.string.failed_o_counter),
                    Toast.LENGTH_SHORT,
                ),
            ),
        ) {
            val newOCounter =
                when (popUpItem.id) {
                    1000L ->
                        if (dataType == DataType.SCENE) {
                            mutationEngine.incrementOCounter(id)
                        } else {
                            mutationEngine.incrementImageOCounter(id)
                        }

                    1001L ->
                        if (dataType == DataType.SCENE) {
                            mutationEngine.decrementOCounter(id)
                        } else {
                            mutationEngine.decrementImageOCounter(id)
                        }

                    1002L ->
                        if (dataType == DataType.SCENE) {
                            mutationEngine.resetOCounter(id)
                        } else {
                            mutationEngine.resetImageOCounter(id)
                        }
                    else -> throw IllegalArgumentException("Unknown id ${popUpItem.id}")
                }
            callBack(newOCounter)
        }
    }

    override fun onClick(v: View) {
        val fake = OCounter("", 0)
        val items = getPopUpItems(v.context, fake)
        onItemLongClick(v.context, fake, items[0])
    }

    override fun onLongClick(v: View): Boolean {
        val fake = OCounter("", 0)
        val items = getPopUpItems(v.context, fake)
        PopupOnLongClickListener(
            items.map {
                it.text
            },
        ) { _, _, position, _ ->
            onItemLongClick(v.context, fake, items[position])
        }.onLongClick(v)
        return true
    }
}
