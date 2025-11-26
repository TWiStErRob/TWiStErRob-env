// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("javax.json:javax.json-api:1.1.4")
@file:DependsOn("org.glassfish:javax.json:1.1.4")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")
@file:DependsOn("io.ktor:ktor-client-java-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-logging-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:3.3.3")
@file:DependsOn("tech.tablesaw:tablesaw-core:0.44.4")

import JsonX.asSafeString
import JsonX.filterNot
import JsonX.format
import JsonX.getSafeString
import JsonX.mapJsonArray
import JsonX.prettyPrint
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
import io.ktor.client.request.header
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
import java.net.URLEncoder
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
		val reference = Json.createReader(File("reference.repo.json5")
			.readLines()
			// Only full-line comments are supported, trailing comments will fail.
			.filterNot { it.matches("""^\s*//.*$""".toRegex()) }
			.joinToString(separator = "\n")
			// Trailing commas at the end of arrays and objects, but only when pretty printed.
			.replace(""",(\n\s*[\}\]])""".toRegex(), "$1")
			//.also { println(it) }
			.reader()
		).use { it.readValue() }
		val response = if (@Suppress("ConstantConditionIf", "RedundantSuppression") true) {
			Json.createReader(StringReader(gitHub.repositoriesDetails(args[0]))).use { it.readValue() }.asJsonObject()
		} else {
			Json.createReader(File("cached.repos.json").reader()).use { it.readValue() }
		}
		Json.createWriter(File("response.repos.json").writer()).use { it.write(response) }
		val repos = response.asJsonObject()
			.getValue("/data/user/repositories/nodes").asJsonArray()
		val result = repos.mapJsonArray {
			val repo = it.asJsonObject()!!
			val diff = JsonX.createDiff(repo, reference).adorn(repo).cleanDiff()
			val mergeDiff = JsonX.createMergeDiff(repo, reference).cleanMergeDiff()
			val files = gitHub.validateFiles(repo)
			Json.createObjectBuilder()
				.add("name", repo.getString("name"))
				.add("url", repo.getString("url"))
				.add("diff", diff)
				.add("mergeDiff", mergeDiff)
				.add("files", files)
				.build()
		}
		File("result.repos.json").writer().use { it.prettyPrint(result) }
		println(Json.createArrayBuilder().apply { result.forEach(::add) }.build().format())
	}
}

@Suppress("NestedBlockDepth")
fun JsonValue.cleanMergeDiff(): JsonValue? =
	when (this.valueType!!) {
		JsonValue.ValueType.ARRAY ->
			this.asJsonArray().let { arr ->
				Json.createArrayBuilder()
					.apply {
						arr.mapNotNull { it.cleanMergeDiff() }.forEach(::add)
					}
					.build()
					.takeIf { it.isNotEmpty() }
			}
		JsonValue.ValueType.OBJECT ->
			this.asJsonObject().let { obj ->
				Json.createObjectBuilder()
					.apply {
						obj.forEach { key, value ->
							val clean = value.cleanMergeDiff()
							if (clean != null) {
								add(key, clean)
							}
						}
					}
					.build()
					.takeIf { it.isNotEmpty() }
			}
		JsonValue.ValueType.STRING ->
			if (this.asSafeString() == "<REPOSITORY_SPECIFIC>") {
				null
			} else {
				this
			}
		JsonValue.ValueType.NUMBER,
		JsonValue.ValueType.TRUE,
		JsonValue.ValueType.FALSE,
		JsonValue.ValueType.NULL,
		-> this
	}

fun JsonArray.cleanDiff(): JsonArray =
	this
		.filterNot { value ->
			// Only in case we're changing a value from "a" to "b".
			value.asJsonObject().getString("op") == JsonPatch.Operation.REPLACE.operationName()
					// If the value we should change to is <REPOSITORY_SPECIFIC>, then those are ignored.
					&& value.asJsonObject().getSafeString("value") == "<REPOSITORY_SPECIFIC>"
					// But if the original value is null, then that means the specific value is missing,
					// so we should keep it.
					&& !value.asJsonObject().isNull("original")
					// Also if the original value is "", then that means the specific value is missing,
					// so we should keep it.
					&& !value.asJsonObject().getSafeString("original").isNullOrBlank()
		}
		.filterNot { value ->
			// The reference only contains a topic so that it's flagged for addition,
			// it doesn't mean that the extra topics must be removed.
			value.asJsonObject().getString("op") == JsonPatch.Operation.REMOVE.operationName()
					&& value.asJsonObject().getString("path").matches("""^/repositoryTopics/nodes/\d+$""".toRegex())
		}
		.filterNot { value ->
			// The reference only contains a required status check so that it's flagged for addition,
			// it doesn't mean that the extra checks must be removed.
			value.asJsonObject().getString("op") == JsonPatch.Operation.REMOVE.operationName()
					&& value.asJsonObject().getString("path")
				.matches("""^/branchProtectionRules/nodes/\d+/requiredStatusChecks/\d+$""".toRegex())
		}

fun JsonArray.adorn(source: JsonObject): JsonArray =
	this
		.mapJsonArray { value ->
			val target = value.asJsonObject()
			when (target.getString("op")) {
				JsonPatch.Operation.REPLACE.operationName(),
				JsonPatch.Operation.REMOVE.operationName(),
				->
					Json.createObjectBuilder(target)
						.remove("value") // Remove and re-add so "original" is inserted before.
						.add("original", source.getValue(target.getString("path")))
						.run { if (target.containsKey("value")) add("value", target["value"]) else this }
						.build()

				else ->
					target
			}
		}

suspend fun GitHub.validateFiles(repo: JsonObject): JsonArray {
	val owner = repo.getValue("/owner/login").asSafeString()!!
	val name = repo.getSafeString("name")!!
	val defaultBranch = repo.getValue("/defaultBranchRef/name").asSafeString()!!
	val response = this.tree(
		owner = owner,
		repo = name,
		ref = defaultBranch
	)
	fun webUrl(entry: TreeResponse.TreeEntry): URI {
		val ownerEnc = URLEncoder.encode(owner, Charsets.UTF_8)
		val nameEnc = URLEncoder.encode(name, Charsets.UTF_8)
		val defaultBranchEnc = URLEncoder.encode(defaultBranch, Charsets.UTF_8)
		val pathEnc = URLEncoder.encode(entry.path, Charsets.UTF_8)
		return URI.create("https://github.com/${ownerEnc}/${nameEnc}/blob/${defaultBranchEnc}/${pathEnc}")
	}

	val builder = Json.createArrayBuilder()
	if (response.truncated) {
		builder.add("Results might be inconclusive because the GitHub tree listing was truncated.")
	}
	validateRenovate(response, ::webUrl).forEach(builder::add)
	validateGitHubActions(response, ::webUrl).forEach(builder::add)
	return builder.build()
}

suspend fun GitHub.validateRenovate(
	response: TreeResponse,
	webUrl: TreeResponse.TreeEntry.() -> URI,
): List<String> {
	val configs: List<TreeResponse.TreeEntry> = response.tree.filter { it.path in Renovate.CONFIGS_LOCATIONS }
	return if (configs.isEmpty()) {
		listOf("Missing Renovate configuration file, add it at `${Renovate.PREFERRED_CONFIG}`.")
	} else {
		val multipleProblems = if (configs.size > 1) {
			listOf("Multiple Renovate configuration files found: ${configs}, keep only `${Renovate.PREFERRED_CONFIG}`.")
		} else {
			emptyList()
		}
		val contentProblems = configs.mapNotNull { configFile ->
			if (configFile.path != Renovate.PREFERRED_CONFIG) {
				"Renovate configuration file should be at `${Renovate.PREFERRED_CONFIG}`, not `${configFile.path}`."
			} else {
				val contents = blob(configFile.url).decodeToString()
				if (!contents.startsWith(Renovate.CONFIG_PREFIX)) {
					"Renovate configuration file ${configFile.webUrl()} doesn't have valid contents," +
							" should start with:" +
							"\n```json\n" +
							Renovate.CONFIG_PREFIX +
							"\n```"
				} else {
					null // AOK
				}
			}
		}
		multipleProblems + contentProblems
	}
}

object Renovate {

	// TODO change to json5 to allow for comments and trailing commas.
	const val PREFERRED_CONFIG = ".github/renovate.json"

	val CONFIG_PREFIX = """
		{
			"${'$'}schema": "https://docs.renovatebot.com/renovate-schema.json",
			"extends": [
				"local>TWiStErRob/renovate-config"
			]
	""".trimIndent()

	/**
	 * https://docs.renovatebot.com/configuration-options
	 */
	val CONFIGS_LOCATIONS = setOf(
		"renovate.json",
		"renovate.json5",
		".github/renovate.json",
		".github/renovate.json5",
		".gitlab/renovate.json",
		".gitlab/renovate.json5",
		".renovaterc",
		".renovaterc.json",
	)

	init {
		check(PREFERRED_CONFIG in CONFIGS_LOCATIONS)
	}
}

suspend fun GitHub.validateGitHubActions(
	response: TreeResponse,
	webUrl: TreeResponse.TreeEntry.() -> URI,
): List<String> {
	val workflows: List<TreeResponse.TreeEntry> = response.tree.filter {
		it.path.startsWith(".github/workflows/") && it.path.endsWith(".yml")
	}
	return validateGitHubCI(workflows) + validateGradleWrapper(workflows, webUrl)
}

suspend fun GitHub.validateGradleWrapper(
	workflows: List<TreeResponse.TreeEntry>,
	webUrl: TreeResponse.TreeEntry.() -> URI,
): List<String> {
	val validation = """
	|      - name: Validate Gradle Wrapper JARs.
	|        uses: gradle/wrapper-validation-action@v1
	""".trimMargin()
	return workflows.mapNotNull { workflowFile ->
		val contents = blob(workflowFile.url).decodeToString()
		if ("gradlew" in contents && validation !in contents) {
			"GitHub Actions workflow ${workflowFile.webUrl()} should validate Gradle Wrapper JARs before executing them:" +
					"\n```yml\n" +
					validation +
					"\n```" +
					"\nCreate a PR with title: \"Validate Gradle Wrappers before Gradle invocations\""
		} else {
			null // AOK
		}
	}
}

suspend fun GitHub.validateGitHubCI(workflows: List<TreeResponse.TreeEntry>): List<String> {
	val ciYml = ".github/workflows/CI.yml"
	val ci = workflows.singleOrNull { it.path == ciYml }
	return if (ci == null) {
		if (workflows.isEmpty()) {
			listOf("Missing GitHub Actions workflow for CI, add it at `${ciYml}`.")
		} else {
			listOf(
				"Missing GitHub Actions workflow for CI, add it at `${ciYml}`, " +
						"or rename one of ${workflows.map { it.path.substringAfter(".github/workflows/") }}."
			)
		}
	} else {
		val contents = blob(ci.url).decodeToString()
		if (contents.lines().first { !it.startsWith('#') }.substringBefore('#').trim() != "name: CI") {
			listOf("GitHub Actions workflow for CI in CI.yml should be named `CI`.")
		} else {
			emptyList()
		}
	}
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

	fun createMergeDiff(source: JsonValue, target: JsonValue): JsonValue =
		Json.createMergeDiff(source, target).toJsonValue()

	fun createDiff(source: JsonValue, target: JsonValue): JsonArray =
		Json.createDiff(source.asJsonObject(), target.asJsonObject()).toJsonArray()

	fun JsonObject.getSafeString(key: String): String? =
		this[key]?.asSafeString()

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

suspend fun GitHub.repositoriesDetails(owner: String): String {
	val response = graph(
		query = File("repositoriesWithDetails.graphql").readText(),
		variables = mapOf(
			"login" to owner,
			"production" to true,
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

/**
 * https://docs.github.com/en/rest/git/trees#get-a-tree
 */
suspend fun GitHub.tree(owner: String, repo: String, ref: String): TreeResponse {
	val response = client.get("https://api.github.com/repos/${owner}/${repo}/git/trees/${ref}?recursive=true")
	return response.body()
}

suspend fun GitHub.blob(url: URI): ByteArray {
	require(url.toString().matches("^https://api.github.com/repos/([^/]+?)/([^/]+?)/git/blobs/([0-9a-f]+)$".toRegex()))
	val response = client.get(url.toString()) {
		header("Accept", "application/vnd.github.raw")
	}
	return response.body()
}

data class TreeResponse(
	val sha: String,
	val url: URI,
	val tree: List<TreeEntry>,
	/**
	 * > The limit for the tree array is 100,000 entries with a maximum size of 7 MB when using the recursive parameter.
	 */
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
