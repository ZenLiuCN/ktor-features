package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.datasource.HikariCP
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
                install(HikariCP)

                }
        ) {
            assert(HikariCP.datasource.isRunning)
            assert(HikariCP.other["mem2"]?.value?.isRunning?:false)
        }
    }

}