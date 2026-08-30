# Delta: settings-view-android-ui (harden-compose-settingsroot-dsl)

## ADDED Requirements

### Requirement: settingsRoot builder の section は Section と同じ属性を受け取る

`settingsRoot { }` の `section(...)` は、accessory 版・文字列ヘッダ版の両オーバーロードで `headerHeight` / `isVisible` / `isHeaderVisible` / `isFooterVisible` を受け取り、生成する `Section` へそのまま転写する (SHALL)。文字列ヘッダ版は文字列 `footer` (省略可) も受け取り、`SectionAccessory.Text` に包んで転写する (SHALL)。各引数の既定値は core `Section` data class の既定値と同一とし、省略時に生成される `Section` は同じ id・header・footer・cells で直接構築した `Section` と等価である (SHALL)。引数の並びは両オーバーロードとも core `Section` (`header, footer, headerHeight, isVisible, isHeaderVisible, isFooterVisible`) に揃え、末尾ラムダ `block` を最後に置く (SHALL)。

#### Scenario: headerHeight を指定して構築する
- **GIVEN** `settingsRoot { }` 内で `section(id, header = SectionAccessory.Text(...), headerHeight = 40.0) { ... }` を呼ぶ
- **WHEN** `SettingsRoot` を構築する
- **THEN** 生成された `Section.headerHeight` は `40.0` である

#### Scenario: Header / Footer の表示トグルを指定して構築する (accessory 版)
- **GIVEN** `section(id, header = ..., footer = ..., isHeaderVisible = false, isFooterVisible = false) { ... }` を呼ぶ
- **WHEN** `SettingsRoot` を構築する
- **THEN** 生成された `Section` の `isHeaderVisible` / `isFooterVisible` はともに `false` で、`header` / `footer` の内容は保持されている

#### Scenario: 文字列ヘッダ版でも同じ属性を指定できる
- **GIVEN** `section(id, header = "一般", footer = "補足", headerHeight = 40.0, isVisible = false, isHeaderVisible = false, isFooterVisible = false) { ... }` を呼ぶ
- **WHEN** `SettingsRoot` を構築する
- **THEN** 生成された `Section` は `header == SectionAccessory.Text("一般")`、`footer == SectionAccessory.Text("補足")`、`headerHeight == 40.0`、`isVisible == false`、`isHeaderVisible == false`、`isFooterVisible == false` である

#### Scenario: 文字列ヘッダ版で footer を省略すると footer は無い
- **GIVEN** `section(id, header = "一般") { ... }` のように `footer` を省略して呼ぶ
- **WHEN** `SettingsRoot` を構築する
- **THEN** 生成された `Section.footer` は `null` である (現行の文字列ヘッダ版と同じ結果)

#### Scenario: 省略時は Section data class の既定値と等価
- **GIVEN** `section(id = "s1", header = SectionAccessory.Text("A")) { cell(c1) }` のように新引数を省略して呼ぶ
- **WHEN** `SettingsRoot` を構築する
- **THEN** 生成された `Section` は `Section(id = "s1", header = SectionAccessory.Text("A"), cells = listOf(c1))` と等価である (`headerHeight` / `isVisible` / `isHeaderVisible` / `isFooterVisible` が data class の既定値に一致する)

#### Scenario: 位置引数で規定の並びどおりに呼び出せる
- **GIVEN** accessory 版を `section("s1", SectionAccessory.Text("H"), SectionAccessory.Text("F"), 40.0, false, false, false) { ... }`、文字列ヘッダ版を `section("s2", "H", "F", 40.0, false, false, false) { ... }` のように名前を付けずに呼ぶ
- **WHEN** `SettingsRoot` を構築する
- **THEN** どちらの `Section` も `header == Text("H")`、`footer == Text("F")`、`headerHeight == 40.0`、`isVisible == false`、`isHeaderVisible == false`、`isFooterVisible == false` である (各値が規定の位置の引数に対応する)

### Requirement: SettingsRootDsl marker は型にのみ付与できる

DSL marker 注釈 `SettingsRootDsl` は型宣言・型使用・型エイリアス (`CLASS` / `TYPE` / `TYPEALIAS`) にのみ付与できる (SHALL)。関数・プロパティへの付与はコンパイルエラーになる。`settingsRoot { }` / `KsSettingsView { }` の receiver 型 (`SettingsRootScope` / `SectionScope` / `DSLSettingsRootScope` / `DSLSectionScope` / `SectionHandle` / `CellHandle`) には引き続き marker が付与されている (SHALL)。`ks-settingsview-compose` のコンパイルで KT-81567 (DSL marker annotation ... has no effect) の警告は出ない (SHALL)。

補足 (非規範): 入れ子ラムダ内で外側 receiver を暗黙に呼び出せないスコープ制御は receiver 型側の marker だけに由来するため、top-level 関数から注釈を除去しても変化しない。本変更はこれを新たに契約化せず、実装時の手動実証で確認するにとどめる (自動テストによる固定は proposal の Non-Goals)。

#### Scenario: marker 注釈の許容ターゲットが型に限定されている
- **GIVEN** `SettingsRootDsl` 注釈クラス
- **WHEN** 注釈クラスの `@Target` をリフレクションで読む
- **THEN** 許容ターゲットは `CLASS` / `TYPE` / `TYPEALIAS` の 3 つだけである

#### Scenario: receiver 型の marker 付与が維持されている
- **GIVEN** `SettingsRootScope` / `SectionScope` / `DSLSettingsRootScope` / `DSLSectionScope` / `SectionHandle` / `CellHandle` の各クラス
- **WHEN** 各クラスの注釈をリフレクションで読む
- **THEN** いずれにも `SettingsRootDsl` が付与されている

#### Scenario: ビルドで DSL marker の無効付与警告が出ない
- **GIVEN** `@SettingsRootDsl` を top-level 拡張関数から除去した `ks-settingsview-compose`
- **WHEN** コンパイルタスクを強制再実行 (`--rerun-tasks`) して module をコンパイルする
- **THEN** コンパイルタスクが実際に実行され、ビルドログに KT-81567 の警告が 1 件も出ない
