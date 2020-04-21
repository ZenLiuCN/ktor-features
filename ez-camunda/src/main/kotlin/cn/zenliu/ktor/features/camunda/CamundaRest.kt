package cn.zenliu.ktor.features.camunda

import java.lang.ref.SoftReference

class CamundaRest private constructor() {
    fun historyCleanUp() = CamundaEngine.history.cleanUpHistoryAsync()
    fun historyInstance()=CamundaEngine.history
            .createCleanableHistoricCaseInstanceReport()

        companion object {
        private var ref: SoftReference<CamundaRest> = SoftReference(CamundaRest())
        val INSTANT
            get
            () = if (ref.get() != null) ref.get() else CamundaRest().apply { ref = SoftReference(this) }
    }
}