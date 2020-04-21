package cn.zenliu.ktor.features.camunda

import io.ktor.application.install
import io.ktor.server.testing.withTestApplication
import org.camunda.bpm.engine.ProcessEngineConfiguration
import org.junit.jupiter.api.Test

internal class CamundaEngineTest {
    @Test
    fun testCamundaEngine() {
        withTestApplication({
            install(CamundaEngine) {
                configure(
                        ProcessEngineConfiguration
                                .createStandaloneInMemProcessEngineConfiguration(),
                        {

                        }) {
                    repositoryService
                            .createDeployment()
                            .name("process_test")
                            .addInputStream("process.bpmn", this::class.java.getResourceAsStream("/process.bpmn"))
                            .enableDuplicateFiltering(true)
                            .deployWithResult().apply { println("test depoly $this ") }
                }
            }
        }) {
            CamundaEngine.runtime.createProcessInstanceByKey("process").execute()
        }
    }

}
