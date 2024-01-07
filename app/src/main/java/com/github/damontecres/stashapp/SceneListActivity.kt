package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.SceneDataSupplier
import kotlinx.coroutines.launch

class SceneListActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)

        val queryEngine = QueryEngine(this, true)
        lifecycleScope.launch {
            val filter = queryEngine.getDefaultFilter(DataType.SCENE)

            if (savedInstanceState == null) {
                findViewById<TextView>(R.id.tag_title).text = "Scenes"
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        StashGridFragment(
                            SceneComparator,
                            SceneDataSupplier(
                                convertFilter(filter?.find_filter),
                                convertSceneObjectFilter(filter?.object_filter),
                            ),
                        ),
                    )
                    .commitNow()
            }
        }
    }
}
