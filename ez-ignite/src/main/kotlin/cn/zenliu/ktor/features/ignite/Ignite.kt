package cn.zenliu.ktor.features.ignite

import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import io.ktor.application.*
import org.apache.ignite.*
import org.apache.ignite.cluster.*
import org.apache.ignite.configuration.*
import org.slf4j.*

class IgniteManager {
	companion
	object IgniteFeature :
		FeatureTemplate.FeatureObjectTemplate<Application, IgniteFeature, IgniteFeature, IgniteFeature.IgniteConf>() {
		override val configClazz = IgniteFeature.IgniteConf::class


		@Properties("ignite")
		class IgniteConf(
			var client: Boolean = false,
			var conf: String = ""
		)

		private lateinit var pignite: Ignite
			private set;
		private var igniteConf: IgniteConfiguration? = null

		val igniteSafe by lazy { kotlin.runCatching { pignite }.getOrNull() }
		val ignite by lazy { pignite }
		/**
		 * initlination
		 * @param pipeline Application
		 * @param configure [@kotlin.ExtensionFunctionType] Function1<IgniteFeature, Unit>
		 * @return IgniteFeature
		 */
		override fun init(pipeline: Application, configure: IgniteFeature.() -> Unit): IgniteFeature {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
				pipeline.install(PropertiesManager)
			}
			config ?: throw Exception("Ignite configuration invalid!")
			this.apply(configure) //we can config Ignition first
			igniteConf?.let {
				logger.info("use program configuration of ignite")
				pignite = Ignition.start(igniteConf)
			} ?: config!!.apply {
				logger.info("use HOCON configuration of ignite")
				Ignition.setClientMode(client)
				pignite = conf.takeIf { it.isNotBlank() }?.let { Ignition.start(it) } ?: Ignition.start()
			}
			logger.info("ignite started")
			return this
		}

		/**
		 * this is use for set ignite configuration program, it can be only config once.
		 * and it will ignore config used by HOCON config
		 * @param conf IgniteConfiguration
		 */
		fun setConfig(conf: IgniteConfiguration) {
			if (igniteConf != null) throw Throwable("ignite configuration already set!")
			igniteConf = conf
		}

		private val logger = LoggerFactory.getLogger(this::class.java)

		//below are helpers
		object group {
			val cluster get() = ignite.cluster()
			val remotes get() = cluster.forRemotes()
			val random get() = cluster.forRandom()
			val local get() = cluster.forLocal()
			val oldest get() = cluster.forOldest()
			val youngest get() = cluster.forYoungest()
			val clients get() = cluster.forClients()
			val servers get() = cluster.forServers()
			val predict = { predict: (ClusterNode) -> Boolean ->
				cluster.forPredicate(predict)
			}
			val attribute ={key:String,value:Any? ->
				cluster.forAttribute(key,value)
			}
			//ext
			val IgniteCluster.local get() = forLocal()
			val IgniteCluster.remotes get() = forRemotes()
			val IgniteCluster.random get() = forRandom()
			val IgniteCluster.oldest get() = forOldest()
			val IgniteCluster.youngest get() = forYoungest()
			val IgniteCluster.clients get() = forClients()
			val IgniteCluster.servers get() = forServers()
		}
	}
}
