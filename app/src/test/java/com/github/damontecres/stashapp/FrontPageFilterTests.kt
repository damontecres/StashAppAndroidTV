package com.github.damontecres.stashapp

import com.apollographql.apollo3.api.json.BufferedSourceJsonReader
import com.apollographql.apollo3.api.parseJsonResponse
import com.github.damontecres.stashapp.api.ConfigurationUIQuery
import com.github.damontecres.stashapp.api.fragment.SavedFilterData
import com.github.damontecres.stashapp.api.type.FilterMode
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.StashCustomFilter
import com.github.damontecres.stashapp.data.StashSavedFilter
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
import org.mockito.kotlin.mock

@RunWith(MockitoJUnitRunner::class)
class FrontPageFilterTests {
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
    ): SavedFilterData {
        return SavedFilterData(id, mode, "savedfilter-$id", null, null, null, "SavedFilterData")
    }

    @Test
    fun basicTest() {
        val mockedQueryEngine =
            mock<QueryEngine> {
                onBlocking { getSavedFilter("1") } doReturn createSavedFilterData("1", FilterMode.SCENES)
                onBlocking { getSavedFilter("2") } doReturn createSavedFilterData("2", FilterMode.PERFORMERS)
                onBlocking { findScenes(anyOrNull(), anyOrNull(), any()) } doReturn listOf()
                onBlocking { findPerformers(anyOrNull(), anyOrNull(), anyOrNull(), any()) } doReturn listOf()
            }
        val frontPageParser = FrontPageParser(mockedQueryEngine, FilterParser(Version.V0_25_0))
        val result = runBlocking { frontPageParser.parse(parseFileToFrontPageContent("front_page_basic.json")) }
        val rows = result.map { runBlocking { it.await() } }
        Assert.assertEquals(4, rows.size)
        rows.forEach { Assert.assertEquals(FrontPageParser.FrontPageRowResult.SUCCESS, it.result) }
        Assert.assertEquals(DataType.SCENE, rows[0].data!!.filter.dataType)
        Assert.assertTrue(rows[0].data!!.filter is StashSavedFilter)
        Assert.assertEquals(DataType.SCENE, rows[1].data!!.filter.dataType)
        Assert.assertTrue(rows[1].data!!.filter is StashCustomFilter)
        Assert.assertEquals(DataType.PERFORMER, rows[2].data!!.filter.dataType)
        Assert.assertTrue(rows[2].data!!.filter is StashCustomFilter)
        Assert.assertEquals(DataType.PERFORMER, rows[3].data!!.filter.dataType)
        Assert.assertTrue(rows[3].data!!.filter is StashSavedFilter)
    }

    @Test
    fun unsupportedTest() {
        val mockedQueryEngine = mock<QueryEngine>()
        val frontPageParser = FrontPageParser(mockedQueryEngine, FilterParser(Version.V0_25_0))
        val result = runBlocking { frontPageParser.parse(parseFileToFrontPageContent("front_page_unsupported.json")) }
        val rows = result.map { runBlocking { it.await() } }
        Assert.assertEquals(2, rows.size)
        Assert.assertEquals(FrontPageParser.FrontPageRowResult.DATA_TYPE_NOT_SUPPORTED, rows[0].result)
        Assert.assertEquals(FrontPageParser.FrontPageRowResult.DATA_TYPE_NOT_SUPPORTED, rows[1].result)
    }
}
