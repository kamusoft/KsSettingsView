---
type: concept
title: 宣言 UI と Native Host の Bridge
description: SwiftUI・Compose の宣言状態を Store と Native Host の共通更新経路へ接続する境界
tags: [architecture, declarative-ui, swiftui, compose]
timestamp: 2026-08-06
---

この文書は、SwiftUI / Compose Bridge に共通する Store 方式と DSL 方式の収束を説明する。読むと、状態の所有者、宣言ツリー再評価、値と callback の橋渡し、Native Host との責務分担、そして両方式の観測結果対称性の契約が分かる。

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

## 両方式の観測結果対称性

Store 方式は利用者が「何が変わったか」を公開操作として明示するため、変化の検出が要らない。一方 DSL 方式は変化が明示されないため、再評価前後の resolved tree を比較して差分を `SettingsRootDiff` 列へ翻訳する検出層 (platform ごとの `DSLDiffCalculator`) を DSL 方式だけが持つ — 前節で「Store 操作へ変換」と述べた変換の実体がこの層である。検出層が翻訳できない変化は diff 0 件となり、エラーを出さずに表示へ反映されない (無音の失敗)。Store 方式の側を修正しても検出層の取りこぼしは残るため、両方式の間に次の契約を置く (core/ADR-0018):

- Store の公開操作で表示へ反映される観測可能な変化は、DSL 経由で同じ変化を与えても同じ表示結果へ到達する。反映しない場合は [表示状態同期](display-state-synchronization.md) に「明示的に非対応」として記録し、意図的な割り切りか未実装かを添える — 無音の取りこぼしは契約違反である
- 対称性は観測結果への契約であり、検出層の実装形 (変化なしと判定する条件・発行する diff の種別) は platform ごとに異なってよい
- 生成後に値を変えると表示へ反映されるプロパティ (動的反映の対象) を追加・修正するときは、Store 経由と DSL 経由の両方へ反映テストを併せて追加する (対称テスト)。片方式だけの修正はレビューで対称テストの欠けとして検出できる (機械的な強制はない)

この契約が検出した欠落の実例: Section header の固定高さはかつて DSL 経由で差分が生成されない無音の取りこぼしとして記録され、のちに両 platform の `DSLDiffCalculator` が可視性と同型の preflight で headerHeight 差を検出し full 更新のみを発行する形で解消された ([表示状態同期](display-state-synchronization.md) の「Section header の固定高さ」節)。

## 状態の橋渡し

宣言 UI が所有する state は、評価時点の値として不変な Cell へ写す。ユーザー操作は Cell の callback から宣言 UI 側の state へ戻す。Cell 自身は `Binding` / `MutableState` を永続状態として所有しない。

state 値の変化は同じ ID の内容更新であり、宣言ツリーの identity を変えない。可視性変化は内容更新ではなく full 更新 ([表示状態同期](display-state-synchronization.md) の用語) へ流す。

## 責務境界

- Bridge は宣言状態、resolved tree、Store 操作、値と callback の相互変換を担う。
- Native list、visible projection、Cell 描画、style resolution は Native Host と UI 層が担う。
- Root Header / Footer、Theme、list style は画面側の指定として扱い、Core の `SettingsRoot` へ混在させない。

## 保証すること

- DSL 方式と Store 方式を同じ Store / Host 更新経路へ収束させる。
- Store 経由で表示へ反映される観測可能な変化を、DSL 経由でも同じ表示結果へ到達させる (非対応は明示的に文書化する — core/ADR-0018)。
- 宣言 UI の identity が続く間、内部 Store と前回 tree を維持する。
- 同じ ID の内容変更を構造上の remove + insert にしない。
- Theme 更新を構造 Diff へ混ぜない。

## してはいけないこと

- SwiftUI / Compose 専用の Cell Renderer と Native list を別実装しない。
- 宣言評価中に Native View や Store を直接変更しない。
- Cell に宣言フレームワークの mutable state object を永続保持させない。
- Store 初期値の構築 API と、再評価される画面 DSL を同じ API / scope として説明しない。
- 動的反映の対象プロパティを、片方式のテストだけで追加・修正しない。

## 関連

- [宣言ツリーの安定 identity](declarative-tree-identity.md)
- [表示状態同期](display-state-synchronization.md)
- [core/ADR-0018 Store と DSL の更新経路間で観測結果の対称性を対称テストで保証する](../../../decisions/core/0018-store-dsl-path-result-symmetry.md)
- [Native Host の責務境界](native-host-boundary.md)
- [iOS SwiftUI Bridge](../../ios/api/ios-swiftui.md)
- [Android Compose Bridge](../../android/api/android-compose.md)
