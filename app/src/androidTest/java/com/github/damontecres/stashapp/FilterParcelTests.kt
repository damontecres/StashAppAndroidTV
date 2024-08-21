package com.github.damontecres.stashapp

import android.os.Parcel
import android.os.Parcelable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CircumcisionCriterionInput
import com.github.damontecres.stashapp.api.type.CircumisedEnum
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
import com.github.damontecres.stashapp.api.type.FloatCriterionInput
import com.github.damontecres.stashapp.api.type.GenderCriterionInput
import com.github.damontecres.stashapp.api.type.GenderEnum
import com.github.damontecres.stashapp.api.type.HierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.api.type.IntCriterionInput
import com.github.damontecres.stashapp.api.type.MultiCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationCriterionInput
import com.github.damontecres.stashapp.api.type.OrientationEnum
import com.github.damontecres.stashapp.api.type.PHashDuplicationCriterionInput
import com.github.damontecres.stashapp.api.type.PhashDistanceCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionCriterionInput
import com.github.damontecres.stashapp.api.type.ResolutionEnum
import com.github.damontecres.stashapp.api.type.SceneFilterType
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput
import com.github.damontecres.stashapp.data.SceneFilterTypeHolder
import com.github.damontecres.stashapp.data.readCircumcisionCriterionInput
import com.github.damontecres.stashapp.data.readDateCriterionInput
import com.github.damontecres.stashapp.data.readFloatCriterionInput
import com.github.damontecres.stashapp.data.readGenderCriterionInput
import com.github.damontecres.stashapp.data.readHierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.data.readIntCriterionInput
import com.github.damontecres.stashapp.data.readList
import com.github.damontecres.stashapp.data.readMultiCriterionInput
import com.github.damontecres.stashapp.data.readOptionalBoolean
import com.github.damontecres.stashapp.data.readOptionalInt
import com.github.damontecres.stashapp.data.readOptionalList
import com.github.damontecres.stashapp.data.readOrientationCriterionInput
import com.github.damontecres.stashapp.data.readPHashDuplicationCriterionInput
import com.github.damontecres.stashapp.data.readPhashDistanceCriterionInput
import com.github.damontecres.stashapp.data.readResolutionCriterionInput
import com.github.damontecres.stashapp.data.readStashIDCriterionInput
import com.github.damontecres.stashapp.data.readStringCriterionInput
import com.github.damontecres.stashapp.data.readTimestampCriterionInput
import com.github.damontecres.stashapp.data.writeCircumcisionCriterionInput
import com.github.damontecres.stashapp.data.writeDateCriterionInput
import com.github.damontecres.stashapp.data.writeFloatCriterionInput
import com.github.damontecres.stashapp.data.writeGenderCriterionInput
import com.github.damontecres.stashapp.data.writeHierarchicalMultiCriterionInput
import com.github.damontecres.stashapp.data.writeIntCriterionInput
import com.github.damontecres.stashapp.data.writeList
import com.github.damontecres.stashapp.data.writeMultiCriterionInput
import com.github.damontecres.stashapp.data.writeOptionalBoolean
import com.github.damontecres.stashapp.data.writeOptionalInt
import com.github.damontecres.stashapp.data.writeOrientationCriterionInput
import com.github.damontecres.stashapp.data.writePHashDuplicationCriterionInput
import com.github.damontecres.stashapp.data.writePhashDistanceCriterionInput
import com.github.damontecres.stashapp.data.writeResolutionCriterionInput
import com.github.damontecres.stashapp.data.writeStashIDCriterionInput
import com.github.damontecres.stashapp.data.writeStringCriterionInput
import com.github.damontecres.stashapp.data.writeTimestampCriterionInput
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@SmallTest
class FilterParcelTests {
    companion object {
        fun <T : Parcelable> parcelizeAndDeparcelize(obj: T): T? {
            val parcel = Parcel.obtain()
            parcel.writeParcelable(obj, 0)
            parcel.setDataPosition(0)
            return parcel.readParcelable(obj.javaClass.classLoader)
        }

        fun <T> parcelAssertEqual(
            sourceObj: T,
            writer: Parcel.(T) -> Unit,
            reader: Parcel.() -> T?,
        ): T? {
            val parcel = Parcel.obtain()
            writer.invoke(parcel, sourceObj)
            parcel.setDataPosition(0)
            val result = reader.invoke(parcel)
            Assert.assertEquals(sourceObj, result)
            return result
        }

        fun <T> parcelAssertNull(
            writer: Parcel.(T?) -> Unit,
            reader: Parcel.() -> T?,
        ) {
            val parcel = Parcel.obtain()
            writer.invoke(parcel, null)
            parcel.setDataPosition(0)
            val result = reader.invoke(parcel)
            Assert.assertNull(result)
        }
    }

    @Test
    fun testList() {
        parcelAssertEqual(
            listOf("test", "test2"),
            writer = { writeList(this, it) { s -> this.writeString(s) } },
            reader = { readList(this) { this.readString()!! } },
        )
        parcelAssertEqual(
            listOf<String>(),
            writer = { writeList(this, it) { s -> this.writeString(s) } },
            reader = { readList(this) { this.readString()!! } },
        )

        parcelAssertEqual(
            Optional.present(listOf("test", "test2")),
            writer = { writeList(this, it) { s -> this.writeString(s) } },
            reader = { readOptionalList(this) { this.readString()!! } },
        )
        parcelAssertEqual(
            Optional.absent<List<String>>(),
            writer = { writeList(this, it) { s -> this.writeString(s) } },
            reader = { readOptionalList(this) { this.readString()!! } },
        )
    }

    @Test
    fun testOptionalInt() {
        parcelAssertEqual(
            Optional.present(1),
            writer = { writeOptionalInt(this, it) },
            reader = { readOptionalInt(this) },
        )
        parcelAssertEqual(
            Optional.present(-1234567),
            writer = { writeOptionalInt(this, it) },
            reader = { readOptionalInt(this) },
        )

        parcelAssertEqual(
            Optional.absent<Int>(),
            writer = { writeOptionalInt(this, it) },
            reader = { readOptionalInt(this) },
        )
    }

    @Test
    fun testOptionalBoolean() {
        parcelAssertEqual(
            Optional.present(true),
            writer = { writeOptionalBoolean(this, it) },
            reader = { readOptionalBoolean(this) },
        )
        parcelAssertEqual(
            Optional.present(false),
            writer = { writeOptionalBoolean(this, it) },
            reader = { readOptionalBoolean(this) },
        )

        parcelAssertEqual(
            Optional.absent(),
            writer = { writeOptionalBoolean(this, it) },
            reader = { readOptionalBoolean(this) },
        )
    }

    @Test
    fun testIntCriterionInput() {
        parcelAssertEqual(
            IntCriterionInput(10, Optional.present(55), CriterionModifier.BETWEEN),
            writer = { writeIntCriterionInput(this, it) },
            reader = { readIntCriterionInput(this) },
        )
        parcelAssertEqual(
            IntCriterionInput(10, Optional.absent(), CriterionModifier.EQUALS),
            writer = { writeIntCriterionInput(this, it) },
            reader = { readIntCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeIntCriterionInput(this, it) },
            reader = { readIntCriterionInput(this) },
        )
    }

    @Test
    fun testFloatCriterionInput() {
        parcelAssertEqual(
            FloatCriterionInput(10.0, Optional.present(55.0), CriterionModifier.BETWEEN),
            writer = { writeFloatCriterionInput(this, it) },
            reader = { readFloatCriterionInput(this) },
        )
        parcelAssertEqual(
            FloatCriterionInput(-10.0, Optional.absent(), CriterionModifier.EQUALS),
            writer = { writeFloatCriterionInput(this, it) },
            reader = { readFloatCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeFloatCriterionInput(this, it) },
            reader = { readFloatCriterionInput(this) },
        )
    }

    @Test
    fun testStringCriterionInput() {
        parcelAssertEqual(
            StringCriterionInput("test", CriterionModifier.BETWEEN),
            writer = { writeStringCriterionInput(this, it) },
            reader = { readStringCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeStringCriterionInput(this, it) },
            reader = { readStringCriterionInput(this) },
        )
    }

    @Test
    fun testPhashDistanceCriterionInput() {
        parcelAssertEqual(
            PhashDistanceCriterionInput("phash", CriterionModifier.IS_NULL, Optional.absent()),
            writer = { writePhashDistanceCriterionInput(this, it) },
            reader = { readPhashDistanceCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writePhashDistanceCriterionInput(this, it) },
            reader = { readPhashDistanceCriterionInput(this) },
        )
    }

    @Test
    fun testPHashDuplicationCriterionInput() {
        parcelAssertEqual(
            PHashDuplicationCriterionInput(Optional.present(false), Optional.absent()),
            writer = { writePHashDuplicationCriterionInput(this, it) },
            reader = { readPHashDuplicationCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writePHashDuplicationCriterionInput(this, it) },
            reader = { readPHashDuplicationCriterionInput(this) },
        )
    }

    @Test
    fun testResolutionCriterionInput() {
        parcelAssertEqual(
            ResolutionCriterionInput(ResolutionEnum.FOUR_K, CriterionModifier.EXCLUDES),
            writer = { writeResolutionCriterionInput(this, it) },
            reader = { readResolutionCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeResolutionCriterionInput(this, it) },
            reader = { readResolutionCriterionInput(this) },
        )
    }

    @Test
    fun testOrientationCriterionInput() {
        parcelAssertEqual(
            OrientationCriterionInput(listOf(OrientationEnum.PORTRAIT, OrientationEnum.SQUARE)),
            writer = { writeOrientationCriterionInput(this, it) },
            reader = { readOrientationCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeOrientationCriterionInput(this, it) },
            reader = { readOrientationCriterionInput(this) },
        )
    }

    fun testHierarchicalMultiCriterionInput() {
        parcelAssertEqual(
            HierarchicalMultiCriterionInput(
                Optional.present(listOf("test")),
                CriterionModifier.INCLUDES_ALL,
                Optional.present(-1),
                Optional.absent(),
            ),
            writer = { writeHierarchicalMultiCriterionInput(this, it) },
            reader = { readHierarchicalMultiCriterionInput(this) },
        )
        parcelAssertEqual(
            HierarchicalMultiCriterionInput(
                Optional.present(listOf("test")),
                CriterionModifier.EXCLUDES,
                Optional.present(-1),
                Optional.present(listOf("more", "strings")),
            ),
            writer = { writeHierarchicalMultiCriterionInput(this, it) },
            reader = { readHierarchicalMultiCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeHierarchicalMultiCriterionInput(this, it) },
            reader = { readHierarchicalMultiCriterionInput(this) },
        )
    }

    fun testMultiCriterionInput() {
        parcelAssertEqual(
            MultiCriterionInput(
                Optional.present(listOf("test")),
                CriterionModifier.INCLUDES_ALL,
                Optional.absent(),
            ),
            writer = { writeMultiCriterionInput(this, it) },
            reader = { readMultiCriterionInput(this) },
        )
        parcelAssertEqual(
            MultiCriterionInput(
                Optional.present(listOf("test")),
                CriterionModifier.EXCLUDES,
                Optional.present(listOf("more", "strings")),
            ),
            writer = { writeMultiCriterionInput(this, it) },
            reader = { readMultiCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeMultiCriterionInput(this, it) },
            reader = { readMultiCriterionInput(this) },
        )
    }

    @Test
    fun testStashIDCriterionInput() {
        parcelAssertEqual(
            StashIDCriterionInput(
                Optional.present("123"),
                Optional.present("456"),
                CriterionModifier.IS_NULL,
            ),
            writer = { writeStashIDCriterionInput(this, it) },
            reader = { readStashIDCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeStashIDCriterionInput(this, it) },
            reader = { readStashIDCriterionInput(this) },
        )
    }

    @Test
    fun testTimestampCriterionInput() {
        parcelAssertEqual(
            TimestampCriterionInput(
                "2024-01-01",
                Optional.present("2024-12-31"),
                CriterionModifier.NOT_BETWEEN,
            ),
            writer = { writeTimestampCriterionInput(this, it) },
            reader = { readTimestampCriterionInput(this) },
        )
        parcelAssertEqual(
            TimestampCriterionInput("2024-01-01", Optional.absent(), CriterionModifier.NOT_BETWEEN),
            writer = { writeTimestampCriterionInput(this, it) },
            reader = { readTimestampCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeTimestampCriterionInput(this, it) },
            reader = { readTimestampCriterionInput(this) },
        )
    }

    @Test
    fun testDateCriterionInput() {
        parcelAssertEqual(
            DateCriterionInput(
                "2024-01-01",
                Optional.present("2024-12-31"),
                CriterionModifier.NOT_BETWEEN,
            ),
            writer = { writeDateCriterionInput(this, it) },
            reader = { readDateCriterionInput(this) },
        )
        parcelAssertEqual(
            DateCriterionInput("2024-01-01", Optional.absent(), CriterionModifier.NOT_BETWEEN),
            writer = { writeDateCriterionInput(this, it) },
            reader = { readDateCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeDateCriterionInput(this, it) },
            reader = { readDateCriterionInput(this) },
        )
    }

    @Test
    fun testCircumcisionCriterionInput() {
        parcelAssertEqual(
            CircumcisionCriterionInput(
                Optional.present(listOf(CircumisedEnum.CUT)),
                CriterionModifier.EQUALS,
            ),
            writer = { writeCircumcisionCriterionInput(this, it) },
            reader = { readCircumcisionCriterionInput(this) },
        )
        parcelAssertEqual(
            CircumcisionCriterionInput(
                Optional.absent(),
                CriterionModifier.EQUALS,
            ),
            writer = { writeCircumcisionCriterionInput(this, it) },
            reader = { readCircumcisionCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeCircumcisionCriterionInput(this, it) },
            reader = { readCircumcisionCriterionInput(this) },
        )
    }

    @Test
    fun testGenderCriterionInput() {
        parcelAssertEqual(
            GenderCriterionInput(
                Optional.present(GenderEnum.FEMALE),
                Optional.present(listOf(GenderEnum.MALE)),
                CriterionModifier.INCLUDES_ALL,
            ),
            writer = { writeGenderCriterionInput(this, it) },
            reader = { readGenderCriterionInput(this) },
        )
        parcelAssertEqual(
            GenderCriterionInput(
                Optional.absent(),
                Optional.present(listOf(GenderEnum.MALE)),
                CriterionModifier.INCLUDES_ALL,
            ),
            writer = { writeGenderCriterionInput(this, it) },
            reader = { readGenderCriterionInput(this) },
        )
        parcelAssertNull(
            writer = { writeGenderCriterionInput(this, it) },
            reader = { readGenderCriterionInput(this) },
        )
    }

    @Test
    fun testSceneInt() {
        val filter =
            SceneFilterType(file_count = Optional.present(IntCriterionInput(100, Optional.absent(), CriterionModifier.INCLUDES_ALL)))
        val holder = SceneFilterTypeHolder(filter)
        val result = parcelizeAndDeparcelize(holder)
        Assert.assertNotNull(result)
        result!!
        Assert.assertNotNull(result.value)
        Assert.assertEquals(filter.file_count.getOrNull(), result.value?.file_count?.getOrNull())
        Assert.assertEquals(filter, result.value)
    }

    @Test
    fun testScene() {
        val filter =
            SceneFilterType(
                AND =
                    Optional.present(
                        SceneFilterType(
                            file_count =
                                Optional.present(
                                    IntCriterionInput(
                                        100,
                                        Optional.absent(),
                                        CriterionModifier.INCLUDES_ALL,
                                    ),
                                ),
                        ),
                    ),
                title = Optional.present(StringCriterionInput("title", CriterionModifier.BETWEEN)),
                file_count =
                    Optional.present(
                        IntCriterionInput(
                            100,
                            Optional.absent(),
                            CriterionModifier.INCLUDES_ALL,
                        ),
                    ),
                phash_distance =
                    Optional.present(
                        PhashDistanceCriterionInput(
                            "distance",
                            CriterionModifier.IS_NULL,
                        ),
                    ),
                organized = Optional.present(true),
                resolution =
                    Optional.present(
                        ResolutionCriterionInput(
                            ResolutionEnum.HUGE,
                            CriterionModifier.NOT_EQUALS,
                        ),
                    ),
                orientation =
                    Optional.present(
                        OrientationCriterionInput(
                            listOf(
                                OrientationEnum.SQUARE,
                                OrientationEnum.PORTRAIT,
                            ),
                        ),
                    ),
                studios =
                    Optional.present(
                        HierarchicalMultiCriterionInput(
                            Optional.present(listOf("123")),
                            CriterionModifier.INCLUDES_ALL,
                            Optional.present(-1),
                            Optional.absent(),
                        ),
                    ),
                movies =
                    Optional.present(
                        MultiCriterionInput(
                            Optional.present(listOf("123")),
                            CriterionModifier.INCLUDES_ALL,
                            Optional.present(listOf("456")),
                        ),
                    ),
                date =
                    Optional.present(
                        DateCriterionInput(
                            "2024-01-01",
                            Optional.present("2024-01-02"),
                            CriterionModifier.EXCLUDES,
                        ),
                    ),
                created_at =
                    Optional.present(
                        TimestampCriterionInput(
                            "2024-01-01",
                            Optional.absent(),
                            CriterionModifier.NOT_MATCHES_REGEX,
                        ),
                    ),
            )
        val holder = SceneFilterTypeHolder(filter)
        val result = parcelizeAndDeparcelize(holder)
        Assert.assertNotNull(result)
        result!!
        Assert.assertNotNull(result.value)
        Assert.assertEquals(filter, result.value)
    }
}
