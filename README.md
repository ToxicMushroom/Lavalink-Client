# Lavalink-Klient
Fork off Lavalink-Client [![Release](https://img.shields.io/github/tag/freyacodes/Lavalink-Client.svg)](https://jitpack.io/#freyacodes/Lavalink-Client)

## Installation
Lavalink does not have a maven repository and instead uses Jitpack.
You can add the following to your POM if you're using Maven:
```xml
<dependencies>
    <dependency>
        <groupId>com.github.toxicmushroom</groupId>
        <artifactId>Lavalink-Klient</artifactId>
        <version>x.y.z</version>
    </dependency>
</dependencies>
```

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Or Gradle.kts

```kotlin
    repositories {
        maven("https://jitpack.io")
    }

    dependencies {
        implementation("com.github.freyacodes:Lavalink-Klient:x.y.z")
    }
```

### Jitpack versions
Jitpack versioning is based on git branches and commit hashes, or tags. Eg:

```
ab123c4d
master-SNAPSHOT
dev-SNAPSHOT
3.2
```

***Note:*** The above versions are for example purposes only.

Version tags of this client are expected to roughly follow lavalink server versioning.