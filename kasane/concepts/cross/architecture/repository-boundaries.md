---
type: concept
title: リポジトリとビルドの責務境界
description: 横断変更をまとめる monorepo と、独立した platform build・Sample の責務分担
tags: [architecture, monorepo, build, sample]
timestamp: 2026-08-21
---

この文書は、KsSettingsView の単一リポジトリと platform 別 build root、利用側 Sample の責務を説明する。読むと、横断変更を一つのリポジトリで扱いながら、iOS・Android・MAUI を一つの build graph に統合しない理由と、Sample が保証する範囲が分かる。

## リポジトリと build root

リポジトリ全体は、platform をまたぐ製品変更、version、変更管理、長命な設計知識を同じ変更単位で扱う。一方、build と test は各 ecosystem の標準入口から独立して開始する。リポジトリ root に全 platform 共通の build file や一括 build 入口は置かない。例外は .NET SDK 版のピン `global.json` で、`maui/` と `samples/maui/` の両方に同じ SDK 版を効かせるため dotnet の上方探索に合わせてルートに置く (統合 build 入口ではなく、他 platform の IDE のプロジェクト認識にも影響しない)。

| build root | 現在の公開単位 | 主な依存方向 | 役割 |
|---|---|---|---|
| iOS `ios/Package.swift` | `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` | SwiftUI wrapper → UI Host → Core | iOS library と test の入口 |
| Android `android/settings.gradle.kts` | `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` | Compose wrapper → UI Host → Core | Android library と test の入口 |
| MAUI `maui/KsSettingsView.slnx` | `KsSettingsView.Maui` (facade。公開レジストリへは未配信) | facade → binding (iOS / Android) → native の xcframework / aar | MAUI library・binding・test・検証ホストの入口 |

公開単位のほかに、iOS の `KsSettingsViewBridge` target と Android の `ks-settingsview-bridge` module が interop 境界として存在する。product / artifact としては公開せず、MAUI binding が束縛する入口である ([Native Bridge の interop 境界](../../maui/api/native-bridge.md))。MAUI の build root は binding の中から native 2 系統の build root を呼ぶ (Android は `gradlew` の Exec、iOS は Xcode project) 取り込む側であり、native が MAUI に依存する逆方向はない。

各 build root は自身の module 構成、依存解決、toolchain、test 入口を所有する。一つの platform の build 成立は、別 platform の toolchain や build 成立を保証しない。

Core、UI Host、宣言 UI wrapper は責務を分けるが、Core が platform 型から完全に独立していることまでは意味しない。現行の Accessory 型は、iOS では UIKit / SwiftUI、Android では Android View / Compose と接続する。

## Sample の consumer 境界

Sample は library 本体の配布物ではなく、利用者 application と同じ側から公開 product / module を組み合わせる実行可能な reference である。

- iOS Sample は `ios/` を Local Swift Package として参照し、3つの SwiftPM product を link する。
- Android Sample は `android/` を Gradle composite build（独立した Gradle build 同士を組み合わせる仕組み）として参照し、外部 module 形式の依存を included project へ置換する。
- MAUI Sample (`samples/maui/KsSettingsView.Sample.Maui`) は facade `KsSettingsView.Maui` への `ProjectReference` 1 本で参照し、binding 層は推移参照で入る。
- いずれも IDE から開発中の本体 source へ移動・debug できるが、package repository から配布物を取得できることまでは保証しない。

Android の composite build は source 参照を接続するが、Sample と included library の build root を一つへ統合しない。両 build root は Android SDK と toolchain の場所をそれぞれの開発環境から解決できる必要がある。local source reference から toolchain 設定の継承まで推論しない。ただしビルド関連バージョン (AGP / Kotlin / Compose BOM / ライブラリの version) の宣言だけは `android/gradle/libs.versions.toml` を両 build root が共有する — Sample 側は settings の `versionCatalogs` で同じファイルを読み、二重に宣言しない (詳細は [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md))。

Sample は公開 API の組み合わせ、app host の前提、統合状態、視覚、操作結果を実行・目視確認する。挙動契約の唯一の正（SSoT）と自動回帰検証は library code と test が担う。Sample の画面数、navigation、表示文字列、デモデータ、比較用の色値は製品契約にしない。ただし製品契約にしないことと platform 間で揃えることは別軸であり、Sample の文言・画面構成は platform 間で一致させる（[Sample のプラットフォーム間一致](../conventions/sample-parity.md)）。

## 保証すること

- platform をまたぐ変更と長命な知識を一つのリポジトリで調整できる。
- iOS と Android は他方の toolchain を build graph に含めず、それぞれの入口から build と test を開始できる。
- Native library は Core、UI Host、宣言 UI wrapper の公開単位を分け、上位層から下位層へ依存する。
- Sample は本体の内部 source set を混在させず、利用者側から公開 product / module を参照する。
- Sample の local source reference により、開発中の本体変更を利用 application で確認できる。
- Android Sample と included library は、それぞれの build root で SDK / toolchain を解決する。バージョン宣言だけは共有 catalog で一致させる。

## してはいけないこと

- リポジトリ root に全 platform の統合 build があると仮定しない。
- 一つの platform の build や test を、別 platform の検証結果として扱わない。
- Sample を library の配布物、挙動契約の SSoT、自動 test の代替として扱わない。
- local source reference の成功を、公開 repository からの配布成立と説明しない。
- MAUI facade がビルドできることを、公開レジストリから取得できる配布物があることと説明しない (配信は未着手)。

## 関連

- [公開識別子と配布座標](../conventions/public-identifiers.md)
- [Sample のプラットフォーム間一致](../conventions/sample-parity.md)
- [iOS Native Host](../../ios/api/ios-native-host.md)
- [iOS SwiftUI Bridge](../../ios/api/ios-swiftui.md)
- [Android Native Host](../../android/api/android-native-host.md)
- [Android Compose Bridge](../../android/api/android-compose.md)
- [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md)
- [MAUI facade の公開契約](../../maui/api/maui-facade.md)
- [Native Bridge の interop 境界](../../maui/api/native-bridge.md)
- [ADR-0001: モノレポとプラットフォーム別ビルドルート](../../../decisions/cross/0001-monorepo-platform-build-roots.md)
