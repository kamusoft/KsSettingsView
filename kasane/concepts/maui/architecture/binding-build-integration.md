---
type: concept
title: MAUI binding の Native artifact 統合
description: MAUI binding が iOS xcframework と Android aar を生成・取り込みする構成、既知の制約、SDK 更新時の再検証箇所、NuGet 3 パッケージの pack 構成と利用者ビルドへ同梱する最低 OS 版ガード
tags: [maui, binding, build, interop, nuget]
timestamp: 2026-09-02
---

# MAUI binding の Native artifact 統合

この文書は、MAUI binding のビルドが Native artifact を生成して binding assembly へ取り込む経路と、その経路が依存する SDK 内部拡張点、そして binding 2 件と facade を NuGet パッケージにする構成を説明する。読むと、iOS と Android で SDK 標準アイテムの採否が異なる理由、警告を抑止していない理由、pack で共通設定をどこに置きどの SDK 挙動を受け入れているか、利用者のビルドへ同梱する最低 OS 版チェックが何をするか、workload や SDK を更新したときに再検証する箇所が分かる。

## 責務境界

binding assembly は interop の輸送層であり、MAUI アプリ利用者向けの公開契約ではない。アプリ向けの型は facade `KsSettingsView.Maui` が提供し、binding は Native Bridge を C# から呼べる形へ束縛する。interop の API 契約は [Native Bridge の interop 境界](../api/native-bridge.md) を参照する。

```text
iOS Swift source ───── XcodeProject ── xcframework ── Binding.iOS ────┐
                                                                     ├── KsSettingsView.Maui (facade)
Android Kotlin source ── gradlew ────── aar ───────── Binding.Android ┘
```

facade は両 platform の binding を参照する。以降の節は、この 2 本の経路それぞれで .NET SDK のどこに割り込んでいるかを扱う。

## Native artifact の生成

binding プロジェクトのビルドが Native 側のビルドを呼ぶため、事前に artifact を生成する必要はない。

| platform | 生成経路 | binding への取り込み |
|---|---|---|
| iOS | `maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj` の `XcodeProject` が `ios/binding/KsSettingsViewBridge.xcodeproj` を archive し、`obj/<構成>/net10.0-ios/xcode/` 配下へ xcframework を生成する | `CreateNativeReference=false` とし、生成物を `_RegisterXcodeProjectNativeReference` で `NativeReference` へ登録する |
| Android | `maui/android/KsSettingsView.Binding.Android/KsSettingsView.Binding.Android.csproj` が `android/gradlew` を呼び、Core・UI・Bridge の release aar を生成する | Bridge aar を束縛し、Core・UI aar は実行時依存として同梱する。Compose module は束縛しない |

`ios/binding/build-xcframework.sh` は binding のビルドから呼ばれない。.NET を介さず Swift 側だけを確認するときの単体スクリプトで、生成物は `ios/binding/build/` に閉じ、binding の増分ビルド入力から除外する。

## SDK 標準アイテムの採否

### iOS

iOS は .NET SDK の `XcodeProject` アイテムを使う。ただし SDK による `NativeReference` の自動登録 (`CreateNativeReference`) は無効にし、生成された xcframework を binding csproj 側で手動登録する。SDK の自動登録が MSBuild 18 のメタデータ自己参照バグ (`MSB4120`) を踏んで壊れるためで、登録は SDK が xcframework を作った後・SDK が `NativeReference` を整理する前に走らせる必要がある。

以降、**アンダースコアで始まる名前は所有者が 2 種類ある**ので区別して読む。

| 名前 | 所有者 | 役割 |
|---|---|---|
| `_BuildXcodeProjects` / `_SanitizeNativeReferences` / `_CategorizeAndroidLibraries` / `_ResolveLibraryProjectImports` / `_GetBuildXcodeProjectsInputs` / `_XcbInputs` | .NET SDK (互換保証なし) | 割り込み先。SDK 更新で名前も挙動も変わり得る |
| `_RegisterXcodeProjectNativeReference` / `_AdjustKsBridgeXcodeProjectInputs` | binding csproj (自作) | 上の拡張点へ割り込む側 |

Xcode project の target 名と SwiftPM package の target 名は同じである。**同名であるため自動生成 scheme ではどちらが archive されるか定まらない**。そこで共有 scheme `ios/binding/KsSettingsViewBridge.xcodeproj/xcshareddata/xcschemes/KsSettingsViewBridge.xcscheme` が project target を明示する。この scheme を失うと framework が install されず、生成物が有効な framework にならない。

### Android

Android は SDK 標準の `AndroidGradleProject` を使わない。SDK が渡す init script は全 project の build directory を単一パスへ束ねるため、project 間依存を持つ複数 module 構成では Gradle の検証に失敗する。代わりに `gradlew` を直接実行し、生成された aar を `AndroidLibrary` として取り込む。

aar の増分ビルド入力には Core・UI・Bridge の source と各 `build.gradle.kts` に加え、version catalog、Gradle wrapper、`gradle.properties` を含める。build toolchain の版整合は [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md) を参照する。

## 既知の制約

### iOS の再生成と増分入力

binding の xcframework を作り直すときは binding project の `obj` を消す。SDK は `obj/<構成>/net10.0-ios/xcode/` の生成物を最新とみなして archive を省くため、消さないと Swift を直しても古い xcframework のままビルドが通る。単体スクリプトの生成物は `ios/binding/build/` にあり、binding の `obj` とは別に扱う。

SDK 既定の Xcode 入力検出は xcodeproj のディレクトリ配下しか走査しないが、target がビルドする Swift は `ios/Sources/` にある。`_AdjustKsBridgeXcodeProjectInputs` は `_XcbInputs` に Bridge・Core・UI の Swift と `Package.swift` を追加し、単体スクリプトの生成物を除く。SDK の item は正規化済みパスなので、`Remove` / `Include` も `NormalizeDirectory` / `NormalizePath` で表記を揃える。

### 生成される C# API の platform 差

Native artifact から C# 型を起こす段は、上の 2 経路 (xcframework / aar の生成) とは別に走る。この binding tool は OS ごとに異なるため、同じ Native の意味が異なる C# 表面として生成される。アプリ向け facade は binding 型を直接公開せず、内部の変換境界で次の差を吸収する。

| 対象 | 生成結果と扱い |
|---|---|
| `KsBridgeFont` | `PointSize` や `FamilyName` の未指定・解決失敗時は、iOS が具体的な system font へ解決し、Android は未指定のまま Native default へ委ねる。facade が必要な正規化を担う |
| DTO のプロパティ名 | 同じ意味でも iOS の `IsEnabled` / `IsVisible` と Android の `Enabled` / `Visible` のように生成名が異なる。facade 内部の変換が同じ意味へ揃える |
| Accessory の更新対象 | iOS は enum、Android は静的プロパティを持つ型として生成され、Android 側は nullable になる。共有 C# シナリオは Android の値を非 null と確認してから使う |

置換時の ID、Bridge の破棄、Host の取り付け前後における更新などの実行時制約は、build tool が作る差ではなく Bridge API 自体の契約である。[Native Bridge の interop 境界](../api/native-bridge.md) の ID と lifecycle の節を正とする。

### Android binding の警告

| 警告 | 意味と扱い |
|---|---|
| `BG8605` / `BG8606` | Kotlin の internal member が束縛対象から外れた通知。JVM 上の `$` を含む member は `java-resolution-report.log` で確認できる |
| `BG8A00` | `Transforms/Metadata.xml` の `remove-node` が generator 段では一致しない警告。SDK が Metadata を fixup 段と generator 段で適用し、対象は fixup 段で既に除去済みであるため発生する。entry を消すと内部 helper 型が公開面へ現れる |

いずれも現在の生成物は意図どおりであるため、警告を抑止しない。

### 生成型の namespace

生成される .NET の型は、iOS / Android のどちらの binding でも `KsSettingsView.Bridge` namespace に入る。Android は Java パッケージ名からの既定変換 (`Jp.Kamusoft.Kssettingsview.Bridge`) を `Transforms/Metadata.xml` の `managedName` 属性で上書きしてこれに揃えている。この上書きを外すと生成型の namespace が Java パッケージ由来へ戻り、[公開識別子の規約](../../../handbook/cross/public-identifiers.md) の .NET namespace 規則と iOS binding との対称性が同時に壊れる。

## NuGet パッケージ化

binding 2 件と facade はそれぞれ NuGet パッケージになり、利用者が参照するのは facade 1 件だけである ([maui/ADR-0025](../../../decisions/maui/0025-nuget-three-package-root-namespace.md))。pack は SDK 標準の `dotnet pack` を csproj 単位で実行するだけで、自作の pack 用 MSBuild は持たない (`maui/KsSettingsView.slnx` はエディタ用の入れ物で、ビルド・pack の単位ではない)。

| パッケージ | 中身 | nuspec の依存 |
|---|---|---|
| `KsSettingsView.Maui` (facade) | `net10.0` / `net10.0-ios` / `net10.0-android` の assembly と XML doc、package README (ルート `README.md`)、`buildTransitive/` の props / targets | 全 TFM 共通が `Microsoft.Maui.Controls` (PackageReference 由来)、platform TFM だけが対応する binding (TFM 条件付き `ProjectReference` が pack 時に TFM group 別の依存へ変換されたもの) |
| `KsSettingsView.Binding.iOS` | binding resource package (`resources.zip`) 内の xcframework (device / simulator 両スライス) | なし |
| `KsSettingsView.Binding.Android` | Gradle 由来の aar 2 本 (`kssettingsview` / `kssettingsview-bridge`) | Gradle 側が implementation 依存とするライブラリに対応する AndroidX / Kotlin 系。aar には同梱されないため依存として届ける (LiveData の版整合は [maui/ADR-0010](../../../decisions/maui/0010-androidx-conflict-absorbed-in-binding-layer.md)) |

3 件は同じ `-p:Version=` で pack する (facade の nuspec が binding を同版で参照するため)。順序は binding 2 件 → facade — facade の pack は `ProjectReference` 先の binding をビルドするが binding の nupkg は作らないので、消費者に配るフィードには binding を先に揃える。version は `Version` の既定値 `0.0.0-dev` を CI が `-p:Version=` で上書きする ([cross/ADR-0020](../../../decisions/cross/0020-release-dispatch-tag-last-version-injection.md))。binding の Description には「facade から推移的に参照される。直接参照しない」旨を英語で書き、`PackageTags` は facade にだけ付けて検索で binding が先に出ないようにしている。

### 共通設定の置き場と評価順序

`maui/` ビルドルート (`Directory.Build.props` / `Directory.Packages.props` が及ぶ範囲の起点。platform ごとに分ける決定は [cross/ADR-0001](../../../decisions/cross/0001-monorepo-platform-build-roots.md)) 直下の 3 ファイルが共通設定を持つ。`samples/maui` は別のビルドルートで、いずれの対象にもならない (利用者の csproj と同じ形 — `MauiVersion` と最低 OS 版の直書き — を保つため)。

| ファイル | 持つもの |
|---|---|
| `maui/Directory.Build.props` | NuGet メタデータ (Authors / License / URL / `PackageIcon`)、`Version` 既定値、`IsPackable` 既定 false (pack は facade と binding 2 件の csproj で opt-in)、SourceLink + snupkg、利用者へ同梱する `buildTransitive/KsSettingsView.Maui.props` (最低 OS 版の定数) をリポジトリ内ビルドでも読む import |
| `maui/Directory.Packages.props` | CPM (`ManagePackageVersionsCentrally`) の版一覧と、その版を選んだ理由 (MAUI 本体の下限、AndroidX の family 整合)。各 csproj は `Version` 属性を持たない |
| `maui/Directory.Build.targets` | `PackageIcon` の同梱アイテム (pack 対象だけ)、利用者へ同梱する `buildTransitive/KsSettingsView.Maui.targets` (下の「最低 OS 版のビルド時ガード」) の import |

`Directory.Build.props` はプロジェクト本体より先に読まれるため、csproj 本体で決まる `TargetFramework` や `IsPackable` をそこで参照できない。TFM に依存する代入 (`SupportedOSPlatformVersion`) は各 csproj の TFM 条件の中で行い、`IsPackable` に依存するアイテム (アイコンの同梱) は csproj 評価後に読まれる `Directory.Build.targets` に置く。この分担を崩すと条件が空振りし、ビルドは通るのに成果物から黙って抜ける。

### 最低 OS 版のビルド時ガード

facade パッケージは `buildTransitive/KsSettingsView.Maui.props` (要件の定数 `KsSettingsViewMinAndroidApi` = 29 / `KsSettingsViewMinIOSVersion` = 16.0) と `buildTransitive/KsSettingsView.Maui.targets` (検査ターゲット) を同梱し、利用者のビルドで `SupportedOSPlatformVersion` が要件未満なら MSBuild エラー `KSSV0001` で止める。要件の数値はこの props が単一の宣言元で、props が配るのは定数値だけ — `SupportedOSPlatformVersion` への代入自体はリポジトリ内の facade / binding / 検証ホストの各 csproj が TFM 条件の中で定数を参照して行う (前項の分担どおり)。数値の直書きは `samples/maui` の Sample だけで、値を変えるときは Sample も合わせる。iOS 16.0 は Bridge が依存する UI モジュール `KsSettingsViewUI` が使う `UIHostingConfiguration` (SwiftUI View を cell に載せる iOS 16 以降の API)、Android 29 は本体 Android モジュールの minSdk に由来する。

| 性質 | 内容 |
|---|---|
| 有効になる範囲 | 複数 TFM のプロジェクトは TFM ごとの内部ビルド (inner build) に分かれるが、働くのは `TargetPlatformIdentifier` が `android` / `ios` の内部ビルドだけ (`TargetFramework` が空の外側のビルドは対象外)。`buildTransitive/` は推移的な全消費者へ import されるため、素の `net10.0`・間接参照するライブラリの非 platform TFM でも何もしない |
| 未設定時 | .NET SDK が platform TFM に必ず既定値を与えるため空にはならない。Android の既定 21 は要件未満で止まり、iOS の既定 (26.x) は要件を満たすため発火しない |
| タイミング | `BeforeTargets="CoreCompile"`。Android では依存 AndroidX の manifest merger (`XAAMM0000`) より先に、要件と設定方法を書いた文面で止まる |
| 比較 | `[MSBuild]::VersionLessThan` で比較する。Android の `SupportedOSPlatformVersion` は SDK が `29` → `29.0` のように小数点付きへ揃えるため、iOS の `16.0` と同じ比較式で扱える |
| リポジトリ内 | `Directory.Build.targets` が同じ targets を import し、facade / binding / 検証ホストのビルドにも同じ検査が当たる |

### SDK 挙動として受け入れているもの

いずれも SDK の pack 内部構造に手を入れないと変えられない事象で、現状は受け入れている ([maui/ADR-0025](../../../decisions/maui/0025-nuget-three-package-root-namespace.md) の Consequences)。利用者に影響しないもの (iOS manifest の絶対パス、facade → binding の依存版、NU1507) と、利用者のビルドに影響が出得るもの (自 assembly 用 aar による XA4301 — 対処は未決で release workflow フェーズが扱う、API 版付き TFM — 利用者側の要件として確定済み) が混在する。

| 事象 | 内容と扱い |
|---|---|
| 自 assembly 用 aar | .NET Android SDK が Android ライブラリ assembly ごとに自動生成する `KsSettingsView.Maui.aar` / `KsSettingsView.Binding.Android.aar` (中身は推移依存 `androidx.graphics.path` の ABI 別 `.so` のみ) が各 nupkg に入る。facade 自身が native コードを持ち込んでいるのではなく、推移依存の `.so` を SDK が assembly ごとの aar に再梱包した結果である。利用者の Android Release ビルドで同じ `.so` が重複し XA4301 が出る — 対処は未決 (release workflow フェーズ) |
| iOS manifest の絶対パス | binding resource package の `manifest` に pack した環境の絶対パスが載る。リリースは CI で pack するため CI ランナーのパスになる |
| API 版付き TFM | nuspec の TFM group は `net10.0-android36.0` / `net10.0-ios26.0` のように SDK (10.0.300) の既定 platform 版が付く。消費者の TFM が API 版なしなら常にこの group が選ばれるが、古い API 版 (`net10.0-android35.0` / `net10.0-ios18.0`) を固定した利用者では restore が警告なく成功したうえで `lib/net10.0` へフォールバックし、binding 2 件が依存グラフに入らない (消費者検証で実測)。利用者向けの要件は [MAUI facade の公開契約](../api/maui-facade.md) の「導入と前提」が持つ |
| facade → binding の依存版 | 完全一致 `[x.y.z]` ではなく下限指定。lockstep で同時発行される ([cross/ADR-0019](../../../decisions/cross/0019-lockstep-single-version.md)) ため、NuGet は下限を満たす最小の版を選ぶ (lowest applicable version) 規則で同版の binding を解決する |
| NU1507 | CPM のため、複数の NuGet ソースを構成した環境では `maui/` 配下全プロジェクトの restore で出る (単一ソースでは出ない)。恒久対処は未決 (release workflow フェーズ) |

## SDK 更新時に再検証する箇所

binding csproj は互換保証のない SDK 内部 target / item に割り込む。workload または SDK の更新時は次を優先して確認する。**本節は [maui/ADR-0006](../../../decisions/maui/0006-android-binding-gradlew-exec.md) が「再検証の入口」として対で維持すると定めた表である。**

| 割り込み先 | 確認する性質 |
|---|---|
| `_BuildXcodeProjects` / `_SanitizeNativeReferences` | xcframework が `NativeReference` に登録され、最終的な binding の配布成果物へ同梱される |
| `_GetBuildXcodeProjectsInputs` / `_XcbInputs` | Bridge・Core・UI の Swift 変更で Native build が再実行され、`ios/binding/build/` の生成物は入力に入らない |
| `_CategorizeAndroidLibraries` / `_ResolveLibraryProjectImports` | aar の生成が `AndroidLibrary` の解決より前に走る |

iOS の入力一覧は次で確認できる。

```bash
dotnet build maui/macios/KsSettingsView.Binding.iOS/KsSettingsView.Binding.iOS.csproj \
  -t:_GetBuildXcodeProjectsInputs -getItem:_XcbInputs
```

Swift の増分判定は、上のコマンドで得た `_XcbInputs` の一覧に `ios/Sources/KsSettingsViewBridge` と取り込まれる Core・UI source が含まれ、binding target に含まれない source が含まれないことで確認する。一覧を見たうえで実際に片方だけを触ってビルドすると、過剰・不足の両方を検出できる。

pack 成果物についても、SDK 更新後に「SDK 挙動として受け入れているもの」の各事象が同じ形で現れるかを確認する。`dotnet pack` した nupkg を展開し (`unzip -o <pkg>.nupkg -d <dir>`)、nuspec の TFM group と依存、同梱物 (自 assembly 用 aar の有無と中身) を見る。あわせて platform TFM の `SupportedOSPlatformVersion` の SDK 既定値が変わっていないかを確認する — 変わると「最低 OS 版のビルド時ガード」の未設定時の前提 (Android 21 / iOS 26.x) が崩れる。

## 関連

- [Native Bridge の interop 境界](../api/native-bridge.md)
- [検証ホストによる binding / facade の確認](../../../handbook/maui/integration-host-verification.md)
- [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md)
- [公開識別子の規約](../../../handbook/cross/public-identifiers.md) — 生成型の namespace 規則と NuGet の配布座標
- [MAUI facade の公開契約](../api/maui-facade.md) — 利用者側から見た導入と前提 (要件・ガードの現れ方・型名衝突)
- 決定の経緯 (NuGet): [maui/ADR-0025](../../../decisions/maui/0025-nuget-three-package-root-namespace.md) (3 パッケージ構成・名前空間と Package ID の非対称・共通設定の置き場)
- 決定の経緯 (AndroidX 版整合): [maui/ADR-0010](../../../decisions/maui/0010-androidx-conflict-absorbed-in-binding-layer.md) (競合は binding 層の明示宣言で吸収し、NuGet 経路でも実証済み)
- 決定の経緯: [maui/ADR-0006](../../../decisions/maui/0006-android-binding-gradlew-exec.md) (Android binding は `gradlew` を Exec で呼ぶ)。上の「SDK 更新時に再検証する箇所」が同 ADR の言う再検証の入口にあたる — ADR 本文が挙げる所在は現行のものではないので、表はここを見る
