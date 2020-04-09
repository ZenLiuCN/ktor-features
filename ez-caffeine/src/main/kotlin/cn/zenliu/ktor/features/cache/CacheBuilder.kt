package cn.zenliu.ktor.features.cache

import com.github.benmanes.caffeine.cache.*
import java.lang.IllegalArgumentException
import kotlin.reflect.*

/**
 *
 * @param K : Any
 * @param V : Any
 * @property name String
 * @property builder Function0<Caffeine<K, V>>
 * @constructor
 */
data class CacheConfig<K : Any, V : Any>(
	val name: String,
	val builder:(() -> Cache<K, V>)
)

interface CacheBuilder {
	fun buildCaches(): Set<CacheConfig<*, *>>
}
