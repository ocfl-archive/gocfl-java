plugins {
    `java-library`
    `maven-publish`
    id("com.google.protobuf") version "0.9.4"
}

group = "org.ocflarchive"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val grpcVersion = "1.68.1"
val protobufVersion = "3.25.5"
val junitVersion = "5.11.3"
val slf4jVersion = "2.0.16"

dependencies {
    // gRPC & Protobuf
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-stub:$grpcVersion")
    api("com.google.protobuf:protobuf-java:$protobufVersion")

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("jakarta.annotation:jakarta.annotation-api:2.1.1")

    // Logging
    api("org.slf4j:slf4j-api:$slf4jVersion")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc")
            }
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.withType<Javadoc> {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
