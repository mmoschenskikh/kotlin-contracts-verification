package analysis

class AnalysisLattice {
    enum class Element {
        TOP, NULL, NOTNULL, TRUE, FALSE, OTHER, BOTTOM
    }

    private val notNull = setOf(Element.TRUE, Element.FALSE, Element.OTHER, Element.NOTNULL)
    var state = Element.BOTTOM

    fun join(vararg elements: Element): Element {
        var elem = elements.toSet()
        if (elem.size == 1 && elem.first() == Element.BOTTOM)
            return Element.BOTTOM
        else
            elem = elem.filterNot { it == Element.BOTTOM }.toSet()
        return when {
            elem.size == 1 -> elem.first()
            elem.all { it in notNull } -> Element.NOTNULL
            else -> Element.TOP
        }
    }

    override fun toString(): String {
        return state.name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnalysisLattice

        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        return state.hashCode()
    }


}
