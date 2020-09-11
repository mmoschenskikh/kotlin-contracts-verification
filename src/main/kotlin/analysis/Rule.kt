package analysis

class Rule(val dependingOn: String, val ruleType: Type) {
    enum class Type {
        COPY, INVERT_BOOLEAN, NULL_WHEN_TRUE, NOT_NULL_WHEN_TRUE
    }

    override fun toString(): String {
        return "${ruleType}($dependingOn)"
    }
}
