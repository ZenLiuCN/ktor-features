package cn.zenliu.ktor.features.datasource

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import com.zaxxer.hikari.*
import io.ktor.application.*
import java.time.*
import kotlin.reflect.*

/**
 * HikariCP feature
 *
 * configuration example:
 * ```HOCON
 * datasource{
 *   url: "jdbc:sqlite::memory:"
 *   extra: {
 *     mem2: {
 *       url: "jdbc:sqlite::memory:"
 *	   }
 *	   full: {
 *	     url: "jdbc:sqlite::memory:"
 *	     username: ''
 *       password: ''
 *       poolSize: ''
 *       cachePrepStmts: true
 *       prepStmtCacheSize: 250
 *       prepStmtCacheSqlLimit: 2048
 *       isolateInternalQueries: ''
 *       maxLifetime: 60m
 *       idleTimeout: 10m
 *       connectionTimeout: 30s
 *       validationTimeout: 5s
 *       initializationFailTimeout: 1ms
 *       readOnly: false
 *       autoCommit: true
 *       allowPoolSuspension: false
 *       schema: ''
 *       catalog: ''
 *       connectionInitSql: ''
 *       connectionTestQuery: ''
 *       transactionIsolation: null
 *       registerMbeans: false
 *       minimumIdle: 1
 *	   }
 *	}}
 * ```
 * @see HikariDataSource
 */
class Hikari private constructor() {
	companion
	object HikariCPFeature : FeatureTemplate.FeatureObjectTemplate<Application, HikariCPFeature, HikariCPFeature, HikariCPFeature.HikariConf>() {
		override val configClazz: KClass<HikariConf> = HikariConf::class
		override fun init(pipeline: Application, configure: HikariCPFeature.() -> Unit): HikariCPFeature {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key){
				pipeline.install(PropertiesManager)
			}
			config ?: throw Exception("hikari configuration invalid!")
			this.apply(configure)
			return this
		}
		@Properties("datasource")
		class HikariConf(
				var url: String,
				var username: String? = null,
				var password: String? = null,
				var poolSize: Int = 1,
				var cachePrepStmts: Boolean = true,
				var prepStmtCacheSize: Int = 250,
				var prepStmtCacheSqlLimit: Int = 2048,
				var isolateInternalQueries: Boolean = false,
				var maxLifetime: Duration = Duration.ofMinutes(30),
				var idleTimeout: Duration = Duration.ofMinutes(10),
				var connectionTimeout: Duration = Duration.ofSeconds(30),
				var validationTimeout: Duration = Duration.ofSeconds(5),
				var initializationFailTimeout: Long = 1,
				var readOnly: Boolean = false,
				var autoCommit: Boolean = true,
				var allowPoolSuspension: Boolean = false,
				var schema: String = "",
				var catalog: String = "",
				var connectionInitSql: String = "",
				var connectionTestQuery: String = "",
				var transactionIsolation: String? = null,
				var registerMbeans: Boolean = false,
				var minimumIdle: Int=1,
				var extra: MutableMap<String, HikariConf> = mutableMapOf()
		)

		/**
		 * HikariCP Datasource
		 */
		val datasource by lazy {
			generateDatasource(this.config!!)
		}
		/**
		 * Other Datasource config by extra
		 */
		val other by lazy {
			this.config!!.extra.map { (k, v) ->
				k to lazy { generateDatasource(v) }
			}.toMap()
		}
		private fun generateDatasource(conf: HikariConf) = HikariDataSource(HikariConfig().apply {
			jdbcUrl = conf.url
			username=conf.username
			isIsolateInternalQueries=conf.isolateInternalQueries
			maxLifetime=conf.maxLifetime.toMillis()
			idleTimeout=conf.idleTimeout.toMillis()
			connectionTimeout=conf.connectionTimeout.toMillis()
			initializationFailTimeout=conf.initializationFailTimeout
			isReadOnly=conf.readOnly
			isAutoCommit=conf.autoCommit
			isAllowPoolSuspension=conf.allowPoolSuspension
			isRegisterMbeans=conf.registerMbeans
			transactionIsolation=conf.transactionIsolation
			validationTimeout=conf.validationTimeout.toMillis()
			minimumIdle=conf.minimumIdle
			schema=conf.schema.takeIf { it.isNotBlank() }
			catalog=conf.catalog.takeIf { it.isNotBlank() }
			connectionInitSql=conf.connectionInitSql.takeIf { it.isNotBlank() }
			connectionTestQuery=conf.connectionTestQuery.takeIf { it.isNotBlank() }
			password=conf.password
			maximumPoolSize = conf.poolSize
			addDataSourceProperty(
				"cachePrepStmts",
				conf.cachePrepStmts
			)
			addDataSourceProperty(
				"prepStmtCacheSize",
				conf.prepStmtCacheSize
			)
			addDataSourceProperty(
				"prepStmtCacheSqlLimit",
				conf.prepStmtCacheSqlLimit
			)
		})
	}
}
