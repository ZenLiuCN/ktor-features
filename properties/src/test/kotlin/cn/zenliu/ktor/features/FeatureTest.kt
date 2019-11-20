package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.properties.manager.PropertiesManager
import com.typesafe.config.Config
import io.github.config4k.toConfig
import io.ktor.application.install

import io.ktor.server.testing.withTestApplication
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestTemplate
import java.util.*
import kotlin.collections.HashMap

internal class FeatureTest{
    @Test
    fun testTemplate(){
        withTestApplication(
                {
                    install(PropertiesManager){

                    }
                }
        ){
            PropertiesManager.properties<Config>("app").apply {
                assert(this!=null)
                assert(this!!.getBoolean("c"))
                assert(this!!.getString("d")=="ahahah")
                assert(this!!.getInt("a")==1)
                assert(this!!.getDouble("b")==1.25)
            }
            PropertiesManager.properties<Config>("app2.app").apply {
                assert(this!=null)
                assert(this!!.getBoolean("c"))
                assert(this!!.getString("d")=="ahahah")
                assert(this!!.getInt("a")==1)
                assert(this!!.getDouble("b")==1.25)
            }

        }
    }

}