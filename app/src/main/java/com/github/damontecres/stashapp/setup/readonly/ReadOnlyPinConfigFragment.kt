package com.github.damontecres.stashapp.setup.readonly

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.util.isNotNullOrBlank

class ReadOnlyPinConfigFragment : GuidedStepSupportFragment() {
    override fun onProvideTheme(): Int {
        return R.style.Theme_StashAppAndroidTV_GuidedStep
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Read Only Mode",
            """Enabling this will prevent the app from making any server side changes.
                |
                |If Scene Play history is enabled, that will still be saved to the server.
                |
                |Additionally, the PIN will be required to change any app settings.
            """.trimMargin(),
            null,
            ContextCompat.getDrawable(requireContext(), R.mipmap.stash_logo),
        )
    }

    override fun onCreateActions(
        actions: MutableList<GuidedAction>,
        savedInstanceState: Bundle?,
    ) {
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_PIN)
                .title("Enter PIN")
                .description("")
                .editDescription("")
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_CONFIRM_PIN)
                .title("Confirm PIN")
                .description("")
                .editDescription("")
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                .enabled(false)
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_OK)
                .title("Save")
                .enabled(false)
                .hasNext(true)
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        val okAction = findActionById(GuidedAction.ACTION_ID_OK)
        if (action.id == ACTION_PIN) {
            if (action.editDescription.isNotNullOrBlank()) {
                val confirmAction = findActionById(ACTION_CONFIRM_PIN)
                confirmAction.isEnabled = true
                confirmAction.isFocusable = true
                notifyActionChanged(findActionPositionById(ACTION_CONFIRM_PIN))
                return ACTION_CONFIRM_PIN
            }
        } else if (action.id == ACTION_CONFIRM_PIN) {
            if (action.editDescription.isNotNullOrBlank()) {
                val pinCode = findActionById(ACTION_PIN).editDescription.toString().toIntOrNull()
                val confirmPin = action.editDescription.toString().toIntOrNull()

                if (pinCode != confirmPin) {
                    Toast.makeText(requireContext(), "PINs do not match!", Toast.LENGTH_SHORT)
                        .show()
                    okAction.isEnabled = false
                    notifyActionChanged(findActionPositionById(GuidedAction.ACTION_ID_OK))
                    return GuidedAction.ACTION_ID_CURRENT
                } else {
                    okAction.isEnabled = true
                    notifyActionChanged(findActionPositionById(GuidedAction.ACTION_ID_OK))
                    return GuidedAction.ACTION_ID_OK
                }
            }
        }
        return GuidedAction.ACTION_ID_CURRENT
    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_OK) {
            val pin = findActionById(ACTION_PIN).editDescription.toString().toIntOrNull()
            val confirmPin =
                findActionById(ACTION_CONFIRM_PIN).editDescription.toString().toIntOrNull()
            if (pin == confirmPin && pin != null) {
                PreferenceManager.getDefaultSharedPreferences(requireContext()).edit(true) {
                    putInt(getString(R.string.pref_key_read_only_mode_pin), pin)
                    putBoolean(getString(R.string.pref_key_read_only_mode), true)
                }
                parentFragmentManager.popBackStack()
            } else {
                Toast.makeText(requireContext(), "PINs do not match!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ACTION_PIN = 100L
        private const val ACTION_CONFIRM_PIN = 200L
    }
}
