package com.github.damontecres.stashapp.image

/**
 * Interface to expose making modifications to an image
 */
interface ImageController {
    fun zoomIn()

    fun zoomOut()

    fun rotateLeft()

    fun rotateRight()

    fun flip()

    fun reset()

    fun isImageZoomedIn(): Boolean
}
