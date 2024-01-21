package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.suppliers.MovieDataSupplier
import kotlinx.coroutines.launch

class MovieListActivity : SecureFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            findViewById<TextView>(R.id.tag_title).text = getString(R.string.stashapp_movies)

            val queryEngine = QueryEngine(this, true)
            lifecycleScope.launch {
                val filter = queryEngine.getDefaultFilter(DataType.MOVIE)
                supportFragmentManager.beginTransaction()
                    .replace(
                        R.id.tag_fragment,
                        StashGridFragment(
                            MovieComparator,
                            MovieDataSupplier(
                                convertFilter(filter?.find_filter),
                                FilterParser.instance.convertMovieObjectFilter(filter?.object_filter),
                            ),
                        ),
                    )
                    .commitNow()
            }
        }
    }
}
