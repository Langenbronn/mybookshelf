rootProject.name = "MyBookshelf"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// The app itself: shared code + Android + iOS entry points
include(":composeApp")
