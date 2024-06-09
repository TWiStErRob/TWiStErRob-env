// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("javax.json:javax.json-api:1.1.4")
@file:DependsOn("org.glassfish:javax.json:1.1.4")
@file:DependsOn("io.ktor:ktor-client-java-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-client-logging-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:2.3.11")

import Query_main.JsonX.asBoolean
import Query_main.JsonX.asSafeString
import Query_main.JsonX.getSafeString
import Query_main.JsonX.prettyPrint
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.io.Closeable
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import java.time.Instant
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonNumber
import javax.json.JsonObject
import javax.json.JsonString
import javax.json.JsonValue
import javax.json.stream.JsonGenerator
import kotlin.system.exitProcess

println("Starting with ${args.contentToString()}")

@Suppress("SpreadOperator")
runBlocking(Dispatchers.Default) { main(*args) }

suspend fun main(vararg args: String) {
	if (args.isEmpty()) {
		usage()
	}
	val reposFile = File(args[0])
	val resultFile = File(args[1]).absoluteFile.also { it.parentFile.mkdirs() }.apply { writeText(header()) }
	GitHub().use { gitHub ->
		val reposResponse = Json.createReader(reposFile.reader()).use { it.readValue() }
		val repos = reposResponse.asJsonObject().getValue("/data/organization/repositories/nodes").asJsonArray()
		repos.map { repo ->
			val nameWithOwner = repo.asJsonObject().getString("nameWithOwner")
			val (owner, name) = nameWithOwner.split("/")
			val issues = gitHub.getRepoIssues(owner, name)
				.map { it.getValue("/data/repository/issues/nodes").asJsonArray() }
				.toList()
				.flatMap { it.toList() }
			val discussions = gitHub.getRepoDiscussions(owner, name)
				.map { it.getValue("/data/repository/discussions/nodes").asJsonArray() }
				.toList()
				.flatMap { it.toList() }
			processRepoIssues(issues.map { it.toIssue() } + discussions.map { it.toIssue() }, resultFile)
		}
	}
}

data class Issue(
	val title: String,
	val url: String,
	val closed: Boolean,
	val author: String,
	val mine: Boolean,
	val createdAt: Instant,
	val updatedAt: Instant,
	val closedAt: Instant?,
	val subscribed: String,
	val votes: Int?,
	val reactions: Map<String, Int>,
	val myReactions: Set<String>,
)

fun JsonValue.toIssue(): Issue {
	val issue = asJsonObject()
	val reactionGroups = issue.getJsonArray("reactionGroups").map { it.asJsonObject() }
	return Issue(
		title = issue.getString("title"),
		url = issue.getString("url"),
		closed = issue.getBoolean("closed"),
		author = issue.getJsonObject("author").getString("login"),
		mine = issue.getBoolean("viewerDidAuthor"),
		createdAt = Instant.parse(issue.getString("createdAt")),
		updatedAt = Instant.parse(issue.getString("updatedAt")),
		closedAt = issue.getSafeString("closedAt")?.let { Instant.parse(it) },
		subscribed = issue.getString("viewerSubscription"),
		votes = if (issue.containsKey("upvoteCount")) issue.getInt("upvoteCount") else null,
		reactions = reactionGroups
			.associate { it.getString("content") to it.getJsonObject("reactors").getInt("totalCount") },
		myReactions = reactionGroups
			.filter { it.getBoolean("viewerHasReacted") }
			.map { it.getString("content") }
			.toSet()
	)
}

fun processRepoIssues(issues: List<Issue>, result: File) {
	issues
		.filter { it.mine || it.subscribed == "SUBSCRIBED" || it.myReactions.isNotEmpty() }
		.forEach { issue ->
			val stateString = if (issue.closed) "CLOSED" else null
			val subString = issue.subscribed.takeIf { it != "SUBSCRIBED" }
			val authorString = if (issue.mine) "AUTHOR" else null
			val reactString = issue.myReactions.takeIf { it.isNotEmpty() }?.toString()
			println(
				"""
				${issue.title}
				  - ${issue.url}
				  - ${listOfNotNull(stateString, subString, authorString, reactString).joinToString()}
				  - Author: ${issue.author}
				  - Created: ${issue.createdAt}
				  - Updated: ${issue.updatedAt}
				  - Closed: ${issue.closedAt}
				  - Votes: ${issue.votes}
				  - Reactions: ${issue.myReactions} ${issue.reactions}
				""".trimIndent()
			)
			result.appendText(issue.toCsvLine())
		}
}

suspend fun GitHub.getRepoIssues(owner: String, name: String): Flow<JsonObject> =
	flow {
		var cursor: String? = null
		do {
			val response = getIssues(owner, name, cursor).asJsonObject()
			emit(response)
			cursor = response.getValue("/data/repository/issues/pageInfo/endCursor").asSafeString()
			val hasNext = response.getValue("/data/repository/issues/pageInfo/hasNextPage").asBoolean()
		} while (hasNext)
	}

suspend fun GitHub.getIssues(owner: String, name: String, page: String?): JsonValue {
	val cache = File("cache/${owner}/${name}/${page}.issues.json").also { it.parentFile.mkdirs() }
	if (!cache.exists()) {
		println("Fetching $owner/$name issues page $page")
		val issuesJson = this.issuesInRepo(owner, name, page)
		val parsed = Json.createReader(StringReader(issuesJson)).use { it.readValue() }
		parsed.asJsonObject() // Validate.
		cache.writer().use { it.prettyPrint(parsed) }
	} else {
		println("Using $cache")
	}
	return Json.createReader(cache.reader()).use { it.readValue() }
}

suspend fun GitHub.getRepoDiscussions(owner: String, name: String): Flow<JsonObject> =
	flow {
		var cursor: String? = null
		do {
			val response = getDiscussions(owner, name, cursor).asJsonObject()
			emit(response)
			cursor = response.getValue("/data/repository/discussions/pageInfo/endCursor").asSafeString()
			val hasNext = response.getValue("/data/repository/discussions/pageInfo/hasNextPage").asBoolean()
		} while (hasNext)
	}

suspend fun GitHub.getDiscussions(owner: String, name: String, page: String?): JsonValue {
	val cache = File("cache/${owner}/${name}/${page}.discussions.json").also { it.parentFile.mkdirs() }
	if (!cache.exists()) {
		println("Fetching $owner/$name discussions page $page")
		val issuesJson = this.discussionsInRepo(owner, name, page)
		val parsed = Json.createReader(StringReader(issuesJson)).use { it.readValue() }
		parsed.asJsonObject() // Validate.
		cache.writer().use { it.prettyPrint(parsed) }
	} else {
		println("Using $cache")
	}
	return Json.createReader(cache.reader()).use { it.readValue() }
}

object JsonX {

	inline fun JsonArray.filterNot(predicate: (JsonValue) -> Boolean): JsonArray =
		Json.createArrayBuilder()
			.apply { forEach { if (!predicate(it)) add(it) } }
			.build()

	inline fun JsonArray.mapJsonArray(transform: (JsonValue) -> JsonValue): JsonArray =
		Json.createArrayBuilder()
			.apply { forEach { add(transform(it)) } }
			.build()

	fun JsonObject.getSafeString(key: String): String? =
		this[key]?.asSafeString()

	fun JsonValue.asString(): String {
		require(valueType == JsonValue.ValueType.STRING)
		return (this as JsonString).string
	}

	fun JsonValue.asInt(): Int {
		require(valueType == JsonValue.ValueType.NUMBER)
		return (this as JsonNumber).intValueExact()
	}

	fun JsonValue.asBoolean(): Boolean =
		when (valueType) {
			JsonValue.ValueType.TRUE -> true
			JsonValue.ValueType.FALSE -> false
			else -> error("Not a boolean: ${this}")
		}

	fun JsonValue.asSafeString(): String? =
		if (valueType == JsonValue.ValueType.STRING) {
			(this as JsonString).string
		} else {
			null
		}

	fun List<JsonValue>.asJsonArray(): JsonArray =
		Json.createArrayBuilder()
			.apply { forEach(::add) }
			.build()

	fun JsonValue.format(): String =
		StringWriter()
			.apply { use { it.prettyPrint(this@format) } }
			.toString()
			.trim()

	fun Writer.prettyPrint(value: JsonValue) {
		Json
			.createWriterFactory(
				mapOf(
					JsonGenerator.PRETTY_PRINTING to true
				)
			)
			.createWriter(this)
			.use { it.write(value) }
	}
}

class GitHub : Closeable {

	val client = HttpClient {
		install(ContentNegotiation) {
			jackson {
				configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
			}
		}
		install(Auth) {
			basic {
				sendWithoutRequest { true }
				credentials {
					BasicAuthCredentials(
						username = System.getenv("GITHUB_USER") ?: error("GITHUB_USER must be set"),
						password = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN must be set"),
					)
				}
			}
		}
		install(Logging) {
			logger = object : Logger {
				override fun log(message: String) {
					System.err.println(message)
				}
			}
			level = LogLevel.HEADERS
		}
		defaultRequest {
			url("https://api.github.com/")
		}
		expectSuccess = true
	}

	override fun close() {
		client.close()
	}

	suspend fun graph(@Language("graphql") query: String, variables: Map<String, Any?>): HttpResponse {
		val response = client.post {
			url("https://api.github.com/graphql")
			contentType(ContentType.Application.Json)
			class GraphQLRequest(
				@JsonProperty("query")
				val query: String,
				@JsonProperty("variables")
				val variables: Map<String, Any?>?,
			)
			setBody(
				GraphQLRequest(
					query = query,
					variables = variables.takeIf { it.isNotEmpty() }
				)
			)
		}
		return response
	}
}

suspend fun GitHub.issuesInRepo(owner: String, name: String, cursor: String?): String {
	val response = graph(
		query = File("repo-issues.graphql").readText() + issueDetailsFragment,
		variables = mapOf(
			"owner" to owner,
			"name" to name,
			"cursor" to cursor,
		),
	)
	return response.bodyAsText().also { it.checkGraphQLError() }
}

suspend fun GitHub.discussionsInRepo(owner: String, name: String, cursor: String?): String {
	val response = graph(
		query = File("repo-discussions.graphql").readText() + issueDetailsFragment,
		variables = mapOf(
			"owner" to owner,
			"name" to name,
			"cursor" to cursor,
		),
	)
	return response.bodyAsText().also { it.checkGraphQLError() }
}

val issueDetailsFragment
	get() = System.lineSeparator() + File("issueDetails.graphql")
		.readText()
		.replace(Regex("""^query\s*\{\n.*?\n\}\n""", setOf(RegexOption.MULTILINE, RegexOption.DOT_MATCHES_ALL)), "")

fun String.checkGraphQLError() {
	val jackson = jacksonObjectMapper().apply {
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	}
	val response: ErrorResponse = jackson.readValue(this)
	if (response.errors != null) {
		if (response.errors.all {
				it.message.matches("""^Could not resolve to an Environment with the name github-pages\.$""".toRegex())
			}) {
			return
		}
		fun ErrorResponse.Error.asString(): String =
			run { "${type ?: "UNKNOWN"}@${locations?.joinToString { "${it.line}:${it.column}" }} ${message}" }
		error("GraphQL error:\n${response.errors.joinToString("\n") { it.asString() }}")
	}
}

data class ErrorResponse(
	val errors: List<Error>?,
) {

	data class Error(
		val message: String,
		val type: String?,
		val locations: List<Location>?,
	) {

		data class Location(
			val line: Int,
			val column: Int,
		)
	}
}

fun usage(): Nothing {
	println(
		"""
			Usage:
			 * kotlin query.main.kts <repos.json> <output.csv>
			
			Parameters:
			 * `<repos.json>`: file name of the JSON file containing repos-in-org response.
			 * `<output.csv>`: file name of the CSV file that will be written (will overwrite!).
			
			Environment variables:
			 * GITHUB_USER: login user name of the user who's running the script
			 * GITHUB_TOKEN: private access token (`ghp_...`) of the user who's running the script
			
			GitHub Enterprise is not supported, need to change code to make it work.
		""".trimIndent()
	)
	exitProcess(1)
}

fun header(): String =
	listOf(
		"URL",
		"Title",
		"Created At",
		"Updated At",
		"Closed At",
		"State",
		"Author",
		"Subscribed",
		"My Reactions",
		"Votes",
		"THUMBS_UP",
		"THUMBS_DOWN",
		"LAUGH",
		"HOORAY",
		"CONFUSED",
		"HEART",
		"ROCKET",
		"EYES",
	).joinToString(separator = ",", postfix = "\n")

fun Issue.toCsvLine(): String =
	listOf(
		url,
		title,
		createdAt.toString(),
		updatedAt.toString(),
		closedAt?.toString() ?: "",
		if (closed) "CLOSED" else "OPEN",
		author,
		subscribed,
		myReactions.joinToString(","),
		votes.toString(),
		(reactions["THUMBS_UP"] ?: 0).toString(),
		(reactions["THUMBS_DOWN"] ?: 0).toString(),
		(reactions["LAUGH"] ?: 0).toString(),
		(reactions["HOORAY"] ?: 0).toString(),
		(reactions["CONFUSED"] ?: 0).toString(),
		(reactions["HEART"] ?: 0).toString(),
		(reactions["ROCKET"] ?: 0).toString(),
		(reactions["EYES"] ?: 0).toString(),
	).joinToString(separator = ",", postfix = "\n") {
		"\"" + it.replace("\"", "\"\"") + "\""
	}
