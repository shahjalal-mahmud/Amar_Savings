pluginManagement {
    repositories {
        google()              // Simplified: removes the restrictive content filters
        mavenCentral()        // KSP plugin wrappers live here
        gradlePluginPortal()  // Fallback portal
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Amar Savings"
include(":app")