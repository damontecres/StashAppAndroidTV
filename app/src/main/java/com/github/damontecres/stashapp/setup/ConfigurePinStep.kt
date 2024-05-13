package com.github.damontecres.stashapp.setup

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
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.addAndSwitchServer
import com.github.damontecres.stashapp.util.isNotNullOrBlank

class ConfigurePinStep(private val server: StashServer) : GuidedStepSupportFragment() {
    private var pinCode: Int? = null

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Protect app with PIN code?",
            "Require entering a PIN whenever the app is opened",
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
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(ACTION_CONFIRM_PIN)
                .title("Confirm PIN")
                .description("")
                .editDescription("")
                .descriptionEditable(true)
                .descriptionEditInputType(InputType.TYPE_CLASS_NUMBER)
                .focusable(false)
                .enabled(false)
                .build(),
        )
        actions.add(
            GuidedAction.Builder(requireContext())
                .id(GuidedAction.ACTION_ID_OK)
                .title("Skip")
                .description("Do not set a PIN")
                .hasNext(true)
                .build(),
        )
    }

    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
        val okAction = findActionById(GuidedAction.ACTION_ID_OK)
        if (action.id == ACTION_PIN) {
            if (action.editDescription.isNotNullOrBlank()) {
                pinCode = action.editDescription.toString().toInt()
                val confirmAction = findActionById(ACTION_CONFIRM_PIN)
                confirmAction.isEnabled = true
                confirmAction.isFocusable = true
                notifyActionChanged(findActionPositionById(ACTION_CONFIRM_PIN))

                okAction.title = "Save PIN"
                okAction.description = null
                okAction.isEnabled = false
                notifyActionChanged(findActionPositionById(GuidedAction.ACTION_ID_OK))
                return ACTION_CONFIRM_PIN
            } else {
                okAction.title = "Skip"
                okAction.description = "Do not set a PIN"
                okAction.isEnabled = true
                notifyActionChanged(findActionPositionById(GuidedAction.ACTION_ID_OK))
                return GuidedAction.ACTION_ID_CURRENT
            }
        } else if (action.id == ACTION_CONFIRM_PIN) {
            if (action.editDescription.isNotNullOrBlank()) {
                val confirmPin = action.editDescription.toString().toInt()
                if (pinCode != confirmPin) {
                    Toast.makeText(requireContext(), "PINs do not match!", Toast.LENGTH_LONG).show()
                    return GuidedAction.ACTION_ID_CURRENT
                } else {
                    okAction.title = "Save PIN"
                    okAction.description = null
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
            addAndSwitchServer(requireContext(), server)
            if (pinCode != null) {
                val manager = PreferenceManager.getDefaultSharedPreferences(requireContext())
                manager.edit(true) {
                    putString("pinCode", pinCode.toString())
                }
            }
            finishGuidedStepSupportFragments()
        }
    }

    companion object {
        private const val ACTION_PIN = 1L
        private const val ACTION_CONFIRM_PIN = 2L
        private const val ACTION_NO = 3L
    }
}
