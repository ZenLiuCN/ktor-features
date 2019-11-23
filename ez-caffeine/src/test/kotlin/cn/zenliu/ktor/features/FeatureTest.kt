package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.cache.*
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*

internal class FeatureTest {
	@Test
	fun testTemplate() {
		withTestApplication(
			{
				install(CaffeineManager){
					buildCache<String,Any>("cache1"){
						it
					}
					buildCache<String,Any>("cache2")
				}
			}
		) {
			assert(CaffeineManager.caches.size==0)
			assert(CaffeineManager.loadingCaches.size==1)
			assert(CaffeineManager.asyncCaches.size==1)
			assert(CaffeineManager.asyncLoadingCaches.size==0)
			CaffeineManager.fetchLoadingCache<String,String>("cache1")?.apply {
				this.put("!23","123")
				this.get("!23").apply {
					assert(this=="123")
				}
				this.get("323").apply {
					assert(this=="323")
				}
			}
			CaffeineManager.fetchAsyncCache<String,String>("cache2")?.apply {
				this.synchronous().put("!23","123")
				this.synchronous().get("!23"){
					"321"
				}.apply {
					assert(this=="123")
				}
			}
		}
	}

}
