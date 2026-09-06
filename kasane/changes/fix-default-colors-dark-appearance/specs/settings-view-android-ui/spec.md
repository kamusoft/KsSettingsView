## ADDED Requirements

### Requirement: 色の未指定表現

`Theme` と `CellStyle` の色フィールドはすべて非 nullable の Compose `Color` 型であり、未指定は `Color.Unspecified` で表す (SHALL)。`Theme` の色フィールドの既定値はすべて `Color.Unspecified` である (SHALL)。`null` を未指定として受ける色フィールドは存在しない (SHALL NOT)。色以外のフィールド (font・寸法・余白) の未指定表現は本 change 前と同じである (SHALL)。

通常属性の色は `CellStyle` → `Theme` → 外観既定 (本 capability「未指定色の外観解決」) の順で、最初に指定された (`Unspecified` でない) 値を使う (SHALL)。利用者が「既定へ戻す」意図で `Color.Unspecified` を渡した場合、その段は未指定として次の段へ進む (SHALL)。既定と同じ生値を明示した Theme は既定 Theme (`Theme()`) と等価ではなく、その色は外観で変わらない (SHALL — 明示指定の扱い)。

#### Scenario: 既定の Theme はすべて未指定
- **GIVEN** 引数なしで構築した `Theme`
- **WHEN** 各色フィールドを読む
- **THEN** すべて `Color.Unspecified` である

#### Scenario: CellStyle の未指定は Theme へ継承する
- **GIVEN** title 色を明示した `Theme` と、title 色が `Unspecified` の `CellStyle`
- **WHEN** 実効 style を解決する
- **THEN** title 色は Theme の値になる

#### Scenario: 既定と同じ生値の明示は既定と等価でない
- **GIVEN** `Theme()` と、Cell 背景に light セットと同じ生値を明示した Theme
- **WHEN** 等価性を比較し、夜間モードで表示する
- **THEN** 2 つは等価ではなく、後者の Cell 背景は夜間でも明示した値のまま描画される

#### Scenario: Unspecified を明示的に渡して既定へ戻す
- **GIVEN** accent を明示した `Theme` を `copy(cellAccentColor = Color.Unspecified)` した Theme
- **WHEN** 実効 style を解決する
- **THEN** accent は外観既定の値になる

### Requirement: ライブラリ既定色の light / dark セット

ライブラリは light / dark の 2 セットの既定 Theme を `KsSettingsViewDefaults` の factory として公開する (SHALL): `lightTheme()` / `darkTheme()` は外観で決まる既定を持つ色フィールド (list 下地・Cell 背景・separator・選択色・accent・disabled 文字・Header / Footer の文字と背景・Cell title・Cell description) が指定済みの `Theme` を返し、`theme(darkTheme: Boolean)` はどちらかを返し、`@Composable theme()` は現在の外観 (`isSystemInDarkTheme()`) で選ぶ (SHALL)。light セットの各値は本 change 前の既定色と同じであり、既存利用者のライト時の見た目を変えない (SHALL)。dark セットの生値は承認モック (`ui/mock/approved.png` の色ロール対応表) を正とし、iOS の dark セットと同じ値である (SHALL)。title の既定はホストや同梱テーマの属性から動的に解決しない (SHALL NOT)。ButtonCell の title 既定 (4 段解決の最終段) は Theme のフィールドではなく、light / dark の既定セットと同じ出所から外観で選ばれ、accent の指定とは独立である (SHALL)。他のフィールドへフォールバックする色 (valueText → title、hintText → accent) と platform 既定に委ねる色 (placeholder のホスト hint 色、`sectionBorderColor` の透明) は両セットでも `Unspecified` のままで、既存のフォールバック契約を保つ (SHALL)。

既定色を表す `Theme` companion の色定数は公開しない (SHALL NOT)。既定値を基準に派生値を作る用途は factory が返す Theme の値から行う (SHALL)。icon サイズ・角丸の既定定数は本 change 前と同じく公開される (SHALL)。

#### Scenario: light セットは現行の既定色と同値
- **GIVEN** `lightTheme()` が返す Theme
- **WHEN** 各色フィールドを読む
- **THEN** 本 change 前の既定色 (承認モックの light 列) と同じ値である

#### Scenario: dark セットは承認モックの対応表と同値
- **GIVEN** `darkTheme()` が返す Theme
- **WHEN** 各色フィールドを読む
- **THEN** 対応表にあるロールは dark 列と同じ値であり、valueText / hintText / placeholder / `sectionBorderColor` は `Unspecified` のままである

#### Scenario: 外観で factory の結果が切り替わる
- **GIVEN** `theme(darkTheme = true)` と `theme(darkTheme = false)`
- **WHEN** それぞれの結果を比べる
- **THEN** 前者は `darkTheme()`、後者は `lightTheme()` と等価である

### Requirement: 未指定色の外観解決

`KsSettingsView` は Theme を受け取った時点で、`Unspecified` の色フィールドのうち既定セットが値を持つもの (前 Requirement の列挙) を現在の外観 (同梱テーマ付き Context と同じ判定源である Configuration の夜間モード) に対応する既定セットの値で埋めた解決済み Theme を作り、描画に関わるすべての箇所 (行の実効 style・list 下地・separator と Section 装飾・Section / Root の Header / Footer・選択面・カレンダーの選択面) は解決済み Theme の値だけを使う (SHALL)。描画時に `Unspecified` が ARGB へ変換されることはない (SHALL NOT — フォールバック先を持つ色は実効 style の解決でフォールバック先の値に、placeholder は platform 既定に、`sectionBorderColor` は透明に落ちる)。明示指定された色は解決で変換されない (SHALL NOT)。利用者が読む `theme` プロパティと Store の `theme` は利用者が渡した Theme のままであり、解決済み値を返さない (SHALL)。構造更新 (Full diff・Section 置換・可視性変更・Store の再同期) は利用者の Theme を変えず、解決済み Theme を利用者の Theme として保存しない (SHALL NOT)。表示中の選択面・カレンダーの選択面は開いた時点の解決済み値で描かれ、表示中の外観変更では描き直さず、次回表示から新しい外観の値を使う (SHALL)。

#### Scenario: Theme を渡さず夜間モードで表示する
- **GIVEN** Theme を渡さない KsSettingsView と、夜間モードの Configuration
- **WHEN** 表示する
- **THEN** list 下地・Cell 背景・separator・Header / Footer の文字・Cell title と description は dark セットの値で描画される

#### Scenario: ライトの既定は本 change 前と同じ
- **GIVEN** Theme を渡さない KsSettingsView と、夜間モードでない Configuration
- **WHEN** 表示する
- **THEN** 各既定色は light セット (本 change 前の既定色) で描画され、Cell title の色も本 change 前の実効値と一致する

#### Scenario: 一部だけ上書きした Theme の残りが追随する
- **GIVEN** accent だけを明示した Theme と、夜間モードの Configuration
- **WHEN** 表示する
- **THEN** accent は明示した色のまま、他の色は dark セットの値で描画される

#### Scenario: 選択面とカレンダーの選択面も解決済み値で描かれる
- **GIVEN** Theme を渡さない KsSettingsView と、夜間モードの Configuration
- **WHEN** PickerCell の選択面と DatePickerCell (calendar) の選択面を開く
- **THEN** 選択面の地色・罫線・文字色は dark セットの値で描画される

#### Scenario: title を明示した Theme の valueText は title へフォールバックする
- **GIVEN** Cell title 色だけを明示した Theme と、夜間モードの Configuration
- **WHEN** valueText を持つ LabelCell を表示する
- **THEN** valueText は明示した title 色で描画され、dark セットの title 値にはならない

#### Scenario: ButtonCell の title 既定は外観で選ばれ accent と独立
- **GIVEN** accent だけを明示した Theme と、title 色未指定の ButtonCell、夜間モードの Configuration
- **WHEN** 表示する (Compose 経路の解決と View 経路の描画の両方)
- **THEN** ButtonCell の title は dark セットの ButtonCell 既定で描画され、明示した accent にはならず、両経路の値は一致する

#### Scenario: 利用者が読む Theme は解決前のまま
- **GIVEN** accent だけを明示した Theme を渡した KsSettingsView
- **WHEN** `theme` プロパティを読む
- **THEN** accent 以外のフィールドは `Unspecified` のままである

### Requirement: 夜間モード変更時の未指定色の再解決

`KsSettingsView` は、Activity が再生成されずに夜間モードだけが変わる Configuration 変更 (`configChanges` で uiMode を自前処理するホスト) を受けたとき、利用者が渡した Theme から未指定色を新しい外観で再解決し、表示中の行・list 下地・Section 装飾・Header / Footer に再適用する (SHALL)。再適用は Theme 更新の既存契約に従い、設定ツリーの構造と Section / Cell の identity を変えない (SHALL)。明示指定された色は再解決で変わらない (SHALL NOT)。window へ attach された時点の夜間モードが解決時と異なる場合も、attach 時に再解決する (SHALL)。夜間モードが変わらない Configuration 変更では再適用しない (SHALL NOT)。

#### Scenario: 表示中に夜間モードへ切り替わる
- **GIVEN** Theme を渡さずライトで表示中の KsSettingsView (Activity は uiMode を自前処理し再生成しない)
- **WHEN** Configuration が夜間モードへ変わる
- **THEN** 表示中の行・list 下地・Header / Footer が dark セットの値で描き直され、Section / Cell の identity は維持される

#### Scenario: 明示指定色は切替後も変わらない
- **GIVEN** list 下地を明示した Theme で表示中の KsSettingsView
- **WHEN** Configuration が夜間モードへ変わる
- **THEN** list 下地は明示した色のまま、未指定の色だけが dark セットへ変わる

#### Scenario: 構造更新を挟んでも未指定色は追随し続ける
- **GIVEN** accent だけを明示した Theme でライト表示中の KsSettingsView
- **WHEN** Full diff・Section 置換・可視性変更を順に適用してから、Configuration が夜間モードへ変わる
- **THEN** accent は明示した色のまま、未指定の色は dark セットへ変わる (構造更新で未指定色が明示扱いにならない)

#### Scenario: 表示中の選択面は次回表示から追随する
- **GIVEN** Theme を渡さずライトで表示中の KsSettingsView で PickerCell の選択面を開いた状態
- **WHEN** Configuration が夜間モードへ変わり、選択面を閉じて開き直す
- **THEN** 開き直した選択面は dark セットの値で描画される

#### Scenario: 夜間モード以外の Configuration 変更では再適用しない
- **GIVEN** 表示中の KsSettingsView
- **WHEN** 夜間モードが同じまま画面向きだけが変わる Configuration 変更を受ける
- **THEN** Theme の再適用は発生しない

### Requirement: Compose 入口の既定 Theme は外観で選ばれる

DSL 方式の `KsSettingsView` Composable の `theme` 引数の既定値は、現在の外観で選ばれた既定セット (`KsSettingsViewDefaults.theme()`) である (SHALL)。外観の変化で既定値が変わったとき、再 composition で新しい Theme が Store の Theme 更新経路へ流れる (SHALL)。

#### Scenario: 夜間モードの composition では dark セットが既定になる
- **GIVEN** 夜間モードの Configuration で composition された、`theme` を渡さない DSL 方式の KsSettingsView
- **WHEN** 表示する
- **THEN** dark セットの値で描画される

## MODIFIED Requirements

### Requirement: Theme の Section 装飾4属性

`Theme` は `sectionMargin: PaddingValues?`・`sectionCornerRadius: Dp?`・`sectionBorderWidth: Dp?`・`sectionBorderColor: Color` を公開する (SHALL)。`sectionMargin` / `sectionCornerRadius` / `sectionBorderWidth` の null と、`sectionBorderColor` の `Color.Unspecified` は未指定を表し、style 別の platform 既定へ解決する (SHALL): Modern は現行実装値を引き継いだライブラリ既定、Classic は margin 上下 0。borderWidth 未指定の実効値は 0、borderColor 未指定の実効値は透明とし、既定の Modern にボーダーは描かれない (SHALL)。4属性は `Theme` の値等価性に参加する (SHALL)。`sectionMargin` の等価比較は `PaddingValues` の equals へ委譲し (SHALL)、可変な独自 `PaddingValues` 実装を同一参照のまま変更した場合の再描画は保証しない。

`sectionMargin` は Section 単位 (Header・Cell 箱・Footer を一体とした表示単位) の**外側**余白であり (SHALL)、水平成分は leading / trailing (start / end) 基準で解釈する (SHALL)。隣接 Section 間の間隔は前 Section の bottom と次 Section の top の加算とし、先頭 Section の top・末尾 Section の bottom は list 端に対しても適用する (SHALL)。負の寸法成分は 0 として扱い、`sectionCornerRadius` は箱の寸法から幾何的に許される値へ描画時に clamp する (SHALL — Theme 構築時には拒否しない)。Modern は新たな色既定を導入しない (SHALL NOT): 箱と下地の色は既存の `cellBackgroundColor` / `backgroundColor` (未指定なら外観既定へ解決した値) から解決し、箱の視認性は両者の対比に依存する。

#### Scenario: 未指定の Theme で Modern を表示する
- **GIVEN** 4属性を指定しない `Theme` と `style = Modern` の KsSettingsView
- **WHEN** 表示する
- **THEN** Section はライブラリ既定の余白・角丸で箱として描画され、ボーダーは描かれない

#### Scenario: 指定値が箱の描画へ反映される
- **GIVEN** 4属性すべてを明示した `Theme` と `style = Modern` の KsSettingsView
- **WHEN** 表示する
- **THEN** 箱の余白・角丸半径・ボーダー幅・ボーダー色は指定値で描画される

#### Scenario: 実行時の Theme 変更が装飾へ反映される
- **GIVEN** Modern で表示中の KsSettingsView
- **WHEN** `sectionCornerRadius` だけが異なる Theme を適用する
- **THEN** 表示中の Section の箱が新しい角丸半径で再描画され、Section / Cell の identity は維持される

#### Scenario: sectionMargin は Header / Footer を含む Section 単位を包む
- **GIVEN** header text と footer text を持つ Section、上下に正の値を持つ `sectionMargin`、`style = Modern`
- **WHEN** 表示する
- **THEN** top 余白は Header の上・bottom 余白は Footer の下に入り、Header と箱の間・箱と Footer の間には入らない

#### Scenario: 負の成分は 0 として扱う
- **GIVEN** 負の成分を含む `sectionMargin` と負の `sectionBorderWidth` を持つ Theme と `style = Modern`
- **WHEN** 表示する
- **THEN** 負の成分は 0 と同じ描画結果になり、例外や不正 geometry を生じない

#### Scenario: Unspecified の枠線色は透明として描かれる
- **GIVEN** 正の `sectionBorderWidth` と `sectionBorderColor = Color.Unspecified` の Theme と `style = Modern`
- **WHEN** 表示する
- **THEN** ボーダーは描かれない
