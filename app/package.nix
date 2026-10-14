{
  lib,
  stdenv,
  androidenv,
  gradle_9,
  jdk21,
  protobuf_36,
  mozc,
  src,
  describe ? "unknown",
}:
let
  buildTools = "37.0.0";
  sdk = "${
    (androidenv.composeAndroidPackages {
      platformVersions = [ "37.0" ];
      buildToolsVersions = [ buildTools ];
      includeEmulator = false;
      includeSystemImages = false;
      includeSources = false;
    }).androidsdk
  }/libexec/android-sdk";
in
stdenv.mkDerivation (finalAttrs: {
  pname = "henkandx";
  version = describe;
  inherit src;

  nativeBuildInputs = [
    gradle_9
    jdk21
  ];

  mitmCache = gradle_9.fetchDeps {
    pkg = finalAttrs.finalPackage;
    data = ./deps.json;
  };

  env = {
    ANDROID_HOME = sdk;
    JAVA_HOME = jdk21.home;
  };

  gradleFlags = [
    "--no-configuration-cache"
    "-Dorg.gradle.java.home=${jdk21.home}"
    "-Pandroid.aapt2FromMavenOverride=${sdk}/build-tools/${buildTools}/aapt2"
    "-Pprotoc=${lib.getExe' protobuf_36 "protoc"}"
    "-Pdescribe=${describe}"
  ];
  gradleBuildTask = "assembleRelease";
  gradleUpdateTask = finalAttrs.gradleBuildTask;

  preBuild = ''
    mkdir -p engine/mozc/out
    cp -rL --no-preserve=mode ${mozc}/. engine/mozc/out/
  '';

  installPhase = ''
    install -Dm644 -t $out app/build/outputs/apk/release/*.apk
  '';
})
