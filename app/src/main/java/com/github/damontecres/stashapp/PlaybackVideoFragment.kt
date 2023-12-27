package com.github.damontecres.stashapp

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.leanback.app.VideoSupportFragment
import androidx.leanback.app.VideoSupportFragmentGlueHost
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackControlGlue.ACTION_FAST_FORWARD
import androidx.leanback.media.PlaybackControlGlue.ACTION_PLAY_PAUSE
import androidx.leanback.media.PlaybackControlGlue.ACTION_REWIND
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.data.Scene


/** Handles video playback with media controls. */
class PlaybackVideoFragment : VideoSupportFragment() {

    private lateinit var mTransportControlGlue: BasicTransportControlsGlue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scene = requireActivity().intent.getParcelableExtra(DetailsActivity.MOVIE) as Scene?

        val glueHost = VideoSupportFragmentGlueHost(this@PlaybackVideoFragment)
        var skipForward = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("skip_forward_time", 30)
        var skipBack = PreferenceManager.getDefaultSharedPreferences(requireContext()).getInt("skip_back_time", 10)

        val playerAdapter = BasicMediaPlayerAdapter(requireActivity(), skipForward, skipBack)

        mTransportControlGlue = BasicTransportControlsGlue(activity, playerAdapter)
        mTransportControlGlue.host = glueHost
        mTransportControlGlue.title = scene?.title
        mTransportControlGlue.subtitle = scene?.details
        mTransportControlGlue.playWhenPrepared()

        playerAdapter.setDataSource(Uri.parse(scene?.streamUrl))
    }

    override fun onPause() {
        super.onPause()
        mTransportControlGlue.pause()
    }



    class BasicMediaPlayerAdapter(context: Context, private var skipForward: Int, private var skipBack: Int) :
        MediaPlayerAdapter(context) {

        override fun fastForward() = seekTo(currentPosition + skipForward*1000)

        override fun rewind() = seekTo(currentPosition - skipBack*1000)

        override fun getSupportedActions(): Long {
            return (ACTION_REWIND xor
                    ACTION_PLAY_PAUSE xor
                    ACTION_FAST_FORWARD).toLong()
        }
    }

    class BasicTransportControlsGlue(
        context: Context?,
        playerAdapter: BasicMediaPlayerAdapter,
    ) : PlaybackTransportControlGlue<BasicMediaPlayerAdapter>(context, playerAdapter) {
        // Primary actions
        private val forwardAction = PlaybackControlsRow.FastForwardAction(context)
        private val rewindAction = PlaybackControlsRow.RewindAction(context)

        init {
            isSeekEnabled = true // Enables scrubbing on the seekbar
        }

        override fun onCreatePrimaryActions(primaryActionsAdapter: ArrayObjectAdapter) {
            primaryActionsAdapter.add(rewindAction)
            super.onCreatePrimaryActions(primaryActionsAdapter) // Adds play/pause action
            primaryActionsAdapter.add(forwardAction)
        }

        override fun onActionClicked(action: Action?) {
            when (action) {
                forwardAction -> playerAdapter.fastForward()
                rewindAction -> playerAdapter.rewind()
                else -> super.onActionClicked(action)
            }
            onUpdateProgress() // Updates seekbar progress
        }

        override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
            // TODO: left/right pauses
            if (host.isControlsOverlayVisible || event.repeatCount > 0) {
                return super.onKey(v, keyCode, event)
            }
            return when (keyCode) {
                KeyEvent.KEYCODE_DPAD_RIGHT -> if (event.action != KeyEvent.ACTION_DOWN) false else {
                    onActionClicked(forwardAction)
                    true
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> if (event.action != KeyEvent.ACTION_DOWN) false else {
                    onActionClicked(rewindAction)
                    true
                }
                else -> super.onKey(v, keyCode, event)
            }
        }
    }

}