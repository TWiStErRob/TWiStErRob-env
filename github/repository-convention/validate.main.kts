// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
@file:DependsOn("io.ktor:ktor-client-java-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-client-logging-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:2.1.2")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:2.1.2")
@file:DependsOn("tech.tablesaw:tablesaw-core:0.43.1")

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import tech.tablesaw.api.BooleanColumn
import tech.tablesaw.api.StringColumn
import tech.tablesaw.api.Table
import tech.tablesaw.api.TextColumn
import tech.tablesaw.sorting.Sort
import java.io.Closeable
import java.net.URI
import kotlin.system.exitProcess

@Suppress("SpreadOperator")
runBlocking(Dispatchers.Default) { main(*args) }

suspend fun main(vararg args: String) {
	if (args.isEmpty()) {
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
	val org = args.size == 1 && '/' !in args[0]
	val repos = GitHub().use { gitHub ->
			if (org) {
				gitHub
					.repositories(user = args[0])
					.map {
						gitHub.repository(org = it.owner.login, repo = it.name)
					}
			} else {
				args
					.map { arg ->
						arg.split("/")
							.also { check(it.size == 2) { "Invalid <org>/<repo> pair: $arg" } }
					}
					.map { (org, repo) ->
						gitHub.repository(org = org, repo = repo)
					}
			}
	}
	val table = Table.create(
		TextColumn.create("Name", repos.map { it.name }),
		StringColumn.create("Type", repos.map { it.type.name }),
		BooleanColumn.create("Projects", repos.map { it.has_projects }),
		BooleanColumn.create("Wiki", repos.map { it.has_wiki }),
		BooleanColumn.create("Issues", repos.map { it.has_issues }),
		BooleanColumn.create("Pages", repos.map { it.has_pages }),
		BooleanColumn.create("Downloads", repos.map { it.has_downloads }),
		BooleanColumn.create("Squash", repos.map { it.allow_squash_merge }),
		BooleanColumn.create("Rebase", repos.map { it.allow_rebase_merge }),
		BooleanColumn.create("Merge", repos.map { it.allow_merge_commit }),
		BooleanColumn.create("Auto", repos.map { it.allow_auto_merge }),
		BooleanColumn.create("Delete", repos.map { it.delete_branch_on_merge }),
		StringColumn.create("Default", repos.map { it.default_branch }),
	)
	val sorted = table
		.sortOn(Sort.create(table, "Type", "Name"))
	println(sorted.printAll())
}

enum class RepoType {
	OWNED,
	FORK,
	ARCHIVED,
}

val RepositoriesGet.type: RepoType
	get() = when {
		archived != null && archived -> RepoType.ARCHIVED
		fork != null && fork -> RepoType.FORK
		else -> RepoType.OWNED
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

suspend fun GitHub.tree(owner: String, repo: String, ref: String): TreeResponse {
	val response = client.get("https://api.github.com/repos/${owner}/${repo}/git/trees/${ref}?recursive=true")
	return response.body()
}

/**
 * https://docs.github.com/en/rest/repos/repos#get-a-repository
 */
suspend fun GitHub.repository(org: String, repo: String): RepositoriesGet {
	val response = client.get("repos/${org}/${repo}")
	return when (response.status) {
		HttpStatusCode.OK -> response.body()
		else -> error("Unexpected response: ${response.status} / ${response.bodyAsText()}")
	}
}

/**
 * https://docs.github.com/en/rest/repos/repos#list-repositories-for-a-user
 * Not: https://docs.github.com/en/rest/repos/repos#list-organization-repositories
 */
suspend fun GitHub.repositories(user: String): List<RepositoriesGet> =
	flow {
		var response = client.get("users/${user}/repos") {
			url {
				parameters["page"] = 1.toString()
				parameters["per_page"] = 100.toString()
			}
		}
		while (true) {
			val repos = when (response.status) {
				HttpStatusCode.OK -> response.body<List<RepositoriesGet>>()
				else -> error("Unexpected response: ${response.status} / ${response.bodyAsText()}")
			}
			emit(repos)
			val nextUrl = parseLinks(response.headers["link"])["next"] ?: break
			response = client.get(nextUrl)
		}
	}.toList().flatten()

/**
 * https://docs.github.com/en/rest/guides/traversing-with-pagination#link-header
 */
fun parseLinks(link: String?): Map<String, String> {
	if (link.isNullOrEmpty()) return emptyMap()
	return link
		.split(",")
		.associate {
			val (wrappedUrl, wrappedRel) = it.split(";")
			val url = wrappedUrl.trim().substringAfter('<').substringBefore('>')
			val rel = wrappedRel.trim().substringAfter("rel=\"").substringBefore("\"")
			rel to url
		}
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

/**
 * Full Repository
 * https://docs.github.com/en/rest/repos/repos#get-a-repository
 */
@Suppress("ConstructorParameterNaming")
data class RepositoriesGet(
	val name: String,
	val owner: Owner,
	val private: Boolean?,
	val fork: Boolean?,
	val default_branch: String?,
	val has_issues: Boolean?,
	val has_projects: Boolean?,
	val has_wiki: Boolean?,
	val has_pages: Boolean?,
	val has_downloads: Boolean?,
	val archived: Boolean?,
	val allow_rebase_merge: Boolean?,
	val allow_squash_merge: Boolean?,
	val allow_auto_merge: Boolean?,
	val delete_branch_on_merge: Boolean?,
	val allow_merge_commit: Boolean?,
	val allow_update_branch: Boolean?,
	val allow_forking: Boolean?,
) {

	data class Owner(
		val login: String,
	)
}
