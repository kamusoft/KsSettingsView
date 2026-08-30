---
type: concept
title: Native Host の責務境界
description: Core の設定状態を各 platform の Native list へ接続し、宣言 UI から再利用する共通境界
tags: [architecture, native-ui, host, lifecycle]
timestamp: 2026-07-19
---

この文書は、iOS の `KsSettingsViewController` と Android の `KsSettingsView` に共通する Native Host の責務を説明する。読むと、Store、完全な model、visible projection、Native 表示構造、Renderer Registry の境界が分かる。

## Cell の3層

このカテゴリでは、似た名前の3層を次のように区別する。

| 用語 | 意味 |
|---|---|
| Core の Cell 抽象 | ID を通して設定ツリーへ参加する最小 interface / protocol |
| Cell model | Core の抽象へ準拠し、UI 層が定義する具象の値。title、callback、`CellStyle` など Cell 種別固有の内容を持てる |
| Native cell | Cell model を画面へ描画する再利用可能な `UICollectionViewCell` または Android `CellViewHolder` |

`SettingsRoot` は具象の Cell model を Core の Cell 抽象として保持する。Core の抽象自体は style 型を要求せず、UI 層の具象 Cell model が必要に応じて `CellStyle` を持つ。以降、単に model と書く場合は hidden 要素を含む `SettingsRoot` 全体、Native cell と書く場合は platform の再利用行を指す。

## 目的

Native Host は、Core の `SettingsRoot` と UI 層の `SettingsRootStore` を platform 標準の list へ接続する描画境界である。Native UI から直接利用できるだけでなく、SwiftUI は `UIViewControllerRepresentable`、Compose は `AndroidView` を内部で使って同じ Host を再利用する。

| 境界 | iOS | Android |
|---|---|---|
| 公開 Host | `KsSettingsViewController` | `KsSettingsView` |
| Native list | `UICollectionView` | `RecyclerView` |
| 表示構造 | Diffable Data Source の snapshot | Section H/F と Cell の平坦 list |
| Cell 描画の解決 | `KsCellRegistry` → `KsCellRenderer` | `KsCellRegistry` → `CellViewHolder` factory |

## 責務境界

Host は、hidden 要素を含む完全な model、表示対象だけの visible projection、Native 表示構造、Store 購読のライフサイクルを整合させる。空の `SettingsRoot` も有効な入力として扱う。

次は Host の外側に置く。

- `SettingsRoot` / `Section` / `Cell` と `SettingsRootDiff` の定義は Core が担う。
- 状態保持と更新通知は `SettingsRootStore` が担う。
- 宣言ツリー同士の比較と identity 解決は SwiftUI / Compose Bridge が担う。
- Theme と CellStyle の実効値解決は UI 層の style resolution が担う。
- Cell 種別固有の描画は Registry が解決した Renderer / ViewHolder が担う。

Root Header / Footer と Theme は画面側の状態であり、Core の `SettingsRoot` に含めない。Section Header / Footer は `Section` の一部として model に保持する。

## ライフサイクル

Host の Store 購読は画面のライフサイクルへ結び付け、長命な Store から画面を延命しない。iOS は Controller の破棄時に購読を解放し、Android は `ViewTreeLifecycleOwner` の `lifecycleScope` を使い detach 時に購読と Adapter 参照を解放する。

宣言 UI の View identity が続く間は同じ Host と Store を再利用し、再評価ごとに Native list を作り直さない。

## 保証すること

- Native list、visible projection、Cell 描画を宣言 UI ごとに重複実装しない。
- hidden 要素を含む model と visible projection を別の状態として扱う。
- Native 表示構造の identity には Section / Cell の安定 ID を使う。
- Cell 種別を追加しても Host 本体へ型分岐を増やさず、Registry へ登録する。
- Theme 更新は設定ツリーの構造を変えず、各 platform が対応する現在の表示属性だけを再評価する。

## してはいけないこと

- hidden 要素を完全な model から削除して可視性を表現しない。
- Cell の内容値を Native snapshot / stable item ID に含めない。
- Store の状態保持、宣言ツリー比較、style 解決を Host へ取り込まない。
- Root Header / Footer や Theme を `SettingsRoot` のフィールドとして扱わない。

## 関連

architecture の全体像から読む場合は、この文書の後に [Store](store-and-update-streams.md) → [表示状態同期](display-state-synchronization.md) → [宣言 UI Bridge](declarative-ui-bridge.md) → [宣言ツリー identity](declarative-tree-identity.md) → [Renderer Registry](cell-renderer-registry.md) の順で読む。styling は [スタイルの所有と実効値解決](../styling/style-resolution.md)、repository / conventions は [リポジトリとビルドの責務境界](repository-boundaries.md) を入口にする。

- [Store の状態と更新通知](store-and-update-streams.md)
- [表示状態同期](display-state-synchronization.md)
- [Cell Renderer Registry](cell-renderer-registry.md)
- [宣言 UI と Native Host の Bridge](declarative-ui-bridge.md)
- [iOS Native Host](../platforms/ios-native-host.md)
- [Android Native Host](../platforms/android-native-host.md)
