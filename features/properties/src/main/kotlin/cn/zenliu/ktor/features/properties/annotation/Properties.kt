package cn.zenliu.ktor.features.properties.annotation

/**
 * annotation a property class
 * @property path String define which section should read
 * @constructor
 */
@Target(AnnotationTarget.CLASS)
annotation class Properties(val path: String)
