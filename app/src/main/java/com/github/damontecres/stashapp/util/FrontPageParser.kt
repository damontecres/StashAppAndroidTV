package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.api.type.FindFilterType
import com.github.damontecres.stashapp.api.type.SortDirectionEnum
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
import com.github.damontecres.stashapp.util.FrontPageParser.FrontPageRow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Parses the front page content from the server into a list of [FrontPageRow]s
 */
class FrontPageParser(private val queryEngine: QueryEngine, private val filterParser: FilterParser, private val pageSize: Int = 25) {
    companion object {
        const val TAG = "MainPageParser"
    }

    enum class FrontPageRowResult {
        SUCCESS,
        ERROR,
        DATA_TYPE_NOT_SUPPORTED,
    }

    data class FrontPageRowData(val name: String, val filter: StashFilter, val data: List<*>)

    data class FrontPageRow(val result: FrontPageRowResult, val data: FrontPageRowData?) {
        constructor(
            name: String,
            filter: StashFilter,
            data: List<*>,
        ) : this(FrontPageRowResult.SUCCESS, FrontPageRowData(name, filter, data))

        constructor(result: FrontPageRowResult) : this(result, null)

        val successful get() = result == FrontPageRowResult.SUCCESS && data!!.data.isNotEmpty()

        companion object {
            val ERROR = FrontPageRow(FrontPageRowResult.ERROR)
            val NOT_SUPPORTED = FrontPageRow(FrontPageRowResult.DATA_TYPE_NOT_SUPPORTED)
        }
    }

    suspend fun parse(frontPageContent: List<Map<String, *>>): List<Deferred<FrontPageRow>> {
        return frontPageContent.mapIndexed { index, frontPageFilter ->
            when (
                val filterType =
                    frontPageFilter["__typename"] as String
            ) {
                "CustomFilter" -> {
                    addCustomFilterRow(
                        frontPageFilter,
                        queryEngine,
                    )
                }

                "SavedFilter" -> {
                    addSavedFilterRow(
                        frontPageFilter,
                        queryEngine,
                        filterParser,
                        index,
                    )
                }

                else -> {
                    Log.w(
                        TAG,
                        "Unknown frontPageFilter typename: $filterType",
                    )
                    CompletableDeferred(FrontPageRow.NOT_SUPPORTED)
                }
            }
        }
    }

    private suspend fun addCustomFilterRow(
        frontPageFilter: Map<String, *>,
        queryEngine: QueryEngine,
    ): Deferred<FrontPageRow> =
        withContext(Dispatchers.IO) {
            try {
                val msg = frontPageFilter["message"] as Map<String, *>
                val objType =
                    (msg["values"] as Map<String, String>)["objects"] as String
                val description =
                    when (msg["id"].toString()) {
                        "recently_added_objects" -> "Recently Added $objType"
                        "recently_released_objects" -> "Recently Released $objType"
                        else -> objType
                    }

                val sortBy =
                    (frontPageFilter.getCaseInsensitive("sortBy") as String?)
                        ?: when (msg["id"].toString()) {
                            // Just in case, fall back to a reasonable default
                            "recently_added_objects" -> "created_at"
                            "recently_released_objects" -> "date"
                            else -> null
                        }
                val mode = FilterMode.safeValueOf(frontPageFilter["mode"] as String)
                if (mode !in supportedFilterModes) {
                    Log.w(TAG, "CustomFilter mode is $mode which is not supported yet")
                    return@withContext CompletableDeferred(FrontPageRow(FrontPageRowResult.DATA_TYPE_NOT_SUPPORTED))
                }
                val job =
                    async {
                        try {
                            val direction = frontPageFilter["direction"] as String?
                            val directionEnum =
                                if (direction != null) {
                                    val enum = SortDirectionEnum.safeValueOf(direction.uppercase())
                                    if (enum == SortDirectionEnum.UNKNOWN__) {
                                        SortDirectionEnum.DESC
                                    } else {
                                        enum
                                    }
                                } else {
                                    SortDirectionEnum.DESC
                                }

                            val filter =
                                FindFilterType(
                                    direction = Optional.presentIfNotNull(directionEnum),
                                    sort = Optional.presentIfNotNull(sortBy),
                                    per_page = Optional.present(pageSize),
                                )
                            val data =
                                when (mode) {
                                    FilterMode.SCENES -> {
                                        queryEngine.findScenes(filter)
                                    }

                                    FilterMode.STUDIOS -> {
                                        queryEngine.findStudios(filter)
                                    }

                                    FilterMode.PERFORMERS -> {
                                        queryEngine.findPerformers(filter)
                                    }

                                    FilterMode.MOVIES -> {
                                        queryEngine.findMovies(filter)
                                    }

                                    FilterMode.IMAGES -> {
                                        queryEngine.findImages(filter)
                                    }

                                    FilterMode.GALLERIES -> {
                                        queryEngine.findGalleries(filter)
                                    }

                                    else -> {
                                        Log.w(TAG, "Unsupported mode in frontpage: $mode")
                                        listOf()
                                    }
                                }
                            val customFilter =
                                StashCustomFilter(mode, direction, sortBy, description)
                            FrontPageRow(description, customFilter, data)
                        } catch (ex: Exception) {
                            Log.e(TAG, "Exception in addCustomFilterRow", ex)
                            FrontPageRow.ERROR
                        }
                    }
                return@withContext job
            } catch (ex: Exception) {
                Log.e(TAG, "Exception during addCustomFilterRow", ex)
                CompletableDeferred(FrontPageRow.ERROR)
            }
        }

    private suspend fun addSavedFilterRow(
        frontPageFilter: Map<String, *>,
        queryEngine: QueryEngine,
        filterParser: FilterParser,
        index: Int,
    ): Deferred<FrontPageRow> =
        withContext(Dispatchers.IO) {
            return@withContext async {
                val filterId = frontPageFilter.getCaseInsensitive("savedFilterId")
                try {
                    val result = queryEngine.getSavedFilter(filterId.toString())
                    if (result?.mode in supportedFilterModes) {
                        val filter =
                            queryEngine.updateFilter(
                                convertFilter(result?.find_filter),
                                useRandom = true,
                            )?.copy(per_page = Optional.present(pageSize))
                        val objectFilter =
                            result?.object_filter as Map<String, Map<String, *>>?

                        val data =
                            when (result?.mode) {
                                FilterMode.SCENES -> {
                                    val sceneFilter =
                                        filterParser.convertSceneObjectFilter(objectFilter)
                                    queryEngine.findScenes(filter, sceneFilter, useRandom = false)
                                }

                                FilterMode.STUDIOS -> {
                                    val studioFilter =
                                        filterParser.convertStudioObjectFilter(objectFilter)
                                    queryEngine.findStudios(
                                        filter,
                                        studioFilter,
                                        useRandom = false,
                                    )
                                }

                                FilterMode.PERFORMERS -> {
                                    val performerFilter =
                                        filterParser.convertPerformerObjectFilter(objectFilter)

                                    queryEngine.findPerformers(
                                        filter,
                                        performerFilter,
                                        useRandom = false,
                                    )
                                }

                                FilterMode.TAGS -> {
                                    val tagFilter =
                                        filterParser.convertTagObjectFilter(objectFilter)

                                    queryEngine.findTags(filter, tagFilter, useRandom = false)
                                }

                                FilterMode.IMAGES -> {
                                    val imageFilter =
                                        filterParser.convertImageObjectFilter(objectFilter)
                                    queryEngine.findImages(filter, imageFilter, useRandom = false)
                                }

                                FilterMode.GALLERIES -> {
                                    val galleryFilter =
                                        filterParser.convertGalleryObjectFilter(objectFilter)
                                    queryEngine.findGalleries(filter, galleryFilter, useRandom = false)
                                }

                                else -> {
                                    Log.w(
                                        TAG,
                                        "Unsupported mode in frontpage: ${result?.mode}",
                                    )
                                    listOf()
                                }
                            }
                        val savedFilter =
                            StashSavedFilter(
                                filterId.toString(),
                                result!!.mode,
                                filter?.sort?.getOrNull(),
                            )
                        FrontPageRow(result.name, savedFilter, data)
                    } else {
                        Log.w(TAG, "SavedFilter mode is ${result?.mode} which is not supported yet")
                        FrontPageRow.NOT_SUPPORTED
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Exception in addSavedFilterRow filterId=$filterId", ex)
                    FrontPageRow.ERROR
                }
            }
        }
}
