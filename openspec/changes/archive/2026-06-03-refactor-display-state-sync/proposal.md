## Why

現在の KsSettingsView は、差分検出（Android: `DiffUtil`、iOS: `UICollectionViewDiffableDataSource`）が **Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）まで比較**している。具体的には：

- Android: `KsSettingsListAdapter.areContentsTheSame` が `oldItem == newItem`（= `Cell.equals` の全フィールド比較）に委譲し、`getItemId` が内容依存の `hashCode` を使う。Compose 層 `DSLDiffCalculator` が `oldCell != cell`（equals）で `ReplaceCell` を発行する。
- iOS: DSL の Diff 算出が `KsCell` の `Hashable`（`Equatable`）契約で内容を比較し、`replaceCell` → `snapshot.reloadItems([cellID])`（セル破棄＆再生成）を行う。

この結果、チェック ON/OFF・スイッチ操作・値の更新など**内容が変わるたびに** `areContentsTheSame=false` / `reloadItems` となり、`ReplaceCell` → ViewHolder/CellView の**フルリバインド（行全体の再生成・再 bind）**が走る。実機（Pixel 6a）でチェック操作時に行全体が再描画されて**描画が乱れる（ちらつく）**ことが確認された。

これは設計上の役割分担の誤りである。**diff / snapshot は「データソースとアダプタの構造同期」のためのもの**であり、Cell の Add / Delete / Move / 差し替え（セルそのものの増減・並べ替え・id 変化）を検出するのが役目で、本来は **id の同一性比較だけで十分**である。Cell の**内容（プロパティ）を diff 比較に使うのは責務の誤り**であり、それが ReplaceCell/reloadItems を誘発して描画を乱す。

移植元 `AiForms.Maui.SettingsView` の Android 実装はこの分離を正しく行っている：
- `SettingsViewRecyclerAdapter.GetItemId(position) => position`（内容に依存しない構造的 ID）
- `DiffUtil` の内容比較を使わず、`CellPropertyChanged` を購読して `NotifyItemChanged(index)` で**該当セルだけを部分更新**
- `CheckboxCellView.RowSelected` で `_checkbox.Checked = !_checkbox.Checked`（View 自身を直接トグル）、`OnCheckedChanged` でモデルへ書き戻す（TwoWay）

MAUI ではこの「内容更新」を `Binding` が担う。Native では **ViewHolder（Android）/ CellView（iOS）が同じ役割**を担い、セルを作り直さずに同一セルの中身だけを更新できる（`NotifyItemChanged` / `reconfigureItems`）。

本変更提案は、この設計原則を iOS / Android 共通の確定仕様として定め、両プラットフォームの実装を根本修正する。`add-cell-types-basic` で入れたちらつきの暫定対処（`equals`/`hashCode` から内部状態を除外）は対症療法であり、本提案の根本修正に置き換える。

## What Changes

### 共通原則（settings-view-core）
- 「表示状態同期の二層分離」原則を追加：**(1) 構造同期（diff/snapshot）は id 同一性のみ**で行い、**(2) 内容更新はセル（ViewHolder/CellView）を作り直さず同一セルを部分更新する**ことを規定する。
- 既存「Hashable / equals 契約」Requirement を Modify：値型の `equals`/`hashCode`/`Hashable` 契約自体は維持（一般的な値比較・テストのため）しつつ、**「差分検出（diff/snapshot の構造同期）がこの内容等価性を用いてはならない（id 同一性のみを用いる）」**ことを明文化する。「フィールド変更後は等しくない」Scenario は値型の性質としては維持するが、それを diff の構造同期判定に使わないことを別 Scenario で規定する。
- `SettingsRootDiff` の `replaceCell` の意味を明確化：**`replaceCell` は「同一 id のセルの内容更新（reconfigure / 部分更新）」を表し、セルの破棄＆再生成（フルリバインド）を意味しない**ことを規定する。

### Android（settings-view-android-ui）
- 「DiffUtil 差分検出」Requirement を Modify：
  - `areItemsTheSame` = id 比較（維持）
  - **`areContentsTheSame` = 同一 id なら常に `true`**（内容比較を廃止）。`equals` 委譲をやめる。
  - **`getItemId` = id ベースの安定 ID**（内容依存の `hashCode` を廃止）。
- 「Compose ラッパ／DSL → Diff 算出」相当を Modify：`DSLDiffCalculator` は**内容変化では `ReplaceCell` を発行しない**（構造変化＝Add/Delete/Move/id 変化のみ）。内容更新は ViewHolder への部分更新経路で扱う。
- 「CellViewHolder 抽象」Requirement を Modify：ViewHolder は最新 Cell を参照して**部分更新（reconfigure 相当）**できる経路を持つ。内容変化は `notifyItemChanged`（または同等の部分 bind）で同一 ViewHolder を更新し、フルリバインドによる再生成を避ける。
- チェック系（Switch/Checkbox/Radio/SimpleCheck）の TwoWay：ViewHolder が View を直接トグルして `onValueChanged` でモデルへ書き戻す。RadioCell のグループ連動（他セルの選択解除）は該当セルの部分更新で行い、Replace を介さない。

### iOS（settings-view-ios-ui）
- 「DiffableDataSource」Requirement を Modify：snapshot のアイテム識別子は `KsCellID`（id ベース、既存）を維持し、**内容変化では `reloadItems`（セル破棄＆再生成）ではなく `reconfigureItems`（iOS 15+、同一セルの再構成）を用いる**。内容更新は `cellIndex[KsCellID: any KsCell]` の最新 Cell を参照して同一セルを再構成する。
- 「DSL → SettingsRootDiff 算出ロジック」Requirement を Modify：内容変化では `replaceCell`（reload を誘発する旧経路）を発行せず、`reconfigure` 経路に載せる（構造変化のみ snapshot を操作）。
- チェック系の TwoWay：CellView が自身の状態を直接更新して `onValueChanged` でモデルへ書き戻す。Radio グループ連動も同様にセル単位の reconfigure で行う。

### 実装
- Android（core / ui / compose）と iOS（core / ui）の両方を本提案で実装し、Pixel 6a 実機および iOS シミュレータ／実機で「内容変化時に行全体が再描画されない（ちらつかない）」「チェック/スイッチ/ラジオが正しくトグル・選択反映される」ことを確認する。
- `add-cell-types-basic` の暫定対処（equals から内部状態除外）は本提案の根本修正で置き換える。

## Capabilities

### Modified Capabilities
- `settings-view-core`: 「表示状態同期の二層分離」原則を ADDED、「Hashable / equals 契約」を MODIFIED（diff は内容等価性を使わない旨を追加）、`SettingsRootDiff` の `replaceCell` 意味論を MODIFIED（reconfigure であってフルリバインドでない）。
- `settings-view-android-ui`: 「DiffUtil 差分検出」を MODIFIED（areContentsTheSame=id のみ true / getItemId=id ベース）、DSL→Diff 算出と CellViewHolder の内容更新経路を MODIFIED（内容変化で ReplaceCell を発行せず部分更新）。
- `settings-view-ios-ui`: 「DiffableDataSource」を MODIFIED（reloadItems→reconfigureItems）、「DSL → SettingsRootDiff 算出ロジック」を MODIFIED（内容変化で replaceCell を発行せず reconfigure）。

## Impact

- 影響範囲：
  - Android: `KsSettingsListAdapter`（areContentsTheSame / getItemId）、`DSLDiffCalculator`（ReplaceCell 発行条件）、`CellViewHolder` 派生（部分更新経路）、Switch/Checkbox/Radio/SimpleCheck の各 ViewHolder（TwoWay・グループ連動）
  - iOS: `KsSettingsViewController`（reloadItems→reconfigureItems）、DSL Diff 算出、各 CellView（TwoWay・グループ連動）
  - core: `SettingsRootDiff` の意味論ドキュメント、Hashable 契約の diff 利用に関する規定
- リスク：差分アニメーション（追加/削除/移動）の挙動は構造変化のみで維持されるが、内容更新のアニメーション有無が変わる可能性。実機で要確認。
- 依存：`add-settings-view-core` / `add-settings-view-android-ui` / `add-settings-view-ios-ui` / `add-declarative-dsl` / `add-cell-types-basic`（暫定対処を置換）。
- 後続：`add-cell-types-input` / `add-cell-types-custom`（本提案で確立する内容更新原則に従う）。

🤖 Generated with [Claude Code](https://claude.com/claude-code)
