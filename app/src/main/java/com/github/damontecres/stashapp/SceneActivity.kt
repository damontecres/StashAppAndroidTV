package com.github.damontecres.stashapp

import android.os.Bundle
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.paging.PagingDataAdapter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.preference.PreferenceManager
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.presenters.SceneComparator
import com.github.damontecres.stashapp.presenters.ScenePagingSource
import com.github.damontecres.stashapp.presenters.ScenePresenter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class SceneActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.tag_fragment, SceneFragment())
                .commitNow()
        }
    }
}

class SceneFragment : VerticalGridSupportFragment() {

    private val mAdapter = PagingDataAdapter(ScenePresenter(), SceneComparator)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val gridPresenter = VerticalGridPresenter()
        gridPresenter.numberOfColumns =
            PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getInt("numberOfColumns", 5)
        setGridPresenter(gridPresenter)

        adapter = mAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onItemViewClickedListener = StashItemViewClickListener(requireActivity())

        val pageSize = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getInt("maxSearchResults", 50)

        val flow = Pager(
            PagingConfig(pageSize = pageSize, prefetchDistance = pageSize * 2)
        ) {
            ScenePagingSource(requireContext(), pageSize)
        }.flow
            .cachedIn(viewLifecycleOwner.lifecycleScope)

        viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest {
                mAdapter.submitData(it)
            }
        }
    }
}
