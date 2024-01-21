package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.StudioComparator
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.convertFilter
import kotlinx.coroutines.launch

class FilterListActivity : SecureFragmentActivity() {
    private fun getFragment(
        dataType: DataType,
        findFilter: FindFilterType?,
        objectFilter: Map<String, Map<String, *>>?,
    ): StashGridFragment<out Query.Data, out Any>? {
        return when (dataType) {
            DataType.SCENE -> {
                val sceneFilter =
                    FilterParser.instance.convertSceneObjectFilter(objectFilter)
                StashGridFragment(SceneComparator, SceneDataSupplier(findFilter, sceneFilter))
            }

            DataType.STUDIO -> {
                val studioFilter =
                    FilterParser.instance.convertStudioObjectFilter(objectFilter)
                StashGridFragment(StudioComparator, StudioDataSupplier(findFilter, studioFilter))
            }

            DataType.PERFORMER -> {
                val performerFilter =
                    FilterParser.instance.convertPerformerObjectFilter(objectFilter)
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(findFilter, performerFilter),
                )
            }

            DataType.TAG -> {
                val tagFilter =
                    FilterParser.instance.convertTagObjectFilter(objectFilter)
                StashGridFragment(TagComparator, TagDataSupplier(findFilter, tagFilter))
            }

            DataType.MOVIE -> {
                val movieFilter = FilterParser.instance.convertMovieObjectFilter(objectFilter)
                StashGridFragment(MovieComparator, MovieDataSupplier(findFilter, movieFilter))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            val savedFilterId = intent.getStringExtra("savedFilterId")
            val direction = intent.getStringExtra("direction")
            val sortBy = intent.getStringExtra("sortBy")
            val mode = FilterMode.safeValueOf(intent.getStringExtra("mode")!!)
            val dataType = DataType.fromFilterMode(mode)
            if (dataType == null) {
                Log.w(TAG, "Got unsupported FilterMode: $mode")
                Toast.makeText(this, "Unsupporited filter mode: $mode", Toast.LENGTH_LONG).show()
            } else {
                val description = intent.getStringExtra("description")

                val title = findViewById<TextView>(R.id.tag_title)
                title.text = description

                val queryEngine = QueryEngine(this, true)
                lifecycleScope.launch {
                    val filter: FindFilterType?
                    val objectFilter: Map<String, Map<String, *>>?
                    if (savedFilterId.isNullOrEmpty()) {
                        filter =
                            FindFilterType(
                                direction =
                                    Optional.presentIfNotNull(
                                        SortDirectionEnum.safeValueOf(
                                            direction!!,
                                        ),
                                    ),
                                sort = Optional.presentIfNotNull(sortBy),
                            )
                        objectFilter = null
                    } else {
                        val result = queryEngine.getSavedFilter(savedFilterId.toString())

                        title.text = result?.name

                        filter = convertFilter(result?.find_filter)
                        objectFilter =
                            result?.object_filter as Map<String, Map<String, *>>?
                    }

                    val fragment = getFragment(dataType, filter, objectFilter)

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.tag_fragment, fragment!!)
                        .commitNow()
                }
            }
        }
    }

    companion object {
        const val TAG = "FilterListActivity"
    }
}
