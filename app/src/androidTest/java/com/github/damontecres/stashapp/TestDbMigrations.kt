package com.github.damontecres.stashapp

import androidx.room.testing.MigrationTestHelper
import androidx.room.util.useCursor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.damontecres.stashapp.data.DataType
import com.github.damontecres.stashapp.data.room.AppDatabase
import com.github.damontecres.stashapp.data.room.MIGRATION_4_TO_5
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class TestDbMigrations {
    private val testDbName = "migration-test"

    private val itemId = "123"
    private val blurValue = 50

    @get:Rule
    val helper: MigrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        helper.createDatabase(testDbName, 4).apply {
            execSQL(
                "INSERT INTO playback_effects VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                arrayOf(
                    "https://server",
                    itemId,
                    90,
                    100,
                    100,
                    100,
                    200,
                    75,
                    100,
                    100,
                    blurValue,
                ),
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(testDbName, 5, true, MIGRATION_4_TO_5)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        db.query("SELECT dataType, id, blur FROM playback_effects").useCursor { c ->
            c.moveToFirst()
            Assert.assertEquals(DataType.SCENE.ordinal, c.getInt(0))
            Assert.assertEquals(itemId, c.getString(1))
            Assert.assertEquals(blurValue, c.getInt(2))
        }
    }
}
