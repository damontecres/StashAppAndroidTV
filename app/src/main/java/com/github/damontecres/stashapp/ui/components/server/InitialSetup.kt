package com.github.damontecres.stashapp.ui.components.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.util.ScreenSize
import com.github.damontecres.stashapp.ui.util.screenSize
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.preferences
import com.github.damontecres.stashapp.util.updateInterfacePreferences
import com.github.damontecres.stashapp.util.updatePinPreferences
import kotlinx.coroutines.launch

@Composable
fun InitialSetup(
    onServerConfigure: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenSize = screenSize()

    var server by remember { mutableStateOf<StashServer?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }

    fun submit(pin: String?) {
        server?.let {
            viewModel.addServer(it)
            scope.launch(StashCoroutineExceptionHandler()) {
                context.preferences.updateData {
                    it
                        .updatePinPreferences {
                            this.pin = pin
                            autoSubmit = true
                        }.updateInterfacePreferences {
                            if (isNotTvDevice) {
                                // Adjust some settings for a better touch device experience
                                // Remove the jump buttons
                                showGridJumpButtons = false
                                // If its a larger device, use larger cards
                                if (screenSize == ScreenSize.EXPANDED) {
                                    cardSize = 4
                                }
                            }
                        }
                }
                onServerConfigure.invoke(it)
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier,
    ) {
        AddServer(
            onSubmit = {
                server = it
                showPinDialog = true
            },
            modifier = Modifier,
        )
    }
    AnimatedVisibility(showPinDialog) {
        Dialog(
            onDismissRequest = {
                showPinDialog = false
            },
            properties =
                DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                ),
        ) {
            ConfigurePin(
                onSkip = { submit(null) },
                onSubmit = { submit(it) },
                modifier =
                    Modifier
                        .background(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                        ),
            )
        }
    }
}
