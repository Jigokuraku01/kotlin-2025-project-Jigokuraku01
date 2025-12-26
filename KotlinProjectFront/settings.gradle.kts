pluginManagement {
    repositories {
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

includeBuild("../KotlinProjectBack/backend") {
    dependencySubstitution {
        substitute(module("org.example:server")).using(project(":server"))
        substitute(module("org.example:client")).using(project(":client"))
        substitute(module("org.example:general")).using(project(":general"))
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "frontend"
include(":app")
 