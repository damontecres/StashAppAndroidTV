package com.github.damontecres.stashapp.util

import android.util.Log
import com.apollographql.apollo.api.Optional
import com.chrynan.parcelable.core.Parcelable
import com.github.damontecres.stashapp.api.type.CriterionModifier
import com.github.damontecres.stashapp.api.type.CustomFieldCriterionInput
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule

val OptionalSerializersModule =
    SerializersModule {
        contextual(Optional::class) { args -> OptionalSerializer(args[0]) }
        contextual(CustomFieldCriterionInput::class) { _ -> CustomFieldCriterionInputSerializer() }
    }

/**
 * Contextual [Parcelable] with [OptionalSerializer]
 */
@OptIn(ExperimentalSerializationApi::class)
val StashParcelable =
    Parcelable {
        serializersModule = OptionalSerializersModule
    }

val StashJson =
    Json {
        serializersModule = OptionalSerializersModule
        classDiscriminator = "_type"
    }

/**
 * Serializes [Optional]s
 *
 * Basically just writes a boolean for whether the [Optional] is present or absent before the value
 */
@OptIn(ExperimentalSerializationApi::class)
class OptionalSerializer<T>(
    private val dataSerializer: KSerializer<T>,
) : KSerializer<Optional<T>> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(
            Optional::class.qualifiedName!!,
            dataSerializer.descriptor,
        ) {
            element<Boolean>("exists")
            element("value", dataSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): Optional<T> =
        decoder.decodeStructure(descriptor) {
            var exists: Boolean = false
            var value: T? = null
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> exists = decodeBooleanElement(descriptor, 0)
                    1 -> value = decodeNullableSerializableElement(descriptor, 1, dataSerializer)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> error("Unexpected index: $index")
                }
            }
            if (exists) {
                Optional.present(value!!)
            } else {
                Optional.Absent
            }
        }

    override fun serialize(
        encoder: Encoder,
        value: Optional<T>,
    ) {
        encoder.encodeStructure(descriptor) {
            if (value == Optional.Absent) {
                encodeBooleanElement(descriptor, 0, false)
                encodeNullableSerializableElement(descriptor, 1, dataSerializer, null)
            } else {
                encodeBooleanElement(descriptor, 0, true)
                encodeSerializableElement(descriptor, 1, dataSerializer, value.getOrNull()!!)
            }
        }
    }
}

@OptIn(ExperimentalSerializationApi::class)
class CustomFieldCriterionInputSerializer : KSerializer<CustomFieldCriterionInput?> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor(CustomFieldCriterionInput::class.qualifiedName!!) {
            element<String>("field")
            element<CriterionModifier>("modifier")
            // TODO
            // element<List<Any>?>("value")
        }

    override fun deserialize(decoder: Decoder): CustomFieldCriterionInput? =
        if (decoder.decodeNotNullMark()) {
            val field = decoder.decodeString()
            val modifier = CriterionModifier.entries[decoder.decodeInt()]
            val value =
                if (decoder.decodeBoolean()) {
                    val size = decoder.decodeInt()
                    val list =
                        buildList {
                            for (i in 0..<size) {
                                val type = decoder.decodeInt()
                                val itemValue =
                                    when (type) {
                                        STRING_TYPE -> {
                                            decoder.decodeString()
                                        }

                                        INT_TYPE -> {
                                            decoder.decodeInt()
                                        }

                                        DOUBLE_TYPE -> {
                                            decoder.decodeDouble()
                                        }

                                        FLOAT_TYPE -> {
                                            decoder.decodeFloat()
                                        }

                                        else -> {
                                            val msg = "Unknown decoding type: $type"
                                            Log.e(TAG, msg)
                                            throw UnsupportedOperationException(msg)
                                        }
                                    }
                                add(itemValue)
                            }
                        }
                    list
                } else {
                    null
                }
            CustomFieldCriterionInput(
                field = field,
                modifier = modifier,
                value = Optional.presentIfNotNull(value),
            )
        } else {
            decoder.decodeNull()
        }

    override fun serialize(
        encoder: Encoder,
        value: CustomFieldCriterionInput?,
    ) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            encoder.encodeNotNullMark()
            encoder.encodeString(value.field)
            encoder.encodeInt(value.modifier.ordinal)
            if (value.value.getOrNull() == null) {
                encoder.encodeBoolean(false)
            } else {
                encoder.encodeBoolean(true)
                encoder.encodeInt(value.value.getOrNull()!!.size)
                value.value.getOrNull()!!.forEach {
                    when (it) {
                        is String -> {
                            encoder.encodeInt(STRING_TYPE)
                            encoder.encodeString(it)
                        }

                        is Int -> {
                            encoder.encodeInt(INT_TYPE)
                            encoder.encodeInt(it)
                        }

                        is Double -> {
                            encoder.encodeInt(DOUBLE_TYPE)
                            encoder.encodeDouble(it)
                        }

                        is Float -> {
                            encoder.encodeInt(FLOAT_TYPE)
                            encoder.encodeFloat(it)
                        }

                        else -> {
                            val msg = "Unknown custom field type: ${it::class}"
                            Log.e(TAG, msg)
                            throw UnsupportedOperationException(msg)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "CustomFieldCriterionInputSerializer"

        private const val STRING_TYPE = 0
        private const val INT_TYPE = 1
        private const val DOUBLE_TYPE = 2
        private const val FLOAT_TYPE = 3
    }
}
