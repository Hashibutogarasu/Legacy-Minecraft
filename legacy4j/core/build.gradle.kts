plugins {
    id("java")
    id("dev.architectury.loom")
}

group = rootProject.property("maven_group").toString()
version = "unspecified"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.fabricmc.net/") }
    maven { url = uri("https://maven.architectury.dev/") }
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://maven.neoforged.net/releases/") }
    maven { url = uri("https://jm.gserv.me/repository/maven-public/") }
    maven { url = uri("https://maven.isxander.dev/releases") }
    maven { url = uri("https://www.cursemaven.com") }
    maven { url = uri("https://raw.githubusercontent.com/Kyubion-Studios/Mod-Resources/main/maven/") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.terraformersmc.com/") }
    maven { url = uri("https://maven.su5ed.dev/releases") }
    maven { url = uri("https://maven.blamejared.com/") }
    maven { url = uri("https://modmaven.dev") }
}

// Get Minecraft version from root project's stonecutter configuration
// This ensures version consistency with the main build
val minecraftVersion = rootProject.extensions.extraProperties["mc_version"].toString()

dependencies {
    // Minecraft dependencies - compileOnly so they're not included in the JAR
    minecraft("com.mojang:minecraft:${minecraftVersion}")
    mappings(loom.officialMojangMappings())
    
    // Mixin dependencies - required for annotation processing
    // These are compileOnly because they're provided by the mod loader at runtime
    compileOnly("org.spongepowered:mixin:0.8.5")
    annotationProcessor("org.spongepowered:mixin:0.8.5:processor")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.13.2")

    // Test dependencies
    testImplementation("junit:junit:4.13.1")
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}

// Determine Java version based on Minecraft version
// Minecraft 1.20.5+ requires Java 21, earlier versions use Java 17
val isJava21 = minecraftVersion.split(".").let { parts ->
    val major = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val minor = parts.getOrNull(2)?.toIntOrNull() ?: 0
    major > 20 || (major == 20 && minor >= 5)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(if (isJava21) 21 else 17)
}

val javaVersion = if (isJava21) JavaVersion.VERSION_21 else JavaVersion.VERSION_17

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.test {
    useJUnitPlatform()
}

loom {
    // Basic Loom configuration for library module
    mixin {
        useLegacyMixinAp.set(true)
    }
}