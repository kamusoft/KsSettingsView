# cell-types-basic Specification

## Purpose

`cell-types-basic` は、`KsSettingsViewCore` の `Cell` / `KsCell` ドメインモデルを実装する基本 Cell 群（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）と、それらが共通して用いる値型（`KsImage`）・登録 API・DSL 直置き規約を定義する capability である。iOS（`KsSettingsViewUI`）/ Android（`ks-settingsview-ui` / `ks-settingsview-compose`）の両プラットフォームで完全に並列な書き味を提供し、`add-declarative-dsl` で確定した「具象 Cell の id デフォルト値規約」「具象 Cell 型ごとの DSL 拡張関数」規約に従う。本 capability の追加に伴い、PoC 用 Cell（`PoCLabelCell` / `PocLabelCell`）は削除される。

## Requirements

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

Compose DSL（`ks-settingsview-compose`）は、各具象 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）について **`DSLSectionScope` の拡張関数** を提供し、`cell(...)` ヘルパを介さず Cell を直接 DSL に並べられるようにしなければならない (SHALL)。各拡張関数の引数として `id: String = "<cell-prefix>-${UUID.randomUUID()}"` を提供し (MUST)、`KsCell` 引数構造を CellHandle として返さなければならない (MUST)。

例: `fun DSLSectionScope.LabelCell(title: String, description: String? = null, valueText: String? = null, icon: KsImage? = null, hintText: String? = null, style: CellStyle = CellStyle()): CellHandle = cell(LabelCell(title = title, description = description, valueText = valueText, icon = icon, hintText = hintText, style = style))`

**`icon` パラメータの型は `KsImage?`（`jp.kamusoft.kssettingsview.ui.KsImage`）でなければならない (MUST)**。`style: CellStyle` の `CellStyle` も UI 層所属。

#### Scenario: LabelCell の DSL 直置き

- **GIVEN** Compose DSL 内で `Section("...") { LabelCell(title = "ストレージ", icon = KsImage.Resource(R.drawable.ic_storage)) }`
- **WHEN** ツリーをビルドする
- **THEN** ビルドエラーなく `CellHandle` が返され、Section の Cell リストに `LabelCell` が並ぶ

#### Scenario: 関連型のインポート

- **GIVEN** 上記コード
- **WHEN** import 文を書く
- **THEN** `import jp.kamusoft.kssettingsview.ui.KsImage` および `import jp.kamusoft.kssettingsview.ui.CellStyle` が必要（Core 側 import では解決できない）

### Requirement: KsImage 値型

`KsImage` は Cell のアイコン表現を運ぶ sealed 型でなければならない (SHALL)。**`KsImage` は `KsSettingsViewUI`（iOS）および `ks-settingsview-ui`（Android）に所属する (MUST)。`KsSettingsViewCore` / `ks-settingsview-core` には所属してはならない (MUST NOT)**。プラットフォーム UI 型（`UIImage`、`Drawable`）を派生の中に保持する（プラットフォーム固有派生として隔離される）。`Hashable` / `equals` 契約を満たさなければならない (MUST)。

iOS 側（`KsSettingsViewUI`）は次の派生を持たなければならない (MUST)：

- `systemName(String)`: SF Symbols 名を保持する派生
- `uiImage(UIImage)`: 任意の `UIImage` を保持する派生

Android 側（`ks-settingsview-ui`）は次の派生を持たなければならない (MUST)：

- `Resource(@DrawableRes resId: Int)`: Android リソース ID を保持する派生（主軸）
- `Drawable(android.graphics.drawable.Drawable)`: 任意の `Drawable` を保持する派生
- `SystemName(String)`: iOS との API 対称性のための派生（Android では解決不可、UI 層は無視する）

#### Scenario: iOS の派生定義

- **GIVEN** Swift `KsSettingsViewUI` モジュール
- **WHEN** `KsImage` を参照する
- **THEN** `public enum KsImage: Hashable` であり、`case systemName(String)` と `case uiImage(UIImage)` の 2 ケースを持つ。`Hashable` 実装は、`systemName` ケースは内部 String の hash、`uiImage` ケースは `ObjectIdentifier(uiImage)` 相当の参照同一性で hash する

#### Scenario: Android の派生定義

- **GIVEN** Kotlin `ks-settingsview-ui` モジュール
- **WHEN** `KsImage` を参照する
- **THEN** `sealed interface KsImage` であり、サブタイプとして `data class Resource(@DrawableRes val resId: Int) : KsImage`、`class Drawable(val drawable: android.graphics.drawable.Drawable) : KsImage`、`data class SystemName(val name: String) : KsImage` の 3 派生を持つ。`Drawable` は参照同一性で `equals` / `hashCode` を持ち、`Resource` / `SystemName` は値同一性で `equals` / `hashCode` を持つ

#### Scenario: iOS の構築

- **GIVEN** Swift コード（`import KsSettingsViewUI` 済み）
- **WHEN** `KsImage.systemName("bell")` または `KsImage.uiImage(UIImage(systemName: "bell")!)` を構築する
- **THEN** 該当ケースのイミュータブル値として生成され、パターンマッチ可能となる

#### Scenario: Android の構築

- **GIVEN** Kotlin コード（`import jp.kamusoft.kssettingsview.ui.KsImage` 済み）
- **WHEN** `KsImage.Resource(R.drawable.ic_settings)` または `KsImage.Drawable(ContextCompat.getDrawable(context, R.drawable.ic_settings)!!)` または `KsImage.SystemName("bell")` を構築する
- **THEN** 該当派生のイミュータブル値として生成され、`when` でパターンマッチ可能となる

#### Scenario: Core モジュールには所属しない

- **GIVEN** iOS の `import KsSettingsViewCore`、Android の `import jp.kamusoft.kssettingsview.core.*`
- **WHEN** `KsImage` を参照する
- **THEN** 解決できずビルドエラーとなる。`KsImage` は UI 層モジュールへのインポートが必須

#### Scenario: 旧 KsImage 形式の廃止

- **GIVEN** 既存コード `KsImage(systemName: "bell")` または `KsImage(name = "bell")` の呼び出し
- **WHEN** 本 change のリリース後にコンパイルする
- **THEN** 旧 `KsImage(...)` 形式は廃止されているためコンパイルエラーとなり、利用者は `KsImage.systemName("bell")`（iOS）または `KsImage.Resource(R.drawable.ic_bell)`（Android）への書き換えが要求される

#### Scenario: iOS Cell からの利用（systemName）

- **GIVEN** `LabelCell(icon: KsImage.systemName("bell"))`
- **WHEN** iOS UI 層が描画する
- **THEN** UI 層は `systemName("bell")` 派生を解決し、`UIImage(systemName: "bell")` を ImageView に設定する

#### Scenario: iOS Cell からの利用（任意 UIImage）

- **GIVEN** `LabelCell(icon: KsImage.uiImage(customImage))`（`customImage: UIImage`）
- **WHEN** iOS UI 層が描画する
- **THEN** UI 層は `uiImage(customImage)` 派生を解決し、`ImageView.image = customImage` を設定する

#### Scenario: Android Cell からの利用（リソース ID）

- **GIVEN** `LabelCell(icon = KsImage.Resource(R.drawable.ic_storage))`
- **WHEN** Android UI 層が描画する
- **THEN** UI 層は `Resource(resId)` 派生を解決し、`ContextCompat.getDrawable(context, R.drawable.ic_storage)` を `ImageView.setImageDrawable(...)` に設定する

#### Scenario: Android Cell からの利用（任意 Drawable）

- **GIVEN** `LabelCell(icon = KsImage.Drawable(customDrawable))`（`customDrawable: android.graphics.drawable.Drawable`）
- **WHEN** Android UI 層が描画する
- **THEN** UI 層は `Drawable(customDrawable)` 派生を解決し、`ImageView.setImageDrawable(customDrawable)` を設定する

#### Scenario: Android で SystemName 派生のフォールバック

- **GIVEN** `LabelCell(icon = KsImage.SystemName("bell"))`
- **WHEN** Android UI 層が描画する
- **THEN** UI 層は `SystemName` 派生を解決できないため、アイコン領域を非表示（`ImageView.visibility = View.GONE`）にしてフォールバックする。エラーログや throw は発生してはならない

#### Scenario: icon = null / nil の Cell

- **GIVEN** `LabelCell(icon: nil)` または `LabelCell(icon = null)`
- **WHEN** UI 層が描画する
- **THEN** Cell のアイコン領域は非表示となり、Title が左寄せでアイコン領域分のインデントなしに配置される

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

### Requirement: 全 Cell 共通の description / valueText / icon / hintText フィールド

本 change で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、すべて以下の **共通 Optional フィールド** を持たなければならない (SHALL)：

- `description: String?`（既定 `nil`）— Cell タイトル下に副題として表示
- `valueText: String?`（既定 `nil`）— Cell タイトル右側に値テキストとして表示
- `icon: KsImage?`（既定 `nil`）— Cell タイトル左側にアイコンとして表示
- `hintText: String?`（既定 `nil`）— Cell 右上に float 表示するヒントテキスト

ただし `ButtonCell` は **`description` フィールドを持たない例外** とする (MUST NOT)。これはオリジナル `AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs` が `Description` / `DescriptionColor` / `DescriptionFontSize` を `private new` で隠蔽し、iOS の `ButtonCellView.cs` も `DescriptionLabel.Hidden = true` としている挙動を踏襲するためである。`ButtonCell` は `valueText` / `icon` / `hintText` の 3 フィールドのみ追加される。

各フィールドは `nil` のとき非表示としなければならない (MUST)。Cell 内のレイアウトはオリジナル `AiForms.Maui.SettingsView` の `CellBase`（iOS `UIStackView` ベース、Android `RelativeLayout` ベース）に準拠し、以下の 2 系統で配置しなければならない (MUST)：

- **本体行（横方向）**: 「`[icon][title / description][valueText (title 行の右寄せ)][accessory (右側中央)]`」の順で配置する。`accessory` は各 Cell 種別固有の trailing コントロール（`SwitchCell` の `UISwitch` / `Switch`、`CheckboxCell` の MaterialCheckBox 等、`CommandCell` の chevron、`LabelCell` / `ButtonCell` の `nil`）に対応し、セル右側中央に配置される。
- **hintText（右上 float）**: `hintText` は本体行とは別系統として **セル右上に float 配置** しなければならない (MUST)。具体的には、セル上端から数 dp 程度のマージン、セル右端から数 dp 程度のマージンで右上に置く。`accessory` と `hintText` は両者とも右端揃いとなるため物理的に重なり得るが、`hintText` がセル上端基準、`accessory` がセル縦中央基準で配置されるため通常は干渉しない。万一の干渉時は `hintText` を前面（accessory より手前）に配置する。

`hintText` の表示振る舞いは、オリジナル `AiForms.Maui.SettingsView` の挙動を踏襲し、以下を満たさなければならない (MUST)：

- 小さなテキスト（既定フォントサイズは Theme/CellStyle の `hintTextFont`、未指定時はプラットフォーム既定で 10sp 〜 small 相当）
- 右寄せ（テキスト揃え）
- セル上端から数 dp 程度のマージン
- 1 行表示。横幅が足りない場合は ellipsize end（末尾省略）

`ButtonCell` は `description` を持たないため、`ButtonCell` の本体行レイアウトは「`[icon][title][valueText (title 行の右寄せ)]`」となる。`hintText` の右上 float 配置は他 Cell と同じである。

各フィールドの文字色・フォント・サイズは Change 1 (`port-theme-and-cellstyle-missing-fields`) で確立された解決順序 (`CellStyle → Theme → 既定`) に従わなければならない (MUST)：

- `description` の色: `CellStyle.descriptionColor → Theme.cellDescriptionColor → UIColor.secondaryLabel`（iOS）/ 対応する Android 既定
- `description` のフォント: `CellStyle.descriptionFont → Theme.cellDescriptionFont → preferredFont(.footnote)`（iOS）/ 対応する Android 既定
- `valueText` の色: `CellStyle.valueTextColor → Theme.cellValueTextColor → Theme.cellTitleColor → UIColor.label`（iOS）/ 対応する Android 既定
- `valueText` のフォント: `CellStyle.valueTextFont → Theme.cellValueTextFont → Theme.cellTitleFont → preferredFont(.body)`
- `hintText` の色: `CellStyle.hintTextColor → Theme.cellHintTextColor → Theme.cellAccentColor`
- `hintText` のフォント: `CellStyle.hintTextFont → Theme.cellHintFont → preferredFont(.footnote)`
- `icon` のサイズ: `CellStyle.iconSize → Theme.cellIconSize → 24pt`
- `icon` の角丸半径: `CellStyle.iconRadius → Theme.cellIconRadius → 0pt`

各 Cell の `Hashable` / `Equatable`（iOS）/ `equals` / `hashCode`（Android）実装は、追加された共通フィールドをすべて含めて判定しなければならない (MUST)。各 Cell の `withDSLID(_:)` / `withStyle(_:)` 実装（iOS）および `data class copy()` 経路（Android）は、追加フィールドを保持しなければならない (MUST)。

DSL 拡張関数（iOS `Section { SwitchCell(...) }`、Android `Section("...") { SwitchCell(...) }`）も、追加フィールドを Optional 引数として受け取れなければならない (MUST)。既存呼び出し（追加フィールドを指定しない呼び出し）は破壊してはならない (MUST NOT)。

#### Scenario: SwitchCell が description / valueText / icon / hintText を持てる

- **GIVEN** iOS の `SwitchCell(title: "通知", description: "プッシュ通知を受信", valueText: "オン", icon: KsImage.systemName("bell"), hintText: "推奨", isOn: true)`、または Android の `SwitchCell(title = "通知", description = "プッシュ通知を受信", valueText = "オン", icon = KsImage.Resource(R.drawable.ic_bell), hintText = "推奨", isOn = true)`
- **WHEN** SettingsView に表示される
- **THEN** Cell 本体行は左端にアイコン（鐘）、その右にタイトル「通知」と説明「プッシュ通知を受信」が縦並びで表示され、title 行の右寄せに「オン」、右側中央に UISwitch / Switch（ON 状態）が配置される。`hintText` 「推奨」はセル右上に float 表示され、Switch とは上端 vs 縦中央で位置が分かれるため通常は重ならない

#### Scenario: CheckboxCell が共通フィールドを持てる

- **GIVEN** `CheckboxCell(title: "規約に同意", description: "全文を読みました", valueText: nil, icon: KsImage.systemName("doc.text"), hintText: nil, isChecked: false)`
- **WHEN** 表示される
- **THEN** 左端アイコン、タイトル「規約に同意」、副題「全文を読みました」、右端に MaterialCheckBox（非チェック）が表示される。`valueText` / `hintText` が `nil` のため対応領域は確保されない

#### Scenario: RadioCell が description / valueText / icon / hintText / accentColor を持てる

- **GIVEN** `RadioCell(title: "ダーク", description: "暗い背景", valueText: "推奨", icon: KsImage.systemName("moon"), hintText: nil, value: "dark", accentColor: UIColor.systemPurple)`
- **WHEN** 表示される
- **THEN** 本体行は左端アイコン、タイトル「ダーク」、副題「暗い背景」、title 行右寄せに「推奨」、右側中央に紫色のチェックマーク（accentColor 反映）が配置される。`hintText` は `nil` のため右上 float 領域は表示されない

#### Scenario: SimpleCheckCell が共通フィールドを持てる

- **GIVEN** `SimpleCheckCell(title: "通知1", description: "週次レポート", valueText: nil, icon: nil, hintText: "新規", isChecked: true, accentColor: nil)`
- **WHEN** 表示される
- **THEN** 本体行はタイトル「通知1」と副題「週次レポート」が縦並び、右側中央にチェックマーク（既定の accent 色）が表示される。`hintText` 「新規」はセル右上に float 表示され、チェックマークとは上端 vs 縦中央で位置が分かれるため通常は重ならない

#### Scenario: hintText は右上 float、accessory は右側中央で物理的に重ならない

- **GIVEN** 任意の Cell（例: `SwitchCell(title: "通知", hintText: "推奨", isOn: true)` または `RadioCell(title: "ダーク", hintText: "推奨", value: "dark", selectedValue: "dark")`）
- **WHEN** 描画される
- **THEN** `hintText` 「推奨」はセル上端から数 dp 程度のマージンでセル右上に float 配置され、accessory（UISwitch / チェックマーク等）はセル縦中央に配置される。両者は右端揃いだが上端 vs 縦中央という縦位置の違いにより通常は物理的に重ならない。万一の重なり（hintText が異常に大きい等）が生じた場合は `hintText` を accessory より前面に配置する

#### Scenario: ButtonCell が icon / valueText / hintText を持てる

- **GIVEN** `ButtonCell(title: "登録", valueText: "送信", icon: KsImage.systemName("paperplane"), hintText: "推奨", titleColor: UIColor.systemBlue, titleAlignment: .start)`
- **WHEN** 表示される
- **THEN** 本体行は左端にアイコン、タイトル「登録」（青系、左寄せ）、title 行右寄せに valueText「送信」が配置される。`hintText` 「推奨」はセル右上に float 表示される。`titleAlignment = .start` は title のみに適用され、icon / valueText / hintText の配置は他 Cell と同じ規約に従う

#### Scenario: ButtonCell には description フィールドが存在しない

- **GIVEN** `ButtonCell` のコンストラクタおよび DSL 拡張関数のシグネチャ
- **WHEN** `ButtonCell(title: "登録", description: "X")` のように `description` 引数を渡そうとコンパイルする
- **THEN** **コンパイルエラー** になる（`ButtonCell` には `description` パラメータが定義されていないため）。オリジナル `AiForms.Maui.SettingsView` の `ButtonCell` が `Description` を `private new` で隠蔽している挙動を踏襲した結果である

#### Scenario: 既存呼び出しの互換性

- **GIVEN** 既存コード `SwitchCell(title: "通知", isOn: true)` / `CheckboxCell(title: "規約", isChecked: false)` / `RadioCell(title: "ダーク", value: "dark")` / `SimpleCheckCell(title: "通知1")` / `ButtonCell(title: "ログアウト", onTap: { ... })`
- **WHEN** コンパイル・実行する
- **THEN** すべて追加フィールドが既定値（`description = nil` / `valueText = nil` / `icon = nil` / `hintText = nil` / `accentColor = nil`）で構築され、ビルドエラーや実行時エラーは発生しない。表示は本 change 適用前と同等になる

#### Scenario: Hashable / Equatable が追加フィールドを含む

- **GIVEN** `let a = SwitchCell(id: id, title: "通知", description: "X", isOn: true)`、`let b = SwitchCell(id: id, title: "通知", description: "Y", isOn: true)`（description のみ異なる、id 同一）
- **WHEN** `a == b` を評価する
- **THEN** `false` になる（description が `==` 比較に含まれる）。`Set` / 辞書キーとしての hash 値も異なる

#### Scenario: withDSLID が追加フィールドを保持する

- **GIVEN** iOS の `SwitchCell(title: "通知", description: "X", icon: KsImage.systemName("bell"), isOn: true).withDSLID(newID)`
- **WHEN** 戻り値の各フィールドを参照する
- **THEN** `description == "X"` / `icon == .systemName("bell")` が保持されている

#### Scenario: 共通行レイアウト関数経由での描画（全 7 種 Cell に適用）

- **GIVEN** 任意の Cell（例: `SwitchCell(title: "通知", description: "X", icon: KsImage.systemName("bell"), isOn: true)`）
- **WHEN** UI 層が当該 Cell を描画する
- **THEN** UI 層は内部的に共通行レイアウト関数（iOS: `applyCellBaseLayout(...)`、Android: `applyCellBaseLayout(views, ...)`）を経由して `title` / `description` / `valueText` / `icon` / `hintText` を配置し、accessory slot にのみ Cell 種別固有のコントロール（UISwitch / MaterialCheckBox 等）を組む。Cell View / ViewHolder 側に title / description / icon / hintText のレイアウトロジックを重複実装してはならない (MUST NOT)

#### Scenario: 既存 LabelCell / CommandCell も共通行レイアウト関数を経由する

- **GIVEN** 本 change で新規追加された Cell（Switch/Checkbox/Radio/SimpleCheck/Button）だけでなく、**既存の `LabelCell` および `CommandCell`** も同じ共通行レイアウト関数経由で描画される必要がある
- **WHEN** `LabelCell(title: "プロフィール", description: "X", valueText: "Y", icon: KsImage.systemName("person"), hintText: "新着")` または `CommandCell(title: "ライセンス", description: "X", icon: KsImage.systemName("doc"), hideArrow: false, onTap: {...})` を描画する
- **THEN** いずれも共通行レイアウト関数（iOS: `applyCellBaseLayout(...)`、Android: `applyCellBaseLayout(views, ...)`）を経由し、`LabelCell` は `accessories: []` / 空の `accessoryHolder`、`CommandCell` は accessory slot に chevron / Disclosure Indicator を組む。これにより 7 種すべての Cell が「本体行 `[icon][title / description][valueText (title 行右寄せ)][accessory (右側中央)]` + `hintText` 右上 float」の共通レイアウト規約を満たす。後続変更提案で追加される新規 Cell 種別も同じ共通行レイアウト関数を経由しなければならない (MUST)

### Requirement: ButtonCell

`ButtonCell` はボタン用途のセルでなければならない (SHALL)。`title` をボタンスタイルで表示しなければならない (MUST)。`valueText: String?`（既定 `nil`） / `icon: KsImage?`（既定 `nil`） / `hintText: String?`（既定 `nil`） の **共通 Optional フィールド** を持たなければならない (MUST)。**`description` フィールドは持ってはならない (MUST NOT)** — オリジナル `AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs` が `Description` / `DescriptionColor` / `DescriptionFontSize` を `private new` で隠蔽し、iOS の `ButtonCellView.cs` も `DescriptionLabel.Hidden = true` としている挙動を踏襲する。タップで `onTap` を発火しなければならない (MUST)。**`titleColor` の型は Native 型 (`UIColor?` / Compose `Color?`) でなければならない (MUST)**。ボタンテキストの色は次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別、Optional、Native 型）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor`（Native 型）が指定されていればそれを採用
3. それ以外で `Theme.cellTitleColor`（Native 型）が指定されていればそれを採用
4. それ以外はプラットフォーム標準のボタン色（iOS: `UIColor.systemBlue`、Android: Material `colorPrimary`）

タイトルの水平方向の揃え位置は `titleAlignment: CellTitleAlignment`（既定 `.center`）で指定できなければならない (MUST)。`titleAlignment` は **title のみ** に適用し、`icon` / `valueText` / `hintText` のレイアウトは「全 Cell 共通の description / valueText / icon / hintText フィールド」Requirement の規約「`[icon][title][valueText (右寄せ)][hintText]`」に従わなければならない (MUST)。すなわち `icon` / `valueText` / `hintText` のいずれかが指定された場合は `titleAlignment` の値に関わらず通常の Cell レイアウト（`[icon][title]...`）になり、`titleAlignment` は title 列の中での揃え位置のみを制御する。`icon` / `valueText` / `hintText` がすべて `nil` のときは、ボタンスタイルの中央寄せ／左寄せ／右寄せフォーマット（既存仕様）を維持する。

#### Scenario: Theme.cellTitleColor が ButtonCell に効く

- **GIVEN** iOS の `Theme(cellTitleColor: UIColor(red: 0.8, green: 0.6, blue: 0.0, alpha: 1.0))`、`ButtonCell(title: "登録", titleColor: nil)`、当該 Cell の `CellStyle.titleColor = nil`
- **WHEN** Cell が描画される
- **THEN** ボタンテキストの色は `Theme.cellTitleColor` 由来の橙系色になる（プラットフォーム標準ボタン色ではない）

#### Scenario: Cell 個別 titleColor が Theme より優先

- **GIVEN** iOS の `Theme(cellTitleColor: UIColor.green)`、`ButtonCell(title: "削除", titleColor: UIColor.red)`
- **WHEN** Cell が描画される
- **THEN** ボタンテキストの色は赤(Cell 個別 `titleColor` 優先、Theme よりも上位)

#### Scenario: 既定の中央寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", onTap: {...})`（`titleAlignment` 省略、`icon` / `valueText` / `hintText` すべて `nil`）
- **WHEN** SettingsView に表示される
- **THEN** Cell 中央にタイトルが表示され、Disclosure Indicator は表示されない

#### Scenario: titleAlignment = .start での左寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", titleAlignment: .start, onTap: {...})`
- **WHEN** SettingsView に表示される
- **THEN** Cell の左端（リーディング側）寄りにタイトルが表示される

#### Scenario: titleAlignment = .end での右寄せ表示

- **GIVEN** `ButtonCell(title: "ログアウト", titleAlignment: .end, onTap: {...})`
- **WHEN** SettingsView に表示される
- **THEN** Cell の右端(トレーリング側)寄りにタイトルが表示される

#### Scenario: titleAlignment 省略時の既定値と API 互換性

- **GIVEN** `ButtonCell(title: "ログアウト", onTap: {...})`（`titleAlignment` を指定しない既存呼び出し）
- **WHEN** コンパイル・実行してインスタンスを参照する
- **THEN** `buttonCell.titleAlignment == .center` で、ビルドエラーや実行時エラーは発生しない

#### Scenario: icon / valueText / hintText を指定したときの titleAlignment の挙動

- **GIVEN** `ButtonCell(title: "登録", valueText: "送信", icon: KsImage.systemName("paperplane"), titleAlignment: .center, onTap: {...})`
- **WHEN** Cell が描画される
- **THEN** 「全 Cell 共通の description / valueText / icon / hintText フィールド」Requirement の規約に従い、左端にアイコン、その右に title、右側に valueText「送信」が配置される。`titleAlignment = .center` は title 列の中での揃え位置のみを制御する（icon がある以上、Cell 全体としてはボタンスタイルの中央寄せフォーマットにはならない）

### Requirement: SwitchCell

`SwitchCell` は ON/OFF を切り替えるトグルスイッチを持つセルでなければならない (SHALL)。`title`、`description: String?`（任意、既定 `nil`）、`valueText: String?`（任意、既定 `nil`）、`icon: KsImage?`（任意、既定 `nil`）、`hintText: String?`（任意、既定 `nil`）、`isOn: Bool` を持ち、ユーザーがスイッチを操作したときに `onValueChanged` 通知を発火しなければならない (MUST)。`accentColor`（任意、**型は Native 型 `UIColor?` / Compose `Color?`**）でスイッチ ON 時の色を指定できなければならない (MUST)。

#### Scenario: 初期状態の表示

- **GIVEN** `SwitchCell(title: "通知", isOn: true)`
- **WHEN** SettingsView に表示される
- **THEN** 右側に UISwitch（または SwitchCompat）が ON 状態で表示される

#### Scenario: ユーザー操作で値が変わる

- **GIVEN** `SwitchCell(title: "通知", isOn: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーがスイッチをタップして ON にする
- **THEN** `onValueChanged(true)` が呼ばれる

#### Scenario: accentColor の型

- **GIVEN** iOS で `SwitchCell(title: "通知", isOn: true, accentColor: UIColor.green)` または Android で `SwitchCell(title = "通知", isOn = true, accentColor = Color.Green)`
- **WHEN** コンパイルする
- **THEN** ビルドエラーなく構築できる。`KsColor` を渡そうとするとビルドエラーとなる

#### Scenario: icon / valueText / hintText を伴う表示

- **GIVEN** `SwitchCell(title: "通知", description: "プッシュ通知", valueText: "オン", icon: KsImage.systemName("bell"), hintText: "推奨", isOn: true)`
- **WHEN** 表示される
- **THEN** 本体行は左端にアイコン、その右にタイトルと説明、title 行の右寄せに valueText「オン」、右側中央に UISwitch（ON）が配置される。`hintText` 「推奨」はセル右上に float 表示され、UISwitch とは上端 vs 縦中央で位置が分かれるため通常は重ならない（全 Cell 共通レイアウト規約に従う）

### Requirement: CheckboxCell

`CheckboxCell` は ON/OFF をチェックマークで表すセルでなければならない (SHALL)。`title`、`description: String?`（任意、既定 `nil`）、`valueText: String?`（任意、既定 `nil`）、`icon: KsImage?`（任意、既定 `nil`）、`hintText: String?`（任意、既定 `nil`）、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。`accentColor`（任意、**型は Native 型 `UIColor?` / Compose `Color?`**）でチェック時の塗り色を指定できなければならない (MUST)。チェック時のアイコン（accent 表示）は `CellStyle.accentColor` または `Theme.cellAccentColor` で着色されなければならない (MUST)。

iOS では、チェック表現はオリジナル `AiForms.Maui.SettingsView` の `CheckBox`（`UIButton` + `Draw`）相当の **角丸の四角いチェックボックス UI** でなければならない (MUST)。すなわち、角丸（CornerRadius 相当）の四角枠（BorderWidth 相当）を持ち、チェック時は accent カラーで塗りつぶしたうえに白いチェックマークを重ね、非チェック時は枠のみを表示する。このチェックボックスは右端に `UICellAccessory.customView`（`placement: .trailing`）として常設し、チェック状態の切り替えは accessory の追加・削除ではなくカスタム View 内部の再描画で行わなければならない (MUST)（追加・削除に伴うスライドアニメーションを避けるため）。

Android では、チェック表現は `com.google.android.material.checkbox.MaterialCheckBox` を用いた角丸の四角いチェックボックスでなければならない (MUST)。`MaterialCheckBox` 自体の内側 padding（タッチ域確保のための既定 padding）は `setPadding(0, 0, 0, 0)` および `minimumWidth = 0` / `minimumHeight = 0` で無効化し、accessoryHolder 右端と CheckboxCell のチェックボックス右端が SwitchCell / RadioCell / SimpleCheckCell と同一 X 座標に揃わなければならない (MUST)。`buttonTintList` は実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）で着色されなければならない (MUST)。

#### Scenario: チェック状態の表示（iOS）

- **GIVEN** `CheckboxCell(title: "規約に同意", isChecked: true)`
- **WHEN** iOS で表示される
- **THEN** 右端に角丸の四角いチェックボックス UI が常設され、チェック時は accent カラーで塗りつぶしたうえに白いチェックマークが重ねて表示される

#### Scenario: 非チェック状態の表示（iOS）

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: false)` を iOS で表示
- **WHEN** 表示される
- **THEN** 右端に角丸の四角い枠のみ（塗りつぶし・チェックマークなし）が表示され、accessory の位置はチェック時と同一である

#### Scenario: チェック状態の表示（Android、accent 色適用）

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)` を Android で表示し、`Theme.cellAccentColor = Color.Yellow`
- **WHEN** ViewHolder が bind する
- **THEN** `MaterialCheckBox.buttonTintList` が黄色（`Theme.cellAccentColor` 由来）で着色される

#### Scenario: 右端アクセサリ位置の整列（Android）

- **GIVEN** 同じ画面に SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell を順に並べた状態
- **WHEN** Android で表示する
- **THEN** 各 Cell の本体行 accessory（Switch / CheckBox / チェックマーク / SimpleCheck）の右端 X 座標がすべて一致する（ピクセル単位の差は ±1 px 以内）。これは accessory のみの整列規約であり、`hintText` は別系統（右上 float、accessory とは縦位置が異なる）として扱われる

#### Scenario: タップで toggle

- **GIVEN** `CheckboxCell(isChecked: false, onValueChanged: { value in ... })`
- **WHEN** ユーザーが Cell をタップする
- **THEN** `onValueChanged(true)` が呼ばれ、内部状態が更新されると次回レンダリング時にチェックマークが表示される

#### Scenario: icon / description / hintText を伴う表示

- **GIVEN** `CheckboxCell(title: "規約に同意", description: "全文を読みました", icon: KsImage.systemName("doc.text"), isChecked: false)`
- **WHEN** 表示される
- **THEN** 左端にアイコン、その右にタイトルと説明、最右に MaterialCheckBox / 角丸四角チェックボックスが配置される（全 Cell 共通レイアウト規約に従う）

### Requirement: RadioCell

`RadioCell` は同一 `groupId` 内で単一選択を行うラジオボタン用セルでなければならない (SHALL)。`title`、`description: String?`（任意、既定 `nil`）、`valueText: String?`（任意、既定 `nil`）、`icon: KsImage?`（任意、既定 `nil`）、`hintText: String?`（任意、既定 `nil`）、`groupId: String`、`value: String`、`selectedValue: String` を持ち、`value == selectedValue` のときチェック表示する (MUST)。タップで `onSelected(value)` を発火し、利用者は `selectedValue` を更新する (MUST)。`accentColor`（任意、**型は Native 型 `UIColor?` / Compose `Color?`**、既定 `nil`）でチェックマークの色を指定できなければならない (MUST)。`accentColor` の解決順序は `RadioCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor → プラットフォーム既定` でなければならない (MUST)。

iOS では、選択状態のチェックマーク（`checkmark`）は右端に **常設の `UICollectionViewCell` accessory**（`customView` ベース）として配置し、選択状態の切り替えは accessory の追加・削除ではなく `alpha` のフェードで行わなければならない (MUST)。すなわち、非選択 → 選択は位置を変えずにフェードイン、選択 → 非選択は位置を変えずにフェードアウトしなければならない (MUST)（accessory の追加・削除に伴う横スライドアニメーションを生じさせてはならない (MUST NOT)）。

#### Scenario: 選択状態の表示

- **GIVEN** 同じ `groupId = "theme"` を持つ 3 つの RadioCell（value = "light"/"dark"/"auto"、selectedValue = "dark"）
- **WHEN** 表示される
- **THEN** "dark" の RadioCell のみチェック表示される

#### Scenario: 選択切り替え

- **GIVEN** 上記の RadioCell 3 つ、selectedValue = "dark"
- **WHEN** ユーザーが "light" の Cell をタップする
- **THEN** `onSelected("light")` が呼ばれる（実際の selectedValue 更新は SettingsRoot 側の責務）

#### Scenario: 選択解除時のフェードアウト

- **GIVEN** チェック表示中の RadioCell が非選択状態へ更新される
- **WHEN** チェックマークが消える
- **THEN** チェックマークは位置を変えずにその場でフェードアウトする（右方向などへスライドして消えてはならない）

#### Scenario: accentColor の反映

- **GIVEN** `RadioCell(title: "ダーク", groupId: "theme", value: "dark", selectedValue: "dark", accentColor: UIColor.systemPurple)`、当該 Cell の `CellStyle.accentColor = nil`、`Theme.cellAccentColor = UIColor.systemBlue`
- **WHEN** iOS で表示される
- **THEN** チェックマーク色は紫（`RadioCell.accentColor` が最優先）になる

#### Scenario: accentColor の Theme フォールバック

- **GIVEN** `RadioCell(title: "ダーク", value: "dark", accentColor: nil)`、`Theme.cellAccentColor = UIColor.systemGreen`
- **WHEN** 表示される
- **THEN** チェックマーク色は緑（`Theme.cellAccentColor` 由来）になる

#### Scenario: icon / description を伴う表示

- **GIVEN** `RadioCell(title: "ダーク", description: "暗い背景", icon: KsImage.systemName("moon"), groupId: "theme", value: "dark", selectedValue: "dark")`
- **WHEN** 表示される
- **THEN** 左端にアイコン、その右にタイトルと説明、最右にチェックマーク（選択中）が配置される（全 Cell 共通レイアウト規約に従う）

### Requirement: SimpleCheckCell

`SimpleCheckCell` はリスト中の任意項目の選択／非選択を表す単純チェックセルでなければならない (SHALL)。`title`、`description: String?`（任意、既定 `nil`）、`valueText: String?`（任意、既定 `nil`）、`icon: KsImage?`（任意、既定 `nil`）、`hintText: String?`（任意、既定 `nil`）、`isChecked: Bool` を持ち、タップで toggle し `onValueChanged` を発火しなければならない (MUST)。`accentColor`（任意、**型は Native 型 `UIColor?` / Compose `Color?`**、既定 `nil`）でチェックマークの色を指定できなければならない (MUST)。`accentColor` の解決順序は `SimpleCheckCell.accentColor → CellStyle.accentColor → Theme.cellAccentColor → プラットフォーム既定` でなければならない (MUST)。

iOS では、チェック表現はオリジナル `AiForms.Maui.SettingsView` の `SimpleCheckCellView`（`UITableViewCellAccessory.Checkmark`）相当の **右端の checkmark** でなければならない (MUST)。レイアウトは `RadioCell` と同形（タイトル左寄せ・チェック右端）であり、選択状態の切り替えは `RadioCell` と同様に位置を変えない `alpha` のフェードで行わなければならない (MUST)。`CheckboxCell` との違いは、`SimpleCheckCell` がシンプルな checkmark を用いるのに対し、`CheckboxCell` は角丸の四角いチェックボックス UI を用いる点である。

#### Scenario: 右端チェック表示

- **GIVEN** `SimpleCheckCell(title: "通知1", isChecked: true)`
- **WHEN** 表示される
- **THEN** タイトルが左寄せで表示され、右端に checkmark（accent カラー）が表示される（RadioCell と同じレイアウト）

#### Scenario: 非チェック時の表示

- **GIVEN** `SimpleCheckCell(title: "通知1", isChecked: false)`
- **WHEN** 表示される
- **THEN** 右端の checkmark は表示されず（フェードアウト済み）、タイトルのみが表示される

#### Scenario: 選択解除時のフェードアウト

- **GIVEN** チェック表示中の SimpleCheckCell が非選択状態へ更新される
- **WHEN** チェックマークが消える
- **THEN** チェックマークは位置を変えずにその場でフェードアウトする

#### Scenario: accentColor の反映

- **GIVEN** `SimpleCheckCell(title: "通知1", isChecked: true, accentColor: UIColor.systemPink)`、`Theme.cellAccentColor = UIColor.systemBlue`
- **WHEN** 表示される
- **THEN** チェックマーク色はピンク（`SimpleCheckCell.accentColor` 最優先）になる

#### Scenario: icon / description / hintText を伴う表示

- **GIVEN** `SimpleCheckCell(title: "通知1", description: "週次レポート", hintText: "新規", isChecked: true)`
- **WHEN** 表示される
- **THEN** 本体行はタイトル「通知1」と副題「週次レポート」が縦並び、右側中央にチェックマークが配置される。`hintText` 「新規」はセル右上に float 表示され、チェックマークとは上端 vs 縦中央で位置が分かれるため通常は重ならない（全 Cell 共通レイアウト規約に従う）

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

### Requirement: 全 Cell 共通の isEnabled

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、すべて `isEnabled: Bool`（既定 `true`）フィールドを持たなければならない (SHALL)。

`isEnabled = false` のとき：

- 当該 Cell のコントロール要素（`SwitchCell` のスイッチ、`CheckboxCell` のチェックボックス、`RadioCell` / `SimpleCheckCell` のチェック表示要素、`CommandCell` / `ButtonCell` のタップ可能領域）はユーザー操作に応答してはならない (MUST NOT)。具体的には、UI コントロールの `isEnabled` を `false` にし、Cell コンテナのタップハンドラを無効化する。
- 当該 Cell のタイトル／説明文／値テキスト／ヒントテキストの色は **`Theme.disabledTextColor`** に置換されなければならない (MUST)。Cell 全体への `alpha` 適用や半透明化は行ってはならない (MUST NOT)。
- `LabelCell` は元来コントロール要素を持たないが、`isEnabled = false` の場合もテキスト色置換規則は同様に適用しなければならない (MUST)。

`isEnabled = true`（既定値）のときは、本変更提案の他の Requirement に従う通常の描画・操作を行う。

#### Scenario: SwitchCell の isEnabled = false

- **GIVEN** `SwitchCell(title: "通知", isOn: true, isEnabled: false)`
- **WHEN** SettingsView に表示してユーザーがスイッチをタップしようとする
- **THEN** スイッチ UI は disabled 表示となりタップに反応せず、タイトル色は `Theme.disabledTextColor` に置換される。`onValueChanged` は発火しない

#### Scenario: CommandCell の isEnabled = false

- **GIVEN** `CommandCell(title: "ライセンス", isEnabled: false, onTap: {...})`
- **WHEN** SettingsView に表示してユーザーが Cell をタップする
- **THEN** タップは無効化されており `onTap` は呼ばれない。タイトル・説明文の色は `Theme.disabledTextColor` に置換される

#### Scenario: LabelCell の isEnabled = false

- **GIVEN** `LabelCell(title: "通知", description: "プッシュ通知設定", valueText: "オン", isEnabled: false)`
- **WHEN** SettingsView に表示される
- **THEN** タイトル・説明文・値テキストすべての色が `Theme.disabledTextColor` に置換される。コントロール要素はないため操作面での変化はない

#### Scenario: API 互換性（既存呼び出し）

- **GIVEN** 既存のコード `SwitchCell(title: "通知", isOn: true)`（`isEnabled` を指定しない呼び出し）
- **WHEN** コンパイル・実行する
- **THEN** `isEnabled` は既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: isEnabled 変更時の差分検出

- **GIVEN** 同一 id の Cell について `isEnabled = true → false` に変更
- **WHEN** Diff 検出（DSL 経路または `SettingsRootDiff.replaceCell`）が走る
- **THEN** Section 構造の追加・削除ではなく `replaceCell` 経路で同一 ViewHolder に対する内容更新（reconfigureItems / notifyItemChanged）として反映される

### Requirement: 全 Cell 共通の Theme.titleColor / Theme.titleFont 反映

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）はすべて、タイトルの色／フォントを次の 3 段階優先順位で解決しなければならない (MUST)：

1. 当該 Cell の `CellStyle.titleColor` / `CellStyle.titleFont` が指定されていればそれを採用（**型は Native 型**）
2. それ以外で `Theme.cellTitleColor` / `Theme.cellTitleFont` が指定されていればそれを採用（**型は Native 型**）
3. それ以外はプラットフォーム既定（iOS: `UIColor.label` / `UIFont.preferredFont(forTextStyle: .body)`、Android: `TextView` 既定色・既定フォント）

`ButtonCell` に限り、第 4 段階としてプラットフォーム標準ボタン色（iOS: `UIColor.systemBlue`、Android: Material `colorPrimary`）が追加され、4 段階目に位置する（Requirement: ButtonCell を参照）。

本 Requirement で参照する `Theme.cellTitleColor` / `Theme.cellTitleFont` は、Change 1 (`port-theme-and-cellstyle-missing-fields`) で **旧名 `Theme.titleColor` / `Theme.titleFont` から rename** されたフィールドである。互換シムは存在しないため、本 Requirement の本文および Scenario のいずれも旧名（`Theme.titleColor` / `Theme.titleFont`）を参照してはならない (MUST NOT)。

#### Scenario: Theme.cellTitleColor が全 Cell タイトル色に反映される

- **GIVEN** iOS の `Theme(cellTitleColor: UIColor.purple)` で初期化された SettingsView に `LabelCell` / `SwitchCell` / `CheckboxCell` などが並ぶ。各 Cell の `CellStyle.titleColor = nil`
- **WHEN** SettingsView が描画される
- **THEN** すべての Cell のタイトル文字色が紫（`Theme.cellTitleColor`）に統一される

#### Scenario: CellStyle.titleColor が Theme.cellTitleColor より優先

- **GIVEN** iOS の `Theme(cellTitleColor: UIColor.purple)`、`LabelCell(title: "強調", style: CellStyle(titleColor: UIColor.orange))`
- **WHEN** Cell が描画される
- **THEN** 当該 Cell のタイトル色は橙（`CellStyle.titleColor` 優先）、他 Cell は紫（Theme 由来）

#### Scenario: Theme.cellTitleColor が nil の場合のフォールバック

- **GIVEN** `Theme()`（`cellTitleColor = nil`）、`LabelCell(title: "標準")` で `CellStyle.titleColor = nil`
- **WHEN** Cell が描画される
- **THEN** タイトル色はプラットフォーム既定（iOS: `UIColor.label`、Android: `TextView` 既定色）になる

#### Scenario: 旧名 Theme.titleColor のコンパイルエラー

- **GIVEN** 既存コード `Theme(titleColor: UIColor.purple)`（Change 1 以前の旧 API 利用）
- **WHEN** 本 change 適用後の `KsSettingsViewUI` モジュールでコンパイルする
- **THEN** **コンパイルエラー** になる（`Theme` には `titleColor` パラメータが存在せず、`cellTitleColor` に書き換える必要がある）

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

### Requirement: 全 Cell 共通の isVisible

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、すべて `isVisible: Bool`（既定 `true`）フィールドを持たなければならない (SHALL)。

`isVisible: Bool` は AiForms.Maui.SettingsView の `CellBase.IsVisible` 相当のプロパティでなければならない (MUST)。意味は以下：

- `true`（既定値） → 通常の表示。UI 層は当該 Cell を visible projection に含め、描画する。
- `false` → UI 層は当該 Cell を visible projection から除外しなければならない (MUST)。model 上（`Section.cells` 配列内）にはデータとして保持されなければならず (MUST)、`true` に戻したとき元の位置に復活しなければならない (MUST)。

各 Cell の `Hashable` / `Equatable`（iOS）/ `equals` / `hashCode`（Android）実装は、`isVisible` を判定対象に含めなければならない (MUST)。各 Cell の `withDSLID(_:)` / `withStyle(_:)` 実装（iOS）および `data class copy()` 経路（Android）は、`isVisible` を保持しなければならない (MUST)。

#### `isEnabled` との関係

`isVisible` と `isEnabled` は **独立フラグ** として扱わなければならない (MUST)：

- `isVisible = false` のとき、`isEnabled` の値はモデル値として保持されなければならない (MUST) が、描画されないため `isEnabled` の視覚効果（テキスト色置換、UI コントロール無効化等）は発生しない。
- 再び `isVisible = true` に切り替わったとき、保持されていた `isEnabled` の値がそのまま視覚効果として反映されなければならない (MUST)。
- `isVisible = false` の Cell に対する `isEnabled` の変更は、`isVisible = true` に戻ったときに初めて視覚効果として現れる。

#### `VisibilityAware` 抽象への opt-in 準拠（UI 層）

UI 層（iOS `KsSettingsViewUI` / Android `ks-settingsview-ui`）は `VisibilityAware` プロトコル / interface（`var isVisible: Bool { get }` を要求）を提供しなければならない (SHALL)。本変更提案で扱う 7 種の Cell は、すべて `VisibilityAware` に opt-in 準拠しなければならない (MUST)。

`VisibilityAware` 非準拠の Cell（外部 Sample Cell や `CustomCell` 等）は、UI 層のフィルタにおいて常に visible（`true`）として扱われなければならない (MUST)。

Core 抽象 `Cell`（Android）/ `KsCell` プロトコル（iOS）には `isVisible` を要求として追加してはならない (MUST NOT)。

#### Scenario: LabelCell が isVisible を持てる

- **GIVEN** `LabelCell(title: "通知", isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されないが、`SettingsRoot` 上の `Section.cells` 配列内には保持される

#### Scenario: CommandCell が isVisible を持てる

- **GIVEN** `CommandCell(title: "詳細", isVisible: false, onTap: {...})`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されず、タップイベントも発火しない（描画されない結果として）

#### Scenario: SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell が isVisible を持てる

- **GIVEN** チェック系 Cell（例: `SwitchCell(title: "プッシュ", isOn: true, isVisible: false)`）
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されない。`isOn` 等のモデル値は保持される

#### Scenario: ButtonCell が isVisible を持てる

- **GIVEN** `ButtonCell(title: "送信", isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されない

#### Scenario: isVisible 既定値（既存呼び出し互換）

- **GIVEN** 既存のコード `LabelCell(title: "通知")`(`isVisible` を指定しない呼び出し)
- **WHEN** インスタンスを構築する
- **THEN** `isVisible` は既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: isVisible 変更時の構造同期検出

- **GIVEN** 同一 id の Cell について `isVisible = true → false` に変更
- **WHEN** UI 層が再描画する
- **THEN** 構造同期上は当該 Cell が削除として検出される（reconfigure 経路ではなく、構造同期の削除アニメーションとして反映）

#### Scenario: isVisible = false の Cell は isEnabled の視覚効果を発生させない

- **GIVEN** `LabelCell(title: "通知", isEnabled: false, isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されず、`isEnabled = false` のテキスト色置換も発生しない（描画自体が無いため）

#### Scenario: isVisible toggle で isEnabled 状態が保持される

- **GIVEN** `LabelCell(title: "通知", isEnabled: false, isVisible: false)`
- **WHEN** `isVisible` を `true` に変更して再描画
- **THEN** 当該 Cell は描画され、保持されていた `isEnabled = false` のテキスト色置換が反映される

#### Scenario: VisibilityAware 非準拠 Cell は常に表示

- **GIVEN** `VisibilityAware` プロトコル / interface に準拠していない外部 Cell（例: Sample アプリの独自 Cell）が `Section.cells` に含まれる
- **WHEN** UI 層がフィルタする
- **THEN** 当該 Cell は visibility に関するフィルタの判定で常に `true` として扱われ、描画される

#### Scenario: Core 抽象は isVisible を要求しない

- **GIVEN** Core 抽象 `Cell`（Android）/ `KsCell` プロトコル（iOS）の定義
- **WHEN** インターフェース / プロトコルのメンバを確認する
- **THEN** `isVisible` プロパティは要求されない。`isVisible` は UI 層配置の 7 Cell が個別に保持し、UI 層の `VisibilityAware` プロトコル / interface 経由でフィルタ層に opt-in する
