## MODIFIED Requirements

### Requirement: SampleLabelCell を含むデモ画面

Sample アプリの起動直後の画面は、`KsSettingsView`（`KsSettingsViewSwiftUI`）を使い、`SampleLabelCell` を 1 セクション・複数行含む `SettingsRoot` を `SettingsRootStore` 経由で描画しなければならない (SHALL)。Sample アプリは `SettingsRootStore` の動的更新メソッド（最低でも `insertCell` / `removeCell`）を呼び出すボタン（または相当の UI）を提供し、部分更新の動作確認ができなければならない (MUST)。

#### Scenario: 起動時の画面表示

- **GIVEN** Sample アプリがシミュレータで起動した直後
- **WHEN** 画面のコンテンツを確認する
- **THEN** `KsSettingsView` が画面いっぱいに表示され、`SampleLabelCell` の `title` を含む 1 行のセルが複数行（2 行以上）描画される

#### Scenario: Section ヘッダ・フッタの描画

- **GIVEN** Sample アプリがシミュレータで起動した直後
- **WHEN** 描画されたセクションを確認する
- **THEN** `SectionAccessory.text(...)` 形式のヘッダおよびフッタが、対応する文字列でセクション境界に表示される

#### Scenario: SettingsRootStore + SwiftUI ラッパの使用

- **GIVEN** Sample のソースコードを参照する
- **WHEN** `ContentView` の本文を確認する
- **THEN** `ContentView` 内に `@StateObject private var store: SettingsRootStore = SettingsRootStore(initialRoot: ...)` が宣言されており、`KsSettingsView(store: store)` を `body` から返している。`store` の初期 root は `SettingsRootBuilder` / `SectionBuilder` の DSL（`SettingsRoot { Section { ... } }` 形式）で構築されている

#### Scenario: Cell 追加ボタンの動作

- **GIVEN** Sample アプリの起動画面に「項目追加」ボタンが存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.insertCell(SampleLabelCell(title: "新規 \(index)"), in: firstSectionID, at: 末尾)` が呼ばれ、画面に新しい Cell 行が挿入アニメーションで追加される

#### Scenario: Cell 削除ボタンの動作

- **GIVEN** Sample アプリの起動画面に「項目削除」ボタンが存在し、削除可能な Cell が複数存在する
- **WHEN** 利用者がボタンを押下する
- **THEN** `store.removeCell(cellID: ...)` が呼ばれ、対応する Cell 行が削除アニメーションで消える

#### Scenario: Root H/F の指定（View modifier）

- **GIVEN** Sample の `ContentView`
- **WHEN** Root Header を表示する場合のコードを確認する
- **THEN** `KsSettingsView(store: store).header(.text("..."))` のように View modifier 形式で Root H/F が指定される（旧 `SettingsRoot(header:, footer:, ...)` 経由ではない）
