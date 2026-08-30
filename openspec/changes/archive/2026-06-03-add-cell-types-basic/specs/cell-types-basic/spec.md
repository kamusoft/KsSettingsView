## ADDED Requirements

### Requirement: 具象 Cell の id デフォルト値規約

本変更提案で追加されるすべての具象 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、`add-declarative-dsl` で確定した「具象 Cell コンストラクタの `id` デフォルト値規約」に従わなければならない (SHALL)。

- iOS: 各 Cell struct は `id: UUID = UUID()` のデフォルト値を持つ
- Android: 各 Cell data class は `id: String = "<className>-${java.util.UUID.randomUUID()}"` のデフォルト値を持つ
- DSL 経路では `DSLReidentifiable.withDSLID(_:)` / `DSLReidentifiableCell.withDSLId(...)` により本仕様の優先順位に従う ID に rebind される
- 利用者は DSL 内で `LabelCell(title: "...")` のように `id` 引数を省略して記述できなければならない (MUST)
- Store 方式で利用する際は、利用者が `id` 引数を明示指定するかデフォルト値を使うかを選択できる

#### Scenario: id 引数省略で生成

- **GIVEN** iOS `LabelCell(title: "プロフィール")`、Android `LabelCell(title = "プロフィール")`（`id` 引数省略）
- **WHEN** Cell インスタンスを生成する
- **THEN** iOS では `id` が `UUID()` で自動採番された値、Android では `"label-${ランダム UUID}"` 形式の文字列が `id` に格納される。コンパイル・実行ともにエラーは出ない

#### Scenario: DSL 経路での id rebind

- **GIVEN** iOS `Section("一般") { LabelCell(title: "通知") }` または Android `Section("一般") { LabelCell(title = "通知") }`（DSL 経路、`id` 省略）
- **WHEN** DSL → Diff 算出ロジックが評価される
- **THEN** `LabelCell` のコンストラクタデフォルト値で生成された `id` は `DSLReidentifiable.withDSLID(_:)` / `DSLReidentifiableCell.withDSLId(...)` により `(SectionID, indexInSection, CellType)` ハッシュベースの安定 ID に rebind され、Recomposition / body 再評価をまたいで同じ ID を保持する

#### Scenario: Store 方式での id 明示指定

- **GIVEN** Store 方式で `store.insertCell(cell = LabelCell(id = "user-name", title = "名前"), sectionId = "general", at = 0)`（`id` 明示指定）
- **WHEN** Store の Diff 経路が発火する
- **THEN** Cell の `id` は利用者指定の `"user-name"` のまま使用される（DSL 経路を通らないため rebind されない）

### Requirement: Compose DSL 拡張関数による Cell 直置き

本変更提案で追加される Compose 側の各具象 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、`add-declarative-dsl` で確定した「具象 Cell 型ごとの DSL 拡張関数」規約に従い、`DSLSectionScope` の拡張関数として直置き API を提供しなければならない (SHALL)。

- 例: `fun DSLSectionScope.LabelCell(title: String, description: String? = null, valueText: String? = null, icon: KsImage? = null, hintText: String? = null, style: CellStyle = CellStyle()): CellHandle = cell(LabelCell(title = title, description = description, valueText = valueText, icon = icon, hintText = hintText, style = style))`
- 各 DSL 拡張関数の戻り値は `CellHandle` でなければならない (MUST)（`.cellHeight(...)` 等の handle 経由 modifier chain を可能にするため）
- data class の `LabelCell` と同名の DSL 関数を同一名前空間で共存させる（Kotlin の overload 解決により DSL ブロック内では拡張関数版が優先される）
- 利用者は `Section("一般") { LabelCell(title = "通知") }` のように iOS と完全並列な書き味で Cell を直置きできる

iOS 側では Swift `@resultBuilder SectionBuilder` の機構により Cell 値を直置きできるため、別途 DSL 拡張関数の規約は不要。

#### Scenario: Compose DSL 内での LabelCell 直置き

- **GIVEN**
  ```kotlin
  KsSettingsView {
      Section("一般") {
          LabelCell(title = "通知")
          LabelCell(title = "プライバシー", description = "詳細設定")
      }
  }
  ```
- **WHEN** Composition する
- **THEN** `DSLSectionScope` の拡張関数 `LabelCell(title:, description:)` が解決され、内部で `cell(LabelCell(...))` が呼ばれて Cell が DSL ツリーに追加される。利用者は `cell(LabelCell(...))` のラップを書かずに済む

#### Scenario: Compose DSL 内での Cell modifier chain

- **GIVEN**
  ```kotlin
  Section("詳細") {
      LabelCell(title = "高さ調整").cellHeight(80.dp)
      SwitchCell(title = "通知", isOn = state).cellID("notification-toggle")
  }
  ```
- **WHEN** Composition する
- **THEN** 各 Cell DSL 拡張関数の戻り値 `CellHandle` に対し `.cellHeight(...)` / `.cellID(...)` が chain され、内部の `DSLCellNode` の Cell 値 / identityHint が適切に更新される

### Requirement: KsImage 値型

`KsImage` は Cell のアイコン表現に用いる論理値型でなければならない (SHALL)。プラットフォーム UI 型（`UIImage`、`Drawable`）を直接保持してはならない (MUST NOT)。最低限、`name: String?`（埋め込みリソース論理名）、`url: String?`（リモート URL）、`systemName: String?`（iOS の SF Symbols 名／Android の Material アイコン名）の 3 フィールドを持たなければならない (MUST)。Hashable / equals 契約を満たさなければならない (MUST)。

#### Scenario: KsImage の構築

- **GIVEN** 任意の `name`、`url`、`systemName` の組み合わせ（すべて任意）
- **WHEN** iOS では `KsImage(name:url:systemName:)`、Android では `KsImage(name = ..., url = ..., systemName = ...)` を構築する
- **THEN** イミュータブル値型として生成される

#### Scenario: 等価性

- **GIVEN** 同じフィールドを持つ 2 つの `KsImage` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される（Swift `==`、Kotlin `equals()`）

#### Scenario: Cell からの参照

- **GIVEN** `LabelCell(icon: KsImage(systemName: "bell"))`
- **WHEN** UI 層で描画する
- **THEN** UI 層は `systemName` から iOS では `UIImage(systemName: "bell")`、Android では Material アイコン名から `Drawable` を解決して表示する（Cell モデル自体はプラットフォーム型を保持しない）

### Requirement: LabelCell

`LabelCell` は読み取り専用の表示用セルでなければならない (SHALL)。`title`、`description`（任意）、`valueText`（任意、右寄せ表示）、`icon`（任意、URL または論理名）、`hintText`（任意、右上）の各フィールドを持たなければならない (MUST)。

#### Scenario: 全フィールド表示

- **GIVEN** `LabelCell(title: "通知", description: "プッシュ通知設定", valueText: "オン", icon: nil, hintText: nil)`
- **WHEN** SettingsView に表示される
- **THEN** 左側に "通知"（タイトル）と "プッシュ通知設定"（説明）が縦並び、右側に "オン"（値）が表示される

#### Scenario: 最小フィールド表示

- **GIVEN** `LabelCell(title: "プロフィール")`（他は省略）
- **WHEN** SettingsView に表示される
- **THEN** 左側にタイトルのみが表示され、説明・値・アイコンの領域は確保されない

### Requirement: CommandCell

`CommandCell` はタップで処理を実行する用途のセルでなければならない (SHALL)。`LabelCell` のフィールドに加えて、デフォルトで右端に Disclosure Indicator（iOS: chevron、Android: 右矢印）を表示しなければならない (MUST)。タップ時にユーザー操作通知（`onTap` クロージャ／コールバック）を発火しなければならない (MUST)。

#### Scenario: タップで通知発火

- **GIVEN** `CommandCell(title: "ライセンス", onTap: {...})` が表示されている
- **WHEN** ユーザーが Cell をタップする
- **THEN** `onTap` クロージャが呼ばれる

#### Scenario: Disclosure Indicator の表示

- **GIVEN** `CommandCell(title: "...")` が表示されている
- **WHEN** Cell の右端を観察する
- **THEN** Disclosure Indicator（iOS: chevron アイコン、Android: 右矢印 ImageView）が表示される

### Requirement: ButtonCell

`ButtonCell` はボタン用途のセルでなければならない (SHALL)。`title` をボタンスタイルで Cell 中央寄せ表示しなければならない (MUST)。タップで `onTap` を発火しなければならない (MUST)。`titleColor`（`CellStyle` 経由）でボタンテキストの色を上書きできなければならない (MUST)。

#### Scenario: 中央寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", onTap: {...})`
- **WHEN** SettingsView に表示される
- **THEN** Cell 中央にタイトルが表示され、Disclosure Indicator は表示されない

### Requirement: SwitchCell

`SwitchCell` は ON/OFF を切り替えるトグルスイッチを持つセルでなければならない (SHALL)。`title`、`description`（任意）、`isOn: Bool` を持ち、ユーザーがスイッチを操作したときに `onValueChanged` 通知を発火しなければならない (MUST)。`accentColor`（任意）でスイッチ ON 時の色を指定できなければならない (MUST)。

#### Scenario: 初期状態の表示

- **GIVEN** `SwitchCell(title: "通知", isOn: true)`
- **WHEN** SettingsView に表示される
- **THEN** 右側に UISwitch（または SwitchCompat）が ON 状態で表示される

#### Scenario: ユーザー操作で値が変わる

- **GIVEN** `SwitchCell(title: "通知", isOn: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーがスイッチをタップして ON にする
- **THEN** `onValueChanged(true)` が呼ばれる

### Requirement: CheckboxCell

`CheckboxCell` は ON/OFF をチェックマークで表すセルでなければならない (SHALL)。`title`、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。チェック時のアイコン（accent 表示）は `Theme.cellAccentColor` または `CellStyle.accentColor` で着色されなければならない (MUST)。

#### Scenario: チェック状態の表示

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)`
- **WHEN** 表示される
- **THEN** 右端にチェックマーク（accent カラー）が表示される

#### Scenario: タップで toggle

- **GIVEN** `CheckboxCell(isChecked: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーが Cell をタップする
- **THEN** `onValueChanged(true)` が呼ばれ、内部状態が更新されると次回レンダリング時にチェックマークが表示される

### Requirement: RadioCell

`RadioCell` は同一 `groupId` 内で単一選択を行うラジオボタン用セルでなければならない (SHALL)。`title`、`groupId: String`、`value: String`、`selectedValue: String` を持ち、`value == selectedValue` のときチェック表示する (MUST)。タップで `onSelected(value)` を発火し、利用者は `selectedValue` を更新する (MUST)。

#### Scenario: 選択状態の表示

- **GIVEN** 同じ `groupId = "theme"` を持つ 3 つの RadioCell（value = "light"/"dark"/"auto"、selectedValue = "dark"）
- **WHEN** 表示される
- **THEN** "dark" の RadioCell のみチェック表示される

#### Scenario: 選択切り替え

- **GIVEN** 上記の RadioCell 3 つ、selectedValue = "dark"
- **WHEN** ユーザーが "light" の Cell をタップする
- **THEN** `onSelected("light")` が呼ばれる（実際の selectedValue 更新は SettingsRoot 側の責務）

### Requirement: SimpleCheckCell

`SimpleCheckCell` はリスト中の任意項目の選択／非選択を表す単純チェックセルでなければならない (SHALL)。`title`、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。`CheckboxCell` との違いはアイコン表現（`SimpleCheckCell` は左側にチェック、`CheckboxCell` は右側に大きめのチェック）である。

#### Scenario: 左側チェック表示

- **GIVEN** `SimpleCheckCell(title: "通知1", isChecked: true)`
- **WHEN** 表示される
- **THEN** タイトルの左側にチェックマーク（小）、タイトルが右に並ぶ

### Requirement: 基本 Cell の登録 API

各プラットフォームは、基本 Cell 群を `KsCellRegistry` にまとめて登録する `registerBasicCells()`（または同等の登録関数）を提供しなければならない (SHALL)。

#### Scenario: iOS 一括登録

- **GIVEN** `KsCellRegistry.shared`
- **WHEN** `KsCellRegistry.registerBasicCells()` を呼ぶ
- **THEN** LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell の 7 種が registry に登録される

#### Scenario: Android 一括登録

- **GIVEN** `KsCellRegistry`
- **WHEN** `KsCellRegistry.registerBasicCells(context)` を呼ぶ
- **THEN** 7 種すべての ViewHolder ファクトリと viewType が登録される

### Requirement: PoC Cell の削除

`add-settings-view-ios-ui` および `add-settings-view-android-ui` で導入された PoC Cell（`PoCLabelCell` / `PocLabelCell`）は本変更提案の完了時点で削除されなければならない (SHALL)。

#### Scenario: PoC Cell の不在

- **GIVEN** 本変更提案実装後の `KsSettingsViewUI` および `ks-settingsview-ui` モジュール
- **WHEN** ソースを検索する
- **THEN** `PoCLabelCell` および `PocLabelCell` の型定義は存在せず、`LabelCell` が public な代替として存在する

### Requirement: ユニットテスト

各基本 Cell に対して bind / 表示確認 / ユーザー操作通知 / Theme 適用 / 再利用後の状態リセットを検証するユニットテストが存在しなければならない (SHALL)。

#### Scenario: SwitchCell の値変更通知テスト

- **GIVEN** `SwitchCell(isOn: false)` を bind した ViewHolder
- **WHEN** UISwitch / SwitchCompat の `setOn(true)` を発火させる（テスト内でシミュレート）
- **THEN** `onValueChanged(true)` のクロージャが呼ばれることをテストアサーションで確認する
