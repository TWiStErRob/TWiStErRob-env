import org.jetbrains.kotlin.mainKts.MainKtsScript
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import kotlin.script.experimental.api.CompiledScript
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.api.ScriptDiagnostic
import kotlin.script.experimental.api.compilerOptions
import kotlin.script.experimental.api.valueOrThrow
import kotlin.script.experimental.dependencies.CompoundDependenciesResolver
import kotlin.script.experimental.dependencies.FileSystemDependenciesResolver
import kotlin.script.experimental.dependencies.maven.MavenDependenciesResolver
import kotlin.script.experimental.dependencies.resolveFromAnnotations
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.withUpdatedClasspath
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.script.experimental.jvmhost.createJvmCompilationConfigurationFromTemplate

fun compilerJars(): List<File> {
	val kotlinMainKtsJarLocation: URL = MainKtsScript::class.java.protectionDomain.codeSource.location
	val kotlinHomeLibs: File = kotlinMainKtsJarLocation
		.toURI()
		.let(::File)
		.also { require(it.isFile) { "MainKtsScript was not loaded from a JAR: $kotlinMainKtsJarLocation" } }
		.parentFile

	return listOf(
		// Contains ScriptJvmK2CompilerIsolated, which kotlin-main-kts.jar references but does not contain.
		"kotlin-scripting-compiler.jar",
		// Contains the compiler implementation used by ScriptJvmK2CompilerIsolated, such as MessageCollector.
		"kotlin-compiler.jar",
		// Contains scripting compiler configuration classes, such as ScriptingConfigurationKeys.
		"kotlin-scripting-compiler-impl.jar",
	)
		.map(kotlinHomeLibs::resolve)
		.onEach { require(it.isFile()) { "Kotlin distribution is missing: $it" } }
}

/** Extend the kotlin-main-kts.jar loader without duplicating scripting API classes. */
fun extendScriptingCompilerClassLoader(compilerJars: List<File>) {
	val hostClassLoader: URLClassLoader = BasicJvmScriptingHost::class.java.classLoader as? URLClassLoader
		?: error("Expected BasicJvmScriptingHost to be loaded by a URLClassLoader")
	val addUrl = URLClassLoader::class.java
		.getDeclaredMethod("addURL", URL::class.java)
		.apply { isAccessible = true }
	compilerJars.forEach { addUrl.invoke(hostClassLoader, it.toURI().toURL()) }
}

/**
 * Parses string-literal coordinates from annotations in these forms:
 * ```kotlin
 * @file:Repository("https://repo1.maven.org/maven2/")
 * @file:DependsOn("group:first:1.0")
 * @file:DependsOn(
 *     "group:second:1.0",
 *     "group:third:1.0",
 * )
 * ```
 * [Repository.options] and [DependsOn.options] is not supported.
 */
fun parseDependencyAnnotations(script: File): List<Annotation> {
	fun String.annotationArguments(name: String): Sequence<String> =
		Regex("""@file:${Regex.escape(name)}\(([^)]*)\)""")
			.findAll(this)
			.flatMap { it.groupValues[1].splitToSequence(',') }
			.map { it.trim().removeSurrounding("\"") }
			.filter { it.isNotEmpty() }

	val scriptText = script.readText()
	return buildList {
		scriptText.annotationArguments("Repository").forEach { add(Repository(it)) }
		scriptText.annotationArguments("DependsOn").forEach { add(DependsOn(it)) }
	}
}

fun compileScript(script: File, compilerArguments: List<String>): ResultWithDiagnostics<CompiledScript> {
	val host = BasicJvmScriptingHost()
	return host.runInCoroutineContext {
		val dependencyClasspath = CompoundDependenciesResolver(
			FileSystemDependenciesResolver(),
			MavenDependenciesResolver(),
		)
			.resolveFromAnnotations(parseDependencyAnnotations(script))
			.valueOrThrow()
		host.compiler(
			script = script.toScriptSource(),
			scriptCompilationConfiguration = createJvmCompilationConfigurationFromTemplate<MainKtsScript> {
				compilerOptions.append(compilerArguments)
			}
				.withUpdatedClasspath(dependencyClasspath),
		)
	}
}

fun reportCompilationResult(result: ResultWithDiagnostics<CompiledScript>): Boolean {
	result.reports.forEach { report ->
		val location = report.location?.let { "${it.start.line}:${it.start.col}: " }
		System.err.println("${report.severity}: ${location.orEmpty()}${report.message}")
	}
	return result is ResultWithDiagnostics.Failure
			|| result.reports.any { it.severity == ScriptDiagnostic.Severity.WARNING }
}

require(args.isNotEmpty()) {
	error("Usage: kotlin -J--add-opens=java.base/java.net=ALL-UNNAMED compile-kotlin-script.main.kts [compiler options] <script.main.kts>")
}
val script = File(args.last())
	.also { require(it.isFile()) { "$it does not exist" } }
val compilerArguments = args.dropLast(1)
extendScriptingCompilerClassLoader(compilerJars())
if (reportCompilationResult(compileScript(script, compilerArguments))) {
	error("Script compilation failed: ${script.absolutePath}")
}
