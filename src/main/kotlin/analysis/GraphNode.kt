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

    fun isEntryNode(): Boolean = name == "%entry0" && predecessors.isNullOrEmpty()
    fun isReturnNode(): Boolean = instructions?.any { it.contains("return") } ?: false && successors.isNullOrEmpty()

    override fun toString(): String {
        return "GraphNode $name\n" +
                "predecessors: ${predecessors?.joinToString(", ") ?: "none"}\n" +
                "successors: ${successors?.joinToString(", ") ?: "none"}\n" +
                "instructions: ${instructions?.filter { it.isNotBlank() }?.joinToString("\n") ?: "none"}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GraphNode

        if (graphView != other.graphView) return false
        if (name != other.name) return false
        if (predecessors != other.predecessors) return false
        if (instructions != other.instructions) return false
        if (successors != other.successors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = graphView.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (predecessors?.hashCode() ?: 0)
        result = 31 * result + (instructions?.hashCode() ?: 0)
        result = 31 * result + (successors?.hashCode() ?: 0)
        return result
    }
}