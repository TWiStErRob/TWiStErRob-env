// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:4.1")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.7.2")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-okhttp4:1.7.2")

import com.opencsv.CSVReader
import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.model.common.ExternalFileDetails
import notion.api.v1.model.common.File
import notion.api.v1.model.common.FileType
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.databases.Database
import notion.api.v1.model.databases.DatabaseProperty
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import notion.api.v1.model.pages.PageProperty

@Suppress("SpreadOperator")
main(*args)
fun java.io.File.readCSV(): List<Array<String>> =
	reader().use { reader ->
		CSVReader(reader).use { csvReader ->
			csvReader.readAll()
		}
	}

fun main(vararg args: String) {
	check(args.size == 2) { "Usage: kotlinc -script notion-import-csv.main.kts <databaseID> <csvFileName>" }
	val csv = java.io.File(args[1]).readCSV()
	val headers = csv[0]
	val data = csv.drop(1)

	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")

	NotionClient(token = secret).apply { httpClient = OkHttp4Client(connectTimeoutMillis = 30_000) }.use { client ->
		val database = client.retrieveDatabase(args[0])
		val properties = headers.map { header -> database.property(header) }
		data.forEach { row ->
			check(row.size == headers.size) {
				"Row has ${row.size} columns, expected ${headers.size}.\n{${headers.contentToString()}\n${row.contentToString()}"
			}
			val icon = if ("icon" in headers) row[headers.indexOf("icon")].takeIf { it.isNotBlank() } else null
			client.createPage(
				parent = PageParent.database(database.id),
				icon = icon?.let { File(type = FileType.External, external = ExternalFileDetails(url = it)) },
				properties = row.mapIndexedNotNull { index, value ->
					when (headers[index]) {
						"icon" -> {
							// No property value for "icon" as it's a special separate field on "Page".
							null
						}
						else -> {
							val property = properties[index]!!
							property.convert(client, value)?.let { property.name!! to it }
						}
					}
				}.toMap()
			)
		}
	}
}

@Suppress("ComplexMethod")
fun DatabaseProperty.convert(client: NotionClient, value: String): PageProperty? {
	if (value.isBlank()) return null
	return when (type) {
		PropertyType.RichText -> PageProperty(richText = value.asRichText())
		PropertyType.Number -> PageProperty(number = value.toDouble())
		PropertyType.Select -> PageProperty(
			select = this.select!!.options!!.single { it.name == value }
		)
		PropertyType.MultiSelect -> PageProperty(
			multiSelect = value.split(",").map { multiSelect!!.options!!.single { it.name == value } }
		)
		PropertyType.Date -> {
			val match = """^(?<start>.*?)(?:->|→)(?<end>.*?)$""".toRegex().matchEntire(value)
			val (start, end) = 
				if (match != null) {
					match.groupValues[1].trim() to match.groupValues[2].trim()
				} else {
					value to null
				}
			fun String.fixDate(): String =
				if (this.matches("""^\d{4}/\d{2}/\d{2}$""".toRegex()))
					this.replace('/', '-')
				else
					this
			PageProperty(
				date = PageProperty.Date(
					start = start.fixDate(),
					end = end?.fixDate(),
				)
			)
		}
		PropertyType.Formula -> TODO()
		PropertyType.Relation -> PageProperty(
			relation = value.split(",").map { PageProperty.PageReference(it.trim().takeLast(32)) }
		)
		PropertyType.Rollup -> null
		PropertyType.Title -> PageProperty(title = value.asRichText())
		PropertyType.People -> PageProperty(people = value.split(",").map { client.retrieveUser(value.trim()) })
		PropertyType.Files -> PageProperty(
			files = value.split(",").map { url ->
				PageProperty.File(
					name = url,
					type = FileType.External,
					external = ExternalFileDetails(url = url)
				)
			}
		)
		PropertyType.Checkbox -> PageProperty(checkbox = value.toBoolean())
		PropertyType.Url -> PageProperty(url = value)
		PropertyType.Email -> PageProperty(email = value)
		PropertyType.PhoneNumber -> PageProperty(phoneNumber = value)
		PropertyType.CreatedTime -> PageProperty(createdTime = value)
		PropertyType.CreatedBy -> PageProperty(createdBy = client.retrieveUser(value))
		PropertyType.LastEditedTime -> PageProperty(lastEditedTime = value)
		PropertyType.LastEditedBy -> PageProperty(lastEditedBy = client.retrieveUser(value))
		PropertyType.PropertyItem -> TODO()
	}
}

fun NotionClient.allPages(databaseId: String): List<Page> =
	generateSequence(queryDatabase(databaseId)) { results ->
		results.nextCursor?.let { queryDatabase(databaseId, startCursor = it) }
	}.flatMap { it.results }.toList()

fun String.asRichText(): List<PageProperty.RichText> =
	listOf(PageProperty.RichText(text = PageProperty.RichText.Text(content = this)))

val Page.title: String?
	get() {
		val prop = this.properties.values.singleOrNull { it.id == "title" && it.type == PropertyType.Title }
			?: error("Missing property of type 'title', available properties: ${this.properties.keys}")
		val title = prop.title ?: error("Missing title structure")
		return title.singleOrNull()?.plainText
	}

fun Database.property(name: String): DatabaseProperty? {
	val prop = properties[name]
	val special = name == "icon"
	return when {
		prop == null && special -> {
			null
		}

		prop == null && !special -> {
			error(
				"No property named '${name}' in database, pick one of ${properties.keys}.\n" +
						"If the column you're missing is a Relation, make sure the referenced Database also has the Connection.")
		}

		prop != null && special -> {
			error("Property name '${name}' is reserved for special use.")
		}

		else -> {
			prop
		}
	}
}
