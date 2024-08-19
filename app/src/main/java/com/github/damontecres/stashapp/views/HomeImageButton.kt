package com.github.damontecres.stashapp.views

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import com.github.damontecres.stashapp.MainActivity
import com.github.damontecres.stashapp.R

class HomeImageButton(context: Context, attrs: AttributeSet?) : AppCompatImageButton(context, attrs) {
    init {
        setOnClickListener {
            val intent = Intent(context, MainActivity::class.java)
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            context.startActivity(intent)
        }
        setBackgroundDrawable(AppCompatResources.getDrawable(context, R.drawable.icon_button_selector))
    }
}
