---
type: policy
title: ローカル開発環境と Sample の実行
description: iOS・Android・MAUI のローカル環境設定、Sample の起動、本体モジュールのビルド / lint コマンド、本体 source へのステップイン手順
tags: [development, setup, sample, ios, android, maui]
timestamp: 2026-08-29
---

# ローカル開発環境と Sample の実行

この文書は、リポジトリを clone した開発者が iOS・Android・MAUI の Sample を開いて実行し、本体モジュールをビルド / lint し、本体 source へデバッガでステップインするまでの手順をまとめる。読むと、Android SDK を二つの Gradle build root から解決する理由と、複数の Xcode を使う環境で選択を固定する方法も分かる。

[リポジトリとビルドの責務境界](../architecture/repository-boundaries.md) を先に読むと、platform ごとに独立した build root を持つ理由が分かりやすい。

## 必要環境

| 対象 | 必要な環境 |
|---|---|
| iOS Native | macOS、Xcode 16 以上、Swift 6、iOS 16 以上の Simulator または実機 |
| Android Native | macOS / Linux / Windows、JDK 17、Android SDK Platform 35 + Build-Tools 35.0.0 以上、API 29 以上の Emulator または実機。Android Studio は Hedgehog (2023.1.1) 以上を推奨 |
| MAUI | .NET 10 SDK と MAUI workload。iOS target には macOS・Xcode・iOS 16 以上の Simulator、Android target には Android SDK と API 29 以上の Emulator または実機 |

固定されている版の正は次のとおり。手元の版が要件に合うか調べるときはここを見る。

| 対象 | 定義元 |
|---|---|
| .NET SDK | `global.json` |
| AGP・Kotlin・Compose 等 | `android/gradle/libs.versions.toml` |
| Gradle | `android/gradle/wrapper/gradle-wrapper.properties` |
| 各 application / library の target | `Package.swift`・Xcode project・csproj の宣言 |

`.NET MAUI` workload が未導入なら、使用する SDK を確認したうえで `dotnet workload install maui` を実行する。

## Android SDK ロケーション

MAUI の `dotnet build` は Android SDK を自身で解決できるため、本節は Android Native のビルドと Sample が対象である。MAUI Sample で `ANDROID_HOME` が要るのは `emulator` / `adb` を CLI から直接呼ぶ場合に限る。

Android Native Sample は `samples/android/` を root build、`android/` を included build とする Gradle composite build である。Android Gradle Plugin は各 build root の `local.properties` を独立して解決するため、SDK を両方から見える状態にする。

### ANDROID_HOME を使う

`ANDROID_HOME` を設定すると両 build root を一度に解決できる。

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/emulator"
```

SDK の場所は環境に合わせて変更する。Windows では同名のシステム環境変数を設定する。

### local.properties を使う

環境変数を使わない場合は、次の 2 ファイルへ同じ `sdk.dir` を置く。

- `samples/android/local.properties`
- `android/local.properties`

```properties
sdk.dir=<Android SDK の絶対パス>
```

Android Studio が自動生成するのは開いた root 側だけで、included build の `android/local.properties` は生成しない。`SDK location not found` が `android/local.properties` を指す場合、Sample 側だけにファイルを置いても解消しない。`ANDROID_HOME` または両方の `local.properties` を設定した後、Gradle sync を再実行する。

## Xcode の選択

複数の Xcode を併用し、`.NET for iOS` が要求する版と `xcode-select` の選択が異なる場合は `DEVELOPER_DIR` を指定する。

```bash
export DEVELOPER_DIR=<使用する Xcode.app の Developer ディレクトリ>
```

要求される版は `.NET for iOS` のエラーに表示される。パスは環境ごとに異なるので、自分の環境で実在する Xcode の Developer ディレクトリを指定する。

## Sample を開く

### iOS

```bash
open samples/ios/KsSettingsViewSample.xcodeproj
```

Xcode の `File > Open...` から同じ project を選んでもよい。初回は Local Swift Package `ios/` の解決完了を待つ。

### Android

Android Studio の `File > Open...` で `samples/android/` を開く。初回の Gradle sync では composite build の本体 module と依存を解決する。`ANDROID_HOME` を使わない場合は、開く前に前節の二つの `local.properties` を用意する。

### MAUI

Visual Studio / Rider では `maui/KsSettingsView.slnx` を開く。Sample 単体なら `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj` を開く。

```bash
open maui/KsSettingsView.slnx
```

## Sample を実行する

### iOS Native

Xcode で `KsSettingsViewSample` scheme と iOS 16 以上の Simulator / 実機を選び、Run する。CLI で build だけを確認する場合は次を使う。

```bash
xcodebuild \
  -project samples/ios/KsSettingsViewSample.xcodeproj \
  -scheme KsSettingsViewSample \
  -destination 'generic/platform=iOS Simulator' \
  build
```

### Android Native

Android Studio で `app` module と API 29 以上の Emulator / 実機を選び、Run する。CLI では次の手順で build、install、起動を行う。

```bash
cd samples/android
./gradlew :app:assembleDebug
./gradlew :app:installDebug
adb shell am start -n jp.kamusoft.kssettingsview.samples.android/.MainActivity
```

### MAUI iOS

利用可能な Simulator の UDID を `xcrun simctl list devices available` で確認し、起動してから実行する。**接続先は UDID で明示する** — `booted` 指定は起動中の Simulator が 2 台以上あると宛先が定まらない。

```bash
xcrun simctl boot <simulator-udid>
open -a Simulator
```

```bash
dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj \
  -f net10.0-ios -c Debug

xcrun simctl install <simulator-udid> \
  samples/maui/KsSettingsView.Sample.Maui/bin/Debug/net10.0-ios/iossimulator-arm64/KsSettingsView.Sample.Maui.app

xcrun simctl launch <simulator-udid> jp.kamusoft.kssettingsview.samples.maui
```

### MAUI Android

Emulator を起動してから実行する。AVD 名は `emulator -list-avds` で確認する。

```bash
"$ANDROID_HOME/emulator/emulator" -list-avds
"$ANDROID_HOME/emulator/emulator" -avd <AVD 名> &
```

```bash
dotnet build samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj \
  -f net10.0-android -c Debug -t:Run -p:AdbTarget="-s emulator-5554"
```

接続先が 1 台だけなら `AdbTarget` は省略できる。`adb devices` で対象を確認する。

## 本体をビルドする

Sample ではなく本体モジュールだけをビルド・静的解析したいときに使う。テストの実行方法と完了判定は [テスト実行規約](test-execution.md) が正であり、本節はビルドのみを扱う。

### iOS

```bash
cd ios
swift package describe          # パッケージ構成を表示
swift build                     # ビルド
```

### Android

```bash
cd android
./gradlew tasks                 # 利用可能タスクを表示
./gradlew build                 # 全モジュールをビルド
./gradlew lint                  # Android Lint
```

個別モジュールだけを組むときは module を指定する:

```bash
cd android
./gradlew :ks-settingsview-core:assembleDebug
./gradlew :ks-settingsview-ui:assembleDebug
./gradlew :ks-settingsview-compose:assembleDebug
./gradlew :ks-settingsview-bridge:assembleDebug
```

### MAUI

```bash
cd maui
dotnet sln KsSettingsView.slnx list                            # ソリューション内容を表示
dotnet build KsSettingsView.Maui/KsSettingsView.Maui.csproj    # facade 層をビルド
```

binding のビルドは Native 側のビルドを自動で呼ぶ。その経路と既知の制約は [MAUI binding の Native artifact 統合](../../maui/architecture/binding-build-integration.md) を参照する。

## 本体 source へステップインする

### iOS

iOS Sample は `ios/Package.swift` を Local Swift Package として参照する。Sample project を Xcode で Run し、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の source に breakpoint を置くと直接ステップインできる。本体 test を中心に調べる場合は `ios/Package.swift` を Xcode で開く。正しい全テスト実行方法は [テスト実行規約](test-execution.md) を参照する。

### Android

Android Sample は `includeBuild("../../android")` で本体を source 参照する。`samples/android/` を Android Studio で Run し、Core・UI・Compose module の source に breakpoint を置くとステップインできる。本体 test を中心に調べる場合は `android/` を別の project として開く。

### MAUI

MAUI Sample は `samples/maui/KsSettingsView.Sample.Maui/KsSettingsView.Sample.Maui.csproj` から `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj` を `ProjectReference` する。IDE で Sample を Debug 実行し、facade の C# source に breakpoint を置くとステップインできる。本書が保証するステップイン範囲は facade の C# source までであり、binding assembly から Native Bridge へ入る platform debugger の設定は対象外とする。

facade の純ロジック test の実行方法と、そのテストが触らない範囲は [テスト実行規約](test-execution.md) の MAUI 節が正。

## デモ画面一覧はどこを見るか

画面の集合・表示名・遷移先は、各 Sample の `SampleScreen` 実装が正である。一覧を書き写した資料は増減に追随しないので、次のファイルを直接見る。

| platform | 定義元 |
|---|---|
| iOS | `samples/ios/KsSettingsViewSample/SampleScreen.swift` |
| Android | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/SampleScreen.kt` |
| MAUI | `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs` |

platform 間で揃える範囲と例外は [Sample のプラットフォーム間一致](sample-parity.md) を参照する。

## 関連

- [リポジトリとビルドの責務境界](../architecture/repository-boundaries.md)
- [テスト実行規約](test-execution.md)
- [Sample のプラットフォーム間一致](sample-parity.md)
- [実行時挙動の検証規約](runtime-behavior-verification.md)
