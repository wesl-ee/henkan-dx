{
  inputs = {
    self.submodules = true;
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs =
    { self, nixpkgs }:
    let
      forAll =
        f:
        nixpkgs.lib.genAttrs [ "x86_64-linux" "aarch64-darwin" ] (
          system:
          f (
            import nixpkgs {
              inherit system;
              config = {
                allowUnfree = true;
                android_sdk.accept_license = true;
              };
            }
          )
        );
    in
    {
      packages = forAll (pkgs: rec {
        mozc = pkgs.callPackage ./engine/mozc/package.nix { };
        apk = pkgs.callPackage ./app/package.nix {
          inherit mozc;
          src = self;
          describe = self.shortRev or self.dirtyShortRev or "unknown";
        };
        default = apk;
      });

      devShells = forAll (
        pkgs:
        let
          buildTools = "37.0.0";
          android = pkgs.androidenv.composeAndroidPackages {
            platformVersions = [ "37.0" ];
            buildToolsVersions = [ buildTools ];
            includeEmulator = false;
            includeSystemImages = false;
            includeSources = false;
          };
          sdk = "${android.androidsdk}/libexec/android-sdk";
          fhs = pkgs.buildFHSEnv {
            name = "fhs";
            targetPkgs =
              p: with p; [
                bazelisk
                bash
                coreutils
                findutils
                gnugrep
                gnused
                gawk
                gcc
                git
                python3
                unzip
                which
                zip
                zlib
              ];
            runScript = "bash";
          };
        in
        {
          default = pkgs.mkShell {
            packages = with pkgs; [
              jdk21
              android.androidsdk
              fhs
            ];
            ANDROID_HOME = sdk;
            ANDROID_SDK_ROOT = sdk;
            JAVA_HOME = pkgs.jdk21.home;
            GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${sdk}/build-tools/${buildTools}/aapt2";
          };
        }
      );
    };
}
