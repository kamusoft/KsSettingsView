# Verification Report: refactor-display-state-sync

検証日時: 2026-06-03

## Summary

| Dimension    | Status |
|--------------|--------|
| Completeness | 25/29 タスク完了。7 章（実機/シミュレータ操作 4 件）は実機操作が必要なため未チェック（正当）。全 Requirement / Scenario に対応するコードが確認済み。 |
| Correctness  | 全中核要件（構造同期=id 同一性のみ MUST NOT / 内容更新=同一セルの部分更新 MUST）が Android / iOS 両プラットフォームに正しく実装されている。全ユニットテスト PASS。 |
| Coherence    | design.md Decision 1〜5 すべてに実装が整合。`add-cell-types-basic` 暫定対処の根本修正も完了。 |

## 検証コマンド結果

| コマンド | 結果 |
|----------|------|
| `cd android && ./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:testDebugUnitTest` | **PASS**（135 tasks UP-TO-DATE, BUILD SUCCESSFUL） |
| `cd ios && swift test` | **PASS**（130 tests, 0 failures） |

## 検証詳細

### Completeness（タスク完了）

- **章 0〜6 + 章 8 のすべてのチェックボックスが `[x]` であることを確認**。
- **7 章（実機/シミュレータ操作 4 件）は `[ ]` のまま**。tasks.md のコメント（「実機操作が必要なため実装フェーズでは未チェックのまま残す」）に従った正当な未完了であり、CRITICAL 扱いとしない。
- Spec 全 Requirement（settings-view-core 2 件、settings-view-android-ui 3 件、settings-view-ios-ui 2 件）に対応するコードが実装済みであることを確認。

### Correctness（要件実装の正確性）

#### 中核要件: 「構造同期は id 同一性のみ（MUST NOT 内容等価性使用）」

- **Android `areContentsTheSame`**:
  `KsSettingsListAdapter.kt:222-228` — `areContentsTheSame` は `return true` のみ。`oldItem == newItem` 等の内容比較を一切用いていない。仕様に適合。
- **Android `getItemId`**:
  `KsSettingsListAdapter.kt:90-98` — `cell.id` / `sectionId` を FNV-1a でハッシュ化した id ベース安定 ID を返す。`hashCode()`（内容依存）は不使用。`CELL_ID_OFFSET=100L` で予約値 1L/2L と非衝突。仕様に適合。
- **iOS `KsCellID`**:
  `KsCellID.swift:72-78` — `== / hash(into:)` は `id（UUID）のみ` を使用。内容ハッシュを含まない。仕様に適合。
- **iOS `DiffableDataSource`**:
  `KsSettingsViewController.swift:104` — `UICollectionViewDiffableDataSource<UUID, KsCellID>` で定義。`KsCellID` が id 限定識別子のため、内容変化で snapshot の item 集合・順序が変わらない。仕様に適合。

#### 中核要件: 「内容更新は同一セルの部分更新（MUST NOT セル破棄・再生成）」

- **Android `applyDiff(ReplaceCell)`**:
  `KsSettingsView.kt:329-354` — 内部 root を更新後、`mainListAdapter.submitContentUpdate(newList, cellId)` を呼ぶ。`submitContentUpdate` は `areContentsTheSame=true` で DiffUtil が差分なしと判定した後、`notifyItemChanged(position)` で当該行のみ部分再 bind する。`submitList` による行差し替え（フルリバインド）を起こさない。仕様に適合。
- **iOS `applyReplaceCell`**:
  `KsSettingsViewController.swift:845-893` — `cellIndex[cellID] = new` でモデル更新後、`snapshot.reconfigureItems([cellID])` で適用。`reloadItems` は使用していない。`if #available(iOS 15.0, *)` の防御的ガードで iOS 15 未満では `reloadItems` にフォールバック（MAY 条項に適合）。仕様に適合。
- **iOS Theme 更新**:
  `KsSettingsViewController.swift:1024-1035` — `reconfigureItems(snapshot.itemIdentifiers)` で全セルを部分更新。`reloadItems` を用いない。仕様に適合。

#### Android DSL: 「内容変化で ReplaceCell を発行しない（MUST NOT）」

- `DSLDiffCalculator.kt:171-207` — `cellLevelDiffs` は Insert/Remove/Move のみ発行。内容変化（`oldCell != cell`）に対し `ReplaceCell` を発行していない。
- `DSLDiffCalculator.kt:251-269` — `contentUpdates` が同一 id で内容が異なる Cell を列挙し、Compose ラッパ（`KsSettingsViewComposable.kt:158-161`）が `store.replaceCell` → `applyDiff(ReplaceCell)` → `notifyItemChanged` の部分更新経路へ流す。仕様に適合。

#### iOS DSL: 「replaceCell は reconfigure 経路（内容更新）として扱う」

- `DSLDiffCalculator.swift:214-221` — 内容変化時（`AnyHashable(oldCell) != AnyHashable(cell)`）に `.replaceCell` を発行。この `.replaceCell` は `applyReplaceCell` → `reconfigureItems` 経路に載せる（構造 snapshot は変更しない）。仕様（spec.md § DSL → SettingsRootDiff 算出ロジック 手順5）に適合。

#### チェック系 TwoWay（MUST）

- **CheckboxCellViewHolder**: `CheckboxCellViewHolder.kt:77-80` — セルタップで `checkBox.toggle()` を呼び、`OnCheckedChangeListener` 経由で `onValueChanged` を発火。`submitList` / DiffUtil を経由しない。仕様に適合。
- **SwitchCellViewHolder**: `SwitchCellViewHolder.kt:78-81` — セルタップで `switchView.toggle()`、`OnCheckedChangeListener` 経由で `onValueChanged` を発火。仕様に適合。
- **SimpleCheckCellViewHolder**: `SimpleCheckCellViewHolder.kt:57-64` — セルタップで `checkView.isChecked` を直接トグルし `handler?.invoke(newValue)` を発火。仕様に適合。

#### RadioCell グループ連動（MUST NOT グループ全体再生成）

- **RadioCellViewHolder**: `RadioCellViewHolder.kt:60-71` — タップで `checkView.isChecked = true` + `handler?.invoke(value)` を発火。旧選択セルの解除は `contentUpdates` 経路（`selectedValue` 変化を検出）→ `notifyItemChanged` の部分更新で反映される。グループ全体の `ReplaceCell` は発行しない。仕様に適合。

#### 値型の equals/hashCode 契約（Decision 2）

- `CheckboxCell.kt`, `SwitchCell.kt`, `RadioCell.kt`, `SimpleCheckCell.kt` — 内部状態（`isChecked`, `isOn`, `selectedValue`）を equals に含める素直な値型に統一。クロージャ（`onValueChanged`, `onSelected`）のみ除外。`add-cell-types-basic` の暫定対処（内部状態除外）を正しく撤回。仕様に適合。

#### SettingsRootDiff.replaceCell の意味論（Decision 5）

- `SettingsRootDiff.kt:36-62`（Kotlin）、`SettingsRootDiff.swift:34-47`（Swift）— `replaceCell` に「同一 id の内容更新（reconfigure / 部分更新）を意味し、セルの破棄・再生成を意味しない」旨の KDoc / doc コメントが明記されている。仕様に適合。

### Coherence（設計整合）

- **Decision 1（二層分離）**: Android / iOS 両プラットフォームで構造同期（id 同一性のみ）と内容更新（部分更新）が分離して実装されている。
- **Decision 2（equals 維持）**: 値型は全フィールド比較を維持し、diff 層は内容等価性を構造同期に使わない設計となっている。
- **Decision 3（Android 内容更新経路）**: DSLDiffCalculator が内容変化で ReplaceCell を発行せず、contentUpdates 経由で notifyItemChanged の部分更新を行う経路が実装されている。
- **Decision 4（iOS reconfigureItems）**: `applyReplaceCell` / `applyUpdateTheme` が `reconfigureItems`（iOS 15+）を使用し、防御的フォールバックが設けられている。
- **Decision 5（replaceCell 意味論）**: core 型に明文化。UI 層が reconfigure 経路で処理。

## 最終判定

CRITICAL なし / WARNING なし / SUGGESTION なし。

**判定: VALID**

全タスク（実機操作 7 章を除く）が実装済みであり、仕様の全中核要件（構造同期は id 同一性のみ MUST NOT 内容等価性使用、内容更新は同一セルの部分更新 MUST）が Android / iOS 両プラットフォームに正確に実装されている。全ユニットテスト PASS。
