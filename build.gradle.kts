import java.io.RandomAccessFile

plugins {
    `java-library`
    application
    `maven-publish`
}

group = "org.glavo"
version = "0.3"

val javahMainClassName = "org.glavo.javah.Main"

application {
    mainClass.set(javahMainClassName)
}

tasks.jar {
    manifest.attributes(mapOf(
            "Implementation-Version" to "1.2",
            "Main-Class" to javahMainClassName
    ))
}

repositories {
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/org.ow2.asm/asm
    implementation("org.ow2.asm:asm:9.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.5.2")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(9)
}

tasks.compileJava {
    modularity.inferModulePath.set(true)
    options.release.set(9)

    doLast {
        val tree = fileTree(destinationDir)
        tree.include("**/*.class")
        tree.exclude("module-info.class")
        tree.forEach {
            RandomAccessFile(it, "rw").use { rf ->
                rf.seek(7)   // major version
                rf.write(52) // java 8
                rf.close()
            }
        }
    }
}

tasks.create<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into("$buildDir/libs")
}

java {
    withSourcesJar()
    // withJavadocJar()
}

tasks.withType<Javadoc>().configureEach {
    (options as StandardJavadocDocletOptions).also {
        it.encoding("UTF-8")
        it.addStringOption("link", "https://docs.oracle.com/en/java/javase/11/docs/api/")
        it.addBooleanOption("html5", true)
        it.addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.withType<GenerateModuleMetadata>().configureEach {
    enabled = false
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            version = project.version.toString()
            artifactId = project.name
            from(components["java"])

            pom {
                licenses {
                    license {
                        name.set("Apache-2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("glavo")
                        name.set("Glavo")
                        email.set("zjx001202@gmail.com")
                    }
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}
