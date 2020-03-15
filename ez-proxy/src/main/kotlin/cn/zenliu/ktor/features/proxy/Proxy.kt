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

import kotlin.reflect.*


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
			install(pipeline)
			return this
		}

		@Properties("proxy")
		class ProxyConf(
			var enable: Boolean = true,
			var debug: Boolean = false,
			val engine: String = "OKHTTP",
			var route: MutableSet<ProxyRoute> = mutableSetOf()
		)

		data class ProxyRoute(
			val method: String = "GET",
			val route: String,
			val prefix: String,
			val uriGenerator: (String) -> String? = { uri: String -> uri },
			val requestHeaderGenerator: (StringValues) -> StringValues = { headers: StringValues -> headers },
			val responseHeaderGenerator: (StringValues) -> StringValues = { headers: StringValues -> headers }
		) {
			fun getHttpMethod() = HttpMethod.parse(method)
		}

		fun config(act: ProxyConf.() -> Unit) = act.invoke(this.config!!)
		fun configHttpClient(act: () -> HttpClient) {
			this.httpGenerator = act
		}
		fun install(app: Application) {
			if (config!!.enable) {
				app.routing {
					ifDebug { trace { application.log.info(it.buildText()) } }
					config!!.route.forEach {
						val mt = it.getHttpMethod()
						route(it.route, mt, {
							handle(doProxy(it))
						})
					}
				}
			}
		}

		private fun ifDebug(act: () -> Unit) = config!!.debug.takeIf { it }?.let { act.invoke() }
		private var httpGenerator: () -> HttpClient = { HttpClient() }
		private val client by lazy { httpGenerator() }

		private fun doProxy(px: ProxyRoute): PipelineInterceptor<Unit, ApplicationCall> = {
			val target = call.request.uri.let { px.uriGenerator(it) }
			if (target == null) {
				ifDebug { application.environment.log.info("${call.request.uri} filter to null by ${px}") }
				call.respond(HttpStatusCode.NotFound)
			} else {
				val response = client.request<HttpResponse>("${px.prefix}$target") {
					method = px.getHttpMethod()
					headers.appendAll(px.requestHeaderGenerator(call.request.headers).filter { key, _ ->
						!key.equals(HttpHeaders.ContentType, ignoreCase = true)
							&& !key.equals(HttpHeaders.ContentLength, ignoreCase = true)
							&& !key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)
							&& !key.equals(HttpHeaders.Upgrade, ignoreCase = true)
					})
					ifDebug { application.environment.log.info("proxy to ${px.prefix}$target with $headers  by ${px}") }
					val ctxLen=call.request.headers[HttpHeaders.ContentLength]?.toLongOrNull()
					val ctxType=call.request.headers[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
					if(ctxLen!=null && ctxLen>0){
						contentType(ctxType?: ContentType.Any)
						body=call.receiveChannel()
					}
				}
				val proxiedHeaders = response.headers
				val contentType = proxiedHeaders[HttpHeaders.ContentType]?.let { ContentType.parse(it) }
				val ctxLen = proxiedHeaders[HttpHeaders.ContentLength]?.toLongOrNull()
				call.respond(object : OutgoingContent.WriteChannelContent() {
					override val contentLength: Long? = ctxLen
					override val contentType: ContentType? = contentType
					override val headers: Headers = Headers.build {
						appendAll(px.responseHeaderGenerator(proxiedHeaders)
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
