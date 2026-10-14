{
  lib,
  path,
  callPackage,
  fetchFromGitHub,
  runCommand,
  symlinkJoin,
  bazel_9,
  androidenv,
  libgcc,
  libxcrypt-legacy,
  python3,
  stdenv,
  zlib,
  abis ? [
    "arm64-v8a"
    "x86_64"
  ],
}:
let
  bazelPackage = callPackage "${path}/pkgs/by-name/ba/bazel_9/build-support/bazelPackage.nix" { };
  ndkVersion = "29.0.14206865";
  registry = fetchFromGitHub {
    owner = "bazelbuild";
    repo = "bazel-central-registry";
    rev = "b2d0812449df9100116d0e43463a1e137e771e0f";
    hash = "sha256-vmfmPn5U5ER1ykC2BJCZ2WSe9Tff0/9JwNRbLW7JoOI=";
  };
  ndk =
    (androidenv.composeAndroidPackages {
      includeNDK = true;
      ndkVersions = [ ndkVersion ];
      platformVersions = [ ];
      buildToolsVersions = [ ];
      includeEmulator = false;
    }).ndk-bundle;

  src = runCommand "mozc" { } ''
    cp -r ${../../third_party/mozc} $out
    chmod -R u+w $out
    mkdir -p $out/src/third_party/ndk
    ln -s ${ndk}/libexec/android-sdk/ndk/${ndkVersion} $out/src/third_party/ndk/android-ndk-r29
    mkdir $out/src/nix
    cat > $out/src/nix/BUILD.bazel <<EOF
    load("@rules_python//python:py_runtime.bzl", "py_runtime")
    load("@rules_python//python:py_runtime_pair.bzl", "py_runtime_pair")
    py_runtime(
        name = "python3",
        interpreter_path = "${lib.getExe python3}",
        python_version = "PY3",
        stub_shebang = "#!${lib.getExe python3}",
    )
    py_runtime_pair(name = "runtime", py3_runtime = ":python3")
    toolchain(
        name = "python",
        toolchain = ":runtime",
        toolchain_type = "@rules_python//python:toolchain_type",
    )
    EOF
    echo 'register_toolchains("//nix:python")' >> $out/src/MODULE.bazel
  '';

  target = {
    arm64-v8a = "//android/jni:mozc.arm64";
    x86_64 = "//android/jni:mozc.x86_64";
    armeabi-v7a = "//android/jni:mozc.arm32";
  };

  build =
    {
      name,
      targets,
      commandArgs,
      hash,
      installPhase,
    }:
    bazelPackage {
      inherit
        name
        src
        registry
        targets
        installPhase
        ;
      sourceRoot = "mozc/src";
      bazel = bazel_9;
      commandArgs = commandArgs ++ [ "--jobs=8" ];
      env.USE_BAZEL_VERSION = bazel_9.version;
      bazelVendorDepsFOD = {
        outputHash = hash;
        outputHashAlgo = "sha256";
      };
      buildInputs = [
        libgcc
        libxcrypt-legacy
        stdenv.cc.cc.lib
        zlib
      ];
    };

  libs = build {
    name = "libmozc";
    targets = map (abi: target.${abi}) abis;
    commandArgs = [
      "--config"
      "oss_android"
      "--config"
      "release_build"
    ];
    hash = "sha256-Kh5trBrRopgJ619pwcP12kRvlHPzsDNL9/b0cGAOGbU=";
    installPhase = ''
      for so in bazel-out/*-opt-ST-*/bin/android/jni/libmozc.so; do
        abi=''${so#bazel-out/}
        install -Dm644 $so $out/jniLibs/''${abi%%-opt-ST-*}/libmozc.so
      done
    '';
  };

  data = build {
    name = "mozc-data";
    targets = [ "//data_manager/oss:mozc_dataset_for_oss" ];
    commandArgs = [
      "--config"
      "release_build"
    ];
    hash = "sha256-bj2sBF+H7d7MraYSTlpTW+cHNBamKCmaaQ/1O74Ftgs=";
    installPhase = ''
      install -Dm644 bazel-bin/data_manager/oss/mozc.data $out/assets/mozc.data
    '';
  };
in
symlinkJoin {
  name = "mozc-android";
  paths = [
    libs
    data
  ];
}
