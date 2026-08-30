# maui-cells — デルタスペック (restore-pickercell-object-items)

## MODIFIED Requirements

### Requirement: PickerCell の候補と表示射影

`PickerCell.ItemsSource` は任意の object 列 (`IList`) を受け付ける SHALL (null 要素は非対応 — 設定時に `ArgumentException`)。主表示は `DisplayMember`、副表示は `SubDisplayMember` (いずれも単一プロパティ名、任意) を要素の実行時型から解決して射影する SHALL。解決対象は public instance の引数なし readable プロパティのみで、`DisplayMember` が未指定または解決不能のとき主表示は要素の `ToString()`、`SubDisplayMember` が未指定または解決不能のとき副表示は無しとする SHALL。プロパティ値が string 以外なら `ToString()` で文字列化し、値が null のとき主表示は空文字列・副表示は無しとする SHALL。空文字列の副表示は「無し」として扱う SHALL。getter が送出した例外は伝播する。

facade は `ItemsSource` 設定時に元要素列と射影結果を snapshot として確定し、表示・逆引き・`SelectedItem(s)` の導出は次の差し替えまで同一 snapshot を参照する SHALL (コレクション自身の増減・in-place 変更は観測しない — 現行契約の踏襲)。射影の適用は `ItemsSource` / `DisplayMember` / `SubDisplayMember` の差し替えで反映される SHALL。

#### Scenario: DisplayMember による主表示の射影
- **GIVEN** object 列と `DisplayMember` にプロパティ名を指定した PickerCell
- **WHEN** 選択面と行の値表示を描画する
- **THEN** 各要素の該当プロパティ値が主表示になる

#### Scenario: SubDisplayMember による副表示
- **GIVEN** `SubDisplayMember` にプロパティ名を指定した PickerCell
- **WHEN** 選択面を描画する
- **THEN** 各要素の該当プロパティ値が候補の副表示になる

#### Scenario: 未指定時は ToString()
- **GIVEN** `DisplayMember` / `SubDisplayMember` 未指定の object 列
- **WHEN** 選択面を描画する
- **THEN** 主表示は各要素の `ToString()`、副表示は表示されない

#### Scenario: 解決不能なプロパティ名
- **GIVEN** 要素の型に存在しないプロパティ名を `DisplayMember` に指定
- **WHEN** 選択面を描画する
- **THEN** 主表示は `ToString()` へフォールバックし、例外は発生しない

#### Scenario: string 以外・null のプロパティ値
- **GIVEN** `DisplayMember` の値が string 以外の要素と、値が null の要素
- **WHEN** 選択面を描画する
- **THEN** string 以外は `ToString()` の結果、null は空文字列が主表示になる

#### Scenario: null 要素は設定時に拒否
- **GIVEN** null 要素を含む `IList`
- **WHEN** `ItemsSource` に設定する
- **THEN** `ArgumentException` が送出される

#### Scenario: 元コレクションの in-place 変更を観測しない
- **GIVEN** 表示中の PickerCell の `ItemsSource` 実体を in-place で変更した状態
- **WHEN** 選択面で候補を確定する
- **THEN** `SelectedItem(s)` には設定時 snapshot の要素が入る

### Requirement: PickerCell の選択項目の相互導出

`SelectedItem` (`object?`) は `SelectedIndex` ⇔ `ItemsSource` の相互導出プロパティである SHALL: index からは該当要素 (範囲外・`ItemsSource` 未設定は null で、このとき正は `SelectedIndex`)、設定時は値等価の逆引きで index を導出し、見つからない要素・`ItemsSource` 未設定は未選択へ揃える SHALL。`SelectedItems` (`IList?`) は `SelectedIndices` との相互導出プロパティである SHALL: index 集合からは該当要素を index 昇順に並べた列 (範囲外 index は除外、有効な選択が無ければ空リスト)、設定時は各要素を値等価で逆引きして `SelectedIndices` を導出し、見つからない要素は保持しない SHALL。null 設定は空リストと同義 (選択なし) とする SHALL。逆引きは単一・複数とも「最初に一致した index」に解決する SHALL — 同値要素を複数設定しても index 集合上は1つに揃い、公開値は正 (`SelectedIndices`) からの再導出で確定する (設定した列がそのまま返る保証はしない)。いずれも TwoWay バインド既定で、ユーザー操作による index の書き戻しに追従して更新される SHALL。

#### Scenario: ユーザー確定で SelectedItem が更新される
- **GIVEN** object 列を持つ単一選択 PickerCell
- **WHEN** 選択面で候補を確定する
- **THEN** `SelectedIndex` と `SelectedItem` (該当要素) の両方が更新される

#### Scenario: SelectedItem 設定から index を導出
- **GIVEN** `ItemsSource` に含まれる要素を `SelectedItem` に設定
- **WHEN** プロパティ変更が処理される
- **THEN** `SelectedIndex` がその要素の位置になる

#### Scenario: ユーザー確定で SelectedItems が更新される
- **GIVEN** object 列を持つ複数選択 PickerCell
- **WHEN** 選択面で複数の候補を確定する
- **THEN** `SelectedIndices` と `SelectedItems` (index 昇順の要素列) の両方が更新される

#### Scenario: SelectedItems 設定で見つからない要素は保持されない
- **GIVEN** `ItemsSource` に含まれない要素を混ぜた列を `SelectedItems` に設定
- **WHEN** プロパティ変更が処理される
- **THEN** `SelectedIndices` は見つかった要素の index だけになり、`SelectedItems` も同じ集合へ揃う

#### Scenario: 同値要素の重複設定は1件に揃う
- **GIVEN** 同値の候補が複数 index にある `ItemsSource` で、同値要素を2件含む列を `SelectedItems` に設定
- **WHEN** プロパティ変更が処理される
- **THEN** `SelectedIndices` は最初に一致した index の1件になり、`SelectedItems` はそこから再導出された1件になる

## REMOVED Requirements

### Requirement: PickerCell.DisplayFormatter

**Reason**: 表示射影は `DisplayMember` / `SubDisplayMember` に置き換わった。`DisplayFormatter` は AiForms に無い独自追加で、object 射影の導入で役割が消える (design Decision 6)。
