## ADDED Requirements

### Requirement: 明示した Cell 色と外観 (iOS)

利用者が `CellStyle` または Cell 固有値 (`ButtonCell.titleColor`、選択系・入力系 Cell の `accentColor`、`EntryCell.placeholderColor`) として明示した `UIColor` は、ライブラリが別の値へ置き換えない (SHALL NOT)。固定色 (dynamic でない `UIColor`) は両外観でその色のまま描かれ、両外観の値を持つ `UIColor` (asset catalog の色や `UIColor(dynamicProvider:)`) はその色自身の現在の外観の値へ解決される (SHALL)。両外観で異なる色を使いたい利用者の手段は dynamic な `UIColor` を渡すことであり、Cell の差し替えは要らない (SHALL)。ライブラリは公開 API として Cell へ外観を渡す型やコールバックを持たない (SHALL NOT)。

`CellStyle` / Cell 固有値の `UIColor` を CGColor など外観を持たない表現へ変換して保持する箇所は、外観 (trait) の変更を受けて現在の外観で再解決する (SHALL)。表示中に外観が変わったとき、明示した dynamic 色と未指定の既定色はともに描き直され、固定色は変わらない (SHALL)。

#### Scenario: CellStyle の dynamic 色はダーク外観でその色の dark 値になる
- **GIVEN** 両外観の値を持つ `UIColor` を title 色に明示した `CellStyle` を持つ LabelCell
- **WHEN** ダーク外観で実効 style を解決し描画する
- **THEN** title は利用者の色の dark 側の値で描かれ、ライブラリの dark 既定の値にはならない

#### Scenario: CellStyle の固定色は外観で変わらない
- **GIVEN** 固定の `UIColor` を title 色に明示した `CellStyle` を持つ LabelCell をライト外観で表示中
- **WHEN** 外観をダークへ切り替える
- **THEN** title は明示した色のまま描かれ、同じ行の未指定の背景は dark 既定の値で描き直される

#### Scenario: Cell 固有の accent に渡した dynamic 色は trait 変更で再解決される
- **GIVEN** 両外観の値を持つ `UIColor` を `accentColor` に明示した CheckboxCell をライト外観で表示中
- **WHEN** 外観をダークへ切り替える
- **THEN** チェックボックスの塗りと枠は利用者の色の dark 側の値で描き直される

#### Scenario: SwiftUI DSL で外観に応じて選んだ CellStyle 色は差分として行に届く
- **GIVEN** `colorScheme` に応じて title 色を切り替える `CellStyle` を渡す SwiftUI DSL の LabelCell を表示中
- **WHEN** 外観を切り替えて再評価する
- **THEN** 差分は内容更新として発行され、表示中の行の title 色が新しい値になる
