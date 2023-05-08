import java.io.File

main(args)

fun main(args: Array<String>) {
	check(args.size == 1) {
		"""
			Usage: kotlinc -script render_groups.main.kts <input_file_list> > <output_file_list>
			Invalid arguments: ${args.contentToString()}
		""".trimIndent()
	}
	val inputFile = File(args[0])
	val groups = parseGroups(inputFile)
	val trees = buildTrees(groups)
	process(trees)
	println(DOT.renderTrees(trees))
}

fun parseGroups(inputFile: File): List<List<File>> =
	inputFile.readLines().split { it.isEmpty() }.map { it.map(::File) }

fun buildTrees(groups: List<List<File>>): Map<File, Tree> {
	val trees = mutableMapOf<File, Tree>()
	groups.forEach { group ->
		val nodes = group.map { file ->
			val root = file.parents.first()
			val tree = trees.getOrPut(root) { Tree(root) }
			tree.insert(file)
		}
		nodes.forEach { node ->
			node.related.addAll(nodes)
			node.related.remove(node)
		}
	}
	return trees
}

fun process(trees: Map<File, Tree>) {
	trees.values.forEach { process(it.root) }
}

fun process(node: Node) {
	node.children.forEach(::process)
	if (node.children.all { it.entry.isFile }) {
		node.children.forEach { child ->
			child.related.forEach {
				it.related.remove(child)
				it.related.add(node)
			}
		}
		node.related.addAll(node.children.flatMap { it.related }.mapNotNull { it.parent })
		node.children.clear()
	}
}

object Text {

	fun renderTrees(trees: Map<File, Tree>): String =
		trees.entries.joinToString("\n\n") { it.key.toString() + ": " + renderTree(it.value.root) }

	private fun renderTree(node: Node): String =
		buildString {
			val name = node.entry.name.takeIf { it.isNotEmpty() } ?: node.entry.absolutePath
			append(name)
			node.children.forEach { child ->
				append("\n")
				append(renderTree(child).prependIndent("\t"))
			}
		}
}

object DOT {

	private fun id(file: File): String =
		escapedString(file.absolutePath)

	private fun label(file: File): String =
		escapedString(file.name.takeIf { it.isNotBlank() } ?: file.absolutePath)

	private fun escapedString(name: String): String =
		"\"" + name.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

	// TODO https://stackoverflow.com/questions/69774111/graphviz-connect-nested-subgraphs
	/**
	 * ```
	 * digraph G {
	 *   a -> b;
	 *   a -> c;
	 *   b -> c [constraint=false, arrowhead = none, style = dashed];
	 * }
	 * ```
	 */
	fun renderTrees(trees: Map<File, Tree>): String =
		renderTree(trees.values.single())

	private fun renderTree(tree: Tree): String =
		buildString {
			append("digraph ${id(tree.root.entry)} {\n")
			renderTree(tree.root)
			append("}\n")
		}

	private fun StringBuilder.renderTree(node: Node) {
		renderNode(node)
		node.children.forEach { child ->
			renderTree(child)
			renderEdge(node, child)
		}
		node.related.forEach { related ->
			renderRelated(node, related)
		}
	}

	private fun StringBuilder.renderNode(node: Node) {
		append("  ")
		append(id(node.entry))
		append("[label = ")
		append(label(node.entry))
		append("];\n")
	}

	private fun StringBuilder.renderEdge(from: Node, to: Node) {
		append("  ")
		append(id(from.entry))
		append(" -> ")
		append(id(to.entry))
		append(";\n")
	}

	private fun StringBuilder.renderRelated(from: Node, to: Node) {
		append("  ")
		append(id(from.entry))
		append(" -> ")
		append(id(to.entry))
		append("[constraint=false, arrowhead = none, style = dashed]")
		append(";\n")
	}
}

class Node(
	val entry: File,
	var parent: Node? = null,
	val children: MutableSet<Node> = mutableSetOf(),
	val related: MutableSet<Node> = mutableSetOf(),
)

class Tree(root: File) {

	val root = Node(root)

	fun insert(file: File): Node {
		val path = file.parents.drop(1)
		var node = root
		path.forEach { pathPart ->
			node = node.children.singleOrNull { it.entry == pathPart }
				?: Node(pathPart, node).also { node.children.add(it) }
		}
		return node
	}
}

val File.parents: List<File>
	get() = generateSequence(this) { it.parentFile }.toList().reversed()

fun <T> List<T>.split(predicate: (T) -> Boolean): List<List<T>> {
	tailrec fun <T> List<T>.split(predicate: (T) -> Boolean, result: List<List<T>>): List<List<T>> {
		if (this.isEmpty()) {
			return result
		}
		val splitIndex = this.indexOfFirst(predicate)
		return if (splitIndex == -1) {
			result + listOf(this)
		} else {
			val before = this.take(splitIndex)
			val after = this.drop(splitIndex + 1)
			after.split(predicate, result + listOf(before))
		}
	}
	return split(predicate, emptyList())
}
