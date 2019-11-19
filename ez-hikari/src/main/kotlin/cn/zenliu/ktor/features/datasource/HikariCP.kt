package cn.zenliu.ktor.features.datasource

import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.template.*
import com.zaxxer.hikari.*
import io.ktor.application.*
import kotlin.reflect.*

class HikariCP {
	companion
	object Feature : FeatureTemplate.FeatureObjectTemplate<Application, Feature, Feature, Feature.HikariConf>() {
		override val configClazz: KClass<HikariConf> = HikariConf::class

		override fun init(pipeline: Application, configure: Feature.() -> Unit): Feature {
			config ?: throw Exception("datasource url not set!")
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
			var extra: MutableMap<String, HikariConf> = mutableMapOf()
		)

		val datasource by lazy {
			generateDatasource(this.config!!)
		}
		val other by lazy {
			this.config!!.extra.map { (k, v) ->
				k to lazy { generateDatasource(v) }
			}.toMap()
		}

		private fun generateDatasource(conf: HikariConf) = HikariDataSource(HikariConfig().apply {
			jdbcUrl = conf.url
			conf.username?.let {
				if (it.isNotBlank()) {
					username = it
				}
			}
			conf.password?.let {
				if (it.isNotBlank()) {
					password = it
				}
			}
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
