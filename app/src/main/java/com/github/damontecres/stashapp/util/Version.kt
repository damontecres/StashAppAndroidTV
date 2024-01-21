package com.github.damontecres.stashapp.util

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val numCommits: Int? = null,
    val hash: String? = null,
) {
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
        val MINIMUM_STASH_VERSION = fromString("0.23.0")

        fun fromString(version: String): Version {
            if (version.contains("-g")) {
                val splits = version.removePrefix("v").split(".")
                val major = splits[0].toInt()
                val minor = splits[1].toInt()
                val patchString = splits[2]
                val patchSplit = patchString.split("-")
                val patch = patchSplit[0].toInt()
                val numCommits = patchSplit[1].toInt()
                val hash = patchSplit[2].removePrefix("g")
                return Version(major, minor, patch, numCommits, hash)
            } else {
                val splits = version.removePrefix("v").split(".")
                val major = splits[0].toInt()
                val minor = splits[1].toInt()
                val patch = splits[2].toInt()
                return Version(major, minor, patch)
            }
        }

        fun isStashVersionSupported(version: Version?): Boolean {
            if (version == null) {
                return false
            }
            return version.isAtLeast(MINIMUM_STASH_VERSION)
        }
    }
}
