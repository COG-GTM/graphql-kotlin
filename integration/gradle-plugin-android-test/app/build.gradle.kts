plugins {
    id("com.android.application")
    id("com.expediagroup.graphql")
    kotlin("android")
    kotlin("plugin.serialization")
}

android {
    compileSdk = 30
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    namespace = "com.expediagroup.graphqlkotlin"
}

dependencies {
    implementation("com.expediagroup:graphql-kotlin-ktor-client")
    implementation(libs.kotlin.stdlib)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

graphql {
    client {
        schemaFile = file("${project.projectDir}/schema.graphql")
        packageName = "com.expediagroup.android.generated"
        serializer = com.expediagroup.graphql.plugin.gradle.config.GraphQLSerializer.KOTLINX
    }
}
