package cn.zenliu.ktor.features.graphlike.type

import com.typesafe.config.Config


typealias ProcessFunction = suspend (String, Config) -> Any?

typealias JsonArrayObject = Collection<Map<String, Any?>>
typealias JsonObject = Map<String, Any?>

