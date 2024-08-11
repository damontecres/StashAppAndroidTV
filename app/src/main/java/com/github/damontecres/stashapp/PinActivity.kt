package com.github.damontecres.stashapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.addCallback
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.setup.SetupActivity
import com.github.damontecres.stashapp.util.StashServer
import com.github.damontecres.stashapp.util.UpdateChecker

class PinActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            val currentServer = StashServer.getCurrentStashServer(this)
            if (currentServer == null) {
                val intent = Intent(this, SetupActivity::class.java)
                startActivity(intent)
            } else {
                onBackPressedDispatcher.addCallback(this) {
                    // Finish this activity and everything above (typically another Activity if the the app was resumed)
                    finishAffinity()
                }
                if (intent.getBooleanExtra(UpdateBroadcastReceiver.INTENT_APP_UPDATED, false)) {
                    val installedVersion = UpdateChecker.getInstalledVersion(this)
                    Toast.makeText(this, "App updated to $installedVersion!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val currentServer = StashServer.getCurrentStashServer(this)
        if (currentServer != null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, PinFragment())
                .commitNow()
        }
    }

    class PinFragment : Fragment(R.layout.pin_dialog) {
        private lateinit var pinEditText: EditText

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            val pinCode =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getString("pinCode", "")
            if (pinCode.isNullOrBlank()) {
                startMainActivity()
            }
        }

        override fun onViewCreated(
            view: View,
            savedInstanceState: Bundle?,
        ) {
            super.onViewCreated(view, savedInstanceState)

            pinEditText = view.findViewById(R.id.pin_edit_text)

            val pinCode =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
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
            (requireActivity().application as StashApplication).hasAskedForPin = true

            val mainDestroyed = requireActivity().intent.getBooleanExtra("mainDestroyed", false)
            Log.v(
                TAG,
                "startMainActivity: isTaskRoot=${requireActivity().isTaskRoot}, mainDestroyed=$mainDestroyed",
            )
            if (requireActivity().isTaskRoot || mainDestroyed) {
                // This is the task root when invoked from the start
                // mainDestroyed is true when the MainActivity was destroyed, but not the entire app
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            requireActivity().finish()
        }
    }

    companion object {
        private const val TAG = "PinActivity"
    }
}
