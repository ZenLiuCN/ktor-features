package cn.zenliu.ktor.features.proxy

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import org.slf4j.*

import kotlin.reflect.*

typealias ResponseProcessor = PipelineContext<Unit, ApplicationCall>.(response:HttpResponse) -> Unit
typealias RequestInterceptor = HttpRequestBuilder.() -> Unit
typealias UriGenerator = (uri: String, route: String) -> String?
typealias HeaderGenerator = (StringValues) -> StringValues

class Proxy private constructor() {
	companion
	object ProxyFeature : FeatureTemplate.FeatureObjectTemplate<Application, ProxyFeature, ProxyFeature, ProxyFeature.ProxyConf>() {
		override val configClazz: KClass<ProxyConf> = ProxyConf::class
		override fun init(pipeline: Application, configure: ProxyFeature.() -> Unit): ProxyFeature {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
				pipeline.install(PropertiesManager)
			}
			config ?: throw Exception("Proxy configuration invalid!")
			this.apply(configure)
			generateRoute()
			install(pipeline)
			return this
		}

		@Properties("proxy")
		class ProxyConf(
			var enable: Boolean = true,
			var debug: Boolean = false,
			var route: MutableSet<ProxyRouteDefine> = mutableSetOf()
		)

		data class ProxyRouteDefine(
			val method: String = "GET",
			val route: String,
			val domain: String,
			val uriGenerator: String = "direct",
			val requestHeaderGenerator: String? = null,
			val responseHeaderGenerator: String? = null,
			val requestIntercepter: String? = null,
			val responseProcessor: String? = null
		) {
			fun toRoute() = ProxyRoute(
				HttpMethod.parse(method), route, domain,
				uriGenerator.let { uriGenerators[it] ?: throw Throwable("uriGenerator of $it is not in registry") },
				requestHeaderGenerator?.let {
					headerGenerators[it] ?: throw Throwable("headerGenerator of $it is not in registry")
				},
				responseHeaderGenerator?.let {
					headerGenerators[it] ?: throw Throwable("headerGenerator of $it is not in registry")
				},
				requestIntercepter?.let {
					requestIntercepters[it] ?: throw Throwable("requestIntercepter of $it is not in registry")
				},
				responseProcessor?.let {
					responseProcessors[it] ?: throw Throwable("responseProcessor of $it is not in registry")
				}
			)
		}

		data class ProxyRoute(
			val method: HttpMethod ,
			val route: String,
			val domain: String,
			val uriGenerator: UriGenerator,
			val requestHeaderGenerator: HeaderGenerator? = null,
			val responseHeaderGenerator: HeaderGenerator? = null,
			val requestIntercepter: RequestInterceptor? = null,
			val responseProcessor: ResponseProcessor? = null
		)
		private var installed:Boolean=false
		private val route:MutableSet<ProxyRoute> = mutableSetOf()
		private val uriGenerators: MutableMap<String, UriGenerator> = mutableMapOf(
			"prefix" to { uri, route -> uri.replace(route.substringBefore("/{"), "/") },
			"direct" to { uri, _ -> uri }
		)
		private val logger = LoggerFactory.getLogger(this::class.java)
		private val headerGenerators: MutableMap<String, HeaderGenerator> = mutableMapOf()
		private val requestIntercepters: MutableMap<String, RequestInterceptor> = mutableMapOf()
		private val responseProcessors: MutableMap<String, ResponseProcessor> = mutableMapOf()
		fun generateRoute(){
			if(installed)throw  Throwable("proxy route already installed")
			loggingDebug { "registry of headerGenerators =>${headerGenerators.keys}" }
			loggingDebug { "registry of uriGenerators =>${uriGenerators.keys}" }
			loggingDebug { "registry of requestIntercepters =>${requestIntercepters.keys}" }
			loggingDebug { "registry of responseProcessors =>${responseProcessors.keys}" }
			route.addAll(config!!.route.map { it.toRoute() })
			loggingDebug { "routes are ${route}"  }
		}
		fun registerResponseProcessor(name: String, act: ResponseProcessor) {
			if(installed)throw  Throwable("proxy route already installed")
			responseProcessors.put(name, act)
		}

		fun registerUriGenerator(name: String, act: UriGenerator) {
			if(installed)throw  Throwable("proxy route already installed")
			uriGenerators.put(name, act)
		}

		fun registerHeaderGenerator(name: String, act: HeaderGenerator) {
			if(installed)throw  Throwable("proxy route already installed")
			headerGenerators.put(name, act)
		}

		fun registerRequestIntercepter(name: String, act: RequestInterceptor) {
			if(installed)throw  Throwable("proxy route already installed")
			requestIntercepters.put(name, act)
		}

		fun updateConfig(act: ProxyConf.() -> Unit) = act.invoke(this.config!!)
		fun addRoute(route:ProxyRoute){
			this.route.add(route)
		}
		fun configHttpClient(act: () -> HttpClient) {
			if(installed)throw  Throwable("proxy route already installed")
			this.httpGenerator = act
		}

		fun install(app: Application) {
			if (config!!.enable) {
				installed=true
				app.routing {
					ifDebug { trace { application.log.info(it.buildText()) } }
					route.forEach {
						route(it.route, it.method, {
							handle(doProxy(it))
						})
					}
				}
			}
		}

		private fun loggingDebug(act: () -> String) = config!!.debug.takeIf { it }?.let { logger.info(act.invoke()) }
		private fun ifDebug(act: () -> Unit) = config!!.debug.takeIf { it }?.let { act.invoke() }
		private var httpGenerator: () -> HttpClient = { HttpClient() }
		private val client by lazy { httpGenerator() }

		private fun doProxy(px: ProxyRoute): PipelineInterceptor<Unit, ApplicationCall> = {
			val target = call.request.uri.let { px.uriGenerator.invoke( it,px.route) }
			if (target == null) {
				ifDebug { "${call.request.uri} filter to null by ${px}" }
				call.respond(HttpStatusCode.NotFound)
			} else {
				val targetUrl="${px.domain}$target".replace("//","/").replace(":/","://")
				val response = client.request<HttpResponse>(targetUrl) {
					method = px.method
					headers.appendAll(
						call.request.headers.let { px.requestHeaderGenerator?.invoke(it) ?: it }.filter { key, _ ->
							!key.equals(HttpHeaders.ContentType, ignoreCase = true)
								&& !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
								&& !key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)
								&& !key.equals(HttpHeaders.Upgrade, ignoreCase = true)
						})
					ifDebug { application.environment.log.info("proxy to $targetUrl with ${headers.build()}  by ${px}") }
					val ctxLen = call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
					val ctxType = call.request.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
					if (ctxLen != null && ctxLen > 0) {
						contentType(ctxType ?: ContentType.Any)
						body = call.receiveChannel()
					}
					px.requestIntercepter?.invoke(this)
				}
				px.responseProcessor?.invoke(this, response) ?: kotlin.run {
					val proxiedHeaders = response.headers
					val contentType = proxiedHeaders[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
					val ctxLen = proxiedHeaders[HttpHeaders.ContentLength]?.toLongOrNull()
					call.respond(object : OutgoingContent.WriteChannelContent() {
						override val contentLength: Long? = ctxLen
						override val contentType: ContentType? = contentType
						override val headers: Headers = Headers.build {
							appendAll(proxiedHeaders
								.let { px.responseHeaderGenerator?.invoke(it) ?: it }
								.filter { key, _ ->
									!key.equals(HttpHeaders.ContentType, ignoreCase = true)
										&& !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
										&& !key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)
										&& !key.equals(HttpHeaders.Upgrade, ignoreCase = true)
								})
						}
						override val status: HttpStatusCode? = response.status
						override suspend fun writeTo(channel: ByteWriteChannel) {
							response.content.copyAndClose(channel)
						}
					})
				}
			}
		}
	}
}
