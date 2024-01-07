package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import kotlinx.coroutines.launch

class FilterListActivity : FragmentActivity() {
    private fun getFragment(
        mode: FilterMode,
        findFilter: FindFilterType?,
        objectFilter: Map<String, Map<String, *>>?,
    ): StashGridFragment<out Query.Data, out Any>? {
        return when (mode) {
            FilterMode.SCENES -> {
                val sceneFilter =
                    convertSceneObjectFilter(objectFilter)
                StashGridFragment(SceneComparator, SceneDataSupplier(findFilter, sceneFilter))
            }

            FilterMode.STUDIOS -> {
                val studioFilter =
                    convertStudioObjectFilter(objectFilter)
                StashGridFragment(StudioComparator, StudioDataSupplier(findFilter, studioFilter))
            }

            FilterMode.PERFORMERS -> {
                val performerFilter =
                    convertPerformerObjectFilter(objectFilter)
                StashGridFragment(
                    PerformerComparator,
                    PerformerDataSupplier(findFilter, performerFilter),
                )
            }

            FilterMode.TAGS -> {
                val tagFilter =
                    convertTagObjectFilter(objectFilter)
                StashGridFragment(TagComparator, TagDataSupplier(findFilter, tagFilter))
            }

            else -> {
                null
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

                val fragment = getFragment(mode, filter, objectFilter)

                supportFragmentManager.beginTransaction()
                    .replace(R.id.tag_fragment, fragment!!)
                    .commitNow()
            }
        }
    }
}
