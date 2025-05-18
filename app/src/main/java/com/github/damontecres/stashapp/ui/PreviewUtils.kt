package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.views.models.CardUiSettings

val performerPreview =
    PerformerData(
        id = "123",
        name = "Performer Name",
        disambiguation = "Disambiguation",
        gender = GenderEnum.FEMALE,
        birthdate = "1980-01-01",
        ethnicity = "White",
        country = null,
        eye_color = "Blue",
        height_cm = 155,
        measurements = null,
        fake_tits = null,
        penis_length = null,
        circumcised = null,
        career_length = "2001-2007",
        tattoos = null,
        piercings = null,
        alias_list = listOf("Alias1", "Alias2"),
        favorite = true,
        image_path = "image",
        scene_count = 10,
        image_count = 150,
        gallery_count = 3,
        group_count = 3,
        o_counter = 1,
        created_at = "2024-03-11T13:42:30-04:00",
        updated_at = "2024-03-11T13:42:30-04:00",
        tags =
            listOf(
                PerformerData.Tag(
                    __typename = "",
                    slimTagData =
                        SlimTagData(
                            id = "12",
                            name = "The tag",
                        ),
                ),
            ),
        rating100 = 80,
        details = "Some details here",
        death_date = null,
        hair_color = null,
        weight = 55,
        custom_fields = Unit,
        __typename = "__typename",
    )

val tagPreview =
    TagData(
        id = "722",
        name = "Midpoint",
        description = null,
        favorite = false,
        aliases = listOf("Center"),
        scene_count = 45,
        performer_count = 12,
        scene_marker_count = 10,
        image_path = null,
        image_count = 2000,
        gallery_count = 88,
        parent_count = 1,
        child_count = 1,
        created_at = "2024-03-11T13:42:30-04:00",
        updated_at = "2024-03-11T13:42:30-04:00",
        sort_name = null,
    )

val uiConfigPreview =
    ComposeUiConfig(
        ratingAsStars = true,
        starPrecision = StarRatingPrecision.HALF,
        showStudioAsText = true,
        debugTextEnabled = true,
        showTitleDuringPlayback = true,
        readOnlyModeEnabled = false,
        showCardProgress = true,
        playSoundOnFocus = true,
        cardSettings =
            CardUiSettings(
                maxSearchResults = 25,
                playVideoPreviews = true,
                videoPreviewAudio = false,
                columns = 5,
                showRatings = true,
                imageCrop = true,
                videoDelay = 1,
            ),
    )
