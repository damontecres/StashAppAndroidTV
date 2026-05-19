package com.github.damontecres.stashapp.di.services

import android.app.Application
import android.widget.Toast
import co.touchlab.kermit.Logger
import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.FilterAndPosition
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.ui.components.ItemOnClicker
import org.koin.core.annotation.Single

@Single
class ItemClicker(
    private val context: Application,
    private val navigationManager: NavigationManager,
) : ItemOnClicker<Any> {
    override fun onClick(
        item: Any,
        filterAndPosition: FilterAndPosition?,
    ) {
        when (item) {
            is FilterArgs -> {
                navigationManager.navigate(
                    Destination.Filter(
                        item,
                        true,
                    ),
                )
            }

            is ImageData -> {
                val (filter, position) = filterAndPosition!!
                navigationManager.navigate(
                    Destination.Slideshow(
                        filter,
                        position,
                        false,
                    ),
                )
            }

            is StashData -> {
                navigationManager.navigate(
                    Destination.fromStashData(item),
                )
            }

            else -> {
                Toast
                    .makeText(
                        context,
                        "Unknown item. This is probably a bug",
                        Toast.LENGTH_SHORT,
                    ).show()
                Logger.e { "Unknown item type: ${item::class.qualifiedName}" }
            }
        }
    }
}
