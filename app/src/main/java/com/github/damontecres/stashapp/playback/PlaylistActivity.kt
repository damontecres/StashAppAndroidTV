package com.github.damontecres.stashapp.playback

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.getFilterArgs

class PlaylistActivity : FragmentActivity() {
    private val viewModel: PlaylistViewModel by viewModels()

    private var fragment: PlaylistFragment<*, *, *>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val filter = intent.getFilterArgs(INTENT_FILTER)!!
            Log.v(TAG, "filter=${filter.sortAndDirection}")
            viewModel.filterArgs.value = filter
            fragment =
                when (filter.dataType) {
                    DataType.MARKER -> PlaylistMarkersFragment()
                    DataType.SCENE -> PlaylistScenesFragment()
                    else -> throw UnsupportedOperationException("${filter.dataType} not supported")
                }
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment!!)
                .commit()
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    @OptIn(UnstableApi::class)
    @SuppressLint("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        return if (fragment != null) {
            fragment!!.dispatchKeyEvent(event) || super.dispatchKeyEvent(event)
        } else {
            super.dispatchKeyEvent(event)
        }
    }

    companion object {
        const val TAG = "PlaylistActivity"
        const val INTENT_FILTER = "$TAG.filter"
    }
}
