## ADDED Requirements

### Requirement: スクロールバー表示の Theme 反映 (iOS)

設定リストの縦スクロールバーの表示有無は `Theme.scrollIndicatorVisible` に従う (SHALL)。既定 (`true`) では表示し、`false` では表示しない (SHALL)。反映は最初の表示時と、表示中の Theme 差し替え時 (Host への直接適用・Store 経由・SwiftUI の DSL 再評価のいずれの経路でも) の両方で行う (SHALL)。Theme の差し替えでスクロールバーの表示だけが変わるとき、表示中の行は作り直されず (Section / Cell の ID と、表示中の行の View インスタンスがともに同じ)、スクロール位置 (設定リストの content offset) は変わらない (SHALL)。横スクロールバーは本プロパティの対象にしない (SHALL NOT)。

PickerCell の選択面の候補リストの縦スクロールバーも、選択面を開いた時点の `Theme.scrollIndicatorVisible` に従う (SHALL)。表示中の選択面は、その後の Theme 差し替えに追従しなくてよく、次に開いたときから新しい値に従う (SHALL)。回転ホイールの選択面 (数値・日付・時刻) は本プロパティの対象にせず、スクロールバーを表示しない (SHALL NOT)。

#### Scenario: 既定の Theme ではスクロールバーを表示する
- **GIVEN** Theme を渡さない KsSettingsView
- **WHEN** 表示する
- **THEN** 設定リストの縦スクロールバーは表示が有効になっている

#### Scenario: false を指定した Theme ではスクロールバーを表示しない
- **GIVEN** `scrollIndicatorVisible` が `false` の Theme を渡した KsSettingsView
- **WHEN** 表示する
- **THEN** 設定リストの縦スクロールバーは表示が無効になっている

#### Scenario: 表示中の Theme 差し替えに追従する
- **GIVEN** 既定の Theme で表示中の KsSettingsView
- **WHEN** `scrollIndicatorVisible` が `false` の Theme を適用し、その後 `true` の Theme を適用する
- **THEN** 縦スクロールバーの表示は無効になり、その後ふたたび有効になる

#### Scenario: Store 経由と DSL 経由でも同じ結果になる
- **GIVEN** Store 方式で表示中の KsSettingsView と、DSL 方式で表示中の KsSettingsView
- **WHEN** それぞれの経路で `scrollIndicatorVisible` が `false` の Theme へ更新する
- **THEN** どちらも縦スクロールバーの表示が無効になる

#### Scenario: スクロールバーの表示だけを変えても行とスクロール位置は維持される
- **GIVEN** 画面に収まらない数の行を持ち、途中までスクロールした状態で表示中の KsSettingsView
- **WHEN** `scrollIndicatorVisible` だけが異なる Theme を適用する
- **THEN** 表示中の行の View インスタンスと Section / Cell の ID は適用前と同じで、content offset も適用前と同じである

#### Scenario: PickerCell の候補リストも表示設定に従う
- **GIVEN** 既定の Theme の KsSettingsView と、`scrollIndicatorVisible` が `false` の Theme の KsSettingsView
- **WHEN** それぞれの PickerCell から選択面を開く
- **THEN** 前者の候補リストは縦スクロールバーの表示が有効で、後者は無効になっている

### Requirement: Header / Footer 背景色の Theme 反映 (iOS)

text 形式の Section Header / Footer と Root Header / Footer の領域の背景は、Header は `Theme.headerBackgroundColor`、Footer は `Theme.footerBackgroundColor` で描画する (SHALL)。反映は最初の表示時と、表示中の Theme 差し替え時・外観 (ライト / ダーク) 切替時の両方で行い、text の色・フォントの再適用と同じく Section / Cell の identity を維持する (SHALL)。View 形式の Header / Footer にはライブラリの背景色を適用しない (SHALL NOT)。text 形式から View 形式へ差し替えられた領域に、以前の背景色を残さない (SHALL NOT)。

本 Requirement は「iOS は Header / Footer 領域の背景に Theme の背景色を適用しない」という従来の platform 非対称を解消し、適用範囲を Android と同じにする。

#### Scenario: 指定した背景色が text の Header / Footer に描画される
- **GIVEN** Header と Footer の背景色をそれぞれ別の色で明示した Theme と、text の Section Header / Footer・Root Header / Footer を持つ KsSettingsView
- **WHEN** 表示する
- **THEN** Section と Root の Header の領域は Header の背景色、Footer の領域は Footer の背景色で描画される

#### Scenario: 表示中の Theme 差し替えに追従する
- **GIVEN** Header の背景色を明示した Theme で表示中の KsSettingsView
- **WHEN** Header の背景色だけが異なる Theme を適用する
- **THEN** 表示中の Header の領域が新しい背景色で描き直され、Section / Cell の identity は維持される

#### Scenario: View 形式の Header には背景色を適用しない
- **GIVEN** Header の背景色を明示した Theme と、View 形式の Section Header を持つ KsSettingsView
- **WHEN** 表示する
- **THEN** View 形式の Header の領域にライブラリの背景色は描画されない

#### Scenario: 利用者の dynamic な背景色は外観切替に追従する
- **GIVEN** 両外観の値を持つ色を Header の背景色に明示した Theme で、ライト外観で表示中の KsSettingsView
- **WHEN** window の外観をダークへ切り替える
- **THEN** Header の領域は利用者の色のダーク側の値で描画される

#### Scenario: text 形式から View 形式へ差し替えた領域に背景色を残さない
- **GIVEN** Header の背景色を明示した Theme と、text の Section Header を表示中の KsSettingsView
- **WHEN** その Section の Header を View 形式へ差し替える
- **THEN** 差し替え後の Header の領域に、以前の text 形式で描画していたライブラリの背景色は残らない

### Requirement: Header / Footer 背景色の既定は透明 (iOS)

`Theme.headerBackgroundColor` / `footerBackgroundColor` の既定は、ライト / ダークのどちらの外観でも透明であり、何も指定しない Header / Footer の領域には list 下地がそのまま見える (SHALL)。既定値を表す公開定数 `Theme.defaultHeaderBackgroundColor` / `defaultFooterBackgroundColor` は、型・名前・可視性と、他の既定色定数と同じ「両外観の値を持つ `UIColor`」である形を保ち、両外観の値だけが透明になる (SHALL — core/ADR-0032)。これにより等価性の契約は他の既定色と同じに保たれる: 既定値の定数を明示した Theme は既定 Theme と等価であり、固定の透明色を明示した Theme は既定 Theme と等価ではない (SHALL)。この既定は Android の既定と同じである (SHALL)。

本 Requirement は、既定色の外観追随 (iOS) が定めた「Header / Footer の背景」の既定値 (ライトは従来の固定値、ダークはライブラリ所有の dark セットの値) を置き換える。他の既定色と、未指定・明示指定・等価性の契約は変えない。

#### Scenario: 何も指定しない Header / Footer には list 下地が見える
- **GIVEN** list 下地だけを明示した Theme と、text の Section Header / Footer を持つ KsSettingsView
- **WHEN** ライト外観とダーク外観のそれぞれで表示する
- **THEN** どちらの外観でも Header / Footer の領域は list 下地と同じ色に見える

#### Scenario: 既定値の定数は両外観で透明に解決される
- **GIVEN** `Theme.defaultHeaderBackgroundColor` と `Theme.defaultFooterBackgroundColor`
- **WHEN** ライトとダークの trait でそれぞれ解決する
- **THEN** いずれも透明である

#### Scenario: 既定 Theme 同士の等価性は保たれる
- **GIVEN** 引数なしで構築した Theme と、Header / Footer の背景色に既定値の定数を明示的に渡して構築した Theme
- **WHEN** 等価性を比較する
- **THEN** 2 つは等価と判定される

#### Scenario: 固定の透明色を明示した Theme は既定と等価でない
- **GIVEN** 引数なしで構築した Theme と、Header の背景色に固定の (dynamic でない) 透明色を明示した Theme
- **WHEN** 等価性を比較する
- **THEN** 2 つは等価ではないと判定され、どちらの Header の領域にも list 下地が見える
