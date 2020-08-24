import analysis.FunctionGraph
import analysis.GraphNode
import analysis.parseLabel
import com.abdullin.kthelper.algorithm.GraphView
import org.jetbrains.research.kfg.ClassManager
import org.jetbrains.research.kfg.KfgConfig
import org.jetbrains.research.kfg.builder.cfg.CfgBuilder
import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.util.Flags
import util.getMethodNodeByName
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FunctionGraphTests {

    private lateinit var graphView: List<GraphView>
    private val methodName = "contractOnReceiver"

    @BeforeTest
    private fun initialize() {
        val cm = ClassManager(KfgConfig(Flags.readAll))
        val bytes = File("target/test-classes/samples/MyContractsKt.class").readBytes()
        val c = ConcreteClass(cm, util.getClassNode(bytes))
        val methodNode = getMethodNodeByName(bytes, methodName)
        val m = Method(cm, methodNode, c)
        CfgBuilder(cm, m).build()
        graphView = m.graphView
    }

    private val nodes = mutableSetOf<GraphNode>()
    private lateinit var entryNode: GraphNode
    private lateinit var returnNode: GraphNode

    @Test
    fun graphNodeTest() {
        graphView.forEach {
            val node = it.parseLabel()
            nodes.add(node)
            if (node.isEntryNode()) {
                entryNode = node
            }
            if (node.isReturnNode()) {
                returnNode = node
            }
        }
        assertTrue(this::entryNode.isInitialized)
        assertTrue(this::returnNode.isInitialized)
        assertTrue(entryNode.predecessors.isNullOrEmpty())

        val graph = FunctionGraph(methodName, nodes, entryNode, returnNode)
        assertEquals(entryNode, graph.findNodeByName("%entry0"))
    }
}