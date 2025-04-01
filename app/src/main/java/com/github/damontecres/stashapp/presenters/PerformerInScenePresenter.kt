package com.github.damontecres.stashapp.presenters

import android.os.Build
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.util.StashServer
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter

/**
 * A [PerformerPresenter] which will use the age of a [PerformerData] at specified date for the content text
 */
class PerformerInScenePresenter(
    server: StashServer,
    private val date: String?,
    callback: LongClickCallBack<PerformerData>? = null,
) : PerformerPresenter(server, callback) {
    override fun getContentText(
        cardView: StashImageCardView,
        item: PerformerData,
    ): CharSequence? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ageInScene =
                if (item.birthdate != null && date != null) {
                    Period
                        .between(
                            LocalDate.parse(item.birthdate, DateTimeFormatter.ISO_LOCAL_DATE),
                            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE),
                        ).years
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
