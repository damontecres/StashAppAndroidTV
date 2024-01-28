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
import com.apollographql.apollo3.api.Query
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.convertFilter
import com.github.damontecres.stashapp.util.toPx
import kotlinx.coroutines.launch

class SceneListActivity<T : Query.Data, D : Any> : SecureFragmentActivity() {
    private lateinit var titleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val queryEngine = QueryEngine(this, true)
        lifecycleScope.launch {
            val filter = queryEngine.getDefaultFilter(dataType)
            if (savedInstanceState == null) {
                if (filter != null) {
                    setupFragment(filter)
                } else {
                    Log.e(TAG, "Default filter for $dataType was null")
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

    private fun setupFragment(filter: SavedFilterData) {
        val dataType = DataType.fromFilterMode(filter.mode)
        if (filter.name.isBlank()) {
            titleTextView.text = getString(dataType!!.pluralStringId)
        } else {
            titleTextView.text = filter.name
        }
        when (dataType) {
            DataType.SCENE -> {
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        StashGridFragment(
                            SceneComparator,
                            SceneDataSupplier(
                                convertFilter(filter.find_filter),
                                FilterParser.instance.convertSceneObjectFilter(filter.object_filter),
                            ),
                            filter,
                        ),
                    ).commitNow()
            }

            else -> {
            }
        }
    }

    companion object {
        const val TAG = "SceneListActivity"
    }
}
