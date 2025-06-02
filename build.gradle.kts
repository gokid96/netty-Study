plugins {
    id("java")
    id("application")
}

group = "com.demo"
version = "1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    // Netty 의존성 (보안 취약점 해결을 위한 최신 버전)
    implementation("io.netty:netty-all:4.1.121.Final")

    // 로깅 (선택사항)
    implementation("org.slf4j:slf4j-simple:1.7.36")
}

tasks.test {
    useJUnitPlatform()
}

// 서버 실행 태스크
tasks.register<JavaExec>("runServer") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.demo.server.EchoServer")
    args = listOf("9090") // 포트 번호 인자
}

// 클라이언트 실행 태스크
tasks.register<JavaExec>("runClient") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.demo.client.EchoClient")
    args = listOf("localhost", "9090") // 호스트, 포트 인자
    standardInput = System.`in`
}