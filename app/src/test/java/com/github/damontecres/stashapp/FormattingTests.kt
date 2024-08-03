package com.github.damontecres.stashapp

import com.github.damontecres.stashapp.views.fileNameFromPath
import org.junit.Assert
import org.junit.Test

class FormattingTests {
    @Test
    fun testFileNameFromPath() {
        Assert.assertEquals("test.zip", "/path/to/test.zip".fileNameFromPath)
        Assert.assertEquals("test.zip", "to/test.zip".fileNameFromPath)
        Assert.assertEquals("test.zip", "C:\\\\path\\to\\test.zip".fileNameFromPath)
    }
}
