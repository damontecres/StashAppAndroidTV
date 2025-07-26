package com.github.damontecres.stashapp.ui

import com.github.damontecres.stashapp.api.fragment.ImageData
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.fragment.SlimTagData
import com.github.damontecres.stashapp.api.fragment.TagData
import com.github.damontecres.stashapp.api.fragment.VideoFile
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.ui.components.StarRatingPrecision
import com.github.damontecres.stashapp.util.asSlimTagData
import com.github.damontecres.stashapp.util.toSeconds
import com.github.damontecres.stashapp.views.models.CardUiSettings
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

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

val slimScenePreview =
    SlimSceneData(
        id = "9876",
        title = "Scene title",
        code = "st-9876",
        details = @Suppress("ktlint:standard:max-line-length")
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer dictum sagittis fermentum. Nam metus augue, tempor at auctor non, egestas in augue. Curabitur ut sagittis velit. Donec volutpat aliquet odio vel dignissim. In pharetra mauris eget maximus vehicula. Sed sit amet tellus orci. Proin lobortis risus eu augue efficitur porttitor. Etiam eu felis blandit, sagittis massa ut, sodales elit. Vivamus posuere lorem purus. Donec at ipsum a ex cursus sodales. Suspendisse sollicitudin, risus id condimentum hendrerit, neque nisi fringilla diam, in tempus diam massa vitae risus.",
        director = "Mr Director",
        urls = listOf(),
        date = "2025-01-01",
        rating100 = 75,
        play_count = 2,
        play_duration = (15.minutes + 20.seconds).toSeconds,
        o_counter = 2,
        organized = true,
        resume_time = null,
        created_at = "2025-01-01",
        updated_at = "2025-01-01",
        files =
            listOf(
                SlimSceneData.File(
                    __typename = "",
                    videoFile =
                        VideoFile(
                            id = "4555",
                            path = "",
                            size = 10_222_5555_111,
                            mod_time = "2025-01-01",
                            duration = (17.minutes + 43.seconds).toSeconds,
                            video_codec = "h264",
                            audio_codec = "aac",
                            format = "mkv",
                            width = 1920,
                            height = 1080,
                            frame_rate = 24.94,
                            bit_rate = 2600,
                            __typename = "",
                        ),
                ),
            ),
        paths =
            SlimSceneData.Paths(
                screenshot = null,
                preview = null,
                stream = null,
                sprite = null,
                caption = null,
            ),
        scene_markers = listOf(),
        galleries = listOf(),
        studio = null,
        groups = listOf(),
        tags = listOf(SlimSceneData.Tag("", tagPreview.asSlimTagData)),
        performers = listOf(SlimSceneData.Performer("122", "Performer 122")),
    )

val imagePreview =
    ImageData(
        id = "341",
        title = "The image title",
        code = "9999",
        rating100 = 40,
        date = "2015-01-01",
        details = "",
        photographer = "He takes the pics",
        o_counter = 3,
        created_at = "",
        updated_at = "",
        paths =
            ImageData.Paths(
                thumbnail = null,
                preview = null,
                image = null,
            ),
        performers =
            listOf(
                ImageData.Performer("12", "Performer #1"),
                ImageData.Performer("13", "Performer #2"),
            ),
        studio = ImageData.Studio("", "First Pictures"),
        tags = listOf(),
        galleries = listOf(),
        visual_files =
            listOf(
                ImageData.Visual_file(
                    "",
                    onBaseFile =
                        ImageData.OnBaseFile(
                            id = "",
                            path = "",
                            size = 10_423_122,
                            __typename = "",
                        ),
                    onImageFile = null,
                    onVideoFile =
                        ImageData.OnVideoFile(
                            width = 800,
                            height = 600,
                            format = "gif",
                            video_codec = "gif",
                            audio_codec = "",
                            duration = 3.seconds.toSeconds,
                        ),
                ),
            ),
    )
