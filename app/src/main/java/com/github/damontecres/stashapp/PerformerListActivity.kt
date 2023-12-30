package com.github.damontecres.stashapp

import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.github.damontecres.stashapp.data.Performer
import com.github.damontecres.stashapp.suppliers.PerformerDataSupplier

class PerformerListActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag)
        if (savedInstanceState == null) {
            val performer = this.intent.getParcelableExtra<Performer>("performer")
            findViewById<TextView>(R.id.tag_title).text =
                "${performer?.name}" + if (performer?.disambiguation != null) "(${performer.disambiguation})" else ""
            getSupportFragmentManager().beginTransaction()
                .replace(
                    R.id.tag_fragment,
                    StashGridFragment(
                        performerComparator, PerformerDataSupplier()
                    )
                )
                .commitNow()
        }
    }
}

