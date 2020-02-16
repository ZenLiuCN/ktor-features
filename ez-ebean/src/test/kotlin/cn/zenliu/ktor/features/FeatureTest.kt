package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.domain.*
import cn.zenliu.ktor.features.ebean.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*
import java.time.*
import kotlin.concurrent.*

internal class FeatureTest {
	@Test
	fun testTemplate() {
		withTestApplication({
			this.main()
		}) {
			assert(EbeanORM.database.name.apply { println(this) }.isNotBlank())
			UserUsecase.create("somebody").apply {
				println("$this,${this.id}")
			}
			(0..100).map {
				thread {
					with(handleRequest(HttpMethod.Put, "/user/${Instant.now().epochSecond}")) {
						assert(response.status() == HttpStatusCode.OK)
					}
				}
			}.forEach {
				it.join()
			}
			with(handleRequest(HttpMethod.Put, "/user/${Instant.now().epochSecond}")) {
				assert(response.status() == HttpStatusCode.OK)
			}
			Thread.sleep(1500) //wait for shutdown
			UserEntity.all().toList().let {
				assert(it.size == 103)
			}
		}
	}

}
