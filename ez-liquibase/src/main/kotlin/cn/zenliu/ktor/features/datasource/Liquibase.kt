package cn.zenliu.ktor.features.datasource

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import io.ktor.application.*
import liquibase.*
import liquibase.Liquibase
import liquibase.configuration.*
import liquibase.database.*
import liquibase.database.jvm.*
import liquibase.logging.*
import liquibase.resource.*
import java.io.*
import java.sql.*
import javax.sql.*
import kotlin.reflect.*


class Liquibase private constructor() {
    companion
    object LiquibaseFeature : FeatureTemplate.FeatureObjectTemplate<Application, LiquibaseFeature, LiquibaseFeature, LiquibaseFeature.LiquibaseConf>() {
        override val configClazz: KClass<LiquibaseConf> = LiquibaseConf::class
        override fun init(pipeline: Application, configure: LiquibaseFeature.() -> Unit): LiquibaseFeature {
            pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
               pipeline.install(PropertiesManager)
            }
            hasHikari = pipeline.attributes.allKeys.find { it.name=="HikariCPFeature" }!=null
            config ?: throw Exception("Liquibase configuration invalid!")
            this.apply(configure)
            validate()
            if (config!!.autoStart) {
                doLiquibaseJob()
            }
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
		private var resourceAccessor:ResourceAccessor=CompositeResourceAccessor(FileSystemResourceAccessor(),ClassLoaderResourceAccessor())
        private var hasHikari: Boolean = true
        private lateinit var ds: DataSource
        private fun validate() {
            if (!hasHikari && !this::ds.isInitialized) {
                throw Throwable("HikariCP feature not installed, plz assign datasource by Liquibase.setDatasource")
            } else if (hasHikari && config!!.datasource.isNotEmpty() && !hikariHas(config!!.datasource)) {
                throw Throwable("HikariCP feature is installed and assign ${config!!.datasource} which not found in Hikari")
            } else if (hasHikari && config!!.other.isNotEmpty() && !checkHikari()) {
                throw Throwable("HikariCP feature is installed, but some LiquibaseConf.other has invalid datasource in Hikari")
            }
        }

        private fun hikariHas(ds: String) = Hikari.other.keys.contains(ds)
        private fun checkHikari() = config!!.other
                .map { it.datasource }
                .let {
                    !it.contains("") && Hikari.other.keys.containsAll(it)
                }

        private fun getDatabaseProductName() =
                ds.connection.use {
                    val db = DatabaseFactory
                            .getInstance()
                            .findCorrectDatabaseImplementation(JdbcConnection(it))
                    db.runCatching {
                        this.databaseProductName
                    }.onFailure {
                        db.close()
                    }.getOrNull().apply {
                        if (!it.autoCommit) it.rollback()
                    }
                } ?: "unknown"


        fun setDatasource(ds: DataSource) {
            this.ds = ds
        }

	    /**
	     * add self define resourceAccessor
	     * @param ra ResourceAccessor
	     */
		fun setResourceAccessor(ra: ResourceAccessor){
			this.resourceAccessor=ra
		}
        private fun getConnection(conf: LiquibaseConf) = when {
            hasHikari && conf.datasource.isNotBlank() -> Hikari.other[conf.datasource]?.value?.connection
            hasHikari && conf.datasource.isBlank() && !this::ds.isInitialized -> Hikari.datasource.connection
            !hasHikari && conf.datasource.isBlank() && this::ds.isInitialized -> ds.connection
            else -> null
        }

        private val log = LogService.getLog(LiquibaseFeature::class.java)
        fun doLiquibaseJob() {
            if (!config!!.enabled) return
            getConnection(config!!)?.use {
                createLiquibase(it, config!!).use {
                    this.generateRollbackFile(config!!)
                    this.performUpdate(config!!)
                }
            }
            config!!.other.forEach { cf ->
                if (!cf.enabled) return@forEach
                getConnection(cf)?.use {
                    createLiquibase(it, cf).use {
                        this.generateRollbackFile(cf)
                        this.performUpdate(cf)
                    }
                }
            }
        }

        private fun Liquibase.generateRollbackFile(conf: LiquibaseConf) {
            conf.rollbackFile.takeIf { it.isNotBlank() }?.let {
                File(it).outputStream().use {
                    OutputStreamWriter(it,
                            LiquibaseConfiguration
                                    .getInstance()
                                    .getConfiguration(GlobalConfiguration::class.java)
                                    .outputEncoding
                    ).use {
                        if (conf.tag.isBlank()) {
                            futureRollbackSQL(Contexts(), LabelExpression(conf.labels), it)
                        } else {
                            futureRollbackSQL(conf.tag, Contexts(), LabelExpression(conf.labels), it)
                        }
                    }
                }
            }
        }

        private fun Liquibase.performUpdate(conf: LiquibaseConf) {
            conf.clearCheckSums.takeIf { it }?.let { this.clearCheckSums() }
            conf.let {
                when {
                    it.testRollbackOnUpdate && it.tag.isNotEmpty() -> this
                            .updateTestingRollback(
                                    it.tag, Contexts(), LabelExpression(it.labels))
                    it.testRollbackOnUpdate && it.tag.isEmpty() -> this
                            .updateTestingRollback(Contexts(), LabelExpression(it.labels))
                    !it.testRollbackOnUpdate && it.tag.isNotEmpty() -> this
                            .update(
                                    it.tag, Contexts(), LabelExpression(it.labels))
                    !it.testRollbackOnUpdate && it.tag.isEmpty() -> this
                            .update(Contexts(), LabelExpression(it.labels))
                }
            }
        }

        private fun createLiquibase(c: Connection?, conf: LiquibaseConf) =
            Liquibase(
                    conf.changeLog,
	                 resourceAccessor,
                    createDatabase(c, resourceAccessor, conf)
            ).apply {
                this.isIgnoreClasspathPrefix = conf.ignoreClasspathPrefix
                conf.parameter.takeIf { it.isNotEmpty() }?.forEach { k, v ->
                    this.setChangeLogParameter(k, v)
                }
                conf.dropFirst.takeIf { it }?.let {
                    this.dropAll()
                }
            }


        private fun createDatabase(c: Connection?, ra: ResourceAccessor, conf: LiquibaseConf) =
                (c?.let { JdbcConnection(c) }
                        ?: run {
                            log.warning(LogType.LOG,
                                    "Null connection returned by liquibase datasource. Using offline unknown database")
                            OfflineConnection("offline:unknown", ra)
                        })
                        .let { DatabaseFactory.getInstance().findCorrectDatabaseImplementation(it) }
                        .apply {
                            conf.defaultSchema.validateRun {
                                if (this.supportsSchemas()) {
                                    this.defaultSchemaName = it
                                } else if (this.supportsCatalogs()) {
                                    this.defaultCatalogName = it
                                }
                            }
                            conf.liquibaseSchema.validateRun {
                                if (this.supportsSchemas()) {
                                    this.liquibaseSchemaName = it
                                } else if (this.supportsCatalogs()) {
                                    this.liquibaseCatalogName = it
                                }
                            }
                            conf.liquibaseTablespace.validateRun(this.supportsTablespaces()) {
                                this.liquibaseTablespaceName = it
                            }
                            conf.databaseChangeLogLockTable.validateRun {
                                this.databaseChangeLogLockTableName = it
                            }
                            conf.databaseChangeLogTable.validateRun {
                                this.databaseChangeLogTableName = it
                            }
                        }

        private fun String.validateRun(condition: Boolean = true, act: (it: String) -> Unit) =
                this
                        .takeIf { it.isNotBlank() && condition }
                        ?.let(act)

        private fun Liquibase.use(act: Liquibase.() -> Unit) {
            try {
                act.invoke(this)
            } finally {
                this.database?.close()
            }
        }
    }
}
