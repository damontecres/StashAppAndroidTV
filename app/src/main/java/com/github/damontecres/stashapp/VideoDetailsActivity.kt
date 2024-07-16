package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

/**
 * Details activity class that loads [VideoDetailsFragment] class.
 */
class VideoDetailsActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.details_fragment, VideoDetailsFragment())
                .commitNow()
        }
    }

    companion object {
        const val SHARED_ELEMENT_NAME = "hero"
        const val MOVIE = "Movie"
        const val MOVIE_ID = "MovieID"
    }
}
