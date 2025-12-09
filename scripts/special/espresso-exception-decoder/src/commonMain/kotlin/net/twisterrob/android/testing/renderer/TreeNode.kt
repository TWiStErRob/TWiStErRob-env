package net.twisterrob.android.testing.renderer

sealed interface TreeNode {

	val name: String
	val children: MutableList<ViewNode>
	var parent: ViewNode?
	val props: MutableMap<String, Any>
}

data class RootNode(
	override val name: String,
	override val children: MutableList<ViewNode> = mutableListOf(),
	override val props: MutableMap<String, Any> = mutableMapOf(),
) : TreeNode {

	override var parent: ViewNode? = null
}

data class ViewNode(
	val level: Int,
	override val name: String,
	val id: String,
	val matches: Boolean,
	override val children: MutableList<ViewNode> = mutableListOf(),
	override val props: MutableMap<String, Any> = mutableMapOf(),
) : TreeNode {

	override var parent: ViewNode? = null
}

data class DataNode(
	override val name: String,
	override val children: MutableList<ViewNode> = mutableListOf(),
	override val props: MutableMap<String, Any> = mutableMapOf(),
) : TreeNode {

	override var parent: ViewNode? = null
}
