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

val DataType.defaultCardWidth
    get() =
        when (this) {
            SCENE -> 345
            GROUP -> 250
            MARKER -> 345
            PERFORMER -> 254 // 2/3 of height
            STUDIO -> 327
            TAG -> 250
            IMAGE -> 345
            GALLERY -> 345
        }

val DataType.defaultCardHeight
    get() =
        when (this) {
            SCENE -> 194
            GROUP -> 250
            MARKER -> 194
            PERFORMER -> 381
            STUDIO -> 184
            TAG -> 250
            IMAGE -> 258
            GALLERY -> 258
        }
