# Delta Spec: cell-types-input (timepickercell-color-adjust)

## ADDED Requirements

### Requirement: TimePickerCell の時刻選択ダイアログはテーマ配色を反映する (Android)

Android の TimePickerCell が表示する時刻選択ダイアログは、キーボード入力・時計文字盤のどちらのモードでも、表示時点の実効テーマ色を反映して描画される SHALL。色ロールと色の対応は次のとおり:

- 背景ロールは `Theme.backgroundColor` を反映する
- 強調ロール (選択状態の表示・確定/取消の操作・入力キャレット・時計の針とノブ・12時間フォーマット時の午前/午後の選択状態) は解決済みアクセント色を反映する
- 通常文字ロールは実効タイトル文字色 (`CellStyle.titleColor` → `Theme.cellTitleColor` → プラットフォーム既定) を反映する
- アクセント上文字ロール (アクセント色の面に直接載る文字) は、黒と白のうちアクセント色とのコントラスト比が高い方を使う

ダイアログ内のどの部位がどのロールに属するか、および着色対象外の部位は、`ui/brief.md` の部位対応表と承認モックを正とする。

#### Scenario: テーマ色の反映

- **GIVEN** 既定値と異なる `backgroundColor` / `cellAccentColor` / `cellTitleColor` を持つ Theme が適用された設定画面
- **WHEN** TimePickerCell をタップして時刻選択ダイアログを表示する
- **THEN** 部位対応表で色ロールを割り当てられた部位がテーマ由来の色で表示され、それらの部位にプラットフォーム既定配色が残らない

#### Scenario: 12時間フォーマットでの反映

- **GIVEN** `format` に AM/PM を含む TimePickerCell とテーマ配色が適用された設定画面
- **WHEN** ダイアログを表示し、午前/午後の選択を切り替える
- **THEN** 午前/午後の選択状態が解決済みアクセント色で表示され、切替後も各部位の配色が維持される

#### Scenario: 入力モード切替後も配色が維持される

- **GIVEN** テーマ配色が適用された時刻選択ダイアログが表示されている
- **WHEN** キーボード入力と時計文字盤の間で入力モードを切り替える
- **THEN** 切替後に現れた UI も同じテーマ配色で表示される

#### Scenario: 時刻選択の操作後も配色が維持される

- **GIVEN** 時計文字盤モードのダイアログが表示されている
- **WHEN** 時を選択して分の選択へ遷移する
- **THEN** 文字と強調領域の配色がテーマ配色のまま維持される

#### Scenario: アクセント上の文字の可読性

- **GIVEN** 任意のアクセント色が解決されている
- **WHEN** ダイアログでアクセント色の面の上に文字が表示される
- **THEN** 黒と白のうち、アクセント色とのコントラスト比が高い方の文字色が使われる

### Requirement: TimePickerCell のアクセント色は Cell 固有値を先頭に解決される (Android)

Android の時刻選択ダイアログの強調領域に使うアクセント色は、`TimePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` の順で最初に指定された値へ解決される SHALL。

#### Scenario: Cell 固有値の優先

- **GIVEN** `TimePickerCell.accentColor` と `Theme.cellAccentColor` の両方が指定されている
- **WHEN** 時刻選択ダイアログを表示する
- **THEN** 強調領域は `TimePickerCell.accentColor` の色で表示される

#### Scenario: 未指定時のフォールバック

- **GIVEN** `TimePickerCell.accentColor` が未指定 (`null`) である
- **WHEN** 時刻選択ダイアログを表示する
- **THEN** 強調領域は `CellStyle.accentColor` → `Theme.cellAccentColor` の順で解決された色で表示される
