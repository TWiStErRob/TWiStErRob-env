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
import notion.api.v1.model.databases.Database
import notion.api.v1.model.databases.DatabaseProperty
import notion.api.v1.model.databases.MultiSelectPropertySchema
import notion.api.v1.model.databases.SelectOptionSchema
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
		.single().sessions

	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")

	NotionClient(token = secret).apply { httpClient = OkHttp4Client() }.use { client ->
		val droidConLondon2022 = client.retrievePage("8b215fe74d6e4fbcabb88f96917b6092")
		val sessionDatabase = client
			.retrieveDatabase("d6d80a4765fe470dbae06fd5cd3d3f41")
			.ensureSpeakers(client, sessions.flatMap { it.speakers }.map { it.name }.distinct())
		val speakerOptions = sessionDatabase.speakers().associateBy { it.name }
		sessions.forEach { session ->
			client.createPage(
				parent = PageParent.database(sessionDatabase.id),
				properties = mapOf(
					"title" to PageProperty(title = session.title.asRichText()),
					"Date" to PageProperty(date = PageProperty.Date(DateTimeFormatter.ISO_LOCAL_DATE.format(session.startsAt))),
					"Time" to PageProperty(richText = DateTimeFormatter.ISO_LOCAL_TIME.format(session.startsAt).asRichText()),
					"Length (minutes)" to PageProperty(number = session.duration?.toMinutes()),
					"Event" to PageProperty(relation = listOf(PageProperty.PageReference(droidConLondon2022.id))),
					"Author(s)" to PageProperty(multiSelect = session.speakers.map { speakerOptions.getValue(it.name) })
				),
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
	) {
		data class Speaker(
			val name: String,
		)
	}
}

val Group.Session.duration: Duration?
	get() = if (startsAt != null && endsAt != null) Duration.between(startsAt, endsAt) else null

fun Database.ensureSpeakers(client: NotionClient, speakerNames: List<String>): Database {
	val speakersInSessionsDatabase = this.speakers().map { it.name!! }
	return if (speakersInSessionsDatabase.containsAll(speakerNames)) {
		this
	} else {
		val originalOptions = speakers().map { SelectOptionSchema(it.name!!) }
		val missingNames = speakerNames.toSet() - speakersInSessionsDatabase.toSet()
		val missingOptions = missingNames.map { SelectOptionSchema(it) }
		client.updateDatabase(
			databaseId = id,
			properties = mapOf(
				"Author(s)" to MultiSelectPropertySchema(originalOptions + missingOptions)
			)
		)
	}
}

fun Database.speakers(): List<DatabaseProperty.MultiSelect.Option> =
	properties["Author(s)"]!!.multiSelect!!.options!!

fun String.asRichText(): List<PageProperty.RichText> =
	listOf(PageProperty.RichText(text = PageProperty.RichText.Text(content = this)))
