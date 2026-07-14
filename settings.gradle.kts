pluginManagement {
    repositories {
        // maven { url = uri("https://maven.myket.ir") }
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // maven { url = uri("https://maven.myket.ir") }
        google()
        mavenCentral()
    }
}

rootProject.name = "IO_motion"
include(":app")
include(":core-common")
include(":core-analysis")
include(":core-pose")
include(":core-ui")
include(":data")
include(":core-export")
include(":feature-home")
include(":feature-workout")
include(":feature-diet")
include(":feature-live")
include(":feature-video")
include(":feature-history")
