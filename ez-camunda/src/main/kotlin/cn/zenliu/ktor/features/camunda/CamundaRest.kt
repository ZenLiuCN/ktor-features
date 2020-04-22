package cn.zenliu.ktor.features.camunda

import cn.zenliu.ktor.features.camunda.CamundaHistoryApi.ActiveInstanceStatus.*
import org.camunda.bpm.engine.batch.history.HistoricBatch
import org.camunda.bpm.engine.batch.history.HistoricBatchQuery
import org.camunda.bpm.engine.history.CleanableHistoricBatchReportResult
import org.camunda.bpm.engine.history.HistoricActivityInstance
import org.camunda.bpm.engine.history.HistoricActivityInstanceQuery


object CamundaHistoryApi {

	//region Activity
	enum class ActiveInstanceStatus {
		ANY, FINISH, PROGRESS;
	}

	private fun activityInstanceQuery(
			activityId: String?,
			activityInstanceId: String?,
			activityName: String?,
			activityType: String?,
			status: ActiveInstanceStatus?,
			config: HistoricActivityInstanceQuery.() -> Unit
	) = CamundaEngine
			.history
			.createHistoricActivityInstanceQuery()
			.apply {
				activityId?.let { activityId(it) }
				activityInstanceId?.let { activityInstanceId(it) }
				activityName?.let { activityName(it) }
				activityType?.let { activityType(it) }
				status?.let {
					when (it) {
						ANY -> Unit
						FINISH -> this.finished()
						PROGRESS -> this.unfinished()
					}
				}
			}
			.apply(config)

	fun activeInstanceList(
			activityId: String?,
			activityInstanceId: String?,
			activityName: String?,
			activityType: String?,
			status: ActiveInstanceStatus?,
			page: Int,
			pageSize: Int
	): List<HistoricActivityInstance> =
			activityInstanceQuery(
					activityId,
					activityInstanceId,
					activityName,
					activityType,
					status
			) {

			}.listPage(pageSize * page, pageSize)

	fun activeInstanceCount(
			activityId: String?,
			activityInstanceId: String?,
			activityName: String?,
			activityType: String?,
			status: ActiveInstanceStatus?
	): Long = activityInstanceQuery(
			activityId,
			activityInstanceId,
			activityName,
			activityType,
			status
	) {}.count()
	//endregion

	//region Batch
	private fun batchQuery(
			batchId: String?,
			completed: Boolean?,
			type: String?,
			config: HistoricBatchQuery.() -> Unit
	): HistoricBatchQuery = CamundaEngine
			.history
			.createHistoricBatchQuery()
			.apply {
				batchId?.let { this.batchId(it) }
				completed?.let { this.completed(it) }
				type?.let { this.type(it) }
				orderByStartTime().desc()
			}
			.apply(config)

	fun deleteBatch(batchId: String) =
			CamundaEngine.history.deleteHistoricBatch(batchId)
	fun batchCount(
			batchId: String?,
			completed: Boolean?,
			type: String?) = batchQuery(batchId, completed, type) {}.count()

	fun batchList(
			batchId: String?,
			completed: Boolean?,
			type: String?,
			page: Int,
			pageSize: Int): List<HistoricBatch> = batchQuery(batchId, completed, type) {}.listPage(pageSize * page, pageSize)
	//endregion

	//region CleanableBatch

	fun cleanableBatchCount() = CamundaEngine
			.history
			.createCleanableHistoricBatchReport()
			.count()

	fun cleanableBatchReport(
			page: Int,
			pageSize: Int
	): List<CleanableHistoricBatchReportResult> =
			CamundaEngine
					.history
					.createCleanableHistoricBatchReport()
					.listPage(pageSize * page, pageSize)
	//endregion
}