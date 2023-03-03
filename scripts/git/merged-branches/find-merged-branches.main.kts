@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.TimeUnit

@Suppress("VariableNaming", "PropertyName")
val BRANCH_REGEX = """^[a-z]+/(?<ticket>[A-Za-z]+-\d+).*""".toRegex()

runBlocking(Dispatchers.Default) { main() }

/**
 * This script is meant to be used from within a git clone with the `git` command on `PATH`.
 */
suspend fun main() {
	val branchesOutput = arrayOf("git", "branch").runCommand()
	val results = branchesOutput
		.split("\n")
		.map { it.trimStart(' ', '*') }
		.filter { it.isNotEmpty() }
		.parallelMap { process(it) }
	val commands = results
		.filter { it.log != null }
		.map { "# Deleting because ${it.log}\ngit branch --delete --force ${it.branch}" }	
		.joinToString("\n") { it }
	println(commands)
}

data class Result(
	val branch: String,
	val ticket: String?,
	val log: String?,
)

fun process(branch: String): Result {
	val match = BRANCH_REGEX.matchEntire(branch)
		?: return Result(branch, null, null)
	val ticket = match.groups["ticket"]!!.value
	val logs = arrayOf("git", "log", "--format=oneline", """--grep=^${ticket}\b""")
		.runCommand()
		.trim()
		.takeIf { it.isNotEmpty() }
	return Result(
		branch = branch,
		ticket = ticket,
		log = logs,
	)
}

fun Array<String>.runCommand(workingDir: File = File(".")): String {
	println("Running ${this.joinToString(separator = " ")} in ${workingDir.absolutePath}...")
	@Suppress("SpreadOperator")
	val proc = ProcessBuilder(*this)
		.directory(workingDir)
		.redirectOutput(ProcessBuilder.Redirect.PIPE)
		.redirectErrorStream(true)
		.start()
	proc.waitFor(5, TimeUnit.SECONDS) // exit code ignored
	return proc.inputStream.bufferedReader().readText()
}

suspend fun <A, B> Iterable<A>.parallelMap(transform: suspend (A) -> B): List<B> =
	coroutineScope { this@parallelMap.map { async { transform(it) } }.awaitAll() }
