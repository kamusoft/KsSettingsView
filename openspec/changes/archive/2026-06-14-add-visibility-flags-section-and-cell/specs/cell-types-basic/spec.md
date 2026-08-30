## ADDED Requirements

### Requirement: 全 Cell 共通の isVisible

本変更提案で扱う 7 種の Cell（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）は、すべて `isVisible: Bool`（既定 `true`）フィールドを持たなければならない (SHALL)。

`isVisible: Bool` は AiForms.Maui.SettingsView の `CellBase.IsVisible` 相当のプロパティでなければならない (MUST)。意味は以下：

- `true`（既定値） → 通常の表示。UI 層は当該 Cell を visible projection に含め、描画する。
- `false` → UI 層は当該 Cell を visible projection から除外しなければならない (MUST)。model 上（`Section.cells` 配列内）にはデータとして保持されなければならず (MUST)、`true` に戻したとき元の位置に復活しなければならない (MUST)。

各 Cell の `Hashable` / `Equatable`（iOS）/ `equals` / `hashCode`（Android）実装は、`isVisible` を判定対象に含めなければならない (MUST)。各 Cell の `withDSLID(_:)` / `withStyle(_:)` 実装（iOS）および `data class copy()` 経路（Android）は、`isVisible` を保持しなければならない (MUST)。

#### `isEnabled` との関係

`isVisible` と `isEnabled` は **独立フラグ** として扱わなければならない (MUST)：

- `isVisible = false` のとき、`isEnabled` の値はモデル値として保持されなければならない (MUST) が、描画されないため `isEnabled` の視覚効果（テキスト色置換、UI コントロール無効化等）は発生しない。
- 再び `isVisible = true` に切り替わったとき、保持されていた `isEnabled` の値がそのまま視覚効果として反映されなければならない (MUST)。
- `isVisible = false` の Cell に対する `isEnabled` の変更は、`isVisible = true` に戻ったときに初めて視覚効果として現れる。

#### `VisibilityAware` 抽象への opt-in 準拠（UI 層）

UI 層（iOS `KsSettingsViewUI` / Android `ks-settingsview-ui`）は `VisibilityAware` プロトコル / interface（`var isVisible: Bool { get }` を要求）を提供しなければならない (SHALL)。本変更提案で扱う 7 種の Cell は、すべて `VisibilityAware` に opt-in 準拠しなければならない (MUST)。

`VisibilityAware` 非準拠の Cell（外部 Sample Cell や `CustomCell` 等）は、UI 層のフィルタにおいて常に visible（`true`）として扱われなければならない (MUST)。

Core 抽象 `Cell`（Android）/ `KsCell` プロトコル（iOS）には `isVisible` を要求として追加してはならない (MUST NOT)。

#### Scenario: LabelCell が isVisible を持てる

- **GIVEN** `LabelCell(title: "通知", isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されないが、`SettingsRoot` 上の `Section.cells` 配列内には保持される

#### Scenario: CommandCell が isVisible を持てる

- **GIVEN** `CommandCell(title: "詳細", isVisible: false, onTap: {...})`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されず、タップイベントも発火しない（描画されない結果として）

#### Scenario: SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell が isVisible を持てる

- **GIVEN** チェック系 Cell（例: `SwitchCell(title: "プッシュ", isOn: true, isVisible: false)`）
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されない。`isOn` 等のモデル値は保持される

#### Scenario: ButtonCell が isVisible を持てる

- **GIVEN** `ButtonCell(title: "送信", isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されない

#### Scenario: isVisible 既定値（既存呼び出し互換）

- **GIVEN** 既存のコード `LabelCell(title: "通知")`（`isVisible` を指定しない呼び出し）
- **WHEN** インスタンスを構築する
- **THEN** `isVisible` は既定値 `true` が適用され、ビルドエラーや実行時エラーは発生しない

#### Scenario: isVisible 変更時の構造同期検出

- **GIVEN** 同一 id の Cell について `isVisible = true → false` に変更
- **WHEN** UI 層が再描画する
- **THEN** 構造同期上は当該 Cell が削除として検出される（reconfigure 経路ではなく、構造同期の削除アニメーションとして反映）

#### Scenario: isVisible = false の Cell は isEnabled の視覚効果を発生させない

- **GIVEN** `LabelCell(title: "通知", isEnabled: false, isVisible: false)`
- **WHEN** SettingsView がレンダリングされる
- **THEN** 当該 Cell は描画されず、`isEnabled = false` のテキスト色置換も発生しない（描画自体が無いため）

#### Scenario: isVisible toggle で isEnabled 状態が保持される

- **GIVEN** `LabelCell(title: "通知", isEnabled: false, isVisible: false)`
- **WHEN** `isVisible` を `true` に変更して再描画
- **THEN** 当該 Cell は描画され、保持されていた `isEnabled = false` のテキスト色置換が反映される

#### Scenario: VisibilityAware 非準拠 Cell は常に表示

- **GIVEN** `VisibilityAware` プロトコル / interface に準拠していない外部 Cell（例: Sample アプリの独自 Cell）が `Section.cells` に含まれる
- **WHEN** UI 層がフィルタする
- **THEN** 当該 Cell は visibility に関するフィルタの判定で常に `true` として扱われ、描画される

#### Scenario: Core 抽象は isVisible を要求しない

- **GIVEN** Core 抽象 `Cell`（Android）/ `KsCell` プロトコル（iOS）の定義
- **WHEN** インターフェース / プロトコルのメンバを確認する
- **THEN** `isVisible` プロパティは要求されない。`isVisible` は UI 層配置の 7 Cell が個別に保持し、UI 層の `VisibilityAware` プロトコル / interface 経由でフィルタ層に opt-in する
