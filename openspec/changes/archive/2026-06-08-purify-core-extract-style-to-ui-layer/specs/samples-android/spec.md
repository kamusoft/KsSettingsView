## MODIFIED Requirements

### Requirement: 基本 Cell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`ks-settingsview-compose` の `@Composable`）を使い、本体公開の `LabelCell` を 1 セクション・複数行含む `SettingsRoot` を `SettingsRootStore` 経由で描画しなければならない (SHALL)。Sample アプリは `SettingsRootStore` の動的更新メソッド（最低でも `insertCell` / `removeCell`）を呼び出すボタン（または相当の UI）を提供し、部分更新の動作確認ができなければならない (MUST)。さらに、`add-cell-types-basic` で導入された 7 種の基本 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）を 1 画面に並べて目視確認できるデモ画面を別途提供しなければならない (SHALL)。

「基本 Cell 7 種デモ画面」は **Cell タイプ別のセクション構成** で実装されなければならない (MUST)。各セクションのセクション名は **Cell タイプ名そのもの**（例: `"CommandCell"`、`"LabelCell"`）とし、iOS 版と一字一句揃えなければならない (MUST)。

- **MAUI 互換 Theme の明示渡し** — `KsSettingsView(theme = ..., ...)` 引数として MAUI 互換の Theme を渡す。**Theme 構築は Compose `androidx.compose.ui.graphics.Color` 直接構築でなければならない (MUST)。`KsColor` は使ってはならない (MUST NOT)**。最低限以下のフィールドを `Color(0xAARRGGBB)` または `Color(red = ..., green = ..., blue = ..., alpha = ...)` 形式で指定する：
  - `viewBackgroundColor = Color(0xFFF2EFE6)`（PaleBackColorPrimary 相当）
  - `cellBackgroundColor = Color.White`
  - `separatorColor = Color(0xFFE6DAB9)`（DisabledColor 相当）
  - `selectedColor = Color(0x50FFBF00)`（AccentColor の半透明 30%）
  - `cellAccentColor = Color(0xFFFFBF00)`（AccentColor）
  - `titleColor = Color(0xFFCC9900)`（TitleTextColor）
  - `headerTextColor = Color(0xFFCC9900)`（TitleTextColor）
  - `headerBackgroundColor = Color(0xFFF2EFE6)`
  - `footerTextColor = Color(0xFF999999)`（PaleTextColor）
  - `hasUnevenRows = true`
  - `disabledTextColor = Color(0xFF999999)`

- **Section 構成（Cell タイプ別、この順序、iOS 版と一字一句一致）** — 各セクション名は Cell タイプ名そのもの、各 Cell 数は 1〜3（RadioCell のみ最低 2 必須）：

  1. **`"CommandCell"` セクション**（3 個）
     - Cell 1: フル構成（`icon = KsImage.Resource(R.drawable.ic_account_circle)`、`title = "Tanaka Taro"`、`description = "tanaka.taro@example.com"`、`CellStyle(cellHeight = 80)`、`onTap` 有り）
     - Cell 2: シンプル（`title = "プロフィール"`、`onTap` 有り）
     - Cell 3: 中間（`title = "通知設定"`、`valueText = "オン"`、`onTap` 有り）
  2. **`"LabelCell"` セクション**（2 個）
     - Cell 1: フル構成（`icon = KsImage.Resource(R.drawable.ic_storage)`、`title = "Storage"`、`description = "This is description. you can write detail explanation of the item here. long text wrap automatically."`、`valueText = "256 GB"`）
     - Cell 2: シンプル（`title = "バージョン"`、`valueText = "1.0.0"`）
  3. **`"SwitchCell"` セクション**（1 個）
     - Cell 1: `title = "Notification"`、`description = "This is description. you can write detail explanation of the item here. long text wrap automatically."`、`isOn = true`
  4. **`"CheckboxCell"` セクション**（1 個）
     - Cell 1: `title = "Agree to Terms"`、`isChecked = true`
  5. **`"RadioCell"` セクション**（2 個、最低 2 必須）
     - Cell 1: `title = "TypeA"`、`groupId = "type"`、`isSelected = true`
     - Cell 2: `title = "TypeB"`、`groupId = "type"`、`isSelected = false`
     - footer テキスト: `"You can select either TypeA or TypeB."`
  6. **`"SimpleCheckCell"` セクション**（3 個）
     - Cell 1〜3: `title = "Item 1"` / `"Item 2"` / `"Item 3"`
  7. **`"ButtonCell"` セクション**（1 個）
     - Cell 1: `title = "ログアウト"`、`titleAlignment = CellTitleAlignment.CENTER`（既定）

- アイコンリソース（`R.drawable.ic_account_circle` / `R.drawable.ic_storage` 等）は `samples/android/app/src/main/res/drawable/` に Material Symbols 由来の VectorDrawable として配置する。
- 「最後にタップ: ...」の現在値表示など、状態確認用の補助 UI を任意で配置してよい。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリがエミュレータで起動した直後
- **WHEN** トップメニューから「Store 方式デモ」を選択する
- **THEN** `KsSettingsView` が画面いっぱいに表示され、`LabelCell` の `title` を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリの Store 方式デモ画面
- **WHEN** 描画されたセクションを確認する
- **THEN** `SectionAccessory.Text(...)` 形式のヘッダおよびフッタが、対応する文字列でセクション境界に表示される

#### Scenario: SettingsRootStore + Compose ラッパの使用

- **GIVEN** Sample のソースコードを参照する
- **WHEN** Store 方式デモ画面の起点 Composable を確認する
- **THEN** `remember { SettingsRootStore(initialRoot = settingsRoot { section { ... } }, initialTheme = ...) }` 等の方法で `SettingsRootStore` が保持され、`KsSettingsView(store = store, ...)` を呼び出している

#### Scenario: Cell 追加ボタンの動作

- **GIVEN** Sample アプリの Store 方式デモ画面に「項目追加」ボタンが存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.insertCell(LabelCell(title = "新規 \$index"), sectionId = firstSectionId, at = 末尾)` が呼ばれ、画面に新しい Cell 行が挿入アニメーションで追加される

#### Scenario: Cell 削除ボタンの動作

- **GIVEN** Sample アプリの Store 方式デモ画面に「項目削除」ボタンが存在し、削除可能な Cell が複数存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.removeCell(cellId = ...)` が呼ばれ、対応する Cell 行が削除アニメーションで消える

#### Scenario: Root H/F の指定（Compose 引数）

- **GIVEN** Sample の Store 方式デモ画面または DSL 方式デモ画面
- **WHEN** Root Header を表示する場合のコードを確認する
- **THEN** `KsSettingsView(store = store, rootHeader = { Text("...") })` のように Compose 引数形式で Root H/F が指定される

#### Scenario: 基本 Cell 7 種デモ画面の存在

- **GIVEN** Sample アプリのトップメニュー
- **WHEN** 「基本 Cell 7 種デモ」を選択する
- **THEN** Cell タイプ別の 7 セクションが順に描画され、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は状態切替が可能、`CommandCell` / `ButtonCell` はタップで `onTap` が発火する

#### Scenario: MAUI 互換 Theme の適用（Compose Color 直接構築）

- **GIVEN** 基本 Cell 7 種デモ画面のソースコード
- **WHEN** Theme 構築箇所を確認する
- **THEN** すべての色フィールドが `Color(0xAARRGGBB)` 形式または `Color.White` などの組み込み色で構築されており、`KsColor(...)` 形式は一切使われていない

#### Scenario: MAUI 互換 Theme の表示反映

- **GIVEN** 基本 Cell 7 種デモ画面
- **WHEN** 起動して画面を観察する
- **THEN** 全体の背景がベージュ系（`#F2EFE6`）が **セクション間の隙間も含めて画面全体に反映**され、セル背景が白、セクションヘッダ文字が黄系（`#CC9900`）、SwitchCell の ON 時 thumb / track と CheckboxCell の Checked 時のボックスが `#FFBF00` 系で表示される

#### Scenario: 長文 Description の折返し

- **GIVEN** 基本 Cell 7 種デモ画面、`hasUnevenRows = true` が設定されている
- **WHEN** SwitchCell または LabelCell に長文 description が指定されている
- **THEN** description は折返して 2 行以上で表示され、当該 Cell の高さは他 Cell よりも大きくなる

#### Scenario: タッチフィードバック（Ripple）の目視確認

- **GIVEN** 基本 Cell 7 種デモ画面で `Theme.selectedColor = Color(0x50FFBF00)`
- **WHEN** ユーザーが CommandCell や LabelCell をタップする
- **THEN** Cell の背景に `#50FFBF00` 系の Ripple エフェクトが発生し、リリース後に元の cellBackgroundColor に戻る

#### Scenario: 右端アクセサリ位置の整列

- **GIVEN** 基本 Cell 7 種デモ画面に SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell が順に並ぶ
- **WHEN** 描画される
- **THEN** 各アクセサリ要素の右端 X 座標が ±1px 以内で一致する（CheckboxCell が他より内側にずれない）

#### Scenario: SwitchCell の Thumb / Track 色分離

- **GIVEN** SwitchCell が `isOn = true`、`Theme.cellAccentColor = Color(0xFFFFBF00)`
- **WHEN** Android で描画される
- **THEN** Track はオレンジ色（`#FFBF00`）で、Thumb は白系（`colorOnPrimary` 相当）で描画され、視覚的に Thumb と Track が分離して見える

#### Scenario: アイコン解決の動作確認

- **GIVEN** CommandCell / LabelCell に `KsImage.Resource(R.drawable.xxx)` でアイコンが指定されている
- **WHEN** Android で描画される
- **THEN** 指定リソース ID の VectorDrawable が実際にアイコン領域に描画される（Cell 左端にアイコンが見える）

#### Scenario: セル高さの均一性（HasUnevenRows = false 想定の補助確認）

- **GIVEN** デモ画面の Theme を一時的に `hasUnevenRows = false, rowHeight = 60` に切り替える補助 UI を持つ（あるいはコード上で確認できる）
- **WHEN** SwitchCell / CheckboxCell / LabelCell を並べて描画する
- **THEN** すべての Cell の高さが 60dp（密度補正後の同一 px 値）に揃い、SwitchCell / CheckboxCell が他より大きくならない

#### Scenario: Section 構成の Cell タイプ別並び

- **GIVEN** 基本 Cell 7 種デモ画面のソースコード
- **WHEN** ソースを参照する
- **THEN** Section は `"CommandCell"` → `"LabelCell"` → `"SwitchCell"` → `"CheckboxCell"` → `"RadioCell"` → `"SimpleCheckCell"` → `"ButtonCell"` の順で並ぶ。各セクション名は Cell タイプ名そのものである

#### Scenario: iOS / Android 間の表記揃え

- **GIVEN** iOS 版と Android 版の基本 Cell 7 種デモ画面
- **WHEN** 両方の画面のセクション名・Cell タイトル・Cell description・Cell valueText・Footer テキストを比較する
- **THEN** すべての文字列が一字一句一致する（差分はゼロ）。**Theme の色値も hex 表現で同じ値（例: `0xF2EFE6`）を使い、Android 側は `Color(0xFFF2EFE6)`、iOS 側は `UIColor(red: 0xF2/255, ...)` のように記述形式は異なるが論理値は一致する**

#### Scenario: Material Symbols VectorDrawable の存在

- **GIVEN** `samples/android/app/src/main/res/drawable/`
- **WHEN** ディレクトリ内のリソースを確認する
- **THEN** CommandCell / LabelCell のアイコン指定で使う VectorDrawable（例: `ic_account_circle.xml` / `ic_storage.xml`）が存在し、Material Symbols 由来のシンプルな線画アイコンが格納されている

#### Scenario: Section.headerHeight 明示指定のサンプル

- **GIVEN** 基本 Cell 7 種デモ画面のソースコード
- **WHEN** ソースを参照する
- **THEN** Compose DSL の `Section(...)` 呼び出しのうち少なくとも 1 箇所が `headerHeight = 60.0`（または明示的な正値）を渡しており、当該セクションのヘッダが固定高さで描画される
