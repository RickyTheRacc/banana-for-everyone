plugins {
    id("fabric-loom") version "1.7-SNAPSHOT"
}

base {
    archivesName = project.property("archives_base_name").toString()
    version = project.property("mod_version").toString()
    group = project.property("maven_group").toString()
}

repositories {
    maven("https://maven.meteordev.org/releases") {
        name = "Meteor Releases"
    }

    maven("https://maven.meteordev.org/snapshots") {
        name = "Meteor Snapshots"
    }

    maven("https://maven.bawnorton.com/releases") {
        name = "Mixin Squared"
    }
}

dependencies {
    // Fabric
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")

    // Meteor
    modImplementation("meteordevelopment:meteor-client:${project.property("meteor_version")}")

    // MixinSquared
    include(implementation(
        annotationProcessor("com.github.bawnorton.mixinsquared:mixinsquared-fabric:${project.property("mixinsquared_version")}")!!
    )!!)
}

tasks {
    processResources {
        val projectProperties = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version")
        )

        inputs.properties(projectProperties)

        filesMatching("fabric.mod.json") {
            expand(projectProperties)
        }
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

loom {
    accessWidenerPath = file("src/main/resources/bananaplus.accesswidener")
}