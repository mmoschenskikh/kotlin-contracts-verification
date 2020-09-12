package verification

import analysis.AnalysisLattice
import analysis.ContractInfo
import analysis.checkReturnsFalseContract
import kotlin.test.Test
import kotlin.test.assertEquals

class ReturnsFalseTests {

    @Test
    fun contractsKtTest() {
        val functionName = "returnsFalseContract"
        val classPath = "target/test-classes/samples/ContractsKt.class"
        val states = prepareForContractCheck(functionName, classPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    private val rnntcClassPath = "target/test-classes/samples/ReturnsSmthTestContractsKt.class"

    @Test
    fun emptyFunctionTest() {
        val functionName = "empty"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/Integer" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    @Test
    fun onlyNullTest() {
        val functionName = "onlyNull"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    @Test
    fun onlyStringTest() {
        val functionName = "onlyString"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions: Map<String, AnalysisLattice.Element> = emptyMap()
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    @Test
    fun instanceOfCheckTest() {
        val functionName = "instanceOfCheck"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0 instanceOf java/lang/String" to AnalysisLattice.Element.FALSE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    @Test
    fun contractOnReceiverTest() {
        val functionName = "contractOnReceiver"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.NOTNULL)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    @Test
    fun contractInContractTest() {
        val functionName = "contractInsideContract"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.TRUE)
        assertEquals(ContractInfo.INAPPLICABLE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }

    @Test
    fun cannotVerifyTest() {
        val functionName = "speciallyForReturnsFalse"
        val states = prepareForContractCheck(functionName, rnntcClassPath)
        val conditions = mapOf("arg$0" to AnalysisLattice.Element.FALSE)
        assertEquals(ContractInfo.FUNCTION_MATCHES_THE_CONTRACT, checkReturnsFalseContract(states, conditions))
    }
}
