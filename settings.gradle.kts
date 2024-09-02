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
    }
}

rootProject.name = "StashAppAndroidTV"
include(":app")

gradle.startParameter.excludedTaskNames.addAll(listOf(":buildSrc:testClasses"))
include(":apollo-compiler")
