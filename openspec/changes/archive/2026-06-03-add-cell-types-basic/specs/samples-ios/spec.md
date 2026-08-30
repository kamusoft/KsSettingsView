## REMOVED Requirements

### Requirement: Sample 専用 Cell の定義と登録

**Reason**: 本変更提案で `LabelCell` が `public` 公開され、`KsSettingsViewUI` モジュール内 `internal` の PoC Cell に依存できないという当時の制約が解消されたため、Sample アプリが独自に `SampleLabelCell` / `SampleLabelCellView` を定義する必要が無くなった。さらに `KsSettingsViewController.init` のデフォルト `registerBasicCells()` 自動呼び出しと、Sample 側の `KsCellRegistry.shared.register(cellType: SampleLabelCell.self, rendererType: SampleLabelCellView.self)` が同一 viewType / 同一 Cell 型に対する競合登録となり、起動時クラッシュを引き起こすため、Sample 専用 Cell は **必ず削除しなければならない**。

**Migration**:

- `SampleLabelCell` / `SampleLabelCellView` / `SampleLabelCellPreview` の各 Swift ファイルを削除する
- Sample コード中の `SampleLabelCell(title: ...)` は `LabelCell(title: ...)` に直接置換する（`add-cell-types-basic` で公開された本体 `LabelCell` で代替できる）
- Sample 側 `KsSettingsViewSampleApp.init` 等で行っていた `KsCellRegistry.shared.register(cellType: SampleLabelCell.self, rendererType: SampleLabelCellView.self)` 呼び出しを削除する（`KsSettingsViewController.init` の自動 `registerBasicCells()` で `LabelCell` が登録される）
- Xcode プロジェクト (`KsSettingsViewSample.xcodeproj/project.pbxproj`) から該当 3 ファイルの `PBXBuildFile` / `PBXFileReference` / `PBXGroup` children / `PBXSourcesBuildPhase` 参照を削除する

## MODIFIED Requirements

### Requirement: 基本 Cell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`KsSettingsViewSwiftUI`）を使い、本体公開の `LabelCell` を 1 セクション・複数行含む `SettingsRoot` を `SettingsRootStore` 経由で描画しなければならない (SHALL)。Sample アプリは `SettingsRootStore` の動的更新メソッド（最低でも `insertCell` / `removeCell`）を呼び出すボタン（または相当の UI）を提供し、部分更新の動作確認ができなければならない (MUST)。さらに、`add-cell-types-basic` で導入された 7 種の基本 Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）を 1 画面に並べて目視確認できるデモ画面を別途提供しなければならない (SHALL)。

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
- **THEN** `KsSettingsView(store: store).header(.text("..."))` のように View modifier 形式で Root H/F が指定される（旧 `SettingsRoot(header:, footer:, ...)` 経由ではない）

#### Scenario: 基本 Cell 7 種デモ画面の存在

- **GIVEN** Sample アプリのトップメニュー
- **WHEN** 「基本 Cell 7 種デモ」ナビゲーションリンクを選択する
- **THEN** `LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` の 7 種それぞれが Section 単位で描画され、`SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` は状態切替が可能、`CommandCell` / `ButtonCell` はタップで `onTap` が発火する
