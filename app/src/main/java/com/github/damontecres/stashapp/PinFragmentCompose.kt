package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

class PinFragmentCompose : Fragment(R.layout.pin_dialog) {
    private lateinit var pinEditText: EditText

    var onCorrectPin: () -> Unit = {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pinCode =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString("pinCode", "")
        if (pinCode.isNullOrBlank()) {
            onCorrectPin.invoke()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        pinEditText = view.findViewById(R.id.pin_edit_text)
        pinEditText.setText("")

        val pinCode =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getString(getString(R.string.pref_key_pin_code), "")

        val submit = view.findViewById<Button>(R.id.pin_submit)
        submit.setOnClickListener {
            val enteredPin = pinEditText.text.toString()
            if (enteredPin == pinCode) {
                onCorrectPin.invoke()
            } else {
                Toast.makeText(requireContext(), "Wrong PIN", Toast.LENGTH_SHORT).show()
            }
        }

        val autoSubmitPin =
            PreferenceManager
                .getDefaultSharedPreferences(requireContext())
                .getBoolean(getString(R.string.pref_key_pin_code_auto), false)
        if (autoSubmitPin) {
            submit.visibility = View.INVISIBLE
            pinEditText.doAfterTextChanged {
                val enteredPin = it.toString()
                if (enteredPin == pinCode) {
                    onCorrectPin.invoke()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pinEditText.requestFocus()
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)!!
        imm.showSoftInput(pinEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onStop() {
        super.onStop()
        val imm = getSystemService(requireContext(), InputMethodManager::class.java)!!
        imm.hideSoftInputFromWindow(pinEditText.windowToken, 0)
        pinEditText.setText("")
    }
}
