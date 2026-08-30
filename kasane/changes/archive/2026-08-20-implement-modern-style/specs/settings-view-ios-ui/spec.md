# Delta: settings-view-ios-ui (implement-modern-style)

## ADDED Requirements

### Requirement: Theme の Section 装飾4属性

`Theme` は `sectionMargin: NSDirectionalEdgeInsets?`・`sectionCornerRadius: CGFloat?`・`sectionBorderWidth: CGFloat?`・`sectionBorderColor: UIColor?` を公開する (SHALL)。nil は未指定を表し、style 別の platform 既定へ解決する (SHALL): Modern はライブラリ所有の既定寸法 (承認モックが正、確定値は design.md Decision 6)、Classic は margin 上下 0。borderWidth 未指定の実効値は 0、borderColor 未指定の実効値は透明とし、既定の Modern にボーダーは描かれない (SHALL)。4属性は `Theme` の値等価性に参加する (SHALL)。

`sectionMargin` は Section 単位 (Header・Cell 箱・Footer を一体とした表示単位) の**外側**余白であり (SHALL)、水平成分は leading / trailing 基準で解釈する (SHALL)。隣接 Section 間の間隔は前 Section の bottom と次 Section の top の加算とし、先頭 Section の top・末尾 Section の bottom は list 端に対しても適用する (SHALL)。負の寸法成分は 0 として扱い、`sectionCornerRadius` は箱の寸法から幾何的に許される値へ描画時に clamp する (SHALL — Theme 構築時には拒否しない)。Modern は新たな色既定を導入しない (SHALL NOT): 箱と下地の色は既存の `cellBackgroundColor` / `backgroundColor` から解決し、箱の視認性は両者の対比に依存する。

#### Scenario: 未指定の Theme で Modern を表示する
- **GIVEN** 4属性を指定しない `Theme` と `style = .modern` の SettingsView
- **WHEN** 表示する
- **THEN** Section はライブラリ既定の余白・角丸で箱として描画され、ボーダーは描かれない

#### Scenario: 指定値が箱の描画へ反映される
- **GIVEN** 4属性すべてを明示した `Theme` と `style = .modern` の SettingsView
- **WHEN** 表示する
- **THEN** 箱の余白・角丸半径・ボーダー幅・ボーダー色は指定値で描画される

#### Scenario: 実行時の Theme 変更が装飾へ反映される
- **GIVEN** Modern で表示中の SettingsView
- **WHEN** `sectionCornerRadius` だけが異なる Theme を適用する
- **THEN** 表示中の Section の箱が新しい角丸半径で再描画され、Section / Cell の identity は維持される

#### Scenario: sectionMargin は Header / Footer を含む Section 単位を包む
- **GIVEN** header text と footer text を持つ Section、上下に正の値を持つ `sectionMargin`、`style = .modern`
- **WHEN** 表示する
- **THEN** top 余白は Header の上・bottom 余白は Footer の下に入り、Header と箱の間・箱と Footer の間には入らない

#### Scenario: 負の成分は 0 として扱う
- **GIVEN** 負の成分を含む `sectionMargin` と負の `sectionBorderWidth` を持つ Theme と `style = .modern`
- **WHEN** 表示する
- **THEN** 負の成分は 0 と同じ描画結果になり、例外や不正 geometry を生じない

### Requirement: Modern の Section 箱描画

`style = .modern` のとき、各 Section の **Cell 行の範囲のみ**を覆う箱 (角丸背景およびボーダー) を描画する (SHALL)。箱の背景色は `Theme.cellBackgroundColor` から解決する (SHALL)。Section Header / Footer および Root Header / Footer は箱に含めてはならない (SHALL NOT)。`.insetGrouped` list appearance は使用しない (SHALL NOT — ios/ADR-0003)。

#### Scenario: Header / Footer は箱の外に置かれる
- **GIVEN** header text と footer text を持つ Section と `style = .modern`
- **WHEN** 表示する
- **THEN** 箱は Section の先頭 Cell から末尾 Cell までを覆い、Header は箱の上外側・Footer は箱の下外側に表示される

#### Scenario: 構造変更後も箱が Cell 範囲に追従する
- **GIVEN** Modern で表示中の Section
- **WHEN** `SettingsRootDiff` で Cell を末尾に挿入する
- **THEN** 箱は挿入後の末尾 Cell までを覆う

#### Scenario: 可視 Cell が0件の Section は箱を生成しない
- **GIVEN** header text を持ち可視の Cell が存在しない Section と `style = .modern`
- **WHEN** 表示する
- **THEN** 箱と separator は生成されず、Header は既存の表示規則どおり表示され、`sectionMargin` は Section 単位に適用される

### Requirement: 箱と Cell 背景の合成

Modern の箱の描画は次の合成契約に従う (SHALL): ボーダーは Cell 背景・押下/ 選択背景より前面に描画され、隠されない。Section 先頭 / 末尾 Cell の背景 (`CellStyle.backgroundColor` を含む) と押下 / 選択背景は箱の角丸形状で clip され、角の外へはみ出さない。`CellStyle.backgroundColor` は当該 Cell の行領域を箱背景より前面で塗る。

#### Scenario: ボーダーが Cell 背景に隠れない
- **GIVEN** 正の `sectionBorderWidth` と、`CellStyle.backgroundColor` を持つ Cell を含む Section と `style = .modern`
- **WHEN** 表示する
- **THEN** ボーダーは全周にわたり視認できる

#### Scenario: 先頭 Cell の背景が角丸からはみ出さない
- **GIVEN** Section 先頭の Cell に `CellStyle.backgroundColor` を指定し `style = .modern`
- **WHEN** 表示する
- **THEN** 背景色は箱の角丸の外側に描かれない

#### Scenario: 押下背景も箱形状に収まる
- **GIVEN** Section 先頭の押下可能な Cell と `style = .modern`
- **WHEN** Cell を押下する
- **THEN** `selectedColor` の押下背景は箱の角丸の内側に収まる

### Requirement: Modern の separator 規則

`style = .modern` のとき、Section 先頭 Cell の上端と末尾 Cell の下端に separator を描いてはならない (SHALL NOT — 箱の縁が区切りを兼ねる)。Section 内の中間 separator は描画し (SHALL)、leading 側は Classic の中間 separator と同じ inset 規則を箱の内側 leading 端を基準に適用し、trailing 側にも同量の inset を箱の内側 trailing 端から取る (SHALL — 箱の両端まで引かず、箱が分断されて見えないようにする。Classic の「trailing は端まで」とは意図的に異なる)。色は `Theme.separatorColor` から解決し、icon の有無で inset を変えない (SHALL)。

#### Scenario: 箱の上下端に separator が出ない
- **GIVEN** 複数 Cell を持つ Section と `style = .modern`
- **WHEN** 表示する
- **THEN** separator は Cell 間にのみ描画され、箱の上端・下端には描画されない

#### Scenario: 単一 Cell の Section に separator が出ない
- **GIVEN** Cell が1つだけの Section と `style = .modern`
- **WHEN** 表示する
- **THEN** その Section に separator は描画されない

### Requirement: Classic への sectionMargin 上下適用

`style = .classic` のとき、`Theme.sectionMargin` の上下成分を Section 単位の外側余白として適用する (SHALL)。leading / trailing 成分は無視する (SHALL — 「Classic の Section 境界は全幅」契約の維持)。未指定時の実効値は上下 0 とし、従来の Classic の表示と一致する (SHALL)。余白領域には `Theme.backgroundColor` が見える (SHALL)。

#### Scenario: 未指定なら Classic の外観は従来と一致する
- **GIVEN** `sectionMargin` を指定しない Theme と `style = .classic`
- **WHEN** 表示する
- **THEN** Section 間に追加の余白はなく、従来の Classic の表示と一致する

#### Scenario: 上下成分だけが効く
- **GIVEN** 上下・左右すべてに正の値を持つ `sectionMargin` と `style = .classic`
- **WHEN** 表示する
- **THEN** Section の前後に上下成分の余白が入り、行と separator の水平方向は全幅のまま変わらない

### Requirement: style 切替の整合

`style` を実行時に切り替えたとき、`SettingsRoot`・Section / Cell ID・Cell 内容を変更せず、切替後の style の装飾・separator 規則で全 Section を再描画する (SHALL)。

#### Scenario: Classic から Modern への切替
- **GIVEN** Classic で表示中の SettingsView
- **WHEN** `style = .modern` に変更する
- **THEN** 同じ設定内容が箱型の装飾で表示され、Cell の内容と順序は変わらない
