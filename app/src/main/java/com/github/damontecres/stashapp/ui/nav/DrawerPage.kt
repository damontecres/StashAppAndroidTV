package com.github.damontecres.stashapp.ui.nav

import android.os.Parcelable
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.data.DataType
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

sealed interface DrawerPage : Parcelable {
    val iconString: Int
    val name: Int

    @Parcelize
    data object HomePage : DrawerPage {
        @IgnoredOnParcel
        override val iconString = R.string.fa_house

        @IgnoredOnParcel
        override val name = R.string.home
    }

    @Parcelize
    data object SearchPage : DrawerPage {
        @IgnoredOnParcel
        override val iconString = R.string.fa_magnifying_glass_plus

        @IgnoredOnParcel
        override val name = R.string.stashapp_actions_search
    }

    @Parcelize
    data object SettingPage : DrawerPage {
        // Unused
        @IgnoredOnParcel
        override val iconString = R.string.fa_arrow_right_arrow_left

        @IgnoredOnParcel
        override val name = R.string.stashapp_settings
    }

    @Parcelize
    data class DataTypePage(
        val dataType: DataType,
    ) : DrawerPage {
        override val iconString: Int
            get() = dataType.iconStringId
        override val name: Int
            get() = dataType.pluralStringId
    }
}
