plugins {
  kotlin("jvm")
  `maven-publish`
}

repositories {
  mavenCentral()
}

dependencies {
  testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
  useJUnitPlatform()
}
kotlin {
  jvmToolchain(17)
}

val jar by tasks

publishing {
  publications {
    create<MavenPublication>("annotation") {
      this.artifact(jar)
    }
  }
}
