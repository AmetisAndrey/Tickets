<<<<<<< HEAD
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.6"
    jacoco
}

group = "com.example"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.flywaydb:flyway-core:10.11.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.11.0")
    runtimeOnly("org.postgresql:postgresql")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.21.4")
    testImplementation("org.testcontainers:postgresql:1.21.4")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
}

// Ключевое изменение: НЕ настраивайте вручную dependsOn, пусть плагин обрабатывает смешанную компиляцию
// Но убедитесь, что каталоги исходников правильные
sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/java"))
        }
        kotlin {
            setSrcDirs(listOf("src/main/kotlin", "src/main/java"))
        }
    }
    test {
        java {
            setSrcDirs(listOf("src/test/java"))
        }
        kotlin {
            setSrcDirs(listOf("src/test/kotlin", "src/test/java"))
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "test")
    finalizedBy(tasks.jacocoTestReport)
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
=======
plugins {
	java
	war
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"

    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
}

group = "com.example"
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
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
	providedRuntime("org.springframework.boot:spring-boot-starter-tomcat-runtime")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}
>>>>>>> 5857b2bcbe0958a9015b8b31dbbb0b3756c7d81c
