import analysis.parseLabel
import org.jetbrains.research.kfg.ClassManager
import org.jetbrains.research.kfg.KfgConfig
import org.jetbrains.research.kfg.builder.cfg.CfgBuilder
import org.jetbrains.research.kfg.ir.ConcreteClass
import org.jetbrains.research.kfg.ir.Method
import org.jetbrains.research.kfg.util.Flags
import util.getClassNode
import util.getMethodNodeByName
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class KfgExtensionsTests {

    @Test
    fun parseLabelTest() {
        val cm = ClassManager(KfgConfig(Flags.readAll))
        val bytes = File("target/test-classes/samples/MyContractsKt.class").readBytes()
        val c = ConcreteClass(cm, getClassNode(bytes))
        val methodNode = getMethodNodeByName(bytes, "contractOnReceiver")
        val m = Method(cm, methodNode, c)
        CfgBuilder(cm, m).build()
        assertTrue { m.graphView.any { it.parseLabel().name == "%entry0" } }
        assertTrue { m.graphView.any { "%entry0" in (it.parseLabel().predecessors ?: listOf()) } }
    }
}