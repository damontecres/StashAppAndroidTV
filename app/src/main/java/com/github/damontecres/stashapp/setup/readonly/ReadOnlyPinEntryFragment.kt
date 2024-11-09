package com.github.damontecres.stashapp.setup.readonly

import android.os.Bundle
import android.text.InputType
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.leanback.app.GuidedStepSupportFragment
import androidx.leanback.widget.GuidanceStylist
import androidx.leanback.widget.GuidedAction
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.R

class ReadOnlyPinEntryFragment(private val callback: () -> Unit) : GuidedStepSupportFragment() {
    override fun onProvideTheme(): Int {
        return R.style.Theme_StashAppAndroidTV_GuidedStep
    }

    override fun onCreateGuidance(savedInstanceState: Bundle?): GuidanceStylist.Guidance {
        return GuidanceStylist.Guidance(
            "Enter PIN",
            "PIN for settings access",
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
                .id(GuidedAction.ACTION_ID_OK)
                .title("Submit")
                .hasNext(true)
                .build(),
        )
    }

//    override fun onGuidedActionEditedAndProceed(action: GuidedAction): Long {
//        val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
//        if (action.id == ACTION_PIN) {
//            if (action.editDescription.isNotNullOrBlank()) {
//                val enteredPIN = action.editDescription.toString().toIntOrNull()
//
//                if (preferences.getBoolean(getString(R.string.pref_key_pin_code_auto), false) &&
//                    preferences.getInt(getString(R.string.pref_key_read_only_mode_pin), -1) == enteredPIN
//                ) {
//                    callback()
//                }
//                return GuidedAction.ACTION_ID_OK
//            }
//        }
//        return GuidedAction.ACTION_ID_CURRENT
//    }

    override fun onGuidedActionClicked(action: GuidedAction) {
        if (action.id == GuidedAction.ACTION_ID_OK) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val enteredPIN = findActionById(ACTION_PIN).editDescription.toString().ifBlank { null }
            val pin = preferences.getString(getString(R.string.pref_key_read_only_mode_pin), null)
            if (enteredPIN != null && pin == enteredPIN) {
                callback()
            } else {
                Toast.makeText(requireContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val ACTION_PIN = 100L
    }
}
