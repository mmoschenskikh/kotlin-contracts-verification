abstract class Rule(var dependingOn: String) {
    override fun toString(): String {
        return "${super.toString()}($dependingOn)"
    }
}

class Copy(dependingOn: String) : Rule(dependingOn)
class InvertBoolean(dependingOn: String) : Rule(dependingOn)
class NullWhenTrue(dependingOn: String) : Rule(dependingOn)
class NotNullWhenTrue(dependingOn: String) : Rule(dependingOn)
