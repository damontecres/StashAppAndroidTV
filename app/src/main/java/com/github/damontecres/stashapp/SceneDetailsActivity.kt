package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.util.isNavHostActive

/**
 * Details activity class that loads [SceneDetailsFragment] class.
 */
class SceneDetailsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        if (savedInstanceState == null) {
            if (isNavHostActive()) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.details_fragment, NavFragment(SceneDetailsFragment()))
                    .commitNow()
            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.details_fragment, SceneDetailsFragment())
                    .commitNow()
            }
        }
    }

    companion object {
        const val SHARED_ELEMENT_NAME = "hero"
        const val MOVIE = "Movie"
        const val MOVIE_ID = "MovieID"
    }
}
