import analysis.AnalysisLattice
import analysis.ContractInfo
import analysis.checkReturnsNullContract
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsNullTests {

    @Test
    fun contractsKtTest() {
        val functionName = "returnsNullContract"
        val classPath = "target/test-classes/samples/ContractsKt.class"
        val states = prepareForContractCheck(functionName, classPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNullContract(states, conditions))
    }

    private val rnntcClassPath = "target/test-classes/samples/ReturnsSmthTestContractsKt.class"

    @Test
    fun emptyFunctionTest() {
        val functionName = "empty"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/Integer" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsNullContract(states, conditions))
    }

    @Test
    fun onlyNullTest() {
        val functionName = "onlyNull"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsNullContract(states, conditions))
    }

    @Test
    fun onlyStringTest() {
        val functionName = "onlyString"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions: Map<String, AnalysisLattice.Element> = emptyMap()
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsNullContract(states, conditions))
    }

    @Test
    fun instanceOfCheckTest() {
        val functionName = "instanceOfCheck"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/String" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsNullContract(states, conditions))
    }

    @Test
    fun contractOnReceiverTest() {
        val functionName = "contractOnReceiver"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NULL)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNullContract(states, conditions))
    }

    @Test
    fun contractInContractTest() {
        val functionName = "contractInsideContract"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsNullContract(states, conditions))
    }
}
