---
type: concept
title: 宣言 UI と Native Host の Bridge
description: SwiftUI・Compose の宣言状態を Store と Native Host の共通更新経路へ接続する境界
tags: [architecture, declarative-ui, swiftui, compose]
timestamp: 2026-07-19
---

この文書は、SwiftUI / Compose Bridge に共通する Store 方式と DSL 方式の収束を説明する。読むと、状態の所有者、宣言ツリー再評価、値と callback の橋渡し、Native Host との責務分担が分かる。

## 二つの利用方式

| 方式 | Store の所有者 | 向く用途 | 更新主体 |
|---|---|---|---|
| DSL | Bridge 内部 | 静的・中規模の一般的な設定画面 | SwiftUI / Compose の状態から宣言ツリーを再評価 |
| Store | 利用者 | 大量データ、高頻度更新、命令型の部分操作 | 利用者が `SettingsRootStore` の公開操作を呼ぶ |

両方式は別の描画基盤を持たず、`SettingsRootStore → Native Host` の同じ更新経路へ収束する。

| 境界 | SwiftUI | Compose |
|---|---|---|
| Native wrapper | 内部 `UIViewControllerRepresentable` | `AndroidView` |
| DSL 状態保持 | View identity に結び付く内部 Store | `remember` した内部 Store |
| Native 更新境界 | Representable の update | `AndroidView.update` |
| 動的 collection | DSL `ForEach` | DSL `forEach` |

## 宣言ツリー方式

Bridge は宣言 UI の identity が続く間、内部 Store と前回の resolved tree を保持する。再評価した tree との差を構造、内容、可視性、Theme に分け、Store の対応する更新へ渡す。

宣言評価中に Native View や Store を直接変更しない。SwiftUI の Representable update と Compose の `AndroidView.update` が、宣言状態を Native 側へ反映する境界になる。

Store の初期値を一度構築する API と、画面 state の変化ごとに再評価される DSL は別物である。

| platform | Store 初期値の構築 | 再評価される画面 DSL |
|---|---|---|
| iOS | `SettingsRoot(sections:)` と `KsSection` で値を作り、利用者所有 `SettingsRootStore` へ渡す | `KsSettingsView { ksSection { ... } }` の `KsSettingsViewBuilder`。SwiftUI の View 更新で再評価 |
| Android | `settingsRoot { section(id = ...) { cell(...) } }` の `SettingsRootScope` で値を作り、利用者所有 Store へ渡す | `KsSettingsView { Section(...) { ... } }` の `DSLSettingsRootScope`。Recomposition で再評価 |

たとえば外部 state の値が変わると、宣言フレームワークが画面 DSL を再評価し、Bridge が新しい resolved tree を作る。Bridge は前回 tree との差を Store 操作へ変換し、Store 通知を受けた Host が Native 表示を更新する。したがって DSL の評価 closure 自体では Store 操作や Native View 変更を行わない。

## 状態の橋渡し

宣言 UI が所有する state は、評価時点の値として不変な Cell へ写す。ユーザー操作は Cell の callback から宣言 UI 側の state へ戻す。Cell 自身は `Binding` / `MutableState` を永続状態として所有しない。

state 値の変化は同じ ID の内容更新であり、宣言ツリーの identity を変えない。可視性変化は内容更新ではなく full 更新へ流す。

## 責務境界

- Bridge は宣言状態、resolved tree、Store 操作、値と callback の相互変換を担う。
- Native list、visible projection、Cell 描画、style resolution は Native Host と UI 層が担う。
- Root Header / Footer、Theme、list style は画面側の指定として扱い、Core の `SettingsRoot` へ混在させない。

## 保証すること

- DSL 方式と Store 方式を同じ Store / Host 更新経路へ収束させる。
- 宣言 UI の identity が続く間、内部 Store と前回 tree を維持する。
- 同じ ID の内容変更を構造上の remove + insert にしない。
- Theme 更新を構造 Diff へ混ぜない。

## してはいけないこと

- SwiftUI / Compose 専用の Cell Renderer と Native list を別実装しない。
- 宣言評価中に Native View や Store を直接変更しない。
- Cell に宣言フレームワークの mutable state object を永続保持させない。
- Store 初期値の構築 API と、再評価される画面 DSL を同じ API / scope として説明しない。

## 関連

- [宣言ツリーの安定 identity](declarative-tree-identity.md)
- [表示状態同期](display-state-synchronization.md)
- [Native Host の責務境界](native-host-boundary.md)
- [iOS SwiftUI Bridge](../platforms/ios-swiftui.md)
- [Android Compose Bridge](../platforms/android-compose.md)
