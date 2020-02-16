package cn.zenliu.ktor.features


import cn.zenliu.ktor.features.properties.manager.*
import com.typesafe.config.*
import io.ktor.application.*
import io.ktor.server.testing.*
import org.junit.jupiter.api.*

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
