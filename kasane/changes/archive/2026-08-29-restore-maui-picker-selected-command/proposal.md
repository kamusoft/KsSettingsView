# Proposal: restore-maui-picker-selected-command

## Why

MAUI 版 `PickerCell` は選択結果を TwoWay バインドで ViewModel へ返せるが、利用者による選択操作の完了を通知する公開 API がない。値の setter を監視しても初期化・プログラム更新と利用者操作を区別できず、移植元 AiForms の `SelectedCommand` を利用していた ViewModel は同じ責務を移行できない。

この欠落は Sample にも現れている。入力 Cell デモの「通知先メンバー」は 3 platform 共通の footer で「確定時に選択要素の一覧を受け取る経路」と説明されており、iOS / Android は選択完了 callback で実装しているが、MAUI だけは対応する公開 API がないため TwoWay バインドで書かれている。`SelectedCommand` の復元は、この Sample のパリティ崩れを同時に解消する。

## What Changes

- MAUI facade の `PickerCell` に OneWay の `ICommand` 公開プロパティ `SelectedCommand` を追加する
- native の選択確定通知を受けたとき、選択値の書き戻しと相互導出を完了してから Command を実行する
- 実行引数は受け取った確定通知の種類で決め、単一選択の通知で `SelectedItem`、複数選択の通知で `SelectedItems` とする。選択面の表示中に `SelectionMode` が変わっても、利用者が確定した種類に対応する引数を渡す。移植元互換で `CanExecute` は確認しない
- API 形状、単一・複数選択の引数、実行順、直接 setter・cancel・未知 Cell 通知での非発火を MAUI facade テストで固定する
- MAUI サンプルの「通知先メンバー」を、native と同じ「選択位置の TwoWay バインド + 選択完了通知」の経路へ組み替える。表示文言は変更しない
- MAUI サンプルに「MAUI 固有 Cell 機能デモ」画面を新設し、単一選択・複数選択の 2 行で、選択完了通知をバインド可能な command として受け取る形と選択要素列そのものを TwoWay バインドで受け取る形 (いずれも native に対応概念がない facade 固有の公開契約) を示す。各行は選択値と完了通知の受信回数を表示し、同じ選択を確定し直したときの発火を観測できるようにする
- サンプルを iOS Simulator と Android Emulator で実行し、native 選択面から facade までの実経路で完了通知が届くことを確認する

影響する能力: MAUI facade の PickerCell 公開契約、Native 操作通知から facade への書き戻し、MAUI サンプルの画面構成。

## Non-Goals

- Native / Bridge の通知 API は変更しない — 選択確定通知は既に C# facade まで届いており、今回の Command 実行は MAUI 固有の公開面で完結する
- 任意の `CommandParameter` は追加しない — 移植元の契約どおり選択項目そのものを引数とし、別パラメータの設計は今回の完了通知復元に不要
- `NumberPickerCell` など他の入力 Cell に同名 API を追加しない — Cell ごとに公開 API と完了通知の意味を検討する必要がある別の公開 API 変更
- iOS / Android のサンプルは変更しない — 両者は既に選択完了 callback で実装済みで、今回の変更は MAUI 側を追随させるものである
- サンプルの単一選択デモ (担当者) は完了通知経路へ変えない — native のサンプルが選択項目の TwoWay バインドで書いており、MAUI だけ Command にすると逆にパリティが崩れる
- 実機での検証は行わない — 選択面の操作は OS 標準 UI で完結し、Simulator / Emulator と挙動が分かれる要素がない
- `skills/` と README 群は直接更新しない — 利用者向け派生ドキュメントの追従は concepts 更新後に、ユーザーが明示的に依頼する `docs-refresh` の責務

## Impact

既存 API の破壊的変更はない。変更は `maui/` の facade・controller・テストと `samples/maui/` に閉じ、Native の選択面や interop wire 形式には影響しない。サンプルの表示文言は変えないため、プラットフォーム間の文言一致も保たれる。

主なリスクは、値の書き戻し前に Command を実行して ViewModel が旧値を観測すること、および直接 setter や同一通知の折り返しで Command が誤発火すること。発火境界と順序をシナリオテストで固定する。加えて、完了通知は native からの確定通知でしか発火しないため、単体テストだけでは選択面から facade までの経路が繋がっている保証にならない。両 platform の gateway を実際に踏む実行時確認をもって完了とする。

## 級: M

MAUI の単一系統に閉じ UI の新規デザインもないが、公開 API の小変更とサンプルの画面追加を伴うため。

domain: maui
