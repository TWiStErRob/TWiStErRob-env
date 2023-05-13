@file:DependsOn("commons-io:commons-io:2.11.0")

import org.apache.commons.io.IOUtils
import java.io.File
import java.io.IOException

main(args)

fun main(args: Array<String>) {
	check(args.size == 3) {
		"""
			Usage: kotlinc -script find_files.main.kts <look_in> <look_up> <found_files>
			Invalid arguments: ${args.contentToString()}
		""".trimIndent()
	}

	val lookIn = File(args[0])
	val lookUp = File(args[1])
	val foundFiles = File(args[2])

	println("Look for ${lookUp} in ${lookIn}")
	val files = lookUp.walk().filter(File::isFile).toList()
	val all = lookIn.walk().filter(File::isFile).groupBy { it.length() }
	println("Found ${files.size} files to search in ${all.size} files")

	val found = files.associateWith { file ->
		println("Searching ${file}")
		val candidates = all[file.length()] ?: emptyList()
		candidates.find { candidate ->
			try {
				IOUtils.contentEquals(file.inputStream(), candidate.inputStream())
			} catch (ex: Throwable) {
				throw IOException("Cannot process ${candidate} vs ${file}", ex)
			}
		}
	}

	foundFiles.writeText(found.entries.joinToString(separator = System.lineSeparator()) { "${it.key} -> ${it.value}" })

	found
		.filterValues { it != null }
		.forEach { (file, _) ->
			println("del \"${file}\"")
			//file.delete()
		}
}
