package com.github.damontecres.stashapp.data

import com.github.damontecres.stashapp.R

data class SortOption(val key: String, val nameStringId: Int)

val SCENE_SORT_OPTIONS =
    listOf(
        SortOption("title", R.string.stashapp_title),
        SortOption("path", R.string.stashapp_path),
        SortOption("rating", R.string.stashapp_rating),
        SortOption("file_mod_time", R.string.stashapp_file_mod_time),
        SortOption("tag_count", R.string.stashapp_tag_count),
        SortOption("performer_count", R.string.stashapp_performer_count),
        SortOption("random", R.string.stashapp_random),
        SortOption("organized", R.string.stashapp_organized),
        SortOption("o_counter", R.string.stashapp_o_counter),
        SortOption("date", R.string.stashapp_date),
        SortOption("file_count", R.string.stashapp_file_count),
        SortOption("filesize", R.string.stashapp_filesize),
        SortOption("duration", R.string.stashapp_duration),
        SortOption("framerate", R.string.stashapp_framerate),
        SortOption("bitrate", R.string.stashapp_bitrate),
        SortOption("last_played_at", R.string.stashapp_last_played_at),
        SortOption("resume_time", R.string.stashapp_resume_time),
        SortOption("play_duration", R.string.stashapp_play_duration),
        SortOption("play_count", R.string.stashapp_play_count),
        SortOption("movie_scene_number", R.string.stashapp_movie_scene_number),
        SortOption("interactive", R.string.stashapp_interactive),
        SortOption("interactive_speed", R.string.stashapp_interactive_speed),
        SortOption("perceptual_similarity", R.string.stashapp_perceptual_similarity),
    ).sortedBy { it.key }
