package com.github.damontecres.stashapp.views

import android.content.Context
import android.content.Intent
import android.view.View
import android.view.View.OnClickListener
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.playback.PlaylistActivity
import com.github.damontecres.stashapp.playback.PlaylistMarkersFragment
import com.github.damontecres.stashapp.suppliers.FilterArgs

class PlayAllOnClickListener(
    private val context: Context,
    private val dataType: DataType,
    private val getFilter: () -> FilterArgs,
) : OnClickListener {
    override fun onClick(v: View) {
        when (dataType) {
            DataType.MARKER -> {
                showSimpleListPopupWindow(v, listOf("15 seconds", "20 seconds", "30 seconds", "60 seconds")) {
                    val duration =
                        when (it) {
                            0 -> 15_000L
                            1 -> 20_000L
                            2 -> 30_000L
                            3 -> 60_000L
                            else -> 30_000L
                        }
                    val intent = Intent(context, PlaylistActivity::class.java)
                    intent.putExtra(PlaylistActivity.INTENT_FILTER, getFilter())
                    intent.putExtra(PlaylistMarkersFragment.INTENT_DURATION_ID, duration)
                    context.startActivity(intent)
                }
            }

            DataType.SCENE -> {
                showSimpleListPopupWindow(v, listOf("In order", "Shuffle")) {
                    val filter =
                        when (it) {
                            0 -> getFilter()
                            1 -> getFilter().with(SortAndDirection.random())
                            else -> throw IllegalStateException("$it")
                        }
                    val intent = Intent(context, PlaylistActivity::class.java)
                    intent.putExtra(PlaylistActivity.INTENT_FILTER, filter)
                    context.startActivity(intent)
                }
            }

            else -> throw UnsupportedOperationException("DataType $dataType")
        }
    }
}
