package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch


class TagActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.tag_fragment, StashGridFragment(ScenePresenter()) { fragment: StashGridFragment, adapter: ArrayObjectAdapter ->
                    val tagId = fragment.requireActivity().intent.getIntExtra("tagId", -1)
                    val tagName = fragment.requireActivity().intent.getStringExtra("tagName")
                    fragment.title=tagName
                    if (tagId >= 0) {
                        val scenes = fetchScenesByTag(fragment.requireContext(), tagId)
                        adapter.addAll(0, scenes)
                    }
                }).commitNow()
        }
    }
}
