package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity


class SearchActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.tag_fragment, StashSearchFragment()).commitNow()
        }
    }
}
