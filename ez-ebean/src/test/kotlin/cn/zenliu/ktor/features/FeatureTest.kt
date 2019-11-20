package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.datasource.HikariCP
import cn.zenliu.ktor.features.domain.UserUsecase
import cn.zenliu.ktor.features.ebean.EbeanDatabase
import io.ktor.application.install
import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Test
import javax.sql.DataSource

internal class FeatureTest {
    @Test
    fun testTemplate() {
        withTestApplication(
                {
                    install(HikariCP)
                    install(EbeanDatabase) {
                        this.configEbean {
                            this.dataSource = HikariCP.datasource as DataSource
                        }
                    }

                }
        ) {
            assert(EbeanDatabase.database.name.apply { println(this) }.isNotBlank())
            UserUsecase.create("xiachang").apply {
                println("$this,${this.id}")
            }

        }
    }

}