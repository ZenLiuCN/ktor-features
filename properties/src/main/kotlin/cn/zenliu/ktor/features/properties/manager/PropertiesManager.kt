package cn.zenliu.ktor.features.properties.manager


import cn.zenliu.ktor.features.properties.annotation.*
import com.typesafe.config.*
import io.github.config4k.*
import io.github.config4k.readers.*
import io.ktor.application.*
import io.ktor.util.*
import kotlin.reflect.*

/**
 *  Properties management
 */
class PropertiesManager private constructor() {
    companion object PropertiesFeature : ApplicationFeature<Application, Config, PropertiesManager> {
        /**
         * extension function for [KClass] may not use out of [cn.zenliu.ktor.features.properties]
         * @receiver KClass<*>
         * @return T?
         */
        inline fun <reified T : Annotation> KClass<*>.findAnnotationSafe() = try {
            this.annotations.firstOrNull { it is T } as T?
        } catch (e: UnsupportedOperationException) {
            this.java.annotations.firstOrNull { it is T } as T?
        }

        /**
         * auto load  application.conf application.json application.properties
         * @see com.typesafe.config.impl.SimpleIncluder.fromBasename
         * @param pipeline Application
         * @param configure [@kotlin.ExtensionFunctionType] Function1<Config, Unit>
         * @return PropertiesManager
         */
        override fun install(pipeline: Application, configure: Config.() -> Unit): PropertiesManager {
            conf = ConfigFactory.load()
            return PropertiesManager()
        }
        //todo: does this need some fixed key?
        override val key: AttributeKey<PropertiesManager> =
                AttributeKey<PropertiesManager>(this::class.simpleName ?: "factory.${System.currentTimeMillis()}")
        /**
         * lateinit var  should be safe checked!
         */
        private lateinit var conf: Config

        /**
         * create default config or read config
         * @param T data class with [Properties] annotation
         * @param path String? configuration path ,default null to use [Properties] annotation configuration
         * @return T? retrun null when T is not with [Properties] annotation
         */
        inline fun <reified T : Any> properties(path: String? = null) = properties(T::class, path)

        /**
         *  create default config or read config
         * @param clazz KClass<T> data class with [Properties] annotation
         * @param path String? configuration path ,default null to use [Properties] annotation configuration
         * @return T? retrun null when T is not with [Properties] annotation
         */
        fun <T : Any> properties(clazz: KClass<T>, path: String? = null) = (
                this.conf.takeIf { this::conf.isInitialized } ?: throw Throwable("config not initialized")
                ).let { cfg ->
            //extract with path
            path
                    ?.let {
                        cfg.extract(path, clazz)
                    } //extract for clazz with annotation
                    ?: clazz.findAnnotationSafe<Properties>()
                            ?.let { anno ->
                                cfg.extract(anno.path, clazz)
                                        ?: clazz.constructors.firstOrNull()?.call()
                                        ?: throw Exception("not found empty constructor for config class ${clazz.simpleName}")
                            }
                    //finally extract with none path
                    ?: cfg.extract(path, clazz)
        }


        /**
         * debug function
         * @return Config
         */
        fun dump() = this.conf.takeIf { this::conf.isInitialized }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> Config.extract(path: String? = null, clazz: KClass<T>): T? {
            val genericType = object : TypeReference<T>() {}.genericType()
            val result = if (path != null) {
                SelectReader.getReader(ClassContainer(clazz, genericType))(this, path)
            } else SelectReader.extractWithoutPath(ClassContainer(clazz, genericType), this)
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


