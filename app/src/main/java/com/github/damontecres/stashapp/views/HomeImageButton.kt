package com.github.damontecres.stashapp.views

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModelProvider
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.views.models.ServerViewModel

/**
 * An [AppCompatImageButton] that is the stash server icon and clicking returns to the main page
 */
class HomeImageButton(
    context: Context,
    attrs: AttributeSet?,
) : AppCompatImageButton(context, attrs) {
    private val serverViewModel by lazy {
        ViewModelProvider(findFragment<Fragment>().requireActivity())[ServerViewModel::class]
    }

    init {
        setOnClickListener {
            serverViewModel.navigationManager.goToMain()
        }
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.icon_button_selector))
    }
}
