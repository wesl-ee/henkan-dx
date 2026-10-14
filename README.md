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

## Keys

- Right shift switches between あ and A (Ctrl+Space on those without right shift)
- Space converts and steps through candidates
- Shift+Space will convert clause by clause
- Enter commits text
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
