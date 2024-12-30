package com.github.damontecres.stashapp

import android.content.Context
import com.apollographql.apollo.api.json.BufferedSourceJsonReader
import com.apollographql.apollo.api.parseJsonResponse
import com.github.damontecres.stashapp.api.ConfigurationUIQuery
import com.github.damontecres.stashapp.api.fragment.PerformerData
import com.github.damontecres.stashapp.api.fragment.SavedFilter
import com.github.damontecres.stashapp.api.fragment.SlimSceneData
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.util.FilterParser
import com.github.damontecres.stashapp.util.FrontPageParser
import com.github.damontecres.stashapp.util.QueryEngine
import com.github.damontecres.stashapp.util.Version
import com.github.damontecres.stashapp.util.getCaseInsensitive
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class FrontPageFilterTests {
    private val mockedContext =
        mock<Context> {
            on { getString(any(), any()) } doReturn "context_string"
        }

    private fun parseFileToFrontPageContent(file: String): List<Map<String, *>> {
        val path = file.toPath()
        FileSystem.RESOURCES.read(path) {
            val jsonReader = BufferedSourceJsonReader(this)
            val response = ConfigurationUIQuery().parseJsonResponse(jsonReader)
            val ui = response.data!!.configuration.ui as Map<String, *>
            return ui.getCaseInsensitive("frontPageContent") as List<Map<String, *>>
        }
    }

    private fun createSavedFilterData(
        id: String,
        mode: FilterMode,
    ): SavedFilter = SavedFilter(id, mode, "savedfilter-$id", null, null, null, "SavedFilterData")

    @Test
    fun basicTest() {
        val mockedQueryEngine =
            mock<QueryEngine> {
                onBlocking { getSavedFilter("1") } doReturn
                    createSavedFilterData(
                        "1",
                        FilterMode.SCENES,
                    )
                onBlocking { getSavedFilter("2") } doReturn
                    createSavedFilterData(
                        "2",
                        FilterMode.PERFORMERS,
                    )
                onBlocking {
                    findScenes(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        any(),
                    )
                } doReturn listOf()
                onBlocking {
                    findPerformers(
                        anyOrNull(),
                        anyOrNull(),
                        anyOrNull(),
                        any(),
                    )
                } doReturn listOf()
                onBlocking {
                    find(
                        eq(DataType.SCENE),
                        anyOrNull(),
                        any(),
                    )
                } doReturn listOf<SlimSceneData>()
                onBlocking {
                    find(
                        eq(DataType.PERFORMER),
                        anyOrNull(),
                        any(),
                    )
                } doReturn listOf<PerformerData>()
            }
        val frontPageParser =
            FrontPageParser(
                mockedContext,
                mockedQueryEngine,
                FilterParser(Version.MINIMUM_STASH_VERSION),
            )
        val result =
            runBlocking { frontPageParser.parse(parseFileToFrontPageContent("front_page_basic.json")) }
        val rows = result.map { runBlocking { it.await() } }
        Assert.assertEquals(4, rows.size)
        rows.forEach { Assert.assertTrue(it is FrontPageParser.FrontPageRow.Success) }
        rows as List<FrontPageParser.FrontPageRow.Success>
        Assert.assertEquals(DataType.SCENE, rows[0].filter.dataType)
        Assert.assertEquals(DataType.SCENE, rows[1].filter.dataType)
        Assert.assertEquals(DataType.PERFORMER, rows[2].filter.dataType)
        Assert.assertEquals(DataType.PERFORMER, rows[3].filter.dataType)
    }

    @Test
    fun unsupportedTest() {
        val mockedQueryEngine = mock<QueryEngine>()
        val frontPageParser =
            FrontPageParser(
                mockedContext,
                mockedQueryEngine,
                FilterParser(Version.MINIMUM_STASH_VERSION),
            )
        val result = runBlocking { frontPageParser.parse(parseFileToFrontPageContent("front_page_unsupported.json")) }
        val rows = result.map { runBlocking { it.await() } }
        Assert.assertEquals(2, rows.size)
        Assert.assertTrue(rows[0] is FrontPageParser.FrontPageRow.NotSupported)
        Assert.assertTrue(rows[1] is FrontPageParser.FrontPageRow.NotSupported)
    }
}
