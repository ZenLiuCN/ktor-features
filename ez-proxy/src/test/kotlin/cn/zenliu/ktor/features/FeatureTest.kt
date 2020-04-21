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
	/*@Test
	fun testProxy() {
		withTestApplication({
			install(Proxy) {
				configHttpClient {
					HttpClient(OkHttp){

					}
				}
				updateConfig {
					this.debug = true
				}
				addRoute(Proxy.ProxyFeature.ProxyRoute(
					HttpMethod.Get,
					"/proxy/{any...}",
					"http://whatismyip.akamai.com",
					{ uri,route ->
						uri.replace(route.substringBefore("/{"), if(uri.endsWith("/")) "/" else "")
					}
				))
			}
		}) {
			handleRequest(HttpMethod.Get, "/proxy") {
			}.apply {
				assertEquals(200,response.status()?.value)
				assertEquals(ContentType.Text.Html,response.contentType())
			}
			handleRequest(HttpMethod.Get, "/proxy?b64=true") {
			}.apply {
				assertEquals(200,response.status()?.value)
				assertEquals(ContentType.Text.Html,response.contentType())
			}
		}
	}*/

}
