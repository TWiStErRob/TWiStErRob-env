// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
// TODEL https://youtrack.jetbrains.com/issue/KT-47384 cannot use kotlinx-serialization...
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.3")
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.0")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.10.0")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-okhttp4:1.10.0")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.model.blocks.HeadingOneBlock
import notion.api.v1.model.blocks.ParagraphBlock
import notion.api.v1.model.common.ExternalFileDetails
import notion.api.v1.model.common.FileType
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.databases.DatabaseProperty
import notion.api.v1.model.databases.QueryResults
import notion.api.v1.model.databases.query.filter.PropertyFilter
import notion.api.v1.model.databases.query.filter.QueryTopLevelFilter
import notion.api.v1.model.databases.query.filter.condition.RelationFilter
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import notion.api.v1.model.pages.PageProperty
import notion.api.v1.request.databases.QueryDatabaseRequest
import java.io.File
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("SpreadOperator")
main(*args)

object Constants {
	const val COLLECTION_PAGE = "d771f424bc914d1089b1705c5042f129"
	const val EVENT_TIME_ZONE = "Europe/London"
	const val TARGET_DATABASE = "d6d80a4765fe470dbae06fd5cd3d3f41"
	const val AUTHORS_DATABASE = "aecb82387adf4d7fa6816b791b0a579c"
	const val TOPICS_DATABASE = "a05a1b8d8eed43a1bc2e684b9fae50e0"
	const val BIO_HEADING = "Bio at DroidCon 2023"
	val SESSIONS_FILE = File("droidcon-2023-london/sessions.json")
	val SPEAKERS_FILE = File("droidcon-2023-london/speakers.json")
}

@Suppress("LongMethod")
fun main(vararg args: String) {
	check(args.isEmpty()) { "No arguments expected." }
	val jsonMapper = jsonMapper {
		addModule(kotlinModule())
		addModule(JavaTimeModule())
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	}
	val sessions = jsonMapper
		.readValue<List<Group>>(Constants.SESSIONS_FILE)
		.single()
		.sessions
		.let { remap(it) }
	describe(sessions)
	val speakers = jsonMapper
		.readValue<List<Speaker>>(Constants.SPEAKERS_FILE)
	describe(speakers)

	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")

	NotionClient(token = secret).apply { httpClient = OkHttp4Client(connectTimeoutMillis = 30_000) }.use { client ->
		val collectionPage = client.retrievePage(Constants.COLLECTION_PAGE)
		val jsonSpeakers = sessions
			.flatMap { it.speakers }
			.map { it.name }
			.distinct()
		val jsonTags = sessions
			.flatMap { it.categories }
			.filter { it.name == "Tags" }
			.flatMap { it.categoryItems }
			.map { it.name }
			.distinct()
		val speakerPages = ensureSpeakers(client, jsonSpeakers, speakers)
		val topicPages = ensureTopics(client, jsonTags)
		val existingSessions = client
			.queryDatabaseAllPages(
				Constants.TARGET_DATABASE,
				filter = PropertyFilter("Event", relation = RelationFilter(contains = collectionPage.id)),
			)
			.associateBy { it.title!! }
		val typeOptions = client
			.retrieveDatabase(Constants.TARGET_DATABASE)
			.properties["Type"]!!.select!!.options!!
		existingSessions.forEach { (title, page) ->
			println("Found existing session: ${title} (${page.id})")
		}
		sessions.filter { it.title !in existingSessions }.forEach { session ->
			client.createPageFor(session, collectionPage, speakerPages, topicPages, typeOptions)
		}
	}
}

fun NotionClient.createPageFor(
	session: Group.Session,
	collectionPage: Page,
	speakerPages: Map<String, Page>,
	topicPages: Map<String, Page>,
	typeOptions: List<DatabaseProperty.Select.Option>
) {
	createPage(
		parent = PageParent.database(Constants.TARGET_DATABASE),
		properties = mapOf(
			"title" to PageProperty(title = session.title.asRichText()),
			"Date" to PageProperty(
				date = PageProperty.Date(
					start = session.startsAt?.let { DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) },
					end = session.endsAt?.let { DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(it) },
					timeZone = Constants.EVENT_TIME_ZONE,
				)
			),
			"Length (minutes)" to session.duration?.let { duration ->
				PageProperty(number = duration.toMinutes())
			},
			"Event" to PageProperty(relation = listOf(PageProperty.PageReference(collectionPage.id))),
			"Track" to PageProperty(richText = session.room.asRichText()),
			"Speaker(s)" to PageProperty(relation = session.speakers.map {
				PageProperty.PageReference(speakerPages.getValue(it.name).id)
			}),
			"Topics" to PageProperty(relation = session.categories.single { it.name == "Tags" }.categoryItems.map {
				PageProperty.PageReference(topicPages.getValue(remapTopic(it.name)).id)
			}),
			"Type" to PageProperty(select = remapFormat(session.format).let { format ->
				typeOptions.single { it.name == format }
			}),
			"Abstract" to session.description?.let { description ->
				PageProperty(richText = description.asRichText())
			},
		).filterValues { it != null }.mapValues { it.value!! },
	)
}

data class Group(
	val sessions: List<Session>,
) {
	data class Session(
		val title: String,
		val description: String?,
		val room: String,
		val startsAt: LocalDateTime?,
		val endsAt: LocalDateTime?,
		val speakers: List<Speaker>,
		val categories: List<Category>,
	) {
		data class Speaker(
			val name: String,
		)
		data class Category(
			val name: String,
			val categoryItems: List<CategoryItem>,
		) {
			data class CategoryItem(
				val name: String,
			)
		}
	}
}

val Group.Session.duration: Duration?
	get() = if (startsAt != null && endsAt != null) Duration.between(startsAt, endsAt) else null

val Group.Session.format: String
	get() = categories.single { it.name == "Session format" }.categoryItems.single().name

data class Speaker(
	val fullName: String,
	val bio: String,
	val tagLine: String,
	val profilePicture: URI,
	val links: List<Link>,
) {
	data class Link(
		val title: String,
		val linkType: String,
		val url: String,
	)
}

fun NotionClient.queryDatabaseAllPages(databaseId: String, filter: QueryTopLevelFilter? = null): List<Page> =
	queryDatabaseAllPages(QueryDatabaseRequest(databaseId = databaseId, filter = filter))

fun NotionClient.queryDatabaseAllPages(query: QueryDatabaseRequest): List<Page> =
	generateSequence(queryDatabase(query)) { results ->
		if (results.hasMore) {
			queryDatabase(query.nextPageRequest(results))
		} else {
			null
		}
	}
		.flatMap { it.results }
		.toList()

fun QueryDatabaseRequest.nextPageRequest(results: QueryResults): QueryDatabaseRequest =
	QueryDatabaseRequest(
		databaseId = this.databaseId,
		startCursor = results.nextCursor,
		pageSize = this.pageSize,
	)

fun ensureSpeakers(client: NotionClient, wantedSpeakerNames: List<String>, speakers: List<Speaker>): Map<String, Page> {
	val speakerDetails = speakers.associateBy { it.fullName }
	val speakerPages = client.queryDatabaseAllPages(Constants.AUTHORS_DATABASE)
	val existingPages = speakerPages.associateBy { it.title ?: it.id }
	val newSpeakersNames = wantedSpeakerNames - existingPages.keys
	val newPages = newSpeakersNames.associateWith { speakerName ->
		val details = speakerDetails.getValue(speakerName)
		val (company, role) = when {
			"""( at |@|, )""".toRegex().findAll(details.tagLine).count() > 1 ->
				null to details.tagLine

			" at " in details.tagLine -> {
				val (role, company) = details.tagLine.split(" at ")
				company.trim() to role.trim()
			}
			", " in details.tagLine -> {
				val (role, company) = details.tagLine.split(", ")
				company.trim() to role.trim()
			}
			"@" in details.tagLine -> {
				val (role, company) = details.tagLine.split("@")
				company.trim() to role.trim()
			}
			else ->
				null to details.tagLine
		}
		fun Speaker.link(linkType: String): PageProperty? =
			this.links.singleOrNull { it.linkType == linkType }?.let { PageProperty(url = it.url) }

		client.createPage(
			parent = PageParent.database(Constants.AUTHORS_DATABASE),
			icon = notion.api.v1.model.common.File(
				type = FileType.External,
				external = ExternalFileDetails(url = details.profilePicture.toString())
			),
			properties = mapOf(
				"title" to PageProperty(title = speakerName.asRichText()),
				"Company" to company?.let { PageProperty(richText = it.asRichText()) },
				"Role" to PageProperty(richText = role.asRichText()),
				"Twitter" to details.link("Twitter"),
				"LinkedIn" to details.link("LinkedIn"),
				"Blog" to details.link("Blog"),
				"Website" to details.link("Company_Website"),
				"Profile picture" to PageProperty(
					files = listOf(
						PageProperty.File(
							name = "Profile picture",
							type = FileType.External,
							external = ExternalFileDetails(url = details.profilePicture.toString())
						)
					)
				),
			).filterValues { it != null }.mapValues { it.value!! },
			children = listOf(
				HeadingOneBlock(heading1 = HeadingOneBlock.Element(Constants.BIO_HEADING.asRichText())),
				ParagraphBlock(ParagraphBlock.Element(details.bio.asRichText())),
			),
		)
	}
	return existingPages + newPages
}

fun ensureTopics(client: NotionClient, wantedTopicNames: List<String>): Map<String, Page> {
	val topicPages = client.queryDatabaseAllPages(Constants.TOPICS_DATABASE)
	val existingPages = topicPages.associateBy { it.title ?: it.id }
	val mappedTopics = wantedTopicNames.map(::remapTopic)
	val newTopicNames = mappedTopics - existingPages.keys
	val newPages = newTopicNames.associateWith { topicName ->
		client.createPage(
			parent = PageParent.database(Constants.TOPICS_DATABASE),
			properties = mapOf(
				"title" to PageProperty(title = topicName.asRichText()),
			),
		)
	}
	return existingPages + newPages
}

fun String.asRichText(): List<PageProperty.RichText> =
	listOf(PageProperty.RichText(text = PageProperty.RichText.Text(content = this)))

val Page.title: String?
	get() {
		val prop = this.properties.values.singleOrNull { it.id == "title" && it.type == PropertyType.Title }
			?: error("Missing property of type 'title', available properties: ${this.properties.keys}")
		val title = prop.title ?: error("Missing title structure")
		return title.singleOrNull()?.plainText
	}

@Suppress("NestedLambdaShadowedImplicitParameter")
@JvmName("describeSessions")
fun describe(sessions: List<Group.Session>) {
	sessions
		.flatMap { it.categories }
		.map { it.name }
		.distinct()
		.forEach { cat ->
			val items = sessions
				.mapNotNull { it.categories.find { it.name == cat } }
				.flatMap { it.categoryItems }
				.map { it.name }
				.distinct()
				.map { item -> item to sessions.count { it.categories.any { it.categoryItems.any { it.name == item } } } }
			println("${cat}: ${items}")
		}
}

fun remap(sessions: List<Group.Session>): List<Group.Session> =
	sessions.map { session ->
		val others = session.categories.filter { it.name != "Tags" }
		val tags = session
			.categories
			.flatMap { cat ->
				cat.categoryItems
					.mapNotNull { item ->
						when (cat.name to item.name) {
							"Track" to "droidcon" -> null
							"Track" to "Flutter" -> "Flutter"
							"Track" to "enterprise & security" -> "Enterprise"
							"Tags" to "Other" -> null
							"Tags" to "Compose" -> "Jetpack Compose"
							"Tags" to "KMP" -> "Kotlin Multiplatform"
							"Tags" to "Flow" -> "Kotlin Coroutines Flow"
							else ->
								if (cat.name == "Tags")
									item.name
								else
									null
						}
					}
			}
			.distinct()
			.map { name -> Group.Session.Category.CategoryItem(name) }
			.let { items -> Group.Session.Category("Tags", items) }
		session.copy(
			categories = others + tags
		)
	}

fun remapTopic(name: String): String =
	when (name) {
		"Coroutines" -> "Kotlin Coroutines"
		"UI/UX" -> "UX Design"
		"Modern Android Development" -> "Modern Android Development (MAD)"
		"Foldables" -> "Form Factors - Foldables"
		"Design" -> "UI Design"
		else -> name
	}

fun remapFormat(format: String): String =
	when (format) {
		"Lightning talk" -> "Lightning Talk"
		"Session" -> "Talk"
		"Workshop" -> "Workshop"
		"Keynote" -> "Keynote"
		"panel" -> "Panel"
		else -> error("Unknown format: ${format}")
	}

@Suppress("NestedLambdaShadowedImplicitParameter")
@JvmName("describeSpeakers")
fun describe(speakers: List<Speaker>) {
	println("Link types: ${speakers.flatMap { it.links }.map { it.linkType to it.title }.distinct()}")
	speakers.forEach {
		if (it.links.groupingBy { it.linkType }.eachCount().filterValues { it > 1 }.isNotEmpty()) {
			println("Duplicate types for ${it.fullName}: ${it.links}")
		}
	}
}
