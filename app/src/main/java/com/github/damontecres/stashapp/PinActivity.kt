package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager

class PinActivity : SecureFragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_browse_fragment, PinFragment())
                .commitNow()
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

            view.findViewById<Button>(R.id.pin_submit).setOnClickListener {
                val enteredPin = pinEditText.text.toString()
                val pinCode = PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("pinCode", "")
                if (enteredPin == pinCode) {
                    startMainActivity()
                } else {
                    Toast.makeText(requireContext(), "Wrong PIN", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun startMainActivity() {
            val intent = Intent(requireContext(), MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

}
