package cn.zenliu.ktor.features.cache

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.manager.*
import com.github.benmanes.caffeine.cache.*
import io.github.config4k.*
import io.ktor.application.*
import kotlin.reflect.*

typealias CaffeineConf = MutableMap<String, CaffeineManager.CaffeineFeature.CaffeineCache>

class CaffeineManager private constructor() {
	companion
	object CaffeineFeature : FeatureTemplate.FeatureObjectTemplate<Application, CaffeineFeature, CaffeineFeature, CaffeineConf>() {
		override val configClazz: KClass<out CaffeineConf> = mutableMapOf<String, CaffeineManager.CaffeineFeature.CaffeineCache>()::class
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
				it.root().keys.map { k -> k to it.extract<CaffeineCache>(k) }.toMap()
			} as? CaffeineConf

		data class CaffeineCache(
			val spec: String,
			val async: Boolean = false
		)


		fun <K, V> buildCache(name: String, builder: ((K) -> V)? = null) {
			val it = config!!.get(name) ?: throw Throwable("$name not a configurated cache!")
			when {
				it.async && builder == null -> Caffeine.from(it.spec).buildAsync<K, V>().let { _asyncCaches.put(name, it) }
				it.async && builder != null -> Caffeine.from(it.spec).buildAsync(builder).let { _asyncCaches.put(name, it) }
				!it.async && builder == null -> Caffeine.from(it.spec).build<K, V>().let { _caches.put(name, it) }
				!it.async && builder != null -> Caffeine.from(it.spec).build(builder).let { _caches.put(name, it) }
				else -> throw Exception("invalid configure for $name")
			}
		}

		private fun validate() {
			val names = _caches.keys.toMutableList().apply {
				addAll(_asyncCaches.keys)
			}
			if (names.size != config!!.size ||
				!config!!.keys.containsAll(names)
			) {
				throw Throwable("not all caches had being inited, plz invoke buildCache function to initalize all caches")
			}
		}

		private var _caches: MutableMap<String, Cache<*, *>> = mutableMapOf()
		private var _asyncCaches: MutableMap<String, AsyncCache<*, *>> = mutableMapOf()
		/**
		 * cache source
		 */
		val caches by lazy {
			_caches.filterValues { it !is LoadingCache }
		}
		val loadingCaches by lazy {
			_caches.filterValues { it is LoadingCache }.mapValues { it.value as LoadingCache<*, *> }
		}
		val asyncCaches by lazy {
			_asyncCaches.filterValues { it !is AsyncLoadingCache }
		}
		val asyncLoadingCaches by lazy {
			_asyncCaches.filterValues { it is AsyncLoadingCache }.mapValues { it.value as AsyncLoadingCache<*, *> }
		}

		@Suppress("UNCHECKED_CAST")
		inline fun <K, V> fetchCache(name: String) = caches[name] as? Cache<K, V>

		@Suppress("UNCHECKED_CAST")
		inline fun <K, V> fetchLoadingCache(name: String) = loadingCaches[name] as? LoadingCache<K, V>

		@Suppress("UNCHECKED_CAST")
		inline fun <K, V> fetchAsyncCache(name: String) = asyncCaches[name] as? AsyncCache<K, V>

		@Suppress("UNCHECKED_CAST")
		inline fun <K, V> fetchAsyncLoadingCache(name: String) = asyncLoadingCaches[name] as? AsyncLoadingCache<K, V>
	}
}
