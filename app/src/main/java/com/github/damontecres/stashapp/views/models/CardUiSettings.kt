package com.github.damontecres.stashapp.views.models

/**
 * Basic UI settings that affect the cards
 */
data class CardUiSettings(
    val maxSearchResults: Int,
    val playVideoPreviews: Boolean,
    val videoPreviewAudio: Boolean,
    val columns: Int,
    val showRatings: Boolean,
    val imageCrop: Boolean,
    val videoDelay: Int,
)
