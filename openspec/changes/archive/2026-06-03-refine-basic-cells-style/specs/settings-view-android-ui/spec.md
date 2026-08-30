## MODIFIED Requirements

### Requirement: Theme / CellStyle の Android 変換

`Theme` および `CellStyle` の論理スタイルを `@ColorInt`（Android `Color` Int 表現）および `Typeface` に変換するユーティリティが提供されなければならない (SHALL)。

実効スタイル合成では、`CellStyle` の各フィールド（`titleColor` / `titleFont` / `descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `iconSize` / `iconRadius` / `cellHeight` / `hintTextColor` / `hintTextFont` / `backgroundColor` / `accentColor`）が `null` のとき、対応する `Theme` フィールドで補完しなければならない (MUST)。`CellStyle.backgroundColor` 未指定時は `Theme.cellBackgroundColor`、`CellStyle.accentColor` 未指定時は `Theme.cellAccentColor` を採用する。`RecyclerView` 自体の背景色は `Theme.viewBackgroundColor` を採用しなければならない (MUST)。

タイトル色／フォントの合成は次の 3 段階優先順位でなければならない (MUST)：

1. `CellStyle.titleColor` が `null` でなければそれを `@ColorInt` に変換して採用
2. それ以外で `Theme.titleColor` が `null` でなければそれを `@ColorInt` に変換して採用
3. それ以外は `TextView` の既定色（`android.R.attr.textColorPrimary` 相当）にフォールバック

`titleFont` も同様に `CellStyle.titleFont` → `Theme.titleFont` → `TextView` 既定フォントの順序で解決する。

EffectiveStyle は「タイトル色が明示由来か（CellStyle または Theme のいずれかから指定されたか）プラットフォーム fallback 由来か」を判定できる Bool フラグ（例: `titleColorIsExplicit`）を提供しなければならない (MUST)。このフラグは ButtonCell の `baseColor` 解決で使用される。

#### Scenario: KsColor から ColorInt

- **GIVEN** `KsColor(red = 1.0, green = 0.5, blue = 0.0, alpha = 1.0)`
- **WHEN** `KsColor.toColorInt()` 拡張関数を呼ぶ
- **THEN** ARGB Int として `0xFFFF8000` が返る

#### Scenario: 実効スタイルの合成（Theme.titleColor 採用）

- **GIVEN** Cell の `CellStyle.titleColor = null`、`Theme.titleColor = KsColor(0.2, 0.4, 0.6, 1.0)`
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `effective.titleColor` は ARGB Int `0xFF335999` 相当となり、`effective.titleColorIsExplicit == true` となる

#### Scenario: 実効スタイルの合成（プラットフォーム fallback）

- **GIVEN** Cell の `CellStyle.titleColor = null`、`Theme.titleColor = null`
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `effective.titleColor` は `TextView` 既定色相当（`android.R.attr.textColorPrimary`）、`effective.titleColorIsExplicit == false` となる

#### Scenario: 実効スタイルの合成（CellStyle 優先）

- **GIVEN** `CellStyle.titleColor = KsColor.red`、`Theme.titleColor = KsColor.blue`
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `effective.titleColor` は赤、`effective.titleColorIsExplicit == true` となる

#### Scenario: CellStyle.backgroundColor の合成

- **GIVEN** `Theme(cellBackgroundColor = KsColor.white)` と `CellStyle(backgroundColor = KsColor.yellow)` の Cell
- **WHEN** 実効スタイルを計算する
- **THEN** 当該 Cell の Cell コンテナ（`RippleDrawable` の content layer）の背景色は黄色（`CellStyle.backgroundColor` 優先）になる

#### Scenario: CellStyle.accentColor の合成（SwitchCell）

- **GIVEN** `Theme(cellAccentColor = KsColor.blue)` と `CellStyle(accentColor = KsColor.green)` の SwitchCell（`isOn = true`）
- **WHEN** ViewHolder が bind する
- **THEN** `MaterialSwitch.thumbTintList` / `trackTintList` が緑（`CellStyle.accentColor` 優先）に設定される

#### Scenario: viewBackgroundColor の反映

- **GIVEN** `Theme(viewBackgroundColor = KsColor(0.95, 0.93, 0.90, 1.0))` で初期化された `KsSettingsView`
- **WHEN** Android で表示される
- **THEN** `RecyclerView` の `setBackgroundColor` に当該色（ARGB Int に変換した値）が設定される

#### Scenario: valueTextColor / valueTextFont の合成

- **GIVEN** `Theme(descriptionColor = KsColor.gray)` と `CellStyle(valueTextColor = KsColor.darkGray)` の LabelCell（`valueText = "オン"`）
- **WHEN** 実効スタイルを計算して LabelCellViewHolder が描画する
- **THEN** `valueTextView.setTextColor(darkGray)` が呼ばれる（`CellStyle.valueTextColor` 優先）

#### Scenario: CellStyle.backgroundColor 適用と罫線描画の両立

- **GIVEN** `CellStyle(backgroundColor = KsColor.yellow)` を持つ Cell が `ClassicSectionDecoration`（罫線描画用 `RecyclerView.ItemDecoration`）配下に表示される
- **WHEN** Cell の `applyCellBackground` で content layer の `ColorDrawable` を黄色に設定する
- **THEN** Cell 間の罫線が `setBackgroundColor` 相当の操作で消失しない。`ItemDecoration` は `onDrawOver` で罫線を描画し、children 描画後に重畳されるため、Cell コンテナの背景色変更によって罫線が上書きされてはならない (MUST NOT)

## ADDED Requirements

### Requirement: 行高さ（RowHeight / HasUnevenRows）の適用

各 `CellViewHolder` は bind 時に、`Theme.rowHeight` / `Theme.hasUnevenRows` / `CellStyle.cellHeight` を合成した実効高さを Cell コンテナに適用しなければならない (MUST)。

実効高さ算出は以下の通り：

- `effectiveBase = CellStyle.cellHeight ?? Theme.rowHeight`（どちらも `-1`／未指定なら `-1`）
- `effectiveHeightDp = max(effectiveBase, MinRowHeight)`（`MinRowHeight = 44dp`）
- `effectiveHeightPx = (effectiveHeightDp * Resources.displayMetrics.density).toInt()`

適用方法：

- `Theme.hasUnevenRows == false` のとき: `container.layoutParams.height = effectiveHeightPx` で **固定高さ** を適用しなければならない (MUST)。すべての Cell が同じ高さに揃う（個別 `CellStyle.cellHeight` が指定された Cell はその Cell 単位で固定高さが上書きされる）。
- `Theme.hasUnevenRows == true` のとき: `container.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT` かつ `container.minimumHeight = effectiveHeightPx` で **最低高さ保証付きの可変高さ** を適用しなければならない (MUST)。長文 Description などで自然に伸縮する。

bind 時の高さ更新は前回値と異なる場合のみ `requestLayout()` を呼んで再レイアウトをトリガーしなければならない (MUST)。

#### Scenario: 固定高さ（HasUnevenRows = false）

- **GIVEN** `Theme(rowHeight = 60, hasUnevenRows = false)` で初期化された `KsSettingsView`、画面密度 2.0、複数 Cell が並ぶ
- **WHEN** Android で表示される
- **THEN** すべての Cell コンテナの `layoutParams.height` が `120 px`（60 dp × 2.0）に設定される

#### Scenario: 可変高さ（HasUnevenRows = true）

- **GIVEN** `Theme(rowHeight = -1, hasUnevenRows = true)` で初期化された `KsSettingsView`、画面密度 2.0、長文 Description を持つ Cell と単行 Cell が混在
- **WHEN** Android で表示される
- **THEN** 各 Cell コンテナの `layoutParams.height` が `WRAP_CONTENT`、`minimumHeight` が `88 px`（44 dp × 2.0）に設定される。長文 Cell は 88 px より高くなり、単行 Cell は 88 px 固定相当

#### Scenario: CellStyle.cellHeight の優先

- **GIVEN** `Theme(rowHeight = 44, hasUnevenRows = false)` と `CellStyle(cellHeight = 80)` を持つ特定 CommandCell、画面密度 2.0
- **WHEN** Android で表示される
- **THEN** 当該 Cell の `layoutParams.height` は `160 px`（80 dp × 2.0、`CellStyle.cellHeight` 優先）。他 Cell は `88 px`（max(44, 44=MinRowHeight) × 2.0）

### Requirement: タッチフィードバック（selectedColor の反映）

各 `CellViewHolder` は bind 時に `applyCellBackground(container, effective)` を呼び、`RippleDrawable` で `effective.selectedColor`（`Theme.selectedColor`）を ripple 色、`effective.backgroundColor`（`CellStyle.backgroundColor ?? Theme.cellBackgroundColor`）を content 背景色として設定しなければならない (MUST)。`container.isClickable = true` を設定し、`isEnabled = false` を除いて常に ripple を発生可能にしなければならない (MUST)。

本 Requirement は既存実装（`applyCellBackground` ヘルパ）で満たされているが、`Theme.selectedColor` および `CellStyle.backgroundColor` の合成経路に変更が入るため、本変更提案で改めて Requirement として確認する。

#### Scenario: タップ中の Ripple 発生

- **GIVEN** `Theme(cellBackgroundColor = KsColor.white, selectedColor = KsColor(1.0, 0.75, 0.0, 0.3))` の LabelCell が表示されている
- **WHEN** ユーザーが Cell をタップする
- **THEN** Cell の背景に Ripple エフェクトが `selectedColor` で発生し、リリース後に元の `cellBackgroundColor` に戻る

### Requirement: isEnabled 描画の反映

各 `CellViewHolder` は bind 時に `cell.isEnabled == false` の場合、以下を適用しなければならない (MUST)：

- container の `isClickable = false` / `setOnClickListener(null)`（タップを無効化）。
- コントロール要素（`MaterialSwitch` / `MaterialCheckBox` / `KsSimpleCheckView` / Button 風 `TextView` 等）の `isEnabled = false`。
- タイトル／説明文／値テキスト／ヒントテキストの `setTextColor` に `Theme.disabledTextColor.toColorInt()` を適用。
- Cell 全体への `alpha` 適用や半透明化は行わない (MUST NOT)。

`cell.isEnabled == true`（既定）のときは、通常の bind ロジックを適用する。

#### Scenario: SwitchCell isEnabled = false の描画

- **GIVEN** `Theme(disabledTextColor = KsColor.lightGray)` と `SwitchCell(title = "通知", isOn = true, isEnabled = false)`
- **WHEN** Android で描画される
- **THEN** `MaterialSwitch.isEnabled = false`（標準の disabled 表示）になり、`titleView.setTextColor(lightGray)` が呼ばれ、`container.setOnClickListener(null)` でタップ無効化される

#### Scenario: LabelCell isEnabled = false の描画

- **GIVEN** `LabelCell(title = "通知", description = "詳細", valueText = "オン", isEnabled = false)` と `Theme.disabledTextColor = KsColor.lightGray`
- **WHEN** Android で描画される
- **THEN** title / description / valueText の TextView すべてが lightGray で表示される

#### Scenario: CheckboxCell isEnabled = false で Ripple 無効

- **GIVEN** `CheckboxCell(title = "規約", isChecked = false, isEnabled = false)`
- **WHEN** ユーザーが Cell をタップする
- **THEN** `MaterialCheckBox.isEnabled = false` でチェック状態は変化せず、container の Ripple も発生しない（`isClickable = false` のため state_pressed が走らない）

### Requirement: ButtonCell の baseColor 解決順序

`ButtonCellViewHolder` はボタンテキストの基準色（disabled 適用前の色）を次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor` が指定されていれば `effective.titleColor` を採用
3. それ以外で `Theme.titleColor` が指定されていれば `effective.titleColor` を採用
4. それ以外は `MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)` 相当（Material primary 色）

「2 または 3 のいずれか」の判定は EffectiveStyle の `titleColorIsExplicit`（または同等のフラグ）で行ってよい。`cell.isEnabled == false` のときは、上記で解決した基準色ではなく `effective.disabledTextColor` を用いて `titleView.setTextColor(...)` を呼ばなければならない (MUST)。

#### Scenario: ButtonCell.titleColor 指定時

- **GIVEN** `ButtonCell(title = "削除", titleColor = KsColor.red)`、`CellStyle.titleColor = null`、`Theme.titleColor = KsColor.green`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(Color.RED)` 相当が呼ばれる（Cell 個別 `titleColor` 優先）

#### Scenario: CellStyle.titleColor 指定時

- **GIVEN** `ButtonCell(title = "次へ", titleColor = null)`、`CellStyle.titleColor = KsColor.purple`、`Theme.titleColor = KsColor.green`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(purple)`（CellStyle 経由）が呼ばれる

#### Scenario: Theme.titleColor 指定時

- **GIVEN** `ButtonCell(title = "登録", titleColor = null)`、`CellStyle.titleColor = null`、`Theme.titleColor = KsColor(0.8, 0.6, 0.0, 1.0)`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(...)` が `Theme.titleColor` の ARGB Int で呼ばれる（4 段階目の Material primary ではない）

#### Scenario: 全段階未指定時の Material primary

- **GIVEN** `ButtonCell(title = "OK", titleColor = null)`、`CellStyle.titleColor = null`、`Theme.titleColor = null`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(...)` が Material `colorPrimary` 属性から解決された色で呼ばれる

#### Scenario: isEnabled = false 時の disabledTextColor 適用

- **GIVEN** `ButtonCell(title = "削除", titleColor = KsColor.red, isEnabled = false)`、`Theme.disabledTextColor = KsColor.lightGray`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(lightGray)`（disabledTextColor）が呼ばれる（baseColor の `.red` は使われない）
