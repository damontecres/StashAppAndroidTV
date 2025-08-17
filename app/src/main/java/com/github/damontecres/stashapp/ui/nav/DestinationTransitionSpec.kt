package com.github.damontecres.stashapp.ui.nav

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import com.github.damontecres.stashapp.navigation.Destination
import dev.olshevski.navigation.reimagined.NavAction
import dev.olshevski.navigation.reimagined.NavTransitionScope
import dev.olshevski.navigation.reimagined.NavTransitionSpec

class DestinationTransitionSpec : NavTransitionSpec<Destination> {
    override fun NavTransitionScope.getContentTransform(
        action: NavAction,
        from: Destination,
        to: Destination,
    ): ContentTransform {
        val enter =
            if (to is Destination.Settings) {
                slideInHorizontally { it / 2 }
            } else {
                fadeIn(tween())
            }
        val exit =
            if (from is Destination.Settings) {
                slideOutHorizontally { it / 2 }
            } else {
                fadeOut(tween())
            }
        return enter togetherWith exit
    }
}
