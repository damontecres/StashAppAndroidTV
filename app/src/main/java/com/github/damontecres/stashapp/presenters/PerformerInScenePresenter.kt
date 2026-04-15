package com.github.damontecres.stashapp.presenters

import android.os.Build
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.util.yearsBetween

/**
 * A [PerformerPresenter] which will use the age of a [PerformerData] at specified date for the content text
 */
class PerformerInScenePresenter(
    private val date: String?,
    callback: LongClickCallBack<PerformerData>? = null,
) : PerformerPresenter(
        callback,
    ) {
    override fun getContentText(
        cardView: StashImageCardView,
        item: PerformerData,
    ): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ageInScene =
                if (item.birthdate != null && date != null) {
                    yearsBetween(item.birthdate, date)
                } else {
                    null
                }
            if (ageInScene != null) {
                val yearsOld = cardView.context.getString(R.string.stashapp_years_old)
                val yearsOldStr =
                    cardView.context.getString(
                        R.string.stashapp_media_info_performer_card_age_context,
                        ageInScene.toString(),
                        yearsOld,
                    )
                yearsOldStr
            } else {
                super.getContentText(cardView, item)
            }
        } else {
            super.getContentText(cardView, item)
        }
}
