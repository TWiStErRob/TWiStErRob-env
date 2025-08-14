// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.ktor:ktor-client-java-jvm:3.2.3")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.2.3")
@file:DependsOn("io.ktor:ktor-client-logging-jvm:3.2.3")
@file:DependsOn("io.ktor:ktor-client-auth-jvm:3.2.3")
@file:DependsOn("io.ktor:ktor-serialization-jackson-jvm:3.2.3")
@file:DependsOn("com.jayway.jsonpath:json-path:2.9.0")

import GitHubDAO.FieldType
import GitHubDAO.ProjectItemField
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.JsonPathException
import com.jayway.jsonpath.TypeRef
import com.jayway.jsonpath.spi.json.JacksonJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
import io.ktor.utils.io.core.Closeable
import kotlinx.coroutines.runBlocking
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.time.LocalDate

runBlocking { main() }

suspend fun main() {
	GitHubDAO().use { github ->
		val project = github.findUserProject("username", 1)
		val fields = github.findFieldDetails(project).associateBy { it.name }
		val item = github.addProjectV2DraftIssue(project, "Test", "issue `1`")
		github.updateTopicFieldValue(project, item, fields, "Learning")
	}
}

suspend fun GitHubDAO.updateTopicFieldValue(
	project: String, item: String, fields: Map<String, ProjectItemField>, value: String
) {
	val field = fields["Topic"]!! as ProjectItemField.SingleSelectField
	val option = field.options.entries.single { it.value == value }
	updateProjectV2ItemFieldValue(project, item, field.id, FieldType.SingleSelect, option.key)
}

class GitHubDAO : Closeable {

	private val client = HttpClient {
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
		expectSuccess = true
	}

	override fun close() {
		client.close()
	}

	/**
	 * https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects#finding-the-node-id-of-a-user-project
	 *
	 * @param login user name (`octocat`) from the URL (`https://github.com/users/octocat/projects/5`).
	 * @param project number (`5`) from the URL (`https://github.com/users/octocat/projects/5`).
	 * @return node ID of a user project.
	 * @throws Exception Requires scope: `read:project`
	 */
	suspend fun findUserProject(login: String, project: Int): String {
		val response = graph(
			query = """
				query(${'$'}login: String!, ${'$'}project: Int!) {
					user(login: ${'$'}login) {
						projectV2(number: ${'$'}project) {
							id
						}
					}
				}
			""".trimIndent(),
			variables = mapOf(
				"login" to login,
				"project" to project,
			),
		)
		val json = response.bodyAsText()
		try {
			return JsonPath.parse(json).read("$.data.user.projectV2.id")
		} catch (e: JsonPathException) {
			throw IOException("Failed to parse response: $json", e)
		}
	}

	/**
	 * https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects#adding-a-draft-issue-to-a-project
	 *
	 * @param project id from [findUserProject].
	 * @param title issue title.
	 * @param body issue body (markdown).
	 * @return node ID of the newly created draft issue.
	 * @throws Exception Requires scope: `project`
	 */
	suspend fun addProjectV2DraftIssue(project: String, title: String, body: String): String {
		val response = graph(
			query = """
				mutation(${'$'}project: ID!, ${'$'}title: String!, ${'$'}body: String!) {
					addProjectV2DraftIssue(
						input: {
							projectId: ${'$'}project
							title: ${'$'}title
							body: ${'$'}body
						}
					) {
						projectItem {
							id
						}
					}
				}
			""".trimIndent(),
			variables = mapOf(
				"project" to project,
				"title" to title,
				"body" to body,
			),
		)
		val json = response.bodyAsText()
		try {
			return JsonPath.parse(json).read("$.data.addProjectV2DraftIssue.projectItem.id")
		} catch (e: JsonPathException) {
			throw IOException("Failed to parse response: $json", e)
		}
	}

	enum class FieldType(
		val value: String,
	) {

		Text("text"),
		SingleSelect("singleSelectOptionId"),
		Iteration("iterationId"),
	}

	/**
	 * https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects#updating-a-custom-text-number-or-date-field
	 * https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects#updating-a-single-select-field
	 * https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects#updating-an-iteration-field
	 *
	 * @param project id from [findUserProject].
	 * @param item id from [addProjectV2DraftIssue].
	 * @param field .
	 * @param value .
	 * @return node ID of the newly created draft issue.
	 * @throws Exception Requires scope: `project`
	 */
	suspend fun updateProjectV2ItemFieldValue(
		project: String,
		item: String,
		field: String,
		type: FieldType,
		value: String
	) {
		val response = graph(
			query = """
				mutation(${'$'}project: ID!, ${'$'}item: ID!, ${'$'}field: ID!, ${'$'}value: String!) {
					updateProjectV2ItemFieldValue(
						input: {
							projectId: ${'$'}project
							itemId: ${'$'}item
							fieldId: ${'$'}field
							value: { 
								${type.value}: ${'$'}value
							}
						}
					) {
						projectV2Item {
							id
						}
					}
				}
			""".trimIndent(),
			variables = mapOf(
				"project" to project,
				"item" to item,
				"field" to field,
				"value" to value,
			),
		)
		val json = response.bodyAsText()
		try {
			JsonPath.parse(json).read("$.data.updateProjectV2ItemFieldValue.projectV2Item.id")
		} catch (e: JsonPathException) {
			throw IOException("Failed to parse response: $json", e)
		}
	}

	@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION, defaultImpl = ProjectItemField.TextField::class)
	@JsonSubTypes(
		value = [
			JsonSubTypes.Type(ProjectItemField.TextField::class),
			JsonSubTypes.Type(ProjectItemField.IterationField::class),
			JsonSubTypes.Type(ProjectItemField.SingleSelectField::class),
		]
	)
	sealed class ProjectItemField {
		abstract val id: String
		abstract val name: String

		data class TextField(
			@JsonProperty("id")
			override val id: String,
			@JsonProperty("name")
			override val name: String,
		) : ProjectItemField()

		data class IterationField(
			@JsonProperty("id")
			override val id: String,
			@JsonProperty("name")
			override val name: String,
			@JsonProperty("configuration")
			@JsonDeserialize(using = StringHashMapValueDeserializer::class)
			val configuration: Map<String, LocalDate>,
		) : ProjectItemField() {
			class StringHashMapValueDeserializer : JsonDeserializer<Map<String, LocalDate>>() {
				override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Map<String, LocalDate> {
					val array = parser.codec.readTree<ObjectNode>(parser).get("iterations") as ArrayNode
					return array.associate { it.get("id").asText() to LocalDate.parse(it.get("startDate").asText()) }
				}
			}
		}

		data class SingleSelectField(
			@JsonProperty("id")
			override val id: String,
			@JsonProperty("name")
			override val name: String,
			@JsonProperty("options")
			@JsonDeserialize(using = StringHashMapValueDeserializer::class)
			val options: Map<String, String>,
		) : ProjectItemField() {
			class StringHashMapValueDeserializer : JsonDeserializer<Map<String, String>>() {
				override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): Map<String, String> =
					parser.codec.readTree<ArrayNode>(parser).associate {
						it.get("id").asText() to it.get("name").asText()
					}
			}
		}
	}

	/**
	 * https://docs.github.com/en/issues/planning-and-tracking-with-projects/automating-your-project/using-the-api-to-manage-projects#finding-the-node-id-of-a-field
	 * @param project id from [findUserProject].
	 * @throws Exception Requires scope: `read:project`
	 */
	suspend fun findFieldDetails(
		project: String,
	): List<ProjectItemField> {
		val response = graph(
			query = """
				query(${'$'}project: ID!) {
					node(id: ${'$'}project) {
						... on ProjectV2 {
							fields(first: 20) {
								nodes {
									... on ProjectV2Field {
										id
										name
									}
									... on ProjectV2IterationField {
										id
										name
										configuration {
											iterations {
												startDate
												id
											}
										}
									}
									... on ProjectV2SingleSelectField {
										id
										name
										options {
											id
											name
										}
									}
								}
							}
						}
					}
				}
			""".trimIndent(),
			variables = mapOf(
				"project" to project,
			),
		)
		val json = response.bodyAsText()
		try {
			val jackson = Configuration.builder()
				.jsonProvider(JacksonJsonProvider())
				.mappingProvider(JacksonMappingProvider())
				.build()
			return JsonPath.parse(json, jackson)
				.read("$.data.node.fields.nodes[*]", object : TypeRef<List<ProjectItemField>>() {})
		} catch (e: JsonPathException) {
			throw IOException("Failed to parse response: $json", e)
		}
	}

	private suspend fun graph(@Language("graphql") query: String, variables: Map<String, Any?>): HttpResponse {
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
