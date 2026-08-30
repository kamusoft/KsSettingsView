## RENAMED Requirements

- FROM: `### Requirement: 表示状態同期の二層分離`
  TO: `### Requirement: 表示状態同期の三層分離`

## MODIFIED Requirements

### Requirement: 表示状態同期の三層分離

SettingsView の表示状態同期は、**(1) 構造同期**、**(2) 内容更新**、**(3) 可視性変化** の三層に分離されなければならない (SHALL)。各 UI 層実装（Android / iOS）はこの原則に従わなければならない (MUST)。

- **(1) 構造同期**: 差分検出（Android `DiffUtil` / iOS `UICollectionViewDiffableDataSource` snapshot）は、Cell / Section の **追加・削除・移動・差し替え（id の変化）** を検出する目的に限定されなければならない (MUST)。構造同期の同一性判定は **id（識別子）の同一性のみ** を用いなければならず (MUST)、Cell の内容プロパティ（`title` / `isOn` / `isChecked` / `selectedValue` 等）を判定に用いてはならない (MUST NOT)。
- **(2) 内容更新**: 同一 id を持つ Cell の内容（表示プロパティ）の変化は、セルを破棄・再生成せずに **同一セル（Android: ViewHolder、iOS: Cell）の部分更新（reconfigure）** で反映されなければならない (MUST)。内容変化を「セルの差し替え（フルリバインド／reload）」として扱ってはならない (MUST NOT)。
- **(3) 可視性変化**: `Section.isVisible` / `Cell.isVisible` の変化は、上記 (1)(2) のいずれにも該当しない第三カテゴリとして扱わなければならない (MUST)。UI 層は **model（hidden 含むフル状態）と visible projection（`isVisible = true` のみで構成される表示用ビュー）を分離管理** しなければならない (MUST)。可視性変化は構造同期上の追加・削除アニメーションを伴って反映されなければならない (MUST)。可視性変化を (2) 内容更新（reconfigure 経路）で表現してはならない (MUST NOT)。

この分離により、内容変化（チェック ON/OFF・スイッチ・値更新等）が行全体の再生成（ちらつき）を引き起こさず、かつ可視性変化が構造同期の追加・削除として正しくアニメートされることを保証する。移植元 `AiForms.Maui.SettingsView` の Android 実装（`GetItemId(position) => position`、`CellPropertyChanged → NotifyItemChanged`）と同一の責務分担に、`Section.IsVisible` / `CellBase.IsVisible` のモデル保持＋表示射影の概念を加えたものである。

#### Scenario: 構造変化は id 同一性で検出

- **GIVEN** SettingsRoot に対し Cell の追加・削除・移動・id 変化を伴う差し替えが発生する
- **WHEN** 構造同期（diff / snapshot）が評価される
- **THEN** id の同一性のみで追加・削除・移動・差し替えが検出され、該当する構造操作（insert / delete / move）が行われる

#### Scenario: 内容変化はセルを再生成しない

- **GIVEN** 同一 id の Cell の内容プロパティ（例: `isChecked` や `title`）が変化する
- **WHEN** 表示状態が更新される
- **THEN** 構造同期は「変化なし（同一 id）」と判定し、内容更新は同一セルの部分更新（reconfigure）として反映される。セル（ViewHolder / Cell）の破棄・再生成は発生しない

#### Scenario: チェック系の TwoWay 反映

- **GIVEN** チェック系 Cell（Switch / Checkbox / Radio / SimpleCheck）をユーザーが操作する
- **WHEN** セルがタップ等で操作される
- **THEN** セル（ViewHolder / Cell）が自身の表示状態を直接更新し、`onValueChanged` / `onSelected` 等でモデルへ通知する（TwoWay）。この内容更新は構造同期（diff / snapshot の再構築）を経由しない

#### Scenario: Cell の isVisible 変化は構造同期上の追加削除になる

- **GIVEN** 同一 id の Cell について `isVisible` が `true → false` に変化する
- **WHEN** 表示状態が更新される
- **THEN** UI 層の visible projection から当該 Cell が除外され、構造同期上は削除として検出される。逆に `false → true` の変化は visible projection に当該 Cell が追加され、構造同期上は挿入として検出される。これは reconfigure 経路を経由しない

#### Scenario: Section の isVisible 変化は section 全体の追加削除になる

- **GIVEN** 同一 id の Section について `isVisible` が `true → false` に変化する
- **WHEN** 表示状態が更新される
- **THEN** UI 層の visible projection から当該 section（header / footer / cells 含む）が除外され、構造同期上は section 削除として検出される

#### Scenario: model と visible projection の分離管理

- **GIVEN** `SettingsRoot.sections` に hidden な Section / Cell を含む model
- **WHEN** UI 層が描画する
- **THEN** UI 層は model（hidden 含むフル状態）を保持しつつ、描画には visible projection（hidden を除外した派生ビュー）を用いる。model と visible projection はそれぞれ独立した責務として管理される

### Requirement: Section ドメインモデル

`Section` は単一セクションを表す値型でなければならない (SHALL)。一意な `id`、任意の `header`（`SectionAccessory?`）、任意の `footer`（`SectionAccessory?`）、`Cell` のリスト、`headerHeight: Double`（既定 `-1`）、および `isVisible: Bool`（既定 `true`）を保持しなければならない (MUST)。`header` / `footer` は文字列のみならず、任意の View を `KsAnyView` 経由で格納できなければならない (MUST)。Cell（タップ・選択・編集する行）は `cells` フィールドにのみ格納し、`header` / `footer` には格納しない (MUST NOT)。

`headerHeight: Double` は AiForms.Maui.SettingsView の `Section.HeaderHeight` 相当のプロパティでなければならない (MUST)。意味は以下：

- `-1`（既定値） → 「自動高さ」を意味し、`header` テキストが空または未設定の場合は UI 層は Section Header の supplementary 自体を生成してはならない (MUST NOT)。`header` テキストが存在する場合は UI 層がテキスト寸法に基づいて自動算出する。
- 正値（> 0） → その値を固定高さとして用いる。

`isVisible: Bool` は AiForms.Maui.SettingsView の `Section.IsVisible` 相当のプロパティでなければならない (MUST)。意味は以下：

- `true`（既定値） → 通常の表示。UI 層は当該 Section（header / footer / cells 含む）を visible projection に含め、描画する。
- `false` → UI 層は当該 Section（header / footer / cells 含む）を visible projection から除外しなければならない (MUST)。model 上にはデータとして保持されなければならず (MUST)、`true` に戻したとき元の位置に復活しなければならない (MUST)。

`isVisible` は値型としての等価性に含まれなければならない (MUST)（`Hashable` / `equals` の判定対象）。

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

#### Scenario: isVisible 既定値

- **GIVEN** `Section(id: ..., header: ..., footer: ..., cells: [...])`（`isVisible` を指定しない）
- **WHEN** インスタンスを生成する
- **THEN** `section.isVisible == true` が適用され、既存呼び出しがビルドエラー・実行時エラーを起こさない

#### Scenario: isVisible = false の Section は visible projection から除外される

- **GIVEN** `Section(id: ..., header: ..., footer: ..., cells: [...], isVisible: false)`
- **WHEN** UI 層が描画する
- **THEN** UI 層は当該 Section の header / footer / 全 cells を visible projection から除外し、画面には描画しない。一方で `SettingsRoot.sections` には当該 Section が保持されたままである

#### Scenario: isVisible を true に戻すと元の位置に復活する

- **GIVEN** `isVisible: false` で描画から除外されていた Section について、`isVisible: true` に切り替える
- **WHEN** 表示状態が更新される
- **THEN** 当該 Section（同一 id・同一の cells リスト）が `SettingsRoot.sections` 内の元の位置に対応する描画位置に復活する

#### Scenario: 値型としての等価性に isVisible が含まれる

- **GIVEN** 同一 id・同一の他フィールドを持つ 2 つの `Section` インスタンスで、`isVisible` のみが異なる
- **WHEN** 等価性を比較する
- **THEN** 等価と判定されない。`Hashable` / `equals` の判定対象に `isVisible` が含まれる

### Requirement: Hashable / equals 契約

すべての構造ドメイン値型（`SettingsRoot`、`Section`、`SectionAccessory`、`RootAccessory`、各具象 `Cell`、`SettingsRootDiff`、`AccessoryTarget`、`SettingsAccessory`）は、Swift `Hashable` または Kotlin `data class`（`equals`/`hashCode` 自動実装）の契約を満たさなければならない (MUST)。これは一般的な値比較・テスト・コレクション操作のための値型としての性質である。**Core から削除された `Theme` / `CellStyle` は本契約の対象外となる (MUST NOT)。**

ただし、差分検出（diff / snapshot の構造同期）は、この内容等価性（`equals` / `Hashable` の全フィールド比較）を **構造同期の同一性判定に用いてはならない** (MUST NOT)。構造同期は id（識別子）の同一性のみを用いなければならない (MUST)（「表示状態同期の三層分離」Requirement を参照）。値型の `equals` / `Hashable` は値比較やテストでは引き続き全フィールドを比較してよい。

また、装飾領域専用の型消去ラッパ `KsAnyView` は `Hashable` / `Equatable` / `equals` / `hashCode` を持たない (MUST NOT)。`SectionAccessory.view(KsAnyView)` および `RootAccessory.view(KsAnyView)` は手動実装で、`KsAnyView` の中身を判定対象外とし「ケース一致のみで等価」とみなさなければならない (MUST)。これに連動して `Section` の hash/equals も、`view` ケースの中身を判定対象から除外しなければならない (MUST)。

#### Scenario: 同一フィールドのインスタンスは等しい

- **GIVEN** 同じフィールド値を持つ 2 つの `Section` または `Cell` インスタンス（`SectionAccessory.view` の `KsAnyView` 中身は問わない）
- **WHEN** ハッシュ値および等価性を比較する
- **THEN** ハッシュ値が一致し、等価と判定される

#### Scenario: フィールド変更後は等しくない（値型としての性質）

- **GIVEN** `text` ケース内容や Cell の通常フィールドが 1 つだけ異なる 2 つのインスタンス
- **WHEN** 値型として等価性を比較する
- **THEN** 等価と判定されない（値型の性質として全フィールドを比較する）

#### Scenario: 差分検出は内容等価性を構造同期に使わない

- **GIVEN** 同一 id だが内容プロパティ（例: `isChecked` や `title`）が異なる 2 つの Cell
- **WHEN** 構造同期（diff / snapshot）の同一性判定が行われる
- **THEN** id が同一であるため「同一アイテム・構造変化なし」と判定される。値型としての `equals` が `false` を返すことを構造同期の判定（areContentsTheSame / snapshot 再構築）に用いてはならない

#### Scenario: KsAnyView の中身違いは等価とみなす

- **GIVEN** `SectionAccessory.view(KsAnyView A)` と `SectionAccessory.view(KsAnyView B)`(A ≠ B、ただしケースは同じ)
- **WHEN** 等価性を比較する
- **THEN** ケース一致のみで等価と判定される（中身は比較されない）

#### Scenario: KsAnyView は Hashable に参加しない

- **GIVEN** `KsAnyView` インスタンス
- **WHEN** Swift では `Hashable` 準拠を確認、Kotlin では `equals` / `hashCode` の独自実装を確認
- **THEN** `KsAnyView` は `Hashable` / `Equatable` 準拠を持たず、Kotlin では `Any` のデフォルト（参照同一性）以外の equals / hashCode を実装していない

#### Scenario: SettingsRootDiff の等価性

- **GIVEN** 同じケース・同じ payload を持つ 2 つの `SettingsRootDiff` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: AccessoryTarget の等価性

- **GIVEN** 同じケース・同じ `sectionID` を持つ 2 つの `AccessoryTarget` インスタンス
- **WHEN** 等価性を比較する
- **THEN** 等価と判定される

#### Scenario: Theme / CellStyle は Core の Hashable 契約対象外

- **GIVEN** `KsSettingsViewCore` / `ks-settingsview-core` の公開型一覧
- **WHEN** 型を確認する
- **THEN** `Theme` / `CellStyle` 型は Core 内に存在しないため、Core 仕様の Hashable 契約の対象外である。`Theme` / `CellStyle` の等価性契約は `settings-view-{ios,android}-style` 仕様で規定される
