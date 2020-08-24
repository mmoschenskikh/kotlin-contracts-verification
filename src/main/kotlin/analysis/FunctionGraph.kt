package analysis

class FunctionGraph constructor(
    val functionName: String,
    val nodes: Set<GraphNode>,
    val entryNode: GraphNode,
    val returnNode: GraphNode
) {
    fun findNodeByName(name: String) = nodes.find { it.name == name }

    override fun toString(): String {
        return "FunctionGraph $functionName\n" +
                "entryNode: ${entryNode.name}\n" +
                "returnNode: ${returnNode.name}"
    }
}