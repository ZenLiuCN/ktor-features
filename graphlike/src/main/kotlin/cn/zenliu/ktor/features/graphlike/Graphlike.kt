package cn.zenliu.ktor.features.graphlike


import cn.zenliu.ktor.features.graphlike.annotation.AUTHED
import cn.zenliu.ktor.features.graphlike.annotation.PROC
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigRenderOptions
import com.typesafe.config.ConfigValueFactory
import io.github.config4k.extract
import io.ktor.application.Application
import io.ktor.application.ApplicationFeature
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.findAnnotation

/**
 * Feature that use Hocon as command protocal
 *
 */

class Graphlike {
    companion object processor : ApplicationFeature<Application, processor, processor> {
        //<editor-fold desc="Main functions">
        private val log = LoggerFactory.getLogger(this::class.java)
        private var proto: ProcessorPool = ProcessorPool()
        private var principalClassCheck: (Class<*>) -> Boolean = { true }
        private var principalCheck: (Any?) -> Boolean = { true }
        /**
         * if use [AUTH] must set principalCheck
         * @param classCheck (Class<*>) -> Boolean for check processor function parameter class is vaild Principal
         * @param instanceCheck (Any?) -> Boolean for check  parameter value is vaild Principal
         */
        fun setPrincipalCheck(classCheck: (Class<*>) -> Boolean, instanceCheck: (Any?) -> Boolean) {
            this.principalCheck = instanceCheck
            this.principalClassCheck = classCheck
        }

        //make it auto maybe later
        fun register(set: Class<out Enum<*>>) {
            if (!set.isEnum) return
            set.enumConstants
                .map { en ->
                    en.javaClass.methods
                        .filter { m ->
                            m.isProcessor().apply {
                                if (!this)
                                    m.getDeclaredAnnotation(PROC::class.java)?.let {
                                        log.debug("not valid function ${m.name}\n${m.parameterCount}\n${m.parameterTypes.map { it.canonicalName }}\n${m.returnType}")
                                    }
                            }
                        }
                        .map { fn ->
                            Processor(
                                set.name,
                                en.javaClass.name.substringAfterLast("$"),
                                fn.name,
                                fn,
                                en,
                                fn.getAnnotation(AUTHED::class.java) != null && principalClassCheck(fn.parameterTypes.last())
                            )
                        }
                }
                .flatten()
                .forEach {
                    proto.put("${it.domain}>${it.name}".toUpperCase(), it)
                }
            log.info("registered processor \n${proto}")
        }

        val errBadRequest = ErrBadRequest("Invaild Request")
        val errAuthcationObject = ErrAuthcationObject("Invaild Authlication Object")
        suspend fun process(usr: Any?, query: String) = kotlin.run {
            if (!principalCheck(usr)) throw errAuthcationObject
            val conf = ConfigFactory.parseString(query)
            val dom = conf.root().keys.firstOrNull() ?: throw errBadRequest

            proto[dom.toUpperCase()]?.let { processor ->
                log.trace("try invoke $processor")
                processor.action.let {
                    it.isAccessible = true
                    if (processor.auth) {
                        it.invoke(processor.instance, conf.getConfig(dom), usr) as JsonNode
                    } else {
                        it.invoke(processor.instance, conf.getConfig(dom)) as JsonNode
                    }
                }
            }?.apply {
                processFields((this), conf.getConfig(dom))
            }
                ?: throw errBadRequest
        }

        suspend fun processConf(usr: Any?, conf: Config) = kotlin.run {
            if (!principalCheck(usr)) throw errAuthcationObject
            val dom = conf.root().keys.find { it.contains(">") } ?: throw errBadRequest
            val req = conf.getConfig(dom).let {
                conf.extract<String?>("wsid")?.let { ws ->
                    it.withValue("wsid", ConfigValueFactory.fromAnyRef(ws))
                }
            }
            proto[dom.toUpperCase()]?.let { processor ->
                log.trace("try invoke $processor")
                processor.action.let {
                    it.isAccessible = true
                    if (processor.auth) {
                        it.invoke(processor.instance, req, usr) as JsonNode
                    } else {
                        it.invoke(processor.instance, req) as JsonNode
                    }
                }
            }?.apply {
                processFields(this, conf.getConfig(dom))
            }
                ?: throw errBadRequest
        }

        private fun processFields(node: JsonNode, config: Config) {
            config.fields()?.let { key ->
                when {
                    node.isObject -> node.keepNested(key)
                    node.isArray -> node.asArray().forEach {
                        if (it.isObject) it.keepNested(key)
                    }
                }
            }

        }

        fun register(sets: MutableList<Class<out Enum<*>>>) {
            sets.forEach {
                register(it)
            }
        }

        //</editor-fold>
        //<editor-fold desc="Feature part">
        override val key = AttributeKey<processor>(this::class.simpleName ?: "processor")

        override fun install(
            pipeline: Application,
            configure: processor.() -> Unit
        ): processor {
            return this.apply(configure)
        }
        //</editor-fold>
        //<editor-fold desc="Helper defined">

        private var objectMapper: ObjectMapper = ObjectMapper()
        fun configObjectMapper(configrate: (ObjectMapper) -> ObjectMapper) {
            this.objectMapper = configrate.invoke(objectMapper)
        }


        @Target(AnnotationTarget.PROPERTY)
        annotation class LINK(val table: String, val toColumn: String = "")

        internal data class ProtoEnum(
            val pkg: String,
            val name: String,
            val clazz: KClass<*>
        )

        internal class ProcessorPool : LinkedHashMap<String, Processor>() {
            override fun toString(): String = this.map { "${it.key}:${it.value}" }.joinToString(";\n")
        }

        internal data class Processor(
            val source: String,
            val domain: String,
            val name: String,
            val action: Method,
            val instance: Any,
            val auth: Boolean = false
        ) {
            override fun toString(): String = buildString {
                append("${source}:${instance.toString()}>${action.name.toUpperCase()}:$auth=${buildString {
                    append(action.declaredAnnotations.joinToString { "@${it.annotationClass.simpleName}" })
                    append(" ")
                    append(action.name)
                    append("(")
                    append(action.parameters.joinToString { "${if (it.isVarArgs) "varargs" else ""}${it.name}:${it.type.canonicalName}" })
                    append("):")
                    append(action.returnType.simpleName)
                }}")

            }
        }

        internal fun KFunction<*>.isProcessor() = when {
            !this.isSuspend -> false
            this.findAnnotation<PROC>() == null -> false
            this.parameters.filter {
                it.kind == KParameter.Kind.VALUE &&
                        !it.type.isMarkedNullable &&
                        (it.type.isTypeString || it.type.isClass<Config>())
            }.let {
                it.size == 2 && it[0].type.isTypeString && it[1].type.isClass<Config>()
            } -> true
            else -> false
        }

//</editor-fold>

        //<editor-fold desc="Helper function">
        private val Nodes = listOf(JsonNode::class.java, ArrayNode::class.java, ObjectNode::class.java)

        internal fun Method.isProcessor() = when {

            this.isAnnotationPresent(PROC::class.java) &&
                    this.parameterCount == 1 &&
                    this.parameterTypes.contains(Config::class.java) &&
                    Nodes.contains(this.returnType)
            -> true
            this.isAnnotationPresent(PROC::class.java) &&
                    this.parameterCount == 2 &&
                    this.parameterTypes.contains(Config::class.java) &&
                    this@processor.principalCheck(this.parameterTypes.last()) &&
                    Nodes.contains(this.returnType)
            -> true

            else -> false
        }

        //</editor-fold>


        //<editor-fold desc="Extensions">


        fun Config.pageSize(k: String = "", default: Int = 5) =
            this.extract<Int?>("${if (k.isNotBlank()) "$k." else ""}S>") ?: default

        fun Config.page(k: String = "", default: Int = 0) =
            this.extract<Int?>("${if (k.isNotBlank()) "$k." else ""}P>") ?: default

        fun Config.fields(k: String = "") = run {
            val path = "${if (k.isNotBlank()) "$k." else ""}F>"
            if (this.hasPath(path)) {
                this.getConfig(path).root().render(ConfigRenderOptions.concise())
            } else {
                null
            }?.apply {
                // this.Logger.debug("field extract $this,${this@fields}")
            }?.let {
                objectMapper.readTree(it)
            }
        }

        internal final class AnyType

        internal fun KType.isClass(cls: KClass<*>): Boolean =
            if (cls == AnyType::class) this.classifier == null else this.classifier == cls

        internal inline fun <reified T> KType.isClass(): Boolean =
            if (T::class == AnyType::class) this.classifier == null else this.classifier == T::class

        internal val KType.isTypeString: Boolean inline get() = this.isClass(String::class) || this.isClass(java.lang.String::class)
        internal val KType.isTypeInt: Boolean inline get() = this.isClass(Int::class) || this.isClass(java.lang.Integer::class)
        internal val KType.isTypeLong: Boolean inline get() = this.isClass(Long::class) || this.isClass(java.lang.Long::class)
        internal val KType.isTypeByte: Boolean inline get() = this.isClass(Byte::class) || this.isClass(java.lang.Byte::class)
        internal val KType.isTypeShort: Boolean inline get() = this.isClass(Short::class) || this.isClass(java.lang.Short::class)
        internal val KType.isTypeChar: Boolean inline get() = this.isClass(Char::class) || this.isClass(java.lang.Character::class)
        internal val KType.isTypeBoolean: Boolean inline get() = this.isClass(Boolean::class) || this.isClass(java.lang.Boolean::class)
        internal val KType.isTypeFloat: Boolean inline get() = this.isClass(Float::class) || this.isClass(java.lang.Float::class)
        internal val KType.isTypeDouble: Boolean inline get() = this.isClass(Double::class) || this.isClass(java.lang.Double::class)
        internal val KType.isTypeByteArray: Boolean inline get() = this.isClass(ByteArray::class)
        internal val KType.isTypeCollection: Boolean inline get() = this.isClass(Collection::class)
        internal val KType.isTypeMap: Boolean inline get() = this.isClass(Map::class)

        /**
         * keep keys in key
         * @receiver JsonNode  source object
         * @param key JsonNode key object (only care of *keys*)
         */
        internal fun JsonNode.keepNested(key: JsonNode) {
            this.tryAsObject()?.also { src ->
                when {
                    key.isValueNode && key.isTextual -> src.retain(key.textValue())
                    key.isObject -> {
                        key.asObject().fieldNames().asSequence().toList().let { keys ->
                            src.retain(keys)
                            keys.forEach { k ->
                                src[k].keepNested(key[k])
                            }
                        }
                    }
                    key.isArray -> src.retain(key.asArray().elements().asSequence().map { it.textValue() }.toList())
                }
            }
        }

        internal fun JsonNode.asObject() = (this as ObjectNode)
        internal fun JsonNode.tryAsObject() = if (this.isObject) (this as ObjectNode) else null
        internal fun JsonNode.asArray() = (this as ArrayNode)
        internal fun JsonNode.tryAsArray() = if (this.isArray) (this as ArrayNode) else null
        //</editor-fold>


//</editor-fold>
    }
}


