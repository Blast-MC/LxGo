pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

rootProject.name = "LxGo"
include("api")

project(":api").name = "LxGoAPI"