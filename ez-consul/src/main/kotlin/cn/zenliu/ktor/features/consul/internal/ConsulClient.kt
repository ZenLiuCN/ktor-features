@file:ContextualSerialization(forClasses = [Instant::class])

package cn.zenliu.ktor.features.consul.internal

import cn.zenliu.ktor.features.consul.internal.dto.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.content.ByteArrayContent
import kotlinx.serialization.ContextualSerialization
import java.time.Instant


class ConsulClient(
        val host: String,
        val port: Int,
        val http: HttpClient
) {

}

interface BaseClient {
    val host: String
    val port: Int
    val http: HttpClient
    fun json() = io.ktor.client.features.json.defaultSerializer()
    fun uriRoot() = "http://$host:$port"
    fun attachToken(builder: HttpRequestBuilder): HttpRequestBuilder
}

interface AgentClient : BaseClient {
    override fun uriRoot(): String = "${super.uriRoot()}/agent"


    //region Common
    suspend fun member(wan: Boolean = false, segment: String? = null) = http
            .get<List<AgentMemberInfo>>("${uriRoot()}/members") {
                wan.takeIf { it }?.let { parameter("wan", it) }
                segment?.let { parameter("segment", it) }
                attachToken(this)
            }

    suspend fun self() = http
            .get<AgentInfo>("${uriRoot()}/self") {
                attachToken(this)
            }

    suspend fun reload() = http
            .put<String>("${uriRoot()}/reload") {
                attachToken(this)
            }

    suspend fun maintenance(enable: Boolean, reason: String? = "") = http
            .get<String>("${uriRoot()}/maintenance") {
                parameter("enable", enable)
                reason?.let { parameter("reason", it) }
                attachToken(this)

            }

    suspend fun metrics(format: String? = null) = http
            .get<AgentMemberInfo>("${uriRoot()}/metrics") {
                attachToken(this)
            }

    suspend fun monitor(logLevel: String? = null, logJson: Boolean = false) = http
            .get<String>("${uriRoot()}/monitor") {
                parameter("logjson", logJson)
                logLevel?.let { parameter("loglevel", logLevel) }
                attachToken(this)
            }
    //endregion

    //region Register
    suspend fun join(address: String, wan: Boolean = false) = http
            .put<String>("${uriRoot()}/join/$address") {
                wan.takeIf { it }?.let { parameter("wan", it) }
                attachToken(this)
            }

    suspend fun leave() = http
            .put<String>("${uriRoot()}/leave") {
                attachToken(this)
            }

    suspend fun forceLeave(node: String, prune: Boolean = false) = http
            .put<String>("${uriRoot()}/force-leave/${buildString { append(node);if (prune) append("?prune") }}") {
                attachToken(this)
            }
    //endregion

    //region Tokens
    suspend fun updateToken(token: String,type:AgentUpdateTokenType) = http
            .put<String>("${uriRoot()}/token/${type.code}") {
                attachToken(this)
                body = ByteArrayContent("""{"Token":"$token"}""".toByteArray(), ContentType.Application.Json)
            }
    enum class AgentUpdateTokenType(val code:String){
        default("default"),
        agent("agent"),
        agentMaster("agent_master"),
        replication("replication"),
    }
    //endregion
    //region Checks
    suspend fun checks(filter: String?=null) = http
            .get<AgentCheckInfo>("${uriRoot()}/checks") {
                filter?.let { parameter("filter", it) }
                attachToken(this)
            }
    suspend fun checkRegister(filter: String?=null) = http
            .get<AgentCheckInfo>("${uriRoot()}/check/register") {
                filter?.let { parameter("filter", it) }
                attachToken(this)
            }

    //endregion

}