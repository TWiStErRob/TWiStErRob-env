// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.8")
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
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.time.DateTimeException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KProperty1

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
		val relations = properties.map {
			if (it?.type == PropertyType.Relation)
				client.allPages(it.relation!!.databaseId!!)
			else
				emptyList()
		}
		val titleProperty = properties.filterNotNull().single { it.type == PropertyType.Title }
		val existingPages = client.getAllPagesByUniqueTitle(database.id)
		data.forEach { row ->
			check(row.size == headers.size) {
				"Row has ${row.size} columns, expected ${headers.size}.\n{${headers.contentToString()}\n${row.contentToString()}"
			}
			val title = row[headers.indexOf(titleProperty.name)]
			val icon = if ("icon" in headers) row[headers.indexOf("icon")].takeIf { it.isNotBlank() } else null
			val existing = existingPages[title.lowercase()].orEmpty()
			val iconValue = icon?.let { File(type = FileType.External, external = ExternalFileDetails(url = it)) }
			val propertyValues = row.mapIndexedNotNull { index, value ->
				when (headers[index]) {
					"icon" -> {
						// No property value for "icon" as it's a special separate field on "Page".
						null
					}

					else -> {
						val property = properties[index]!!
						property.convert(client, value, relations[index])?.let { property.name!! to it }
					}
				}
			}.toMap()
			if (existing.isEmpty()) {
				client.createPage(database, iconValue, propertyValues)
			} else {
				check(existing.size == 1) {
					val duplicateDescription = existing
						.joinToString(separator = "\n") { "${title}: ${existing.map { it.url }}" }
					"Cannot update duplicate pages:\n$duplicateDescription"
				}
				client.updatePage(existing.single(), iconValue, propertyValues)
			}
		}
	}
}

fun NotionClient.getAllPagesByUniqueTitle(database: String): Map<String, List<Page>> =
	this.allPages(database)
		// TODO ability to key by multiple properties
		// example: What’s new with Amazon Appstore for Developers (Meet at devLounge):
		// https://www.notion.so/What-s-new-with-Amazon-Appstore-for-Developers-Meet-at-devLounge-1587d2a54f1845a1850653895d4aa1cd
		// https://www.notion.so/What-s-new-with-Amazon-Appstore-for-Developers-Meet-at-devLounge-6ed906121f514addb58cf039b1cd13a2
		.groupBy { it.title?.lowercase() ?: error("Page without title: ${it.url}") }

fun NotionClient.createPage(database: Database, icon: File?, properties: Map<String, PageProperty>) {
	this.createPage(
		parent = PageParent.database(database.id),
		icon = icon,
		properties = properties
	)
}

fun NotionClient.updatePage(page: Page, icon: File?, properties: Map<String, PageProperty>) {
	val (filtered, ignored, conflicting) = classify(page.properties, properties)
	if (conflicting.isNotEmpty()) {
		val conflictDetails = conflicting.entries.joinToString(separator = "\n\n") { (name, props) ->
			"${name} (old):\n${props.first}\n${name} (new):\n${props.second}"
		}
		error(
			"Existing page '${page.title}' (${page.url}) has conflicting values:\n" +
					conflictDetails +
					"\nPlease update the CSV data or the target Database in Notion to resolve this."
		)
	}
	if (filtered.isNotEmpty()) {
		if (ignored.isNotEmpty()) {
			println("Ignoring redundant properties on '${page.title}' (${page.url}): ${ignored.keys}")
		}
		this.updatePage(
			pageId = page.id,
			icon = icon,
			properties = filtered
		)
	} else {
		println("All properties (${ignored.keys}) were up to date in '${page.title}' (${page.url})")
	}
}

@Suppress("ComplexMethod", "LongMethod")
fun DatabaseProperty.convert(client: NotionClient, value: String, pages: List<Page>): PageProperty? {
	if (value.isBlank()) return null
	// Cast to nullable to prevent GSON-null problems.
	// https://github.com/seratch/notion-sdk-jvm/issues/81
	@Suppress("MoveVariableDeclarationIntoWhen", "RemoveRedundantQualifierName")
	val propertyType = type as notion.api.v1.model.common.PropertyType?
	return when (propertyType) {
		null -> {
			System.err.println("Ignoring unknown property type: ${this.name}")
			null
		}
		PropertyType.RichText -> PageProperty(richText = value.asRichText())
		PropertyType.Number -> PageProperty(number = value.toDouble())
		PropertyType.Select -> PageProperty(
			select = this.select!!.options!!.singleOrNull { it.name == value }
//				?: error("Cannot find option '${value}' in ${this.select!!.options!!.map { it.name!! }}")
				?: DatabaseProperty.Select.Option(name = value)
					.also {
						val existingOptions = this.select!!.options!!.map { it.name!! }
						System.err.println("WARNING: Select option ${value} not found in ${existingOptions}, creating it.")
					}
		)
		PropertyType.MultiSelect -> PageProperty(
			multiSelect = value.split(",").map { option ->
				multiSelect!!.options!!.singleOrNull { it.name == option }
//					?: error("Cannot find option '${value}' in ${this.multiSelect!!.options!!.map { it.name!! }}")
					?: DatabaseProperty.MultiSelect.Option(name = option)
						.also {
							val existingOptions = this.multiSelect!!.options!!.map { it.name!! }
							System.err.println(
								"WARNING: Multi-select option ${option} from ${value} not found in ${existingOptions}, creating it."
							)
						}
			}
		)
		PropertyType.Date -> {
			val (start, end) = parseDateRange(value)
			PageProperty(
				date = PageProperty.Date(
					start = start,
					end = end,
				)
			)
		}
		PropertyType.Formula -> TODO()
		PropertyType.Relation -> PageProperty(
			relation = value.parseReferencedIds(pages).map { PageProperty.PageReference(it) }
		)
		PropertyType.Rollup -> null
		PropertyType.Title -> PageProperty(title = value.asRichText())
		PropertyType.People -> PageProperty(people = value.split(",").map { client.retrieveUser(value.trim()) })
		PropertyType.Files -> PageProperty(
			files = value.split(",").map { url ->
				PageProperty.File(
					name = "file",
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

fun parseDateRange(value: String): Pair<String, String?> {
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

	val start1 = start.fixDate()
	val end1 = end?.fixDate()
	return Pair(start1, end1)
}

fun String.parseReferencedIds(pages: List<Page>): List<String> {
	fun findPage(title: String): Page {
		val found = pages.filter { it.title == title }
		when {
			found.isEmpty() -> error("Cannot find ${title} in ${pages.map { it.title }}")
			found.size == 1 -> return found.single()
			else -> error("Found multiple pages with title ${title}: ${found.map { it.url }}")
		}
	}

	return when {
		// 0123456789abcdef0123456789abcdef
		// 0123456789abcdef0123456789abcdef,0123456789abcdef0123456789abcdef
		// https://www.notion.so/Page-Title-0123456789abcdef0123456789abcdef,https://www.notion.so/Page-Title-0123456789abcdef0123456789abcdef
		this.matches("""^((https://www.notion.so/([^,]*)-)?[0-9a-f]{32},?)+$""".toRegex()) ->
			this.split(",").map { it.trim().takeLast(32) }

		pages.any { it.title == this } ->
			listOf(findPage(this).id)

		else ->
			this.split(",").map { findPage(it).id }
	}
}

fun NotionClient.allPages(databaseId: String): List<Page> =
	generateSequence(queryDatabase(databaseId)) { results ->
		results.nextCursor?.let { queryDatabase(databaseId, startCursor = it) }
	}.flatMap { it.results }.toList()

val PageProperty.isRichText: Boolean
	get() = this.type == PropertyType.RichText || this.type == PropertyType.Title

fun String.asRichText(): List<PageProperty.RichText> =
	// As best effort to try to get over the validation errors:
	// > body failed validation: body.properties.body.rich_text[0].text.content.length should be ≤ `2000`
	// > body failed validation: body.properties.body.rich_text.length should be ≤ `100`
	// Tried splitting on new-lines, but then it easily becomes too many RichText objects.
	this
		.windowed(2000, 2000, true) {
			PageProperty.RichText(text = PageProperty.RichText.Text(content = it.toString()))
		}

fun List<PageProperty.RichText>.asString(): String =
	this.joinToString(separator = "") { it.text!!.content!! }

val Page.title: String?
	get() {
		val title = this.titleProperty.title ?: error("Missing title structure in ${this.url}")
		return title.asString().takeIf { it.isNotBlank() }
	}

val Page.titleProperty: PageProperty
	get() = this.properties.values.singleOrNull { it.type == PropertyType.Title }
		?: error("Missing property of type 'title', available properties: ${this.properties.mapValues { it.value.type }}")

val PropertyType.associatedProperty: KProperty1<PageProperty, Any?>
	get() = when (this) {
		PropertyType.RichText -> PageProperty::richText
		PropertyType.Number -> PageProperty::number
		PropertyType.Select -> PageProperty::select
		PropertyType.MultiSelect -> PageProperty::multiSelect
		PropertyType.Date -> PageProperty::date
		PropertyType.Formula -> PageProperty::formula
		PropertyType.Relation -> PageProperty::relation
		PropertyType.Rollup -> PageProperty::rollup
		PropertyType.Title -> PageProperty::title
		PropertyType.People -> PageProperty::people
		PropertyType.Files -> PageProperty::files
		PropertyType.Checkbox -> PageProperty::checkbox
		PropertyType.Url -> PageProperty::url
		PropertyType.Email -> PageProperty::email
		PropertyType.PhoneNumber -> PageProperty::phoneNumber
		PropertyType.CreatedTime -> PageProperty::createdTime
		PropertyType.CreatedBy -> PageProperty::createdBy
		PropertyType.LastEditedTime -> PageProperty::lastEditedTime
		PropertyType.LastEditedBy -> PageProperty::lastEditedBy
		PropertyType.PropertyItem -> error("Who dis? ${this}")
	}

fun Database.property(name: String): DatabaseProperty? {
	val prop = properties[name]
	val special = name == "icon"
	return when {
		prop == null && special ->
			null

		prop == null && !special ->
			error(
				"No property named '${name}' in database, pick one of ${properties.keys}.\n" +
						"If the column you're missing is a Relation, make sure the referenced Database also has the Connection."
			)

		prop != null && special ->
			error("Property name '${name}' is reserved for special use.")

		else ->
			prop
	}
}

/**
 * Filters the [new] list into lists, where
 * 1. is good, the values that are fresh and new in [new].
 * 2. is sketchy, the values that are redundant between [old] and [new], if `PATCH`'d the value might lose formatting.
 * 3. is bad, the values here are different between [old] and [new], can't resolve the conflict.
 * @return `Triple<good, sketchy, bad>`
 */
fun classify(
	old: Map<String, PageProperty>,
	new: Map<String, PageProperty>,
): Triple<Map<String, PageProperty>, Map<String, PageProperty>, Map<String, Pair<PageProperty, PageProperty>>> {
	val (good, conflicting) = old
		// Property that is not updated can be ignored.
		.mapNotNull { (name, prop) -> new[name]?.let { Triple(name, prop, it) } }
		.partition { (_, old, new) -> isSimilar(old, new) }
	val (fresh, redundant) = good
		.partition { (_, old, _) ->
			val oldValue = old.type!!.associatedProperty.get(old)
			oldValue == null || (old.isRichText && oldValue == emptyList<PageProperty.RichText>())
		}
	return Triple(
		fresh.associateBy({ it.first }, { it.third }),
		redundant.associateBy({ it.first }, { it.third }),
		conflicting.associateBy({ it.first }, { it.second to it.third }),
	)
}

fun isSimilar(old: PageProperty, new: PageProperty): Boolean {
	val type = old.type!!
	// new.type is always null, so can't infer; use old.type instead for both.
	val oldValue = type.associatedProperty.get(old)
	val newValue = type.associatedProperty.get(new)
	fun comparable(value: Any?): Any? =
		when {
			value == null ->
				null
			old.type == PropertyType.Title ||
			old.type == PropertyType.RichText ->
				@Suppress("UNCHECKED_CAST")
				(value as List<PageProperty.RichText>).asString().lowercase()
			old.type == PropertyType.Url ->
				if (value.toString().toHttpUrl().host == "www.youtube.com") {
					// YouTube specific exception - these are all the same:
					// https://www.youtube.com/watch?v=yKfuq3luNVM
					// https://www.youtube.com/watch?v=yKfuq3luNVM&list=PLWz5rJ2EKKc_L3n1j4ajHjJ6QccFUvW1u&index=24
					// https://www.youtube.com/watch?v=yKfuq3luNVM&list=PLWz5rJ2EKKc_L3n1j4ajHjJ6QccFUvW1u&index=25
					value.toString().toHttpUrl().queryParameter("v")
				} else {
					value
				}
			old.type == PropertyType.Number ->
				(value as Number).toDouble()
			old.type == PropertyType.Date -> {
				fun parseDateTime(value: String): LocalDateTime =
					try {
						LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(value))
					} catch (@Suppress("SwallowedException") ex: DateTimeException) {
						LocalDate.from(DateTimeFormatter.ISO_DATE.parse(value)).atStartOfDay()
					}
				value as PageProperty.Date
				Pair(value.start?.let(::parseDateTime), value.end?.let(::parseDateTime))
			}
			// TODEL https://github.com/seratch/notion-sdk-jvm/issues/82
			old.type == PropertyType.MultiSelect ->
				@Suppress("UNCHECKED_CAST")
				(value as List<DatabaseProperty.MultiSelect.Option>).map { it.name }
			else ->
				// Fall back to data class equals 🤞.
				value
		}

	val oldComparable = comparable(oldValue)
	val newComparable = comparable(newValue)
	return oldComparable == newComparable
}
