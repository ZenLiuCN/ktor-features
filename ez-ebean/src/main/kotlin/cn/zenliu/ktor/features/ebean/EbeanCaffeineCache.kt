package cn.zenliu.ktor.features.ebean

import com.github.benmanes.caffeine.cache.*
import io.ebean.*
import io.ebean.cache.*
import io.ebean.config.*
import java.util.concurrent.*


/**
 * wapper of ServerCache for ebean
 * @property name String
 * @property maximumSize Int
 * @property cache Cache<Any, Any?>
 * @property sizeCurrent Int
 * @property clearCnt Long
 * @property removeCnt Long
 * @constructor
 */
class EbeanCaffeineCache private constructor(
	private val config: ServerCacheConfig,
	private val maximumSize: Int,
	private val cache: Cache<Any, Any?>
) : ServerCache {
	private var clearCnt = 0L
	private var removeCnt = 0L
	override fun clear() {
		clearCnt += 1
		cache.invalidateAll()
	}

	override fun put(id: Any, value: Any) {
		cache.put(id, value)
	}

	override fun remove(id: Any) {
		removeCnt += 1
		cache.invalidate(id)
	}

	override fun getStatistics(reset: Boolean): ServerCacheStatistics {
		if (reset) {
			cache.cleanUp()
			clearCnt = 0L
			removeCnt = 0L
		}
		return ServerCacheStatistics().apply {
			val stat = cache.stats()
			cacheName = config.cacheKey
			missCount = stat.missCount()
			size = cache.estimatedSize().toInt()
			hitCount = stat.hitCount()
			removeCount = removeCnt
			evictCount = stat.evictionCount()
			clearCount = clearCnt
			maxSize = maximumSize

		}
	}

	override fun getHitRatio(): Int {
		return (cache.stats().hitRate() * 100.0).toInt()
	}

	override fun size(): Int {
		return cache.estimatedSize().toInt()
	}

	override fun get(id: Any): Any? {
		return cache.getIfPresent(id)
	}

	companion object : ServerCachePlugin, ServerCacheFactory, ServerCacheNotify {
		private lateinit var cfgExecutor: BackgroundExecutor
		private lateinit var serverCacheNotify: ServerCacheNotify
		fun buildFromConfig(config: ServerCacheConfig): ServerCache {
			val cfg = config.cacheOptions
			return if (config.isQueryCache)
				EbeanCaffeineCache(
					config,
					cfg.maxSize,
					Caffeine.newBuilder().apply {
						expireAfterAccess(cfg.maxIdleSecs.toLong(), TimeUnit.SECONDS)
						expireAfterWrite(cfg.maxSecsToLive.toLong(), TimeUnit.SECONDS)
						executor { command -> cfgExecutor.execute(command) }
					}.build<Any, Any>()
				)
			else EbeanCaffeineCache(
				config,
				cfg.maxSize,
				Caffeine.newBuilder().apply {
					expireAfterAccess(cfg.maxIdleSecs.toLong(), TimeUnit.SECONDS)
					expireAfterWrite(cfg.maxSecsToLive.toLong(), TimeUnit.SECONDS)
					executor { command -> cfgExecutor.execute(command) }

				}.build<Any, Any>()
			)
		}

		override fun createCacheNotify(listener: ServerCacheNotify): ServerCacheNotify {
			return this
		}

		override fun createCache(config: ServerCacheConfig): ServerCache {
			return buildFromConfig(config)
		}

		override fun create(config: ServerConfig, executor: BackgroundExecutor): ServerCacheFactory {
			this.cfgExecutor = executor
			return this
		}

		override fun notify(notification: ServerCacheNotification) {
			if (this::serverCacheNotify.isInitialized) serverCacheNotify.notify(notification)
		}
	}
}


