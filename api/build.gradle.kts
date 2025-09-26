publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }

    repositories {
        maven {
            name = "edenSnapshots"
            url = uri("https://maven.projecteden.gg/snapshots")
            credentials(PasswordCredentials::class)
        }
    }
}

tasks {
    java {
        withSourcesJar()
        withJavadocJar()
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
}