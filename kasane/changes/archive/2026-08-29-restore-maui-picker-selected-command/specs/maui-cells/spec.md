## ADDED Requirements

### Requirement: PickerCell の選択完了 Command

MAUI facade の `PickerCell` は OneWay の `ICommand` プロパティ `SelectedCommand` を公開しなければならない (SHALL)。既定値は null とする。

Native から有効な Cell ID の選択確定通知を受けたとき、facade は選択 index とそこから相互導出される選択項目を公開プロパティおよび TwoWay バインド先へ反映した後、`SelectedCommand` を1回実行しなければならない (SHALL)。`CanExecute` の結果にかかわらず移植元互換で `Execute` を直接呼ばなければならない (SHALL)。確定通知の値が現在値と同じ場合も、選択操作の完了通知として Command を実行しなければならない (SHALL)。

実行引数は**受け取った確定通知の種類**で決まる。単一選択の確定通知では `SelectedItem`、複数選択の確定通知では `SelectedItems` を渡さなければならない (SHALL)。選択面は表示を開始した時点の選択モードで動作するため、引数の選び方を Cell の現在の `SelectionMode` に依存させてはならない (SHALL NOT) — 表示中に `SelectionMode` が変更されると、利用者が確定した種類と異なる引数を渡すことになる。

公開選択プロパティの直接設定、確定通知を伴わない cancel・dismiss、未知の Cell ID の通知では Command を実行してはならない (SHALL NOT)。

#### Scenario: 単一選択の完了後に選択項目を通知

- **GIVEN** 単一選択の `PickerCell` に `SelectedCommand` と TwoWay バインドを設定している
- **WHEN** 利用者が候補を確定する
- **THEN** `SelectedIndex` と `SelectedItem` およびバインド先が新しい選択へ更新された後、`SelectedItem` を引数として Command が1回実行される

#### Scenario: 複数選択の完了後に選択項目列を通知

- **GIVEN** 複数選択の `PickerCell` に `SelectedCommand` と TwoWay バインドを設定している
- **WHEN** 利用者が複数の候補を確定する
- **THEN** `SelectedIndices` と `SelectedItems` およびバインド先が新しい選択へ更新された後、`SelectedItems` を引数として Command が1回実行される

#### Scenario: 選択面表示中にモードが変わっても確定した種類の引数を渡す

- **GIVEN** 単一選択の `PickerCell` の選択面を表示している
- **WHEN** 表示中に Cell の `SelectionMode` が複数選択へ変更され、その後利用者が表示中の単一選択の面で候補を確定する
- **THEN** Command には単一選択の実行引数 (`SelectedItem`) が渡される

#### Scenario: 同じ選択の再確定も完了として通知

- **GIVEN** 現在の選択と同じ値を持つ `PickerCell`
- **WHEN** 利用者がその選択を再び確定する
- **THEN** 選択プロパティの不要な再書き戻しは発生せず、`SelectedCommand` は選択完了として1回実行される

#### Scenario: CanExecute が false でも完了を通知

- **GIVEN** 実行引数に対して `CanExecute` が false を返す `SelectedCommand` を設定している
- **WHEN** 利用者が選択を確定する
- **THEN** facade は `CanExecute` を確認せず、更新後の選択項目を引数として `Execute` を1回呼ぶ

#### Scenario: 公開選択値の直接設定では実行しない

- **GIVEN** `SelectedCommand` を設定した `PickerCell`
- **WHEN** ViewModel または利用コードが `SelectedIndex`・`SelectedIndices`・`SelectedItem`・`SelectedItems` のいずれかを直接設定する
- **THEN** `SelectedCommand` は実行されない

#### Scenario: 選択を確定しなければ実行しない

- **GIVEN** `SelectedCommand` を設定した `PickerCell` の選択面を表示している
- **WHEN** 利用者が選択を確定せずに選択面を閉じる
- **THEN** `SelectedCommand` は実行されない

#### Scenario: 未知の Cell 通知を無視

- **GIVEN** `SelectedCommand` を設定した `PickerCell`
- **WHEN** facade がその Cell と一致しない ID の選択通知を受ける
- **THEN** 選択値は変わらず `SelectedCommand` も実行されない
