package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.TagDataSupplier
import kotlinx.coroutines.launch

class TagListActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            findViewById<TextView>(R.id.tag_title).text = "Tags"
            val queryEngine = QueryEngine(this, true)
            lifecycleScope.launch {
                val filter = queryEngine.getDefaultFilter(DataType.TAG)
                getSupportFragmentManager().beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        StashGridFragment(
                            tagComparator,
                            TagDataSupplier(
                                convertFilter(filter?.find_filter),
                                convertTagObjectFilter(filter?.object_filter)
                            )
                        )
                    )
                    .commitNow()
            }
        }
    }
}

