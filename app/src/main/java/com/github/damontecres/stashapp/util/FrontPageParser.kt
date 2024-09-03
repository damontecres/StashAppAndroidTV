package com.github.damontecres.stashapp.util

import android.content.Context
import android.util.Log
import com.github.damontecres.stashapp.R
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.SortAndDirection
import com.github.damontecres.stashapp.data.StashFindFilter
import com.github.damontecres.stashapp.suppliers.FilterArgs
import com.github.damontecres.stashapp.suppliers.toFilterArgs
import com.github.damontecres.stashapp.util.FrontPageParser.FrontPageRow
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Parses the front page content from the server into a list of [FrontPageRow]s
 */
class FrontPageParser(
    private val context: Context,
    private val queryEngine: QueryEngine,
    private val filterParser: FilterParser,
    private val pageSize: Int = 25,
) {
    companion object {
        const val TAG = "FrontPageParser"
    }

    enum class FrontPageRowResult {
        SUCCESS,
        ERROR,
        DATA_TYPE_NOT_SUPPORTED,
    }

    data class FrontPageRowData(val name: String, val filter: FilterArgs, val data: List<*>)

    data class FrontPageRow(val result: FrontPageRowResult, val data: FrontPageRowData?) {
        constructor(
            name: String,
            filter: FilterArgs,
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
                        "recently_added_objects" ->
                            context.getString(
                                R.string.stashapp_recently_added_objects,
                                objType,
                            )

                        "recently_released_objects" ->
                            context.getString(
                                R.string.stashapp_recently_released_objects,
                                objType,
                            )
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
                            val dataType = DataType.fromFilterMode(mode)!!
                            val customFilter =
                                FilterArgs(
                                    dataType = dataType,
                                    name = description,
                                    findFilter =
                                        StashFindFilter(
                                            SortAndDirection.create(
                                                dataType,
                                                sortBy,
                                                direction,
                                            ),
                                        ),
                                ).withResolvedRandom()
                            val data =
                                queryEngine.find(
                                    customFilter.dataType,
                                    customFilter.findFilter!!.toFindFilterType(1, pageSize),
                                    useRandom = false,
                                )
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
                    if (result != null) {
                        val filter =
                            result.toFilterArgs(filterParser)
                                .withResolvedRandom()
                        val findFilter =
                            filter.findFilter?.toFindFilterType(page = 1, perPage = pageSize)
                        val objectFilter = result.object_filter

                        val data =
                            when (filter.dataType) {
                                DataType.SCENE -> {
                                    val sceneFilter =
                                        filterParser.convertSceneObjectFilter(objectFilter)
                                    queryEngine.findScenes(
                                        findFilter,
                                        sceneFilter,
                                        useRandom = false,
                                    )
                                }

                                DataType.STUDIO -> {
                                    val studioFilter =
                                        filterParser.convertStudioObjectFilter(objectFilter)
                                    queryEngine.findStudios(
                                        findFilter,
                                        studioFilter,
                                        useRandom = false,
                                    )
                                }

                                DataType.PERFORMER -> {
                                    val performerFilter =
                                        filterParser.convertPerformerObjectFilter(objectFilter)

                                    queryEngine.findPerformers(
                                        findFilter,
                                        performerFilter,
                                        useRandom = false,
                                    )
                                }

                                DataType.TAG -> {
                                    val tagFilter =
                                        filterParser.convertTagObjectFilter(objectFilter)

                                    queryEngine.findTags(findFilter, tagFilter, useRandom = false)
                                }

                                DataType.IMAGE -> {
                                    val imageFilter =
                                        filterParser.convertImageObjectFilter(objectFilter)
                                    queryEngine.findImages(
                                        findFilter,
                                        imageFilter,
                                        useRandom = false,
                                    )
                                }

                                DataType.GALLERY -> {
                                    val galleryFilter =
                                        filterParser.convertGalleryObjectFilter(objectFilter)
                                    queryEngine.findGalleries(
                                        findFilter,
                                        galleryFilter,
                                        useRandom = false,
                                    )
                                }

                                DataType.MOVIE -> {
                                    val movieFilter =
                                        filterParser.convertMovieObjectFilter(objectFilter)
                                    queryEngine.findMovies(
                                        findFilter,
                                        movieFilter,
                                        useRandom = false,
                                    )
                                }

                                DataType.MARKER -> {
                                    val markerFilter =
                                        filterParser.convertMarkerObjectFilter(objectFilter)
                                    queryEngine.findMarkers(
                                        findFilter,
                                        markerFilter,
                                        useRandom = false,
                                    )
                                }
                            }
                        FrontPageRow(result.name, filter, data)
                    } else {
                        Log.w(TAG, "SavedFilter does not exist")
                        FrontPageRow.ERROR
                    }
                } catch (ex: Exception) {
                    Log.e(TAG, "Exception in addSavedFilterRow filterId=$filterId", ex)
                    FrontPageRow.ERROR
                }
            }
        }
}
