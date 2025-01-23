package com.github.damontecres.stashapp.ui

import android.os.Bundle
import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.StashData
import com.github.damontecres.stashapp.navigation.Destination
import com.github.damontecres.stashapp.util.getDestination
import com.github.damontecres.stashapp.views.models.ServerViewModel

class ComposeGridFragment : Fragment(R.layout.compose_frame) {
    private val serverViewModel: ServerViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val destination = requireArguments().getDestination<Destination.Filter>()
        val composeView = view.findViewById<ComposeView>(R.id.compose_view)
        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MainTheme {
                    StashGrid(
                        modifier = Modifier.padding(16.dp),
                        filterArgs = destination.filterArgs,
                        itemOnClick = {
                            serverViewModel.navigationManager.navigate(
                                Destination.fromStashData(
                                    it as StashData,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}
