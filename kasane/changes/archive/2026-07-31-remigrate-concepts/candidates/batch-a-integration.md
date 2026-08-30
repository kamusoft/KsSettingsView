# Batch A 統合結果

## 統合方針

`settings-view-core` と `cell-types-basic` の抽出候補を、読者が必要とする公開契約のまとまりで5概念へ再編した。

| 統合ドラフト | 元候補 | 判断 |
|---|---|---|
| `core-model/settings-tree.md` | 設定ツリーと Accessory | `SettingsRoot`、`Section`、Cell 抽象、Accessory を1つの公開モデルとして説明する |
| `core-model/structural-changes.md` | 設定ツリーの構造変更 | `SettingsRootDiff` の変更語彙と適用側との境界を独立させる |
| `cells/basic-cells.md` | 基本 Cell の意味と公開 API | 基本7種を同じ状態所有・callback 契約としてまとめる |
| `cells/input-cells.md` | 入力 Cell の意味と公開 API | 実装済みだが旧確定 spec に未回収の入力5種を独立した公開 API として残す |
| `cells/ks-image.md` | Cell 用画像の公開契約 | 画像の判別ケース、同一性、解決不能時の fallback を独立境界として残す |

「表示状態同期」は Core の `replaceCell` 契約に必要な範囲を `structural-changes.md` に残し、iOS / Android の実装経路を含む独立した architecture 概念は Batch B/C 後の Batch D 横断統合へ送る。現時点で確定すると、後続 platform 候補との重複や粒度不一致を招くためである。

## ADR 候補の扱い

Core candidate の4候補は、既存 accepted ADR に包含されているため新規 ADR は起こさない。

| 候補 | 既存 ADR |
|---|---|
| 構造・内容・可視性の三分離 | ADR-0010 |
| Root / Section Accessory 境界 | ADR-0005 |
| Theme・CellStyle の UI 層分離 | ADR-0009 |
| 拡張可能な Cell 抽象 | ADR-0013 |

## drift 所見

解消方向は決めず、オーナーレビュー対象として保持する。

1. 旧 spec / docs は Core を「プラットフォーム型を含まない」と説明するが、現行 `KsAnyView` は SwiftUI / UIKit / Compose / Android View を公開境界で受け取る。現行境界は「描画と style の責務を持たない」である。
2. `docs/core-model.md` の iOS `Section.cells: [AnyCell]` は現行 `[any KsCell]` と不一致で、`AnyCell` は存在しない。
3. `docs/core-model.md` の Kotlin `CellTitleAlignment` は小文字だが、現行 case は `START` / `CENTER` / `END`。
4. 旧 spec は Compose の基本 Cell DSL が `id` 引数を持つとするが、現行は戻り値の `.cellID(...)` で明示 ID を設定する。
5. 旧 spec の `RadioCell` 例には必須の `groupId` / `selectedValue` を欠くものがある。
6. `docs/cells.md` / `docs/platform-guide-ios.md` の基本 Cell `Binding` initializer 例は現行 API に存在しない。基本 Cell は値 + callback で使う。
7. `docs/platform-guide-android.md` は二値・選択 Cell 全般に `MutableState` overload があるように説明するが、基本 Cell では `SwitchCell` のみが該当する。

## 見送った情報

- Renderer / ViewHolder の内部 bind 手順、レイアウト寸法、全メソッド一覧はコードから再導出しやすいため記載しない。
- 旧 docs の `TextPickerCell` は現行コードに存在しないため移行しない。
- callback / formatter のインスタンス比較や hash 実装の逐語説明は、利用者が必要とする「callback は値等価から除外される」という保証だけに縮約した。

## 初見可読性レビューの反映

`batch-a-readability-review.md` の必須6件・推奨5件を確認し、Root Accessory 所有、visible projection、独自 Cell 登録、Diff の payload と事前条件、状態更新経路、Picker 選択モード、値域前提、CellStyle の意味、Radio 再タップの platform 差を本文へ反映した。

相対リンクの変更要求だけは採用しない。`ksn-core` が概念間リンクを `concepts/` ルートからの相対パスで記述すると定めているため、candidate 配下での一時的な実配置ではなく、確定後の規約上のパスを維持する。

再レビューで残った move index 基準、TwoWay overload の適用範囲、`NumberPickerCell.step` fallback と事前条件の矛盾も修正した。
