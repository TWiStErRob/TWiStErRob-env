// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")
@file:DependsOn("io.ktor:ktor-client-java-jvm:3.1.2")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.1.2")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:3.1.2")

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.util.encodeBase64
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URI
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

/**
 * https://docs.github.com/en/rest/metrics/statistics#get-all-contributor-commit-activity
 */
class ContributionsResponse : ArrayList<ContributionsResponse.ContributorActivity>() {

	@Suppress("ConstructorParameterNaming", "KDocUnresolvedReference")
	data class ContributorActivity(
		val author: Author,
		val total: Int,
		val weeks: List<WeeklyContributions>
	) {

		class WeeklyContributions(
			val w: Long,
			val a: Int,
			val d: Int,
			val c: Int,
		)

		data class Author(
			/**
			 * @param optional
			 */
			val name: String?,
			/**
			 * @param optional
			 */
			val email: String?,
			/**
			 * @sample "octocat"
			 * @param required
			 */
			val login: String,
			/**
			 * @sample 1
			 * @param required
			 */
			val id: Int,
			/**
			 * @sample "MDQ6VXNlcjE="
			 * @param required
			 */
			val node_id: String,
			/**
			 * @sample "https://github.com/images/error/octocat_happy.gif"
			 * @param required
			 */
			val avatar_url: URI,
			/**
			 * @sample "41d064eb2195891e12d0413f63227ea7"
			 * @param required
			 */
			val gravatar_id: String?,
			/**
			 * @sample "https://api.github.com/users/octocat"
			 * @param required
			 */
			val url: URI,
			/**
			 * @sample "https://github.com/octocat"
			 * @param required
			 */
			val html_url: URI,
			/**
			 * @sample "https://api.github.com/users/octocat/followers"
			 * @param required
			 */
			val followers_url: URI,
			/**
			 * @sample "https://api.github.com/users/octocat/following{/other_user}"
			 * @param required
			 */
			val following_url: String,
			/**
			 * @sample "https://api.github.com/users/octocat/gists{/gist_id}"
			 * @param required
			 */
			val gists_url: String,
			/**
			 * @sample "https://api.github.com/users/octocat/starred{/owner}{/repo}"
			 * @param required
			 */
			val starred_url: String,
			/**
			 * @sample "https://api.github.com/users/octocat/subscriptions"
			 * @param required
			 */
			val subscriptions_url: URI,
			/**
			 * @sample "https://api.github.com/users/octocat/orgs"
			 * @param required
			 */
			val organizations_url: URI,
			/**
			 * @sample "https://api.github.com/users/octocat/repos"
			 * @param required
			 */
			val repos_url: URI,
			/**
			 * @sample "https://api.github.com/users/octocat/events{/privacy}"
			 * @param required
			 */
			val events_url: String,
			/**
			 * @sample "https://api.github.com/users/octocat/received_events"
			 * @param required
			 */
			val received_events_url: URI,
			/**
			 * @sample "User"
			 * @param required
			 */
			val type: String,
			/**
			 * @param required
			 */
			val site_admin: Boolean,
			/**
			 * @sample "2020-07-09T00:17:55Z"
			 * @param optional
			 */
			val starred_at: String?,
			/**
			 * @sample "CN=Full Name,OU=Pending Deletion,OU=Company,DC=company,DC=com"
			 * @param undocumented
			 */
			val ldap_dn: String?,
		)
	}
}

fun ObjectMapper.jsonConfig() {
	configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

fun cache(org: String, repo: String): File =
	File("cache/$org/$repo/contributors.json")

@Suppress("LongParameterList")
fun load(
	host: String,
	user: String,
	token: String,
	org: String,
	repo: String,
	force: Boolean = false
): ContributionsResponse {
	val serializer = jacksonObjectMapper().apply { jsonConfig() }
	val client = HttpClient {
		install(ContentNegotiation) {
			jackson {
				jsonConfig()
			}
		}
		expectSuccess = true
	}
	val cache = cache(org, repo)
	if (force || !cache.exists()) {
		val response = runBlocking {
			println("Loading $org/$repo data from $host...")
			client.contributions(host, org, repo, user, token).readRawBytes()
		}
		cache.parentFile.mkdirs()
		println("Saving cached data for $org/$repo from ${cache.absolutePath}.")
		cache.writeBytes(response)
	}
	println("Loading cached data for $org/$repo from ${cache.absolutePath}.")
	return serializer.readValue(cache, ContributionsResponse::class.java)
}

suspend fun HttpClient.contributions(
	host: String,
	org: String,
	repo: String,
	user: String,
	token: String
): HttpResponse {
	val response = get("https://$host/repos/$org/$repo/stats/contributors") {
		header("Authorization", "Basic ${"$user:$token".encodeBase64()}")
	}
	return when (response.status) {
		HttpStatusCode.OK -> response
		HttpStatusCode.NoContent -> error("No contributions for $org/$repo")
		HttpStatusCode.Forbidden -> error("Cannot access for $org/$repo, check user/token.")

		HttpStatusCode.Accepted -> {
			println("GitHub is calculating contributions for $org/$repo, waiting 5 seconds...")
			delay(5.seconds)
			contributions(host, org, repo, user, token)
		}

		else -> error("Unexpected response: ${response.status}")
	}
}

val token = System.getenv("GITHUB_TOKEN") ?: error("GITHUB_TOKEN must be set")
val user = System.getenv("GITHUB_USER") ?: error("GITHUB_USER must be set")
val host = System.getenv("GITHUB_HOST")?.let { "$it/api/v3" } ?: "api.github.com"
if (args.size != 2) {
	println(
		"""
			Usage: kotlinc -script get.main.kts <org> <repo>
			Invalid arguments: ${args.contentToString()}
		""".trimIndent()
	)
	exitProcess(1)
}
val org = args[0] // "TWiStErRob"
val repo = args[1] // "net.twisterrob.gradle"
val data = load(host, user, token, org, repo, true)
println(cache(org, repo))
