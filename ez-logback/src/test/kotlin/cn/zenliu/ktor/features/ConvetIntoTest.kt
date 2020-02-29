package cn.zenliu.ktor.features

import cn.zenliu.ktor.features.logback.*
import cn.zenliu.ktor.features.logback.DirectHOCONConfigurator.Companion.convertInto
import com.typesafe.config.*
import org.junit.jupiter.api.*
import java.io.*
import java.nio.*
import javax.xml.parsers.*
import javax.xml.transform.*
import javax.xml.transform.dom.*
import javax.xml.transform.stream.*

internal class ConvetIntoTest {
	@Test
	fun testParse(){
		val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
		val cfg = doc.createElement("configuration")
		doc.appendChild(cfg)
		ConfigFactory.parseString(DirectHOCONConfigurator.DEFAULT_LOGBACK).getConfig("logback").getConfig("config").convertInto(doc, cfg)
		val buf = ByteBuffer.allocate(1024 * 20)
		TransformerFactory.newInstance().newTransformer()
			.transform(DOMSource(doc), StreamResult(object : OutputStream() {
				override fun write(b: Int) {
					buf.put(b.toByte())
				}
			}))
		val outbuf = buf.flip() as ByteBuffer
		File("HOCON.logback.debug.xml").apply {
			if (this.exists()) this.delete()
			this.createNewFile()
			this.appendText(Charsets.UTF_8.decode(outbuf.duplicate()).toString())
		}
	}
}
