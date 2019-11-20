package cn.zenliu.ktor.features.domain

import io.ebean.Model
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class UserEntity(
        @Id
        val id: Long = 0,
        val name: String
):Model()