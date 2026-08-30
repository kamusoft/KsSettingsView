## MODIFIED Requirements

### Requirement: 基本 Cell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`KsSettingsViewSwiftUI`）を使い、本体公開の `LabelCell` を 1 セクション・複数行含む `SettingsRoot` を `SettingsRootStore` 経由で描画しなければならない (SHALL)。Sample アプリは `SettingsRootStore` の動的更新メソッド（最低でも `insertCell` / `removeCell`）を呼び出すボタン（または相当の UI）を提供し、部分更新の動作確認ができなければならない (MUST)。さらに、`add-cell-types-basic` で導入された 7 種の基本 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）を 1 画面に並べて目視確認できるデモ画面を別途提供しなければならない (SHALL)。

「基本 Cell 7 種デモ画面」は移植元 `AiForms.Maui.SettingsView` の `Sample/Views/MainPage.xaml` の見た目と限りなく一致するよう、以下の構成で実装されなければならない (MUST)：

- **MAUI 互換 Theme の明示渡し** — `KsSettingsView { ... }` または `SettingsRootStore` の初期 `Theme` として MAUI 互換の Theme を渡す。最低限以下のフィールドを指定する：
  - `viewBackgroundColor = #F2EFE6`（PaleBackColorPrimary 相当）
  - `cellBackgroundColor = #FFFFFF`
  - `separatorColor = #E6DAB9`（DisabledColor 相当）
  - `selectedColor = #50FFBF00`（AccentColor の半透明 30%）
  - `cellAccentColor = #FFBF00`（AccentColor）
  - `headerTextColor = #CC9900`（TitleTextColor）
  - `headerBackgroundColor = #F2EFE6`
  - `footerTextColor = #999999`（PaleTextColor）
  - `hasUnevenRows = true`
  - `disabledTextColor = #999999`
- **Section 構成** — 以下のセクションをこの順序で含む：
  1. `CommandCell`（プロフィール風、`icon` + `description` + `CellStyle(cellHeight: 80)`、長文 description の折返し確認）
  2. `LabelCell`（Storage 例、`icon` + `valueText`、`description` を 2 行以上の長文で）
  3. `SwitchCell`（長文 `description` 付き、`isOn = true`）+ `CheckboxCell`（`isChecked = true`、accent 反映確認）
  4. `RadioCell` 群（`groupId = "type"`、`TypeA` / `TypeB` 2 件、Section の `footer` テキストに `"You can select either TypeA or TypeB."` を指定）
  5. `SimpleCheckCell` 複数件（任意セクション）
  6. `ButtonCell`（`titleAlignment = .center` 既定、`CellStyle(titleColor: ...)` で TitleTextColor 反映確認）
  7. 任意 1 セルで `hintText` を指定（Hint 描画確認）
- 画面上には「最後にタップ: ...」の現在値表示など、状態確認用の補助 UI を任意で配置してよい。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリがシミュレータで起動した直後
- **WHEN** トップメニューから「Store 方式デモ」を選択する
- **THEN** `KsSettingsView` が画面いっぱいに表示され、`LabelCell` の `title` を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリの Store 方式デモ画面
- **WHEN** 描画されたセクションを確認する
- **THEN** `SectionAccessory.text(...)` 形式のヘッダおよびフッタが、対応する文字列でセクション境界に表示される

#### Scenario: SettingsRootStore + SwiftUI ラッパの使用

- **GIVEN** Sample のソースコードを参照する
- **WHEN** Store 方式デモ画面（例: `StoreDemoView`）の本文を確認する
- **THEN** 当該 View 内に `@StateObject private var store: SettingsRootStore = SettingsRootStore(initialRoot: ...)` が宣言されており、`KsSettingsView(store: store)` を `body` から返している。`store` の初期 root は `SettingsRootBuilder` / `SectionBuilder` の DSL（`SettingsRoot { Section { ... } }` 形式）で構築されている

#### Scenario: Cell 追加ボタンの動作

- **GIVEN** Sample アプリの Store 方式デモ画面に「項目追加」ボタンが存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.insertCell(LabelCell(title: "新規 \(index)"), in: firstSectionID, at: 末尾)` が呼ばれ、画面に新しい Cell 行が挿入アニメーションで追加される

#### Scenario: Cell 削除ボタンの動作

- **GIVEN** Sample アプリの Store 方式デモ画面に「項目削除」ボタンが存在し、削除可能な Cell が複数存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.removeCell(cellID: ...)` が呼ばれ、対応する Cell 行が削除アニメーションで消える

#### Scenario: Root H/F の指定（View modifier）

- **GIVEN** Sample の Store 方式デモ画面または DSL 方式デモ画面
- **WHEN** Root Header を表示する場合のコードを確認する
- **THEN** `KsSettingsView(store: store).rootHeader("...")` のように View modifier 形式で Root H/F が指定される（旧 `SettingsRoot(header:, footer:, ...)` または旧 `.header(.text("..."))` 経由ではない）

#### Scenario: 基本 Cell 7 種デモ画面の存在

- **GIVEN** Sample アプリのトップメニュー
- **WHEN** 「基本 Cell 7 種デモ」ナビゲーションリンクを選択する
- **THEN** `LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` の 7 種それぞれが Section 単位で描画され、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は状態切替が可能、`CommandCell` / `ButtonCell` はタップで `onTap` が発火する

#### Scenario: MAUI 互換 Theme の適用

- **GIVEN** 基本 Cell 7 種デモ画面
- **WHEN** 起動して画面を観察する
- **THEN** 全体の背景がベージュ系（`#F2EFE6`）、セル背景が白、セクションヘッダ文字が黄系（`#CC9900`）、SwitchCell / CheckboxCell の ON / Checked 色が `#FFBF00` 系で表示される

#### Scenario: 長文 Description の折返し

- **GIVEN** 基本 Cell 7 種デモ画面、`hasUnevenRows = true` が設定されている
- **WHEN** SwitchCell または LabelCell に長文 description（`"This is description. you can write detail explanation of the item here. long text wrap automatically."` 相当）が指定されている
- **THEN** description は折返して 2 行以上で表示され、当該 Cell の高さは他 Cell よりも大きくなる

#### Scenario: タッチフィードバックの目視確認

- **GIVEN** 基本 Cell 7 種デモ画面で `Theme.selectedColor = #50FFBF00`
- **WHEN** ユーザーが CommandCell や LabelCell をタップして指を離さない
- **THEN** タップ中に背景色が `#50FFBF00` に変化し、リリース後に元に戻る

#### Scenario: HintText の表示

- **GIVEN** 基本 Cell 7 種デモ画面で 1 セル以上に `hintText` が指定されている
- **WHEN** 表示される
- **THEN** 当該 Cell の右上または所定位置に HintText が表示される

#### Scenario: CommandCell.icon と CellStyle.cellHeight の反映

- **GIVEN** プロフィール風 CommandCell が `icon` と `CellStyle(cellHeight: 80)` を持つ
- **WHEN** 表示される
- **THEN** 当該 Cell の高さが他 Cell よりも大きく `80pt` 程度になり、icon が左端に表示される
