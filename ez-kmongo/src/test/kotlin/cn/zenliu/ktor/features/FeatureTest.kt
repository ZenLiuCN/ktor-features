package cn.zenliu.ktor.features




import cn.zenliu.ktor.features.datasource.*
import io.ktor.application.install

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test

internal class FeatureTest {
    @Test
    fun testTemplate() {
        withTestApplication(
                {
                install(Kmongo)
                }
        ) {
            assert(Kmongo.client!=null)

        }
    }

}
