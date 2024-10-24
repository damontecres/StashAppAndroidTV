package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.getDataType

/**
 * Activity that shows a specific [DataType]
 */
class DataTypeActivity : FragmentActivity(R.layout.activity_main) {
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
        supportFragmentManager.commit {
            replace(android.R.id.content, fragment)
        }
    }
}
