import com.android.build.api.dsl.ApplicationExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.protobuf) apply false
}

val mozc by extra(
    file("third_party/mozc/src/version.bzl").readText().let { bzl ->
        listOf("MAJOR", "MINOR", "BUILD_OSS").map { Regex("""^$it = (\d+)""", RegexOption.MULTILINE).find(bzl)!!.groupValues[1].toInt() }
    },
)

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            val release = providers.environmentVariable("KEYSTORE").orNull?.let { keystore ->
                signingConfigs.create("release") {
                    storeFile = file(keystore)
                    storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").get()
                    keyAlias = providers.environmentVariable("KEY_ALIAS").get()
                    keyPassword = providers.environmentVariable("KEY_PASSWORD").get()
                }
            }
            buildTypes.getByName("release").signingConfig = release
        }
    }
}
