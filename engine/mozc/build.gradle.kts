plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "ee.wesl.henkandx.engine.mozc"
    compileSdk = 37
    buildToolsVersion = "37.0.0"
    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }
    sourceSets {
        getByName("main") {
            jniLibs.directories += "out/jniLibs"
            assets.directories += "out/assets"
        }
    }
}

dependencies {
    api(project(":engine:mozc:proto"))
    api(project(":core"))
}
