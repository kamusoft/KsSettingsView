## ADDED Requirements

### Requirement: Cell 固有色の未指定表現

Cell 種別が意味上の固有値として持つ色引数 (`EntryCell` の `placeholderColor` / `accentColor`、`ButtonCell` の `titleColor`、`SwitchCell` / `CheckboxCell` / `SimpleCheckCell` / `RadioCell` / `PickerCell` / `NumberPickerCell` / `TimePickerCell` / `DatePickerCell` の `accentColor`、`DatePickerCell` の `androidButtonColor`) は、Theme / CellStyle の色フィールドと同じく非 nullable の Compose `Color` 型であり、未指定は `Color.Unspecified` で表す (SHALL)。既定値は `Color.Unspecified` である (SHALL)。同じ引数を持つ Compose DSL の Cell 関数と、選択系 Cell の projection の引数も同じ型・同じ既定値を持つ (SHALL)。`null` を未指定として受ける色引数は存在しない (SHALL NOT)。色以外の nullable 引数の未指定表現は本 change 前と同じである (SHALL)。

Cell 固有色の解決順 (Cell 固有値 → CellStyle → Theme → 外観既定または platform 既定) は本 change 前と同じで、`Unspecified` の段は次の段へ進む (SHALL)。描画時に `Unspecified` が ARGB へ変換されることはない (SHALL NOT)。`Unspecified` 同士の Cell は等価であり、未指定の Cell 固有色は Cell の等価性判定を本 change 前 (`null` 同士) と同じ結果にする (SHALL)。

#### Scenario: 既定の Cell 固有色は未指定
- **GIVEN** 色引数を渡さずに構築した `EntryCell` / `ButtonCell` / `SwitchCell` / `DatePickerCell`
- **WHEN** 各 Cell 固有色を読む
- **THEN** すべて `Color.Unspecified` である

#### Scenario: 未指定の Cell 固有色は CellStyle と Theme へ継承する
- **GIVEN** accent を明示した `Theme` と、accent が `Unspecified` の `SwitchCell`
- **WHEN** 実効 accent を解決する
- **THEN** accent は Theme の値になり、透明な黒にはならない

#### Scenario: 明示した Cell 固有色は CellStyle と Theme より優先する
- **GIVEN** accent を明示した `Theme`、accent を明示した `CellStyle`、それらと異なる accent を明示した `SwitchCell`
- **WHEN** 実効 accent を解決する
- **THEN** accent は Cell 固有値になる

#### Scenario: DSL の Cell 関数は色引数を省略すると未指定になる
- **GIVEN** `accentColor` を渡さない Compose DSL の `switchCell` と、`placeholderColor` を渡さない `entryCell`
- **WHEN** 生成された Cell を読む
- **THEN** 各 Cell 固有色は `Color.Unspecified` である

#### Scenario: EntryCell の placeholder は段ごとに解決する
- **GIVEN** placeholder 色を (a) Cell 固有値 / `CellStyle` / `Theme` の 3 段に別の値で明示、(b) Cell 固有値だけ未指定で `CellStyle` / `Theme` を明示、(c) `Theme` だけ明示、(d) いずれも未指定、の 4 通りの EntryCell
- **WHEN** 実効 placeholder 色を解決する
- **THEN** (a) は Cell 固有値、(b) は CellStyle の値、(c) は Theme の値、(d) は `Unspecified` (platform 既定へ委ねる印) になる

#### Scenario: ButtonCell の title は段ごとに解決する
- **GIVEN** title 色を (a) `titleColor` に明示、(b) `titleColor` 未指定で `CellStyle.titleColor` を明示、(c) `Theme.cellTitleColor` だけ明示、(d) いずれも未指定、の 4 通りの ButtonCell
- **WHEN** 実効 title 色を解決する
- **THEN** (a) は Cell 固有値、(b) は CellStyle の値、(c) は Theme の値、(d) は現在の外観の ButtonCell 既定になる

#### Scenario: DatePickerCell の androidButtonColor は未指定なら accent へ倒れる
- **GIVEN** `androidButtonColor` が未指定で accent が解決できる DatePickerCell
- **WHEN** 選択面のボタン色を解決する
- **THEN** ボタン色は解決済みの accent になり、透明な黒にはならない

### Requirement: 明示した Cell 色と外観

利用者が `CellStyle` または Cell 固有値として明示した色は、ライブラリが別の値へ置き換えない (SHALL NOT)。表示中に夜間モードが変わっても明示した色は変わらず、同じ行の未指定の色だけが現在の外観の既定へ再解決される (SHALL)。両外観で異なる色を使いたい利用者の手段は、Compose DSL では構築時に外観 (`isSystemInDarkTheme()`) で色を選ぶこと、Store / View 経路では外観の変更を受けて `replaceCell` / `replaceCells` で style または Cell 固有色を差し替えた Cell に置き換えることであり (SHALL)、ライブラリは公開 API として Cell へ外観を渡す型やコールバックを持たない (SHALL NOT。実効 style の解決が内部で外観を受けることは含まない)。

Compose DSL で再 composition により `CellStyle` または Cell 固有色だけが変わった Cell は内容更新として検出され、表示中の行が新しい色で再 bind される (SHALL)。Store 経路で `replaceCell` により style だけが異なる Cell に置き換えたときも同じである (SHALL)。

#### Scenario: 明示した CellStyle 色は夜間モードへの切替後も変わらない
- **GIVEN** title 色を固定値で明示した `CellStyle` を持つ LabelCell と、title 色が未指定の LabelCell を、Theme を渡さずライトで表示中
- **WHEN** 夜間モードへの Configuration 変更を View が受ける
- **THEN** 前者の title は明示した色のまま、後者の title は dark 既定の値で描き直される

#### Scenario: 明示した Cell 固有色は夜間モードへの切替後も変わらない
- **GIVEN** accent を固定値で明示した SwitchCell を、Theme を渡さずライトで表示中
- **WHEN** 夜間モードへの Configuration 変更を View が受ける
- **THEN** Switch の accent は明示した色のまま、行の背景は dark 既定の値で描き直される

#### Scenario: DSL で外観に応じて選んだ CellStyle 色は再 composition で行に届く
- **GIVEN** 外観フラグに応じて title 色を切り替える `CellStyle` を渡す Compose DSL の LabelCell を表示中
- **WHEN** フラグを切り替えて再 composition する
- **THEN** 差分は内容更新 (`replaceCell`) として発行され、表示中の行の title 色が新しい値になる

#### Scenario: Store 経路で style だけ差し替えた Cell は行に反映される
- **GIVEN** Store 経路で表示中の LabelCell
- **WHEN** 同じ ID で title 色だけ異なる `CellStyle` を持つ LabelCell に `replaceCell` で置き換える
- **THEN** 表示中の行の title 色が新しい値になり、Section / Cell の identity は維持される
