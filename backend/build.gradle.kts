plugins {
    java
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.epages.restdocs-api-spec") version "0.19.4"
}

group = "com.drafturl"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Database
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // AWS SDK for S3 (R2/MinIO compatible)
    implementation(platform("software.amazon.awssdk:bom:2.31.1"))
    implementation("software.amazon.awssdk:s3")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    // HTML sanitizer
    implementation("org.jsoup:jsoup:1.18.3")

    // Logging (JSON structured logging for production)
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Sentry (error tracking)
    implementation("io.sentry:sentry-spring-boot-starter-jakarta:7.19.1")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("com.epages:restdocs-api-spec-mockmvc:0.19.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

openapi3 {
    setServer("http://localhost:8080")
    title = "DraftURL API"
    description = "DraftURL 백엔드 REST API 문서"
    version = "1.0.0"
    format = "yaml"
    outputDirectory = "build/api-spec"
    outputFileNamePrefix = "openapi"
}

tasks.register<Copy>("copyOpenApiSpec") {
    dependsOn("openapi3")
    from("build/api-spec/openapi.yaml")
    into("src/main/resources/static/docs")
}
