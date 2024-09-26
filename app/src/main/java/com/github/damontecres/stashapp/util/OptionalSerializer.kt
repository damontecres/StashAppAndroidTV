package com.github.damontecres.stashapp.util

import com.apollographql.apollo.api.Optional
import com.chrynan.parcelable.core.Parcelable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule

val OptionalSerializersModule =
    SerializersModule {
        contextual(Optional::class) { args -> OptionalSerializer(args[0]) }
    }

/**
 * Contextual [Parcelable] with [OptionalSerializer]
 */
@OptIn(ExperimentalSerializationApi::class)
val StashParcelable =
    Parcelable {
        serializersModule = OptionalSerializersModule
    }

/**
 * Serializes [Optional]s
 *
 * Basically just writes a boolean for whether the [Optional] is present or absent before the value
 */
class OptionalSerializer<T>(private val dataSerializer: KSerializer<T>) : KSerializer<Optional<T>> {
    override val descriptor: SerialDescriptor
        get() = dataSerializer.descriptor

    override fun deserialize(decoder: Decoder): Optional<T> {
        return if (decoder.decodeBoolean()) {
            Optional.present(dataSerializer.deserialize(decoder))
        } else {
            Optional.Absent
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Optional<T>,
    ) {
        if (value == Optional.Absent) {
            encoder.encodeBoolean(false)
        } else {
            encoder.encodeBoolean(true)
            dataSerializer.serialize(encoder, value.getOrNull()!!)
        }
    }
}
