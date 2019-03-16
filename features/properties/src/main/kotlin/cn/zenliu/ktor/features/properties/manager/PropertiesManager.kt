package cn.zenliu.ktor.features.properties.manager


import cn.zenliu.ktor.features.properties.annotation.Properties
import com.typesafe.config.Config
import com.typesafe.config.ConfigException
import com.typesafe.config.ConfigFactory
import io.github.config4k.ClassContainer
import io.github.config4k.TypeReference
import io.github.config4k.readers.SelectReader
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import kotlin.reflect.KClass

/**
 *  Properties management
 */
class PropertiesManager {
    companion object properties : ApplicationFeature<Application, Config, PropertiesManager> {
        /**
         * extention function for [KClass] may not use out of [cn.zenliu.ktor.features.properties]
         * @receiver KClass<*>
         * @return T?
         */
        inline fun <reified T : Annotation> KClass<*>.findAnnotationSafe() = try {
            this.annotations.firstOrNull { it is T } as T?
        } catch (e: UnsupportedOperationException) {
            this.java.annotations.firstOrNull { it is T } as T?
        }

        override fun install(pipeline: Application, configure: Config.() -> Unit): PropertiesManager {
            conf = ConfigFactory.load()
            return PropertiesManager()
        }

        override val key: AttributeKey<PropertiesManager> =
            AttributeKey<PropertiesManager>(this::class.simpleName ?: "factory.${System.currentTimeMillis()}")
        /**
         * lateinit var  should be safe checked!
         */
        private lateinit var conf: Config

        /**
         * create default config or read config
         * @param T data class with [Properties] annotation
         * @return T? retrun null when T is not with [Properties] annotation
         */
        inline fun <reified T : Any> properties() = this.properties(T::class)


        /**
         *  create default config or read config
         * @param kclazz KClass<T> data class with [Properties] annotation
         * @return T? retrun null when T is not with [Properties] annotation
         */
        fun <T : Any> properties(kclazz: KClass<T>) =
            kclazz.findAnnotationSafe<Properties>()?.let { anno ->
                conf.extract(anno.path, kclazz) ?: kclazz.constructors.firstOrNull()?.call()
            }


        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> Config.extract(path: String, kclazz: KClass<T>): T? {
            val genericType = object : TypeReference<T>() {}.genericType()
            val result = SelectReader.getReader(ClassContainer(kclazz, genericType))(this, path)
            return try {
                result as T
            } catch (e: Exception) {
                throw result?.let { e } ?: ConfigException.BadPath(
                    path, "take a look at your config"
                )
            }
        }


        private fun ConfigurationException(property: String) = Exception("$property not set!")

    }
}


