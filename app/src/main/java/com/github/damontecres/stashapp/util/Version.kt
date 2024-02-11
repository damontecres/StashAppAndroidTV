package com.github.damontecres.stashapp.util

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val numCommits: Int? = null,
    val hash: String? = null,
) {
    fun isAtLeast(version: Version): Boolean {
        if (this.major > version.major) {
            return true
        } else if (this.major == version.major) {
            if (this.minor > version.minor) {
                return true
            } else if (this.minor == version.minor) {
                if (this.patch > version.patch) {
                    return true
                } else if (this.patch == version.patch) {
                    if (this.compareNumCommits(version) >= 0) {
                        return true
                    }
                }
            }
        }
        return false
    }

    fun isGreaterThan(version: Version): Boolean {
        if (this.major > version.major) {
            return true
        } else if (this.major == version.major) {
            if (this.minor > version.minor) {
                return true
            } else if (this.minor == version.minor) {
                if (this.patch > version.patch) {
                    return true
                } else if (this.patch == version.patch) {
                    if (this.compareNumCommits(version) > 0) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun compareNumCommits(version: Version): Int {
        return (this.numCommits ?: 0) - (version.numCommits ?: 0)
    }

    companion object {
        private val VERSION_REGEX = Regex("v?(\\d+)\\.(\\d+)\\.(\\d+)(-(\\d+)-g([a-zA-Z0-9]+))?")
        private val MINIMUM_STASH_VERSION = fromString("0.23.0")

        fun fromString(version: String): Version {
            val m = VERSION_REGEX.matchEntire(version)
            if (m == null) {
                throw IllegalArgumentException()
            } else {
                val major = m.groups[1]!!.value.toInt()
                val minor = m.groups[2]!!.value.toInt()
                val patch = m.groups[3]!!.value.toInt()
                // group 4 is the optional commit info
                val numCommits = m.groups[5]?.value?.toInt()
                val hash = m.groups[6]?.value
                return Version(major, minor, patch, numCommits, hash)
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
