package cn.zenliu.ktor.features.cache

import com.github.benmanes.caffeine.cache.*
import java.lang.IllegalArgumentException
import kotlin.reflect.*

/**
 *
 * @param K : Any
 * @param V : Any
 * @property spec String?
 * @property name String
 * @property async Boolean
 * @property key KClass<out K>
 * @property value KClass<out V>
 * @property builder Function1<K, V>?
 * @property config [@kotlin.ExtensionFunctionType] Function1<Caffeine<K, V>, Unit>?
 * @constructor
 */
data class CacheConfig<K : Any, V : Any>(
	val name: String,
	val builder:((Caffeine<*, *>) -> Caffeine<*, *>)
)

interface CacheBuilder {
	fun buildCaches(): Set<CacheConfig<*, *>>
}
