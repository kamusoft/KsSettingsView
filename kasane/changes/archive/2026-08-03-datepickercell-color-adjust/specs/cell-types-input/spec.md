# Delta Spec: cell-types-input (datepickercell-color-adjust)

## ADDED Requirements

### Requirement: DatePickerCell の日付選択ダイアログはテーマ配色を反映する (Android)

Android の DatePickerCell (カレンダーモード、`uiStyle = Material`) が表示する日付選択ダイアログは、カレンダー表示・テキスト入力のどちらのモードでも、表示時点の実効テーマ色を反映して描画される SHALL。色ロールと色の対応は次のとおり:

- 背景ロールは `Theme.backgroundColor` を反映する
- 強調ロール (選択日の表示・今日の日付の表示・確定/取消の操作・テキスト入力モードの入力欄の枠とキャレット・年選択の選択状態) は解決済みアクセント色を反映する
- 通常文字ロール (ヘッダ・曜日・日付数字・年月表示・入力文字等) は実効タイトル文字色 (`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定) を反映する
- アクセント上文字ロール (アクセント色の面に直接載る文字) は、黒と白のうち、アクセント色をダイアログ背景へ合成した実効面とのコントラスト比が高い方を使う (半透明アクセントでも実描画と判定が食い違わないため)

ダイアログ内のどの部位・状態がどのロールに属するか、および着色対象外の部位は、`ui/brief.md` の部位対応表と承認モックを正とする。本 Requirement の適用範囲はダイアログの表示セッション内 (表示してから閉じるまで) とし、Activity/構成の再生成をまたぐ復元後の状態は対象外とする (既知の構造問題として別変更で扱う)。

#### Scenario: テーマ色の反映

- **GIVEN** 既定値と異なる `backgroundColor` / `cellAccentColor` / `cellTitleColor` を持つ Theme が適用された設定画面
- **WHEN** DatePickerCell (カレンダーモード) をタップして日付選択ダイアログを表示する
- **THEN** 部位対応表で色ロールを割り当てられた部位がテーマ由来の色で表示され、それらの部位にプラットフォーム既定配色が残らない

#### Scenario: 入力モード切替後も配色が維持される

- **GIVEN** テーマ配色が適用された日付選択ダイアログが表示されている
- **WHEN** カレンダー表示とテキスト入力の間で入力モードを切り替える
- **THEN** 切替後に現れた UI も同じテーマ配色で表示される

#### Scenario: カレンダー操作後も配色が維持される

- **GIVEN** カレンダー表示モードのダイアログが表示されている
- **WHEN** 月を移動し、年月表示から年選択を開いて年を選択する
- **THEN** 移動先の月・年選択の表示もテーマ配色で描画され、選択状態の配色が維持される

#### Scenario: 日付を選び直しても配色が維持される

- **GIVEN** カレンダー表示モードのダイアログで日付が選択されている
- **WHEN** 別の日付を選択する
- **THEN** 元の選択日は非選択状態のロールへ戻り、新しい選択日・今日・無効日の各部位が部位対応表どおりのテーマ配色で表示される

#### Scenario: アクセント上の文字の可読性

- **GIVEN** 任意の (半透明を含む) アクセント色が解決されている
- **WHEN** ダイアログでアクセント色の面の上に文字が表示される
- **THEN** 黒と白のうち、アクセント色をダイアログ背景へ合成した実効面とのコントラスト比が高い方の文字色が使われる

### Requirement: DatePickerCell のアクセント色は Cell 固有値を先頭に解決される (Android)

Android の日付選択ダイアログの強調領域に使うアクセント色は、`DatePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の順で最初に指定された値へ解決される SHALL。

#### Scenario: Cell 固有値の優先

- **GIVEN** `DatePickerCell.accentColor` と `Theme.cellAccentColor` の両方が指定されている
- **WHEN** 日付選択ダイアログを表示する
- **THEN** 強調領域は `DatePickerCell.accentColor` の色で表示される

#### Scenario: CellStyle 値へのフォールバック

- **GIVEN** `DatePickerCell.accentColor` が未指定 (`null`) で、`CellStyle.accentColor` と `Theme.cellAccentColor` の両方が指定されている
- **WHEN** 日付選択ダイアログを表示する
- **THEN** 強調領域は `CellStyle.accentColor` の色で表示される

#### Scenario: Theme 値へのフォールバック

- **GIVEN** `DatePickerCell.accentColor` と `CellStyle.accentColor` がともに未指定 (`null`) である
- **WHEN** 日付選択ダイアログを表示する
- **THEN** 強調領域は `Theme.cellAccentColor` の色で表示される

### Requirement: 日付選択ダイアログのヘッダはタイトルと選択日の両方が読める (Android)

Android の日付選択ダイアログのヘッダは、タイトル (`pickerTitle` の指定値または既定タイトル) と選択日テキストが互いに重ならず、それぞれが自身の表示領域内でクリップされずに読める SHALL。保証する構成はダイアログ表示 (縦・横) かつ端末既定のフォント倍率とし、表示領域の幅を超えるタイトルは省略表示を許容する (省略表示となった場合も選択日テキストとは重ならない)。

#### Scenario: 日本語タイトルと日本語日付の同時表示

- **GIVEN** 日本語の `pickerTitle` を指定した DatePickerCell (カレンダーモード) と日本語ロケールの端末
- **WHEN** 日付選択ダイアログを表示する
- **THEN** タイトルと選択日テキストが重ならず、両方の全体が読める

#### Scenario: 日付選択後もヘッダが崩れない

- **GIVEN** 日付選択ダイアログが表示されている
- **WHEN** 別の日付を選択してヘッダの選択日テキストが更新される
- **THEN** 更新後もタイトルと選択日テキストは重ならない
