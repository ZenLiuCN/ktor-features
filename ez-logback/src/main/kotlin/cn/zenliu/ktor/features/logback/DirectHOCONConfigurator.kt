package cn.zenliu.ktor.features.logback

import ch.qos.logback.classic.*
import ch.qos.logback.classic.joran.*
import ch.qos.logback.classic.spi.*
import ch.qos.logback.core.*
import ch.qos.logback.core.joran.util.*
import ch.qos.logback.core.spi.*
import com.typesafe.config.*
import org.intellij.lang.annotations.*
import org.w3c.dom.*
import java.io.*
import java.net.*
import java.nio.*
import java.time.*
import java.util.concurrent.*
import javax.xml.parsers.*
import javax.xml.transform.*
import javax.xml.transform.dom.*
import javax.xml.transform.stream.*

class DirectHOCONConfigurator : ContextAwareBase(), Configurator {
	private lateinit var ctx: LoggerContext
	override fun configure(ctx: LoggerContext) {
		if (!this::ctx.isInitialized) this.ctx = ctx
		JoranConfigurator().apply {
			context = ctx
			doConfigure(parseConfiguration())
		}
	}

	private fun parseConfiguration(): InputStream? =
		runCatching {
			ConfigFactory.load().getConfig("logback")
		}.getOrNull().let {
			if (it != null && it.hasPath("autoReload")) {
				it.getConfig("autoReload").let { ar ->
					if (!ar.hasPath("disabled") || !ar.getBoolean("disabled")) {
						createChangeTask(it.getConfig("autoReload"))
					}
				}
			}
			runCatching { it?.let { it to it.getConfig("config") } }.getOrNull()
				?: runCatching {
					addWarn("could not parse configuration in application.conf,use default configuration")
					ConfigFactory.parseString(DEFAULT_LOGBACK).getConfig("logback").let { it to it.getConfig("config") }
				}.getOrNull()
		}?.let {(full,conf)->
			val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
			val cfg = doc.createElement("configuration")
			doc.appendChild(cfg)
			conf.convertInto(doc, cfg)
			doc.sort()
			val buf = ByteBuffer.allocate(1024 * 20)
			TransformerFactory.newInstance().newTransformer()
				.transform(DOMSource(doc), StreamResult(object : OutputStream() {
					override fun write(b: Int) {
						buf.put(b.toByte())
					}
				}))
			val outbuf = buf.flip() as ByteBuffer
			if (full.hasPath("debug") && !full.getIsNull("debug") && full.getBoolean("debug")) {
				File("HOCON.logback.debug.xml").apply {
					if (this.exists()) this.delete()
					this.createNewFile()
					this.appendText(Charsets.UTF_8.decode(outbuf.duplicate()).toString())
				}
			}

			object : InputStream() {
				override fun read(): Int = outbuf.takeIf { it.remaining()>0 }?.get()?.toInt()?:-1
			}
		}


	private val watchTask = {
		ConfigurationWatchListUtil.getConfigurationWatchList(ctx)
			.takeIf { it != null && it.copyOfFileWatchList.isNotEmpty() && it.changeDetected() }
			.let {
				if (it == null) {
					addWarn("Null ConfigurationWatchList in context")
				} else {
					ctx.reset()
					configure(ctx)
				}
			}
	}

	private fun createChangeTask(config: Config) {
		if (this::ctx.isInitialized) return
		val files = ({
			val files = mutableSetOf<URL>()
			config.origin().takeIf { it.filename() != null }?.let { files.add(it.url()) }
			runCatching { config.getStringList("files") }
				.getOrNull()
				?.map {
					File(it)
						.takeIf { it.exists() && it.isFile && it.canRead() && it.length() > 0 }
						?.toURI()
						?.toURL()
				}
				?.filterNotNull()
				?.let { files.addAll(it) }
			files
		})()
		if (!registerFileWatchers(files)) return

		val delay = config.takeIf { it.hasPath("duration") && !it.getIsNull("duration") }
			?.getDuration("duration")
			?.takeIf { !it.isZero && !it.isNegative && it.toMillis() >= 1000 }
			?: Duration.ofSeconds(60)

		ctx.putObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK, watchTask)
		ctx.addScheduledFuture(
			ctx.scheduledExecutorService.scheduleAtFixedRate(
				watchTask,
				delay.toMillis(),
				delay.toMillis(),
				TimeUnit.MILLISECONDS
			)
		)
	}

	private fun registerFileWatchers(sourceFiles: Set<URL>): Boolean {
		val iterator = sourceFiles.iterator()
		if (iterator.hasNext()) {
			ConfigurationWatchListUtil.setMainWatchURL(ctx, iterator.next())
			iterator.forEachRemaining { u -> ConfigurationWatchListUtil.addToWatchList(ctx, u) }
			return true
		}
		addWarn("No configuration files to watch, so no file scanning is possible")
		return false
	}

	companion object {
		@Language("HOCON")
		internal const val DEFAULT_LOGBACK = "\nlogback {\n  debug: false\n  autoReload {\n    disabled: true\n    duration: 1s #minimal: 1s default: 60s\n  }\n  config {\n    debug = false\n    appenders {\n      CONSOLE {\n        class = \"ch.qos.logback.core.ConsoleAppender\"\n        encoder {\n          pattern.value = \"%d{YYYY-MM-dd HH:mm:ss.SSS} %highlight(%-5level) %green([%-4.30thread]) %blue(%logger{36}) %boldGreen(\\\\(%F:%line\\\\)) - %msg%n\"\n        }\n      }\n      ROLLFILE: {\n        class = \"ch.qos.logback.core.rolling.RollingFileAppender\"\n        file = \"log/system.log\"\n        rollingPolicy {\n          class = \"ch.qos.logback.core.rolling.TimeBasedRollingPolicy\"\n          FileNamePattern.value = \"log/system_%d{yyyy-MM-dd}_log.zip\"\n          maxHistory.value = \"30\"\n        }\n        encoder {\n          pattern.value = \"%d{YYYY-MM-dd HH:mm:ss.SSS} %-4relative [%thread] %-5level %logger{35} - %msg%n\"\n        }\n      }\n    }\n    root {\n      level: info\n      ref: [CONSOLE,ROLLFILE]\n    }\n    loggers {\n      \"ktor.application\": {level = INFO}\n      \"org.jooq.Constants\": {level = ERROR}\n      \"org.eclipse.jetty\": {level = INFO}\n      \"io.netty\": {level = INFO}\n      \"io.lettuce.core\": {level = INFO}\n      \"com.zaxxer\": {level = INFO}\n      \"org.jooq\": {level = INFO}\n    }\n  }\n}\n		"

		internal fun Config.nestElement(doc: Document, current: Element,curr: Element){
			this.root().forEach { (k, v) ->
				when (v.valueType()) {
					ConfigValueType.OBJECT -> doc.createElement(k)//object should be a element
						.let { knode ->
							this.getConfig(k.takeIf { !k.contains(".") }?:"\"$k\"")
								.convertInto(doc, knode)
							curr.appendChild(knode)
						}
					ConfigValueType.LIST -> {
						when {
							k == "ref" -> this.getList(k)
								.map { it.unwrapped() as? String }
								.filterNotNull()
								.map {
									doc.createElement("appender-ref").apply {
										setAttribute("ref", it)
									}
								}.forEach {
									curr.appendChild(it)
								}
							else -> Unit//should only use <appender-ref> as list
						}
					}
					ConfigValueType.NUMBER -> curr.setAttribute(k, this.getNumber(k).toString())
					ConfigValueType.BOOLEAN -> curr.setAttribute(k, this.getBoolean(k).toString())
					ConfigValueType.NULL -> TODO()
					ConfigValueType.STRING -> when {
						//object value
						k == "value" -> curr.appendChild(doc.createTextNode(this.getString(k)))
						//attr in object
						else -> curr.setAttribute(k, this.getString(k))
					}
					else -> TODO()
				}
			}
			current.appendChild(curr)
		}
		internal fun Document.sort(){
			val conf=this.getElementsByTagName("configuration").item(0)
			val child=conf.childNodes
			(0 until child.length).map {child.item(it)}.let {
				it.forEach {
					conf.removeChild(it)
				}
				it.filter { it.nodeName=="property" }.forEach { conf.appendChild(it) }
				it.filter { it.nodeName=="appender" }.forEach { conf.appendChild(it) }
				it.filter { it.nodeName=="root" }.forEach { conf.appendChild(it) }
				it.filter { it.nodeName=="logger" }.forEach { conf.appendChild(it) }
				it.filterNot { it.nodeName=="logger"||it.nodeName=="root"|| it.nodeName=="appender"||it.nodeName=="property" }.forEach { conf.appendChild(it) }

			}

		}
		internal fun String.quite()=this.takeIf { !this.contains(".") }?:"\"$this\""
		internal fun Config.convertInto(doc: Document, curr: Element) {
			this.root().forEach { (k, v) ->
				when (v.valueType()) {
					ConfigValueType.OBJECT -> when {
						k == "appenders" ->  this.getConfig(k).root().forEach{
							k1,v1->
							when(v1.valueType()){
								ConfigValueType.OBJECT->this.getConfig(k).getConfig(k1.quite()).nestElement(doc, curr, doc.createElement("appender").apply {
									this.setAttribute("name",k1)
								})
								else->TODO()
							}
						}
						k == "loggers" -> this.getConfig(k).root().forEach{
							k1,v1->
							when(v1.valueType()){
								ConfigValueType.OBJECT->this.getConfig(k).getConfig(k1.quite()).nestElement(doc, curr, doc.createElement("logger").apply {
									this.setAttribute("name",k1)
								})
								else->TODO()
							}
						}
							else ->doc.createElement(k)//object should be a element
							.let { knode ->
								this.getConfig(k.takeIf { !k.contains(".") }?:"\"$k\"")
									.convertInto(doc, knode)
								curr.appendChild(knode)
							}
					}
					ConfigValueType.LIST -> {
						when {
							k == "ref" -> this.getList(k)
								.map { it.unwrapped() as? String }
								.filterNotNull()
								.map {
									doc.createElement("appender-ref").apply {
										setAttribute("ref", it)
									}
								}.forEach {
									curr.appendChild(it)
								}
							else -> Unit//should only use <appender-ref> as list
						}
					}
					ConfigValueType.NUMBER -> curr.setAttribute(k, this.getNumber(k).toString())
					ConfigValueType.BOOLEAN -> curr.setAttribute(k, this.getBoolean(k).toString())
					ConfigValueType.NULL -> TODO()
					ConfigValueType.STRING -> when {
						//object value
						k == "value" -> curr.appendChild(doc.createTextNode(this.getString(k)))
						//attr in object
						else -> curr.setAttribute(k, this.getString(k))
					}
					else -> TODO()
				}
			}
		}
	}
}
