plugins {
  `java-gradle-plugin`
  id("com.gradle.plugin-publish")
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
}

dependencies {
  implementation(kotlin("gradle-plugin-api"))
}

buildConfig {
  val project = project(":nullabledataclasses-plugin")
  packageName(project.group.toString())
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"${rootProject.extra["kotlin_plugin_id"]}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_GROUP", "\"${project.group}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_NAME", "\"${project.name}\"")
  buildConfigField("String", "KOTLIN_PLUGIN_VERSION", "\"${project.version}\"")

  val annotationProject = project(":nullabledataclasses-annotation")
  buildConfigField("String", "ANNOTATION_LIBRARY_GROUP", "\"${annotationProject.group}\"")
  buildConfigField("String", "ANNOTATION_LIBRARY_NAME", "\"${annotationProject.name}\"")
  buildConfigField("String", "ANNOTATION_LIBRARY_VERSION", "\"${annotationProject.version}\"")
}

gradlePlugin {
  plugins {
    create("kotlinIrPluginTemplate") {
      id = rootProject.extra["kotlin_plugin_id"] as String
      displayName = "Nullable Data Classes Plugin"
      description = "Nullable Data Classes Plugin"
      implementationClass = "minerofmillions.nullabledataclasses.NDCGradlePlugin"
    }
  }
}
