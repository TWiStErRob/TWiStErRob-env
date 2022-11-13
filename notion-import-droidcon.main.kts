// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
// TODEL https://youtrack.jetbrains.com/issue/KT-47384 cannot use kotlinx-serialization...
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.7.2")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-okhttp4:1.7.2")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import notion.api.v1.model.pages.PageProperty
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Suppress("SpreadOperator")
main(*args)

fun main(vararg args: String) {
	check(args.isEmpty()) { "No arguments expected." }
	val jsonMapper = jsonMapper {
		addModule(kotlinModule())
		addModule(JavaTimeModule())
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	}
	val sessions = jsonMapper
		.readValue<List<Group>>(File("droidcon-2022-london/sessions.json"))
		.single()
		.sessions
		.let { remap(it) }
	describe(sessions)

	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")

	NotionClient(token = secret).apply { httpClient = OkHttp4Client(connectTimeoutMillis = 30_000) }.use { client ->
		val droidConLondon2022 = client.retrievePage("8b215fe74d6e4fbcabb88f96917b6092")
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
		val speakerPages = ensureSpeakers(client, jsonSpeakers)
		val topicPages = ensureTopics(client, jsonTags)
		val existingSessions = client
			.allPages("d6d80a4765fe470dbae06fd5cd3d3f41")
			.filter { it.properties["Event"]!!.relation!!.singleOrNull()?.id == droidConLondon2022.id }
			.associateBy { it.title!! }
		existingSessions.keys.forEach { println("Found existing session: ${it}") }
		sessions.filter { it.title !in existingSessions }.forEach { session ->
			client.createPage(
				parent = PageParent.database("d6d80a4765fe470dbae06fd5cd3d3f41"),
				properties = mapOf(
					"title" to PageProperty(title = session.title.asRichText()),
					"Date" to session.startsAt?.let { time ->
						PageProperty(date = PageProperty.Date(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time)))
					},
					"Length (minutes)" to session.duration?.let { duration ->
						PageProperty(number = duration.toMinutes())
					},
					"Event" to PageProperty(relation = listOf(PageProperty.PageReference(droidConLondon2022.id))),
					"Author(s)" to PageProperty(relation = session.speakers.map {
						PageProperty.PageReference(speakerPages.getValue(it.name).id)
					}),
					"Topics" to PageProperty(relation = session.categories.single { it.name == "Tags" }.categoryItems.map {
						PageProperty.PageReference(topicPages.getValue(it.name).id)
					}),
				).filterValues { it != null }.mapValues { it.value!! },
			)
		}
	}
}

data class Group(
	val sessions: List<Session>,
) {
	data class Session(
		val title: String,
		val description: String,
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

fun NotionClient.allPages(databaseId: String): List<Page> =
	generateSequence(queryDatabase(databaseId)) { results ->
		results.nextCursor?.let { queryDatabase(databaseId, startCursor = it) }
	}.flatMap { it.results }.toList()

fun ensureSpeakers(client: NotionClient, wantedSpeakerNames: List<String>): Map<String, Page> {
	val speakerPages = client.allPages("aecb82387adf4d7fa6816b791b0a579c")
	val existingPages = speakerPages.associateBy { it.title ?: it.id }
	val newSpeakersNames = wantedSpeakerNames - existingPages.keys
	val newPages = newSpeakersNames.associateWith { speakerName ->
		client.createPage(
			parent = PageParent.database("aecb82387adf4d7fa6816b791b0a579c"),
			properties = mapOf(
				"title" to PageProperty(title = speakerName.asRichText()),
			),
		)
	}
	return existingPages + newPages
}

fun ensureTopics(client: NotionClient, wantedTopicNames: List<String>): Map<String, Page> {
	val topicPages = client.allPages("a05a1b8d8eed43a1bc2e684b9fae50e0")
	val existingPages = topicPages.associateBy { it.title ?: it.id }
	val newTopicNames = wantedTopicNames - existingPages.keys
	val newPages = newTopicNames.associateWith { topicName ->
		client.createPage(
			parent = PageParent.database("a05a1b8d8eed43a1bc2e684b9fae50e0"),
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
