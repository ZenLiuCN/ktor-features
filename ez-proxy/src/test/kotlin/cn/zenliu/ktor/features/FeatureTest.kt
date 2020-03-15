package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.proxy.*
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*
import kotlin.test.*

internal class FeatureTest {
	@Test
	fun testLiquibase() {
		withTestApplication({
			install(Proxy) {
				configHttpClient {
					HttpClient(OkHttp)
				}
				updateConfig {
					this.debug = true
				}
				addRoute(Proxy.ProxyFeature.ProxyRoute(
					"GET",
					"/proxy/{any...}",
					"http://39.100.6.164:9543/echo",
					{ url,_ -> url.replace("/proxy", "/") }
				))
			}
		}) {
			handleRequest(HttpMethod.Get, "/proxy") {
			}.apply {
				assertEquals(response.status()?.value,200)
				assertEquals(response.contentType(),ContentType.Text.Plain.withParameter("charset","UTF-8"))
			}
			handleRequest(HttpMethod.Get, "/proxy?b64=true") {
			}.apply {
				assertEquals(response.status()?.value,200)
				assertEquals(response.contentType(),ContentType.Text.Plain.withParameter("charset","UTF-8"))
			}
		}
	}

}
