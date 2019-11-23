package cn.zenliu.ktor.features.cache

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.manager.*
import com.github.benmanes.caffeine.cache.*
import io.github.config4k.*
import io.ktor.application.*
import kotlin.reflect.*

typealias CaffeineConf = MutableMap<String, String>

class CaffeineManager private constructor() {
	companion
	object CaffeineFeature : FeatureTemplate.FeatureObjectTemplate<Application, CaffeineFeature, CaffeineFeature, CaffeineConf>() {
		override val configClazz: KClass<out CaffeineConf> = mutableMapOf<String, String>()::class
		override fun init(pipeline: Application, configure: CaffeineFeature.() -> Unit): CaffeineFeature {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
				pipeline.install(PropertiesManager)
			}
			config ?: throw Exception("CaffeineFeature configuration invalid!")
			this.apply(configure)
			validate()
			return this
		}


		override val config: CaffeineConf?
			get() = PropertiesManager.dump()?.getConfig("caffeine")?.let {
				it.root().keys.map { k -> k to it.extract<String>(k) }.toMap()
			} as? CaffeineConf

		data class CaffeineCache(
			val spec: String,
			val async: Boolean = false
		)

		/**
		 * build cache **really**
		 * @param name String
		 * @param async Boolean  default false
		 * @param builder Function1<K, V>? optional loading function
		 */

		fun <K, V> buildCache(name: String, async: Boolean = false, builder: ((K) -> V)? = null) {
			val it = config!!.get(name) ?: throw Throwable("$name not a configurated cache!")
			when {
				async && builder == null -> Caffeine.from(it).buildAsync<K, V>().let { _caches.put(name, it) }
				async && builder != null -> Caffeine.from(it).buildAsync(builder).let { _caches.put(name, it) }
				!async && builder == null -> Caffeine.from(it).build<K, V>().let { _caches.put(name, it) }
				!async && builder != null -> Caffeine.from(it).build(builder).let { _caches.put(name, it) }
				else -> throw Exception("invalid configure for $name")
			}
		}

		private fun validate() {
			val names = _caches.keys
			if (names.size != config!!.size ||
				!config!!.keys.containsAll(names)
			) {
				throw Throwable("not all caches had being inited, plz invoke buildCache function to initalize all caches")
			}
		}

		private var _caches: MutableMap<String, Any> = mutableMapOf()
		/**
		 * cache source
		 */
		val caches by lazy {
			_caches.filterValues { it !is LoadingCache<*, *> && it !is AsyncCache<*, *> }
		}
		val loadingCaches by lazy {
			_caches.filterValues { it is LoadingCache<*, *> }.mapValues { it.value as LoadingCache<*, *> }
		}
		val asyncCaches by lazy {
			_caches.filterValues { it !is AsyncLoadingCache<*, *> && it !is Cache<*, *> }
		}
		val asyncLoadingCaches by lazy {
			_caches.filterValues { it is AsyncLoadingCache<*, *> }.mapValues { it.value as AsyncLoadingCache<*, *> }
		}

		@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
		inline fun <K, V> fetchCache(name: String) = caches[name] as? Cache<K, V>

		@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
		inline fun <K, V> fetchLoadingCache(name: String) = loadingCaches[name] as? LoadingCache<K, V>

		@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
		inline fun <K, V> fetchAsyncCache(name: String) = asyncCaches[name] as? AsyncCache<K, V>

		@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
		inline fun <K, V> fetchAsyncLoadingCache(name: String) = asyncLoadingCaches[name] as? AsyncLoadingCache<K, V>
	}
}
