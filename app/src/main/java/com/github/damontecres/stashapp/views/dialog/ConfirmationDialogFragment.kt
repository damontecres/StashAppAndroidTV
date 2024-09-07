package com.github.damontecres.stashapp.views.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.github.damontecres.stashapp.R

class ConfirmationDialogFragment(
    private val message: String,
    private val onClickListener: DialogInterface.OnClickListener,
) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton(getString(R.string.stashapp_actions_confirm), onClickListener)
            .setNegativeButton(getString(R.string.stashapp_actions_cancel), onClickListener)
            .create()
    }
}
