package cn.zenliu.ktor.features.ebean

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import io.ebean.config.*
import io.ktor.application.*
import io.ktor.util.pipeline.*
import java.util.concurrent.*
import kotlin.reflect.*

class EbeanProvider private constructor() {
	companion
	object Feature : FeatureTemplate.FeatureObjectTemplate<Application, Feature, Feature, Feature.Conf>() {
		override val configClazz: KClass<Conf> = Conf::class
		override fun init(pipeline: Application, configure: Feature.() -> Unit): Feature {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
				pipeline.install(PropertiesManager)
			}

			this.apply(configure)
			when {
				currentTenantProvider != null && currentUserProvider != null -> {
					pipeline.insertPhaseAfter(ApplicationCallPipeline.Call, phase)
					pipeline.intercept(phase) {
						currentTenantProvider?.remove()
						currentUserProvider?.remove()
					}
				}
				currentUserProvider != null -> {
					pipeline.insertPhaseAfter(ApplicationCallPipeline.Call, phase)
					pipeline.intercept(phase) {
						currentUserProvider?.remove()
					}
				}
				currentTenantProvider != null -> {
					pipeline.insertPhaseAfter(ApplicationCallPipeline.Call, phase)
					pipeline.intercept(phase) {
						currentTenantProvider?.remove()
					}
				}
				else -> throw Throwable("not create currentUserProvider or currentTenantProvider!!")
			}

			return this
		}

		val phase = PipelinePhase("EbeanIdProviderRemove")

		@Properties("ebean")
		class Conf

		interface ConfigableProvider<T> {
			fun set(id: T)
			fun remove(): T?
			fun clear()
		}

		class ThreadUserProvider<T>(val default: T?, val debug: Boolean = false) : ConfigableProvider<T>, CurrentUserProvider {
			private val ids = ConcurrentHashMap<Long, T>()
			override fun set(id: T) {
				debug.takeIf { it }?.let { println("save user id ${Thread.currentThread().id} $id") }
				ids[Thread.currentThread().id] = id
			}

			override fun remove() = ids.remove(Thread.currentThread().id).apply {
				debug.takeIf { it }?.let { println("remove user id ${Thread.currentThread().id} $this") }
			}

			override fun clear() = ids.clear()
			override fun currentUser() = (ids[Thread.currentThread().id] ?: default).apply {
				debug.takeIf { it }?.let { println("get User id ${Thread.currentThread().id} $this") }
			}
		}

		class ThreadTenantProvider<T>(val default: T?, val debug: Boolean = false) : ConfigableProvider<T>, CurrentTenantProvider {
			private val ids = ConcurrentHashMap<Long, T>()
			override fun set(id: T) {
				debug.takeIf { it }?.let { println("save tenant id ${Thread.currentThread().id} $id") }
				ids[Thread.currentThread().id] = id
			}

			override fun clear() = ids.clear()
			override fun remove() = ids.remove(Thread.currentThread().id).apply {
				debug.takeIf { it }?.let { println("remove tenant id ${Thread.currentThread().id} $this") }
			}

			override fun currentId() = (ids[Thread.currentThread().id] ?: default).apply {
				debug.takeIf { it }?.let { println("get Tenant id ${Thread.currentThread().id} $this") }
			}

		}

		private var currentUserProvider: ThreadUserProvider<Any>? = null
		private var currentTenantProvider: ThreadTenantProvider<Any>? = null

		fun createUserProvider(default: Any?, debug: Boolean = false) =
			if (currentUserProvider == null)
				currentUserProvider = ThreadUserProvider(default, debug)
			else Unit


		fun createTenantProvider(default: Any?, debug: Boolean = false) =
			if (currentTenantProvider == null)
				currentTenantProvider = ThreadTenantProvider(default, debug)
			else Unit

		fun setUserId(id: Any) = currentUserProvider?.set(id)
		fun setTenantId(id: Any) = currentTenantProvider?.set(id)
		fun getCurrentUserProvider() = currentUserProvider
		fun getCurrentTenantProvider() = currentTenantProvider

	}
}
