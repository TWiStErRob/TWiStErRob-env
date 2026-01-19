#!/usr/bin/env kotlin
@file:DependsOn("com.fasterxml.jackson.core:jackson-databind:2.21.0")
@file:DependsOn("com.fasterxml.jackson.module:jackson-module-kotlin:2.21.0")
@file:DependsOn("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.0")
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.1.0")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("org.fusesource.jansi:jansi:2.4.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

import ClipsResponse.Clip
import ClipsResponse.Tag
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.path
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.utils.io.jvm.javaio.copyTo
import kotlinx.coroutines.runBlocking
import kotlinx.io.IOException
import org.fusesource.jansi.AnsiConsole
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.outputStream
import kotlin.io.path.reader
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writer

/**
 * Usage: `kotlin gyg-download.main.kts --input clips.json [--dry-run]`
 *
 * Writes into current folder:
 *  * `<fileName>.mp4`
 *  * `<fileName>.replay`
 *  * `<fileName>.clip.json`
 *
 * Where `<fileName>` == `<yyyyMMddHHmmss> <title> (<remoteFileStem>) <hashtags>`
 *
 * Behaviour:
 *   * Skips downloading/writing if target already exists.
 */
private class DownloadGygCommand : CliktCommand("gyg-download") {

	val input: Path by option("--input", help = "Path to clips.json")
		.path(mustExist = true, canBeDir = false)
		.required()

	val dryRun: Boolean by option("--dry-run", help = "Print actions without downloading/writing.")
		.flag(default = false)

	override fun help(context: Context): String =
		"Download GifYourGame clips (video + replay) and save the per-clip JSON next to them."

	override fun run() {
		runBlocking {
			main(input, dryRun)
		}
	}
}

private object Log {

	fun info(msg: String) = println(msg)
	fun warn(msg: String) = System.err.println("WARN: $msg")
	fun error(msg: String) = System.err.println("ERROR: $msg")
}

private data class ClipsResponse(
	val result: List<Clip>,
) {

	data class Clip(
		val name: String,
		val createdAt: Instant,
		val updatedAt: Instant,
		val game: Game,
		val tags: List<Tag>,
		val gyg: Gyg,
		val replayFile: ReplayFile,
	)

	data class Game(
		val url: String,
	)

	data class Tag(
		val slug: String,
		val category: String,
	)

	data class Gyg(
		val name: String,
		val videoEndpoints: Map<String, Endpoint>,
	)

	data class Endpoint(
		val url: String,
		val contentType: String,
		val status: String,
		val inProgress: Boolean,
	)

	data class ReplayFile(
		val replayId: String,
		val file: String,
		val createdAt: Instant,
	)
}

private fun json(): ObjectMapper =
	ObjectMapper()
		.registerModule(KotlinModule.Builder().build())
		.registerModule(JavaTimeModule())
		.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

private fun newHttpClient(): HttpClient =
	HttpClient(CIO) {
		install(HttpTimeout) {
			connectTimeoutMillis = 30_000
			requestTimeoutMillis = 120_000
			socketTimeoutMillis = 120_000
		}
		followRedirects = true
		expectSuccess = true
	}

private fun hashtagsFrom(tags: List<Tag>): List<String> {
	val excludedCategories: Set<String> = setOf(
		"autotag.year",
		"autotag.month",
		"autotag.week",
		"autotag.day",
	)

	fun categoryRank(category: String): Int =
		when (category.lowercase()) {
			"autotag.matchinfo" -> 0
			"autotag.rank" -> 1
			"autotag.type" -> 2
			"autotag.map" -> 3
			else -> 9
		}

	val picked = tags
		.filter { it.category.lowercase() !in excludedCategories }
		.sortedWith(compareBy({ categoryRank(it.category) }, { it.slug.lowercase() }))
		.map { it.slug }

	return picked
}

private fun parseQualityScore(key: String): Int {
	val match = Regex("""(?<resolution>\d+)p(?<fps>\d+)?""").matchEntire(key)
	val res = match?.groups["resolution"]?.value?.toInt() ?: 0
	val fps = match?.groups["fps"]?.value?.toInt() ?: 0
	return res * 1000 + fps
}

private fun pickBestVideo(clip: Clip): Map.Entry<String, ClipsResponse.Endpoint> =
	clip
		.gyg
		.videoEndpoints
		.filterValues { it.contentType == ContentType.Video.MP4.toString() }
		.maxByOrNull { parseQualityScore(it.key) }
		.let { requireNotNull(it) { "No eligible MP4 endpoints for clip '${clip.name}'" } }

private fun writeJsonIfMissing(mapper: ObjectMapper, node: JsonNode, target: Path, dryRun: Boolean) {
	if (target.exists()) {
		Log.warn("SKIP existing -> ${target.fileName}")
		return
	}
	if (dryRun) {
		Log.warn("DRY  write -> ${target.fileName}")
		return
	}
	target.parent.createDirectories()
	mapper.writerWithDefaultPrettyPrinter().writeValue(target.writer(), node)
}

private suspend fun downloadIfMissing(
	client: HttpClient,
	url: String,
	target: Path,
	timestamp: Instant,
	dryRun: Boolean,
) {
	if (target.exists()) {
		Log.warn("SKIP existing -> ${target.fileName}")
		return
	}
	if (dryRun) {
		Log.warn("DRY  download -> ${target.fileName}")
		return
	}

	try {
		target.parent.createDirectories()
		target.outputStream().use { out ->
			client.get(url).bodyAsChannel().copyTo(out)
		}
		target.setLastModifiedTime(FileTime.from(timestamp))
	} catch (ex: IOException) {
		runCatching { Files.deleteIfExists(target) }
			.onFailure { ex.addSuppressed(it) }
		throw ex
	}
}

private data class ClipPlan(
	val id: String,
	val game: String,
	val index: Int,
	val total: Int,
	val clipTimestamp: Instant,
	val json: JsonNode,
	val jsonPath: Path,
	val videoUrl: String,
	val videoPath: Path,
	val replayUrl: String,
	val replayPath: Path,
	val replayTimestamp: Instant,
)

private fun plan(clip: Clip, clipJson: JsonNode, index: Int, total: Int): ClipPlan {
	fun fileName(path: String): Path =
		path
			.replace(Regex("""[<>:"/\\|?*\u0000-\u001F]"""), "_")
			.let(Path::of)

	val date = clip.createdAt.atZone(ZoneId.systemDefault())
		.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
	val baseName = "$date ${clip.name} (${clip.gyg.name})"

	val video = pickBestVideo(clip)
	val hashtags = hashtagsFrom(clip.tags) + if (video.key != "720p60") listOf(video.key) else emptyList()

	return ClipPlan(
		id = clip.gyg.name,
		game = clip.game.url,
		index = index,
		total = total,
		clipTimestamp = clip.createdAt,
		json = clipJson,
		jsonPath = fileName("$baseName.clip.json"),
		videoUrl = video.value.url,
		videoPath = fileName("$baseName ${hashtags.joinToString(" ") { "#$it" }}.mp4"),
		replayUrl = clip.replayFile.file,
		replayPath = fileName("$baseName ${clip.replayFile.replayId}.replay"),
		replayTimestamp = clip.replayFile.createdAt,
	)
}

private suspend fun process(
	client: HttpClient,
	mapper: ObjectMapper,
	plan: ClipPlan,
	outDir: Path,
	dryRun: Boolean,
) {
	val outDir = outDir.resolve(plan.game)
	val prefix = "[${plan.index}/${plan.total}] "
	Log.info("${prefix}${plan.id}")
	runCatching {
		downloadIfMissing(client, plan.videoUrl, outDir.resolve(plan.videoPath), plan.clipTimestamp, dryRun)
	}.onFailure { Log.error("${prefix}video failed for '${plan.id}': ${it.message}") }
	runCatching {
		downloadIfMissing(client, plan.replayUrl, outDir.resolve(plan.replayPath), plan.replayTimestamp, dryRun)
	}.onFailure { Log.error("${prefix}replay failed for '${plan.id}': ${it.message}") }
	runCatching {
		writeJsonIfMissing(mapper, plan.json, outDir.resolve(plan.jsonPath), dryRun)
	}.onFailure { Log.error("${prefix}clip json failed for '${plan.id}': ${it.message}") }
}

private suspend fun main(input: Path, dryRun: Boolean) {
	val jsonMapper = json()
	val rawResults = jsonMapper.readTree(input.reader()).get("result") as ArrayNode
	val clips = jsonMapper.readValue<ClipsResponse>(input.reader()).result
	require(clips.size == rawResults.size()) {
		"Typed clip count (${clips.size} != JSON clip count (${rawResults.size()}; cannot align nodes safely."
	}

	val outDir = Paths.get(".").toAbsolutePath().normalize()
	newHttpClient().use { client ->
		clips.forEachIndexed { index, clip ->
			process(
				plan = plan(
					clip = clip,
					clipJson = rawResults.get(index),
					index = index + 1,
					total = clips.size,
				),
				client = client,
				mapper = jsonMapper,
				outDir = outDir,
				dryRun = dryRun,
			)
		}
	}
}

try {
	AnsiConsole.systemInstall()
	DownloadGygCommand().main(args)
} finally {
	AnsiConsole.systemUninstall()
}
