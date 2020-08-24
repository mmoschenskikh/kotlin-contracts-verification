package analysis

import com.abdullin.kthelper.algorithm.GraphView

typealias NodeName = String

class GraphNode private constructor(
    val graphView: GraphView,
    val name: NodeName,
    val predecessors: Set<NodeName>?,
    val instructions: List<String>?,
    val successors: Set<NodeName>?
) {
    data class Builder(
        var graphView: GraphView? = null,
        var name: NodeName? = null,
        var predecessors: Set<NodeName>? = null,
        var instructions: List<String>? = null,
        var successors: Set<NodeName>? = null
    ) {
        fun build(): GraphNode = try {
            GraphNode(graphView!!, name!!, predecessors, instructions, successors)
        } catch (npe: NullPointerException) {
            throw IllegalStateException("GraphView, name and instructions must be specified.")
        }
    }

    override fun toString(): String {
        return "GraphNode $name\n" +
                "predecessors: ${predecessors?.joinToString(", ") ?: "none"}\n" +
                "successors: ${successors?.joinToString(", ") ?: "none"}\n" +
                "instructions: ${instructions?.filter { it.isNotBlank() }?.joinToString("\n") ?: "none"}"
    }
}