## MODIFIED Requirements

### Requirement: Section ドメインモデル

`Section` は単一セクションを表す値型でなければならない (SHALL)。一意な `id`、任意の `header`（`SectionAccessory?`）、任意の `footer`（`SectionAccessory?`）、`Cell` のリスト、および `headerHeight: Double`（既定 `-1`）を保持しなければならない (MUST)。`header` / `footer` は文字列のみならず、任意の View を `KsAnyView` 経由で格納できなければならない (MUST)。Cell（タップ・選択・編集する行）は `cells` フィールドにのみ格納し、`header` / `footer` には格納しない (MUST NOT)。

`headerHeight: Double` は AiForms.Maui.SettingsView の `Section.HeaderHeight` 相当のプロパティでなければならない (MUST)。意味は以下：

- `-1`（既定値） → 「自動高さ」を意味し、`header` テキストが空または未設定の場合は UI 層は Section Header の supplementary 自体を生成してはならない (MUST NOT)。`header` テキストが存在する場合は UI 層がテキスト寸法に基づいて自動算出する。
- 正値（> 0） → その値を固定高さとして用いる。

#### Scenario: Section の構築（文字列ヘッダ）

- **GIVEN** id・header（`SectionAccessory.text("一般")`）・footer（`nil`）・cells リスト
- **WHEN** `Section` を構築する
- **THEN** すべてのフィールドを保持するイミュータブル値型として生成され、`header` から元の文字列 `"一般"` を取り出せる

#### Scenario: Section の構築（任意 View ヘッダ）

- **GIVEN** id・header（`SectionAccessory.view(anyView)`）・footer（`nil`）・cells リスト（`anyView` は任意 View をラップした `KsAnyView`）
- **WHEN** `Section` を構築する
- **THEN** `header` から元の `KsAnyView` を取り出せ、UI 層が任意 View として描画する根拠となる

#### Scenario: 空セクション

- **GIVEN** cells が空リストの `Section`
- **WHEN** インスタンスを生成する
- **THEN** 例外なく構築でき、`cells.isEmpty` が真となる

#### Scenario: headerHeight 既定値

- **GIVEN** `Section(id: ..., header: nil, footer: nil, cells: [...])`（`headerHeight` を指定しない）
- **WHEN** インスタンスを生成する
- **THEN** `section.headerHeight == -1`（自動）が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: headerHeight 明示指定（固定高さ）

- **GIVEN** `Section(id: ..., header: SectionAccessory.text("一般"), footer: nil, cells: [...], headerHeight: 40)`
- **WHEN** 値を参照する
- **THEN** `section.headerHeight == 40` を保持し、UI 層はその値を固定 Header 高さとして用いる

#### Scenario: headerHeight = -1 で header テキスト空のときの supplementary 非生成（UI 層への契約）

- **GIVEN** `Section(id: ..., header: nil, footer: nil, cells: [...])`（既定 `headerHeight = -1`、`header` 未指定）
- **WHEN** UI 層が描画する
- **THEN** UI 層は Section Header の supplementary 領域を生成してはならず、Section 間に header 由来の余白が発生しない
