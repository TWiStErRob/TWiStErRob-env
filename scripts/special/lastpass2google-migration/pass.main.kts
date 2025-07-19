@file:DependsOn("com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.19.2")
@file:DependsOn("org.skyscreamer:jsonassert:1.5.3")

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.reflect.KClass

fun main() {
	val dir = File("...")
	val google = dir.resolve("Google Passwords.csv").inputStream()
			.readCsv(GooglePassword::class, Schemas.google)
	val googleMapped = google.map {
		it/*.copy(
			name = it.url.host
		)*/
	}
	dir.resolve("Google Passwords-rewrite.csv").outputStream()
		.writeCsv(googleMapped.sortedBy { it.url + it.username }, Schemas.google)

	val lastpassExport = dir.resolve("lastpass_vault_export.csv").inputStream()
		.readCsv(LastPassPassword::class, Schemas.lastpass)
	dir.resolve("lastpass_vault_export-rewrite.csv").outputStream()
		.writeCsv(lastpassExport, Schemas.lastpass)
	val migrated = lastpassExport.map {
		GooglePassword(
			name = it.extra.name ?: it.url.host,
			url = it.url,
			username = it.username,
			password = it.password,
			note = it.extra.remap(it.grouping),
		)
	}
	dir.resolve("migrated.csv").outputStream()
		.writeCsv(migrated, Schemas.google)

	fun <T> Map<String, List<T>>.printPasswords() {
		forEach { println("Key: " + it.key); it.value.forEach(::println); println() }
	}

	val googlePasswords = google.groupBy { it.url.host }
	val lastPasswords = lastpassExport.groupBy { it.url.host }
	println("=== Google multi-account per host ===")
	googlePasswords.filterValues { it.size > 1 }.printPasswords()
	println("=== Lastpass multi-account per host ===")
	lastPasswords.filterValues { it.size > 1 }.printPasswords()

	println("=== Both has it ===")
	val both = (googlePasswords.keys intersect lastPasswords.keys)
	println(both)
}

val String.host: String
	get() =
		if (this.startsWith("android://"))
			""
		else
			this.split("/") // Assume protocol://host/path -> [protocol:, "", host, path] split.
				.takeIf { it.size > 2 }
				?.get(2)
				?: error("Not a valid url: $this")

val String.name: String?
	get() = this
		.lines()
		.dropWhile { it.isEmpty() }
		.firstOrNull()
		?.takeIf { it.startsWith("Name: ") }
		?.removePrefix("Name: ")

fun String.remap(category: String): String {
	var drop = 0
	// Hard trim, to prevent parsing [""].
	val lines = this.lines().dropWhile { it.isEmpty() }.dropLastWhile { it.isEmpty() }
	val nameLine = lines.firstOrNull()?.takeIf { it.startsWith("Name: ") }?.also { drop++ }
	lines.drop(drop).firstOrNull()?.takeIf { it.startsWith("Category: ") }?.also { drop++ }
	val catLine = "Category: $category"
	val restLines = lines.drop(drop).takeIf { it.isNotEmpty() }?.joinToString("\n")
	return listOfNotNull(nameLine, catLine, restLines).joinToString("\n")
}

object Schemas {

	val google: CsvSchema = CsvSchema.builder()
		.setUseHeader(true)
		.setStrictHeaders(true)
		.addColumn("name")
		.addColumn("url")
		.addColumn("username")
		.addColumn("password")
		.addColumn("note")
		.build()

	val lastpass: CsvSchema = CsvSchema.builder()
		.setUseHeader(true)
		.setStrictHeaders(true)
		.addColumn("url")
		.addColumn("username")
		.addColumn("password")
		.addColumn("totp")
		.addColumn("extra")
		.addColumn("name")
		.addColumn("grouping")
		.addColumn("fav")
		.build()
}

data class GooglePassword(
	@field:JsonProperty("name") val name: String,
	@field:JsonProperty("url") val url: String,
	@field:JsonProperty("username") val username: String,
	@field:JsonProperty("password") val password: String,
	@field:JsonProperty("note") val note: String,
) {

	constructor() : this("", "", "", "", "")
}

data class LastPassPassword(
	@field:JsonProperty("url") val url: String,
	@field:JsonProperty("username") val username: String,
	@field:JsonProperty("password") val password: String,
	@field:JsonProperty("totp") val totp: String,
	@field:JsonProperty("extra") val extra: String,
	@field:JsonProperty("name") val name: String,
	@field:JsonProperty("grouping") val grouping: String,
	@field:JsonProperty("fav") val fav: String,
) {

	constructor() : this("", "", "", "", "", "", "", "")
}

val csvMapper = CsvMapper().apply {
	enable(CsvParser.Feature.TRIM_SPACES)
	enable(CsvParser.Feature.SKIP_EMPTY_LINES)
}

fun <T : Any> InputStream.readCsv(klass: KClass<T>, schema: CsvSchema): List<T> =
	csvMapper.readerFor(klass.java)
		.with(schema)
		.readValues<T>(this)
		.readAll()

fun <T : Any> OutputStream.writeCsv(data: List<T>, schema: CsvSchema) {
	csvMapper.writer()
		.with(schema)
		.writeValues(this).writeAll(data)
}

main()
