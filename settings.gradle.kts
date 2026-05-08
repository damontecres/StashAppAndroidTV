pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://repo.repsy.io/mvn/chrynan/public") }
        maven { url = uri("https://jitpack.io") }
        @Suppress("ktlint:standard:property-naming")
        val WholphinExtensionsUsername: String? by settings
        if (!WholphinExtensionsUsername.isNullOrBlank()) {
            maven("https://maven.pkg.github.com/damontecres/wholphin-extensions") {
                name = "WholphinExtensions"
                credentials(PasswordCredentials::class)
            }
        }
    }
}

rootProject.name = "StashAppAndroidTV"
include(":app")

gradle.startParameter.excludedTaskNames.addAll(listOf(":buildSrc:testClasses"))
include(":apollo-compiler")
