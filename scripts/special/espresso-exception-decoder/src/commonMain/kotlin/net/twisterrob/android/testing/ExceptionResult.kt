package net.twisterrob.android.testing

import net.twisterrob.android.testing.renderer.DataNode
import net.twisterrob.android.testing.renderer.RootNode
import net.twisterrob.android.testing.renderer.ViewNode

sealed class ExceptionResult

data class ViewExceptionResult(
	val ex: String,
	val matcher: String,
	val marker: String,
	val hierarchyText: String,
	var hierarchyArray: List<String> = emptyList(),
	var stacktrace: String? = null,
	var hierarchy: ViewNode? = null,
	var resNames: List<String> = emptyList(),
	var types: List<String> = emptyList()
) : ExceptionResult()

data class DataExceptionResult(
	val ex: String,
	val matcher: String,
	val dataText: String,
	var data: MutableList<DataNode> = mutableListOf()
) : ExceptionResult()

data class RootExceptionResult(
	val ex: String,
	val matcher: String,
	val marker: String,
	val rootText: String,
	var roots: MutableList<RootNode> = mutableListOf()
) : ExceptionResult()
