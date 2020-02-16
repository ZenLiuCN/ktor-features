package cn.zenliu.ktor.features.ebean

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import com.typesafe.config.*
import io.ebean.*
import io.ebean.config.*
import io.ktor.application.*
import kotlin.reflect.*

/**
 *  EbeanORM feature
 *  only load configuration from application.conf#ebean
 */
class EbeanORM private constructor() {
    companion
    object EbeanFeature : FeatureTemplate.FeatureObjectTemplate<Application, EbeanFeature, EbeanFeature, EbeanFeature.EbeanConf>() {
        override val configClazz: KClass<EbeanConf> = EbeanConf::class

        override fun init(pipeline: Application, configure: EbeanFeature.() -> Unit): EbeanFeature {
            pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
                pipeline.install(PropertiesManager)
            }

            config ?: throw Exception("Ebean config not set!")
            this.apply(configure)
            this::db.isInitialized.takeIf { it }
                    ?: throw Throwable("plz use configEbean function to initialize database")
            return this
        }

        val database
            get() = db
        private lateinit var db: Database
        fun configEbean(config: DatabaseConfig.(conf: EbeanConf) -> Unit) {
            db = DatabaseFactory.create(DatabaseConfig().apply {
                this.loadFromProperties(ConfigProperties(PropertiesManager.dump()!!.getObject("ebean")!!))
                config.invoke(this, this@EbeanFeature.config!!)
            })
        }

        @Properties("ebean")
        class EbeanConf

        private fun ConfigObject.toProperties(m: MutableMap<String, String>, prefix: String? = null): MutableMap<String, String> = run {
            this.forEach { t, u ->
                val k = prefix?.let { "$prefix.$t" } ?: t
                when (u.valueType()) {
                    ConfigValueType.OBJECT -> (u as ConfigObject).toProperties(m, k)
                    ConfigValueType.LIST -> m.put(k, (u.unwrapped() as List<Any>).joinToString { it.toString() })
                    ConfigValueType.NUMBER -> m.put(k, u.unwrapped().toString())
                    ConfigValueType.BOOLEAN -> m.put(k, u.unwrapped().toString())
                    ConfigValueType.NULL -> Unit
                    ConfigValueType.STRING -> m.put(k, u.unwrapped() as String)
	                else-> TODO()
                }
            }
            m
        }

        private class ConfigProperties(config: ConfigObject) : java.util.Properties() {
            init {
                super.putAll(config.toProperties(mutableMapOf(),"ebean"))
            }
        }

    }
}
