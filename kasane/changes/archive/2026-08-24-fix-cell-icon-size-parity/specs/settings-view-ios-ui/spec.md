# Delta: settings-view-ios-ui (fix-cell-icon-size-parity)

## ADDED Requirements

### Requirement: Cell icon 枠の寸法が画像の intrinsic size に依存しない

iOS の共通行レイアウトは、icon を持つ Cell の icon 領域を、解決済み icon size (`CellStyle.iconSize` → `Theme.cellIconSize` → 既定値の順。既定値は Android と同じ生値) を一辺とする正方形枠として確保する SHALL。枠の幅と高さは画像の intrinsic size (SF Symbols の字形差・任意寸法の `UIImage`) に関わらず解決済み icon size に等しく、画像は枠を超えない SHALL。解決済み icon size が同じ icon 付き Cell は、行をまたいで同じ icon 領域幅と title の開始位置を持つ SHALL。枠の寸法は bind のたびに実効値から再評価し、`applyTheme(_:)` による Theme の変更は表示中の行の枠へ反映される SHALL。

icon を持たない Cell では icon 領域を非表示にし、そのとき icon 領域の寸法を固定する制約は無効化されている SHALL (非表示の制約と衝突しない)。再び icon を持つ Cell として bind されたとき、制約は再び有効化され枠が戻る SHALL。

icon size の有効値は正の有限値とする SHALL。それ以外の値 (0・負値・非有限) は未指定として扱い、次の段 (CellStyle → Theme → 既定値) へ解決する SHALL。

#### Scenario: intrinsic 幅が異なる SF Symbols でも icon 列幅が揃う
- **GIVEN** 同じ Theme 下に、intrinsic 幅の異なる SF Symbols を `KsImage.systemName` で指定し、`CellStyle.iconSize` が同一または未指定の複数の Cell が並んでいる
- **WHEN** 行がレイアウトされる
- **THEN** すべての行で icon 領域の幅と高さは解決済み icon size に等しく、title の開始位置が行間で一致する

#### Scenario: 枠より大きい intrinsic size の画像でも枠は解決済みサイズのまま
- **GIVEN** 解決済み icon size より幅も高さも大きい intrinsic size の `UIImage` を icon に持つ Cell
- **WHEN** 行がレイアウトされる
- **THEN** icon 領域の幅と高さは解決済み icon size に等しく、画像は枠を超えない

#### Scenario: CellStyle.iconSize は Theme より優先される
- **GIVEN** `cellIconSize` を指定した Theme と、それと異なる `iconSize` を持つ `CellStyle` の icon 付き Cell
- **WHEN** 行がレイアウトされる
- **THEN** icon 領域の幅と高さは `CellStyle.iconSize` に等しい

#### Scenario: Theme 変更で表示中の行の枠が更新される
- **GIVEN** icon 付きの Cell が `KsSettingsViewController` で表示されている
- **WHEN** `cellIconSize` だけが異なる Theme を `applyTheme(_:)` で適用する
- **THEN** 表示中の行の icon 領域は新しい `cellIconSize` の正方形になる

#### Scenario: icon のない Cell では枠の制約が無効化される
- **GIVEN** icon を持たない Cell
- **WHEN** 行がレイアウトされる
- **THEN** icon 領域は非表示で、その寸法を固定する制約は無効 (`isActive == false`) であり、title は icon 領域の余白を伴わない通常の開始位置に置かれる

#### Scenario: icon なし → icon ありの再 bind で枠が戻る
- **GIVEN** icon を持たない Cell として bind された行
- **WHEN** 同じ行を icon 付きの Cell として再 bind する (リサイクルを含む)
- **THEN** icon 領域は表示され、寸法を固定する制約は有効で、幅と高さは解決済み icon size に等しい

#### Scenario: 無効な icon size は未指定として次の段へ解決する
- **GIVEN** `CellStyle.iconSize` に 0 以下または非有限の値を指定し、Theme には正の `cellIconSize` を指定した icon 付き Cell
- **WHEN** 行がレイアウトされる
- **THEN** icon 領域の幅と高さは Theme の `cellIconSize` に等しい

### Requirement: Cell icon の正方形枠に対する角丸

iOS の共通行レイアウトは、解決済み icon radius (`CellStyle.iconRadius` → `Theme.cellIconRadius` → 既定値 = 角丸なし。Android と同じ生値) が正のとき、icon 領域の**正方形枠**に対して角丸で clip する SHALL (core/ADR-0025)。角丸は画像の描画矩形には追従しない SHALL。icon radius の有効値は 0 以上の有限値とし、それ以外 (負値・非有限) は未指定として次の段へ解決する SHALL。解決済み radius が枠の半辺を超える場合の描画は platform の描画系に委ね、clamp しない SHALL。

#### Scenario: 角丸は枠に対してかかり画像の描画矩形には追従しない
- **GIVEN** 正の `cellIconRadius` を指定した Theme で、縦横比が 1:1 でない `UIImage` を icon に持つ Cell
- **WHEN** 行がレイアウトされる
- **THEN** 角丸の clip 形状は icon 領域の正方形枠に対して決まり、画像の描画矩形の寸法には依存しない

#### Scenario: 角丸未指定なら clip しない
- **GIVEN** `cellIconRadius` を持たない Theme と `iconRadius` を持たない `CellStyle` の icon 付き Cell
- **WHEN** 行がレイアウトされる
- **THEN** icon 領域は角丸で clip されない

#### Scenario: 無効な radius は未指定として次の段へ解決する
- **GIVEN** `CellStyle.iconRadius` に負値または非有限の値を指定し、Theme には正の `cellIconRadius` を指定した icon 付き Cell
- **WHEN** 行がレイアウトされる
- **THEN** icon 領域の角丸は Theme の `cellIconRadius` になる

### Requirement: 主行の幅配分は title を守り valueText を省略する (Android と同一契約)

iOS の共通行レイアウトは、主行 (title と行内 trailing) の幅が足りないとき、title のコンテンツ幅を確保し (主行幅を上限とし、超える分だけ末尾省略)、valueText に主行の残り幅を与えて収まらない分を末尾省略する SHALL (残り幅が 0 なら valueText は表示されない)。icon 領域と Cell 級アクセサリの幅は主行より先に譲らない SHALL。行内 trailing がない Cell では title が主行の全幅を使える SHALL。EntryCell は title がコンテンツ幅を維持し入力フィールドが残り幅を占める SHALL (core/ADR-0026。iOS は現行の優先度のままこの契約を満たしており、本 Requirement はそれをテストで固定する)。

#### Scenario: 長い valueText は省略され title は全文残る
- **GIVEN** 短い title と主行幅を超える長さの valueText と icon と Cell 級アクセサリを持つ Cell
- **WHEN** 行がレイアウトされる
- **THEN** title は全文表示され、valueText は残り幅で末尾省略され、icon 領域と Cell 級アクセサリの幅は縮まず、行からはみ出す要素がない

#### Scenario: 主行幅を超える title は上限で省略され valueText は残り幅になる
- **GIVEN** 主行幅を超える長さの title と短い valueText を持つ Cell
- **WHEN** 行がレイアウトされる
- **THEN** title は主行幅を上限に末尾省略され、valueText の幅は残り幅 (0 以上) で、行からはみ出す要素がない

#### Scenario: 行内 trailing がない Cell では title が主行の全幅を使う
- **GIVEN** valueText を持たない Cell
- **WHEN** 行がレイアウトされる
- **THEN** title の領域は主行の全幅に等しい

#### Scenario: EntryCell では title がコンテンツ幅を維持し入力フィールドが縮む
- **GIVEN** 長い title を持つ EntryCell
- **WHEN** 行がレイアウトされる
- **THEN** title はコンテンツ幅で表示され、入力フィールドの表示幅は残り幅 (0 以上) になる

#### Scenario: 狭幅でも icon 枠は縮まない
- **GIVEN** 大きめの `cellIconSize` と長い title を持つ icon 付き Cell
- **WHEN** 行幅を自然幅の合計より狭くしてレイアウトする
- **THEN** icon 領域の幅と高さは解決済み icon size に等しいまま、title が末尾省略される
