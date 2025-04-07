package com.github.damontecres.stashapp.util

/**
 * A list that lazily transforms items on [get]
 */
class MappedList<T, R>(
    private val sourceList: List<T>,
    private val transform: (Int, T) -> R,
) : AbstractList<R>() {
    constructor(
        sourceList: List<T>,
        transform: (T) -> R,
    ) : this(sourceList, { _, x: T -> transform(x) })

    override val size: Int
        get() = sourceList.size

    override fun get(index: Int): R = transform(index, sourceList[index])
}
