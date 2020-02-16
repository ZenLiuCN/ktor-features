package cn.zenliu.ktor.features.domain

import io.ebean.*
import io.ebean.annotation.*
import javax.persistence.*

@Entity
class UserEntity(
	val name: String,
	@WhoModified
	val modifiedBy: Long = 0,
	@WhoCreated
	val createdBy: Long = 0
):Model(){
	@Id
	val id: Long = 0
	companion object :UserFinder()
}

open class UserFinder:Finder<Long,UserEntity>(UserEntity::class.java)
