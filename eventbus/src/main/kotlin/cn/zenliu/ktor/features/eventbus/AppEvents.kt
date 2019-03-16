package cn.zenliu.ktor.features.eventbus


enum class AppEvents : EventEnum {
    START,
    STOP;

    fun createEvent(message: String = "") = AppEvent(this.name, message)

    companion object {
        data class AppEvent internal constructor(override val name: String, val message: String = "") :
            Event<AppEvents>() {
            override fun toEnum(): AppEvents = AppEvents.valueOf(this.name)
        }


    }
}
