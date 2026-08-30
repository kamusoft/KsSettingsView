# Delta Spec: maui-cells — CustomCell

## ADDED Requirements

### Requirement: CustomCell の配置と Content の表示

`CustomCell` (`CellBase` 派生) を Section 配下に配置でき、`Content` (`View?`、content property、既定 null) に設定した View が行の内容として表示される (SHALL)。行の内容領域は accessory (Disclosure Indicator) 領域を除く全域で、共通行レイアウトのスロット (title / description / icon) を持たない (SHALL)。`Content` が null の間は空の内容の行として表示される (SHALL — XAML 構築順に依存しないため)。`CustomCell` の派生サブクラスも `CustomCell` と同一の経路で描画される (SHALL)。

#### Scenario: XAML 直書きの Content が行に表示される

- **GIVEN** Handler 接続済みの SettingsView 配下の Section
- **WHEN** XAML で `CustomCell` の直下に View を書いて配置する
- **THEN** その View が行の内容として表示される

#### Scenario: 派生サブクラスが同様に描画される

- **GIVEN** コンストラクタで `Content` を組み立てる `CustomCell` 派生クラス
- **WHEN** その派生クラスを XAML から Section に配置する
- **THEN** 派生クラスが組み立てた View が行の内容として表示される

### Requirement: 内容変化の live 反映と Content の差し替え

同一 View インスタンスの内部内容の変化 (バインド値の更新等) は、プロパティの再設定なしに行の表示へ反映される (SHALL)。`Content` に別の View インスタンスを設定すると行の表示が新しい View に置き換わる (SHALL)。一度取り外した View を再び `Content` に設定する往復 (A → B → A) も成立する (SHALL)。

#### Scenario: バインド値の変更が再設定なしで反映される

- **GIVEN** バインディングを持つ View を `Content` に設定した CustomCell が表示されている
- **WHEN** バインド元の値を変更する (プロパティの再設定はしない)
- **THEN** 行の表示内容が更新される

#### Scenario: 別 View への差し替えで表示が置き換わる

- **GIVEN** View A を `Content` に設定した CustomCell が表示されている
- **WHEN** `Content` に別の View B を設定する
- **THEN** 行の表示が B に置き換わる

#### Scenario: 同一 View の往復差し替えが成立する

- **GIVEN** View A → View B と差し替え済みの CustomCell
- **WHEN** `Content` に再び A を設定する
- **THEN** 行の表示が A に戻り、例外も表示の欠落も発生しない

#### Scenario: null への差し替えで空内容になり View は再利用できる

- **GIVEN** View A を `Content` に設定した CustomCell が表示されている
- **WHEN** `Content` を null にし、その後 A を別の CustomCell の `Content` に設定する
- **THEN** 元の行は空内容になり、A は新しい行の内容として表示される

### Requirement: 構造的な除去で Content の所有と表示資源は解放される

CustomCell 自体の削除・置換、所属 Section の削除・Reset、`ItemsSource` からの項目除去、Root の再構築のいずれでも、当該 CustomCell の `Content` は論理所有と表示資源を解放される (SHALL)。除去後の View は別の CustomCell や accessory へ再設定でき、多重配置例外にならない (SHALL)。

#### Scenario: Cell 削除後の View 再利用が成立する

- **GIVEN** View を `Content` に持つ CustomCell が Section に表示されている
- **WHEN** その CustomCell を Section から削除し、同じ View を別の CustomCell の `Content` に設定する
- **THEN** 例外は発生せず、新しい行の内容として表示される

#### Scenario: ItemsSource からの除去で行と資源が解放される

- **GIVEN** `ItemsSource` から生成された CustomCell 群が表示されている
- **WHEN** ソースコレクションから項目を除去する
- **THEN** 対応する行が消え、残る行の表示は維持される

### Requirement: Content は所有 Cell の BindingContext を継承する

`Content` の View は論理ツリーに接続され、所有する CustomCell の `BindingContext` を継承する (SHALL — 既存の facade 意味論 SettingsView → Section → Cell の末端)。所有 Cell の `BindingContext` の変更は View へ伝播する (SHALL)。View 自身に明示的な `BindingContext` が設定されている場合は継承で上書きしない (SHALL)。

#### Scenario: ItemTemplate 生成の CustomCell は item を継承する

- **GIVEN** Section の `ItemsSource` / `ItemTemplate` から生成された CustomCell 群
- **WHEN** 各 CustomCell の Content 内のバインディングが解決される
- **THEN** それぞれ対応する item を `BindingContext` として解決される

#### Scenario: BindingContext の変更が Content へ伝播する

- **GIVEN** CustomCell の Content が表示されている (View に明示的な BindingContext はない)
- **WHEN** SettingsView の `BindingContext` を別の値に変更する
- **THEN** Content 内のバインディングが新しい値で再解決される

### Requirement: 行タップは Command / Tapped で通知される

`Command` (+ `CommandParameter`) / `Tapped` のいずれかが設定され、かつ実効的に有効なとき、行のタップで発火する (SHALL)。実効有効状態 (`IsEnabled` と `Command.CanExecute` の連動、`CanExecuteChanged` への追従) と発火順 (`Tapped` → `Command`) は既存 CommandCell の公開契約と同一とする (SHALL)。`Content` 内の操作可能要素がタップを消費した場合は発火しない — 二重発火は起きない (SHALL)。`Command` / `Tapped` がいずれも未設定のとき、行タップ動作は持たず `Content` 内部の操作を妨げない (SHALL)。表示後の購読の変化 (Command の設定・解除、最初の `Tapped` 購読・最後の購読解除) は行タップ動作の有無へ反映される (SHALL)。

#### Scenario: 行タップで Command が発火する

- **GIVEN** `Command` を設定した CustomCell が表示されている
- **WHEN** content 内の操作可能要素以外の行領域をタップする
- **THEN** `Command` が `CommandParameter` を引数に実行される

#### Scenario: content 内の操作はタップを消費し二重発火しない

- **GIVEN** `Command` を設定し、`Content` にボタンを含む CustomCell が表示されている
- **WHEN** content 内のボタンをタップする
- **THEN** ボタンの操作だけが実行され、行の `Command` は発火しない

#### Scenario: 未設定なら content 内部の操作を妨げない

- **GIVEN** `Command` / `Tapped` を設定していない CustomCell (Content にスライダーを含む)
- **WHEN** スライダーを操作する
- **THEN** スライダーの操作が通常どおり機能する

#### Scenario: 表示後の Command 設定が行タップ動作に反映される

- **GIVEN** `Command` / `Tapped` 未設定で表示中の CustomCell
- **WHEN** `Command` を設定してから行をタップする
- **THEN** `Command` が発火する (表示内容は差し替わらない)

#### Scenario: CanExecute=false の間は発火しない

- **GIVEN** `CanExecute` が false を返す `Command` を設定した CustomCell
- **WHEN** 行をタップする
- **THEN** `Command` は実行されない (既存 CommandCell と同一の実効有効状態)

### Requirement: ShowArrowIndicator で Disclosure Indicator を表示する

`ShowArrowIndicator` (既定 false) を true にすると、行の trailing に CommandCell と同一の Disclosure Indicator が表示され、`Content` の占有領域は indicator 領域を除いた範囲になる (SHALL)。`Command` / `Tapped` の設定と独立に指定できる (SHALL)。

#### Scenario: true で indicator が表示される

- **GIVEN** `ShowArrowIndicator = true` の CustomCell
- **WHEN** その行が表示される
- **THEN** trailing に Disclosure Indicator が表示され、Content は indicator 領域を除いた範囲に表示される

### Requirement: IsEnabled / IsVisible の挙動

`IsEnabled = false` のとき、行タップと `Content` 内部の操作の両方が抑止され、content 全体が無効の視覚状態になる (SHALL — native CustomCell 契約に従う。Android で content が TalkBack の読み上げ対象から外れる非対称も native 契約のまま)。`IsVisible = false` のとき、行として出力されない (SHALL — 既存 Cell と同一の visible projection 契約)。

#### Scenario: 無効時は content 内部の操作も抑止される

- **GIVEN** `IsEnabled = false` の CustomCell (Content にスライダーを含む)
- **WHEN** スライダーを操作しようとする
- **THEN** 操作は効かず、content は無効の視覚状態で表示されている

#### Scenario: IsVisible=false で行が出力されない

- **GIVEN** 表示中の CustomCell
- **WHEN** `IsVisible` を false にする
- **THEN** その行がリストから消える (他の行は維持される)

### Requirement: 継承プロパティのうち不適用のものは silent no-op

`CellBase` から継承する Title / Description / HintText / IconSource / テキスト系 style 項目 (色・フォント) / IconSize / IconRadius は、CustomCell では表示に影響しない (SHALL)。設定しても例外・警告は発生せず、`Content` の表示は変わらない (SHALL — silent no-op)。`BackgroundColor` / `Height` は既存 Cell と同一の意味で効く (SHALL)。

#### Scenario: Title を設定しても表示に現れない

- **GIVEN** `Content` を設定した CustomCell
- **WHEN** `Title` に文字列を設定する
- **THEN** 行の表示は変わらず、例外も発生しない

#### Scenario: 共有 Style の適用が例外にならない

- **GIVEN** `TitleColor` 等を含む `CellBase` 対象の共有 Style
- **WHEN** その Style を CustomCell を含む Cell 群へ適用する
- **THEN** 他の Cell には項目が効き、CustomCell では無視され、例外は発生しない

### Requirement: 行高さは Content の self-sizing に追従する

行高さは `Content` のサイズに追従し、表示中に `Content` の必要サイズが変化した場合も行高さが追従する (SHALL)。`Height` 指定時の解決は既存の高さ解決契約 (hasUnevenRows 依存) に従う (SHALL)。

#### Scenario: 表示中のサイズ変化に行高さが追従する

- **GIVEN** 内容の展開/折りたたみでサイズが変わる View を `Content` に設定した CustomCell が表示されている
- **WHEN** content 内の操作でサイズを変化させる
- **THEN** 行の高さが新しいサイズに追従する

### Requirement: 同一 View インスタンスの多重配置は例外になる

同一 SettingsView 配下で、同じ View インスタンスを複数の `Content` (または accessory View との重複) へ同時に設定することはできず、検出時に `InvalidOperationException` を送出する (SHALL — 既存の accessory View 多重配置検出と同一の契約)。null 解除後の再設定は重複にならない (SHALL)。

#### Scenario: 同一インスタンスを2つの CustomCell へ設定すると例外

- **GIVEN** ある View が CustomCell A の `Content` に設定されている
- **WHEN** 同じ SettingsView 配下の CustomCell B の `Content` にも同じインスタンスを設定する
- **THEN** `InvalidOperationException` が送出される

#### Scenario: null 解除後の再利用は許容される

- **GIVEN** ある View が CustomCell A の `Content` に設定され、その後 null で解除された
- **WHEN** 同じインスタンスを CustomCell B の `Content` に設定する
- **THEN** 例外は発生せず、B の行の内容として表示される

### Requirement: Handler 切断・再接続をまたいで CustomCell は復元される

ページ離脱 (Handler 切断) 後の再訪問 (再接続) で、CustomCell の行は `Content` を含めて再表示される (SHALL)。切断中に行われた `Content` ・プロパティの変更は再接続後の表示に反映される (SHALL)。

#### Scenario: 再訪問で CustomCell が復元される

- **GIVEN** CustomCell を含むページを表示した後、離脱する
- **WHEN** 同じページへ再訪問する
- **THEN** CustomCell の行が `Content` ごと再表示される

#### Scenario: 切断中の Content 差し替えが再接続後に反映される

- **GIVEN** ページ離脱中 (Handler 切断中) の SettingsView
- **WHEN** CustomCell の `Content` に新しい View を設定し、その後ページへ再訪問する
- **THEN** 再接続後の行には新しい View が表示される
