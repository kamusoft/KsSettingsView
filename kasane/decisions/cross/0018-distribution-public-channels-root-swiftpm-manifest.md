---
id: 0018
title: 配布は公開レジストリの標準チャネルのみとし、SwiftPM は配信リポジトリで配る
status: accepted
date: 2026-08-21
---

## Context

KsSettingsView は AiForms.Maui.SettingsView の後継として公開 OSS で配布する前提で作られている (cross/ADR-0017)。公開識別子は Maven Central を前提に定められている (cross/ADR-0002) が、配布の仕組みは未整備で、リポジトリは private、CI も tag も存在せず、Sample は 3 platform ともソース参照 (Local Swift Package / composite build / ProjectReference) でしか本体を取り込んでいない。

SwiftPM の git 配布はリポジトリルート直下の Package.swift しか解決できず (サブディレクトリ指定は未サポート)、現状の `ios/Package.swift` のままでは SwiftPM 配布が成立しない。SwiftPM には Maven Central / NuGet.org のような中央サーバーが実用上なく、git 直接参照 + semver tag が事実上の唯一解である。iOS の公開 product は `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 本 (target と 1:1) で、`KsSettingsViewBridge` target は Xcode project の同名 target との scheme 衝突を避けるため product として公開していない。Android 側は単一 artifact `jp.kamusoft:kssettingsview` に統合する (android/ADR-0016) ため、iOS も「利用者が手で入れるのは 1 点」で揃えたい。ただし SwiftUI module には看板の型 `public struct KsSettingsView` があり、Core の `Section` は SwiftUI 標準の `Section` と衝突するため、利用者は `KsSettingsViewCore.Section` のような module 修飾を日常的に使う。

姉妹ライブラリ KsDialogs は同じ構図で配布モデルを先に設計しており (KsDialogs cross/ADR-0008)、本 ADR はその翻案である。

当初案はルート Package.swift への移設 (monorepo を SwiftPM が直接解決する形) だったが、public 化の準備 (package-distribution ロードマップ) で次が判明した: SwiftPM 利用者は monorepo を履歴ごと full clone するため、`kasane/changes/` に蓄積する検証証跡の媒体 (公開時点で約 180 MB、以後も変更ごとに増える) がそのまま利用者のコストになり、配信の都合で開発の足場 (証跡の保存方法) を制約しなければならなくなる。`ios/Package.swift` の `path:` は `ios/` 相対 (`Sources/...` `Tests/...`) なので、`ios/` 配下をそのまま別リポジトリのルートへ置けば無改変で解決できる。

## Decision

**配布先: 公開レジストリの標準チャネルのみ。private 配信経路は作らない。**

| 形態 | チャネル |
|---|---|
| Native iOS | SwiftPM (公開 git リポジトリ + semver tag)。package `KsSettingsView`、product は umbrella の `KsSettingsView` 1 本 |
| Native Android | Maven Central (`jp.kamusoft:ks-settingsview-*`、cross/ADR-0002) |
| .NET MAUI | NuGet.org |

(2026-09-01 追記) 表の Native Android の座標は、Android の module 統合により
`jp.kamusoft:kssettingsview` の単一 artifact になった (android/ADR-0016)。groupId は
cross/ADR-0002 の `jp.kamusoft` のままで、artifactId が `ks-settingsview-*` の 3 本から
`kssettingsview` 1 本に変わった。interop Bridge は Maven に公開しない。

GitHub Packages 等の private / 認証付きフィードは提供しない。SwiftPM が git を直接解決する都合上、リポジトリは public に切り替える (切り替えのタイミングは配信 CI の整備と合わせて決める)。

Android の artifact 粒度 (module 間依存の公開スコープ、bridge module の公開可否) と MAUI のパッケージ分割は本 ADR の対象外とし、それぞれ別の決定で扱う。

**SwiftPM チャネル: 配信リポジトリへのスナップショット配布。**

- SwiftPM 専用の公開配信リポジトリを別に持ち、release CI が `ios/Package.swift` と `ios/Sources/` `ios/Tests/` (と LICENSE) のスナップショットを配信リポジトリのルートへ commit し、同じ version の semver tag を push する。配信リポジトリの履歴はリリース回数分しか増えない
- monorepo のルートには Package.swift を置かない。`ios/Package.swift` が開発用かつ配信用の唯一のマニフェストであり (配信リポジトリ側はそのコピー)、2 枚持ちにはしない
- 配信リポジトリは Issues / PR を無効化し、README で monorepo (ソース・Issue 窓口) へ誘導する。手で commit しない (CI のみが書く)
- product は `KsSettingsView` 1 本とし、`KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` の 3 target を束ねる (umbrella product)。利用者はこの 1 product をリンクし、`import` は使う module 名で書く。target 構成・module 名・Bridge target の非公開・`platforms` (iOS 16 + テスト用 macOS) は変更しない
- module 名は型名と別に保つ。module 名を `KsSettingsView` にすると型 `KsSettingsView` に吸われて `KsSettingsView.Section` のような修飾が解決できなくなるため、3 target を 1 target へ物理統合しない
- 配信リポジトリの tag は monorepo の semver tag と同じ値を持つ (lockstep は cross/ADR-0019)。配信リポジトリへの push は SwiftPM の publish 工程であり、リリース手順上の位置づけは cross/ADR-0020 に従う
- **配信リポジトリ名は `KsSettingsView-SPM`** (Package URL: `https://github.com/kamusoft/KsSettingsView-SPM`)。(2026-08-29 追記) SwiftPM の package identity は git URL の最終パスコンポーネント由来で `Package.swift` の `name:` は表示専用のため、利用者の Xcode 上の表示は package 名の `KsSettingsView` だが、`Package.swift` を書く利用者は `.product(name: "KsSettingsView", package: "KsSettingsView-SPM")` と identity を書き、`Package.resolved` にも `kssettingsview-spm` として残る。`-SPM` サフィックスは配信専用リポジトリの既存慣例に倣う (`airbnb/lottie-spm`・`RevenueCat/purchases-ios-spm`・`BranchMetrics/ios-branch-sdk-spm`・`forcedotcom/SalesforceMobileSDK-iOS-SPM`)。大文字表記は PascalCase の製品名に付ける Salesforce の形に合わせた。姉妹ライブラリも同型で展開する (`KsDialogs-SPM`)

cross/ADR-0001 (リポジトリルートに共通ビルドファイルを置かない) への例外は不要になる。

## Alternatives Considered

- **配信リポジトリ名を `KsSettingsView-swift` にする**: Swift 版の入口として自然だが、本体の Swift 実装がそこにあると読まれ、Issues / PR を無効化して monorepo へ誘導する方針と衝突しやすい。却下。
- **配信リポジトリ名を `swift-kssettingsview` にする**: Apple 公式パッケージ (swift-collections 等) の命名に揃うが、配信専用であることが名前から伝わらず、PascalCase の製品名表記とも割れる。却下。

- **private 配信 (GitHub Packages + private git の SwiftPM) のまま配る**: 却下。公開 OSS が前提であり、private 配信は不要。GitHub Packages は匿名取得ができず利用者に認証設定を強いる。
- **段階方式 (private 経路で配信パイプラインを確立してから公開レジストリへ向け替える)**: 却下。使わない private 経路を作る意味がない。
- **開発用 `ios/Package.swift` と配布用ルート Package.swift の 2 枚持ち**: 却下。2 つのマニフェストが乖離する事故の温床になる。
- **binary 配布 (xcframework を GitHub Release に添付し、ルート Package.swift は binaryTarget のみ)**: 却下。毎リリースの xcframework 生成と checksum 管理が CI に乗り、Swift バージョン依存も生じる。source 配布で成立する現状では不要。将来 binary 配布が必要になった時点で再検討する。
- **ルート Package.swift へ移設し monorepo を直接配信する (当初案)**: 却下。SwiftPM 利用者が monorepo 全体を履歴ごと full clone するため、開発の足場 (検証証跡の媒体) が利用者の clone コストになり、配信の都合で足場の運用を制約することになる。cross/ADR-0001 への例外も要る。かつて懸念した「同期 CI と tag の二重管理」「URL と issue 窓口の分裂」は、tag を CI が release 時に自動で打ち、配信リポジトリの Issues / PR を無効化して monorepo へ誘導することで解消できる。
- **3 target を 1 target `KsSettingsView` に物理統合する (Android と同じ統合)**: 却下。module 名が型 `KsSettingsView` と同名になり、利用者が SwiftUI の `Section` と区別するための `KsSettingsView.Section` 修飾が壊れる。回避策 (看板の型の改名、module 名を `KsSettingsViewKit` 等にする) はいずれも API か命名を汚す。
- **umbrella module (`@_exported import` で 3 module を再輸出し `import KsSettingsView` 1 行にする)**: 今回は採らない。umbrella product の上にいつでも足せるため、要望が出た時点で module 名と型名の衝突を PoC で確認してから検討する。

## Consequences

- 正: 利用者は各 ecosystem の標準手段 (SwiftPM / Maven / NuGet) だけで導入でき、認証設定が不要になる。
- 正: KsDialogs と同じ配布構造になり、配信 CI・検証方法を両リポジトリで共有できる。
- 正: `ios/Package.swift` は無改変で配信リポジトリのルートに置ける (`path:` が `ios/` 相対)。target 構成・API・対応 OS に影響しない。
- 正: SwiftPM 利用者の clone は配信リポジトリ (ライブラリ本体のみ、数 MB) で済み、monorepo の証跡・他 platform を引かない。monorepo 側は証跡媒体の保存方法を配信の都合で制約されない。
- 正: iOS (product 1 本) と Android (artifact 1 本) で「利用者が手で入れるのは 1 点」が揃う。
- 負: product 名と module 名が一致しないため、利用者は product `KsSettingsView` をリンクした上で `import KsSettingsViewUI` のように module 名を書く (Firebase や swift-collections と同じ形)。`samples/ios` のリンク設定を 3 product から 1 product へ変更する。
- 負: リポジトリの public 化が配布の前提条件になる。
- 負: 配信リポジトリという 2 つ目のリポジトリと、release CI がそこへ書き込むための secret (deploy key または fine-grained PAT) を持つ。
- 負: 利用者から見て「ソース・Issue は monorepo、Package URL は配信リポジトリ」の 2 URL 体制になる。
- 負: 利用者が `Package.swift` に書く identity は製品名そのものではなく `KsSettingsView-SPM` になる (Xcode の Package Dependencies 一覧の表示は `KsSettingsView`)。
- 負: 消費者検証の publish 前 dry-run は配信リポジトリの prerelease tag か `path:` 参照で行う必要がある。
- 負: 公開レジストリに出したものは取り下げにくい (NuGet.org は unlist のみ、Maven Central は原則削除不可)。
- 負 (2026-09-01 実装結果): umbrella product をリンクする staticlib archive では、参照しない module のオブジェクトも dead-strip されず成果物に入る。MAUI binding の xcframework に `KsSettingsViewSwiftUI` 由来シンボル 642 件が混入することを実測し、受容した (除外が必要になったら別の変更で検討)。
- 中立 (2026-09-01 実装結果): product 一本化により Xcode 生成 scheme は package と同名の `KsSettingsView` 1 本になり、検証 CI・規約が参照する scheme 名も連動して変わる。

出典: kasane/roadmaps/package-distribution/exploration.md (A・C) / kasane/roadmaps/package-distribution/phases/phase-2-public-readiness/history.md (2026-08-21: SwiftPM の配信形) / ../KsDialogs/kasane/decisions/cross/0008-distribution-model-standard-channels.md (翻案元)
出典 (2026-08-29 配信リポジトリ名の確定): kasane/roadmaps/package-distribution/phases/phase-9-docs/history.md (2026-08-29「SwiftPM 配信リポジトリの名前」)
出典 (2026-09-01 実装結果の追記と accepted 昇格): kasane/changes/archive/2026-09-01-add-spm-distribution/deviation.md / 同 review-001.md
出典 (2026-09-01 Android 座標の統合の追記): kasane/decisions/android/0016-single-module-single-maven-artifact.md / kasane/roadmaps/package-distribution/phases/phase-5-android-packaging/history.md
