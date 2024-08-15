package com.github.damontecres.stashapp.data

import android.os.Parcel
import android.os.Parcelable
import com.apollographql.apollo3.api.Optional
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.DateCriterionInput
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
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler

private const val ABSENT = 0.toByte()
private const val PRESENT = 1.toByte()

interface FilterHolder<T> : Parcelable {
    val value: T?
}

@Parcelize
@TypeParceler<SceneFilterType?, SceneFilterTypeParceler>()
data class SceneFilterTypeHolder(override val value: SceneFilterType?) :
    FilterHolder<SceneFilterType>

fun <T> writeList(
    parcel: Parcel,
    list: Optional<List<T>?>,
    addFun: (T) -> Unit,
) {
    if (list.getOrNull() != null) {
        parcel.writeByte(PRESENT)
        writeList(parcel, list.getOrNull()!!, addFun)
    } else {
        parcel.writeByte(ABSENT)
    }
}

fun <T> writeList(
    parcel: Parcel,
    list: List<T>,
    addFun: (T) -> Unit,
) {
    parcel.writeInt(list.size)
    list.forEach(addFun)
}

fun <T> readList(
    parcel: Parcel,
    mapFun: (Parcel) -> T,
): List<T> {
    return buildList {
        0.rangeUntil(parcel.readInt()).forEach { _ ->
            add(mapFun(parcel))
        }
    }
}

fun <T> readOptionalList(
    parcel: Parcel,
    mapFun: (Parcel) -> T,
): Optional<List<T>?> {
    if (parcel.readByte() == PRESENT) {
        return Optional.present(readList(parcel, mapFun))
    } else {
        return Optional.absent()
    }
}

fun writeString(
    parcel: Parcel,
    value: String?,
) {
    parcel.writeString(value)
}

fun readString(parcel: Parcel): String? {
    return parcel.readString()
}

fun writeOptionalInt(
    parcel: Parcel,
    op: Optional<Int?>,
) {
    if (op.getOrNull() != null) {
        parcel.writeByte(PRESENT)
        parcel.writeInt(op.getOrNull()!!)
    } else {
        parcel.writeByte(ABSENT)
    }
}

fun readOptionalInt(parcel: Parcel): Optional<Int?> {
    if (parcel.readByte() == PRESENT) {
        return Optional.present(parcel.readInt())
    } else {
        return Optional.absent()
    }
}

fun writeOptionalBoolean(
    parcel: Parcel,
    op: Optional<Boolean?>,
) {
    if (op.getOrNull() != null) {
        parcel.writeByte(PRESENT)
        if (op.getOrNull()!!) {
            parcel.writeByte(PRESENT)
        } else {
            parcel.writeByte(ABSENT)
        }
    } else {
        parcel.writeByte(ABSENT)
    }
}

fun readOptionalBoolean(parcel: Parcel): Optional<Boolean?> {
    if (parcel.readByte() == PRESENT) {
        return Optional.present(parcel.readByte() == PRESENT)
    } else {
        return Optional.absent()
    }
}

fun writeOptionalString(
    parcel: Parcel,
    op: Optional<String?>,
) {
    if (op.getOrNull() != null) {
        parcel.writeByte(PRESENT)
        parcel.writeString(op.getOrNull())
    } else {
        parcel.writeByte(ABSENT)
    }
}

fun readOptionalString(parcel: Parcel): Optional<String?> {
    if (parcel.readByte() == PRESENT) {
        return Optional.present(parcel.readString())
    } else {
        return Optional.absent()
    }
}

fun writeIntCriterionInput(
    parcel: Parcel,
    value: IntCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeInt(value.value)
        if (value.value2 is Optional.Present) {
            parcel.writeByte(PRESENT)
            parcel.writeInt(value.value2.value!!)
        } else {
            parcel.writeByte(ABSENT)
        }
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readIntCriterionInput(parcel: Parcel): IntCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        val value = parcel.readInt()
        val value2 =
            if (parcel.readByte() == PRESENT) {
                parcel.readInt()
            } else {
                null
            }
        val modifier = CriterionModifier.entries[parcel.readInt()]
        return IntCriterionInput(value, Optional.presentIfNotNull(value2), modifier)
    } else {
        return null
    }
}

fun writeStringCriterionInput(
    parcel: Parcel,
    value: StringCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeString(value.value)
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readStringCriterionInput(parcel: Parcel): StringCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        val value = parcel.readString()!!
        val modifier = CriterionModifier.entries[parcel.readInt()]
        return StringCriterionInput(value, modifier)
    } else {
        return null
    }
}

fun writeBoolean(
    parcel: Parcel,
    value: Boolean?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        if (value) {
            parcel.writeByte(PRESENT)
        } else {
            parcel.writeByte(ABSENT)
        }
    }
}

fun readBoolean(parcel: Parcel): Boolean? {
    if (parcel.readByte() == PRESENT) {
        return parcel.readByte() == PRESENT
    } else {
        return null
    }
}

fun writePhashDistanceCriterionInput(
    parcel: Parcel,
    value: PhashDistanceCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeString(value.value)
        parcel.writeInt(value.modifier.ordinal)
        writeOptionalInt(parcel, value.distance)
    }
}

fun readPhashDistanceCriterionInput(parcel: Parcel): PhashDistanceCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        val value = parcel.readString()!!
        val modifier = CriterionModifier.entries[parcel.readInt()]
        val distance = readOptionalInt(parcel)
        return PhashDistanceCriterionInput(value, modifier, distance)
    } else {
        return null
    }
}

fun writePHashDuplicationCriterionInput(
    parcel: Parcel,
    value: PHashDuplicationCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        writeOptionalBoolean(parcel, value.duplicated)
        writeOptionalInt(parcel, value.distance)
    }
}

fun readPHashDuplicationCriterionInput(parcel: Parcel): PHashDuplicationCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return PHashDuplicationCriterionInput(readOptionalBoolean(parcel), readOptionalInt(parcel))
    } else {
        return null
    }
}

fun writeResolutionCriterionInput(
    parcel: Parcel,
    value: ResolutionCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeInt(value.value.ordinal)
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readResolutionCriterionInput(parcel: Parcel): ResolutionCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return ResolutionCriterionInput(
            ResolutionEnum.entries[parcel.readInt()],
            CriterionModifier.entries[parcel.readInt()],
        )
    } else {
        return null
    }
}

fun writeOrientationCriterionInput(
    parcel: Parcel,
    value: OrientationCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        writeList(parcel, value.value) {
            parcel.writeInt(it.ordinal)
        }
    }
}

fun readOrientationCriterionInput(parcel: Parcel): OrientationCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return OrientationCriterionInput(
            readList(parcel) {
                OrientationEnum.entries[it.readInt()]
            },
        )
    } else {
        return null
    }
}

fun writeHierarchicalMultiCriterionInput(
    parcel: Parcel,
    value: HierarchicalMultiCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        writeList(parcel, value.value) {
            parcel.writeString(it)
        }
        parcel.writeInt(value.modifier.ordinal)
        writeOptionalInt(parcel, value.depth)
        writeList(parcel, value.excludes) {
            parcel.writeString(it)
        }
    }
}

fun readHierarchicalMultiCriterionInput(parcel: Parcel): HierarchicalMultiCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return HierarchicalMultiCriterionInput(
            value =
                readOptionalList(parcel) {
                    it.readString()!!
                },
            modifier = CriterionModifier.entries[parcel.readInt()],
            depth = readOptionalInt(parcel),
            excludes =
                readOptionalList(parcel) {
                    it.readString()!!
                },
        )
    } else {
        return null
    }
}

fun writeMultiCriterionInput(
    parcel: Parcel,
    value: MultiCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        writeList(parcel, value.value) {
            parcel.writeString(it)
        }
        parcel.writeInt(value.modifier.ordinal)
        writeList(parcel, value.excludes) {
            parcel.writeString(it)
        }
    }
}

fun readMultiCriterionInput(parcel: Parcel): MultiCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return MultiCriterionInput(
            value =
                readOptionalList(parcel) {
                    it.readString()!!
                },
            modifier = CriterionModifier.entries[parcel.readInt()],
            excludes =
                readOptionalList(parcel) {
                    it.readString()!!
                },
        )
    } else {
        return null
    }
}

fun writeStashIDCriterionInput(
    parcel: Parcel,
    value: StashIDCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        writeOptionalString(parcel, value.endpoint)
        writeOptionalString(parcel, value.stash_id)
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readStashIDCriterionInput(parcel: Parcel): StashIDCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return StashIDCriterionInput(
            endpoint = readOptionalString(parcel),
            stash_id = readOptionalString(parcel),
            modifier = CriterionModifier.entries[parcel.readInt()],
        )
    } else {
        return null
    }
}

fun writeTimestampCriterionInput(
    parcel: Parcel,
    value: TimestampCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeString(value.value)
        writeOptionalString(parcel, value.value2)
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readTimestampCriterionInput(parcel: Parcel): TimestampCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return TimestampCriterionInput(
            value = parcel.readString()!!,
            value2 = readOptionalString(parcel),
            modifier = CriterionModifier.entries[parcel.readInt()],
        )
    } else {
        return null
    }
}

fun writeDateCriterionInput(
    parcel: Parcel,
    value: DateCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeString(value.value)
        writeOptionalString(parcel, value.value2)
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readDateCriterionInput(parcel: Parcel): DateCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return DateCriterionInput(
            value = parcel.readString()!!,
            value2 = readOptionalString(parcel),
            modifier = CriterionModifier.entries[parcel.readInt()],
        )
    } else {
        return null
    }
}

object SceneFilterTypeParceler : Parceler<SceneFilterType?> {
    override fun create(parcel: Parcel): SceneFilterType? {
        // Regex: (public )?val (\w+): Optional<(\w+)\?> = Optional.Absent,?
        // Replace: $2 = Optional.presentIfNotNull(read$3(parcel)),
        if (parcel.readByte() == ABSENT) {
            return null
        } else {
            return SceneFilterType(
                AND = Optional.presentIfNotNull(create(parcel)),
                OR = Optional.presentIfNotNull(create(parcel)),
                NOT = Optional.presentIfNotNull(create(parcel)),
                id = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                title = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                code = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                details = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                director = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                oshash = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                checksum = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                phash = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                phash_distance = Optional.presentIfNotNull(readPhashDistanceCriterionInput(parcel)),
                path = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                file_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                rating100 = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                organized = Optional.presentIfNotNull(readBoolean(parcel)),
                o_counter = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                duplicated = Optional.presentIfNotNull(readPHashDuplicationCriterionInput(parcel)),
                resolution = Optional.presentIfNotNull(readResolutionCriterionInput(parcel)),
                orientation = Optional.presentIfNotNull(readOrientationCriterionInput(parcel)),
                framerate = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                bitrate = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                video_codec = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                audio_codec = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                duration = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                has_markers = Optional.presentIfNotNull(readString(parcel)),
                is_missing = Optional.presentIfNotNull(readString(parcel)),
                studios = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                movies = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                galleries = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                tag_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performer_tags = Optional.presentIfNotNull(readHierarchicalMultiCriterionInput(parcel)),
                performer_favorite = Optional.presentIfNotNull(readBoolean(parcel)),
                performer_age = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                performers = Optional.presentIfNotNull(readMultiCriterionInput(parcel)),
                performer_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                stash_id_endpoint = Optional.presentIfNotNull(readStashIDCriterionInput(parcel)),
                url = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                interactive = Optional.presentIfNotNull(readBoolean(parcel)),
                interactive_speed = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                captions = Optional.presentIfNotNull(readStringCriterionInput(parcel)),
                resume_time = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                play_count = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                play_duration = Optional.presentIfNotNull(readIntCriterionInput(parcel)),
                last_played_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                date = Optional.presentIfNotNull(readDateCriterionInput(parcel)),
                created_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
                updated_at = Optional.presentIfNotNull(readTimestampCriterionInput(parcel)),
            )
        }
    }

    override fun SceneFilterType?.write(
        parcel: Parcel,
        flags: Int,
    ) {
        if (this == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)
            // Regex: (public )?val (\w+): Optional<(\w+)\?> = Optional.Absent,?
            // Replace: write$3(parcel, $2.getOrNull())
            AND.getOrNull().write(parcel, flags)
            OR.getOrNull().write(parcel, flags)
            NOT.getOrNull().write(parcel, flags)
            writeIntCriterionInput(parcel, id.getOrNull())
            writeStringCriterionInput(parcel, title.getOrNull())
            writeStringCriterionInput(parcel, code.getOrNull())
            writeStringCriterionInput(parcel, details.getOrNull())
            writeStringCriterionInput(parcel, director.getOrNull())
            writeStringCriterionInput(parcel, oshash.getOrNull())
            writeStringCriterionInput(parcel, checksum.getOrNull())
            writeStringCriterionInput(parcel, phash.getOrNull())
            writePhashDistanceCriterionInput(parcel, phash_distance.getOrNull())
            writeStringCriterionInput(parcel, path.getOrNull())
            writeIntCriterionInput(parcel, file_count.getOrNull())
            writeIntCriterionInput(parcel, rating100.getOrNull())
            writeBoolean(parcel, organized.getOrNull())
            writeIntCriterionInput(parcel, o_counter.getOrNull())
            writePHashDuplicationCriterionInput(parcel, duplicated.getOrNull())
            writeResolutionCriterionInput(parcel, resolution.getOrNull())
            writeOrientationCriterionInput(parcel, orientation.getOrNull())
            writeIntCriterionInput(parcel, framerate.getOrNull())
            writeIntCriterionInput(parcel, bitrate.getOrNull())
            writeStringCriterionInput(parcel, video_codec.getOrNull())
            writeStringCriterionInput(parcel, audio_codec.getOrNull())
            writeIntCriterionInput(parcel, duration.getOrNull())
            writeString(parcel, has_markers.getOrNull())
            writeString(parcel, is_missing.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, studios.getOrNull())
            writeMultiCriterionInput(parcel, movies.getOrNull())
            writeMultiCriterionInput(parcel, galleries.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, tags.getOrNull())
            writeIntCriterionInput(parcel, tag_count.getOrNull())
            writeHierarchicalMultiCriterionInput(parcel, performer_tags.getOrNull())
            writeBoolean(parcel, performer_favorite.getOrNull())
            writeIntCriterionInput(parcel, performer_age.getOrNull())
            writeMultiCriterionInput(parcel, performers.getOrNull())
            writeIntCriterionInput(parcel, performer_count.getOrNull())
            writeStashIDCriterionInput(parcel, stash_id_endpoint.getOrNull())
            writeStringCriterionInput(parcel, url.getOrNull())
            writeBoolean(parcel, interactive.getOrNull())
            writeIntCriterionInput(parcel, interactive_speed.getOrNull())
            writeStringCriterionInput(parcel, captions.getOrNull())
            writeIntCriterionInput(parcel, resume_time.getOrNull())
            writeIntCriterionInput(parcel, play_count.getOrNull())
            writeIntCriterionInput(parcel, play_duration.getOrNull())
            writeTimestampCriterionInput(parcel, last_played_at.getOrNull())
            writeDateCriterionInput(parcel, date.getOrNull())
            writeTimestampCriterionInput(parcel, created_at.getOrNull())
            writeTimestampCriterionInput(parcel, updated_at.getOrNull())
        }
    }
}
