package com.github.damontecres.stashapp.views

import android.view.View
import android.view.View.OnClickListener
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.navigation.NavigationManager

/**
 * The "Play All" button clicker which starts playback for multiple scenes or markers or slideshow for images
 */
class PlayAllOnClickListener(
    private val navigationManager: NavigationManager,
    private val dataType: DataType,
    private val getFilter: () -> FilterAndPosition,
) : OnClickListener {
    override fun onClick(v: View) {
        val filterAndPosition = getFilter()
        when (dataType) {
            DataType.MARKER -> {
                showSimpleListPopupWindow(
                    v,
                    listOf(
                        "15 seconds",
                        "20 seconds",
                        "30 seconds",
                        "60 seconds",
                        "5 minutes",
                        "10 minutes",
                        "20 minutes",
                    ),
                ) {
                    val duration =
                        when (it) {
                            0 -> 15_000L
                            1 -> 20_000L
                            2 -> 30_000L
                            3 -> 60_000L
                            4 -> 5 * 60_000L
                            5 -> 10 * 60_000L
                            6 -> 20 * 60_000L
                            else -> 30_000L
                        }
                    navigationManager.navigate(
                        Destination.Playlist(
                            filterAndPosition.filter,
                            0,
                            duration,
                        ),
                    )
                }
            }

            DataType.SCENE -> {
                showSimpleListPopupWindow(v, listOf("In order", "Shuffle")) {
                    val filter =
                        when (it) {
                            0 -> filterAndPosition.filter
                            1 -> filterAndPosition.filter.with(SortAndDirection.random())
                            else -> throw IllegalStateException("$it")
                        }
                    navigationManager.navigate(Destination.Playlist(filter, 0))
                }
            }

            DataType.IMAGE -> {
                navigationManager.navigate(
                    Destination.Slideshow(
                        filterAndPosition.filter,
                        filterAndPosition.position,
                        true,
                    ),
                )
            }

            else -> throw UnsupportedOperationException("DataType $dataType")
        }
    }
}
