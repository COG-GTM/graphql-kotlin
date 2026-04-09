description = "GraphQL Kotlin Maven Plugin that can generate type-safe GraphQL Kotlin client and GraphQL schema in SDL format using reflections"

buildscript {
    dependencies {
        // Force ASM 9.7 to support Java 21 class files (major version 65) in the
        // maven-plugin-development plugin's descriptor generator
        classpath("org.ow2.asm:asm:9.7")
        classpath("org.ow2.asm:asm-commons:9.7")
        classpath("org.ow2.asm:asm-tree:9.7")
    }
}

plugins {
    id("com.expediagroup.graphql.conventions")
    alias(libs.plugins.maven.plugin.development)
}

dependencies {
    api(projects.graphqlKotlinClientGenerator)
    api(projects.graphqlKotlinSdlGenerator)
    api(projects.graphqlKotlinGraalvmMetadataGenerator)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.maven.plugin.annotations)
    implementation(libs.maven.plugin.api)
    implementation(libs.maven.project)
}

tasks {
    publishing {
        publications {
            val mavenPublication = findByName("mavenJava") as? MavenPublication
            mavenPublication?.pom {
                packaging = "maven-plugin"
            }
        }
    }
}
