package analysis

/**
 * Represents a lattice flat(A) built from set A = {elem1, elem2, elem3}, like:
 *         T
 *     /   |   \
 * elem1 elem2 elem3
 *     \   |   /
 *         ⊥
 */
class FlatLattice<T> constructor(set: Set<T>) {

    val top = set
    val elements = set.map { setOf(it) }.toSet()
    val bottom = emptySet<T>()
    var state = top
        set(value) {
            if (value in elements || value == top || value == bottom) field = value
            else throw NoSuchElementException("No such element in the lattice")
        }

    fun join(vararg latticeElements: Set<T>): Set<T> {
        val elem = latticeElements.toSet()
        return when {
            elem.any { it == top } -> top
            elem.count { it in elements } > 1 -> top
            elem.count { it in elements } == 1 -> elem.first { it in elements }
            else -> bottom
        }
    }

    fun meet(vararg latticeElements: Set<T>): Set<T> {
        val elem = latticeElements.toSet()
        return when {
            elem.any { it == bottom } -> bottom
            elem.count { it in elements } > 1 -> bottom
            elem.count { it in elements } == 1 -> elem.first { it in elements }
            else -> top
        }
    }
}