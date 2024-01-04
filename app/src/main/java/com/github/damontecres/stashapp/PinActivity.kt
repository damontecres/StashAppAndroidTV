package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import kotlin.system.exitProcess

class PinActivity : SecureFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, PinFragment())
                .commitNow()
        }
        onBackPressedDispatcher.addCallback(this) {
            // Finish this activity and everything above (typically another Activity if the the app was resumed)
            finishAffinity()
        }
    }

    class PinFragment : Fragment(R.layout.pin_dialog) {

        private lateinit var pinEditText: EditText

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val pinCode = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("pinCode", "")
            if (pinCode.isNullOrBlank()) {
                startMainActivity()
            }
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            pinEditText = view.findViewById(R.id.pin_edit_text)

            val pinCode = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString(getString(R.string.pref_key_pin_code), "")

            val submit = view.findViewById<Button>(R.id.pin_submit)
            submit.setOnClickListener {
                val enteredPin = pinEditText.text.toString()
                if (enteredPin == pinCode) {
                    startMainActivity()
                } else {
                    Toast.makeText(requireContext(), "Wrong PIN", Toast.LENGTH_SHORT).show()
                }
            }

            val autoSubmitPin =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getBoolean(getString(R.string.pref_key_pin_code_auto), false)
            if (autoSubmitPin) {
                submit.visibility = View.INVISIBLE
                pinEditText.doAfterTextChanged {
                    val enteredPin = it.toString()
                    if (enteredPin == pinCode) {
                        startMainActivity()
                    }
                }
            }
        }

        private fun startMainActivity() {
            val mainDestroyed = requireActivity().intent.getBooleanExtra("mainDestroyed", false)
            if (requireActivity().isTaskRoot || mainDestroyed) {
                // This is the task root when invoked from the start
                // mainDestroyed is true when the MainActivity was destroyed, but not the entire app
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            requireActivity().finish()
        }
    }

}
