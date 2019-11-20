package cn.zenliu.ktor.features.logback


import ch.qos.logback.classic.LoggerContext
import ch.qos.logback.classic.joran.JoranConfigurator
import ch.qos.logback.classic.spi.Configurator
import ch.qos.logback.core.CoreConstants
import ch.qos.logback.core.joran.util.ConfigurationWatchListUtil
import ch.qos.logback.core.spi.ContextAwareBase
import com.typesafe.config.*
import io.github.config4k.extract
import org.intellij.lang.annotations.Language
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult


class HoconConfigurator : ContextAwareBase(), Configurator {
    override fun configure(loggerContext: LoggerContext) {
        val configurator = JoranConfigurator()
        configurator.context = loggerContext
        configurator.doConfigure(loadAndParseXml(loggerContext))
    }

    private fun getSourceFiles(config: ConfigValue, files: MutableSet<URL>): Set<URL> {
        if (config.origin().filename() != null) {
            // Check 'contains' first to avoid re-adding, and messing with the order
            if (!files.contains(config.origin().url())) {
                files.add(config.origin().url())
            }
        }
        if (config.valueType() == ConfigValueType.OBJECT) {
            for (value in (config as ConfigObject).values) {
                getSourceFiles(value, files)
            }
        }
        return files
    }

    private fun createChangeTask(loggerContext: LoggerContext, config: Config) {
        if (config.hasPath("duration") && !config.getIsNull("duration")) {
            val delay = config.getDuration("duration", TimeUnit.MILLISECONDS)
            if (delay > 0) {
                val rocTask = {
                    ConfigurationWatchListUtil.getConfigurationWatchList(loggerContext)?.let { watch ->
                        watch.copyOfFileWatchList?.let {
                            when {
                                it.isEmpty() -> {
                                    addInfo("Empty watch file list. Disabling ")
                                    1
                                }
                                !watch.changeDetected() -> 1
                                else -> 1
                            }
                        }
                    }.let {
                        if (it == null) {
                            addWarn("Null ConfigurationWatchList in context")
                        } else {
                            loggerContext.reset()
                            configure(loggerContext)
                        }
                    }
                }
                loggerContext.putObject(CoreConstants.RECONFIGURE_ON_CHANGE_TASK, rocTask)
                loggerContext.addScheduledFuture(
                        loggerContext.scheduledExecutorService.scheduleAtFixedRate(
                                rocTask,
                                delay,
                                delay,
                                TimeUnit.MILLISECONDS
                        )
                )
            }
        }
    }

    private fun loadAndParseXml(loggerContext: LoggerContext): InputStream? {
        var inner: Boolean = false
        val config = try {
            ConfigFactory.load().getConfigSafe("logback")
        } catch (t: Throwable) {
            addError("Unable to load logback config", t)
            null
        } ?: run {
            addWarn("Will load default configuration")
            inner = true
            ConfigFactory.parseString(default)
        }.let { config ->
            (config.extract<String?>("configuration") ?: "").let {
                (if (it.isBlank()) config else config.getConfigSafe(it)) ?: run {
                    addError("Unable to parse logback config", Throwable("lost current configuration $it"))
                    return null
                }
            }
        }
        if (!inner && config.hasPath("auto-reload")) {
            if (registerFileWatchers(loggerContext, getSourceFiles(config.getValue("auto-reload"), mutableSetOf()))) {
                createChangeTask(loggerContext, config.getConfig("auto-reload"))
            }
        }

        //convert into xml
        return DocumentBuilderFactory.newInstance().let { factory ->
            factory.newDocumentBuilder().let { builder ->
                builder.newDocument().let { doc ->
                    doc.createElement("configuration").let { elConf ->
                        doc.appendChild(elConf)
                        config.getConfigSafe("appenders")?.addNodes(doc, elConf, name = "appender")
                        config.getConfigSafe("statusListener")?.addNodes(doc, elConf, name = "statusListener")
                        config.getConfigSafe("root")?.let {
                            doc.createElement("root").let { elRoot ->
                                it.addNodes(doc, elRoot)
                                elConf.appendChild(elRoot)
                            }
                        }
                        config.getConfigSafe("loggers")?.addNodes(doc, elConf, name = "logger")
                    }
                    val buf = ByteBuffer.allocate(1024 * 20)

                    TransformerFactory.newInstance().newTransformer()
                            .transform(DOMSource(doc), StreamResult(object :OutputStream(){
                                override fun write(b: Int) {
                                    buf.put(b.toByte())
                                }
                            }))
                    buf.flip() as ByteBuffer
                }
            }
        }.let {
            if (System.getProperty("DEBUG") != null) {
                File("HOCON.logback.debug.xml").apply {
                    if (this.exists()) this.delete()
                    this.createNewFile()
                    this.appendText(Charsets.UTF_8.decode(it.duplicate()).toString())
                }
            }
            object :InputStream(){
                override fun read(): Int =it.get().toInt()
            }
        }


    }

    private fun registerFileWatchers(loggerContext: LoggerContext, sourceFiles: Set<URL>): Boolean {
        val iterator = sourceFiles.iterator()
        if (iterator.hasNext()) {
            ConfigurationWatchListUtil.setMainWatchURL(loggerContext, iterator.next())
            iterator.forEachRemaining { u -> ConfigurationWatchListUtil.addToWatchList(loggerContext, u) }
            return true
        }
        addWarn("No configuration files to watch, so no file scanning is possible")
        return false
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
                ConfigValueType.NULL -> TODO()
                ConfigValueType.STRING -> when {
                    k == "value" -> root.appendChild(doc.createTextNode(this.getString(k)))
                    else -> root.setAttribute(k, this.getString(k))
                }
                else -> TODO()
            }
        }
    }

    companion object {
        private fun Config.getConfigSafe(path: String): Config? = try {
            this.getConfig(path)
        } catch (E: Throwable) {
            null
        }

        @Language("HOCON")
        const val default = """
logback {
  appenders: {
    STDOUT: {
      class = "ch.qos.logback.core.ConsoleAppender"
      encoder {
        pattern.value = "%d{YYYY-MM-dd HH:mm:ss.SSS} %highlight(%-5level) %green([%-4.30thread]) %blue(%logger{36}) %boldGreen(\\(%F:%line\\)) - %msg%n"
      }
    }
  }
  root {
    level = INFO
    appender-ref=[STDOUT]
  }
  loggers {
    "ktor.application": {level = INFO}
    "org.jooq.Constants": {level = ERROR}
    "org.eclipse.jetty": {level = INFO}
    "io.netty": {level = INFO}
    "io.lettuce.core": {level = INFO}
    "com.zaxxer": {level = INFO}
    "org.jooq": {level = INFO}
  }
}
"""
    }


}
