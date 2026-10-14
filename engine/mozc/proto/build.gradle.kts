import com.google.protobuf.gradle.proto

plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

val protocol = tasks.register<Sync>("protocol") {
    from("../../../third_party/mozc/src/protocol") { include("*.proto") }
    into(layout.buildDirectory.dir("mozc/protocol"))
}

sourceSets.main {
    proto { srcDir(files(layout.buildDirectory.dir("mozc")).builtBy(protocol)) }
}

protobuf {
    protoc {
        val local = providers.gradleProperty("protoc").orNull
        if (local != null) path = local else artifact = libs.protoc.get().toString()
    }
    generateProtoTasks {
        all().configureEach {
            builtins { named("java") { option("lite") } }
        }
    }
}

dependencies {
    api(libs.protobuf.javalite)
}
