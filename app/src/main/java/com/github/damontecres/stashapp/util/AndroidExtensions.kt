package com.github.damontecres.stashapp.util

import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.DataType.GALLERY
import com.github.damontecres.stashapp.data.DataType.GROUP
import com.github.damontecres.stashapp.data.DataType.IMAGE
import com.github.damontecres.stashapp.data.DataType.MARKER
import com.github.damontecres.stashapp.data.DataType.PERFORMER
import com.github.damontecres.stashapp.data.DataType.SCENE
import com.github.damontecres.stashapp.data.DataType.STUDIO
import com.github.damontecres.stashapp.data.DataType.TAG
import com.github.damontecres.stashapp.presenters.GalleryPresenter
import com.github.damontecres.stashapp.presenters.GroupPresenter
import com.github.damontecres.stashapp.presenters.ImagePresenter
import com.github.damontecres.stashapp.presenters.MarkerPresenter
import com.github.damontecres.stashapp.presenters.PerformerPresenter
import com.github.damontecres.stashapp.presenters.ScenePresenter
import com.github.damontecres.stashapp.presenters.StudioPresenter
import com.github.damontecres.stashapp.presenters.TagPresenter

val DataType.defaultCardWidth
    get() =
        when (this) {
            SCENE -> ScenePresenter.CARD_WIDTH
            GROUP -> GroupPresenter.CARD_WIDTH
            MARKER -> MarkerPresenter.CARD_WIDTH
            PERFORMER -> PerformerPresenter.CARD_WIDTH
            STUDIO -> StudioPresenter.CARD_WIDTH
            TAG -> TagPresenter.CARD_WIDTH
            IMAGE -> ImagePresenter.CARD_WIDTH
            GALLERY -> GalleryPresenter.CARD_WIDTH
        }

val DataType.defaultCardHeight
    get() =
        when (this) {
            SCENE -> ScenePresenter.CARD_HEIGHT
            GROUP -> GroupPresenter.CARD_HEIGHT
            MARKER -> MarkerPresenter.CARD_HEIGHT
            PERFORMER -> PerformerPresenter.CARD_HEIGHT
            STUDIO -> StudioPresenter.CARD_HEIGHT
            TAG -> TagPresenter.CARD_HEIGHT
            IMAGE -> ImagePresenter.CARD_HEIGHT
            GALLERY -> GalleryPresenter.CARD_HEIGHT
        }
