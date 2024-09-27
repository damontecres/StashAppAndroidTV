package com.github.damontecres.stashapp.util

data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val numCommits: Int? = null,
    val hash: String? = null,
) {
    /**
     * Is this version at least the given version
     *
     * Checks major, minor, & patch only!
     */
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

    /**
     * Is this greater than the given version (and not equal to!)
     *
     * Checks major, minor, & patch only!
     */
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

    /**
     * Is this equal to or before the specified version
     */
    fun isEqualOrBefore(version: Version): Boolean {
        return !isGreaterThan(version)
    }

    private fun compareNumCommits(version: Version): Int {
        return (this.numCommits ?: 0) - (version.numCommits ?: 0)
    }

    override fun toString(): String {
        return if (numCommits != null && hash != null) {
            "v$major.$minor.$patch-$numCommits-g$hash"
        } else {
            "v$major.$minor.$patch"
        }
    }

    companion object {
        private val VERSION_REGEX = Regex("v?(\\d+)\\.(\\d+)\\.(\\d+)(-(\\d+)-g([a-zA-Z0-9]+))?")
        val V0_26_0 = fromString("v0.26.0")
        val V0_26_2 = fromString("v0.26.2")
        val V0_27_0 = fromString("v0.27.0")
        val DEVELOP = fromString("v0.26.2-97-gad442fbe")

        val MINIMUM_STASH_VERSION = DEVELOP

        /**
         * Parse a version string throwing if it is invalid
         */
        fun fromString(version: String): Version {
            val v = tryFromString(version)
            if (v == null) {
                throw IllegalArgumentException(version)
            } else {
                return v
            }
        }

        /**
         * Attempt to parse a version string or else return null
         */
        fun tryFromString(version: String?): Version? {
            if (version == null) {
                return null
            }
            val m = VERSION_REGEX.matchEntire(version)
            return if (m == null) {
                null
            } else {
                val major = m.groups[1]!!.value.toInt()
                val minor = m.groups[2]!!.value.toInt()
                val patch = m.groups[3]!!.value.toInt()
                // group 4 is the optional commit info
                val numCommits = m.groups[5]?.value?.toInt()
                val hash = m.groups[6]?.value
                Version(major, minor, patch, numCommits, hash)
            }
        }

        /**
         * Returns if the version is at least [MINIMUM_STASH_VERSION]
         */
        fun isStashVersionSupported(version: Version?): Boolean {
            if (version == null) {
                return false
            }
            return version.isAtLeast(MINIMUM_STASH_VERSION)
        }

        /**
         * Checks if the given server and app versions are compatible
         *
         * Versions are offset by minor version 22: https://github.com/damontecres/StashAppAndroidTV/discussions/308
         */
        fun isServerSupportedByAppVersion(
            serverVersion: Version,
            appVersion: Version,
        ): Boolean {
            val minServerVer = appVersion.copy(minor = appVersion.minor + 22, patch = 0)
            return serverVersion.isAtLeast(minServerVer)
        }
    }
}
