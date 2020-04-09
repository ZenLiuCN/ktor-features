package cn.zenliu.ktor.features.ebean

import io.ebean.*
import io.ebean.annotation.*
import io.ebean.config.*
import io.ebean.dbmigration.DbMigration
import java.io.*
import java.time.*
import java.time.format.*
import java.util.zip.*
import javax.sql.*


/**
 * useable util for ebean
 * current is just migration generate for liquibase
 */
object EbeanUtil {
	private fun now(pattern: String = "YYYYMMdd") = Instant.now().atOffset(ZoneOffset.ofHours(8)).format(DateTimeFormatter.ofPattern(pattern))
	/**
	 * generate liquibase sql changelogs
	 *
	 * below is how to config maven to ignore package temp files
	 *
	 * if: we use `resources/schema/` as changelogs folder
	 *
	 *        use `resources/schema_backup/` as backup folder
	 *
	 *        use `resources/` as generate resourcePath folder
	 *
	 *  ```xml
	 *  <!-- star is replace as # -->
	 *   <build>
	 *    <resources>
	 *      <resource>
	 *          <directory>src/main/resources</directory>
	 *          <excludes>
	 *              <exclude>##/#test#</exclude>
	 *              <exclude>##/dbmigration/##</exclude>
	 *              <exclude>##/schema_backup/##</exclude>
	 *          </excludes>
	 *          <filtering>false</filtering>
	 *      </resource>
	 *    </resources>
	 *  </build>
	 *  ```
	 *  header template can be config with property [EbeanUtil.generateLiquibaseChangeLog.header] with place holder `{changeset}`
	 * @param user String  liquibase author
	 * @param changeSetName String liquibase changeset name
	 * @param majorVersion Int  finnal file name will be majorVersion.minorVersion.typeNumber__changeSetName.sql
	 * @param minorVersion Int
	 * @param typeNumber Int  default to 0 ;suggested : 0 schema 1 init data(so you chould add by yourself)
	 * @param resourcePath String? where should generate dbmigration files and dirs in
	 * @param outputPath String? where put all changelogs
	 * @param backupPath String?  where to put zipped old changelogs
	 * @param dataSource DataSource? must set datasource or platform
	 * @param platform Platform?
	 */
	fun generateLiquibaseChangeLog(
		user: String,
		changeSetName: String,
		majorVersion: Int,
		minorVersion: Int,
		typeNumber: Int = 0,
		resourcePath: String? = null,
		outputPath: String? = null,
		backupPath: String? = null,
		dataSource: DataSource? = null,
		platform: Platform? = null,
		configure:((DbMigration)->Unit)?=null
	) {
		if (dataSource == null && platform == null) throw Throwable("please set datasource or platform")
		dataSource?.let {
			DatabaseFactory.create(DatabaseConfig().apply {
				this.currentUserProvider = object : CurrentUserProvider {
					override fun currentUser() = 0L
				}
				this.dataSource = dataSource
			})
		}
		DbMigration.create()
			.apply {
				platform?.let { setPlatform(platform) }
				val changeset="$user-${now("YYYYMMdd")}:$changeSetName"
				val header= System.getProperty("EbeanUtil.generateLiquibaseChangeLog.header")?.takeIf { it.isNotBlank() }
					?:"""
					--liquibase formatted sql
					--changeset {changeset}
					-- THIS IS GENERATED FORM KTOR-FEATURES:EBEANUTIL SHOULD NOT EDIT: !! PLZ check must have --liquibase formatted sql  (--WITHOUT SPACE MUST HAVE IT WITH MYSQL) IN FIRST LINE
					""".trimIndent()
				setHeader(header.replace("{changeset}",changeset))
				setName(changeSetName)
				setVersion("$majorVersion.$minorVersion.$typeNumber")
				resourcePath?.let { setPathToResources(File(it).absolutePath) }
				setLogToSystemOut(true)
				setIncludeGeneratedFileComment(true)
				configure?.invoke(this)
				generateMigration()?.let {
					resourcePath ?: return
					outputPath?.let { out ->
						File(out).apply { if (!exists()) this.mkdirs() }.listFiles()
							?.let { old ->
								backupPath?.let { bk -> File("$bk/${now("YYYYMMdd_HHmmss")}.zip") }
									?.let {
										if (!it.parentFile.exists()) it.parentFile.mkdirs()
										ZipOutputStream(it.outputStream())
									}?.use { os ->
										old.filter { it.isFile }.forEach {
											os.putNextEntry(ZipEntry(it.name))
											os.write(it.readBytes())
											os.closeEntry()
											it.delete()
										}
									}
							}
						File("$resourcePath/dbmigration").listFiles()
							?.filter { it.isFile && it.name.endsWith(".sql") }
							?.forEach {
								it.copyTo(File("$out/${it.name}"), true)
							}
					}
				}
				Unit
			}
	}
}
