package cn.zenliu.ktor.features.consul.internal

import cn.zenliu.ktor.features.consul.internal.dto.*
import io.ktor.client.request.*
import io.ktor.http.content.ByteArrayContent

interface AclClient : BaseClient {
    override fun uriRoot(): String = "${super.uriRoot()}/acl"


    suspend fun bootstrap() = http
            .put<BootstrapInfo>("${uriRoot()}/bootstrap")

    suspend fun replication(dataCenter: String?) = http
            .get<ReplicationInfo>("${uriRoot()}/replication") {
                dataCenter?.let {
                    parameter("dc", it)
                }
            }

    //region Old
    @Deprecated("only for consul >1.4.0,will be removed in futrue")
    suspend fun translateRule(rule: String) = http
            .post<String>("${uriRoot()}/rules/translate") {
                body = ByteArrayContent(rule.toByteArray())
            }

    @Deprecated("only for consul >1.4.0,will be removed in futrue")
    suspend fun translateAccessorIdRule(accessorId: String) = http
            .get<String>("${uriRoot()}/rules/translate/$accessorId")
    //endregion

    suspend fun login(login: LoginReq) = http
            .get<LoginInfo>("${uriRoot()}/login") {
                body = json().write(login)
            }


    suspend fun loginout() = http
            .post<LoginInfo>("${uriRoot()}/loginout") {
                attachToken(this)
            }


    //region Token
    suspend fun createToken(req: TokenReq) = http
            .put<TokenInfo>("${uriRoot()}/token") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun updateToken(accessorID: String, req: TokenReq) = http
            .put<TokenInfo>("${uriRoot()}/token/$accessorID") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun cloneToken(accessorID: String,
                           description: String? = null,
                           namespace: String? = null) = http
            .put<TokenInfo>("${uriRoot()}/token/$accessorID/clone") {
                attachToken(this)
                body = json().write(mutableMapOf<String, String>().apply {
                    description?.let { put("Description", it) }
                    namespace?.let { put("Namespace", it) }
                })
            }

    suspend fun readToken(accessorID: String) = http
            .get<TokenInfo>("${uriRoot()}/token/$accessorID") {
                attachToken(this)

            }

    suspend fun readSelfToken() = http
            .get<TokenInfo>("${uriRoot()}/token/self") {
                attachToken(this)
            }

    suspend fun deleteToken(accessorID: String, ns: String? = null) = http
            .delete<Boolean>("${uriRoot()}/token/$accessorID") {
                ns?.let { parameter("ns", it) }
                attachToken(this)
            }

    suspend fun listTokens(
            policy: String? = null,
            roleId: String? = null,
            authmethod: String? = null,
            authMethodNamespace: String? = null,
            namespace: String? = null
    ) = http
            .get<List<TokenInfo>>("${uriRoot()}/tokens") {
                policy?.let { parameter("policy", it) }
                roleId?.let { parameter("role", it) }
                authmethod?.let { parameter("authmethod", it) }
                authMethodNamespace?.let { parameter("authmethod-ns", it) }
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }
    //endregion

    //region Policy
    suspend fun createPolicy(req: PolicyReq) = http
            .put<PolicyInfo>("${uriRoot()}/policy") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun updatePolicy(id: String, req: PolicyReq) = http
            .put<PolicyInfo>("${uriRoot()}/policy/$id") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun deletePolicy(id: String, namespace: String? = null) = http
            .delete<Boolean>("${uriRoot()}/policy/$id") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }

    suspend fun readPolicy(id: String, namespace: String? = null) = http
            .get<PolicyInfo>("${uriRoot()}/policy/$id") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)

            }

    suspend fun listPolicy(namespace: String? = null) = http
            .get<List<PolicyInfo>>("${uriRoot()}/policies") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)

            }
    //endregion

    //region Roles
    suspend fun createRole(req: RoleReq) = http
            .put<RoleInfo>("${uriRoot()}/role") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun updateRole(id: String, req: RoleReq) = http
            .put<RoleInfo>("${uriRoot()}/role/$id") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun deleteRole(id: String, namespace: String? = null) = http
            .delete<Boolean>("${uriRoot()}/role/$id") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }

    suspend fun readRole(id: String, namespace: String? = null) = http
            .get<RoleInfo>("${uriRoot()}/role/$id") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)

            }

    suspend fun readRoleByName(name: String, namespace: String? = null) = http
            .get<RoleInfo>("${uriRoot()}/role/name/$name") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)

            }

    suspend fun listRole(namespace: String? = null) = http
            .get<List<RoleInfo>>("${uriRoot()}/roles") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }
    //endregion

    //region AuthMethod
    suspend fun createAuthMethod(req: AuthMethodReq) = http
            .put<AuthMethodInfo>("${uriRoot()}/auth-method") {
                assert(req.type != null)
                attachToken(this)
                body = json().write(req)
            }

    suspend fun updateAuthMethod(name: String, req: AuthMethodReq) = http
            .put<AuthMethodInfo>("${uriRoot()}/auth-method/$name") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun deleteAuthMethod(name: String, namespace: String? = null) = http
            .delete<Boolean>("${uriRoot()}/auth-method/$name") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }

    suspend fun readAuthMethod(name: String, namespace: String? = null) = http
            .get<AuthMethodInfo>("${uriRoot()}/auth-method/$name") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)

            }


    suspend fun listAuthMethod(namespace: String? = null) = http
            .get<List<AuthMethodInfo>>("${uriRoot()}/auth-methods") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }

    //endregion
    //region BindingRule
    suspend fun createBindingRule(req: BindingRuleInfo) = http
            .put<BindingRuleInfo>("${uriRoot()}/binding-rule") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun updateBindingRule(id: String, req: BindingRuleInfo) = http
            .put<BindingRuleInfo>("${uriRoot()}/binding-rule/$id") {
                attachToken(this)
                body = json().write(req)
            }

    suspend fun deleteBindingRule(id: String, namespace: String? = null) = http
            .delete<Boolean>("${uriRoot()}/binding-rule/$id") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)
            }

    suspend fun readBindingRule(id: String, namespace: String? = null) = http
            .get<BindingRuleInfo>("${uriRoot()}/binding-rule/$id") {
                namespace?.let { parameter("ns", it) }
                attachToken(this)

            }


    suspend fun listBindingRule(authMethod:String?=null, namespace: String? = null) = http
            .get<List<BindingRuleInfo>>("${uriRoot()}/binding-rules") {
                namespace?.let { parameter("ns", it) }
                authMethod?.let { parameter("authmethod", it) }
                attachToken(this)
            }
    //endregion
}