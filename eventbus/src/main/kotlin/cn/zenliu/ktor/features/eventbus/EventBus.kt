package cn.zenliu.ktor.features.eventbus


import cn.zenliu.ktor.features.properties.annotation.Properties
import cn.zenliu.ktor.features.template.FeatureTemplate
import io.ktor.application.Application
import io.ktor.application.ApplicationStarted
import io.ktor.application.ApplicationStarting
import io.ktor.application.ApplicationStopPreparing
import kotlinx.coroutines.DisposableHandle
import reactor.core.publisher.Flux
import reactor.core.publisher.FluxSink
import reactor.core.scheduler.Schedulers
import java.util.logging.Level
import kotlin.reflect.KClass

@Properties("eventbus")
class BusProperties(val enable: Boolean = true)


object EventBus : FeatureTemplate.FeatureObjectTemplate<Application, EventBus, EventBus, BusProperties>() {
    override val configClazz: KClass<BusProperties> = BusProperties::class

    override fun init(pipeline: Application, configure: EventBus.() -> Unit): EventBus {
        addSubsciber(AppEvents.START.name)
        addSubsciber(AppEvents.STOP.name)
        closeWatcher = pipeline.environment.monitor.subscribe(ApplicationStopPreparing) {
            subscriber[AppEvents.STOP.name]?.next(AppEvents.STOP.createEvent())

            subscriber.forEach { (_, sk) ->
                sk.complete()//close all flux
            }
            closeWatcher.dispose()
        }
        startingWatcher = pipeline.environment.monitor.subscribe(ApplicationStarting) {
            subscriber[AppEvents.START.name]?.next(AppEvents.START.createEvent())
        }
        startedWatcher = pipeline.environment.monitor.subscribe(ApplicationStarted) {
            startingWatcher.dispose()
            startedWatcher.dispose()
        }
        this.apply(configure)
        return this
    }

    /**
     * flux sink per event name
     */
    private val subscriber = mutableMapOf<String, FluxSink<Event<*>>>()
    /**
     * Flux per event name
     */
    private val flux = HashMap<String, Flux<Event<*>>>()
    private lateinit var closeWatcher: DisposableHandle
    private lateinit var startingWatcher: DisposableHandle
    private lateinit var startedWatcher: DisposableHandle


    private fun removeSubsciber(name: String) {
        subscriber[name]?.let {
            it.complete()
            subscriber.remove(name)
            flux.remove(name)
        }
    }

    private fun addSubsciber(name: String) = subscriber[name] ?: run {
        Flux.create<Event<*>> { snk -> subscriber.put(name, snk) }
            .share()
            .publishOn(Schedulers.single())
            .apply { flux.put(name, this) }
            .log(name, Level.INFO)
            .doOnComplete { removeSubsciber(name) }
    }

    private fun addSubscriberFlux(name: String) = run {
        addSubsciber(name)
        flux[name]!!
    }

    /**
     * Subscrib an event and get one Flux to use
     * @param events Array<out Event>
     * @return ([reactor.core.publisher.Flux]<[cn.zenliu.ext.ebus.Event]>)
     */
    fun subscrib(vararg events: Enum<*>) = run {
        events.map {
            addSubscriberFlux(it.name)
        }.let {
            if (it.size > 1) {
                Flux.concat(it)
            } else {
                it.first()
            }
        }
    }

    /**
     *
     * @param events Array<out [Event]>
     */
    fun publish(vararg events: Event<*>) {
        events.forEach {
            subscriber[it.name]?.next(it)
        }
    }

}
