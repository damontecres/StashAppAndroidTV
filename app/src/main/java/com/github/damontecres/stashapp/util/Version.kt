package com.github.damontecres.stashapp.util

data class Version(val version: String) {
    val major: Int
    val minor: Int
    val patch: Int

    init {
        val splits = version.removePrefix("v").split(".").map { it.toInt() }.toList()
        major = splits[0]
        minor = splits[1]
        patch = splits[2]
    }

    fun isAtLeast(version: Version): Boolean {
        return this.major > version.major || (
            this.major == version.major &&
                (
                    this.minor > version.minor || this.minor == version.minor &&
                        this.patch >= version.patch
                )
        )
    }

    companion object {
        val MINIMUM_STASH_VERSION = Version("0.23.0")

        fun isStashVersionSupported(version: Version?): Boolean {
            if (version == null) {
                return false
            }
            return version.isAtLeast(MINIMUM_STASH_VERSION)
        }
    }
}
