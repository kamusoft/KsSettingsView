# Proposal: add-spm-distribution

## Why

iOS の SwiftPM 配布は「配信リポジトリへのスナップショット配布」と決定済み (cross/ADR-0018) だが、その受け皿がまだ存在しない。配信リポジトリ `KsSettingsView-SPM` の作成、スナップショット生成スクリプト、umbrella product への一本化、そして https 実リモートでの解決確認 (姉妹ライブラリ KsDialogs の PoC は file:// 止まり) までを通し、release workflow (phase-8) が乗る土台を作る。

## What Changes

- `ios/Package.swift`: products を umbrella `KsSettingsView` 1 本に置き換える (既存 3 product `KsSettingsViewCore` / `KsSettingsViewUI` / `KsSettingsViewSwiftUI` を削除。target 構成・module 名は不変)
- `samples/ios/KsSettingsViewSample.xcodeproj` と `ios/binding/KsSettingsViewBridge.xcodeproj`: product 参照 (productRef) を umbrella 1 本へ差し替える
- `scripts/spm-snapshot/`: スナップショット同期スクリプト (ホワイトリスト 5 点をチェックアウト済み配信リポジトリ作業コピーへ冪等に配置。git 操作は責務外) と誘導 README テンプレートを新設する
- 配信リポジトリ `KsSettingsView-SPM` を agenda の初期設定一式 (public / Issues 等無効 / PR collaborators only / workflow・protection なし) で作成し、手動でスナップショットを初回 push、prerelease tag で https 解決を確認する。**検証完了後に tag は削除し、公開状態を残さない** (lockstep / tag-last との緊張を残さないため — design.md Decision 2)
- `kasane/handbook/cross/public-identifiers.md`: SwiftPM product 行 (3 本 → umbrella 1 本)・Package URL・配信リポジトリ名を追記する。規範層 (handbook) の改訂だが、phase-9 申し送りと phase-4 agenda 決定を承認の出典とする承認済み規範改訂として本変更に含める

影響する能力: iOS の配布 (SwiftPM 解決経路)、iOS ビルド入口 (Package.swift の product 面)

## Non-Goals

- release workflow への組み込み・deploy key の作成と secrets 登録 — phase-8 の責務 (フェーズ分担の決定どおり。phase-4 は手動実行の経路確立まで)
- 正式版 tag (`X.Y.Z`) の付与 — tag は publish 全成功後にのみ生まれる (cross/ADR-0020)。本変更で打つのは検証用 prerelease tag のみ
- KsDialogs への同型展開 — KsDialogs 側 phase-11 (逆流) の責務
- umbrella module (`@_exported import`) や binary 配布 — ADR-0018 で「要望が出た時点で再検討」と保留済み
- concepts (repository-boundaries.md) の追随 — 蒸留時の定型作業 (agenda TODO に記録済み)

## Impact

- 破壊的変更: product 名の変更は SwiftPM 消費者にとって破壊的だが、未リリースのため実利用者はゼロ。monorepo 内の消費者 (samples / binding) は本変更内で追随する
- リスク: binding xcodeproj は umbrella 経由で SwiftUI target もリンク対象に入る。Bridge は SwiftUI の記号を参照しないためリンカが落とす見込みだが、ビルド成功 + xcframework 生成確認を受け入れ条件として実証する
- 外部リソース: 公開リポジトリ `KsSettingsView-SPM` が新規に生まれる (中身はホワイトリスト 5 点のみで機密混入の余地なし)

## 級: L

外部連携 (公開 GitHub リポジトリの新設と実リモート統合) を含むため ksn-core の L 基準に該当。配布方針レベルの設計判断はフェーズ議論と ADR (0018 / 0020 追記) で確定済みだが、同期スクリプトの安全契約・検証 tag のライフサイクル・外部状態の検証方法は design.md の Decision として定める。

domain: cross
roadmap: package-distribution/phase-4-ios-packaging
