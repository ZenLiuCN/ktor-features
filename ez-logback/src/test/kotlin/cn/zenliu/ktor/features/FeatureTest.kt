package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.properties.manager.*
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*
import org.slf4j.*

internal class FeatureTest {
    @Test
    fun testTemplate() {
        withTestApplication(
                {
                    install(PropertiesManager)
                }
        ) {
            LoggerFactory.getLogger("test")
                    .let {
                        assert(!it.isInfoEnabled)
	                    it.error("this should be displayed")
                    }
        }
    }

}
