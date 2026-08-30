## Why

Core モデルと UI 基盤（iOS / Android）が揃った段階で、設定画面で最も使用頻度の高い基本 Cell 群（旧 AiForms.Maui.SettingsView 互換）を Native 両プラットフォームで実装する必要がある。これらの Cell は入力／ピッカー系（`add-cell-types-input` で対応）に依存しないため、独立した変更提案として先に実装する。本変更提案では PoCLabelCell を本格 `LabelCell` に置き換える形になる。

## What Changes

- Native Core モジュール（iOS `KsSettingsViewCore` / Android `ks-settingsview-core`）に `KsImage` 値型を追加（`name`、`url`、`systemName` の各 String? を持ち、Cell の `icon` フィールドで使用される論理型）
- iOS `KsSettingsViewUI` に以下の具象 Cell（Swift `struct`）と対応する `UICollectionViewCell` サブクラスを追加：
  - `LabelCell`：title / description / valueText / icon を表示
  - `CommandCell`：tap 通知あり、Disclosure indicator 表示
  - `ButtonCell`：title をボタンスタイル中央寄せ表示、tap 通知
  - `SwitchCell`：右側に `UISwitch`、`isOn` 双方向通知
  - `CheckboxCell`：右側にチェックマーク（or trailing accessory）、`isChecked` 双方向通知
  - `RadioCell`：単一選択グループ用。`groupId` と `selectedValue` を持ち、tap で `selectedValue` を更新
  - `SimpleCheckCell`：単純なチェックリスト用、tap で `isChecked` トグル
- Android `ks-settingsview-ui` に対応する Kotlin `data class`（`Cell` を継承）と `CellViewHolder` 実装を追加
- 各 Cell に対して `KsCellRegistry` への登録ロジック（`registerBasicCells()` 公開関数）
- PoC `PoCLabelCell` / `PocLabelCell` を削除（`LabelCell` で置換）
- **`add-declarative-dsl` 連動**: すべての具象 Cell に `id` パラメータのデフォルト値（iOS: `UUID = UUID()`、Android: `String = "<className>-${UUID.randomUUID()}"`）を持たせ、DSL 経路で `id` 引数省略を可能にする
- **`add-declarative-dsl` 連動**: Android の各具象 Cell に対応する `DSLSectionScope` 拡張関数（`fun DSLSectionScope.LabelCell(...): CellHandle` 等）を `ks-settingsview-compose` モジュールに追加し、`Section("...") { LabelCell(title = "...") }` のような直置き書き味を提供
- **`add-declarative-dsl` 連動**: Android の各具象 Cell data class は `DSLReidentifiableCell` / `DSLStyleModifiableCell` 規約に準拠（`withDSLId(...)` / `withDSLStyle(...)` 実装）。iOS は `DSLReidentifiable` / `DSLStyleModifiable` 規約に準拠（`withDSLID(_:)` / `withStyle(_:)` 実装）
- 各 Cell の単体テスト：bind 後の表示内容、Theme/CellStyle 適用、SwitchCell の `isOn` 更新通知、CommandCell の tap 通知、DSL 拡張関数経由での Cell 配置検証
- iOS Sample / Android Sample に各 Cell の表示例を追加（最小限、新 DSL 形式で記述）
- iOS Sample / Android Sample から旧 `SampleLabelCell` / `SampleLabelCellView`（iOS）/ `SampleLabelCellViewHolder` / `SampleLabelCellDsl`（Android）/ `SampleLabelCellPreview`（iOS）を削除し、`LabelCell` に置換（Sample 専用 Cell は `internal` PoC Cell に依存できない時期の回避策であり、`LabelCell` 公開後は viewType 競合 / Renderer 重複登録を引き起こすため温存不可）
- **実機レビュー（Pixel 6a, 2026-06-02）由来の Android 改修（Decision 9）**:
  - **ちらつき修正**: `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` の `equals` / `hashCode` から内部状態（`isOn` / `isChecked` / `selectedValue`）を除外し、`KsSettingsListAdapter` に `getChangePayload` + payload 部分 bind を導入。ON/OFF 操作時の行全体フルリバインド（ちらつき）を解消しつつ、親の `submitList` による外部状態更新は payload 経由で反映する
  - **ナビゲーションインジケータのオリジナル準拠**: `CommandCellViewHolder` の Disclosure Indicator を `TextView ">"` からオリジナル `ic_navigate_next.xml` 相当の VectorDrawable（`AppCompatImageView`）に置換
  - **RadioCell / SimpleCheckCell のカスタムチェック描画**: オリジナル `SimpleCheck.cs` の Canvas 描画ロジック（2 本線のチェックマーク）を移植したカスタム View（`KsSimpleCheckView`）を新設し、`RadioCellViewHolder`（標準 RadioButton 廃止）と `SimpleCheckCellViewHolder`（`TextView "✓"` 廃止）の両方で採用してオリジナルに準拠
  - **タッチフィードバック（Ripple）の移植**: 全 Cell の背景を `RippleDrawable` 化し、タップ時の Ripple / 選択ハイライトをオリジナル `CellBaseView.cs` に準拠して表示。色は既存の `Theme.selectedColor`（設定で変更可能）に連動
  - **SwitchCell のセル全体タップ ON/OFF**: スイッチウィジェット直接操作だけでなく、セル本体タップでもトグルするよう変更（Android 標準設定アプリ準拠）
  - iOS 側にも同種のちらつきが実在することを確認済みだが、その修正は本提案の対象外とし、別途 **iOS 実機レビューのタスク**で対応する

## Capabilities

### New Capabilities
- `cell-types-basic`: 基本 Cell 群（Label/Command/Button/Switch/Checkbox/Radio/SimpleCheck）の振る舞いを規定する

### Modified Capabilities
- `settings-view-ios-ui`: 「PoC Cell の存在」要件を REMOVED（具象 Cell の追加により不要化）
- `settings-view-android-ui`: 「PoC Cell の存在」要件を REMOVED（同上）
- `samples-ios`: 「Sample 専用 Cell の定義と登録」要件を REMOVED（`LabelCell` 公開により Sample 独自 Cell が不要かつ viewType 競合の原因となるため）、「`SampleLabelCell` を含むデモ画面」を「基本 Cell を含むデモ画面」に MODIFIED（`LabelCell` ベースに変更、加えて基本 Cell 7 種デモ画面の存在 Scenario を追加）
- `samples-android`: 同上 + 「Material3 派生テーマの使用」要件を ADDED（`ks-settingsview-ui` が `MaterialSwitch` / `AppCompatCheckBox` / `AppCompatRadioButton` / `AppCompatImageView` を使用するため Material3 派生テーマが必須。`Theme.Material.*` / `Theme.AppCompat.*` / `Theme.MaterialComponents.*` では `MaterialSwitch` の `?attr/materialSwitchStyle` が解決できず SwitchCell が描画されないかクラッシュ）。Cell 間罫線描画 Scenario も追加（`onDrawOver` 化により背景上書きされないことを検証）

> 注: 本提案の基本 Cell 7 種の DSL 拡張関数（`fun DSLSectionScope.LabelCell(...)` 等）は `cell-types-basic` capability の Requirement「Compose DSL 拡張関数による Cell 直置き」として規定する。`settings-view-android-ui` capability の Compose DSL Requirement で既に「具象 Cell 型ごとに `DSLSectionScope` の拡張関数として直置き API を提供する規約」が `add-declarative-dsl` で確定済みのため、本提案はその規約に従って実装するのみで、`settings-view-android-ui` 自体の Requirement を本目的では Modify しない（PoC Cell REMOVED の Modification は維持）。

## Impact

- 影響範囲：iOS UI モジュール、Android UI モジュール（`SwitchCellViewHolder` の `MaterialSwitch` 化、`CheckboxCellViewHolder` / `RadioCellViewHolder` の `AppCompatCheckBox` / `AppCompatRadioButton` 化、`ClassicSectionDecoration` の `onDrawOver` 化、`com.google.android.material:material` 依存追加。**Decision 9 で追加**: `KsSettingsListAdapter` への payload 差分（`getChangePayload` / `onBindViewHolder(payloads)`）導入、4 Cell 型の `equals`/`hashCode` からの内部状態除外、`CommandCellViewHolder` の矢印 drawable 化、カスタム View `KsSimpleCheckView` 新設と `RadioCellViewHolder` / `SimpleCheckCellViewHolder` での採用、`ic_navigate_next` 相当の VectorDrawable 追加）、Android Compose モジュール（DSL 拡張関数追加）、両 Sample（旧 `SampleLabelCell` 一族の削除と `LabelCell` への置換、iOS Xcode プロジェクトファイルからの参照削除＋`BasicCellsDemoView.swift` の追加、Android Manifest の `Theme.Material3.*` 必須化と `com.google.android.material:material` 依存追加を含む）
  - なお iOS のちらつき修正は本提案の対象外（別途 iOS 実機レビューのタスクで対応）
- 依存：`add-monorepo-foundation`、`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、**`add-declarative-dsl`**（DSL 拡張関数規約・`SectionHandle` / `CellHandle` / `KsIdentifiable` / `DSLReidentifiableCell` / `DSLStyleModifiableCell` の定義を前提とする）
- 後続変更が依存：`add-maui-cells`（基本 Cell の MAUI 側 BindableObject + Handler の実装、および `samples/maui/` への 7 種基本 Cell ページ追加を担当）
- リスク：低〜中。各 Cell の振る舞いが旧 AiForms と互換であることを Sample で目視確認する必要がある
