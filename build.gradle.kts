plugins {
    java
    `maven-publish`
    id("io.freefair.lombok") version "8.11"
    id("com.gradleup.shadow") version "8.3.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

allprojects {
    group = "${project.group}"
    version = "${project.version}"

    repositories {
        mavenLocal()
        maven { url = uri("https://repo.papermc.io/repository/maven-public/") }
        maven { url = uri("https://sonatype.projecteden.gg/repository/maven-public/") }
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "io.freefair.lombok")
    apply(plugin = "maven-publish")

    dependencies {
        compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":LxGoAPI"))
    implementation("gg.projecteden:commands-api:1.0.0-SNAPSHOT")
    implementation("org.reflections:reflections:0.10.2")
    paperweight.paperDevBundle("1.21.8-R0.1-SNAPSHOT", "gg.projecteden.parchment")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
        options.compilerArgs.add("-parameters")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()

        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        relocate("org.reflections", "holograms.org.reflections")
    }

}
