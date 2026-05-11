import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

tasks {
    kotlin {
        jvmToolchain(21)
    }
    val kotlinJvmVersion: String by project
    withType<KotlinCompile> {
        kotlinOptions {
            // intellij gets confused without it
            jvmTarget = kotlinJvmVersion
            freeCompilerArgs = listOf("-Xjsr305=strict")
        }
    }
}
