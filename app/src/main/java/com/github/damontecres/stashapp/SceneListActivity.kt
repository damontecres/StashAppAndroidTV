package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.SceneComparator
import com.github.damontecres.stashapp.util.convertFilter
import kotlinx.coroutines.launch

class SceneListActivity : SecureFragmentActivity() {
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
            Toast.makeText(this, "Filter button clicked!", Toast.LENGTH_SHORT).show()
        }
        val onFocusChangeListener = StashOnFocusChangeListener(this)
        filterButton.onFocusChangeListener = onFocusChangeListener

        val queryEngine = QueryEngine(this, true)
        lifecycleScope.launch {
            val filter = queryEngine.getDefaultFilter(dataType)
            if (savedInstanceState == null) {
                val titleTextView = findViewById<TextView>(R.id.tag_title)
                if (filter?.name.isNullOrBlank()) {
                    titleTextView.text = getString(dataType.pluralStringId)
                } else {
                    titleTextView.text = filter?.name
                }

                val fragment =
                    StashGridFragment(
                        SceneComparator,
                        SceneDataSupplier(
                            convertFilter(filter?.find_filter),
                            FilterParser.instance.convertSceneObjectFilter(filter?.object_filter),
                        ),
                        filter,
                    )

                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        fragment,
                    ).commitNow()
            }
        }
    }
}
