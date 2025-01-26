package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.views.models.ServerViewModel

abstract class ComposeFragment : Fragment(R.layout.compose_frame) {
    protected val serverViewModel: ServerViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.apply {
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MainTheme {
                    Content(Modifier.fillMaxSize())
                }
            }
        }
    }

    @Composable
    abstract fun Content(modifier: Modifier)
}
