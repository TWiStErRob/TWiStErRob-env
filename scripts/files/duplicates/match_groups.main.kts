import java.io.File

main(args)

fun main(args: Array<String>) {
	check(args.size == 1) {
		"""
			Usage: kotlinc -script match_groups.main.kts <input_file_list> > <output_file_list>
			Invalid arguments: ${args.contentToString()}
		""".trimIndent()
	}
	val inputFile = File(args[0])
	println(group(inputFile))
}

fun group(inputFile: File): String {
	val lines = inputFile.readLines().map(::File).filter(File::exists)
	val groups = group(lines)
	val output = groups.joinToString("\n\n") { group ->
		group.joinToString("\n") { it.absolutePath }
	}
	return output
}

fun group(files: List<File>): List<List<File>> {
	val groups = mutableListOf<MutableList<File>>()
	var currentGroup = mutableListOf(File("."))
	files.forEach { file ->
		val reference = currentGroup.first()
		if (!same(reference, file)) {
			currentGroup = mutableListOf(file)
			groups.add(currentGroup)
		} else {
			currentGroup.add(file)
		}
	}
	return groups
}

fun same(a: File, b: File): Boolean =
	a.length() == b.length()
			&& a.readBytes().contentEquals(b.readBytes())
