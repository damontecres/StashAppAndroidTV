package com.github.damontecres.stashapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.ListPopupWindow
import androidx.fragment.app.FragmentActivity
import androidx.leanback.widget.ClassPresenterSelector
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.MarkerData
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StashPresenter
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
import com.github.damontecres.stashapp.util.getInt
import com.github.damontecres.stashapp.util.toPx
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.launch

class FilterListActivity : FragmentActivity() {
    private lateinit var titleTextView: TextView
    private lateinit var queryEngine: QueryEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        queryEngine = QueryEngine(this, true)

        setContentView(R.layout.filter_list)

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

        titleTextView = findViewById(R.id.list_title)

        val exHandler =
            CoroutineExceptionHandler { _, ex: Throwable ->
                Log.e(TAG, "Error in filter coroutine", ex)
                Toast.makeText(this, "Error: ${ex.message}", Toast.LENGTH_LONG).show()
            }

        lifecycleScope.launch(exHandler) {
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
        lifecycleScope.launch(exHandler) {
            val savedFilters = queryEngine.getSavedFilters(dataType)
            if (savedFilters.isEmpty()) {
                // TODO: Hide the filter button?
//                filterButton.visibility = View.INVISIBLE
                filterButton.setOnClickListener {
                    Toast.makeText(
                        this@FilterListActivity,
                        "No saved filters found",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            } else {
                val listPopUp =
                    ListPopupWindow(
                        this@FilterListActivity,
                        null,
                        android.R.attr.listPopupWindowStyle,
                    )
                listPopUp.inputMethodMode = ListPopupWindow.INPUT_METHOD_NEEDED
                listPopUp.anchorView = filterButton
                // TODO: Better width calculation
                listPopUp.width = this@FilterListActivity.toPx(250).toInt()
                listPopUp.isModal = true

                val adapter =
                    ArrayAdapter(
                        this@FilterListActivity,
                        R.layout.popup_item,
                        savedFilters.map { it.name },
                    )
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
        val query = intent.getStringExtra("query")
        if (savedFilterId != null) {
            // Load a saved filter
            return queryEngine.getSavedFilter(savedFilterId.toString())
        } else if (direction != null || sortBy != null || query != null) {
            // Generic filter
            return SavedFilterData(
                id = "-1",
                mode = dataType.filterMode,
                name = getString(dataType.pluralStringId),
                find_filter =
                    SavedFilterData.Find_filter(
                        q = query,
                        page = null,
                        per_page = null,
                        sort = sortBy,
                        direction = if (direction != null) SortDirectionEnum.valueOf(direction) else null,
                        __typename = "",
                    ),
                object_filter = null,
                ui_options = null,
                __typename = "",
            )
        } else {
            // Default filter
            val filter = queryEngine.getDefaultFilter(dataType)
            if (filter == null) {
                return SavedFilterData(
                    id = "-1",
                    mode = dataType.filterMode,
                    name = getString(dataType.pluralStringId),
                    find_filter =
                        SavedFilterData.Find_filter(
                            q = null,
                            page = null,
                            per_page = null,
                            sort = null,
                            direction = null,
                            __typename = "",
                        ),
                    object_filter = null,
                    ui_options = null,
                    __typename = "",
                )
            } else {
                return filter
            }
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
                R.id.list_fragment,
                fragment,
            ).commitNow()
    }

    private fun getFragment(
        dataType: DataType,
        findFilter: FindFilterType?,
        objectFilter: Any?,
    ): StashGridFragment<out Query.Data, out Any> {
        val cardSize =
            PreferenceManager.getDefaultSharedPreferences(this)
                .getInt("cardSize", getString(R.string.card_size_default))
        val performerCardSize =
            (cardSize * (ScenePresenter.CARD_WIDTH.toDouble() / PerformerPresenter.CARD_WIDTH)).toInt()
        // TODO other sizes
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
                    performerCardSize,
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
                val selectorPresenter =
                    ClassPresenterSelector().addClassPresenter(
                        MarkerData::class.java,
                        MarkerPresenter(MarkerLongClickCallBack(this)),
                    )
                StashGridFragment(
                    selectorPresenter,
                    MarkerComparator,
                    MarkerDataSupplier(findFilter, markerFilter),
                    null,
                )
            }
        }
    }

    class MarkerLongClickCallBack(private val context: Context) :
        StashPresenter.LongClickCallBack<MarkerData> {
        override val popUpItems: List<String>
            get() = listOf(context.getString(R.string.go_to_scene))

        override fun onItemLongClick(
            item: MarkerData,
            popUpItemPosition: Int,
        ) {
            val intent = Intent(context, VideoDetailsActivity::class.java)
            intent.putExtra(VideoDetailsActivity.MOVIE, item.scene.slimSceneData.id)
            context.startActivity(intent)
        }
    }

    companion object {
        const val TAG = "FilterListActivity"
    }
}
