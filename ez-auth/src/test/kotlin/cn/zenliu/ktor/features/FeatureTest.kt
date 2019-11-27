package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.auth.Auth
import cn.zenliu.ktor.features.auth.token
import io.ktor.application.*
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.http.HttpMethod
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.testing.*
import org.junit.jupiter.api.*

internal class FeatureTest {
	@Test
	fun testTemplate() {
		withTestApplication(
			{
				install(Auth){

				}
				install(Authentication){
					token {
						validate {tk->
							val tok=tk.token
							UserIdPrincipal(name = "1")
						}
					}
				}
				routing {
					authenticate {
						get("/"){
							call.respondText { "!" }
						}
					}
				}
			}
		) {
				val call=handleRequest(HttpMethod.Get,"/") {
					addHeader("authIt","2")
				}
				assert(call.response.content=="!")
		}
	}

}
