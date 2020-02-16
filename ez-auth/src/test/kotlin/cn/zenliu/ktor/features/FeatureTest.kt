package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.auth.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
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
