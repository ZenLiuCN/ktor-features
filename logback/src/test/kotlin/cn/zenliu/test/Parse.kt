package cn.zenliu.test

import com.fasterxml.jackson.databind.util.ByteBufferBackedOutputStream
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueType
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import java.nio.ByteBuffer
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class Parse {
    private fun Config.getConfigSafe(path: String): Config? = try {
        this.getConfig(path)
    } catch (E: Throwable) {
        null
    }

    @Test
    fun parse() {
        val config = ConfigFactory.load().getConfigSafe("logback")!!
        DocumentBuilderFactory.newInstance().let { factory ->
            factory.newDocumentBuilder().let { builder ->
                builder.newDocument().let { doc ->
                    doc.createElement("configuration").let { elConf ->
                        doc.appendChild(elConf)
                        config.getConfigSafe("appenders")?.addNodes(doc, elConf, name = "appender")
                        config.getConfigSafe("statusListener")?.addNodes(doc, elConf, name = "statusListener")
                        config.getConfigSafe("root")?.let {
                            doc.createElement("root").let { elRoot ->
                                elConf.appendChild(elRoot)
                                it.addNodes(doc, elRoot)
                            }
                        }
                        config.getConfigSafe("loggers")?.addNodes(doc, elConf, name = "logger")
                    }
                    val buf = ByteBuffer.allocate(1024 * 20)
                    TransformerFactory.newInstance().newTransformer()
                        .transform(DOMSource(doc), StreamResult(ByteBufferBackedOutputStream(buf)))

                    buf.flip() as ByteBuffer
                }
            }
        }.let {
            File("HOCON.logback.debug.xml").apply {
                if (this.exists()) this.delete()
                this.createNewFile()
                this.appendText(Charsets.UTF_8.decode(it.duplicate()).toString())
            }
            Charsets.UTF_8.decode(it.duplicate()).toString().apply {
                println(Charsets.UTF_8.decode(it))
            }

        }.let(::println)

    }

    private fun Config.addNodes(doc: Document, root: Element, name: String? = null) {
        this.root().forEach { (k, v) ->
            when (v.valueType()) {
                ConfigValueType.OBJECT -> doc.createElement(name ?: k).let { knode ->
                    name?.let { knode.setAttribute("name", k) }
                    this.getConfig(k.let { if (k.contains(".")) "\"$k\"" else k }).addNodes(doc, knode)
                    root.appendChild(knode)
                }
                ConfigValueType.LIST -> {
                    when {
                        k == "appender-ref" -> this.getList(k).map { it.unwrapped() as? String }.filterNotNull().map {
                            doc.createElement(k).apply {
                                setAttribute("ref", it)
                            }
                        }.forEach {
                            root.appendChild(it)
                        }
                    }


                }
                ConfigValueType.NUMBER -> root.setAttribute(k, this.getNumber(k).toString())
                ConfigValueType.BOOLEAN -> root.setAttribute(k, this.getBoolean(k).toString())
                ConfigValueType.NULL -> kotlin.TODO()
                ConfigValueType.STRING -> when {
                    k == "value" -> root.appendChild(doc.createTextNode(this.getString(k)))
                    else -> root.setAttribute(k, this.getString(k))
                }
                else -> kotlin.TODO()
            }
        }
    }
}
