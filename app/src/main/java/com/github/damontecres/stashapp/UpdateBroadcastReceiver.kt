package com.github.damontecres.stashapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class UpdateBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val mainIntent = Intent(context, PinActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            mainIntent.putExtra(INTENT_APP_UPDATED, true)
            context.startActivity(mainIntent)
        }
    }

    companion object {
        const val INTENT_APP_UPDATED = "UpdateBroadcastReceiver.appUpdated"
    }
}
