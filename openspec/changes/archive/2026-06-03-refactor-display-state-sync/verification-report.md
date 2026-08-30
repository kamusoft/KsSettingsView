# Verification Report: refactor-display-state-sync

検証日時: 2026-06-03

## Summary

| Dimension    | Status |
|--------------|--------|
| Completeness | 0〜6 章・8.1〜8.4 完了（25/29 タスク）。7 章（実機・シミュレータ目視検証 4 件）は実機/シミュレータ操作が必要なため未チェック（オーナー/オーケストレーター側で実施） |
| Correctness  | core / Android UI / Android Compose / iOS UI / iOS SwiftUI の全 Requirement / Scenario を実装に反映。全ユニットテスト PASS |
| Coherence    | 「表示状態同期の二層分離」原則（構造同期=id 同一性のみ／内容更新=同一セルの部分更新）を両プラットフォーム共通で確立。`add-cell-types-basic` の暫定対処を根本修正で置換 |

## 検証コマンド結果

| コマンド | 結果 |
|----------|------|
| `cd android && ./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:testDebugUnitTest` | **PASS**（全モジュール BUILD SUCCESSFUL） |
| `cd ios && swift test` | **PASS**（123 tests, 0 failures） |
| `cd ios && swift build --build-tests` | **PASS**（UIKit-only テストターゲット含めコンパイル成功。`ApplyDiffTests` は `#if canImport(UIKit)` のため macOS ホストでは実行されないが、コンパイルは通る） |
| `openspec validate refactor-display-state-sync --strict` | **PASS**（`Change 'refactor-display-state-sync' is valid`） |

## iOS Deployment Target 判定（タスク 0.2）

- `ios/Package.swift` の `platforms` は `.iOS(.v16)`。
- **iOS 16+ のため `reconfigureItems`（iOS 15+）は常に利用可能**。フォールバック（`reloadItems`）は必須ではないが、spec の MAY 条項に従い `applyReplaceCell` / `applyUpdateTheme` を `if #available(iOS 15.0, *)` で防御的にガードし、不可時のみ `reloadItems` にフォールバックする分岐を用意した。

## 実装サマリ（章ごと）

### 0. 事前確認
- オリジナル `SettingsViewRecyclerAdapter.cs`（`GetItemId => position`、`CellPropertyChanged → NotifyItemChanged`）の責務分担を確認。
- iOS Deployment Target を iOS 16+ と判定（上記）。

### 1. core: 表示状態同期の二層分離と意味論
- Kotlin `SettingsRootDiff.ReplaceCell` / Swift `SettingsRootDiff.replaceCell` の KDoc / Swift doc を「同一 id の内容更新（reconfigure）」として明文化（型定義は不変）。
- Kotlin `Cell` / Swift `KsCell` のドキュメントに「差分検出（構造同期）は内容等価性を使わず id 同一性のみで行う」原則を追記。値型の `equals` / `Hashable` 契約自体は維持（core テストは全フィールド比較を継続）。

### 2. Android: 構造同期を id のみに
- `KsSettingsListAdapter.areContentsTheSame` を「同一 id なら常に `true`」に変更（`oldItem == newItem` 委譲を廃止）。
- `KsSettingsListAdapter.getItemId` を内容非依存の id ベース安定 ID（`CellRow=cell.id` / `SectionHeader=sectionId:H` / `SectionFooter=sectionId:F` を 64bit FNV-1a でハッシュ化、`CELL_ID_OFFSET=100L` 加算で予約値 1L/2L と非衝突）に変更。
- `SwitchCell` / `CheckboxCell` / `SimpleCheckCell` の `equals` / `hashCode` に内部状態（`isOn` / `isChecked`）を戻し、`RadioCell` と合わせて素直な値型に統一（クロージャのみ除外）。

### 3. Android: DSLDiffCalculator を構造変化のみに
- `DSLDiffCalculator.compute` を構造同期のみ（Insert/Remove/Move/Section H/F/Root H/F/Theme）に変更し、内容変化での `ReplaceCell` 発行を廃止。早期 return も id ベースの `sameStructure` 判定に変更。
- 内容更新は新設の `DSLDiffCalculator.contentUpdates` が列挙し、Compose ラッパが `store.replaceCell` → `applyDiff(ReplaceCell)` → `KsSettingsListAdapter.submitContentUpdate`（`notifyItemChanged`）の部分更新経路で反映する（`submitList` による行差し替えを起こさない）。

### 4. Android: ViewHolder の TwoWay とグループ連動
- 各チェック系 ViewHolder（Checkbox/Switch/SimpleCheck）は既存の TwoWay（View 直接トグル → `onValueChanged`）が本原則に整合済みであることを確認。`RadioCell` のグループ連動は `contentUpdates` → 部分更新で旧選択セルの ✓ 解除を反映（グループ全体の再生成・構造 ReplaceCell を用いない）。
- `CellViewHolder` 抽象の KDoc に内容更新の部分反映経路を明記。

### 5. iOS: reconfigureItems への移行
- `KsSettingsViewController.applyReplaceCell` を `reloadItems` → `reconfigureItems`（iOS 15+、不可時 `reloadItems` フォールバック）に変更。`cellIndex` 更新後に reconfigure。
- `applyUpdateTheme` も `reloadItems(itemIdentifiers)` → `reconfigureItems(itemIdentifiers)` に変更。

### 6. iOS: DSL Diff 算出と CellView の TwoWay
- iOS DSL は仕様どおり内容変化で `.replaceCell` を発行し続け、`applyReplaceCell` の reconfigure 経路に載せる（構造 snapshot は不変）。ドキュメント・コメントを reconfigure 意味論に更新。
- 各 CellView（Checkbox/Radio/Switch/SimpleCheck）は `tapHandler` → `onValueChanged`/`onSelected` でモデル書き戻し（TwoWay）。表示更新は `.replaceCell` → reconfigure で反映。
- `ApplyDiffTests.test_applyDiff_replaceCell` を強化し、reconfigure 経路で構造同期（item 集合・順序・KsCellID）が不変であること、`cellIndex` の内容が更新されることを検証。

### 8. 仕上げ
- 全ユニットテスト・`openspec validate --strict` PASS（上表）。

## add-cell-types-basic の暫定対処の置換（タスク 8.4）

`add-cell-types-basic` で導入したちらつき暫定対処（`SwitchCell`/`CheckboxCell`/`SimpleCheckCell` の `equals`/`hashCode` から内部状態 `isOn`/`isChecked` を除外、`RadioCell` のみ `selectedValue` を残す不統一）を、本提案の根本修正で置換した:

- **根本原因**: 差分検出（`DiffUtil` / `UICollectionViewDiffableDataSource`）が Cell の内容プロパティまで構造同期の判定に用いていたこと。
- **根本修正**: 構造同期を id 同一性のみに限定（Android: `areContentsTheSame`=同一 id で true / `getItemId`=id ベース、Compose DSL: 内容変化で `ReplaceCell` 非発行。iOS: `reloadItems` → `reconfigureItems`）。内容更新は同一セルの部分更新（Android: `notifyItemChanged` / iOS: `reconfigureItems`）で反映。
- これにより 4 種の Cell すべてを「内部状態を含む素直な値型（クロージャのみ除外）」に統一でき、暫定対処の不統一を解消した。

> 注: `add-cell-types-basic` の提案ドキュメント（design.md 等）は実装フェーズでは変更していない（tasks.md 8.4 の指示に従う）。当該 design.md の追補要否はオーナーがアーカイブ前に別途判断する。

## 未完了・要確認（実機検証）

以下は実機/シミュレータ操作が必要なため未チェック（**実機検証が必要**）:

- **7.1 / 7.2 Android（Pixel 6a 実機, serial=<android-device-serial>）**: Switch/Checkbox/SimpleCheck トグル時に当該ウィジェットのみ変化しちらつかないこと、Radio で選択移動時に旧 ✓ が消え他セルが再描画されないことの目視/スクショ差分確認。
- **7.3 iOS（シミュレータ/実機）**: `reconfigureItems` でセルが破棄されず内容のみ更新（ちらつき非発生）の目視確認。
- **7.4 両プラットフォーム**: Add/Delete/Move の構造アニメーションがリグレッションなく動作することの目視確認。
