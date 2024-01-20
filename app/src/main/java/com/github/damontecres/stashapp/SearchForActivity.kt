package com.github.damontecres.stashapp

import android.os.Bundle
import com.github.damontecres.stashapp.data.DataType

/**
 * Search for something
 */
class SearchForActivity : SecureFragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            val dataType = DataType.valueOf(intent.getStringExtra("dataType")!!)
            supportFragmentManager.beginTransaction()
                .replace(R.id.main_browse_fragment, SearchForFragment(dataType)).commitNow()
        }
    }
}
