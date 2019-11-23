package cn.zenliu.ktor.features.properties.manager


import cn.zenliu.ktor.features.properties.annotation.*
import com.typesafe.config.*
import io.github.config4k.*
import io.github.config4k.readers.*
import io.ktor.application.*
import io.ktor.util.*
import java.lang.reflect.*
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

		inline fun <reified T : Any> fetchMap(path: String? = null) = fetchMap(T::class, path)

		fun <T : Any> fetchMap(clazz: KClass<T>, path: String? = null) = (this.conf.takeIf { this::conf.isInitialized }
			?: throw Throwable("config not initialized")).let { cfg ->
			when {
				cfg.isEmpty -> emptyMap<String, T>()
				path == null -> cfg.root().keys.map { k ->
					k to (cfg.extract(k, clazz) ?: throw Throwable("config under $k invalid"))
				}.toMap()
				!cfg.hasPath(path) -> null
				else -> cfg.getConfig(path).root().keys.map { k ->
					k to (cfg.extract(k, clazz) ?: throw Throwable("config under $k invalid"))
				}.toMap()
			}
		}

		private fun <T : Any> Config.tryExtract(kClass: KClass<T>, path: String): T? = when {
			kClass == String::class -> this.getString(path) as? T
			kClass == Double::class -> this.getDouble(path) as? T
			kClass == Int::class -> this.getInt(path) as? T
			kClass == Long::class -> this.getLong(path) as? T
			kClass == Boolean::class -> this.getBoolean(path) as? T
			else -> extract(path, kClass)
		}

		/**
		 * debug function or use for special useage like extract some map...
		 * @return Config
		 */
		fun dump() = this.conf.takeIf { this::conf.isInitialized }

        @Suppress("UNCHECKED_CAST")
        private fun <T : Any> Config.extract(path: String? = null, clazz: KClass<T>): T? {
	        /*
	        //todo some better way of this
			val genericType = (
				((clazz.java.genericSuperclass as? ParameterizedType)?:(clazz.java as? ParameterizedType))
				?.actualTypeArguments
				?.firstOrNull()
				?.takeIf { it is ParameterizedType }
				?.let { getGenericMap(it as ParameterizedType) }
				?: emptyMap()).let { ClassContainer(clazz, it) }
			val result = if (path.isNullOrBlank()) {
				SelectReader.extractWithoutPath(genericType, this)
			} else SelectReader.getReader(genericType)(this, path)
			return try {
				result as T
			} catch (e: Exception) {
				throw result?.let { e } ?: ConfigException.BadPath(
					path, "take a look at your config"
				)
			}
	         */
	        //TODO found how to do with collections
            val genericType = object : TypeReference<T>() {}.genericType().let { ClassContainer(clazz,it) }
            val result  = if (path.isNullOrBlank()) {
		        SelectReader.extractWithoutPath(genericType, this)
	        } else SelectReader.getReader(genericType)(this, path)
            return try {
                result as T
            } catch (e: Exception) {
                throw result?.let { e } ?: ConfigException.BadPath(
                        path, "take a look at your config"
                )
            }
        }
		inline fun <reified T>extract(path: String="")= dump()?.extract<T>(path)

		internal fun getGenericMap(type: ParameterizedType,
		                           typeArguments: Map<String, ClassContainer> = emptyMap()): Map<String, ClassContainer> {
			val typeParameters = (type.rawType as Class<*>).kotlin.typeParameters
			return type.actualTypeArguments.mapIndexed { index, r ->
				val typeParameterName = typeParameters[index].name
				val impl = if (r is WildcardType) r.upperBounds[0] else r
				typeParameterName to if (impl is TypeVariable<*>) {
					requireNotNull(typeArguments[impl.name]) { "no type argument for ${impl.name} found" }
				} else {
					val wild = ((if (impl is ParameterizedType) impl.rawType else impl) as Class<*>).kotlin
					if (impl is ParameterizedType) ClassContainer(wild, getGenericMap(impl, typeArguments))
					else ClassContainer(wild)
				}
			}.toMap()
		}

		private fun ConfigurationException(property: String) = Exception("$property not set!")
	}

}



