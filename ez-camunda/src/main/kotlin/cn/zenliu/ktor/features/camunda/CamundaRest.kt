package cn.zenliu.ktor.features.camunda

import java.lang.ref.SoftReference


class CamundaHistoryApi private constructor() {
    fun historyCleanUp() = CamundaEngine.history.cleanUpHistoryAsync()
    fun historyInstance() = CamundaEngine.history
            .createCleanableHistoricCaseInstanceReport()

    companion object {
        @JvmStatic
        private var ref =  SoftReference(CamundaHistoryApi())

        @JvmStatic
        val INSTANT
            get() = ref.get() ?: CamundaHistoryApi().apply { ref = SoftReference(this) }

    }
}