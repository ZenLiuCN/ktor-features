package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.datasource.Hikari
import cn.zenliu.ktor.features.datasource.Liquibase
import io.ktor.application.install

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

internal class FeatureTest {
    @Test
    fun testLiquibase() {
        withTestApplication(
                {
                    install(Hikari) {

                    }
                    install(Liquibase) {

                    }

                }
        ) {

        }
    }

}
