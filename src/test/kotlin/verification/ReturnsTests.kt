package verification

import analysis.AnalysisLattice
import analysis.ContractInfo
import analysis.checkReturnsContract
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
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun requireNotNullTest() {
        val functionName = "requireNotNull"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun checkTest() {
        val functionName = "check"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun checkNotNullTest() {
        val functionName = "checkNotNull"
        val states = prepareForContractCheck(functionName, rtcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    private val rnntcClassPath = "target/test-classes/samples/ReturnsSmthTestContractsKt.class"

    @Test
    fun emptyFunctionTest() {
        val functionName = "empty"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/Integer" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun onlyNullTest() {
        val functionName = "onlyNull"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun onlyStringTest() {
        val functionName = "onlyString"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions: Map<String, AnalysisLattice.Element> = emptyMap()
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun instanceOfCheckTest() {
        val functionName = "instanceOfCheck"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/String" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun contractOnReceiverTest() {
        val functionName = "contractOnReceiver"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.FUNCTION_DOES_NOT_MATCH_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun contractInContractTest() {
        val functionName = "contractInsideContract"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }

    @Test
    fun checkWithCastTest() {
        val functionName = "checkWithCast"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/String" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsContract(states, conditions))
    }
}
