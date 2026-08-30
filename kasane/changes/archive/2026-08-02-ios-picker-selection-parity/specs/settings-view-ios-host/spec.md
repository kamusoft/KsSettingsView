# Delta: settings-view-ios-host (ios-picker-selection-parity)

## ADDED Requirements

### Requirement: PickerCell 選択面のスタイル継承

iOS の PickerCell 選択面は、呼び出し元の Cell / Theme から解決したスタイル実効値を継承して描画する SHALL:

- 候補行のタイトル文字色・フォントは実効タイトル値 (CellStyle → Theme の解決。`Theme.cellTitleFontSize` がフォントサイズを上書きする既存規則を含む) を用いる
- 候補行および選択面の背景は実効セル背景色 (CellStyle → Theme) を用いる
- 候補行の区切り線は `Theme.separatorColor`、タップ時のハイライトは `Theme.selectedColor` を用いる
- 選択印 (チェックマーク) の強調色は3段解決 (`PickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor`) を維持する

#### Scenario: 候補行のタイトルが実効値で描画される
- **GIVEN** `Theme.cellTitleColor` と `Theme.cellTitleFontSize` を指定した KsSettingsView 内の PickerCell
- **WHEN** 選択面を開く
- **THEN** 候補行のタイトルは指定した色・サイズで表示される

#### Scenario: CellStyle は Theme より優先される
- **GIVEN** `Theme.cellTitleColor` / `Theme.cellBackgroundColor` と、それぞれ異なる値の `style.titleColor` / `style.backgroundColor` を指定した PickerCell
- **WHEN** 選択面を開く
- **THEN** 候補行のタイトル色と背景は CellStyle の値で表示される

#### Scenario: 背景・区切り線・ハイライトが Theme から解決される
- **GIVEN** `cellBackgroundColor` / `separatorColor` / `selectedColor` を指定した Theme (CellStyle の指定なし)
- **WHEN** 選択面を開く
- **THEN** 候補行と選択面の背景・区切り線・タップ時ハイライトへそれぞれの解決値が反映される

#### Scenario: 選択印は Cell 固有値が最優先される
- **GIVEN** `accentColor` を明示指定した PickerCell (`style.accentColor` も指定あり)
- **WHEN** 選択面を開く
- **THEN** 選択印は `accentColor` の指定色で表示される

#### Scenario: 選択印は CellStyle へフォールバックする
- **GIVEN** `accentColor` が null で `style.accentColor` を指定した PickerCell
- **WHEN** 選択面を開く
- **THEN** 選択印は `style.accentColor` の色で表示される

#### Scenario: 選択印は Theme の既定色へフォールバックする
- **GIVEN** `accentColor` と `style.accentColor` がいずれも null の PickerCell
- **WHEN** 選択面を開く
- **THEN** 選択印は `Theme.cellAccentColor` の色で表示される

### Requirement: ナビゲーションバーへのスタイル適用

選択面のナビゲーションバーは、Cancel / 確定ボタンの色に**選択印と同一の解決済み強調色** (3段解決の結果値。Theme を別途参照しない) を適用し、タイトルの文字色に実効タイトル色を適用する SHALL。ボタン構成は現行を維持する (単一選択は Cancel のみ、複数選択は Cancel と確定)。タイトル・ボタンのフォントサイズはシステム既定を維持する。

#### Scenario: 単一選択の Cancel へ解決値が反映される
- **GIVEN** `accentColor` を明示指定した単一選択の PickerCell
- **WHEN** 選択面を開く
- **THEN** Cancel ボタンは選択印と同じ `accentColor` の色で表示され、確定ボタンは存在しない

#### Scenario: 複数選択の Cancel / 確定とタイトルへ解決値が反映される
- **GIVEN** Theme で強調色とタイトル色を指定した複数選択の PickerCell
- **WHEN** 選択面を開く
- **THEN** Cancel / 確定ボタンは選択印と同じ解決済み強調色、ナビゲーションバーのタイトルは実効タイトル色で表示される

### Requirement: 選択面のタイトル解決

選択面のタイトルには `pageTitle ?: title` を表示する SHALL (pageTitle が nil のとき Cell の title へフォールバックする)。

#### Scenario: pageTitle が nil なら title を表示する
- **GIVEN** `pageTitle = nil` かつ `title = "テーマ"` の PickerCell
- **WHEN** 選択面を開く
- **THEN** 選択面のタイトルに「テーマ」が表示される (pageTitle 指定時は pageTitle)

### Requirement: 候補行のアクセシビリティ状態

選択面の各候補行は、候補の表示名と現在の選択状態をアクセシビリティサービスへ公開する SHALL。複数選択でチェックをトグルした後は、公開される選択状態も更新される。

#### Scenario: 選択状態が公開される
- **GIVEN** 選択済み項目のある選択面
- **WHEN** 候補行のアクセシビリティ情報を取得する
- **THEN** 選択済み項目は「選択されている」状態、未選択項目は「選択されていない」状態として公開される

#### Scenario: トグル後に公開状態が更新される
- **GIVEN** 複数選択の選択面の未チェック項目
- **WHEN** その項目をタップしてチェックする
- **THEN** その候補行の公開される選択状態が「選択されている」に更新される

### Requirement: 選択中の項目への初期スクロール

選択面を開いたとき、選択中の項目 (単一選択は `selectedIndex`、複数選択は選択中の最小 index) が可視領域の**中央付近**に来た状態で表示する SHALL (リスト端部付近の項目では、スクロール可能範囲によるクランプで端寄せになることを許容する)。有効 (範囲内) index の抽出は**スクロール先の計算にのみ**用い、選択集合は正規化しない — 範囲外 index が作業状態・確定 callback に保持される現行挙動を維持する。選択が無い場合、または範囲外の index しか無い場合は先頭から表示する。`items` が空の場合も候補0件の選択面を提示し、スクロールは行わない。

#### Scenario: 単一選択は選択中の項目が中央付近に来た状態で開く
- **GIVEN** 候補50件・`selectedIndex = 30` の単一選択 PickerCell
- **WHEN** 選択面を開く
- **THEN** index 30 の項目が可視領域の中央付近に表示されている

#### Scenario: 複数選択は選択中の最小 index が中央付近に来た状態で開く
- **GIVEN** 候補50件・`selectedIndices = {40, 12, 25}` の複数選択 PickerCell
- **WHEN** 選択面を開く
- **THEN** index 12 の項目が可視領域の中央付近に表示されている

#### Scenario: 範囲外 index はスクロール対象から除外されるが選択集合には残る
- **GIVEN** 候補3件・`selectedIndices = {1, 5}` の複数選択 PickerCell
- **WHEN** 選択面を開き、確定操作を行う
- **THEN** スクロール先は index 1 で決まり、確定 callback には範囲外の 5 が保持された `{1, 5}` が渡される

#### Scenario: 未選択・範囲外のみの場合は先頭から表示する
- **GIVEN** 選択なし (または範囲外 index のみ選択) の PickerCell
- **WHEN** 選択面を開く
- **THEN** 先頭の項目から表示される

#### Scenario: items が空でも選択面は提示される
- **GIVEN** `items` が空の PickerCell
- **WHEN** 行をタップする
- **THEN** 候補0件の選択面が提示され、クラッシュしない
