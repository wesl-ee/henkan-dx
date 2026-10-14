#!/usr/bin/env bash
set -euo pipefail

here=$(cd "$(dirname "$0")" && pwd)
out=$here/out
cd "$here/../../third_party/mozc/src"
unset ANDROID_HOME ANDROID_SDK_ROOT

declare -A target=(
  [arm64-v8a]=//android/jni:mozc.arm64
  [x86_64]=//android/jni:mozc.x86_64
  [armeabi-v7a]=//android/jni:mozc.arm32
)
read -ra abis <<<"${ABIS:-arm64-v8a x86_64}"
android=(--config oss_android --config release_build)
data=//data_manager/oss:mozc_dataset_for_oss

[[ -d third_party/ndk ]] ||
  python3 build_tools/update_deps.py --noninja --noqt --nollvm --nomsys2 --nowix

targets=()
for abi in "${abis[@]}"; do targets+=("${target[$abi]}"); done
bazelisk build "${android[@]}" "${targets[@]}"
bazelisk build --config release_build "$data"

rm -rf "$out"
mkdir -p "$out/assets"
for abi in "${abis[@]}"; do
  mkdir -p "$out/jniLibs/$abi"
  so=$(bazelisk cquery "${android[@]}" --output=files "${target[$abi]}" 2>/dev/null)
  install -m644 "$so" "$out/jniLibs/$abi/libmozc.so"
done
install -m644 "$(bazelisk cquery --config release_build --output=files "$data" 2>/dev/null | grep 'mozc\.data$')" "$out/assets/mozc.data"
