# Tasks: restore-maui-picker-selected-command

## 1. MAUI facade 公開 API

- [x] 1.1 `PickerCell` に OneWay・既定 null の `SelectedCommand` BindableProperty と公開プロパティを追加する (→ Requirement: PickerCell の選択完了 Command)
- [x] 1.2 選択モードに応じて更新後の `SelectedItem` / `SelectedItems` を引数に `Execute` を直接呼ぶ内部の完了通知口を追加する (→ Requirement: PickerCell の選択完了 Command)
- [x] 1.3 完了通知口が実行引数を選ぶ根拠を、Cell の現在の `SelectionMode` から**呼び出し側が渡す確定通知の種類**へ変える (→ Scenario: 選択面表示中にモードが変わっても確定した種類の引数を渡す)

## 2. Native 選択確定通知との接続

- [x] 2.1 単一・複数選択の有効な native 通知について、選択値の書き戻し後に完了通知口を1回呼ぶ。同一値通知でも Command は実行し、未知 Cell ID では実行しない (→ Requirement: PickerCell の選択完了 Command)
- [x] 2.2 単一選択の通知口は単一、複数選択の通知口は複数として、それぞれ確定通知の種類を完了通知口へ渡す (→ Scenario: 選択面表示中にモードが変わっても確定した種類の引数を渡す)

## 3. テスト

- [x] 3.1 `SelectedCommand` の API 形状、既定値、OneWay binding mode をテストする (→ Requirement: PickerCell の選択完了 Command / Scenario: 公開選択値の直接設定では実行しない)
- [x] 3.2 単一選択で選択値と TwoWay バインド先の更新後に `SelectedItem` を引数として1回実行されることをテストする (→ Scenario: 単一選択の完了後に選択項目を通知)
- [x] 3.3 複数選択で選択値と TwoWay バインド先の更新後に `SelectedItems` を引数として1回実行されることをテストする (→ Scenario: 複数選択の完了後に選択項目列を通知)
- [x] 3.4 同一値の再確定でも実行されること、`CanExecute=false` でも `Execute` が呼ばれることをテストする (→ Scenario: 同じ選択の再確定も完了として通知 / Scenario: CanExecute が false でも完了を通知)
- [x] 3.5 選択プロパティの直接設定と未知 Cell ID の通知では実行されないことを facade テストで固定する。cancel・非確定 dismiss の非発火は、native 選択面が選択 callback を生成しないことを確認済みの既存テスト (`android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt`、`ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift`) を対応根拠とする (→ Scenario: 公開選択値の直接設定では実行しない / Scenario: 選択を確定しなければ実行しない / Scenario: 未知の Cell 通知を無視)
- [x] 3.6 MAUI facade の全テストを実行する (→ Requirement: PickerCell の選択完了 Command)
- [x] 3.7 未知 Cell ID の通知テストを単一・複数で分け、複数側で `SelectedIndices` と `SelectedItems` が初期値のまま変わらないことも assert する (→ Scenario: 未知の Cell 通知を無視)
- [x] 3.8 `SelectionMode` を変更したうえで変更前の種類の確定通知を届け、実行引数が Cell の現在のモードではなく通知の種類に従うことを単一・複数の両方向でテストする (→ Scenario: 選択面表示中にモードが変わっても確定した種類の引数を渡す)

## 4. Sample への反映 (samples/maui)

- [x] 4.1 `samples/maui/KsSettingsView.Sample.Maui/ViewModels/InputCellsDemoViewModel.cs` に「通知先メンバー」の選択位置 (初期 `[0, 2]` — native と同値) と選択完了 Command を追加し、直近イベントの記録を Command 側へ移す。選択要素列を保持していた既存プロパティは 4.3 の新画面へ移し、この ViewModel からは取り除く (→ Requirement: 入力 Cell デモの複数選択を選択完了通知で受ける)
- [x] 4.2 `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml` の「通知先メンバー」を選択位置バインド + `SelectedCommand` へ組み替える。section の header / footer・行の title・候補・初期選択・選択面のタイトルは変更しない (→ Requirement: 入力 Cell デモの複数選択を選択完了通知で受ける の全 Scenario)
- [x] 4.3 「MAUI 固有 Cell 機能デモ」ページと ViewModel を新設する。単一選択の行 (選択項目の TwoWay バインド + `SelectedCommand`) と複数選択の行 (選択要素列の TwoWay バインド + `SelectedCommand`) を 1 行ずつ持ち、各行についてバインド先が保持する選択値と完了通知の受信回数を画面上で読み取れるようにする。文言は既存デモページの書き方に揃え、各 section の footer でその行が何を示すデモかを述べる (→ Requirement: MAUI 固有 Cell 機能デモ画面 / Scenario: 単一選択の確定で選択項目と受信回数がともに更新される / Scenario: 複数選択の確定で選択要素列と受信回数がともに更新される / Scenario: 同じ選択の再確定でも受信回数が増える / Scenario: 確定せずに閉じると受信回数が増えない)
- [x] 4.4 `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs` の `All` に新画面を `MauiSpecific` 区分で登録する (→ Scenario: 画面がルートメニューから開ける)
- [x] 4.5 MAUI facade の全テストを再実行し、実行件数 (`N tests / M failures`) を報告する (→ Requirement: PickerCell の選択完了 Command)

## 5. 実経路の検証 (Simulator / Emulator)

単体テストは controller の通知口を直接呼ぶため、native 選択面 → gateway → controller → Cell の経路を踏んでいない。以下は Sample を実際に動かして経路を通す。証跡のスクリーンショットは `evidence/<os>-<画面>-<操作>-<前|後>.png` の形で残す (OS と観測時点をファイル名に必ず含める。両 OS で同名になると出所が追えなくなる)。

- [x] 5.1 iOS Simulator で Sample を起動し、入力 Cell デモの「通知先メンバー」を確認する。顔ぶれを変えて確定 / 別の行を操作して直近イベントを上書きしてから同じ顔ぶれで再確定 / Cancel で閉じる / 選択面を対話的に閉じる (下方向スワイプ) の 4 操作を、それぞれ操作前後で撮る (→ Requirement: 入力 Cell デモの複数選択を選択完了通知で受ける の全 Scenario)
- [x] 5.2 iOS Simulator で「MAUI 固有 Cell 機能デモ」を開き、単一選択・複数選択の各行について 選択を変えて確定 / 同じ選択で再確定 / Cancel で閉じる / 対話的に閉じる の 4 操作で、選択値と受信回数の変化を確認する (→ Requirement: MAUI 固有 Cell 機能デモ画面 の全 Scenario)
- [x] 5.3 Android Emulator で 5.1 と同じ手順を実施する。非確定の閉じ方は Cancel・外側タップ・Back の 3 通りを踏む (→ 同上)
- [x] 5.4 Android Emulator で 5.2 と同じ手順を実施する (→ 同上)
- [x] 5.5 取得したスクリーンショットに個体・個人を特定する情報が写っていないことを確認してから `evidence/` へ保存する
