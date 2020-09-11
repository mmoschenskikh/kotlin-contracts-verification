package analysis

enum class ContractInfo {
    FUNCTION_MATCHES_THE_CONTRACT,
    FUNCTION_DOES_NOT_MATCH_THE_CONTRACT,
    INAPPLICABLE_CONTRACT
    /*
    The third state is about returns(null) where function return type is Int;
    returns(true) when function body is just "return false", etc.
    */
}
