## ADDED Requirements

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

## MODIFIED Requirements

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
- **THEN** ボタンテキストの色は赤（Cell 個別 `titleColor` 優先、Theme よりも上位）

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
