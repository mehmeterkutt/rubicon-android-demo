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
rootProject.name = "Rubicon"
include(":app")

// The SDK, built from its checkout until it is published (composite build): its
// com.rubicon.analytics:sdk-android replaces the dependency below. Override the path with
// rubicon.sdk.dir in ~/.gradle/gradle.properties or with -Prubicon.sdk.dir=...
val sdkDir = providers.gradleProperty("rubicon.sdk.dir").getOrElse("../../rubicon-android-sdk")
includeBuild(sdkDir)
