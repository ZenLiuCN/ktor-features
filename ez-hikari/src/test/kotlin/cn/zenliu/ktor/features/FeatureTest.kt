package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.datasource.Hikari
import io.ktor.application.install

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

internal class FeatureTest {
    @Test
    fun testTemplate() {
        withTestApplication(
                {
                install(Hikari)

                }
        ) {
            assert(Hikari.datasource.isRunning)
            assert(Hikari.other["mem2"]?.value?.isRunning?:false)
        }
    }

}
