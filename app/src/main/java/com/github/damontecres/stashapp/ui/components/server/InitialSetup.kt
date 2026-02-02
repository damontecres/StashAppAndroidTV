package com.github.damontecres.stashapp.ui.components.server

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
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
import androidx.tv.material3.Text
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.util.ScreenSize
import com.github.damontecres.stashapp.ui.util.screenSize
import com.github.damontecres.stashapp.util.StashCoroutineExceptionHandler
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.preferences
import com.github.damontecres.stashapp.util.showToastOnMain
import com.github.damontecres.stashapp.util.updateInterfacePreferences
import com.github.damontecres.stashapp.util.updatePinPreferences
import com.github.damontecres.stashapp.views.models.ServerViewModel
import kotlinx.coroutines.launch

@Composable
fun InitialSetup(
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
    serverViewModel: ServerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val screenSize = screenSize()

    var server by remember { mutableStateOf<StashServer?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    val isNotTvDevice = isNotTvDevice

    val serverConnection by serverViewModel.serverConnection.observeAsState()

    fun submit(pin: String) {
        server?.let { newServer ->
            viewModel.addServerAsCurrent(newServer)
            scope.launch(StashCoroutineExceptionHandler(autoToast = true)) {
                try {
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
                } catch (ex: Exception) {
                    Log.e("InitialSetup", "Error saving pin", ex)
                    showToastOnMain(context, "Error saving pin", Toast.LENGTH_LONG)
                }
                serverViewModel.switchServer(newServer)
            }
        }
    }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = modifier,
    ) {
        Column {
            AddServer(
                onSubmit = {
                    server = it
                    showPinDialog = true
                },
                modifier = Modifier,
            )
            if (serverConnection is ServerViewModel.ServerConnection.Failure) {
                Text(
                    text =
                        (serverConnection as? ServerViewModel.ServerConnection.Failure)?.exception?.localizedMessage
                            ?: "",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
    AnimatedVisibility(showPinDialog) {
        Dialog(
            onDismissRequest = {
                showPinDialog = false
            },
            properties =
                DialogProperties(
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true,
                ),
        ) {
            ConfigurePin(
                onCancel = { submit("") },
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
