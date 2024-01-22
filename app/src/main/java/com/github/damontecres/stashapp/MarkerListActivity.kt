package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.MarkerDataSupplier
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.MarkerComparator
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.convertFilter
import kotlinx.coroutines.launch

class MarkerListActivity : SecureFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)

        val queryEngine = QueryEngine(this, true)
        lifecycleScope.launch {
            val filter = queryEngine.getDefaultFilter(DataType.MARKER)

            if (savedInstanceState == null) {
                findViewById<TextView>(R.id.tag_title).text = getString(R.string.stashapp_markers)
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        StashGridFragment(
                            MarkerComparator,
                            MarkerDataSupplier(
                                convertFilter(filter?.find_filter),
                                FilterParser.instance.convertMarkerObjectFilter(filter?.object_filter),
                            ),
                        ),
                    )
                    .commitNow()
            }
        }
    }
}