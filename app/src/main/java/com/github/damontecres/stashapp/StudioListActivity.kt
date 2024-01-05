package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.StudioDataSupplier
import kotlinx.coroutines.launch

class StudioListActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            findViewById<TextView>(R.id.tag_title).text = "Studios"
            val queryEngine = QueryEngine(this, true)
            lifecycleScope.launch {
                val filter = queryEngine.getDefaultFilter(DataType.STUDIO)

                getSupportFragmentManager().beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        StashGridFragment(
                            studioComparator, StudioDataSupplier(
                                convertFilter(filter?.find_filter),
                                convertStudioObjectFilter(filter?.object_filter)
                            )
                        )
                    )
                    .commitNow()
            }
        }
    }
}

