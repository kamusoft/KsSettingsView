## ADDED Requirements

### Requirement: SwiftUI ラッパの既定配置は親の全面
`KsSettingsView` (SwiftUI ラッパ) は Store 方式・DSL 方式のどちらでも、既定で container のセーフエリアを全辺で無視し、置かれた親の全面に広がる SHALL。bar (ナビゲーションバー・タブバー) に覆われる領域の一覧の inset は UIKit ホストの自動調整に任せ、ラッパ側で加算しない SHALL。keyboard 領域は無視の対象に含めない SHALL (キーボード表示中はラッパがキーボード領域の外側に縮む)。

#### Scenario: NavigationStack の中身として全画面で使うと一覧が画面下端まで描かれる
- **GIVEN** `NavigationStack` の中身として、修飾なしの `KsSettingsView` を全画面で置いた画面 (タブバーの有無を問わない)
- **WHEN** 画面を表示し、末尾までスクロールする
- **THEN** 一覧の背景が画面の上端から下端まで途切れず描かれ、画面下端 (ホームインジケータ領域、タブバーがあればその手前) に別色の帯が出ない。先頭 Cell はナビゲーションバーに隠れず、末尾 Cell はホームインジケータ領域やタブバーに隠れずに見える

#### Scenario: iOS 26 の大タイトルがスクロールで畳まれる
- **GIVEN** iOS 26 以降で、大タイトル表示の `NavigationStack` の中身として修飾なしの `KsSettingsView` を全画面で置いた画面
- **WHEN** 一覧を上方向へスクロールする
- **THEN** 大タイトルがインラインタイトルへ畳まれ、タイトルがバーの外へ流れて消えない

#### Scenario: キーボード表示中は入力 Cell がキーボードに隠れない
- **GIVEN** 修飾なしの `KsSettingsView` に EntryCell があり、キーボードが出ていない
- **WHEN** 画面下端付近の EntryCell をタップしてキーボードを出す
- **THEN** ラッパがキーボード領域の外側に縮み、編集中の EntryCell がキーボードに隠れずに見える (本変更前と同じ挙動)

#### Scenario: Store 方式と DSL 方式で配置が同じ
- **GIVEN** 同じ画面に Store 方式の `KsSettingsView` と DSL 方式の `KsSettingsView` をそれぞれ全画面で置いた 2 つの画面
- **WHEN** 両画面を表示する
- **THEN** 一覧の上端・下端の位置が両方式で一致する

### Requirement: セーフエリアを尊重する側へ戻す Root modifier
`KsSettingsView` は、セーフエリアを尊重する (置かれた親のセーフエリアの内側に縮む) 側へ戻す Root modifier `respectsSafeArea(_:)` を提供する SHALL。引数は Bool で既定値は `true`、`false` を渡すと既定の全面配置に戻る SHALL。modifier は元の値を変更せず copy を返す SHALL (既存の Root modifier と同じ規約)。`.style(_:)` / `.theme(_:)` / `.rootHeader(_:)` / `.rootFooter(_:)` との連鎖順序に依存しない SHALL。

#### Scenario: modifier を付けると従来どおりセーフエリアの内側に収まる
- **GIVEN** `NavigationStack` の中身として `KsSettingsView` に `.respectsSafeArea()` を付けて全画面で置いた画面
- **WHEN** 画面を表示する
- **THEN** 一覧はセーフエリアの内側に収まり、タブバー・ナビゲーションバーの後ろへ回り込まない (本変更前の既定と同じ配置)

#### Scenario: false を渡すと既定の全面配置になる
- **GIVEN** `KsSettingsView` に `.respectsSafeArea(false)` を付けて全画面で置いた画面
- **WHEN** 画面を表示する
- **THEN** 修飾なしのときと同じ全面配置になる

#### Scenario: 他の Root modifier と連鎖しても互いの値を失わない
- **GIVEN** `KsSettingsView` に `.style(.modern)` と `.respectsSafeArea()` と `.rootHeader("H")` を任意の順で連鎖させた値
- **WHEN** 各設定値を読む
- **THEN** style は `.modern`、セーフエリア尊重は有効、Root Header は "H" で、元の `KsSettingsView` の値は変わっていない
