---
type: concept
title: MAUI binding の Native artifact 統合
description: MAUI binding が iOS xcframework と Android aar を生成・取り込みする構成、既知の制約、SDK 更新時の再検証箇所
tags: [maui, binding, build, interop]
timestamp: 2026-08-29
---

# MAUI binding の Native artifact 統合

この文書は、MAUI binding のビルドが Native artifact を生成して binding assembly へ取り込む経路と、その経路が依存する SDK 内部拡張点を説明する。読むと、iOS と Android で SDK 標準アイテムの採否が異なる理由、警告を抑止していない理由、workload や SDK を更新したときに再検証する箇所が分かる。

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

生成される .NET の型は、iOS / Android のどちらの binding でも `KsSettingsView.Bridge` namespace に入る。Android は Java パッケージ名からの既定変換 (`Jp.Kamusoft.Kssettingsview.Bridge`) を `Transforms/Metadata.xml` の `managedName` 属性で上書きしてこれに揃えている。この上書きを外すと生成型の namespace が Java パッケージ由来へ戻り、[公開識別子の規約](../../cross/conventions/public-identifiers.md) の .NET namespace 規則と iOS binding との対称性が同時に壊れる。

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

## 関連

- [Native Bridge の interop 境界](../api/native-bridge.md)
- [検証ホストによる binding / facade の確認](../conventions/integration-host-verification.md)
- [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md)
- [公開識別子の規約](../../cross/conventions/public-identifiers.md) — 生成型の namespace 規則
- 決定の経緯: [maui/ADR-0006](../../../decisions/maui/0006-android-binding-gradlew-exec.md) (Android binding は `gradlew` を Exec で呼ぶ)。上の「SDK 更新時に再検証する箇所」が同 ADR の言う再検証の入口にあたる — ADR 本文が挙げる所在は現行のものではないので、表はここを見る
