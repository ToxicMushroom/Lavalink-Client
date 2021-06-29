import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.20"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("maven-publish")
}

group = "me.melijn.llklient"
version = "2.2.3"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

repositories {
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
    mavenCentral()
    mavenLocal()
}

val ktor = "1.6.0"
val kotlin = "1.5.20"
val kotlinX = "1.5.0"

dependencies {
    // https://ci.dv8tion.net/job/JDA/
    implementation("net.dv8tion:JDA:4.3.0_288")

    // https://github.com/sedmelluq/lavaplayer
    api("com.sedmelluq:lavaplayer:1.3.78")

    // https://mvnrepository.com/artifact/io.prometheus/simpleclient
    implementation("io.prometheus:simpleclient:0.10.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinX")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$kotlinX")


    // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
    implementation("io.ktor:ktor:$ktor")
    implementation("io.ktor:ktor-client-okhttp:$ktor")
    implementation("io.ktor:ktor-client-websockets:$ktor")

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:1.7.30")
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

tasks {
    withType(JavaCompile::class) {
        options.encoding = "UTF-8"
    }
    withType(KotlinCompile::class) {
        kotlinOptions {
            jvmTarget = "15"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("https://nexus.melijn.com/repository/maven-releases/")
            credentials {
                username = property("melijnPublisher").toString()
                password = property("melijnPassword").toString()
            }
        }
    }
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
}