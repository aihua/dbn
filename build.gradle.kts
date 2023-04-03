plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.7.20"
  id("org.jetbrains.intellij") version "1.13.2"
}

group = "com.dci"
version = "3.3.6771.0"

repositories {
  mavenCentral()
}
dependencies {
  annotationProcessor("org.projectlombok:lombok:1.18.26")

  implementation("org.projectlombok:lombok:1.18.26")
  implementation("org.dom4j:dom4j:2.1.4")
  implementation("org.apache.poi:poi:5.2.3")
  implementation("org.apache.poi:poi-ooxml:5.2.3")
  implementation("org.apache.poi:poi-ooxml-schemas:4.1.2")
  implementation("com.jcraft:jsch:0.1.55")
  implementation("com.oracle.database.jdbc:ojdbc8:21.9.0.0")
  implementation("com.oracle.database.xml:xdb:21.9.0.0")
  implementation("com.oracle.database.xml:xmlparserv2:21.9.0.0")

}

sourceSets{
  main {
    resources {
      srcDir("src/main/java")
      include("**/*.xml")
    }
    resources{
      srcDir("lib/ext/")
      include("**/*.jar")
    }
    resources {
      include(
              "**/*.png",
              "**/*.jpg",
              "**/*.xml",
              "**/*.css",
              "**/*.html",
              "**/*.template")
    }
  }

}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2022.3.1")
  type.set("IC") // Target IDE Platform

  plugins.set(listOf("java"))

}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "11"
    targetCompatibility = "11"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
  }

  patchPluginXml {
    sinceBuild.set("222")
    untilBuild.set("232.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin {
    token.set(System.getenv("PUBLISH_TOKEN"))
  }
}
