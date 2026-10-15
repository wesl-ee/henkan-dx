plugins {
    alias(libs.plugins.android.application)
}

val mozc: List<Int> by rootProject.extra

android {
    namespace = "ee.wesl.henkandx.ja"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "ee.wesl.henkandx.ja"
        minSdk = 26
        targetSdk = 37
        versionName = mozc.joinToString(".")
        versionCode = mozc.last()
    }

    sourceSets {
        getByName("main") {
            assets.directories += "../../engine/mozc/out/assets"
        }
    }
}

androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) {
    it.applicationId.set("ee.wesl.henkandx.debug.ja")
}
