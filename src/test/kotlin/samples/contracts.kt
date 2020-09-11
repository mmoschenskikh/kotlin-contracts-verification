/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 */

// Package name changed
package samples

// Redundant imports removed
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@kotlin.contracts.ExperimentalContracts
fun returnsContract(condition: Boolean) {
    contract {
        returns() implies (condition)
    }
    if (!condition) throw IllegalArgumentException()
}

@kotlin.contracts.ExperimentalContracts
fun returnsTrueContract(condition: Boolean): Boolean {
    contract {
        returns(true) implies (condition)
    }
    return condition
}

@kotlin.contracts.ExperimentalContracts
fun returnsFalseContract(condition: Boolean): Boolean {
    contract {
        returns(false) implies (condition)
    }
    return !condition
}

@kotlin.contracts.ExperimentalContracts
fun returnsNullContract(condition: Boolean): Boolean? {
    contract {
        returns(null) implies (condition)
    }
    return if (condition) null else false
}

@kotlin.contracts.ExperimentalContracts
fun returnsNotNullContract(condition: Boolean): Boolean? {
    contract {
        returnsNotNull() implies (condition)
    }
    return if (condition) true else null
}

@kotlin.contracts.ExperimentalContracts
inline fun callsInPlaceAtMostOnceContract(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_MOST_ONCE)
    }
}

@kotlin.contracts.ExperimentalContracts
inline fun callsInPlaceAtLeastOnceContract(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.AT_LEAST_ONCE)
    }
    block()
    block()
}

@kotlin.contracts.ExperimentalContracts
inline fun <T> callsInPlaceExactlyOnceContract(block: () -> T): T {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

@kotlin.contracts.ExperimentalContracts
inline fun callsInPlaceUnknownContract(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.UNKNOWN)
    }
    block()
    block()
    block()
}

// Class "Contracts" was removed
