## MODIFIED Requirements

### Requirement: PickerCell の選択完了 Command
MAUI facade の `PickerCell` は OneWay の `ICommand` プロパティ `SelectedCommand` を公開しなければならない (SHALL)。既定値は null とする。

Native から有効な Cell ID の確定通知 (`PickerCellSelectionChanged` / `PickerCellMultiSelectionChanged`) を受けたとき、facade は選択 index とそこから相互導出される選択項目を公開プロパティおよび TwoWay バインド先へ反映しなければならない (SHALL)。この時点では `SelectedCommand` を実行してはならない (SHALL NOT)。

Native から同じ Cell ID の閉じ切り通知 (`PickerCellSelectionCompleted` / `PickerCellMultiSelectionCompleted`) を受けたとき、facade は `SelectedCommand` を 1 回実行しなければならない (SHALL)。`CanExecute` の結果にかかわらず移植元互換で `Execute` を直接呼ばなければならない (SHALL)。確定通知の値が現在値と同じ場合も、選択操作の完了通知として閉じ切り通知で Command を実行しなければならない (SHALL)。

実行引数は**受け取った閉じ切り通知の種類**で決まる。単一選択の閉じ切り通知では `SelectedItem`、複数選択の閉じ切り通知では `SelectedItems` を渡さなければならない (SHALL)。引数は閉じ切り通知を受けた時点の Cell の現値であり、確定通知の値や閉じ切り通知が運ぶ index から別途解決した snapshot ではない (SHALL)。確定から閉じ切りまでの間にアプリが `ItemsSource` や選択を変更した場合は、変更後の現値が渡る。選択面は表示を開始した時点の選択モードで動作するため、引数の選び方を Cell の現在の `SelectionMode` に依存させてはならない (SHALL NOT)。

公開選択プロパティの直接設定、確定通知を伴わない cancel・dismiss、未知の Cell ID または PickerCell 以外の Cell ID の通知では Command を実行してはならない (SHALL NOT)。`SelectedCommand` が null なら閉じ切り通知で何もしない (SHALL)。

#### Scenario: 確定通知の時点ではコマンドが実行されない
- **GIVEN** `SelectedCommand` を設定した単一選択の PickerCell
- **WHEN** Bridge から `PickerCellSelectionChanged(cellId, index)` だけが届く
- **THEN** `SelectedIndex` は index に更新されるが、`SelectedCommand` はまだ実行されない

#### Scenario: 閉じ切り通知でコマンドが SelectedItem を引数に 1 回実行される
- **GIVEN** `SelectedCommand` を設定した単一選択の PickerCell に `PickerCellSelectionChanged(cellId, index)` が届いている
- **WHEN** Bridge から `PickerCellSelectionCompleted(cellId, index)` が届く
- **THEN** `SelectedCommand` が `SelectedItem` (書き戻し済みの現値) を引数に 1 回実行される

#### Scenario: 複数選択の閉じ切り通知でコマンドが SelectedItems を引数に 1 回実行される
- **GIVEN** `SelectedCommand` を設定した複数選択の PickerCell に `PickerCellMultiSelectionChanged(cellId, indices)` が届いている
- **WHEN** Bridge から `PickerCellMultiSelectionCompleted(cellId, indices)` が届く
- **THEN** `SelectedCommand` が `SelectedItems` を引数に 1 回実行される

#### Scenario: 選択面の表示中に SelectionMode が変わっても届いた通知の種類で引数を選ぶ
- **GIVEN** 単一選択で選択面を開き `PickerCellSelectionChanged` が届いた後、`SelectionMode` を複数選択に変えた PickerCell
- **WHEN** `PickerCellSelectionCompleted(cellId, index)` が届く
- **THEN** `SelectedCommand` の引数は `SelectedItem` (単一の現値) である

#### Scenario: SelectedCommand 未設定なら閉じ切り通知で何も起きない
- **GIVEN** `SelectedCommand` を設定していない PickerCell
- **WHEN** `PickerCellSelectionCompleted(cellId, index)` が届く
- **THEN** 例外も副作用も無い

#### Scenario: PickerCell 以外の cellId の閉じ切り通知は無視される
- **GIVEN** PickerCell でない Cell の cellId
- **WHEN** その cellId で `PickerCellSelectionCompleted` が届く
- **THEN** どの Cell の `SelectedCommand` も実行されない

#### Scenario: 確定から閉じ切りまでの間に選択が変わっても閉じ切り時点の現値を渡す
- **GIVEN** 単一選択の PickerCell に `PickerCellSelectionChanged(cellId, 1)` が届き、その後コードで `SelectedIndex` を 2 に設定した
- **WHEN** `PickerCellSelectionCompleted(cellId, 1)` が届く
- **THEN** `SelectedCommand` の引数は index 2 に対応する `SelectedItem` (現値) である

#### Scenario: 同じ選択の再確定も閉じ切り通知で完了として通知
- **GIVEN** 現在の選択と同じ値を持つ PickerCell に `SelectedCommand` を設定している
- **WHEN** 同じ index の `PickerCellSelectionChanged` に続いて `PickerCellSelectionCompleted` が届く
- **THEN** 選択プロパティの不要な再書き戻しは発生せず、`SelectedCommand` は閉じ切り通知で 1 回実行される

#### Scenario: CanExecute が false でも閉じ切り通知で実行する
- **GIVEN** 実行引数に対して `CanExecute` が false を返す `SelectedCommand` を設定している
- **WHEN** `PickerCellSelectionCompleted` が届く
- **THEN** `Execute` が 1 回呼ばれる

#### Scenario: 公開プロパティの直接設定では実行しない
- **GIVEN** `SelectedCommand` を設定した PickerCell
- **WHEN** コードから `SelectedIndex` を設定する
- **THEN** `SelectedCommand` は実行されない

#### Scenario: Handler 切断後は閉じ切り通知でコマンドが実行されない
- **GIVEN** `SelectedCommand` を設定した PickerCell を持つ SettingsView の Handler が切断され、Bridge が破棄されている
- **WHEN** 破棄前の選択面が閉じ切る
- **THEN** `SelectedCommand` は実行されない
