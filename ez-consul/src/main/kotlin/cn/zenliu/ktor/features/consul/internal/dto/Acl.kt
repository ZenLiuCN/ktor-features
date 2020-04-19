@file:ContextualSerialization(forClasses = [Instant::class])

package cn.zenliu.ktor.features.consul.internal.dto

import kotlinx.serialization.ContextualSerialization
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName
import java.math.BigInteger
import java.time.Instant


@Serializable
data class BootstrapInfo(
        @SerialName("AccessorID")
        val accessorID: String, // b5b1a918-50bc-fc46-dec2-d481359da4e3
        @SerialName("CreateIndex")
        val createIndex: Long, // 12
        @SerialName("CreateTime")
        val createTime: Instant, // 2018-10-24T10:34:20.843397-04:00
        @SerialName("Description")
        val description: String, // Bootstrap Token (Global Management)
        @SerialName("Hash")
        val hash: String, // oyrov6+GFLjo/KZAfqgxF/X4J/3LX0435DOBy9V22I0=
        @SerialName("ID")
        val id: String? = null, // 527347d3-9653-07dc-adc0-598b8f2b0f4d
        @SerialName("Local")
        val local: Boolean, // false
        @SerialName("ModifyIndex")
        val modifyIndex: Long, // 12
        @SerialName("Policies")
        val policies: List<Policy>,
        @SerialName("SecretID")
        val secretID: String // 527347d3-9653-07dc-adc0-598b8f2b0f4d
)

@Serializable
data class Policy(
        @SerialName("ID")
        val id: String, // 00000000-0000-0000-0000-000000000001
        @SerialName("Name")
        val name: String // global-management
)

@Serializable
data class ReplicationInfo(
        @SerialName("Enabled")
        val enabled: Boolean, // true
        @SerialName("LastError")
        val lastError: Instant, // 2016-11-03T06:28:28Z
        @SerialName("LastSuccess")
        val lastSuccess: Instant, // 2018-11-03T06:28:58Z
        @SerialName("ReplicatedIndex")
        val replicatedIndex: Long, // 1976
        @SerialName("ReplicatedTokenIndex")
        val replicatedTokenIndex: Long, // 2018
        @SerialName("ReplicationType")
        val replicationType: String, // tokens
        @SerialName("Running")
        val running: Boolean, // true
        @SerialName("SourceDatacenter")
        val sourceDatacenter: String // dc1
)


@Serializable
data class LoginReq(
        @SerialName("AuthMethod")
        val authMethod: String, // minikube
        @SerialName("BearerToken")
        val bearerToken: String, // eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9...
        @SerialName("Meta")
        val meta: Map<String, String>? = null,
        @SerialName("Namespace")
        val namespace: String? = null
)

@Serializable
data class LoginInfo(
        @SerialName("AccessorID")
        val accessorID: String, // 926e2bd2-b344-d91b-0c83-ae89f372cd9b
        @SerialName("AuthMethod")
        val authMethod: String, // minikube
        @SerialName("CreateIndex")
        val createIndex: Long, // 36
        @SerialName("CreateTime")
        val createTime: Instant, // 2019-04-29T10:08:08.404370762-05:00
        @SerialName("Description")
        val description: String, // token created via login
        @SerialName("Hash")
        val hash: String, // nLimyD+7l6miiHEBmN/tvCelAmE/SbIXxcnTzG3pbGY=
        @SerialName("Local")
        val local: Boolean, // true
        @SerialName("ModifyIndex")
        val modifyIndex: Long, // 36
        @SerialName("Roles")
        val roles: List<Role>,
        @SerialName("SecretID")
        val secretID: String, // b78d37c7-0ca7-5f4d-99ee-6d9975ce4586
        @SerialName("ServiceIdentities")
        val serviceIdentities: List<ServiceIdentity>
)

@Serializable
data class Role(
        @SerialName("ID")
        val id: String, // 3356c67c-5535-403a-ad79-c1d5f9df8fc7
        @SerialName("Name")
        val name: String // demo
)


/**
 * https://www.consul.io/api/acl/tokens.html
 * @property description String
 * @property local Boolean
 * @property policies List<Policy>
 * @constructor
 */
@Serializable
data class TokenReq(
        @SerialName("AccessorID") val accessorID: String? = null,
        @SerialName("SecretID") val secretID: String? = null,
        @SerialName("Roles") val roles: List<RoleLink>? = null,
        @SerialName("ServiceIdentities") val serviceIdentities: List<ServiceIdentity>? = null,
        @SerialName("Description") val description: String, // Agent token for 'node1'
        @SerialName("Local") val local: Boolean, // false
        @SerialName("ExpirationTime") val expirationTime: Instant? = null,
        @SerialName("ExpirationTTL") val expirationTTL: String? = null,
        @SerialName("authMethod ") val authMethod: String? = null,
        @SerialName("Namespace") val namespace: String? = null,
        @SerialName("Policies") val policies: List<PolicyLink>
)

@Serializable
data class ServiceIdentity(
        @SerialName("ServiceName")
        val serviceName: String, // 00000000-0000-0000-0000-000000000001
        @SerialName("Datacenters")
        val datacenters: List<String>? = null
)

@Serializable
data class PolicyLink(
        @SerialName("ID")
        val id: String? = null, // 00000000-0000-0000-0000-000000000001
        @SerialName("Name")
        val name: String? = null // global-management
)

@Serializable
data class RoleLink(
        @SerialName("ID")
        val id: String? = null, // 3356c67c-5535-403a-ad79-c1d5f9df8fc7
        @SerialName("Name")
        val name: String? = null // demo
)

@Serializable
data class TokenInfo(
        @SerialName("AccessorID")
        val accessorID: String, // 6a1253d2-1785-24fd-91c2-f8e78c745511
        @SerialName("CreateIndex")
        val createIndex: Long, // 59
        @SerialName("CreateTime")
        val createTime: Instant, // 2018-10-24T12:25:06.921933-04:00
        @SerialName("Description")
        val description: String, // Agent token for 'node1'
        @SerialName("Hash")
        val hash: String, // UuiRkOQPRCvoRZHRtUxxbrmwZ5crYrOdZ0Z1FTFbTbA=
        @SerialName("Local")
        val local: Boolean, // false
        @SerialName("ModifyIndex")
        val modifyIndex: Long, // 59
        @SerialName("Policies")
        val policies: List<Policy>,
        @SerialName("SecretID")
        val secretID: String // 45a3bd52-07c7-47a4-52fd-0745e0cfe967
)

@Serializable
data class PolicyReq(
        @SerialName("Datacenters")
        val datacenters: List<String>,
        @SerialName("Description")
        val description: String, // Grants read access to all node information
        @SerialName("Name")
        val name: String, // node-read
        @SerialName("Rules")
        val rules: String,// node_prefix "" { policy = "read"}
        @SerialName("Namespace")
        val namespace: String? = null, // node_prefix "" { policy = "read"}
        @SerialName("ID")
        val id: String? = null // node_prefix "" { policy = "read"}
)

@Serializable
data class PolicyInfo(
        @SerialName("CreateIndex")
        val createIndex: Long, // 14
        @SerialName("Datacenters")
        val datacenters: List<String>,
        @SerialName("Description")
        val description: String, // Grants read access to all node information
        @SerialName("Hash")
        val hash: String, // OtZUUKhInTLEqTPfNSSOYbRiSBKm3c4vI2p6MxZnGWc=
        @SerialName("ID")
        val id: String, // e359bd81-baca-903e-7e64-1ccd9fdc78f5
        @SerialName("ModifyIndex")
        val modifyIndex: Long, // 14
        @SerialName("Name")
        val name: String, // node-read
        @SerialName("Rules")
        val rules: String // node_prefix "" { policy = "read"}
)

@Serializable
data class RoleReq(
        @SerialName("Description")
        val description: String, // Showcases all input parameters
        @SerialName("Name")
        val name: String, // example-role
        @SerialName("Policies")
        val policies: List<Policy>,
        @SerialName("ServiceIdentities")
        val serviceIdentities: List<ServiceIdentity>,
        @SerialName("Namespace")
        val namespace: String? = null // node_prefix "" { policy = "read"}
)

@Serializable
data class RoleInfo(
        @SerialName("CreateIndex")
        val createIndex: Long, // 57
        @SerialName("Description")
        val description: String, // Showcases all input parameters
        @SerialName("Hash")
        val hash: String, // mBWMIeX9zyUTdDMq8vWB0iYod+mKBArJoAhj6oPz3BI=
        @SerialName("ID")
        val id: String, // aa770e5b-8b0b-7fcf-e5a1-8535fcc388b4
        @SerialName("ModifyIndex")
        val modifyIndex: Long, // 57
        @SerialName("Name")
        val name: String, // example-role
        @SerialName("Policies")
        val policies: List<Policy>,
        @SerialName("ServiceIdentities")
        val serviceIdentities: List<ServiceIdentity>
)

@Serializable
data class AuthMethodReq(
        @SerialName("Config")
        val config: AuthMethodConfig,
        @SerialName("Description")
        val description: String, // dev minikube cluster
        @SerialName("Name")
        val name: String, // minikube
        @SerialName("Type")
        val type: String? = null,// kubernetes
        @SerialName("Namespace")
        val namespace: String? = null // node_prefix "" { policy = "read"}
)

@Serializable
data class AuthMethodConfig(
        @SerialName("CACert")
        val caCert: String, // -----BEGIN CERTIFICATE-----...-----END CERTIFICATE-----
        @SerialName("Host")
        val host: String, // https://192.0.2.42:8443
        @SerialName("ServiceAccountJWT")
        val serviceAccountJWT: String // eyJhbGciOiJSUzI1NiIsImtpZCI6IiJ9...
)

@Serializable
data class AuthMethodInfo(
        @SerialName("Config")
        val config: AuthMethodConfig? = null, //won't with Lists
        @SerialName("CreateIndex")
        val createIndex: Int, // 15
        @SerialName("Description")
        val description: String, // dev minikube cluster
        @SerialName("ModifyIndex")
        val modifyIndex: Int, // 15
        @SerialName("Name")
        val name: String, // minikube
        @SerialName("Type")
        val type: String // kubernetes
)

@Serializable
data class BindingRuleReq(
        @SerialName("AuthMethod")
        val authMethod: String, // minikube
        @SerialName("BindName")
        val bindName: String, // {{ serviceaccount.name }}
        @SerialName("BindType")
        val bindType: String, // service
        @SerialName("Description")
        val description: String, // example rule
        @SerialName("Selector")
        val selector: String, // serviceaccount.namespace==default
        @SerialName("Namespace")
        val namespace: String? = null // node_prefix "" { policy = "read"}
)

@Serializable
data class BindingRuleInfo(
    @SerialName("AuthMethod")
    val authMethod: String, // minikube
    @SerialName("BindName")
    val bindName: String, // {{ serviceaccount.name }}
    @SerialName("BindType")
    val bindType: String, // service
    @SerialName("CreateIndex")
    val createIndex: Long, // 17
    @SerialName("Description")
    val description: String, // example rule
    @SerialName("ID")
    val id: String, // 000ed53c-e2d3-e7e6-31a5-c19bc3518a3d
    @SerialName("ModifyIndex")
    val modifyIndex: Long, // 17
    @SerialName("Selector")
    val selector: String // serviceaccount.namespace==default
)