package com.github.damontecres.stashapp

import android.os.Bundle
import android.text.TextUtils
import androidx.leanback.app.SearchSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.ObjectAdapter
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.presenters.MoviePresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StashSearchFragment : SearchSupportFragment(), SearchSupportFragment.SearchResultProvider {
    private var taskJob: Job? = null

    private val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())

    private val sceneAdapter = ArrayObjectAdapter(ScenePresenter())
    private val studioAdapter = ArrayObjectAdapter(StudioPresenter())
    private val performerAdapter = ArrayObjectAdapter(PerformerPresenter())
    private val tagAdapter = ArrayObjectAdapter(TagPresenter())
    private val movieAdapter = ArrayObjectAdapter(MoviePresenter())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSearchResultProvider(this)
        setOnItemViewClickedListener(StashItemViewClickListener(requireActivity()))
        rowsAdapter.add(ListRow(HeaderItem("Scenes"), sceneAdapter))
        rowsAdapter.add(ListRow(HeaderItem("Studios"), studioAdapter))
        rowsAdapter.add(ListRow(HeaderItem("Performers"), performerAdapter))
        rowsAdapter.add(ListRow(HeaderItem("Tags"), tagAdapter))
        rowsAdapter.add(ListRow(HeaderItem("Movies"), movieAdapter))
    }

    override fun getResultsAdapter(): ObjectAdapter {
        return rowsAdapter
    }

    override fun onQueryTextChange(newQuery: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch {
                val searchDelay =
                    PreferenceManager.getDefaultSharedPreferences(requireContext())
                        .getInt("searchDelay", 500)
                delay(searchDelay.toLong())
                search(newQuery)
            }
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        taskJob?.cancel()
        taskJob =
            viewLifecycleOwner.lifecycleScope.launch {
                search(query)
            }
        return true
    }

    private suspend fun search(query: String) {
        sceneAdapter.clear()
        studioAdapter.clear()
        performerAdapter.clear()
        tagAdapter.clear()
        movieAdapter.clear()

        if (!TextUtils.isEmpty(query)) {
            val perPage =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    .getInt("maxSearchResults", 25)
            val filter =
                FindFilterType(
                    q = Optional.present(query),
                    per_page = Optional.present(perPage),
                )
            val queryEngine = QueryEngine(requireContext(), true)
            viewLifecycleOwner.lifecycleScope.async {
                sceneAdapter.addAll(0, queryEngine.findScenes(filter))
            }
            viewLifecycleOwner.lifecycleScope.async {
                studioAdapter.addAll(0, queryEngine.findStudios(filter))
            }
            viewLifecycleOwner.lifecycleScope.async {
                performerAdapter.addAll(0, queryEngine.findPerformers(filter))
            }
            viewLifecycleOwner.lifecycleScope.async {
                tagAdapter.addAll(0, queryEngine.findTags(filter))
            }
            viewLifecycleOwner.lifecycleScope.async {
                movieAdapter.addAll(0, queryEngine.findMovies(filter))
            }
        }
    }
}
