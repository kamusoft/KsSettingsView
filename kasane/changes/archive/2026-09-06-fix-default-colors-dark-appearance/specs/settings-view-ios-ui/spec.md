## ADDED Requirements

### Requirement: 既定色の外観追随 (iOS)

Theme の既定色 (list 下地・Cell 背景・separator・選択色・accent・disabled 文字・Header / Footer の文字と背景・ButtonCell の title) は、アプリの外観 (ライト / ダーク) に追随する (SHALL)。ライト外観での既定色は本 change 前と同じ値であり、既存利用者のライト時の見た目を変えない (SHALL)。ダーク外観での既定色はライブラリが所有する dark セットの値で、その生値は承認モック (`ui/mock/approved.png` の色ロール対応表) を正とし、Android の dark セットと同じ値である (SHALL)。Cell title / description の既定は従来どおりシステム色に委ねる (SHALL)。

既定色を表す `Theme` の public static 定数と `init` の既定引数は、両外観の値を持つ `UIColor` (dynamic) として公開され、型・名前・可視性は本 change 前と同じである (SHALL)。ライブラリは利用者が明示指定した色を別の既定値へ置き換えない (SHALL NOT): 固定色 (dynamic でない `UIColor`) は外観で変わらず、両外観の値を持つ `UIColor` はその色自身の値へ解決される (SHALL)。`Theme` の値等価性は、既定同士 (`Theme() == Theme()`) と既定値の定数 (`Theme.defaultXxx`) を明示的に渡した Theme の間で本 change 前と同じ判定結果を返す (SHALL)。既定値と同じ生値を固定色 (dynamic でない `UIColor`) として明示した Theme は、既定 Theme と等価ではなく、その色は外観で変わらない (SHALL — 明示指定の扱い)。

#### Scenario: Theme を渡さずダーク外観で表示する
- **GIVEN** Theme を渡さない (既定の `Theme()`) KsSettingsView と、ダーク外観の window
- **WHEN** 表示する
- **THEN** list 下地・Cell 背景・separator・Header / Footer の文字は dark セットの値で描画され、Cell title と組み合わせて判読できる

#### Scenario: ライト外観の既定色は変わらない
- **GIVEN** Theme を渡さない KsSettingsView と、ライト外観の window
- **WHEN** 表示する
- **THEN** 各既定色は本 change 前の固定値と同じ値で描画される

#### Scenario: 一部だけ上書きした Theme の残りが追随する
- **GIVEN** accent だけを明示した Theme と、ダーク外観の window
- **WHEN** 表示する
- **THEN** accent は明示した色のまま、他の既定色は dark セットの値で描画される

#### Scenario: 明示指定した色は外観で変わらない
- **GIVEN** 固定の (dynamic でない) 色を list 下地に明示した Theme
- **WHEN** 外観をライトからダークへ切り替える
- **THEN** list 下地は明示した色のまま描画される

#### Scenario: 利用者の dynamic な色はその色自身の dark 値になる
- **GIVEN** 両外観の値を持つ `UIColor` を list 下地に明示した Theme
- **WHEN** ダーク外観で表示する
- **THEN** list 下地は利用者の色の dark 側の値で描画され、ライブラリの dark セットの値にはならない

#### Scenario: 表示中の外観切替に既定色が追随する
- **GIVEN** Theme を渡さずライト外観で表示中の KsSettingsView
- **WHEN** window の外観をダークへ切り替える
- **THEN** 表示中の行・list 下地・Header / Footer が dark セットの値で描き直され、Section / Cell の identity は維持される

#### Scenario: 既定色の生値が承認モックの対応表と一致する
- **GIVEN** Theme の各既定色
- **WHEN** ライトとダークの trait でそれぞれ解決する
- **THEN** 解決した生値が承認モックの色ロール対応表の light / dark の値と一致する

#### Scenario: 既定 Theme 同士の等価性
- **GIVEN** 引数なしで構築した 2 つの Theme と、既定値の定数 (`Theme.defaultBackgroundColor` 等) を明示的に渡して構築した Theme
- **WHEN** 等価性を比較する
- **THEN** 3 つは互いに等価と判定される

#### Scenario: 固定色で明示した Theme は既定と等価でない
- **GIVEN** 引数なしの Theme と、list 下地に既定と同じ生値の固定色を明示した Theme
- **WHEN** 等価性を比較し、ダーク外観で表示する
- **THEN** 2 つは等価ではなく、後者の list 下地はダークでも明示した固定色のまま描画される

### Requirement: Section 装飾の枠線色の外観再解決

Modern の Section 装飾の枠線色は、外観 (ライト / ダーク) の変化に追随する (SHALL)。両外観の値を持つ `sectionBorderColor` を指定した場合、外観の切替後に表示中の枠線が新しい外観の値で描き直される (SHALL)。

#### Scenario: dynamic な枠線色が外観切替に追随する
- **GIVEN** 両外観の値を持つ `sectionBorderColor` と正の `sectionBorderWidth` を指定した Theme で Modern を表示中
- **WHEN** window の外観をダークへ切り替える
- **THEN** 表示中の Section の枠線がダーク側の値で描き直される
