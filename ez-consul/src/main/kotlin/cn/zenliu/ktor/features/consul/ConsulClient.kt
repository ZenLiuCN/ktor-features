package cn.zenliu.ktor.features.datasource

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import io.ktor.application.*
import java.io.*
import java.sql.*
import javax.sql.*
import kotlin.reflect.*


class ConsulClient private constructor() {
    companion
    object ConsulFeature : FeatureTemplate.FeatureObjectTemplate<Application, ConsulFeature, ConsulFeature, ConsulFeature.LiquibaseConf>() {
        override val configClazz: KClass<LiquibaseConf> = LiquibaseConf::class
        override fun init(pipeline: Application, configure: ConsulFeature.() -> Unit): ConsulFeature {
            pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
               pipeline.install(PropertiesManager)
            }

            config ?: throw Exception("Consul Feature configuration invalid!")
            this.apply(configure)

            return this
        }

        @Properties("liquibase")
        class LiquibaseConf(
                var autoStart: Boolean=true,
                var enabled: Boolean=true,
                var dropFirst: Boolean = false,
                var clearCheckSums: Boolean = false,
                var labels: String? = null,
                var tag: String = "",
                var changeLog: String = "classpath:db-changelog.yaml",
                var ignoreClasspathPrefix: Boolean = true,
                var testRollbackOnUpdate: Boolean = false,
                var databaseChangeLogTable: String = "LIQUIBASE_LOG_TAB",
                var databaseChangeLogLockTable: String = "LIQUIBASE_LOCK_TAB",
                var rollbackFile: String = "",
                var defaultSchema: String = "",
                var liquibaseSchema: String = "",
                var liquibaseTablespace: String = "",
                var parameter: MutableMap<String, String> = mutableMapOf(),
                var datasource: String = "",
                var other: Set<LiquibaseConf> = emptySet()
        )


        private lateinit var ds: DataSource

    }
}
