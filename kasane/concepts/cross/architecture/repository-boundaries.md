---
type: concept
title: リポジトリとビルドの責務境界
description: 横断変更をまとめる monorepo と、独立した platform build・Sample・消費者検証 (verification/) の責務分担
tags: [architecture, monorepo, build, sample, verification]
timestamp: 2026-09-02
---

この文書は、KsSettingsView の単一リポジトリと platform 別 build root、利用側 Sample、配布物の消費者検証 (`verification/`) の責務を説明する。読むと、横断変更を一つのリポジトリで扱いながら、iOS・Android・MAUI を一つの build graph に統合しない理由と、Sample が保証する範囲、消費者検証が確かめる範囲が分かる。

## リポジトリと build root

リポジトリ全体は、platform をまたぐ製品変更、version、変更管理、長命な設計知識を同じ変更単位で扱う。一方、build と test は各 ecosystem の標準入口から独立して開始する。リポジトリ root に全 platform 共通の build file や一括 build 入口は置かない。例外は .NET SDK 版のピン `global.json` で、`maui/` と `samples/maui/` の両方に同じ SDK 版を効かせるため dotnet の上方探索に合わせてルートに置く (統合 build 入口ではなく、他 platform の IDE のプロジェクト認識にも影響しない)。

| build root | 現在の公開単位 | 主な依存方向 | 役割 |
|---|---|---|---|
| iOS `ios/Package.swift` | umbrella product `KsSettingsView` 1 本 (module は `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI`) | SwiftUI wrapper → UI Host → Core | iOS library と test の入口 |
| Android `android/settings.gradle.kts` | 単一 artifact `jp.kamusoft:kssettingsview` (単一 module。層は Kotlin パッケージ `.core` / `.ui` / `.compose` で表す) | Compose wrapper → UI Host → Core | Android library と test の入口 |
| MAUI `maui/KsSettingsView.slnx` | NuGet 3 パッケージ — facade `KsSettingsView.Maui` (利用者が書く 1 点) + binding `KsSettingsView.Binding.iOS` / `.Android` (推移依存)。pack 可、公開レジストリへは未発行 | facade → binding (iOS / Android) → native の xcframework / aar | MAUI library・binding・test・検証ホストの入口 |

公開単位のほかに、iOS の `KsSettingsViewBridge` target と Android の `kssettingsview-bridge` module が interop 境界として存在する。product / artifact としては公開せず、MAUI binding が束縛する入口である ([Native Bridge の interop 境界](../../maui/api/native-bridge.md))。MAUI の build root は binding の中から native 2 系統の build root を呼ぶ (Android は `gradlew` の Exec、iOS は Xcode project) 取り込む側であり、native が MAUI に依存する逆方向はない。

各 build root は自身の module 構成、依存解決、toolchain、test 入口を所有する。一つの platform の build 成立は、別 platform の toolchain や build 成立を保証しない。

iOS の SwiftPM 配布は monorepo を直接解決させず、専用の公開配信リポジトリ `KsSettingsView-SPM` から行う ([ADR-0018](../../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md))。リリース時に `ios/Package.swift` / `ios/Sources/` / `ios/Tests/` と `LICENSE`・誘導 README のスナップショットを配信リポジトリのルートへ commit し、同じ version の tag を push する (ファイル配置は `scripts/spm-snapshot/` のスクリプト、commit / tag / push は呼び出し側の責務)。monorepo のルートに Package.swift は置かず、`ios/Package.swift` が開発用かつ配信用の唯一のマニフェストである。配信リポジトリには初回リリースまで tag が存在せず、利用者が解決できる版はまだない。

Core、UI Host、宣言 UI wrapper は責務を分けるが、Core が platform 型から完全に独立していることまでは意味しない。現行の Accessory 型は、iOS では UIKit / SwiftUI、Android では Android View / Compose と接続する。

## Sample の consumer 境界

Sample は library 本体の配布物ではなく、利用者 application と同じ側から公開 product / module を組み合わせる実行可能な reference である。

- iOS Sample は `ios/` を Local Swift Package として参照し、umbrella product `KsSettingsView` 1 本を link する。
- Android Sample は `android/` を Gradle composite build（独立した Gradle build 同士を組み合わせる仕組み）として参照し、外部 module 形式の依存を included project へ置換する。
- MAUI Sample (`samples/maui/KsSettingsView.Sample.Maui`) は facade `KsSettingsView.Maui` への `ProjectReference` 1 本で参照し、binding 層は推移参照で入る。
- いずれも IDE から開発中の本体 source へ移動・debug できるが、package repository から配布物を取得できることまでは保証しない。

Android の composite build は source 参照を接続するが、Sample と included library の build root を一つへ統合しない。両 build root は Android SDK と toolchain の場所をそれぞれの開発環境から解決できる必要がある。local source reference から toolchain 設定の継承まで推論しない。ただしビルド関連バージョン (AGP / Kotlin / Compose BOM / ライブラリの version) の宣言だけは `android/gradle/libs.versions.toml` を両 build root が共有する — Sample 側は settings の `versionCatalogs` で同じファイルを読み、二重に宣言しない (詳細は [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md))。

Sample は公開 API の組み合わせ、app host の前提、統合状態、視覚、操作結果を実行・目視確認する。挙動契約の唯一の正（SSoT）と自動回帰検証は library code と test が担う。Sample の画面数、navigation、表示文字列、デモデータ、比較用の色値は製品契約にしない。ただし製品契約にしないことと platform 間で揃えることは別軸であり、Sample の文言・画面構成は platform 間で一致させる（[Sample のプラットフォーム間一致](../../../handbook/cross/sample-parity.md)）。

## 消費者検証 (`verification/`) の境界

`verification/{ios,android,maui}/` は、配布物を利用者と同じ経路で解決してビルドできるかを確かめる消費者プロジェクトである。Sample と違って本体のソースは参照せず、公開座標 (SwiftPM product `KsSettingsView` / `jp.kamusoft:kssettingsview` / `KsSettingsView.Maui`) だけを参照する。ソースはルート README の最小コード例 (iOS / Android / MAUI XAML / MAUI C# の 4 コードブロック) の無編集の写しで、README と食い違うと `scripts/readme-example-lint.py` が CI の lint job で失敗する — README の例が壊れれば消費者ビルドが落ちる仕組みである。`samples/` のプラットフォーム間一致規約の対象ではなく、3 platform で内容が揃うのは README 一致の帰結にすぎない。

検証するのは配信経路 (解決・メタデータ・推移依存・利用者側 SDK での Release ビルド) までで、Simulator / Emulator / 実機での起動、`dotnet publish`、R8 の縮小 (`isMinifyEnabled` は false) は含めない。実行時挙動は Sample と手元の手順が担う ([ADR-0026](../../../decisions/cross/0026-ci-guarantee-logic-and-wiring-not-e2e.md))。

### 参照先の切り替えと排他性

参照先はモードと version の 2 引数で切り替える。消費者のソースはモードで変わらない。

| モード | 参照先 | 呼ばれる場面 |
|---|---|---|
| `dry-run` | iOS はスナップショット (`scripts/spm-snapshot/` の出力を identity `KsSettingsView-SPM` のディレクトリに置いた `path:` 参照)、Android は mavenLocal、MAUI はローカルフォルダフィード。version 未指定なら本体の開発用既定値 (Android `0.1.0-SNAPSHOT` / MAUI `0.0.0-dev`。iOS は version を持たない) | PR / push の検証 CI で毎回。release では publish 前 |
| `smoke` | 配信リポジトリ `KsSettingsView-SPM` の tag、Maven Central、nuget.org。version は必須 | release の publish 後 (初回リリースまで正ケースは未実証) |

dry-run の参照先は本リポジトリ由来の座標について排他的である。Android は `exclusiveContent` で `jp.kamusoft` を mavenLocal だけに割り当て、MAUI は packageSourceMapping で `KsSettingsView.*` をローカルフィードに固定したうえで実行ごとに空のパッケージ展開先を使い、取得元をパッケージ単位の `.nupkg.metadata` で検査する (global packages folder に既にある版には mapping が働かないため)。ローカルに無い version は公開レジストリやユーザー環境のキャッシュへ黙ってフォールバックせず失敗する — フォールバックを許すと dry-run が偽陽性になる。MAUI はさらに restore 警告 NU1605 / NU1608 / NU1107 をエラーにし、binding 2 件の解決版が facade と一致することを検査する (lockstep の消費者側の現れ)。

### 実行手段と CI

実行手段は platform ごとに「フィード準備」(`prepare-feed.sh`: スナップショット配置 / `publishToMavenLocal` / `pack`) と「消費者ビルド」(`build-consumer.sh`: Release 構成、署名情報なし、MAUI iOS は Simulator RID) の 2 段に分かれ、準備済みの参照先を `--reference` で渡せば準備段を再実行しない。CI では platform 別の再利用可能 workflow `verify-consumer-{ios,android,maui}.yml` ([ADR-0025](../../../decisions/cross/0025-verification-ci-reusable-platform-workflows.md) の形) がこの 2 段を包み、release workflow の publish 段が作った成果物は `artifact` 入力で受け取る。artifact は dry-run 専用で、smoke は version だけを受ける (`smoke` + `artifact` は入力検査で拒否する — 消費者ビルドが artifact を使わないため、許すと公開済みの版を検証したものが artifact の検証に見える)。消費者 job は `contents: read` だけで secrets を受け取らず、配信先への書き込み経路を持たない。手元での実行手順は [ローカル開発環境と Sample の実行](../../../handbook/cross/local-development-setup.md) にある。

## 保証すること

- platform をまたぐ変更と長命な知識を一つのリポジトリで調整できる。
- iOS と Android は他方の toolchain を build graph に含めず、それぞれの入口から build と test を開始できる。
- Native library は Core、UI Host、宣言 UI wrapper の公開単位を分け、上位層から下位層へ依存する。
- Sample は本体の内部 source set を混在させず、利用者側から公開 product / module を参照する。
- Sample の local source reference により、開発中の本体変更を利用 application で確認できる。
- Android Sample と included library は、それぞれの build root で SDK / toolchain を解決する。バージョン宣言だけは共有 catalog で一致させる。
- `verification/` の消費者は公開座標だけを参照し、dry-run ではローカル参照先に無い配布物を公開レジストリやキャッシュから補わずに失敗する。

## してはいけないこと

- リポジトリ root に全 platform の統合 build があると仮定しない。
- 一つの platform の build や test を、別 platform の検証結果として扱わない。
- Sample を library の配布物、挙動契約の SSoT、自動 test の代替として扱わない。
- local source reference の成功を、公開 repository からの配布成立と説明しない。
- MAUI の 3 パッケージがローカルで pack できることを、公開レジストリから取得できる配布物があることと説明しない (発行は未着手。ローカル pack と消費者検証まで)。
- 配信リポジトリ `KsSettingsView-SPM` を開発の入口として扱わない。手で commit せず、ソース・Issue の窓口は monorepo である。
- `verification/` を Sample や本体開発の入口として扱わない。README 最小例の写し以外のコードを置かず、Sample の一致規約の対象にも数えない。
- 消費者 dry-run の成功を、公開レジストリから取得できる配布物があることと説明しない (それを示すのは smoke である)。

## 関連

### 各 platform の公開契約

- [iOS Native Host](../../ios/api/ios-native-host.md)
- [iOS SwiftUI Bridge](../../ios/api/ios-swiftui.md)
- [Android Native Host](../../android/api/android-native-host.md)
- [Android Compose Bridge](../../android/api/android-compose.md)
- [Android ビルドツールチェーンの契約](../../android/architecture/build-toolchain.md)
- [MAUI facade の公開契約](../../maui/api/maui-facade.md)
- [Native Bridge の interop 境界](../../maui/api/native-bridge.md)
- [MAUI binding の Native artifact 統合](../../maui/architecture/binding-build-integration.md) — NuGet 3 パッケージの pack 構成

### 規約と決定

- [公開識別子と配布座標](../../../handbook/cross/public-identifiers.md)
- [Sample のプラットフォーム間一致](../../../handbook/cross/sample-parity.md)
- [ADR-0001: モノレポとプラットフォーム別ビルドルート](../../../decisions/cross/0001-monorepo-platform-build-roots.md)
- [ADR-0018: 配布チャネルと SwiftPM 配信リポジトリ](../../../decisions/cross/0018-distribution-public-channels-root-swiftpm-manifest.md)
- [ADR-0025: 検証 CI は platform 別の再利用可能 workflow で構成する](../../../decisions/cross/0025-verification-ci-reusable-platform-workflows.md)
- [ADR-0026: CI が保証する範囲](../../../decisions/cross/0026-ci-guarantee-logic-and-wiring-not-e2e.md)
- [ローカル開発環境と Sample の実行](../../../handbook/cross/local-development-setup.md) — 消費者検証を手元で回す手順
