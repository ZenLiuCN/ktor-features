package cn.zenliu.ktor.features.datasource

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import com.fasterxml.jackson.databind.*
import io.ktor.application.*
import org.litote.kmongo.coroutine.*
import org.litote.kmongo.reactivestreams.*
import java.nio.*
import java.nio.charset.*

class Kmongo {
	companion
	object KmongoFeature :
		FeatureTemplate.FeatureObjectTemplate<Application, KmongoFeature, KmongoFeature, KmongoFeature.KMongoConf>() {
		override val configClazz = KmongoFeature.KMongoConf::class
		private val objectMapper: ObjectMapper = ObjectMapper()

		@Properties("mongo")
		class KMongoConf(
			var conn: String
		)

		/**
		 * configrate inner ObjectMapper
		 * @param configurate (ObjectMapper) -> Unit
		 */

		fun configObjectMapper(configurate: (ObjectMapper) -> Unit) {
			configurate.invoke(objectMapper)
		}

		val client by lazy {
			KMongo.createClient(config!!.conn)
		}
		val coroutineClient by lazy { client.coroutine }
		fun newClient(conn: String) = KMongo.createClient(conn)
		fun newClient(conn: com.mongodb.ConnectionString) = KMongo.createClient(conn)
		fun newClient(connSetting: com.mongodb.MongoClientSettings) = KMongo.createClient(connSetting)
		override fun init(
			pipeline: Application,
			configure: KmongoFeature.() -> Unit
		) = run {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
				pipeline.install(PropertiesManager)
			}
			config ?: throw Exception("datasource mongo.conn not set!")
			this
		}


		private
		fun ByteBuffer.decodeString(charset: Charset = Charsets.UTF_8): String {
			return charset.decode(this).toString()
		}

	}
}
