@file:ContextualSerialization(forClasses = [Instant::class])

package cn.zenliu.ktor.features.consul.internal

import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.fromHttpDateString
import io.ktor.http.toHttpDateString
import kotlinx.serialization.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter


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
    fun attackToken(builder: HttpRequestBuilder): HttpRequestBuilder
}

interface AclClient : BaseClient {
    override fun uriRoot(): String = "${super.uriRoot()}/acl"

    @Serializable
    data class Bootstrap(
            val AccessorID: String,
            val CreateIndex: Int,
            @ContextualSerialization
            val CreateTime: Instant,
            val Description: String,
            val Hash: String,
            val ID: String,
            val Local: Boolean,
            val ModifyIndex: Int,
            val Policies: List<Policy>,
            val SecretID: String
    )


    suspend fun bootstrap() = http
            .put<Bootstrap>("${uriRoot()}/bootstrap")

    @Serializable
    data class Replication(
            val Enabled: Boolean,
            val LastError: Instant,
            val LastSuccess: Instant,
            val ReplicatedIndex: Int,
            val ReplicatedTokenIndex: Int,
            val ReplicationType: String,
            val Running: Boolean,
            val SourceDatacenter: String
    )

    suspend fun replication(dataCenter: String?) = http
            .get<Replication>("${uriRoot()}/replication") {
                dataCenter?.let {
                    parameter("dc", it)
                }
            }

    @Deprecated("only for consul >1.4.0,will be removed in futrue")
    suspend fun translateRule(rule: String) = http
            .post<String>("${uriRoot()}/rules/translate") {
                body = ByteArrayContent(rule.toByteArray())
            }

    @Deprecated("only for consul >1.4.0,will be removed in futrue")
    suspend fun translateAccessorIdRule(accessorId: String) = http
            .get<String>("${uriRoot()}/rules/translate/$accessorId")

    /**
     *
     * @property AuthMethod String
     * @property BearerToken String
     * @property Meta Map<String, String>?
     * @property Namespace String? Enterprise Only
     * @constructor
     */
    data class LoginRequest(
            val AuthMethod: String,
            val BearerToken: String,
            val Meta: Map<String, String>? = null,
            val Namespace: String? = null
    )

    suspend fun login(login: LoginRequest) = http
            .get<Login>("${uriRoot()}/login") {
                body = json().write(login)
            }

    @Serializable
    data class Login(
            val AccessorID: String,
            val AuthMethod: String,
            val CreateIndex: Int,
            val CreateTime: Instant,
            val Description: String,
            val Hash: String,
            val Local: Boolean,
            val ModifyIndex: Int,
            val Roles: List<Role>,
            val SecretID: String,
            val ServiceIdentities: List<ServiceIdentity>
    )

    @Serializable
    data class Role(
            val ID: String,
            val Name: String
    )

    @Serializable
    data class ServiceIdentity(
            val ServiceName: String
    )

    suspend fun loginout() = http
            .post<Login>("${uriRoot()}/loginout")

    @Serializable
    data class TokenRequest(
            @SerialName("Description")
            val description: String, // Agent token for 'node1'
            @SerialName("Local")
            val local: Boolean, // false
            @SerialName("Policies")
            val policies: List<Policy>
    )

    @Serializable
    data class Policy(
            @SerialName("ID")
            val iD: String, // 165d4317-e379-f732-ce70-86278c4558f7
            @SerialName("Name")
            val name: String // node-read
    )

    suspend fun token() = http
            .post<Login>("${uriRoot()}/token")


}

@Serializer(forClass = Instant::class)
object InstantSerializer : KSerializer<Instant> {
    private val format = DateTimeFormatter.ISO_INSTANT
    override val descriptor: SerialDescriptor = PrimitiveDescriptor("JvmInstant", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Instant {
        return Instant.from(format.parse(decoder.decodeString()))
    }

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(format.format(value))
    }

}