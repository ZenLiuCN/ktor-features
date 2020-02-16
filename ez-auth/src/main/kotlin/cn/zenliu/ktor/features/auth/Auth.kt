package cn.zenliu.ktor.features.auth


import cn.zenliu.ktor.features.*
import cn.zenliu.ktor.features.properties.annotation.*
import cn.zenliu.ktor.features.properties.manager.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.request.*
import io.ktor.response.*
import javax.naming.*

class Auth private constructor() {
    companion
    object AuthFeature :
            FeatureTemplate.FeatureObjectTemplate<Application, AuthFeature, AuthFeature, AuthFeature.AuthProperties>() {
        override val configClazz = AuthProperties::class

        override fun init(pipeline: Application, configure: AuthFeature.() -> Unit): AuthFeature = run {
            pipeline.attributes.computeIfAbsent(PropertiesManager.key) {
                pipeline.install(PropertiesManager)
            }
            this.config ?: throw ConfigurationException("AuthProperties not config")
            this.apply(configure)
            this
        }

        private val defaultExtractor: TokenExtractor = {
            it.header(config!!.header)?.let { HD ->
                HD.split(" ").takeIf { it.size == 2 }?.let { auth ->
                    AuthTokenHeader(auth.first(), auth[1])
                } ?: AuthTokenHeader("", HD)
            } ?: it.queryParameters.get(config!!.param)?.let { HD ->
                HD.split(" ").takeIf { it.size == 2 }?.let { auth ->
                    AuthTokenHeader(auth.first(), auth[1])
                } ?: AuthTokenHeader("", HD)
            }
        }
        internal var extractor: TokenExtractor = defaultExtractor
        fun challengeKey() = this.config!!.challengeKey
        fun setExtractor(extractor: TokenExtractor) {
            this.extractor = extractor
        }


        @Properties("authenticate")
        class AuthProperties(
                var header: String = "token",
                var param: String = "token",
                var challengeKey: String = ""
        )

    }
}
typealias TokenExtractor = (request: ApplicationRequest) -> AuthTokenHeader?

data class AuthTokenHeader(val authScheme: String, val blob: String)
data class UserTokenPrincipal(val token: String) : Principal

class EzAuthProvider(name: String? = "EzAuth") : AuthenticationProvider(Configuration(name)) {
    internal var authenticationFunction: suspend ApplicationCall.(UserTokenPrincipal) -> Principal? = { null }
    fun validate(body: suspend ApplicationCall.(UserTokenPrincipal) -> Principal?) {
        authenticationFunction = body
    }

    companion object {
        class Configuration(name: String? = "EzTokenAuth") : AuthenticationProvider.Configuration(name) {

        }
    }
}

fun HttpAuthHeader.Companion.tokenAuthChallenge(schema: String, token: String) = AuthTokenHeader(schema, token)

fun ApplicationRequest.parseTokenAuthorizationHeader() = Auth.extractor(this)

fun Authentication.Configuration.token(name: String? = null, configure: EzAuthProvider.() -> Unit) {
    val provider = EzAuthProvider(name).apply(configure)
    val authenticate = provider.authenticationFunction
    provider.pipeline.intercept(AuthenticationPipeline.RequestAuthentication) { context ->
        val credentials = call.request.tokenAuthenticationCredentials()
        val principal = credentials?.let { authenticate(call, it) }
        val cause = when {
            credentials == null -> AuthenticationFailedCause.NoCredentials
            principal == null -> AuthenticationFailedCause.InvalidCredentials
            else -> null
        }
        if (cause != null) {
            context.challenge(Auth.challengeKey(), cause) {
                call.respond(
                        HttpStatusCode.Unauthorized,
                        mapOf(
                                "timestamp" to System.currentTimeMillis(),
                                "status" to HttpStatusCode.Unauthorized.value
                        )
                )
                it.complete()
            }
        }
        if (principal != null) {
            context.principal(principal)
        }
    }
    register(provider)
}

fun ApplicationRequest.tokenAuthenticationCredentials(): UserTokenPrincipal? {
    val parsed = parseTokenAuthorizationHeader()
    return when (parsed) {
        is AuthTokenHeader -> when {
            !parsed.authScheme.equals(Auth.challengeKey(), ignoreCase = true) -> null
            else -> UserTokenPrincipal(parsed.blob)
        }
        else -> null
    }
}


