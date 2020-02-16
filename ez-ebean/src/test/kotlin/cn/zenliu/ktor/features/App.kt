package cn.zenliu.ktor.features

import cn.zenliu.ktor.features.datasource.*
import cn.zenliu.ktor.features.domain.*
import cn.zenliu.ktor.features.ebean.*
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import java.time.*

fun Application.main() {
	install(Hikari)
	install(EbeanIdProvider) {
		createUserProvider(0L)
		//createTenantProvider(0L)
	}
	install(EbeanORM) {
		this.configEbean {
			this.dataSource = Hikari.datasource
			this.currentTenantProvider = EbeanIdProvider.getCurrentTenantProvider()
			this.currentUserProvider = EbeanIdProvider.getCurrentUserProvider()
		}
	}
	routing {
		put("/user/{name}") {
			val id = Instant.now().epochSecond
			EbeanIdProvider.setUserId(id)
			UserUsecase.create(name = call.parameters["name"]!!).let {
				assert(it.createdBy == id)
				assert(it.modifiedBy == id)
				call.respond(it.id.toString())
			}
		}
	}
}
