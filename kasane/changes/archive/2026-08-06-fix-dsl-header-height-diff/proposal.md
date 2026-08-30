# Proposal: fix-dsl-header-height-diff

## Why

宣言的 DSL (Compose / SwiftUI) で `Section.headerHeight` だけを動的に変えても、両 platform の `DSLDiffCalculator` がどの段でも diff を生成せず (Android は `sameStructure` の早期 return で `emptyList()`、iOS は早期 return は抜けるがどの段も headerHeight を扱わない)、表示が更新されない。Store / View API 経由では反映される ([fix-android-header-height-refresh](../archive/fix-android-header-height-refresh/exploration.md) で修正・実機検証済み) ため、[core/ADR-0018](../../decisions/core/0018-store-dsl-path-result-symmetry.md) が禁じる「経路間の無音の取りこぼし」の現存例になっている。

## What Changes

探索の決定 A に従い、両 platform の `DSLDiffCalculator` に**可視性 preflight と同型の headerHeight 差の preflight 検出**を追加し、検出時は `Full` のみを発行する:

1. Android (`DSLDiffCalculator.kt`): 同一 ID の Section 間で `headerHeight` が変化していたら `SettingsRootDiff.Full(newRoot)` のみを返す。`contentUpdates` も可視性時と同様に同条件で空リストを返す (内容更新は Full の共通出口 `setRootDirect` が内包するため)
2. iOS (`DSLDiffCalculator.swift`): 可視性 preflight と同じ位置に headerHeight 差の検出を追加し、検出時は `.full(newRoot)` を発行する。同一再評価内で内容が変わった同一 ID の Cell があれば、`.full` に**続けて**当該 Cell の `.replaceCell` を発行して内容反映を担保する (host 変更なしで完結 — second-opinion-001 Critical で確定した方式)
3. ADR-0018 対称テスト義務の初適用: 両 platform の `DSLDiffCalculator` テストに「headerHeight のみ変更 → 表示更新につながる diff が出る」を追加する。iOS の Store 経由 (exploration の到達経路表で「未確認」) もこの機に検証して対称を閉じる

影響する能力: settings-view-android-ui / settings-view-ios-ui

## Non-Goals

- iOS の `.full` 適用における同一 ID Cell 内容反映の疑いの**全面解消** ([fix-ios-full-content-refresh](../fix-ios-full-content-refresh/exploration.md) の責務。本 change は「headerHeight + 内容の同時変更」シナリオの観測結果のみを要求する)
- iOS が View accessory にも headerHeight を適用する非対称の是正 ([fix-ios-view-header-height-override](../fix-ios-view-header-height-override/exploration.md) の責務)
- `SettingsRootDiff` への headerHeight 専用種別の追加 (探索で却下 — 公開 API 変更を伴う割に `Full` で表現できる)
- footerHeight 対応 (`Section` モデルに footerHeight は存在しない — コード確認済み)

## Impact

- 公開 API 変更なし・可逆 (preflight 関数の追加のみ)
- headerHeight 変化時の更新コストは Full (全行照合・再描画は差分行のみ)。可視性 preflight と同型で、設定画面の想定利用 (高さの動的変更は稀) では実害なし
- 蒸留時の申し送り: concepts の [display-state-synchronization.md](../../concepts/core/architecture/display-state-synchronization.md)「Section header の固定高さ」節の「DSL 経由では表示が更新されない」記述と、[declarative-ui-bridge.md](../../concepts/core/architecture/declarative-ui-bridge.md)「既知の非対応の例」の追随更新が必要

## 級: M

2 platform 跨ぎ + ADR-0018 対称テスト義務の初適用として verify で仕様対応を追跡する価値があるため (コード差分自体は小)。

domain: core
