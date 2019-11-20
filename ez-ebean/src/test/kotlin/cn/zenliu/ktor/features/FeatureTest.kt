package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.datasource.Hikari
import cn.zenliu.ktor.features.domain.UserUsecase
import cn.zenliu.ktor.features.ebean.EbeanORM
import io.ktor.application.install
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import javax.sql.DataSource

internal class FeatureTest {
    @Test
    fun testTemplate() {
        withTestApplication(
                {
                    install(Hikari)
                    install(EbeanORM) {
                        this.configEbean {
                            this.dataSource = Hikari.datasource as DataSource
                        }
                    }

                }
        ) {
            assert(EbeanORM.database.name.apply { println(this) }.isNotBlank())
            UserUsecase.create("xiachang").apply {
                println("$this,${this.id}")
            }

        }
    }

}
