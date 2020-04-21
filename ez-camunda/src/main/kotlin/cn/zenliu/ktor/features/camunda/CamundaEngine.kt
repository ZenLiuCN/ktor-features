package cn.zenliu.ktor.features.camunda

import cn.zenliu.ktor.features.properties.manager.PropertiesManager
import cn.zenliu.ktor.features.FeatureTemplate
import cn.zenliu.ktor.features.properties.annotation.Properties
import io.ktor.application.Application
import io.ktor.application.ApplicationStopPreparing
import io.ktor.application.install
import org.camunda.bpm.engine.ProcessEngine
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties
import java.time.Duration
import kotlin.reflect.KClass

class CamundaEngine private constructor() {
	companion object CamundaFeature : FeatureTemplate.FeatureObjectTemplate<Application, CamundaFeature, CamundaFeature, CamundaFeature.CamundaConf>() {
		override val configClazz: KClass<CamundaConf> = CamundaConf::class
		override fun init(pipeline: Application, configure: CamundaFeature.() -> Unit): CamundaFeature {
			pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
				pipeline.install(PropertiesManager)
			}
			//config ?: throw Exception("Camunda configuration invalid!")
			this.apply(configure)
			pipeline.environment.monitor.subscribe(ApplicationStopPreparing) {
				close()
			}
			return this
		}

		@Properties("camunda")
		class CamundaConf

		private lateinit var procEngine: ProcessEngine
		val engine by lazy { procEngine }
		val reopsitory by lazy { procEngine.repositoryService }
		val runtime by lazy { procEngine.runtimeService }
		val task by lazy { procEngine.taskService }
		val authorization by lazy { procEngine.authorizationService }
		val case by lazy { procEngine.caseService }
		val decision by lazy { procEngine.decisionService }
		val externalTask by lazy { procEngine.externalTaskService }
		val filter by lazy { procEngine.filterService }
		val form by lazy { procEngine.formService }
		val history by lazy { procEngine.historyService }
		val identity by lazy { procEngine.identityService }
		val management by lazy { procEngine.managementService }

		/**
		 *
		 * @param conf ProcessEngineConfiguration? your self predefine configuration
		 * @param doWithEngine [@kotlin.ExtensionFunctionType] Function1<ProcessEngine, Unit>?
		 * @param config [@kotlin.ExtensionFunctionType] Function1<ProcessEngineConfiguration, Unit>
		 * @return Unit
		 */
		fun configure(
				conf: ProcessEngineConfiguration? = null,
				config: ProcessEngineConfiguration.() -> Unit,
				doWithEngine: ProcessEngine.() -> Unit
		) {
			procEngine = (conf ?: ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration())
					.apply(config)
					.buildProcessEngine()
			doWithEngine.invoke(procEngine)
		}

		/**
		 * create new Engine
		 * @param replace Boolean replace inner default engine
		 * @param conf ProcessEngineConfiguration?
		 * @param config [@kotlin.ExtensionFunctionType] Function1<ProcessEngineConfiguration, Unit>
		 * @return ProcessEngine
		 */
		fun newEngine(
				replace: Boolean = true,
				conf: ProcessEngineConfiguration? = null,
				config: ProcessEngineConfiguration.() -> Unit
		): ProcessEngine {
			if (replace && this::procEngine.isInitialized) procEngine.close()
			val engine = (conf ?: ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration())
					.apply(config)
					.buildProcessEngine()
			if (replace) {
				procEngine = engine
			}
			return engine
		}

		fun close() {
			if (this::procEngine.isInitialized) procEngine.close()
		}

	}
}
