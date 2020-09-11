package samples

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ExperimentalContracts
fun empty(int: Number) {
    contract {
        returnsNotNull() implies (int is Int)
    }
}

@ExperimentalContracts
fun onlyNull(param: Any?): Unit? {
    contract {
        returnsNotNull() implies (param != null)
    }
    return null
}

@Suppress("Parameter")
@ExperimentalContracts
fun onlyString(cond: Boolean): String {
    contract {
        returnsNotNull()
    }
    return "hi there"
}

@ExperimentalContracts
fun instanceOfCheck(param: Any?): Boolean? {
    contract {
        returnsNotNull() implies (param is String)
    }
    return param is String
}

@ExperimentalContracts
fun Any?.contractOnReceiver(): Unit? {
    contract {
        returnsNotNull() implies (this@contractOnReceiver != null)
    }
    return if (this == null) null else Unit
}

@ExperimentalContracts
fun contractInsideContract(condition: Boolean) {
    contract {
        returnsNotNull() implies (condition)
    }
    require(condition)
}
