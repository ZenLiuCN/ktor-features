package cn.zenliu.ktor.features.consul.internal

import kotlinx.serialization.*
import java.time.Instant
import java.time.format.DateTimeFormatter

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {
    private val format = DateTimeFormatter.ISO_INSTANT
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("JvmInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.from(format.parse(decoder.decodeString()))
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(format.format(value))
    }

}