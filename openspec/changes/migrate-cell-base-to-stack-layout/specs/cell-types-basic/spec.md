# cell-types-basic Specification 差分（migrate-cell-base-to-stack-layout）

## MODIFIED Requirements

### Requirement: 全 Cell 共通の description / valueText / icon / hintText フィールド

本 change で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、すべて以下の **共通 Optional フィールド** を持たなければならない (SHALL)：

- `description: String?`（既定 `nil`）— Cell タイトル下に副題として表示
- `valueText: String?`（既定 `nil`）— Cell タイトル右側に値テキストとして表示
- `icon: KsImage?`（既定 `nil`）— Cell タイトル左側にアイコンとして表示
- `hintText: String?`（既定 `nil`）— Cell 右上に float 表示するヒントテキスト

ただし `ButtonCell` は **`description` フィールドを持たない例外** とする (MUST NOT)。これはオリジナル `AiForms.Maui.SettingsView/SettingsView/Cells/ButtonCell.cs` が `Description` / `DescriptionColor` / `DescriptionFontSize` を `private new` で隠蔽し、iOS の `ButtonCellView.cs` も `DescriptionLabel.Hidden = true` としている挙動を踏襲するためである。`ButtonCell` は `valueText` / `icon` / `hintText` の 3 フィールドのみ追加される。

各フィールドは `nil` のとき非表示としなければならない (MUST)。Cell 内のレイアウトはオリジナル `AiForms.Maui.SettingsView` の `CellBase`（iOS `UIStackView` ベース、Android `RelativeLayout` ベース）に準拠し、以下の 2 系統で配置しなければならない (MUST)：

- **本体行（横方向）**: 「`[icon][title / description][valueText (title 行の右寄せ)][trailing controls (右側中央)]`」の順で配置する。`trailing controls` は各 Cell 種別固有のコントロール（`SwitchCell` の `UISwitch` / `Switch`、`CheckboxCell` の MaterialCheckBox 等、`CommandCell` の chevron、`LabelCell` / `ButtonCell` の `nil`）に対応し、セル右側中央に配置される。
- **hintText（右上 float）**: `hintText` は本体行とは別系統として **セル右上に float 配置** しなければならない (MUST)。具体的には、セル上端から数 dp 程度のマージン、セル右端から数 dp 程度のマージンで右上に置く。`trailing controls` と `hintText` は両者とも右端揃いとなるため物理的に重なり得るが、`hintText` がセル上端基準、`trailing controls` がセル縦中央基準で配置されるため通常は干渉しない。万一の干渉時は `hintText` を前面（`trailing controls` より手前）に配置する。

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

#### Scenario: hintText は右上 float、trailing controls は右側中央で物理的に重ならない

- **GIVEN** 任意の Cell（例: `SwitchCell(title: "通知", hintText: "推奨", isOn: true)` または `RadioCell(title: "ダーク", hintText: "推奨", value: "dark", selectedValue: "dark")`）
- **WHEN** 描画される
- **THEN** `hintText` 「推奨」はセル上端から数 dp 程度のマージンでセル右上に float 配置され、`trailing controls`（UISwitch / チェックマーク等）はセル縦中央に配置される。両者は右端揃いだが上端 vs 縦中央という縦位置の違いにより通常は物理的に重ならない。万一の重なり（hintText が異常に大きい等）が生じた場合は `hintText` を `trailing controls` より前面に配置する

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
- **THEN** UI 層は内部的に共通行レイアウト関数（iOS: `applyCellBaseLayout(...)`、Android: `applyCellBaseLayout(views, ...)`）を経由して `title` / `description` / `valueText` / `icon` / `hintText` を配置し、`trailingViews`（iOS）/ `accessoryHolder`（Android）の slot にのみ Cell 種別固有のコントロール（UISwitch / MaterialCheckBox 等）を組む。Cell View / ViewHolder 側に title / description / icon / hintText のレイアウトロジックを重複実装してはならない (MUST NOT)。iOS では `UIListContentConfiguration` / `UICellAccessory` 経路を使ってはならない (MUST NOT)。代わりに `KsListCellBase` が `contentView` 直下に install する自前 `UIStackView` 階層（`stackH` / `stackV` / `contentStack` / `descriptionLabel`）を更新する形で描画する

#### Scenario: 既存 LabelCell / CommandCell も共通行レイアウト関数を経由する

- **GIVEN** 本 change で新規追加された Cell（Switch/Checkbox/Radio/SimpleCheck/Button）だけでなく、**既存の `LabelCell` および `CommandCell`** も同じ共通行レイアウト関数経由で描画される必要がある
- **WHEN** `LabelCell(title: "プロフィール", description: "X", valueText: "Y", icon: KsImage.systemName("person"), hintText: "新着")` または `CommandCell(title: "ライセンス", description: "X", icon: KsImage.systemName("doc"), hideArrow: false, onTap: {...})` を描画する
- **THEN** いずれも共通行レイアウト関数（iOS: `applyCellBaseLayout(...)`、Android: `applyCellBaseLayout(views, ...)`）を経由し、`LabelCell` は `trailingViews: []`（または `valueLabelText` のみで自動 trailing）、`CommandCell` は `trailingViews: [makeChevronView()]` で chevron を組む。これにより 7 種すべての Cell が「本体行 `[icon][title / description][valueText (title 行右寄せ)][trailing controls (右側中央)]` + `hintText` 右上 float」の共通レイアウト規約を満たす。後続変更提案で追加される新規 Cell 種別も同じ共通行レイアウト関数を経由しなければならない (MUST)

#### Scenario: iOS では自前 UIStackView 階層が使われ UIListContentConfiguration は使われない

- **GIVEN** 任意の Cell renderer が `applyCellBaseLayout(...)` を呼び出した直後の `KsListCellBase` インスタンス
- **WHEN** `cell.contentConfiguration` と `cell.accessories` を観察する
- **THEN** `cell.contentConfiguration == nil` かつ `cell.accessories == []` でなければならない (MUST)。代わりに `cell.titleLabel.text == title` / `cell.descriptionLabel.text == description (空文字列は isHidden=true)` / `cell.iconImageView.image == icon の解決画像 (nil は isHidden=true)` / `cell.contentStack.arrangedSubviews` の `[0]` が `cell.titleLabel`、`[1]` 以降が呼び出し側 `trailingViews` 順で並ぶ
