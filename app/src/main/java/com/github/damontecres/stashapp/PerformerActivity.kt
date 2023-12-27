package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class PerformerActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_performer)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.performer_fragment, PerformerFragment())
                .replace(R.id.performer_list_fragment, PerFormerListFragment())
                .commitNow()
        }
    }
}

