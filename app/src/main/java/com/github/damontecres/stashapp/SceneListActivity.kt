package com.github.damontecres.stashapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.lifecycle.lifecycleScope
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.MovieComparator
import com.github.damontecres.stashapp.util.PerformerComparator
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.StudioComparator
import com.github.damontecres.stashapp.util.TagComparator
import com.github.damontecres.stashapp.util.convertFilter
import com.github.damontecres.stashapp.util.toPx
import kotlinx.coroutines.launch

class SceneListActivity<T : Query.Data, D : Any> : SecureFragmentActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        queryEngine = QueryEngine(this, true)

        setContentView(R.layout.activity_tag)

        val dataTypeStr = intent.getStringExtra("dataType")
        val dataType =
            if (dataTypeStr != null) {
                DataType.valueOf(dataTypeStr)
            } else {
                DataType.SCENE
            }

        val filterButton = findViewById<Button>(R.id.filter_button)
        filterButton.setOnClickListener {
            Toast.makeText(this, "Filters not loaded yet!", Toast.LENGTH_SHORT).show()
        }
        val onFocusChangeListener = StashOnFocusChangeListener(this)
        filterButton.onFocusChangeListener = onFocusChangeListener

        titleTextView = findViewById(R.id.tag_title)

        lifecycleScope.launch {
            val filter = getStartingFilter()
            if (savedInstanceState == null) {
                if (filter != null) {
                    setupFragment(filter)
                } else {
                    Log.e(TAG, "No starting filter found for $dataType was null")
                    finish()
                }
            }
        }
        lifecycleScope.launch {
            val savedFilters = queryEngine.getSavedFilters(dataType)
            val listPopUp =
                ListPopupWindow(
                    this@SceneListActivity,
                    null,
                    android.R.attr.listPopupWindowStyle,
                )
            listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
            listPopUp.anchorView = filterButton
            // listPopUp.width = ViewGroup.LayoutParams.MATCH_PARENT
            // TODO: Better width calculation
            listPopUp.width = this@SceneListActivity.toPx(200).toInt()
            listPopUp.isModal = true

            val focusChangeListener = StashOnFocusChangeListener(this@SceneListActivity)

            val adapter =
                object : ArrayAdapter<String>(
                    this@SceneListActivity,
                    android.R.layout.simple_list_item_1,
                    savedFilters.map { it.name },
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup,
                    ): View {
                        val itemView = super.getView(position, convertView, parent)
                        // TODO: this doesn't seem to work?
                        itemView.onFocusChangeListener = focusChangeListener
                        return itemView
                    }
                }
            listPopUp.setAdapter(adapter)

            listPopUp.setOnItemClickListener { parent: AdapterView<*>, view: View, position: Int, id: Long ->
                val filter = savedFilters[position]
                listPopUp.dismiss()
                setupFragment(filter)
            }

            filterButton.setOnClickListener {
                listPopUp.show()
                listPopUp.listView?.requestFocus()
            }
        }
    }

    private suspend fun getStartingFilter(): SavedFilterData? {
        val savedFilterId = intent.getStringExtra("savedFilterId")
        val direction = intent.getStringExtra("direction")
        val sortBy = intent.getStringExtra("sortBy")
        val dataTypeStr = intent.getStringExtra("dataType")
        val dataType =
            if (dataTypeStr != null) {
                DataType.valueOf(dataTypeStr)
            } else {
                throw RuntimeException("dataType is required")
            }
        if (savedFilterId != null) {
            // Load a saved filter
            return queryEngine.getSavedFilter(savedFilterId.toString())
        } else if (direction != null || sortBy != null) {
            // Generic filter
            val findFilter =
                FindFilterType(
                    direction =
                        Optional.presentIfNotNull(
                            SortDirectionEnum.safeValueOf(
                                direction!!,
                            ),
                        ),
                    sort = Optional.presentIfNotNull(sortBy),
                )
            return SavedFilterData(
                id = "-1",
                mode = dataType.filterMode,
                name = getString(dataType.pluralStringId),
                find_filter =
                    SavedFilterData.Find_filter(
                        q = null,
                        page = null,
                        per_page = null,
                        sort = sortBy,
                        direction = SortDirectionEnum.valueOf(direction),
                        __typename = "",
                    ),
                object_filter = null,
                ui_options = null,
                __typename = "",
            )
        } else {
            // Default filter
            return queryEngine.getDefaultFilter(dataType)
        }
    }

    private fun setupFragment(filter: SavedFilterData) {
        val dataType = DataType.fromFilterMode(filter.mode)!!
        if (filter.name.isBlank()) {
            titleTextView.text = getString(dataType.pluralStringId)
        } else {
            titleTextView.text = filter.name
        }
        val fragment =
            getFragment(dataType, convertFilter(filter.find_filter), filter.object_filter)

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.tag_fragment,
                fragment,
            ).commitNow()
    }

    private fun getFragment(
        dataType: DataType,
        findFilter: FindFilterType?,
        objectFilter: Any?,
    ): StashGridFragment<out Query.Data, out Any> {
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

            DataType.MARKER -> {
                val markerFilter = FilterParser.instance.convertMarkerObjectFilter(objectFilter)
                StashGridFragment(MarkerComparator, MarkerDataSupplier(findFilter, markerFilter))
            }
        }
    }

    companion object {
        const val TAG = "SceneListActivity"
    }
}
