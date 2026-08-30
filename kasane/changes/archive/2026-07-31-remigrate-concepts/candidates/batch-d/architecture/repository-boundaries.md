---
type: concept
title: リポジトリとビルドの責務境界
description: 横断変更をまとめる monorepo と、独立した platform build・Sample の責務分担
tags: [architecture, monorepo, build, sample]
timestamp: 2026-07-19
---

この文書は、KsSettingsView の単一リポジトリと platform 別 build root、利用側 Sample の責務を説明する。読むと、横断変更を一つのリポジトリで扱いながら、iOS・Android・将来の MAUI を一つの build graph に統合しない理由と、Sample が保証する範囲が分かる。

## リポジトリと build root

リポジトリ全体は、platform をまたぐ製品変更、version、変更管理、長命な設計知識を同じ変更単位で扱う。一方、build と test は各 ecosystem の標準入口から独立して開始する。リポジトリ root に全 platform 共通の build file や一括 build 入口は置かない。

| build root | 現在の公開単位 | 主な依存方向 | 役割 |
|---|---|---|---|
| iOS `ios/Package.swift` | `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` | SwiftUI wrapper → UI Host → Core | iOS library と test の入口 |
| Android `android/settings.gradle.kts` | `ks-settingsview-core` / `ks-settingsview-ui` / `ks-settingsview-compose` | Compose wrapper → UI Host → Core | Android library と test の入口 |
| MAUI `maui/KsSettingsView.slnx` | なし | なし | 将来の binding 用に予約した空の入口 |

各 build root は自身の module 構成、依存解決、toolchain、test 入口を所有する。一つの platform の build 成立は、別 platform の toolchain や build 成立を保証しない。

Core、UI Host、宣言 UI wrapper は責務を分けるが、Core が platform 型から完全に独立していることまでは意味しない。現行の Accessory 型は、iOS では UIKit / SwiftUI、Android では Android View / Compose と接続する。

## Sample の consumer 境界

Sample は library 本体の配布物ではなく、利用者 application と同じ側から公開 product / module を組み合わせる実行可能な reference である。

- iOS Sample は `ios/` を Local Swift Package として参照し、3つの SwiftPM product を link する。
- Android Sample は `android/` を Gradle composite build（独立した Gradle build 同士を組み合わせる仕組み）として参照し、外部 module 形式の依存を included project へ置換する。
- どちらも IDE から開発中の本体 source へ移動・debug できるが、package repository から配布物を取得できることまでは保証しない。

Sample は公開 API の組み合わせ、app host の前提、統合状態、視覚、操作結果を実行・目視確認する。挙動契約の唯一の正（SSoT）と自動回帰検証は library code と test が担う。Sample の画面数、navigation、表示文字列、デモデータ、比較用の色値は製品契約にしない。

## 保証すること

- platform をまたぐ変更と長命な知識を一つのリポジトリで調整できる。
- iOS と Android は他方の toolchain を build graph に含めず、それぞれの入口から build と test を開始できる。
- Native library は Core、UI Host、宣言 UI wrapper の公開単位を分け、上位層から下位層へ依存する。
- Sample は本体の内部 source set を混在させず、利用者側から公開 product / module を参照する。
- Sample の local source reference により、開発中の本体変更を利用 application で確認できる。

## してはいけないこと

- リポジトリ root に全 platform の統合 build があると仮定しない。
- 一つの platform の build や test を、別 platform の検証結果として扱わない。
- Sample を library の配布物、挙動契約の SSoT、自動 test の代替として扱わない。
- local source reference の成功を、公開 repository からの配布成立と説明しない。
- 空の MAUI solution と Sample placeholder を、利用可能な MAUI product とみなさない。

## 関連

- [公開識別子と配布座標](../conventions/public-identifiers.md)
- [iOS Native Host](../platforms/ios-native-host.md)
- [iOS SwiftUI Bridge](../platforms/ios-swiftui.md)
- [Android Native Host](../platforms/android-native-host.md)
- [Android Compose Bridge](../platforms/android-compose.md)
- [ADR-0001: モノレポとプラットフォーム別ビルドルート](../../decisions/0001-monorepo-platform-build-roots.md)
