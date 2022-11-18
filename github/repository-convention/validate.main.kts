// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("javax.json:javax.json-api:1.1.2")
@file:DependsOn("org.glassfish:javax.json:1.1.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
@file:DependsOn("io.ktor:ktor-client-java-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-client-logging-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:2.1.2")
@file:DependsOn("tech.tablesaw:tablesaw-core:0.43.1")

import Validate_main.JsonX.filterNot
import Validate_main.JsonX.format
import Validate_main.JsonX.getSafeString
import Validate_main.JsonX.map
import Validate_main.JsonX.prettyPrint
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.io.Closeable
import java.io.File
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import java.net.URI
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonPatch
import javax.json.JsonString
import javax.json.JsonValue
import javax.json.stream.JsonGenerator
import kotlin.system.exitProcess

@Suppress("SpreadOperator")
runBlocking(Dispatchers.Default) { main(*args) }

suspend fun main(vararg args: String) {
	if (args.isEmpty()) {
		usage()
	}
	GitHub().use { gitHub ->
		val response = if (@Suppress("ConstantConditionIf", "RedundantSuppression") true) {
			Json.createReader(StringReader(gitHub.repositoriesDetails(args[0]))).use { it.readValue() }.asJsonObject()
		} else {
			Json.createReader(File("cached.repos.json").reader()).use { it.readValue() }
		}
		Json.createWriter(File("response.repos.json").writer()).use { it.write(response) }
		val repos = response.asJsonObject()
			.getValue("/data/user/repositories/nodes").asJsonArray()
		val reference = Json.createReader(File("reference.repo.json").reader()).use { it.readValue() }
		val result = repos.map {
			val repo = it.asJsonObject()!!
			val diff = JsonX.createDiff(repo, reference).clean().adorn(repo)
			val mergeDiff = JsonX.createMergeDiff(repo, reference).clean()
			Json.createObjectBuilder()
				.add("name", repo.getString("name"))
				.add("url", repo.getString("url"))
				.add("diff", diff)
				.add("mergeDiff", mergeDiff)
				.build()
		}
		File("result.repos.json").writer().use { it.prettyPrint(result) }
		println(Json.createArrayBuilder().apply { result.forEach(::add) }.build().format())
	}
}

fun JsonObject.clean(): JsonObject =
	Json.createObjectBuilder(this)
		.apply {
			keys
				.filter { getValue("/$it").valueType == JsonValue.ValueType.OBJECT }
				.forEach { add(it, getJsonObject(it).clean()) }
			keys
				.filter { getSafeString(it) == "<REPOSITORY_SPECIFIC>" }
				.forEach { remove(it) }
			remove("repositoryTopics")
		}
		.build()

fun JsonArray.clean(): JsonArray =
	this
		.filterNot { value ->
			value.asJsonObject().getSafeString("value") == "<REPOSITORY_SPECIFIC>"
					&& value.asJsonObject().getString("op") == JsonPatch.Operation.REPLACE.operationName()
		}
		.filterNot { value ->
			value.asJsonObject().getString("path").matches("""^/repositoryTopics/nodes/\d+$""".toRegex())
					&& value.asJsonObject().getString("op") == JsonPatch.Operation.REMOVE.operationName()
		}
		.filterNot { value ->
			value.asJsonObject().getString("path")
				.matches("""^/branchProtectionRules/nodes/\d+/requiredStatusChecks/\d+$""".toRegex())
					&& value.asJsonObject().getString("op") == JsonPatch.Operation.REMOVE.operationName()
		}

fun JsonArray.adorn(source: JsonObject): JsonArray =
	this
		.map { value ->
			val target = value.asJsonObject()
			when (target.getString("op")) {
				JsonPatch.Operation.REPLACE.operationName(),
				JsonPatch.Operation.REMOVE.operationName(),
				-> {
					Json.createObjectBuilder(target)
						.remove("value") // Remove and re-add so "original" is inserted before.
						.add("original", source.getValue(target.getString("path")))
						.run { if (target.containsKey("value")) add("value", target["value"]) else this }
						.build()
				}

				else -> {
					target
				}
			}
		}

object JsonX {

	fun JsonArray.filterNot(predicate: (JsonValue) -> Boolean): JsonArray =
		Json.createArrayBuilder()
			.apply { forEach { if (!predicate(it)) add(it) } }
			.build()

	fun JsonArray.map(transform: (JsonValue) -> JsonValue): JsonArray =
		Json.createArrayBuilder()
			.apply { forEach { add(transform(it)) } }
			.build()

	fun createMergeDiff(source: JsonValue, target: JsonValue): JsonObject =
		Json.createMergeDiff(source, target).toJsonValue().asJsonObject()

	fun createDiff(source: JsonValue, target: JsonValue): JsonArray =
		Json.createDiff(source.asJsonObject(), target.asJsonObject()).toJsonArray()

	fun JsonObject.getSafeString(key: String): String? =
		this[key]?.let { value ->
			if (value.valueType == JsonValue.ValueType.STRING) {
				(value as JsonString).string
			} else {
				null
			}
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

suspend fun GitHub.repositoriesDetails(owner: String): String {
	val response = graph(
		query = File("repositoriesWithDetails.graphql").readText(),
		variables = mapOf(
			"login" to owner,
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

suspend fun GitHub.tree(owner: String, repo: String, ref: String): TreeResponse {
	val response = client.get("https://api.github.com/repos/${owner}/${repo}/git/trees/${ref}?recursive=true")
	return response.body()
}

data class TreeResponse(
	val sha: String,
	val url: URI,
	val tree: List<TreeEntry>,
	val truncated: Boolean,
) {
	data class TreeEntry(
		val path: String,
		val mode: String,
		val type: String,
		val sha: String,
		val size: Int?,
		val url: URI,
	)
}

fun usage(): Nothing {
	println(
		"""
			Usage:
			 * kotlinc -script validate.main.kts <owner>
			
			Parameters:
			 * `<org>`: the name of the user who owns the repositories to process (organizations not supported yet).
			
			Environment variables:
			 * GITHUB_USER: login user name of the user who's running the script
			 * GITHUB_TOKEN: private access token (`ghp_...`) of the user who's running the script
			
			GitHub Enterprise is not supported, need to change code to make it work.
		""".trimIndent()
	)
	exitProcess(1)
}
