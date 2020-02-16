package cn.zenliu.ktor.features




import cn.zenliu.ktor.features.datasource.*
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*

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
