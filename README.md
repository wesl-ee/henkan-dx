# Henkan DX (変換DX)

CJK input for physical keyboards on Android phones

<p align="center">
  <img src="screenshot.jpeg" alt="Henkan DX on a Titan 2 typing English and Japanese in a Google Keep note." width="45%" hspace="8">
  <img src="photo.jpeg" alt="An orange Titan 2 typing 超芸術トマソン with Henkan DX candidates on the bar." width="45%" hspace="8">
</p>

Status
------

I use a Unihertz Titan Elite 2 and have access to a Titan Pocket so support for
these phones is first-class. Everything else is best-effort. Software is
pre-release.

## Installing

1. Download app-arm64-v8a-release.apk from the
   [latest release](https://github.com/wesl-ee/henkan-dx/releases) (Android
   8.0 or newer). Check it against SHA256SUMS if you like
2. Open the APK and allow your browser or file manager to install apps
3. Open Henkan DX, tap Open Keyboard Settings and enable Henkan DX
4. Pick Henkan DX as your keyboard from the switcher in the navigation bar or
   notification shade

## Keys

- Right shift switches between あ and A (Ctrl+Space on those without right shift)
- Space picks from the suggestions on the bar, or converts and steps through candidates
- Shift+Space will convert clause by clause
- Enter commits text
- Shift, Alt and Fn are sticky: tap once for the next key, twice quickly to lock
- The number keys on the Alt layer, or a tap on the bar, pick a candidate
- Fn with E, S, D and F are arrow keys
- While converting, Fn+S and Fn+F (left + right) move between clauses
- Shorten or lengthen clause by pressing Shift+S or Shift+F
- Fn+Q cancels the conversion.
- Sym opens the symbol page
- Press Fn+B to add the word you are typing to your user dictionary

## Privacy

- Zero android permissions and zero network access
- Switches to direct input in password and other sensitive inputs
- Does not back up or transfer learning data.
- Learned words can be erased any time

## Tech

- Japanese conversion is Mozc
- Mozc runs with its desktop MS-IME style keymap
- Kotlin with plain Android Views and no AndroidX

## Building

Mozc is a git submodule so fetch it after cloning.

```sh
git submodule update --init --depth 1
```

with Nix, nix build produces unsigned release APKs in result/.

```sh
nix build
```

nix develop opens a shell with the JDK and the Android SDK.

```sh
nix develop
```

Without Nix, engine/mozc/build.sh builds libmozc.so and mozc.data (it needs bazelisk, Python 3 and a C++ compiler) and then Gradle builds the APK

```sh
engine/mozc/build.sh
./gradlew assembleDebug
```

On NixOS run the Mozc build script inside the dev shell's FHS environment

```sh
nix develop -c fhs -c engine/mozc/build.sh
```

## License

MIT License (available in the source tree as /LICENSE)

Mozc is BSD-3-Clause licensed (see NOTICE)
