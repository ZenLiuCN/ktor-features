package cn.zenliu.ktor.features.eventbus

/**
 * Basic event class
 * @property timestamp Long
 * @property name String
 */
abstract class Event<E> {
    val timestamp = System.currentTimeMillis()
    abstract val name: String
    abstract fun toEnum(): E

}


/**
 * interface of EventEnum
 * with createEvent
 */
interface EventEnum
