plugins {
    alias(libs.plugins.android.library)
}

val mozc: List<Int> by rootProject.extra

android {
    namespace = "ee.wesl.henkandx.engine.mozc"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
        buildConfigField("String", "MOZC", "\"${mozc.joinToString(".")}\"")
    }
    buildFeatures.buildConfig = true
    sourceSets {
        getByName("main") {
            jniLibs.directories += "out/jniLibs"
        }
    }
}

dependencies {
    api(project(":engine:mozc:proto"))
    api(project(":core"))
}
