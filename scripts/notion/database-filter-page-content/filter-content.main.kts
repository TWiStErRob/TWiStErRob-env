// Iextractedependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.9.0")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-okhttp4:1.9.0")

import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.blocks.BlockType
import notion.api.v1.model.common.PropertyType
import notion.api.v1.model.databases.CheckboxPropertySchema
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageProperty

@Suppress("SpreadOperator")
main(*args)

fun main(vararg args: String) {
	check(args.size == 3) {
		"Usage: kotlinc -script filter-content.main.kts <databaseID> <propertyName> <isStrict>"
	}
	val databaseId = args[0]
	val propertyName = args[1]
	val isStrict = args[2].toBooleanStrict()

	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")

	NotionClient(token = secret).apply { httpClient = OkHttp4Client(connectTimeoutMillis = 30_000) }.use { client ->
		client.retrieveDatabase(databaseId).properties[propertyName]
			?.let { existingProp ->
				require(existingProp.type == PropertyType.Checkbox) {
					error("Property ${propertyName} already exist, and it's a ${existingProp.type} typed property.")
				}
			}
			?: client.updateDatabase(databaseId, properties = mapOf(propertyName to CheckboxPropertySchema()))
		val pages = client.allPages(databaseId)
		pages.forEach { page ->
			val body = client.retrieveBlockChildren(page.id).results
			client.updatePage(
				pageId = page.id,
				properties = mapOf(
					propertyName to PageProperty(
						checkbox = body.any { isStrict || it.hasContent() }
					)
				)
			)
		}
	}
}

fun NotionClient.allPages(databaseId: String): List<Page> =
	generateSequence(queryDatabase(databaseId)) { results ->
		results.nextCursor?.let { queryDatabase(databaseId, startCursor = it) }
	}.flatMap { it.results }.toList()

fun List<PageProperty.RichText>.asString(): String =
	this.joinToString(separator = "") { it.text!!.content!! }

fun Block.hasContent(): Boolean =
	when (this.type) {
		BlockType.Paragraph -> this.asParagraph().paragraph.richText.asString().isNotBlank()
		else -> true
	}
