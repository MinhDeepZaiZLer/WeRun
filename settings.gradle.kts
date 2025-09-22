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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = "pk.eyJ1IjoibWluaHNvZGVlcCIsImEiOiJjbWFxbnF4cXEwMXJ1Mm1weWNlMjMzM2swIn0.kCOAaSSzFck9YUTW3sHqxg" // Thay bằng token của bạn
            }
        }
    }
}

rootProject.name = "WeRun"
include(":app")
 