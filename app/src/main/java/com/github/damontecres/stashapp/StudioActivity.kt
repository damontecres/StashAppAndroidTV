package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.ArrayObjectAdapter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter


class StudioActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.tag_fragment, StashGridFragment(ScenePresenter()) { fragment, adapter ->
                    val studioId = fragment.requireActivity().intent.getIntExtra("studioId", -1)
                    val studioName = fragment.requireActivity().intent.getStringExtra("studioName")
                    fragment.title=studioName
                    if (studioId >= 0) {
                        val scenes = fetchScenesByStudio(fragment.requireContext(), studioId)
                        adapter.addAll(0, scenes)
                    }
                }).commitNow()
        }
    }
}
