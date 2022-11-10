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
		val response = Json.createReader(StringReader(gitHub.repositoriesDetails())).use { it.readValue() }.asJsonObject()
		val repos = response.getValue("/data/user/repositories/nodes").asJsonArray()
		val reference = Json.createReader(File("reference.repo.json").reader()).use { it.readValue().asJsonObject() }
		repos.forEach { repoJson ->
			val repo = repoJson.asJsonObject()
			val diff = Json.createDiff(repo, reference).toJsonArray()
			val mergeDiff = Json.createMergeDiff(repo, reference).toJsonValue().asJsonObject()
			println(diff.format())
			println(mergeDiff.format())
		}
	}
}

fun JsonValue.format(): String =
	StringWriter()
		.apply { use { it.prettyPrint(this@format) } }
		.toString()

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
					println(message)
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

suspend fun GitHub.repositoriesDetails(): String {
	val response = graph(
		query = File("repositoriesWithDetails.graphql").readText(),
		variables = mapOf(
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
			 * kotlinc -script validate.main.kts <org>
			 * kotlinc -script validate.main.kts <org1>/<repo1> <org2>/<repo2>
			
			Parameters:
			 * `<org>`: the name of the user who owns the repositories to process (organizations not supported yet).
			 * `<org>/<repo>`: list of repository coordinates to process explicitly.
			
			Environment variables:
			 * GITHUB_USER: login user name of the user who's running the script
			 * GITHUB_TOKEN: private access token (`ghp_...`) of the user who's running the script
			
			GitHub Enterprise is not supported, need to change code to make it work.
		""".trimIndent()
	)
	exitProcess(1)
}
