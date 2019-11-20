package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.properties.manager.PropertiesManager
import io.github.config4k.toConfig
import io.ktor.application.install

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import org.slf4j.LoggerFactory

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
                        it.isInfoEnabled.apply(::println)
                        assert(!it.isInfoEnabled)
                    }
        }
    }

}