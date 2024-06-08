// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("javax.json:javax.json-api:1.1.4")
@file:DependsOn("org.glassfish:javax.json:1.1.4")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.11.0")
@file:DependsOn("io.ktor:ktor-client-java-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-client-logging-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:2.3.11")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:2.3.11")
@file:DependsOn("tech.tablesaw:tablesaw-core:0.43.1")

import Query_main.JsonX.asBoolean
import Query_main.JsonX.asString
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
import javax.json.Json
import javax.json.JsonArray
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
	GitHub().use { gitHub ->
		val reposResponse = Json.createReader(reposFile.reader()).use { it.readValue() }
		val repos = reposResponse.asJsonObject().getValue("/data/organization/repositories/nodes").asJsonArray()
		repos.map { repo ->
			val nameWithOwner = repo.asJsonObject().getString("nameWithOwner")
			val issues = gitHub.getRepoIssues(nameWithOwner)
				.map { it.getValue("/data/repository/issues/nodes").asJsonArray() }
				.toList()
				.flatMap { it.toList() }
			processRepoIssues(issues)
		}
	}
}

fun processRepoIssues(issues: List<JsonValue>) {
	issues.forEach { issueNode ->
		val issue = issueNode.asJsonObject()
		val title = issue.getString("title")
		println(title)
	}
}

suspend fun GitHub.getRepoIssues(nameWithOwner: String): Flow<JsonObject> =
	flow {
		var cursor: String? = null
		do {
			val response = getIssues(nameWithOwner, cursor).asJsonObject()
			emit(response)
			cursor = response.getValue("/data/repository/issues/pageInfo/endCursor").asString()
			val hasNext = response.getValue("/data/repository/issues/pageInfo/hasNextPage").asBoolean()
			println("Processed $nameWithOwner page $cursor (more: $hasNext)")
		} while (hasNext)
	}

suspend fun GitHub.getIssues(nameWithOwner: String, page: String?): JsonValue {
	val (owner, name) = nameWithOwner.split("/")
	val cache = File("cache/${owner}/${name}-${page}.issues.json").also { it.parentFile.mkdirs() }
	if (!cache.exists()) {
		val issuesJson = this.issuesInRepo(owner, name, page)
		val parsed = Json.createReader(StringReader(issuesJson)).use { it.readValue() }
		parsed.asJsonObject() // Validate.
		cache.writer().use { it.prettyPrint(parsed) }
	} else {
		println("Using cached issues for $nameWithOwner page $page from $cache")
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
			level = LogLevel.ALL
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
		query = File("issues-in-repo.graphql").readText(),
		variables = mapOf(
			"owner" to owner,
			"name" to name,
			"cursor" to cursor,
		),
	)
	return response.bodyAsText().also { it.checkGraphQLError() }
}

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
			 * kotlin query.main.kts <repos.json>
			
			Parameters:
			 * `<repos.json>`: file name of the JSON file containing repos-in-org response.
			
			Environment variables:
			 * GITHUB_USER: login user name of the user who's running the script
			 * GITHUB_TOKEN: private access token (`ghp_...`) of the user who's running the script
			
			GitHub Enterprise is not supported, need to change code to make it work.
		""".trimIndent()
	)
	exitProcess(1)
}
