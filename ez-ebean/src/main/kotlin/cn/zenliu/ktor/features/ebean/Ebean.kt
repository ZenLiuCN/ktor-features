package cn.zenliu.ktor.features.ebean

import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.template.*
import io.ktor.application.*
import kotlin.reflect.*


class Ebean {
	companion
	object Feature : FeatureTemplate.FeatureObjectTemplate<Application, Feature, Feature, Feature.EbeanConf>() {
		override val configClazz: KClass<EbeanConf> = EbeanConf::class

		override fun init(pipeline: Application, configure: Feature.() -> Unit): Feature {
			config ?: throw Exception("Ebean config not set!")
			this.apply(configure)

			return this
		}

		@Properties("ebean")
		class EbeanConf(
			var useDatasource: Boolean
		)


	}
}
