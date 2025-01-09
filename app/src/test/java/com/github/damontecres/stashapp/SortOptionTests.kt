package com.github.damontecres.stashapp

import com.github.damontecres.stashapp.data.SortOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class SortOptionTests {
    private fun test(sortOption: SortOption) {
        val json = Json.encodeToString(sortOption)
        val result = Json.decodeFromString<SortOption>(json)
        Assert.assertEquals(sortOption, result)
        Assert.assertEquals(sortOption::class, result::class)
        Assert.assertEquals(sortOption.key, result.key)
    }

    @Test
    fun testSerialization() {
        test(SortOption.CreatedAt)
        test(SortOption.FileModTime)
        test(SortOption.Title)
    }

    @Test
    fun testSerializationUnknown() {
        test(SortOption.Unknown("new-key"))
        test(SortOption.Unknown("test test test"))
    }
}
