import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

val pluginName = "VTAndroidCore"

val pluginPackageName = "com.vectortouch.plugin"

android {
    namespace = pluginPackageName
    compileSdk = 35

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        minSdk = 24
        manifestPlaceholders["godotPluginName"] = pluginName
        manifestPlaceholders["godotPluginPackageName"] = pluginPackageName
        buildConfigField("String", "GODOT_PLUGIN_NAME", "\"${pluginName}\"")
        setProperty("archivesBaseName", pluginName)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    compileOnly(files("lib/godot-lib.aar"))
}

// BUILD TASKS DEFINITION
val copyDebugAARToAddons by tasks.registering(Copy::class) {
    description = "Copies the generated debug AAR binary to the addons directory"
    from("build/outputs/aar")
    include("$pluginName-debug.aar")
    into("../addons/$pluginName/bin/debug")
}

val copyReleaseAARToAddons by tasks.registering(Copy::class) {
    description = "Copies the generated release AAR binary to the addons directory"
    from("build/outputs/aar")
    include("$pluginName-release.aar")
    into("../addons/$pluginName/bin/release")
}

val cleanAddons by tasks.registering(Delete::class) {
    delete("../addons/$pluginName")
}

val copyPluginToAddons by tasks.registering(Copy::class) {
    description = "Copies the export scripts to the addons directory"

    dependsOn(cleanAddons)
    finalizedBy(copyDebugAARToAddons)
    finalizedBy(copyReleaseAARToAddons)

    from("export_scripts")
    into("../addons/$pluginName")
}

tasks.named("assemble").configure {
    finalizedBy(copyPluginToAddons)
}

tasks.named<Delete>("clean").apply {
    dependsOn(cleanAddons)
}
