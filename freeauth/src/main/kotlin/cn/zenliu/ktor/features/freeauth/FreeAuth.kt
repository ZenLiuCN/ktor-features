package cn.zenliu.ktor.features.freeauth

import cn.zenliu.ktor.features.properties.annotation.Properties
import cn.zenliu.ktor.features.template.FeatureTemplate
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.auth.*
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.AuthScheme
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.request.ApplicationRequest
import io.ktor.request.header
import io.ktor.response.respond
import javax.naming.ConfigurationException

class FreeAuth {
    companion
    object Feature :
        FeatureTemplate.FeatureObjectTemplate<Application, Feature, Feature, Feature.AuthProperties>() {
        override val configClazz = Feature.AuthProperties::class

        override fun init(pipeline: Application, configure: Feature.() -> Unit): Feature = run {
            config ?: throw ConfigurationException("AuthProperties not configurated")
            this.apply(configure)
            this
        }


        internal val defualtExtractor: TokenExtractor = {
            it.header(config!!.header)?.let {
                HttpAuthHeader.tokenAuthChallenge(it)
            } ?: it.queryParameters.get(config!!.param)?.let {
                HttpAuthHeader.tokenAuthChallenge(it)
            }
        }
        internal var extractor: TokenExtractor = defualtExtractor
        fun challengeKey() = this.config!!.challengeKey

        @Properties("author")
        class AuthProperties {
            var header: String = "token"
            var param: String = "token"
            var challengeKey: String = "TokenAuth"
        }

    }
}
typealias TokenExtractor = (request: ApplicationRequest) -> HttpAuthHeader?

data class UserTokenPrincipal(val token: String) : Principal

class AuthProvider(name: String? = "FreeAuth") : AuthenticationProvider(name) {
    internal var authenticationFunction: suspend ApplicationCall.(UserTokenPrincipal) -> Principal? = { null }
    fun validate(body: suspend ApplicationCall.(UserTokenPrincipal) -> Principal?) {
        authenticationFunction = body
    }
}

val AuthScheme.Token get() = FreeAuth.challengeKey()
fun HttpAuthHeader.Companion.tokenAuthChallenge(token: String) = HttpAuthHeader.Single(AuthScheme.Token, token)
fun ApplicationRequest.parseTokenAuthorizationHeader(): HttpAuthHeader? = FreeAuth.extractor(this)
fun Authentication.Configuration.token(name: String? = null, configure: AuthProvider.() -> Unit) {
    val provider = AuthProvider(name).apply(configure)
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
            context.challenge(FreeAuth.challengeKey(), cause) {
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
    when (parsed) {
        is HttpAuthHeader.Single -> when {
            !parsed.authScheme.equals(FreeAuth.challengeKey(), ignoreCase = true) -> return null
            else -> return UserTokenPrincipal(parsed.blob)
        }
        else -> return null
    }
}



