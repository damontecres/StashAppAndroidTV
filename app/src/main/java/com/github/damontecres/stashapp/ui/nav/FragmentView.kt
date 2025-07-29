package com.github.damontecres.stashapp.ui.nav

import android.view.LayoutInflater
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.navigation.NavigationManagerCompose

@Composable
fun FragmentView(
    navManager: NavigationManagerCompose,
    destination: Destination,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            LayoutInflater
                .from(context)
                .inflate(
                    R.layout.root_fragment_layout,
                    null,
                )
        },
        update = { view ->
            navManager.navigate(destination)
        },
    )
}
