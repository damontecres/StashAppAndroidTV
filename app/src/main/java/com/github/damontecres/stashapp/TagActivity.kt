package com.github.damontecres.stashapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.ArrayObjectAdapter
import com.github.damontecres.stashapp.data.Tag
import com.github.damontecres.stashapp.presenters.ScenePresenter


class TagActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(
                    R.id.tag_fragment,
                    StashGridFragment(ScenePresenter()) { fragment: StashGridFragment, adapter: ArrayObjectAdapter ->
                        val tag = fragment.requireActivity().intent.getParcelableExtra<Tag>("tag")
                        if (tag != null) {
                            fragment.title = tag.name
                            val scenes = fetchScenesByTag(fragment.requireContext(), tag.id)
                            adapter.addAll(0, scenes)
                        }
                    }).commitNow()
        }
    }
}
