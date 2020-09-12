import analysis.AnalysisLattice
import analysis.ContractInfo
import analysis.checkReturnsContract
import analysis.checkReturnsNotNullContract
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsTests {

    @Test
    fun contractsKtTest() {
        val functionName = "returnsContract"
        val classPath = "target/test-classes/samples/ContractsKt.class"
        val states = prepareForContractCheck(functionName, classPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    private val rtcClassPath = "target/test-classes/samples/ReturnsTestContractsKt.class"

    @Test
    fun requireTest() {
        val functionName = "require"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun requireNotNullTest() {
        val functionName = "requireNotNull"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun checkTest() {
        val functionName = "check"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }

    @Test
    fun checkNotNullTest() {
        val functionName = "checkNotNull"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsNotNullContract(states, conditions))
    }
}
