description = "GraphQL Kotlin Maven Plugin that can generate type-safe GraphQL Kotlin client and GraphQL schema in SDL format using reflections"

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

// Override ASM version used by maven-plugin-development plugin to support Java 21 class files (major version 65)
buildscript {
    dependencies {
        classpath("org.ow2.asm:asm:9.7")
        classpath("org.ow2.asm:asm-commons:9.7")
        classpath("org.ow2.asm:asm-tree:9.7")
        classpath("org.ow2.asm:asm-analysis:9.7")
    }
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
