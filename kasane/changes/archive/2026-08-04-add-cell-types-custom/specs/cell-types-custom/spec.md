# cell-types-custom デルタスペック

## ADDED Requirements

### Requirement: CustomCell の定義と等価性

CustomCell は、利用者定義の content 値と、content から宣言 UI（iOS: SwiftUI View / Android: Composable）を生成する builder を保持する Cell として提供される SHALL。等価性（equals / hashCode / Hashable）は `id` / `style` / `content` / `showArrow` / `isEnabled` / `isVisible` のみが参加し、`builder` / `onTap` の関数値は除外される SHALL。

利用者側の契約として以下を要求する: content は値等価（iOS: `Hashable`、Android: `equals` / `hashCode` 実装）を持つ non-null の型であること（Android は型制約で強制）。`builder` / `onTap` は同一 id・同一 content の間で意味的に安定であること — 見た目や動作を変える値は関数のキャプチャではなく content に含めること（関数値だけを差し替えても再バインドは発生しない）。

#### Scenario: builder だけが異なるインスタンスは等価

- **GIVEN** 同一の id / content / スカラー値を持つ 2 つの CustomCell
- **WHEN** それぞれ別個に生成された builder クロージャを渡して比較する
- **THEN** 2 つは等価と判定される（差分検出は再バインドを発生させない）

#### Scenario: content が異なれば非等価

- **GIVEN** id が同一で content 値だけが異なる 2 つの CustomCell
- **WHEN** 等価性を比較する
- **THEN** 非等価と判定される（差分検出が再バインドを発生させる）

#### Scenario: 表示に効くスカラーの変更も非等価

- **GIVEN** content が同一で `showArrow` だけが異なる 2 つの CustomCell
- **WHEN** 等価性を比較する
- **THEN** 非等価と判定される

### Requirement: 事前登録なしの描画

CustomCell の描画型は標準登録集合（基本 Cell・入力 Cell と同列）に含まれ、利用者が Registry を操作することなく描画される SHALL。

#### Scenario: Registry 未操作で表示できる

- **GIVEN** 利用者が Registry への登録操作を一切行っていない通常の Host 構成
- **WHEN** CustomCell を含む root を表示する
- **THEN** CustomCell が builder の出力で描画される（strictMode でも例外にならない）

### Requirement: content 駆動の描画と再利用

CustomCell の行は bind 時に builder(content) の出力を表示する SHALL。行の再利用時、前の content に由来する表示・listener・購読を残さない SHALL（Cell Renderer Registry の再利用境界に従う）。content が等価のままの再構成では再バインドを要求しない SHALL。

#### Scenario: content の更新で表示が変わる

- **GIVEN** content 値 A で表示中の CustomCell
- **WHEN** 同一 id で content 値 B に置き換えた root を適用する
- **THEN** 行の表示が builder(B) の出力に更新される

#### Scenario: 再利用時に前の内容が残らない

- **GIVEN** CustomCell を含む長いリストをスクロールして行が再利用される状況
- **WHEN** 別の Cell の行として再バインドされる
- **THEN** 前の CustomCell の content 表示・タップ listener は残らない

### Requirement: 静的コンテンツの省略形

content 値を持たない CustomCell を builder のみで生成できる SHALL。この形の等価性は content を除く参加要素（id / style / showArrow / isEnabled / isVisible）で判定される SHALL。

#### Scenario: content なしで生成・表示できる

- **GIVEN** データを持たない固定表示のカスタム UI
- **WHEN** content 引数なしの形で CustomCell を生成して配置する
- **THEN** builder の出力が行として表示される

### Requirement: 行タップ

`onTap` が非 nil かつ `isEnabled` が true のとき、行のタップで `onTap` が発火する SHALL。content 内の操作可能要素がタップ・ジェスチャを消費した場合、行の `onTap` は発火しない SHALL（子の操作と行タップの二重発火は起きない）。`onTap` が nil（既定）のとき行タップの動作を持たず、content 内部の操作（ボタン・スライダー等）を妨げない SHALL。

`isEnabled` が false のとき、行タップは発火せず、content 内部の操作も抑止される SHALL（Cell の視覚状態契約「無効 Cell は操作 callback と内包 control の操作を抑止する」に従う）。ただしテキスト色の disabled 置換等の視覚表現は任意ビューには適用できないため、無効時の見た目の描き分けは利用者責務とする。

#### Scenario: onTap 指定時に行タップで発火する

- **GIVEN** `onTap` を指定した有効な CustomCell
- **WHEN** 行をタップする
- **THEN** `onTap` が 1 回呼ばれる

#### Scenario: 既定では行タップ動作を持たない

- **GIVEN** `onTap` を指定しない CustomCell（content 内に操作可能なコントロールを含む）
- **WHEN** content 内のコントロールを操作する
- **THEN** 行レベルのタップ処理は発生せず、コントロールの操作がそのまま機能する

#### Scenario: 子要素の操作では行タップが発火しない

- **GIVEN** `onTap` を指定し、content 内にボタンを含む CustomCell
- **WHEN** content 内のボタンをタップする
- **THEN** ボタンのアクションだけが実行され、行の `onTap` は呼ばれない

#### Scenario: 無効時はタップが発火しない

- **GIVEN** `onTap` を指定し `isEnabled = false` とした CustomCell
- **WHEN** 行をタップする
- **THEN** `onTap` は呼ばれない

#### Scenario: 無効時は content 内の操作も抑止される

- **GIVEN** `isEnabled = false` で content 内にボタンを含む CustomCell
- **WHEN** content 内のボタンをタップする
- **THEN** ボタンのアクションは実行されない

### Requirement: Disclosure Indicator の表示

`showArrow` が true のとき、標準 Cell（CommandCell）と同一の Disclosure Indicator が行に表示され、content の占有領域は indicator の領域を除いた範囲になる SHALL。false（既定）のとき indicator は表示されず、content が行全域を占有する SHALL。`showArrow` は `onTap` と独立に指定できる SHALL。

#### Scenario: showArrow で indicator が表示される

- **GIVEN** `showArrow = true` の CustomCell
- **WHEN** 行を表示する
- **THEN** 標準 Cell と同一の Disclosure Indicator が表示される

#### Scenario: 既定では表示されない

- **GIVEN** `showArrow` を指定しない CustomCell
- **WHEN** 行を表示する
- **THEN** Disclosure Indicator は表示されず、content が行全域に描画される

### Requirement: スタイルの適用範囲

CustomCell は `style: CellStyle` を保持し、行レベルの項目（背景色・cellHeight）が適用される SHALL。テキスト色・フォント等のコンテンツ内装項目は builder の出力に影響しない SHALL。既存 DSL の style / cellHeight modifier チェーンからも同様に機能する SHALL。cellHeight の意味は既存の高さ解決契約に従う SHALL — `Theme.hasUnevenRows == true` のとき解決済み高さは最低高として働き内容に応じて伸び、`false` のときだけ内容の自然高にかかわらず固定される。

#### Scenario: hasUnevenRows が true なら cellHeight は最低高として働く

- **GIVEN** `hasUnevenRows == true` の Theme と、cellHeight modifier を適用した CustomCell
- **WHEN** content の自然高が指定値を超える行を表示する
- **THEN** 行高さは content に応じて指定値より伸びる（指定値は最低高）

#### Scenario: hasUnevenRows が false なら cellHeight で固定できる

- **GIVEN** `hasUnevenRows == false` の Theme と、cellHeight modifier を適用した CustomCell
- **WHEN** 行を表示する
- **THEN** 行高さが指定値に固定される

#### Scenario: テキスト系スタイルは content に影響しない

- **GIVEN** titleColor 等のテキスト系項目を含む CellStyle を指定した CustomCell
- **WHEN** 行を表示する
- **THEN** builder の出力の見た目は変化しない（背景色・cellHeight のみが効く）

### Requirement: 可視性フィルタへの参加

CustomCell は VisibilityAware に準拠し、`isVisible` が false のとき visible projection から除外される SHALL。

#### Scenario: isVisible=false で行が現れない

- **GIVEN** `isVisible = false` の CustomCell を含む root
- **WHEN** 表示する
- **THEN** その行は表示されず、他の Cell の並びが詰められる

### Requirement: 高さの自動追従

`Theme.hasUnevenRows == true`（可変高さ）の構成において、CustomCell の行高さは content の自然サイズに従い、content のサイズが実行時に変化した場合も行高さが追従する SHALL。利用者・ライブラリのどちらにも専用の再計測 API を要求しない SHALL。

#### Scenario: content の展開で行高さが追従する

- **GIVEN** タップで展開/折りたたみ状態を切り替える content を持つ CustomCell
- **WHEN** content 内の操作で展開状態に切り替える
- **THEN** 行高さが展開後の content サイズに追従し、後続の行の位置も更新される

### Requirement: DSL による配置

Android は `DSLSectionScope` の拡張関数（content あり / なしの 2 形）として CustomCell を直置きでき、戻り値の CellHandle から既存 modifier チェーンが機能する SHALL。iOS は SectionBuilder への CustomCell 値の直書きで配置できる SHALL。id 省略時の同一性は既存 DSL 規約（安定 ID 付与）に従う SHALL。icon modifier は CustomCell に適用できない SHALL（型として非対応）。

#### Scenario: Android DSL で直置きできる

- **GIVEN** Compose DSL で組む設定画面
- **WHEN** 拡張関数形式で content と builder を渡して CustomCell を配置する
- **THEN** CustomCell の行が表示され、CellHandle 経由の modifier（cellHeight 等）も機能する

#### Scenario: iOS DSL で直書きできる

- **GIVEN** SectionBuilder で組む設定画面
- **WHEN** CustomCell をイニシャライザ直書きで配置する
- **THEN** CustomCell の行が表示される
