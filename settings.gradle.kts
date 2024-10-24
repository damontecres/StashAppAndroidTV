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
    }
}

rootProject.name = "StashAppAndroidTV"
include(":app")

gradle.startParameter.excludedTaskNames.addAll(listOf(":buildSrc:testClasses"))
include(":apollo-compiler")
