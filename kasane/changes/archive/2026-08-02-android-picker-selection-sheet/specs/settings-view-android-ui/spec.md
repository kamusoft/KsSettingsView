# Delta: settings-view-android-ui (android-picker-selection-sheet)

## ADDED Requirements

### Requirement: PickerCell 選択面の提示

Android host は、`isEnabled` な PickerCell の行タップで選択面をモーダル提示する SHALL。選択面のタイトルには `pageTitle ?: title` を表示する。候補は `items` の全項目を列挙し、`displayFormatter` 指定時は各項目の表示へ適用する。確定に至らない閉じ方 — キャンセルボタン・選択面の外側タップ・Back 操作・下方向スワイプによる dismiss — では、いずれの経路でも選択状態 callback を発火しない。確定・キャンセルの操作ラベルは OS の公開文字列リソース (`android.R.string.ok` / `android.R.string.cancel`) から解決し、OS のロケールに追従する。

#### Scenario: タイトルの解決
- **GIVEN** `pageTitle = "テーマを選択"` かつ `title = "テーマ"` の PickerCell
- **WHEN** 行をタップして選択面を開く
- **THEN** 選択面のタイトルに「テーマを選択」が表示される (pageTitle が null なら「テーマ」)

#### Scenario: キャンセルは callback を発火しない
- **GIVEN** 選択面が表示されている
- **WHEN** 確定せずにキャンセル操作で閉じる
- **THEN** `onSelectionChanged` / `onMultiSelectionChanged` は発火しない

#### Scenario: 非確定 dismiss は経路によらず callback を発火しない
- **GIVEN** 複数選択の選択面で作業状態のチェックを変更した
- **WHEN** 選択面の外側タップ / Back 操作 / 下方向スワイプのいずれかで選択面が閉じる
- **THEN** `onMultiSelectionChanged` は発火せず、作業状態は破棄される

#### Scenario: 候補の全件列挙と displayFormatter の適用
- **GIVEN** `items = ["10", "20", "30"]` かつ各項目へ "px" を付加する `displayFormatter` を持つ PickerCell
- **WHEN** 選択面を開く
- **THEN** 候補は `items` の順序どおり3件列挙され、各項目の表示は「10px」「20px」「30px」となる

#### Scenario: 操作ラベルは OS リソースから解決される
- **GIVEN** 複数選択の PickerCell の選択面
- **WHEN** 選択面を表示する
- **THEN** 確定・キャンセルのラベルは `android.R.string.ok` / `android.R.string.cancel` の解決文字列と一致する

### Requirement: 単一選択の即時確定

`selectionMode == Single` の選択面は、現在の `selectedIndex` に対応する項目へ選択印を表示する SHALL。候補項目をタップすると `onSelectionChanged(index)` を発火し、選択面を閉じる。

#### Scenario: 項目タップで即確定して閉じる
- **GIVEN** `selectedIndex = 0` の単一選択 PickerCell の選択面が表示されている
- **WHEN** index 2 の項目をタップする
- **THEN** `onSelectionChanged(2)` が1回発火し、選択面が閉じる

### Requirement: 複数選択の確定・破棄と上限

`selectionMode == Multiple` の選択面は、`selectedIndices` に対応する各項目へ選択印を表示する SHALL。候補項目のタップは選択面内の作業状態のチェックをトグルするのみで callback を発火せず、確定操作で `onMultiSelectionChanged(作業状態の集合)` を1回発火して閉じる。キャンセル操作では作業状態を破棄する。`maxSelectedNumber > 0` のとき、上限到達後の新規チェックは無視して、拒否を示す触覚フィードバックをシステムへ要求する。既にチェック済みの項目の解除は上限到達後も常に可能とする。

#### Scenario: 確定操作で確定する
- **GIVEN** `selectedIndices = {1}` の複数選択 PickerCell の選択面で index 3 をチェックした
- **WHEN** 確定操作を行う
- **THEN** `onMultiSelectionChanged({1, 3})` が1回発火し、選択面が閉じる

#### Scenario: キャンセルで作業状態を破棄する
- **GIVEN** 複数選択の選択面で作業状態のチェックを変更した
- **WHEN** キャンセル操作で閉じる
- **THEN** `onMultiSelectionChanged` は発火しない

#### Scenario: 上限到達時は新規チェックを無視して触覚フィードバックを要求
- **GIVEN** `maxSelectedNumber = 3` で既に3項目チェック済みの選択面
- **WHEN** 未チェックの項目をタップする
- **THEN** チェックは付与されず、拒否を示す触覚フィードバックがシステムへ要求される

#### Scenario: 上限到達時もチェック解除は可能
- **GIVEN** `maxSelectedNumber = 3` で既に3項目チェック済みの選択面
- **WHEN** チェック済み項目をタップする
- **THEN** その項目のチェックが外れる

### Requirement: 選択印の強調色

選択面の選択印の色は、既存のスタイル解決契約「Cell 固有値 → CellStyle → Theme」に従い、`PickerCell.accentColor` → `PickerCell.style.accentColor` → `Theme.cellAccentColor` の順で解決する SHALL。

#### Scenario: Cell 固有値が最優先される
- **GIVEN** `accentColor` を明示指定した PickerCell (`style.accentColor` も指定あり)
- **WHEN** 選択面を開く
- **THEN** 選択済み項目の選択印は `accentColor` の指定色で表示される

#### Scenario: CellStyle へフォールバックする
- **GIVEN** `accentColor` が null で `style.accentColor` を指定した PickerCell
- **WHEN** 選択面を開く
- **THEN** 選択印は `style.accentColor` の色で表示される

#### Scenario: Theme の既定色へフォールバックする
- **GIVEN** `accentColor` と `style.accentColor` がいずれも null の PickerCell
- **WHEN** 選択面を開く
- **THEN** 選択印は `Theme.cellAccentColor` の色で表示される

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

### Requirement: モデル値の許容と非正規化

選択面は PickerCell のモデル値を正規化せずに扱う SHALL (現行挙動の維持)。範囲外の `selectedIndex` には選択印を表示しない。`selectedIndices` に含まれる範囲外 index は複数選択の作業状態に保持され、確定時の callback 集合にも残り、上限判定の件数にも含まれる。作業状態が最初から `maxSelectedNumber` を超えている場合も、既存チェックの解除は可能で、新規チェックは無視される。`items` が空の場合も選択面は提示され、候補は0件となる。

#### Scenario: 範囲外 index を含む複数選択の確定
- **GIVEN** `items` 3件・`selectedIndices = {1, 5}`・`maxSelectedNumber = 0` の複数選択 PickerCell の選択面
- **WHEN** index 2 をチェックして確定操作を行う
- **THEN** `onMultiSelectionChanged({1, 2, 5})` が発火する (範囲外の 5 は保持される)

#### Scenario: 初期上限超過時も新規チェックは無視・解除は可能
- **GIVEN** `items` 5件・`selectedIndices = {0, 1, 2, 3}`・`maxSelectedNumber = 3` の選択面
- **WHEN** 未チェックの index 4 をタップし、続けてチェック済みの index 0 をタップする
- **THEN** index 4 のチェックは付与されず、index 0 のチェックは外れる
