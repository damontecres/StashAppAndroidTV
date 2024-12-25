package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import com.github.damontecres.stashapp.MainActivity
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.RootActivity

/**
 * An [AppCompatImageButton] that is the stash server icon and clicking returns to [MainActivity]
 */
class HomeImageButton(
    context: Context,
    attrs: AttributeSet?,
) : AppCompatImageButton(context, attrs) {
    init {
        setOnClickListener {
            val activity = findFragment<Fragment>().requireActivity() as RootActivity
            activity.navigationManager.goToMain()
        }
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.icon_button_selector))
    }
}
