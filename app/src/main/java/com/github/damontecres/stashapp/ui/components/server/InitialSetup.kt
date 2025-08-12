package com.github.damontecres.stashapp.ui.components.server

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.preference.PreferenceManager
import androidx.tv.material3.MaterialTheme
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.ui.compat.isNotTvDevice
import com.github.damontecres.stashapp.ui.util.ScreenSize
import com.github.damontecres.stashapp.ui.util.screenSize
import com.github.damontecres.stashapp.util.StashServer

@Composable
fun InitialSetup(
    onServerConfigure: (StashServer) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageServersViewModel = viewModel(),
) {
    val context = LocalContext.current
    val screenSize = screenSize()

    var server by remember { mutableStateOf<StashServer?>(null) }
    var showPinDialog by remember { mutableStateOf(false) }
    val isNotTvDevice = isNotTvDevice

    fun submit(pin: String?) {
        server?.let {
            viewModel.addServer(it)
            PreferenceManager.getDefaultSharedPreferences(context).edit(true) {
                putString(context.getString(R.string.pref_key_pin_code), pin)
                putBoolean(context.getString(R.string.pref_key_pin_code_auto), true)
                if (isNotTvDevice) {
                    // Adjust some settings for a better touch device experience
                    // Remove the jump buttons
                    putBoolean(context.getString(R.string.pref_key_ui_grid_jump_controls), false)

                    // If its a larger device, use larger cards
                    if (screenSize == ScreenSize.EXPANDED) {
                        putString(context.getString(R.string.pref_key_card_size), "4")
                    }
                }
            }
            onServerConfigure.invoke(it)
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
