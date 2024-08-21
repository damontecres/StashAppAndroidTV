package com.github.damontecres.stashapp.data

import android.os.Parcel
import android.os.Parcelable
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
import com.github.damontecres.stashapp.api.type.StashIDCriterionInput
import com.github.damontecres.stashapp.api.type.StringCriterionInput
import com.github.damontecres.stashapp.api.type.TimestampCriterionInput

// This file contains the parcelize implementation for "primitive" object filter types (int, float, gender, etc)

/**
 * Represents that the next object is null
 */
const val ABSENT = 0.toByte()

/**
 * Represents that the next object is not null
 */
const val PRESENT = 1.toByte()

/**
 * The generated FilterType objects cannot be directly parcelized and must be held in another object.
 *
 * This interface is the parent for those other objects.
 *
 * See [createFilterHolder]
 */
interface FilterHolder<T> : Parcelable {
    val value: T?
}

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
): Optional<List<T>> {
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

fun readOptionalInt(parcel: Parcel): Optional<Int> {
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

fun readOptionalBoolean(parcel: Parcel): Optional<Boolean> {
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

fun writeFloatCriterionInput(
    parcel: Parcel,
    value: FloatCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        parcel.writeDouble(value.value)
        if (value.value2 is Optional.Present) {
            parcel.writeByte(PRESENT)
            parcel.writeDouble(value.value2.value!!)
        } else {
            parcel.writeByte(ABSENT)
        }
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readFloatCriterionInput(parcel: Parcel): FloatCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        val value = parcel.readDouble()
        val value2 =
            if (parcel.readByte() == PRESENT) {
                parcel.readDouble()
            } else {
                null
            }
        val modifier = CriterionModifier.entries[parcel.readInt()]
        return FloatCriterionInput(value, Optional.presentIfNotNull(value2), modifier)
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

fun writeCircumcisionCriterionInput(
    parcel: Parcel,
    value: CircumcisionCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        writeList(parcel, value.value) {
            parcel.writeInt(it.ordinal)
        }
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readCircumcisionCriterionInput(parcel: Parcel): CircumcisionCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        return CircumcisionCriterionInput(
            value =
                readOptionalList(parcel) {
                    CircumisedEnum.entries[it.readInt()]
                },
            modifier = CriterionModifier.entries[parcel.readInt()],
        )
    } else {
        return null
    }
}

fun writeGenderCriterionInput(
    parcel: Parcel,
    value: GenderCriterionInput?,
) {
    if (value == null) {
        parcel.writeByte(ABSENT)
    } else {
        parcel.writeByte(PRESENT)
        if (value.value.getOrNull() == null) {
            parcel.writeByte(ABSENT)
        } else {
            parcel.writeByte(PRESENT)
            parcel.writeInt(value.value.getOrNull()!!.ordinal)
        }
        writeList(parcel, value.value_list) {
            parcel.writeInt(it.ordinal)
        }
        parcel.writeInt(value.modifier.ordinal)
    }
}

fun readGenderCriterionInput(parcel: Parcel): GenderCriterionInput? {
    if (parcel.readByte() == PRESENT) {
        val valuePresent = parcel.readByte() == PRESENT
        return GenderCriterionInput(
            value =
                if (valuePresent) {
                    Optional.present(GenderEnum.entries[parcel.readInt()])
                } else {
                    Optional.absent()
                },
            value_list =
                readOptionalList(parcel) {
                    GenderEnum.entries[it.readInt()]
                },
            modifier = CriterionModifier.entries[parcel.readInt()],
        )
    } else {
        return null
    }
}
