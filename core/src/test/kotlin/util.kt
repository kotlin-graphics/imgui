infix fun <T> T.`should be`(v: T) = assert(this == v)
infix fun <T : Comparable<T>> T.`should be greater than`(v: T) = assert(this > v)