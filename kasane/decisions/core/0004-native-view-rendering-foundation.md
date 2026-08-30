---
id: 0004
title: Native View を描画基盤として宣言 UI から再利用する
status: accepted
date: 2026-05-10
---

## Context

KsSettingsView は iOS・Android の Native UI に加え、SwiftUI・Compose と将来の MAUI バインディングからも利用される。宣言 UI と Native UI に別々の描画実装を持つと、Cell の追加や描画修正が二重化する。一方、旧実装を踏襲するだけではモダンな差分更新やリスト API を活用できない。

## Decision

iOS は `UICollectionView`、`UICollectionViewDiffableDataSource`、`UICollectionLayoutListConfiguration` を描画基盤とする。Android は Section の Header・Cell・Footer を平坦化した単一の `RecyclerView` / `ListAdapter` とし、`DiffUtil` による差分計算を利用する。

SwiftUI は Native の ViewController を `UIViewControllerRepresentable` で、Compose は Native View を `AndroidView` でラップする。Native UI と宣言 UI は同じ描画基盤を共有する。

Cell 型と Renderer または ViewHolder factory の対応は、各プラットフォームの Cell Registry に集約する。Cell 種類を増やすときは Registry へ登録し、DataSource や Adapter 本体へ型分岐を追加しない。

## Alternatives Considered

- iOS で手動の `UICollectionViewFlowLayout` を使う案は、区切り線や Header の実装をすべて自前で持つ必要があるため採用しない。
- iOS で `UITableView` を使う案は、モダン実装へ刷新する目的と矛盾するため採用しない。
- SwiftUI で `UIViewRepresentable` を使う案は、`UICollectionView` のライフサイクル管理を自前で担う必要があるため採用しない。
- Android で Section ごとに `ConcatAdapter` を分割する案は、Section の追加・削除、ViewType の名前空間、境界判定、将来の Section 間移動を複雑にするため採用しない。Root Header・Footer を外側で扱う `ConcatAdapter` は別の責務である。
- Android で `RecyclerView.Adapter` と `DiffUtil.calculateDiff` を直接扱う案は、差分計算を手動管理する必要があるため採用しない。
- Android を純粋な Compose / `LazyColumn` で実装する案は、MAUI と描画実装を共通化できず保守コストが倍になるため採用しない。
- DataSource や Adapter 内の `switch` / `when` で Cell 型を判定する案は、Cell 追加のたびに基盤を編集する必要があるため採用しない。

## Consequences

正の影響として、Native、SwiftUI・Compose、MAUI が同じ描画実装と Cell 登録機構を共有できる。各プラットフォーム標準の差分更新とライフサイクルを利用しながら、後続の Cell 種類を基盤から独立して追加できる。

負の影響として、宣言 UI は純粋な SwiftUI / Compose 実装ではなく Native View のラッパとなる。Android の平坦リストは Section 境界を項目の所属情報から判定する必要があり、iOS と Android では基盤 API と統合方法が異なる。未登録 Cell は開発時エラーとして扱うため、Cell 追加時の Registry 登録が必須になる。

出典:

- `openspec/changes/archive/2026-05-09-add-settings-view-ios-ui/design.md` Decision 1, 3–4
- `openspec/changes/archive/2026-05-10-add-settings-view-android-ui/design.md` Decision 1–4
