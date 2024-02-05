// Implicit dependency: Java 11 (because ktor 2.x is compiled as Class 55)
// Note: normally these dependencies are listed without a -jvm suffix,
// but there's no Gradle resolution in play here, so we have to pick a platform manually.
@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-core:1.11.0")
@file:DependsOn("com.github.seratch:notion-sdk-jvm-okhttp4:1.11.0")

import notion.api.v1.NotionClient
import notion.api.v1.http.OkHttp4Client
import notion.api.v1.model.blocks.Block
import notion.api.v1.model.blocks.BlockType
import notion.api.v1.model.pages.Page
import notion.api.v1.model.pages.PageProperty

@Suppress("SpreadOperator")
main(*args)

fun main(vararg args: String) {
	check(args.size == 3) {
		"Usage: kotlinc -script notion-move-to-property.main.kts <databaseID> <heading> <propertyName>"
	}
	val helpUrl = "https://www.notion.so/my-integrations"
	val secret = System.getenv("NOTION_TOKEN")
		?: error("NOTION_TOKEN environment variable not set, copy secret from 'Internal Integration Token' at ${helpUrl}.")
	NotionClient(token = secret).apply { httpClient = OkHttp4Client(connectTimeoutMillis = 30_000) }.use { client ->
		val pages = client.allPages(args[0])
		pages.forEach { page ->
			val body = client.retrieveBlockChildren(page.id).results
			val (heading, section) = body.sectionBetween(
				start = { it.isHeading && it.headingText.asString() == args[1] },
				end = { it.isHeading }
			)
			val error = page.validate(section, heading, args[1])
			if (error != null) {
				System.err.println(error)
				return@forEach
			}
			val text = section
				.map { it.asParagraph().paragraph.richText }
				// Ignore empty paragraphs as they don't contribute content.
				.filter { it.asString().isNotBlank() }
			client.updatePage(
				pageId = page.id,
				properties = mapOf(
					args[2] to PageProperty(
						richText = text
							// Inserting new RichTexts in the middle to keep formatting. 
							.joinTo(separator = "\n\n".asRichText())
							.flatten()
					)
				)
			)
			section.forEach { client.deleteBlock(it.id!!) }
			client.deleteBlock(heading!!.id!!)
		}
	}
}

fun NotionClient.allPages(databaseId: String): List<Page> =
	generateSequence(queryDatabase(databaseId)) { results ->
		results.nextCursor?.let { queryDatabase(databaseId, startCursor = it) }
	}.flatMap { it.results }.toList()

val Block.isHeading: Boolean
	get() = type == BlockType.HeadingOne || type == BlockType.HeadingTwo || type == BlockType.HeadingThree

val Block.headingText: List<PageProperty.RichText>
	get() = when (type) {
		BlockType.HeadingOne -> this.asHeadingOne().heading1.richText
		BlockType.HeadingTwo -> this.asHeadingTwo().heading2.richText
		BlockType.HeadingThree -> this.asHeadingThree().heading3.richText
		else -> error("Not a heading: ${this}")
	}

fun String.asRichText(): List<PageProperty.RichText> =
	listOf(PageProperty.RichText(text = PageProperty.RichText.Text(content = this)))

fun List<PageProperty.RichText>.asString(): String =
	this.joinToString { it.text!!.content!! }

fun List<Block>.sectionBetween(start: (Block) -> Boolean, end: (Block) -> Boolean): Pair<Block?, List<Block>> {
	val headingStartIndex = this.indexOfFirst(start)
	if (headingStartIndex == -1) return null to emptyList()
	val headingEndIndexPart = this.subList(headingStartIndex + 1, size).indexOfFirst(end)
	val headingEndIndex =
		if (headingEndIndexPart == -1)
			this.size
		else
			headingStartIndex + 1 + headingEndIndexPart
	return this[headingStartIndex] to this.subList(headingStartIndex + 1, headingEndIndex)
}

fun <T> Iterable<T>.joinTo(
	buffer: MutableCollection<T> = mutableListOf(),
	separator: T? = null,
	prefix: T? = null,
	postfix: T? = null
): Collection<T> {
	prefix?.let { buffer.add(it) }
	for ((index, element) in this.withIndex()) {
		if (0 < index) separator?.let { buffer.add(it) }
		buffer.add(element)
	}
	postfix?.let { buffer.add(it) }
	return buffer
}

fun Page.validate(section: List<Block>, heading: Block?, header: String): String? =
	when {
		heading == null ->
			"${this.id} / ${this.url}: Section heading ${header} is missing."
		heading.hasChildren == true ->
			"${this.id} / ${this.url}: Section heading has children: ${heading}."
		section.isEmpty() ->
			"${this.id} / ${this.url}: Section between heading ${header} and next heading is empty."
		!section.all { it.type == BlockType.Paragraph } ->
			"${this.id} / ${this.url}: Section is not all paragraphs: ${section.map { it.type }}"
		section.any { it.hasChildren == true } ->
			"${this.id} / ${this.url}: Section elements have children: ${section.filter { it.hasChildren == true }}."
		else ->
			null
	}
