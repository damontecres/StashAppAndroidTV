package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.KeyEvent
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.getDataType

/**
 * Activity that shows a specific [DataType]
 */
class DataTypeActivity : FragmentActivity(R.layout.activity_main) {
    private var keyEventCallback: KeyEvent.Callback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dataType = intent.getDataType()
        val fragment =
            when (dataType) {
                DataType.SCENE -> SceneDetailsFragment()
                DataType.TAG -> TagFragment()
                DataType.GROUP -> GroupFragment()
                DataType.PERFORMER -> PerformerFragment()
                DataType.STUDIO -> StudioFragment()
                DataType.GALLERY -> GalleryFragment()
                DataType.MARKER -> MarkerDetailsFragment()
                DataType.IMAGE -> TODO()
            }
        if (fragment is KeyEvent.Callback) {
            keyEventCallback = fragment
        }
        supportFragmentManager.commit {
            replace(android.R.id.content, fragment)
        }
    }

    override fun onKeyUp(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean = keyEventCallback?.onKeyUp(keyCode, event) ?: false || super.onKeyUp(keyCode, event)

    override fun onKeyDown(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean =
        keyEventCallback?.onKeyDown(keyCode, event) ?: false ||
            super.onKeyDown(
                keyCode,
                event,
            )

    override fun onKeyLongPress(
        keyCode: Int,
        event: KeyEvent?,
    ): Boolean =
        keyEventCallback?.onKeyLongPress(keyCode, event) ?: false ||
            super.onKeyLongPress(
                keyCode,
                event,
            )

    override fun onKeyMultiple(
        keyCode: Int,
        repeatCount: Int,
        event: KeyEvent?,
    ): Boolean =
        keyEventCallback?.onKeyMultiple(
            keyCode,
            repeatCount,
            event,
        ) ?: false ||
            super.onKeyMultiple(keyCode, repeatCount, event)
}
