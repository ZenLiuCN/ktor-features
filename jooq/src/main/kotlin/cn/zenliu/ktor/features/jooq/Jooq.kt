package cn.zenliu.ktor.features.jooq


import cn.zenliu.ktor.features.properties.annotation.Properties
import cn.zenliu.ktor.features.template.FeatureTemplate
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.application.Application
import org.jooq.SQLDialect
import org.jooq.conf.Settings
import org.jooq.exception.ConfigurationException
import org.jooq.impl.DSL
import java.sql.DriverManager
import javax.sql.DataSource

class Jooq {
    companion
    object Feature :
        FeatureTemplate.FeatureObjectTemplate<Application, Feature, Feature, Feature.JooqConf>() {
        override val configClazz = Feature.JooqConf::class

        override fun init(pipeline: Application, configure: Feature.() -> Unit): Feature = run {
            config ?: throw ConfigurationException("JooqFactory not configurated")
            this.apply(configure)
            this
        }

        private val settings: Settings = Settings().apply {
            isExecuteLogging = false
        }

        private val ds by lazy {
            HikariDataSource(HikariConfig().apply {
                jdbcUrl = this@Feature.config!!.url
                this@Feature.config!!.username.let {
                    if (it.isNotBlank()) {
                        username = it
                    }
                }
                this@Feature.config!!.password.let {
                    if (it.isNotBlank()) {
                        password = it
                    }
                }
                maximumPoolSize = this@Feature.config!!.poolSize
                addDataSourceProperty(
                    "cachePrepStmts",
                    this@Feature.config!!.cachePrepStmts
                )
                addDataSourceProperty(
                    "prepStmtCacheSize",
                    this@Feature.config!!.prepStmtCacheSize
                )
                addDataSourceProperty(
                    "prepStmtCacheSqlLimit",
                    this@Feature.config!!.prepStmtCacheSqlLimit
                )
            })
        }
        /**
         * default DSLContext from configuration
         */
        val ctx by lazy {
            createDSL(
                ds,
                SQLDialect.valueOf(config!!.dialect)
            )
        }

        /**
         * create DSL from parameter
         * @param url String
         * @param user String
         * @param password String
         * @param driver String
         * @param dialect SQLDialect
         * @return [org.jooq.DSLContext]
         */
        @JvmStatic
        fun createDSL(url: String, user: String, password: String, driver: String, dialect: SQLDialect) = {
            Class.forName(driver)
            DSL.using(
                DriverManager.getConnection(url, user, password), dialect,
                settings
            )
        }.invoke()

        /**
         * create DSL from other datasource
         * @param ds DataSource
         * @param dialect SQLDialect
         * @return [DSLContext]
         */
        @JvmStatic
        fun createDSL(ds: DataSource, dialect: SQLDialect) = DSL.using(
            ds, dialect,
            settings
        )!!

        @Properties("jooq")
        class JooqConf(
            var dialect: String,
            var url: String,
            var username: String = "",
            var password: String = "",
            var poolSize: Int = 10,
            var cachePrepStmts: Boolean = true,
            var prepStmtCacheSize: Int = 250,
            var prepStmtCacheSqlLimit: Int = 2048
        )


    }
}



