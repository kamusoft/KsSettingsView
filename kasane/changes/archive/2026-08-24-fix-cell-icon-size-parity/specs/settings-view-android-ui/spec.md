# Delta: settings-view-android-ui (fix-cell-icon-size-parity)

## ADDED Requirements

### Requirement: Cell icon の正方形枠への実効 icon size の反映

Android の共通行レイアウトは、icon を持つ Cell の icon 領域を、解決済み icon size (`CellStyle.iconSize` → `Theme.cellIconSize` → 既定値の順) を一辺とする正方形枠として配置する SHALL。既定値は iOS の `Theme.defaultCellIconSize` と同じ生値とし、platform ごとに異なる既定を持たない SHALL。画像は枠を超えず、画像の実寸や縦横比によって枠の寸法を変えない SHALL。枠の寸法は bind のたびに実効値から再評価し、Theme の変更は表示中の行の枠へ反映される SHALL。icon を持たない Cell の配置 (icon 領域を残さず title を通常の開始位置へ置く) は変えない SHALL。

icon size の有効値は正の有限値とする SHALL。それ以外の値 (0・負値・非有限) は未指定として扱い、次の段 (CellStyle → Theme → 既定値) へ解決する SHALL (負値を LayoutParams の予約値として解釈しない)。

行幅が足りないとき、icon 領域と Cell 級アクセサリの幅は維持され、主行だけが縮む SHALL。

#### Scenario: Theme.cellIconSize が icon 枠に反映される
- **GIVEN** `cellIconSize` を指定した Theme で icon 付きの Cell が表示されている
- **WHEN** 行を bind する
- **THEN** icon 領域の幅と高さは Theme の `cellIconSize` に等しく、同じ値である

#### Scenario: CellStyle.iconSize は Theme より優先される
- **GIVEN** `cellIconSize` を指定した Theme と、それと異なる `iconSize` を持つ `CellStyle` の icon 付き Cell
- **WHEN** 行を bind する
- **THEN** icon 領域の幅と高さは `CellStyle.iconSize` に等しい

#### Scenario: 未指定なら iOS と同じ生値の既定枠になる
- **GIVEN** `cellIconSize` を持たない Theme と `iconSize` を持たない `CellStyle` の icon 付き Cell
- **WHEN** 行を bind する
- **THEN** icon 領域の幅と高さは既定の icon size に等しく、その生値は iOS の既定 (`Theme.defaultCellIconSize`) と同じである

#### Scenario: Theme 変更で表示中の行の枠が更新される
- **GIVEN** icon 付きの Cell が表示されている
- **WHEN** `cellIconSize` だけが異なる Theme へ切り替える
- **THEN** 表示中の行の icon 領域は新しい `cellIconSize` の正方形になる

#### Scenario: 非正方形画像でも枠は正方形のまま
- **GIVEN** 縦横比が 1:1 でない drawable を icon に持つ Cell
- **WHEN** 行を bind する
- **THEN** icon 領域の幅と高さは解決済み icon size に等しい正方形で、画像は枠を超えない

#### Scenario: icon のない Cell の配置は変わらない
- **GIVEN** icon を持たない Cell
- **WHEN** 行を bind する
- **THEN** icon 領域は表示されず、title は icon 領域の余白を伴わない通常の開始位置に置かれる

#### Scenario: 無効な icon size は未指定として次の段へ解決する
- **GIVEN** `CellStyle.iconSize` に 0 以下または非有限の値を指定し、Theme には正の `cellIconSize` を指定した icon 付き Cell
- **WHEN** 行を bind する
- **THEN** icon 領域の幅と高さは Theme の `cellIconSize` に等しい

#### Scenario: 狭幅でも icon 枠は縮まない
- **GIVEN** 大きめの `cellIconSize` と長い title を持つ icon 付き Cell
- **WHEN** 行幅を自然幅の合計より狭くしてレイアウトする
- **THEN** icon 領域の幅と高さは解決済み icon size に等しいまま、title が末尾省略される

### Requirement: Cell icon の正方形枠に対する角丸

Android の共通行レイアウトは、解決済み icon radius (`CellStyle.iconRadius` → `Theme.cellIconRadius` → 既定値 = 角丸なし。iOS の `Theme.defaultCellIconRadius` と同じ生値) が正のとき、icon 領域の**正方形枠**に対して角丸で clip する SHALL (core/ADR-0025)。角丸は画像の描画矩形には追従しない SHALL。解決済み icon radius が角丸なしのとき、clip を行わない SHALL。同じ行を別の radius で再 bind したとき、前回の clip 状態を残さず新しい値で再評価する SHALL。icon radius の有効値は 0 以上の有限値とし、それ以外 (負値・非有限) は未指定として次の段へ解決する SHALL。解決済み radius が枠の半辺を超える場合の描画は platform の描画系に委ね、clamp しない SHALL。

#### Scenario: Theme.cellIconRadius で枠が角丸に clip される
- **GIVEN** 正の `cellIconRadius` を指定した Theme で正方形画像の icon 付き Cell が表示されている
- **WHEN** 行を bind する
- **THEN** icon 領域は枠の四隅を `cellIconRadius` で丸めた形に clip される

#### Scenario: CellStyle.iconRadius は Theme より優先される
- **GIVEN** 正の `cellIconRadius` を指定した Theme と、それと異なる正の `iconRadius` を持つ `CellStyle` の icon 付き Cell
- **WHEN** 行を bind する
- **THEN** icon 領域の角丸は `CellStyle.iconRadius` になる

#### Scenario: 角丸未指定なら clip しない
- **GIVEN** `cellIconRadius` を持たない Theme と `iconRadius` を持たない `CellStyle` の icon 付き Cell
- **WHEN** 行を bind する
- **THEN** icon 領域は角丸で clip されない

#### Scenario: 角丸は枠に対してかかり画像の描画矩形には追従しない
- **GIVEN** 正の `cellIconRadius` を指定した Theme で、縦横比が 1:1 でない drawable を icon に持つ Cell
- **WHEN** 行を bind する
- **THEN** 角丸の clip 形状は icon 領域の正方形枠に対して決まり、画像の描画矩形の寸法には依存しない

#### Scenario: 再 bind で radius の変更と解除が反映される
- **GIVEN** 正の radius で bind 済みの行
- **WHEN** 同じ行を別の正の radius で再 bind し、続けて角丸なしで再 bind する
- **THEN** 1 回目の再 bind では角丸が新しい値になり、2 回目では clip が解除される

#### Scenario: 無効な radius は未指定として次の段へ解決する
- **GIVEN** `CellStyle.iconRadius` に負値または非有限の値を指定し、Theme には正の `cellIconRadius` を指定した icon 付き Cell
- **WHEN** 行を bind する
- **THEN** icon 領域の角丸は Theme の `cellIconRadius` になる

### Requirement: 主行の幅配分は title を守り valueText を省略する (iOS と同一契約)

Android の共通行レイアウトは、主行 (title と行内 trailing) の幅が足りないとき、title のコンテンツ幅を確保し (主行幅を上限とし、超える分だけ末尾省略)、valueText に主行の残り幅を与えて収まらない分を末尾省略する SHALL (残り幅が 0 なら valueText は表示されない)。icon 領域と Cell 級アクセサリの幅は主行より先に譲らない SHALL。行内 trailing がない Cell では title が主行の全幅を使える SHALL (`ButtonCell` の中央揃えが依存する)。EntryCell は title がコンテンツ幅を維持し入力フィールドが残り幅を占める SHALL (従来どおり)。title と行内 trailing の最小クリアランスは変えない SHALL (core/ADR-0026。android/ADR-0002 の既定配分の項目を置き換える)。

#### Scenario: 長い valueText は省略され title は全文残る
- **GIVEN** 短い title と主行幅を超える長さの valueText と icon と Cell 級アクセサリを持つ Cell
- **WHEN** 行を bind してレイアウトする
- **THEN** title は全文表示され、valueText は残り幅で末尾省略され、icon 領域と Cell 級アクセサリの幅は縮まず、行からはみ出す要素がない

#### Scenario: 主行幅を超える title は上限で省略され valueText は残り幅になる
- **GIVEN** 主行幅を超える長さの title と短い valueText を持つ Cell
- **WHEN** 行を bind してレイアウトする
- **THEN** title は主行幅を上限に末尾省略され、valueText の幅は残り幅 (0 以上) で、行からはみ出す要素がない

#### Scenario: 行内 trailing がない Cell では title が主行の全幅を使う
- **GIVEN** valueText を持たない Cell (ButtonCell の中央揃えを含む)
- **WHEN** 行を bind してレイアウトする
- **THEN** title の領域は主行の全幅に等しく、ButtonCell の中央揃えは従来どおり成立する

#### Scenario: 同じ行で valueText の有無が切り替わっても配分が追随する
- **GIVEN** valueText を持つ Cell として bind 済みの行
- **WHEN** 同じ行を valueText のない Cell として再 bind し、続けて valueText のある Cell として再 bind する
- **THEN** 1 回目の再 bind では title が主行の全幅を使い、2 回目では title がコンテンツ幅・valueText が残り幅になる

#### Scenario: EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む
- **GIVEN** 長い title を持つ EntryCell
- **WHEN** 行を bind してレイアウトする
- **THEN** title はコンテンツ幅で表示され、入力フィールドの表示幅は残り幅 (0 以上) になる
