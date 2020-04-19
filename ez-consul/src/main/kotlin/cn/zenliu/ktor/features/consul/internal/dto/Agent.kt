@file:ContextualSerialization(forClasses = [Instant::class])
package cn.zenliu.ktor.features.consul.internal.dto

import kotlinx.serialization.ContextualSerialization
import java.time.Instant
import kotlinx.serialization.Serializable

import kotlinx.serialization.SerialName


@Serializable
data class AgentMemberInfo(
    @SerialName("Addr")
    val addr: String, // 10.1.10.12
    @SerialName("DelegateCur")
    val delegateCur: Long, // 3
    @SerialName("DelegateMax")
    val delegateMax: Long, // 3
    @SerialName("DelegateMin")
    val delegateMin: Long, // 1
    @SerialName("Name")
    val name: String, // foobar
    @SerialName("Port")
    val port: Int, // 8301
    @SerialName("ProtocolCur")
    val protocolCur: Long, // 2
    @SerialName("ProtocolMax")
    val protocolMax: Long, // 2
    @SerialName("ProtocolMin")
    val protocolMin: Long, // 1
    @SerialName("Status")
    val status: Long, // 1
    @SerialName("Tags")
    val tags: Tags
)
@Serializable
data class Tags(
        @SerialName("bootstrap")
        val bootstrap: String, // 1
        @SerialName("dc")
        val dc: String, // dc1
        @SerialName("id")
        val id: String?="", // 40e4a748-2192-161a-0510-9bf59fe950b5
        @SerialName("port")
        val port: String, // 8300
        @SerialName("role")
        val role: String, // consul
        @SerialName("vsn")
        val vsn: String?=null, // 1
        @SerialName("vsn_max")
        val vsnMax: String?=null, // 1
        @SerialName("vsn_min")
        val vsnMin: String?=null // 1
)


@Serializable
data class AgentInfo(
    @SerialName("Config")
    val config: AgentConfig,
    @SerialName("Coord")
    val coord: Coord,
    @SerialName("DebugConfig")
    val debugConfig: Map<String,String>,
    @SerialName("Member")
    val member: Member,
    @SerialName("Meta")
    val meta: Meta
)

@Serializable
data class AgentConfig(
    @SerialName("Datacenter")
    val datacenter: String, // dc1
    @SerialName("NodeID")
    val nodeID: String, // 9d754d17-d864-b1d3-e758-f3fe25a9874f
    @SerialName("NodeName")
    val nodeName: String, // foobar
    @SerialName("Revision")
    val revision: String, // deadbeef
    @SerialName("Server")
    val server: Boolean, // true
    @SerialName("Version")
    val version: String // 1.0.0
)

@Serializable
data class Coord(
    @SerialName("Adjustment")
    val adjustment: Int, // 0
    @SerialName("Error")
    val error: Double, // 1.5
    @SerialName("Vec")
    val vec: List<Int>
)


@Serializable
data class Member(
    @SerialName("Addr")
    val addr: String, // 10.1.10.12
    @SerialName("DelegateCur")
    val delegateCur: Long, // 4
    @SerialName("DelegateMax")
    val delegateMax: Long, // 4
    @SerialName("DelegateMin")
    val delegateMin: Long, // 2
    @SerialName("Name")
    val name: String, // foobar
    @SerialName("Port")
    val port: Int, // 8301
    @SerialName("ProtocolCur")
    val protocolCur: Long, // 2
    @SerialName("ProtocolMax")
    val protocolMax: Long, // 2
    @SerialName("ProtocolMin")
    val protocolMin: Long, // 1
    @SerialName("Status")
    val status: Int, // 1
    @SerialName("Tags")
    val tags: Tags
)

@Serializable
data class Meta(
    @SerialName("instance_type")
    val instanceType: String, // i2.xlarge
    @SerialName("os_version")
    val osVersion: String // ubuntu_16.04
)

@Serializable
data class AgentMetrics(
    @SerialName("Counters")
    val counters: List<Counter>,
    @SerialName("Gauges")
    val gauges: List<Gauge>,
    @SerialName("Points")
    val points: List<Map<String,String>>,
    @SerialName("Samples")
    val samples: List<Sample>,
    @SerialName("Timestamp")
    val timestamp: String // 2017-08-08 02:55:10 +0000 UTC
)

@Serializable
data class Counter(
    @SerialName("Count")
    val count: Int, // 1
    @SerialName("Labels")
    val labels: Map<String,String>,
    @SerialName("Max")
    val max: Int, // 1
    @SerialName("Mean")
    val mean: Int, // 1
    @SerialName("Min")
    val min: Int, // 1
    @SerialName("Name")
    val name: String, // consul.consul.catalog.service.query
    @SerialName("Stddev")
    val stddev: Int, // 0
    @SerialName("Sum")
    val sum: Int // 1
)

@Serializable
data class Gauge(
    @SerialName("Labels")
    val labels: Map<String,String>,
    @SerialName("Name")
    val name: String, // consul.consul.session_ttl.active
    @SerialName("Value")
    val value: Int // 0
)

@Serializable
data class Sample(
    @SerialName("Count")
    val count: Int, // 1
    @SerialName("Labels")
    val labels: Map<String,String>,
    @SerialName("Max")
    val max: Double, // 0.1817069947719574
    @SerialName("Mean")
    val mean: Double, // 0.1817069947719574
    @SerialName("Min")
    val min: Double, // 0.1817069947719574
    @SerialName("Name")
    val name: String, // consul.consul.http.GET.v1.agent.metrics
    @SerialName("Stddev")
    val stddev: Int, // 0
    @SerialName("Sum")
    val sum: Double // 0.1817069947719574
)


typealias AgentCheckInfo=Map<String,ServiceInfo>

@Serializable
data class ServiceInfo(
    @SerialName("CheckID")
    val checkID: String, // service:redis
    @SerialName("Name")
    val name: String, // Service 'redis' check
    @SerialName("Node")
    val node: String, // foobar
    @SerialName("Notes")
    val notes: String,
    @SerialName("Output")
    val output: String,
    @SerialName("ServiceID")
    val serviceID: String, // redis
    @SerialName("ServiceName")
    val serviceName: String, // redis
    @SerialName("ServiceTags")
    val serviceTags: List<String>,
    @SerialName("Status")
    val status: String // passing
)

@Serializable
data class AgentCheckRegister(
    @SerialName("Args")
    val args: List<String>,
    @SerialName("Body")
    val body: String, // {"check":"mem"}
    @SerialName("DeregisterCriticalServiceAfter")
    val deregisterCriticalServiceAfter: String, // 90m
    @SerialName("DockerContainerID")
    val dockerContainerID: String, // f972c95ebf0e
    @SerialName("HTTP")
    val http: String, // https://example.com
    @SerialName("Header")
    val header: Header,
    @SerialName("ID")
    val id: String, // mem
    @SerialName("Interval")
    val interval: String, // 10s
    @SerialName("Method")
    val method: String, // POST
    @SerialName("Name")
    val name: String, // Memory utilization
    @SerialName("Notes")
    val notes: String, // Ensure we don't oversubscribe memory
    @SerialName("Shell")
    val shell: String, // /bin/bash
    @SerialName("TCP")
    val tcp: String, // example.com:22
    @SerialName("TLSSkipVerify")
    val tlsSkipVerify: Boolean, // true
    @SerialName("Timeout")
    val timeout: String // 5s
)

@Serializable
data class Header(
    @SerialName("Content-Type")
    val contentType: String // application/json
)