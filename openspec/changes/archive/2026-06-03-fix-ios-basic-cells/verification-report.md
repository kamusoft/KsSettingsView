# Verification Report: fix-ios-basic-cells

**Date**: 2026-06-03
**Verifier**: openspec-verify-change

---

## Summary

| Dimension    | Status                                        |
|--------------|-----------------------------------------------|
| Completeness | 19/20 tasks complete (task 7.2 pending/archive) |
| Correctness  | All requirements covered                      |
| Coherence    | Design decisions followed                     |

---

## Completeness

### Task Completion

- Tasks 1.1–6.2: **[x]** 全て完了 (19/20)
- Task 7.1 **[x]**: 完了
- Task 7.2 **[ ]**: アーカイブ時の本体 spec 反映 — これはアーカイブ手順の一部であり、検証時点で未完了は正常。実装 blocking ではない。

### Spec Coverage

#### cell-types-basic

**Requirement: CheckboxCell**
- `KsCheckBoxView.swift` に `cornerRadius = 3`、`borderWidth = 2`、`isChecked`、`accentColor` API が実装されている。
- `draw(_:)` で checked 時に accent 塗りつぶし + 白チェックマーク（座標比 22/52 → 38/68 → 76/30）をストローク、unchecked 時は枠のみ。
- `CheckboxCellView.render()` で `UICellAccessory.customView(configuration: .init(customView: checkBoxView, placement: .trailing()))` を `self.accessories = [checkAccessory]` として常設（追加・削除なし）。
- accent カラーを `cb.accentColor ?? theme.cellAccentColor` で解決して適用。
- `prepareForReuse` で `checkBoxView.isChecked = false` リセット済み。

**Requirement: RadioCell**
- `KsCheckmarkAccessoryView.swift` に alpha フェード（`UIView.animate`）で選択切替、位置不動の実装。
- `RadioCellView.render()` で `self.accessories = [checkAccessory]` として checkmarkView を常設。
- `isInitialBind` フラグで初回 bind は即時 alpha、状態変化時のみ animate。
- `prepareForReuse` で `isInitialBind = true`、`checkmarkView.alpha = 0` リセット済み。

**Requirement: SimpleCheckCell**
- `SimpleCheckCellView.render()` で `content.image` を設定していない（`UIListContentConfiguration.cell()` のみ使用）。
- `checkmarkView`（`KsCheckmarkAccessoryView`）を `placement: .trailing()` で常設。
- `isChecked` を alpha フェードで反映。RadioCell と同等の実装。

#### settings-view-ios-ui

**Requirement: スタイル切替（クラシック/モダン）**
- `makeLayout(for:)` の sectionProvider 内で `section.boundarySupplementaryItems.map` を用いて `elementKind == UICollectionView.elementKindSectionHeader` な item の `pinToVisibleBounds = false` を設定。
- `supplementaryModes(for:)` が `sections.contains { $0.footer != nil }` で footer の有無を判定し、`.none` / `.supplementary` を出し分けている。
- `style` didSet で `rebuildLayout()` を呼び出し、レイアウト再構築済み。
- `appearance(for:)` で `.classic → .plain`、`.modern → .insetGrouped` を変換。

---

## Correctness

### Scenario Coverage

#### cell-types-basic / CheckboxCell

| Scenario | Test | 実装 |
|---|---|---|
| チェック状態の表示（右端角丸チェックボックス・accent 塗りつぶし） | `test_CheckboxCellView_isChecked_trueで角丸チェックボックスがcustomView常設かつchecked()` | `KsCheckBoxView.draw()` |
| 非チェック状態の表示（枠のみ・位置同一） | `test_CheckboxCellView_isChecked_falseでもcustomViewは常設されuncheckedになる()` | `KsCheckBoxView.updateFill()` |
| タップで toggle | `test_CheckboxCellView_タップでtoggleされた値が通知される()` | `tapHandler` 経由 |

#### cell-types-basic / RadioCell

| Scenario | Test | 実装 |
|---|---|---|
| 選択状態の表示（alpha 1） | `test_RadioCellView_value一致でcheckmarkがcustomView常設かつalpha1()` | `KsCheckmarkAccessoryView.apply()` |
| 選択切り替え（onSelected 呼び出し） | `test_RadioCellView_タップでonSelectedにvalueが渡される()` | `tapHandler` 経由 |
| 選択解除時のフェードアウト（位置不動） | `test_RadioCellView_選択切替でaccessory数が変化しない()` | `self.accessories = [checkAccessory]` 常設 |

#### cell-types-basic / SimpleCheckCell

| Scenario | Test | 実装 |
|---|---|---|
| 右端チェック表示（RadioCell 同レイアウト・content.image 不使用） | `test_SimpleCheckCellView_右端customView方式でcontentImageは使われない()` | `UIListContentConfiguration.cell()`、image 未設定 |
| 非チェック時（alpha 0・タイトルのみ） | `test_SimpleCheckCellView_isChecked_falseで右端checkmarkはalpha0()` | `apply(selected: false, ...)` |
| 選択解除時のフェードアウト | `test_SimpleCheckCellView_タップでtoggle通知()` + alpha フェード実装 | `UIView.animate` |

#### settings-view-ios-ui

| Scenario | Test | 実装 |
|---|---|---|
| classic の Appearance = .plain | `test_classicに対応するAppearanceはplain()` | `appearance(for: .classic) → .plain` |
| classic でヘッダーが固定されない（pinToVisibleBounds = false） | （直接 UI テストなし、コードレビューで確認） | `item.pinToVisibleBounds = false` |
| footer なし → footerMode = .none | `test_footerを持たないrootではfooterModeがnoneになる()` | `supplementaryModes(for:)` |
| footer あり → footerMode = .supplementary | `test_footerを持つsectionが1つでもあればfooterModeはsupplementary()` | `supplementaryModes(for:)` |
| modern の Appearance = .insetGrouped | `test_modernに対応するAppearanceはinsetGrouped()` | `appearance(for: .modern) → .insetGrouped` |
| 動的スタイル切替でレイアウト再構築 | `test_動的style切替でレイアウトインスタンスが差し替わる()` | `style.didSet → rebuildLayout()` |
| SwiftUI ラッパでのスタイル指定 | （KsSettingsViewStyleTests には SwiftUI ラッパ直接テストなし） | `KsSettingsView.makeUIViewController` で `style` 渡し |

---

## Coherence

### Design Adherence

- Decision 1（pinToVisibleBounds / footerMode 出し分け）: `makeLayout(for:)` および `supplementaryModes(for:)` で忠実に実装済み。
- Decision 2（KsCheckBoxView / CheckboxCellView）: `KsCheckBoxView` を `UIView` ベースで実装し、customView accessory として常設。`draw()` でオリジナル座標比を使用。
- Decision 3（RadioCellView / KsCheckmarkAccessoryView）: alpha フェード方式で accessory 常設。`isInitialBind` による即時/アニメーション制御も一致。
- Decision 4（SimpleCheckCellView）: RadioCell と共通の `KsCheckmarkAccessoryView` を使用し、`content.image` を廃止。

### Code Pattern Consistency

- `KsCheckBoxView`、`KsCheckmarkAccessoryView` ともにファイル名・型名が既存プロジェクト規約と一致。
- `@MainActor` 属性、`#if canImport(UIKit)` ガード、日本語コメントが既存パターンと一致。
- テスト用アクセサ（`_hasTrailingCheckBoxAccessory` 等）を `internal` で公開するパターンも既存コードと一致。

---

## Issues

### CRITICAL

なし

### WARNING

なし

### SUGGESTION

- **settings-view-ios-ui / Scenario: SwiftUI ラッパでのスタイル指定** のテストが `KsSettingsViewStyleTests` に存在しない。既存テストは `makeUIViewController` を直接呼ぶ範囲に踏み込んでいないが、該当コードは既存 `KsSettingsView.swift` に実装されており、動作上の問題は確認されていない。余力があれば SwiftUI ラッパの `style` 伝達をカバーするテストを追加することを推奨する。

---

## Final Assessment

CRITICAL なし、SUGGESTION 1 件（テスト追加の軽微な推奨）のみ。

**判定: VALID**

タスク 7.2 はアーカイブ手順の一部であり実装を blocking しない。全 Requirement・Scenario が実装でカバーされており、spec delta と実装コードは一致している。`openspec validate fix-ios-basic-cells --strict` 相当で CRITICAL / WARNING は検出されない。
