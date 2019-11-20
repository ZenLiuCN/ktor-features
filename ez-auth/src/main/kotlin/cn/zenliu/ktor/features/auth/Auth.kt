package cn.zenliu.ktor.features.auth


import cn.zenliu.ktor.features.FeatureTemplate
import cn.zenliu.ktor.features.properties.annotation.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.http.*
import io.ktor.http.auth.*
import io.ktor.request.*
import io.ktor.response.*
import javax.naming.*

class Auth private constructor(){
	companion
	object AuthFeature :
		FeatureTemplate.FeatureObjectTemplate<Application, AuthFeature, AuthFeature, AuthFeature.AuthProperties>() {
		override val configClazz = AuthProperties::class

		override fun init(pipeline: Application, configure: AuthFeature.() -> Unit): AuthFeature = run {
			config ?: throw ConfigurationException("AuthProperties not config")
			this.apply(configure)
			this
		}

		private val defaultExtractor: TokenExtractor = {
			it.header(config!!.header)?.let {
				HttpAuthHeader.tokenAuthChallenge(it)
			} ?: it.queryParameters.get(config!!.param)?.let {
				HttpAuthHeader.tokenAuthChallenge(it)
			}
		}
		internal var extractor: TokenExtractor = defaultExtractor
		fun challengeKey() = this.config!!.challengeKey
		fun setExtractor(extractor: TokenExtractor) {
			this.extractor = extractor
		}

		@Properties("authenticate")
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

val AuthScheme.Token get() = Auth.challengeKey()
fun HttpAuthHeader.Companion.tokenAuthChallenge(token: String) = HttpAuthHeader.Single(AuthScheme.Token, token)
fun ApplicationRequest.parseTokenAuthorizationHeader(): HttpAuthHeader? = Auth.extractor(this)
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
	when (parsed) {
		is HttpAuthHeader.Single -> when {
			!parsed.authScheme.equals(Auth.challengeKey(), ignoreCase = true) -> return null
			else -> return UserTokenPrincipal(parsed.blob)
		}
		else -> return null
	}
}


