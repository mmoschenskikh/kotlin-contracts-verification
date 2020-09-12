package samples

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
fun empty(int: Number) {
    contract {
        returnsNotNull() implies (int is Int)
        returns() implies (int is Int)
        returns(null) implies (int is Int)
        returns(true) implies (int is Int)
        returns(false) implies (int is Int)
    }
}

@ExperimentalContracts
fun onlyNull(param: Any?): Unit? {
    contract {
        returnsNotNull() implies (param != null)
        returns() implies (param != null)
        returns(null) implies (param != null)
        returns(true) implies (param != null)
        returns(false) implies (param != null)
    }
    return null
}

@Suppress("Parameter")
@ExperimentalContracts
fun onlyString(cond: Boolean): String {
    contract {
        returnsNotNull()
        returns()
        returns(null)
        returns(true)
        returns(false)
    }
    return "hi there"
}

@ExperimentalContracts
fun instanceOfCheck(param: Any?): Boolean {
    contract {
        returnsNotNull() implies (param is String)
        returns() implies (param is String)
        returns(null) implies (param is String)
        returns(true) implies (param is String)
        // Note that contract changed for returns(false) effect
        returns(false) implies (param !is String)
    }
    return param is String
}

@ExperimentalContracts
fun Any?.contractOnReceiver(): Unit? {
    contract {
        returnsNotNull() implies (this@contractOnReceiver != null)
        returns() implies (this@contractOnReceiver != null)
        // Note that contract changed for returns(null) effect
        returns(null) implies (this@contractOnReceiver == null)
        returns(true) implies (this@contractOnReceiver != null)
        returns(false) implies (this@contractOnReceiver != null)
    }
    return if (this == null) null else Unit
}

@ExperimentalContracts
fun contractInsideContract(condition: Boolean) {
    contract {
        returnsNotNull() implies (condition)
        returns() implies (condition)
        returns(null) implies (condition)
        returns(true) implies (condition)
        returns(false) implies (condition)
    }
    require(condition) // Function matches the contract because require() is inline function
}

// Only for returns(true)
@kotlin.contracts.ExperimentalContracts
fun speciallyForReturnsTrue(condition: Boolean): Boolean? {
    contract {
        returns(true) implies (condition)
    }
    return if (condition) true else null
}

// Only for returns(false)
@kotlin.contracts.ExperimentalContracts
fun speciallyForReturnsFalse(condition: Boolean): Boolean? {
    contract {
        returns(false) implies (!condition)
    }
    return condition
}
