## ADDED Requirements

### Requirement: Material3 派生テーマの使用

Android Sample アプリは、`AndroidManifest.xml` の `android:theme` 属性に `Theme.Material3.*` 派生テーマを指定しなければならない (SHALL)。フレームワーク標準の `@android:style/Theme.Material.*`、`@android:style/Theme.DeviceDefault.*`、`Theme.AppCompat.*`、`Theme.MaterialComponents.*` を使用してはならない (MUST NOT)。これは `ks-settingsview-ui` が以下を内部で使用するためである:

- `com.google.android.material.materialswitch.MaterialSwitch`（SwitchCell）— `?attr/materialSwitchStyle` を要求するため Material3 必須
- `androidx.appcompat.widget.AppCompatCheckBox`（CheckboxCell）
- `androidx.appcompat.widget.AppCompatRadioButton`（RadioCell）
- `androidx.appcompat.widget.AppCompatImageView`（LabelCell 系アイコン）

これらは Material3 非派生テーマでは初期化属性が解決できず、`SwitchCell` の描画時に `SwitchCompat.makeLayout` で `NullPointerException` を発生させて起動時クラッシュを引き起こす（あるいはトラック/サムが描画されない退行を起こす）。

#### Scenario: Material3 テーマの指定

- **GIVEN** Sample アプリの `samples/android/app/src/main/AndroidManifest.xml`
- **WHEN** `<application>` 要素の `android:theme` 属性を確認する
- **THEN** `@style/Theme.Material3.*` 派生の値が設定されている

#### Scenario: 基本 Cell 7 種デモ画面が SwitchCell でクラッシュしない

- **GIVEN** Sample アプリがエミュレータで起動した状態
- **WHEN** トップメニューから「基本 Cell 7 種デモ」を選択する
- **THEN** `SwitchCell` を含む 7 種の Cell すべてが `NullPointerException` 等の例外を発生させずに描画され、`SwitchCell` の `MaterialSwitch` トラック/サム、`CheckboxCell` の `AppCompatCheckBox` 角丸チェックボックス、`RadioCell` の `AppCompatRadioButton` の ring/dot が Material Design 3 風に描画される

#### Scenario: Cell 間の罫線描画

- **GIVEN** Sample アプリの「Store 方式デモ」「DSL 方式デモ」「基本 Cell 7 種デモ」のいずれかの画面
- **WHEN** 描画された各 Cell の境界を確認する
- **THEN** 各 Cell の下端に灰色の 1px 罫線が描画されており、Cell の白背景 (`LabelCellViewHolder.bind` 内の `setBackgroundColor`) によって罫線が上書きされて消えていない

## REMOVED Requirements

### Requirement: Sample 専用 Cell の定義と登録

**Reason**: 本変更提案で `LabelCell` が `public` 公開され、`ks-settingsview-ui` モジュール内 `internal` の PoC Cell (`PocLabelCell`) に依存できないという当時の制約が解消されたため、Sample アプリが独自に `SampleLabelCell` / `SampleLabelCellViewHolder` / `SampleLabelCellDsl` を定義する必要が無くなった。さらに `KsSettingsView.<init>` のデフォルト `registerBasicCells(context)` 自動呼び出しと、Sample 側の `KsCellRegistry.register(SampleLabelCell::class, viewType = CELL_VIEW_TYPE_MIN, ...)` が同一 viewType (100) を競合請求して起動時 `IllegalArgumentException` を発生させるため、Sample 専用 Cell は **必ず削除しなければならない**。

**Migration**:

- `SampleLabelCell.kt` / `SampleLabelCellViewHolder.kt` / `SampleLabelCellDsl.kt` の各 Kotlin ファイルを削除する
- Sample コード中の `SampleLabelCell(title = ...)` は `LabelCell(title = ...)` に直接置換する（`add-cell-types-basic` で公開された本体 `LabelCell` で代替できる）
- Sample 側 `MainActivity.onCreate` 等で行っていた `KsCellRegistry.register(cellClass = SampleLabelCell::class, viewType = KsCellRegistry.CELL_VIEW_TYPE_MIN, factory = ...)` 呼び出しを削除する（`KsSettingsView.<init>` の自動 `registerBasicCells(context)` で `LabelCell` が登録される）
- DSL 経路は `add-cell-types-basic` §1.5.5 で追加された `fun DSLSectionScope.LabelCell(...)` 拡張関数（`ks-settingsview-compose` モジュール）を直接使用する

## MODIFIED Requirements

### Requirement: 基本 Cell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`ks-settingsview-compose` の `@Composable`）を使い、本体公開の `LabelCell` を 1 セクション・複数行含む `SettingsRoot` を `SettingsRootStore` 経由で描画しなければならない (SHALL)。Sample アプリは `SettingsRootStore` の動的更新メソッド（最低でも `insertCell` / `removeCell`）を呼び出すボタン（または相当の UI）を提供し、部分更新の動作確認ができなければならない (MUST)。さらに、`add-cell-types-basic` で導入された 7 種の基本 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）を 1 画面に並べて目視確認できるデモ画面を別途提供しなければならない (SHALL)。

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
- **THEN** `remember { SettingsRootStore(initialRoot = settingsRoot { section { ... } }) }` 等の方法で `SettingsRootStore` が保持され、`KsSettingsView(store = store, ...)` を呼び出している（旧 `KsSettingsView(root = state, onChange = ...)` 形式ではない）

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
- **THEN** `KsSettingsView(store = store, rootHeader = { Text("...") })` のように Compose 引数形式で Root H/F が指定される（旧 `SettingsRoot(header = ..., footer = ..., ...)` 経由ではない）

#### Scenario: 基本 Cell 7 種デモ画面の存在

- **GIVEN** Sample アプリのトップメニュー
- **WHEN** 「基本 Cell 7 種デモ」を選択する
- **THEN** `LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` の 7 種それぞれが Section 単位で描画され、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は状態切替が可能、`CommandCell` / `ButtonCell` はタップで `onTap` が発火する
