package samples

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ExperimentalContracts
fun Boolean?.multipleEffects(number: Int?, condition: Boolean, block: () -> Unit): Boolean? {
    contract {
        returns(true) implies (this@multipleEffects == null)
        returnsNotNull() implies (number != null)
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    return condition
}

@ExperimentalContracts
fun String?.contractOnReceiver() {
    contract {
        returnsNotNull() implies (this@contractOnReceiver is String)
    }
    return
}

@kotlin.contracts.ExperimentalContracts
fun returnsTrueContract(string: String?, condition: Boolean): Boolean {
    contract {
        returns(true) implies (condition && string != null)
    }
    require(string != null)
    return condition
}

@kotlin.contracts.ExperimentalContracts
fun returnsFalseContract(string: CharSequence?, falseCondition: Boolean): Boolean {
    contract {
        returns(false) implies (string is String || falseCondition)
    }
    return !falseCondition
}