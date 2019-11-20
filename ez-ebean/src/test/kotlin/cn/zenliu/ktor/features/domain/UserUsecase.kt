package cn.zenliu.ktor.features.domain

object UserUsecase {
    fun create(name: String) = UserEntity(name = name)
            .apply {
                save()
            }

}