package cn.zenliu.ktor.features.redis

import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.template.*
import com.fasterxml.jackson.databind.*
import io.ktor.application.*
import io.lettuce.core.*
import io.lettuce.core.codec.*
import kotlinx.io.core.*
import java.nio.*
import java.nio.charset.*

class Redis {
	companion
	object Feature :
		FeatureTemplate.FeatureObjectTemplate<Application, Feature, Feature, Feature.RedisConf>() {
		override val configClazz = Feature.RedisConf::class
		private val objectMapper: ObjectMapper = ObjectMapper()

		@Properties("redis")
		class RedisConf(
			var url: String
		)

		/**
		 * configrate inner ObjectMapper
		 * @param configurate (ObjectMapper) -> Unit
		 */

		fun configObjectMapper(configurate: (ObjectMapper) -> Unit) {
			configurate.invoke(objectMapper)
		}

		val client by lazy {
			RedisClient.create(config!!.url)
		}
		/**
		 * template<String,JsonNode>
		 */
		val redisJson by lazy {
			client.connect(
				codec<String, JsonNode>(
					{ ByteBuffer.wrap(it.toByteArray()) },
					{ it.decodeString() },
					{ ByteBuffer.wrap(it.toString().toByteArray()) },
					{ objectMapper.readTree(it.decodeString()) }
				)).sync()
		}
		val redis by lazy {
			client.connect(
				codec<String, String>(
					{ ByteBuffer.wrap(it.toByteArray()) },
					{ it.decodeString() },
					{ ByteBuffer.wrap(it.toByteArray()) },
					{ it.decodeString() }
				)).sync()
		}

		/**
		 * create new template connection
		 * @param keyEncoder (K) -> ByteBuffer
		 * @param keyDecoder (ByteBuffer) -> K
		 * @param valueEncoder (V) -> ByteBuffer
		 * @param valueDecoder (ByteBuffer) -> V
		 * @return (io.lettuce.core.api.StatefulRedisConnection<(K..K?), (V..V?)>..io.lettuce.core.api.StatefulRedisConnection<(K..K?), (V..V?)>?)
		 */
		fun <K : Any, V : Any> newConnection(
			keyEncoder: (K) -> ByteBuffer,
			keyDecoder: (ByteBuffer) -> K,
			valueEncoder: (V) -> ByteBuffer,
			valueDecoder: (ByteBuffer) -> V
		) = client.connect(
			codec<K, V>(
				keyEncoder,
				keyDecoder,
				valueEncoder,
				valueDecoder
			)
		)

		override fun init(
			pipeline: Application,
			configure: Feature.() -> Unit
		) = run {
			config ?: throw Exception("datasource redis.url not set!")
			this
		}

		/**
		 * create redis codec
		 * @param k2b (K) -> ByteBuffer
		 * @param b2k (ByteBuffer) -> K
		 * @param v2b (V) -> ByteBuffer
		 * @param b2v (ByteBuffer) -> V
		 * @return RedisCodec<K, V>
		 */
		fun <K : Any, V : Any> codec(
			k2b: (K) -> ByteBuffer,
			b2k: (ByteBuffer) -> K,
			v2b: (V) -> ByteBuffer,
			b2v: (ByteBuffer) -> V
		) = object : RedisCodec<K, V> {
			override fun decodeKey(bytes: ByteBuffer): K = b2k.invoke(bytes)
			override fun encodeValue(value: V): ByteBuffer = v2b.invoke(value)
			override fun encodeKey(key: K): ByteBuffer = k2b.invoke(key)
			override fun decodeValue(bytes: ByteBuffer): V = b2v.invoke(bytes)
		}

		private
		fun ByteBuffer.decodeString(charset: Charset = Charsets.UTF_8): String {
			return charset.decode(this).toString()
		}

	}
}
