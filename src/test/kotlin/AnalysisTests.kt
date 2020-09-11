import analysis.*
import org.jetbrains.research.kfg.ir.BasicBlock
import util.getClassNode
import util.recomputeFrames
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class AnalysisTests {

    private fun prepareForContractCheck(
        functionName: String,
        classPath: String
    ): Map<BasicBlock, Map<String, AnalysisLattice>> {
        val bytes = File(classPath).readBytes()
        val classNode = getClassNode(bytes).recomputeFrames(ClassLoader.getSystemClassLoader())
        val method = prepareFunctionCfg(classNode, functionName)
        return analyze(method)
    }

    @Test
    fun contractsKtTest() {
        val functionName = "returnsNotNullContract"
        val classPath = "target/test-classes/samples/ContractsKt.class"
        val states = prepareForContractCheck(functionName, classPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun myContractsKtTest() {
        val functionName = "multipleEffects" // Checking only returnsNotNull effect
        val classPath = "target/test-classes/samples/MyContractsKt.class"
        val states = prepareForContractCheck(functionName, classPath)
        val conditions = mapOf("arg$1" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    private val rnntsClassPath = "target/test-classes/samples/ReturnsNotNullTestContractsKt.class"

    @Test
    fun emptyFunctionTest() {
        val functionName = "empty"
        val states = prepareForContractCheck(functionName, rnntsClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/Integer" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun onlyNullTest() {
        val functionName = "onlyNull"
        val states = prepareForContractCheck(functionName, rnntsClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun onlyStringTest() {
        val functionName = "onlyString"
        val states = prepareForContractCheck(functionName, rnntsClassPath)
        val conditions: Map<String, AnalysisLattice.Element> = emptyMap()
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun instanceOfCheckTest() {
        val functionName = "instanceOfCheck"
        val states = prepareForContractCheck(functionName, rnntsClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/String" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun contractOnReceiverTest() {
        val functionName = "contractOnReceiver"
        val states = prepareForContractCheck(functionName, rnntsClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }
}
