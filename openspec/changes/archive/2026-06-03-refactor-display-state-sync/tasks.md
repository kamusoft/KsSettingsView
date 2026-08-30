# tasks — refactor-display-state-sync

## 0. 事前確認

- [x] 0.1 オリジナル `SettingsViewRecyclerAdapter.cs`（`GetItemId => position`、`CellPropertyChanged → NotifyItemChanged`）、`CheckboxCellView.cs`（TwoWay）、`RadioCellView.cs`（グループ連動）を読み、内容更新の責務分担を把握する
- [x] 0.2 iOS の Deployment Target を確認し（`ios/Package.swift` / プロジェクト設定）、`reconfigureItems`（iOS 15+）が利用可能か判定する。利用不可の場合は、タスク 5.2 のフォールバック分岐（`reloadItems`）を実施対象とする（判定結果は実装メモ／PR 説明に記録する。提案ドキュメント design.md は変更しない）。**判定結果: `ios/Package.swift` の `platforms` は `.iOS(.v16)`。iOS 16+ のため `reconfigureItems`（iOS 15+）は常に利用可能。フォールバック（5.2）は必須ではないが `if #available(iOS 15.0, *)` で防御的にガードする（spec の MAY 条項）**

## 1. core: 表示状態同期の二層分離と意味論

- [x] 1.1 `settings-view-core` の仕様に従い、`SettingsRootDiff.replaceCell` の意味論を「同一 id の内容更新（reconfigure）」として確定する（型定義自体は既存のまま。ドキュメントコメント / KDoc / Swift doc を更新）
- [x] 1.2 値型の `equals`/`hashCode`/`Hashable` 契約は維持しつつ、「差分検出の構造同期に内容等価性を用いない」原則をコメント等で明示する
- [x] 1.3 core のユニットテスト：値型の equals は全フィールド比較を維持（「フィールド変更後は等しくない」）、ただし diff の構造同期が id 同一性のみであることは UI 層テストで担保する旨を確認

## 2. Android: 構造同期を id のみに

- [x] 2.1 `KsSettingsListAdapter.areContentsTheSame` を「同一 id（areItemsTheSame=true）なら常に true」に変更し、`oldItem == newItem`（equals 委譲）を廃止する
- [x] 2.2 `KsSettingsListAdapter.getItemId` を Cell / Section の **id ベースの安定 ID** に変更する（内容依存の `hashCode` を廃止。`RootHeaderFooterAdapter` の予約値 1L/2L と非衝突）
- [x] 2.3 `add-cell-types-basic` の暫定対処（`SwitchCell`/`CheckboxCell`/`SimpleCheckCell` の equals/hashCode からの内部状態除外、`RadioCell` の selectedValue 残し）を見直し、Decision 2 に従って素直な値型に統一する（diff が内容等価性を使わなくなるため内部状態を equals に戻してよい）

## 3. Android: DSLDiffCalculator を構造変化のみに

- [x] 3.1 `DSLDiffCalculator` が**内容変化（`oldCell != cell`）では `ReplaceCell` を発行しない**よう変更する（Add/Delete/Move/id 変化の構造差分のみを発行）。早期 return 判定（`from.sections == to.sections`）も id ベースの構造比較に見直す
- [x] 3.2 内容更新の反映経路を実装する：アダプタが id → position を解決し、内容変化時に該当 ViewHolder を部分更新（`notifyItemChanged(position)` による再生成なしの bind 反映、または ViewHolder 直接更新）。`submitList` による行差し替えを起こさない
- [x] 3.3 `DSLDiffCalculatorTest` を更新：内容のみ変化では構造 Diff（ReplaceCell）を発行しないこと、Add/Delete/Move は従来どおり発行することを検証

## 4. Android: ViewHolder の TwoWay とグループ連動

- [x] 4.1 `CheckboxCellViewHolder` / `SwitchCellViewHolder` / `SimpleCheckCellViewHolder`：セルタップで View 自身を直接トグルし `onValueChanged` を発火（TwoWay）。`submitList`/`DiffUtil` を経由しない（既存実装を本原則に整合させる）
- [x] 4.2 `RadioCellViewHolder`：タップで自分を選択状態にして `onSelected(value)` 発火。同一 `groupId` の旧選択セルの解除は、該当セルの部分更新で反映する（グループ全体の再生成・`ReplaceCell` を用いない）
- [x] 4.3 `CellViewHolder` 抽象に、内容更新の部分反映に必要な共通経路（最新 Cell 参照 / 部分 bind）を整える
- [x] 4.4 `BasicCellsTest.kt` を更新：内容変化で `areContentsTheSame=true`・`getItemId` 不変、チェック系の TwoWay トグル、Radio グループ連動が部分更新で行われることを検証

## 5. iOS: reconfigureItems への移行

- [x] 5.1 `KsSettingsViewController` の `replaceCell` 適用を `snapshot.reloadItems([cellID])` から `snapshot.reconfigureItems([cellID])`（iOS 15+）に変更する。`cellIndex` の当該 Cell を更新してから reconfigure する
- [x] 5.2 Deployment Target が iOS 15 未満を含む場合のフォールバック（`reloadItems`）を条件分岐で用意する（0.2 の判定結果に従う）。**Deployment Target は iOS 16+ のため必須ではないが、spec の MAY 条項に従い `if #available(iOS 15.0, *)` で防御的にガードし、不可時は `reloadItems` へフォールバックする分岐を用意した**
- [x] 5.3 Theme 更新（全可視セル再描画）も `reloadItems(itemIdentifiers)` から `reconfigureItems(itemIdentifiers)` に見直す（セル破棄を避ける）

## 6. iOS: DSL Diff 算出と CellView の TwoWay

- [x] 6.1 DSL Diff 算出の手順5を見直し、内容変化での `.replaceCell` は「reconfigure 経路の内容更新」として扱うことを実装・ドキュメントに反映（構造同期 snapshot を変えない）
- [x] 6.2 各 CellView（Checkbox/Switch/SimpleCheck/Radio）：操作時に自身の状態を直接更新し `onValueChanged`/`onSelected` でモデル書き戻し（TwoWay）。Radio グループ連動はセル単位 reconfigure で反映
- [x] 6.3 iOS ユニットテスト：内容変化で `.replaceCell` が reconfigure 経路に載ること、構造変化（insert/delete/move）は従来どおりであることを検証

## 7. 実機・シミュレータ検証

> 注: 以下 4 件は実機/シミュレータ操作が必要なため、実装フェーズでは未チェックのまま残す（**実機検証が必要**。オーナー/オーケストレーター側で実施）。ビルド・全ユニットテストは PASS 済み（§8 参照）。

- [x] 7.1 Android（Pixel 6a 実機, serial=<android-device-serial>）: Sample「基本 Cell 7 種デモ」で Switch/Checkbox/SimpleCheck をトグル → ちらつき非発生をオーナー実機確認済み
- [x] 7.2 Android: Radio で別項目をタップ → 選択が移り旧選択の✓が消える（複数✓にならない）、かつ他セルが再描画されないことを確認（Pixel 6a 実機で Dark→Light→Auto と連続切替し、常に✓が1つだけ移動することを確認。実機確認で発覚した「同一操作で複数セルの内容更新が連続 submitList され AsyncListDiffer が一部の notifyItemChanged を破棄 → 旧✓が残る」不具合は `SettingsRootStore.replaceCells` によるバッチ化（単一 submitList + 複数 notifyItemChanged）で修正済み）
- [x] 7.3 iOS（シミュレータまたは実機）: 同等の操作で `reconfigureItems` によりセルが破棄されず内容のみ更新される（ちらつき非発生）ことをオーナー実機確認済み
- [x] 7.4 両プラットフォームで Add/Delete/Move の構造アニメーションが従来どおり動作することを確認（リグレッションなし）。Add / Delete はオーナー実機で動作確認済み。Move は Sample に該当 UI が無く実機試行不可のため、ユニットテスト（Android: `ApplyDiffTest` / `SettingsRootStoreTest` / `DSLDiffCalculatorTest`、iOS: `ApplyDiffTests` / `SettingsRootStoreTests` / `DSLDiffCalculatorTests`）で構造同期（MoveCell/MoveSection）の発行・適用を担保

## 8. 仕上げ

- [x] 8.1 `cd android && ./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:testDebugUnitTest` 全 PASS
- [x] 8.2 `cd ios && swift test` 全 PASS
- [x] 8.3 `openspec validate refactor-display-state-sync --strict` PASS
- [x] 8.4 本提案で `add-cell-types-basic` の暫定対処（equals からの内部状態除外）を根本修正で置換したことを、PR 説明および本提案の verification-report に記録する（`add-cell-types-basic` の提案ドキュメント design.md の追補が必要な場合はオーナーがアーカイブ前に別途判断する。実装フェーズでは他提案のドキュメントを変更しない）。**verification-report.md に記録済み**

## 依存関係

- 先行：`add-settings-view-core`、`add-settings-view-android-ui`、`add-settings-view-ios-ui`、`add-declarative-dsl`、`add-cell-types-basic`（暫定対処を置換）
- 後続：`add-cell-types-input`、`add-cell-types-custom`（本提案で確立する内容更新原則に従う）

## 完了条件

- 全タスクのチェックボックスが完了している
- 構造同期が id 同一性のみで行われ、内容変化でセルが再生成されない（両プラットフォーム）
- チェック系の TwoWay と Radio グループ連動が部分更新で動作する
- 実機/シミュレータでちらつき解消を確認済み
- 全ユニットテスト成功・`openspec validate --strict` PASS
