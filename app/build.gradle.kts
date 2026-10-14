import com.android.build.api.variant.FilterConfiguration.FilterType

plugins {
    alias(libs.plugins.android.application)
}

val describe = providers.gradleProperty("describe").orElse(
    providers.exec {
        commandLine("git", "describe", "--tags", "--long", "--always", "--match", "v[0-9]*")
        isIgnoreExitValue = true
    }.standardOutput.asText,
).get().trim()

val tag = Regex("""v(\d+)\.(\d+)\.(\d+)-(\d+)-g(\w+)""").matchEntire(describe)?.groupValues?.drop(1)
val (x, y, z) = tag?.take(3)?.map(String::toInt) ?: listOf(0, 0, 1)
val release = tag?.get(3) == "0"
val sha = tag?.get(4) ?: describe.ifEmpty { "unknown" }
val semver = "$x.$y.$z" + if (release) "" else "-dev+$sha"
val code = (x * 10000 + y * 100 + z) * 10
val abis = listOf("arm64-v8a", "x86_64")

android {
    namespace = "ee.wesl.henkandx"
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "ee.wesl.henkandx"
        minSdk = 26
        targetSdk = 37
        versionName = semver
        versionCode = code
    }

    signingConfigs {
        providers.environmentVariable("KEYSTORE").orNull?.let { keystore ->
            create("release") {
                storeFile = file(keystore)
                storePassword = providers.environmentVariable("KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    splits.abi {
        isEnable = true
        reset()
        include(*abis.toTypedArray())
        isUniversalApk = true
    }

    sourceSets {
        getByName("main") {
            assets.directories += "../profiles"
        }
    }

    buildFeatures.buildConfig = true
}

androidComponents.onVariants { variant ->
    variant.outputs.forEach { output ->
        val abi = output.filters.find { it.filterType == FilterType.ABI }?.identifier
        output.versionCode.set(code + abis.indexOf(abi) + 1)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":engine:mozc"))
}
