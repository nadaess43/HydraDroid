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
        maven("https://dl.frostwire.com/maven") // jlibtorrent (libtorrent для Android)
    }
}
rootProject.name = "HydraDroid"
include(":app")
