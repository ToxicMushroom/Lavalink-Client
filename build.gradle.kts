import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.32"
    id("maven-publish")
}

group = "me.melijn.llklient"
version = "2.1.5"

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_15
    targetCompatibility = JavaVersion.VERSION_15
}

repositories {
    jcenter()
    maven("https://jitpack.io")
    maven {
        url = uri("https://m2.dv8tion.net/releases")
        name = "m2-dv8tion"
    }
    mavenCentral()
    mavenLocal()
}

dependencies {
    // https://bintray.com/dv8fromtheworld/maven/JDA/
    implementation("net.dv8tion:JDA:4.2.1_256")

    // https://github.com/sedmelluq/lavaplayer
    api("com.sedmelluq:lavaplayer:1.3.76")

    // https://mvnrepository.com/artifact/io.prometheus/simpleclient
    implementation("io.prometheus:simpleclient:0.10.0")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib-jdk8
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.4.32")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.3")

    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-jdk8
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.4.3")


    // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio
    implementation("io.ktor:ktor:1.5.3")
    implementation("io.ktor:ktor-client-okhttp:1.5.3")
    implementation("io.ktor:ktor-client-websockets:1.5.3")

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

    artifacts {
        archives(sourcesJar)
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            groupId = rootProject.group as String
            artifactId = "Lavalink-Klient"

            from(components["java"])
            artifact(sourcesJar.get())
        }
    }
    repositories {
        mavenLocal()
    }
}
