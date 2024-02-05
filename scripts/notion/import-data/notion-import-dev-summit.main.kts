// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
// TODEL https://youtrack.jetbrains.com/issue/KT-47384 cannot use kotlinx-serialization...
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.1")
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.16.1")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.11.0")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-okhttp4:1.11.0")

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.model.common.ExternalFileDetails
import notion.api.v1.model.common.File
import notion.api.v1.model.common.FileType
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageParent
import notion.api.v1.model.pages.PageProperty
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Suppress("SpreadOperator")
main(*args)

@Suppress("LongMethod")
fun main(vararg args: String) {
	check(args.isEmpty()) { "No arguments expected." }
	val jsonMapper = jsonMapper {
		addModule(kotlinModule())
		addModule(JavaTimeModule())
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	}
	val sessions = jsonMapper.readValue<List<Session>>(java.io.File("dev-summit-2022/london.json"))
	val speakers = jsonMapper.readValue<List<Speaker>>(java.io.File("dev-summit-2022/london-speakers.json"))
	val speakersById = speakers.associateBy { it.id }

	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")

	NotionClient(token = secret).apply { httpClient = OkHttp4Client(connectTimeoutMillis = 30_000) }.use { client ->
		val ads2022 = client.retrievePage("4cc71ceb543c46f2bccf2badd8419705")
		val speakerPages = ensureSpeakers(client, speakers)
		val existingSessions = client
			.allPages("d6d80a4765fe470dbae06fd5cd3d3f41")
			.filter { it.properties["Event"]!!.relation!!.singleOrNull()?.id == ads2022.id }
			.associateBy { it.title!! }
		val typeOptions = client
			.retrieveDatabase("d6d80a4765fe470dbae06fd5cd3d3f41")
			.properties["Type"]!!.select!!.options!!
		existingSessions.forEach { (title, page) ->
			println("Found existing session: ${title} (${page.id})")
		}
		sessions.filter { it.title !in existingSessions }.forEach { session ->
			client.createPage(
				parent = PageParent.database("d6d80a4765fe470dbae06fd5cd3d3f41"),
				properties = mapOf(
					"title" to PageProperty(title = session.title.asRichText()),
					"Date" to PageProperty(
						date = PageProperty.Date(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(session.startsAt))
					),
					"Length (minutes)" to PageProperty(number = session.duration.toMinutes()),
					"Event" to PageProperty(relation = listOf(PageProperty.PageReference(ads2022.id))),
					"Author(s)" to PageProperty(relation = session.speakerIds.map { id ->
						PageProperty.PageReference(speakerPages.getValue(speakersById.getValue(id).fullName).id)
					}),
					"Type" to PageProperty(select = typeOptions.single { it.name == "Lightning Talk" })
				),
			)
		}
	}
}

data class Session(
	val title: String,
	@JsonProperty("Pa")
	@JsonDeserialize(converter = LongConverter::class)
	val startsAt: LocalDateTime,
	@JsonProperty("Ab")
	@JsonDeserialize(converter = LongConverter::class)
	val endsAt: LocalDateTime,
	@JsonProperty("Uh")
	val speakerIds: List<String>,
) {
	class LongConverter : StdConverter<Long, LocalDateTime>() {
		override fun convert(value: Long): LocalDateTime =
			LocalDateTime.ofEpochSecond(value / 1000L, 0, ZoneOffset.UTC)
	}
}

data class Speaker(
	val id: String,
	@JsonProperty("Xc")
	val firstName: String,
	@JsonProperty("Gc")
	val lastName: String,
	@JsonProperty("vm")
	val role: String,
	@JsonProperty("sh")
	val photo: Photo?,
) {
	data class Photo(
		@JsonProperty("url")
		val url: URI,
	)
}

val Speaker.fullName: String
	get() = "${firstName} ${lastName}"

val Session.duration: Duration
	get() = Duration.between(startsAt, endsAt)

fun NotionClient.allPages(databaseId: String): List<Page> =
	generateSequence(queryDatabase(databaseId)) { results ->
		results.nextCursor?.let { queryDatabase(databaseId, startCursor = it) }
	}.flatMap { it.results }.toList()

fun ensureSpeakers(client: NotionClient, speakers: List<Speaker>): Map<String, Page> {
	val speakerDetails = speakers.associateBy { it.fullName }
	val speakerPages = client.allPages("aecb82387adf4d7fa6816b791b0a579c")
	val existingPages = speakerPages.associateBy { it.title ?: it.id }
	val newSpeakersNames = speakerDetails.keys - existingPages.keys
	println("Found existing speakers: ${speakerDetails.keys - newSpeakersNames}")
	val newPages = newSpeakersNames.associateWith { speakerName ->
		val speaker = speakerDetails.getValue(speakerName)
		client.createPage(
			parent = PageParent.database("aecb82387adf4d7fa6816b791b0a579c"),
			icon = speaker.photo?.let { photo ->
				File(
					type = FileType.External,
					external = ExternalFileDetails(url = photo.url.toString())
				)
			},
			properties = mapOf(
				"title" to PageProperty(title = speakerName.asRichText()),
				"Company" to PageProperty(richText = "Google".asRichText()),
				"Role" to PageProperty(richText = speaker.role.asRichText()),
				"Profile picture" to speaker.photo?.let { photo ->
					PageProperty(
						files = listOf(
							PageProperty.File(
								name = "Profile picture",
								type = FileType.External,
								external = ExternalFileDetails(url = photo.url.toString())
							)
						)
					)
				},
			).filterValues { it != null }.mapValues { it.value!! }
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
