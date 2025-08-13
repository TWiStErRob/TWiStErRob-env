// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.19.2")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.2")
@file:DependsOn("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-html-jvm:0.12.0")

import ContributionsResponse.ContributorActivity
import ContributionsResponse.ContributorActivity.WeeklyContributions
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.net.URI

typealias OrganisationName = String
typealias RepositoryName = String
typealias LoginName = String

@Suppress("SpreadOperator")
main(*args)

/** Hack to expose this to the lowest level [unique] function. */
lateinit var params: Params
fun main(vararg args: String) {
	check(args.size == 2) {
		"""
			Usage: kotlinc -script summarize.main.kts <params.summarize.yml> <output.json>
			Invalid arguments: ${args.contentToString()}
		""".trimIndent()
	}
	val paramsFile = File(args[0])
	val summaryFile = File(args[1])
	params = paramsFile.readYaml()
	val data = params.orgs
		.flatMap { org ->
			org.repos.map { repo ->
				repo to cache(org.name, repo)
			}
		}

	val result: List<ContributionHistory> = process(data)
	summaryFile.writeJson(result)
}

/**
 * https://docs.github.com/en/rest/metrics/statistics#get-all-contributor-commit-activity
 */
class ContributionsResponse : ArrayList<ContributorActivity>() {

	@Suppress("ConstructorParameterNaming", "KDocUnresolvedReference")
	data class ContributorActivity(
		val author: Author,
		val total: Int,
		val weeks: List<WeeklyContributions>
	) {

		data class WeeklyContributions(
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
			val login: LoginName,
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

fun cache(org: OrganisationName, repo: RepositoryName): ContributionsResponse {
	val cache = File("cache/$org/$repo/contributors.json")
	val serializer = jsonMapper {
		addModule(kotlinModule())
		configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
	}
	return serializer.readValue(cache, ContributionsResponse::class.java)
}

data class ContributionHistoryTemp(
	val login: LoginName,
	val repo: RepositoryName,
	val contribs: List<ContributorActivity>,
)

data class ContributionHistory(
	val login: LoginName,
	val repo: RepositoryName,
	val contribs: List<WeeklyContributions>,
)

fun process(map: List<Pair<RepositoryName, ContributionsResponse>>): List<ContributionHistory> =
	map
		.map { (repo, contributions) -> repo to contributions.trim() }
		.flatMap { (repo, contributions) ->
			contributions.map { contrib ->
				ContributionHistoryTemp(
					login = contrib.author.login,
					repo = repo,
					contribs = listOf(contrib),
				)
			}
		}
		.groupBy { it.login }
		.mergeAuthors()
		.mapValues { (_, value) -> value.collapseRepositories() }
		.values
		.flatten()

fun ContributionsResponse.trim(): ContributionsResponse =
	this.mapTo(ContributionsResponse()) { activity ->
		activity.copy(
			weeks = activity.weeks.filter { it.c > 0 },
		)
	}

fun Map<LoginName, List<ContributionHistoryTemp>>.mergeAuthors(): Map<LoginName, List<ContributionHistoryTemp>> {
	val merged = mutableMapOf<LoginName, List<ContributionHistoryTemp>>()
	this.forEach { (name, contribs) ->
		merged[unique(name)] = (merged[unique(name)] ?: emptyList()) + contribs
	}
	return merged
}

fun List<ContributionHistoryTemp>.collapseRepositories(): List<ContributionHistory> {
	val collapsed = this.groupBy { it.repo }
	return collapsed.map { (repo, contribs) ->
		check(contribs.all { unique(it.login) == unique(contribs.first().login) }) {
			"All contributors should be the same: ${contribs.associate { it.login to unique(it.login) }}"
		}
		check(contribs.all { it.repo == contribs.first().repo }) {
			"All repos should be the same: ${contribs.map { it.repo }}"
		}
		ContributionHistory(
			login = unique(contribs.first().login),
			repo = repo,
			contribs = contribs.flatMap { it.contribs }.merge(),
		)
	}
}

fun List<ContributorActivity>.merge(): List<WeeklyContributions> =
	this
		.flatMap { it.weeks }
		.groupBy { it.w }
		.mapValues { (w, perAuthor) ->
			WeeklyContributions(
				w = w,
				a = perAuthor.sumOf { it.a },
				d = perAuthor.sumOf { it.d },
				c = perAuthor.sumOf { it.c },
			)
		}
		.values
		.toList()

fun unique(name: LoginName): LoginName =
	params.remapping[name] ?: name

data class Params(
	val orgs: List<Org>,
	val remapping: Map<LoginName, LoginName>,
) {

	data class Org(
		val name: OrganisationName,
		val repos: List<RepositoryName>,
	)
}

inline fun <reified T> File.readYaml(): T {
	val yaml = ObjectMapper(YAMLFactory()).apply {
		registerKotlinModule()
	}
	return yaml.readValue(this.reader())
}

fun File.writeJson(obj: Any) {
	val serializer = jsonMapper {
		addModule(kotlinModule())
//		configure(SerializationFeature.INDENT_OUTPUT, true)
	}
	serializer.writeValue(this, obj)
}
