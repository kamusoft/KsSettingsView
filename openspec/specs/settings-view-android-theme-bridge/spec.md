# settings-view-android-theme-bridge Specification

## Purpose

`settings-view-android-theme-bridge` は、`KsSettingsViewCore` の `Theme` / `CellStyle` / `KsColor` / `KsImage` などの論理スタイル値を Android プラットフォーム値（`@ColorInt` / `Typeface` / `Drawable` 等）に変換する **テーマ変換ブリッジ層** を担う capability である。各 Cell ViewHolder が描画時に参照する実効スタイルの合成（CellStyle → Theme → プラットフォーム fallback の3段階）、タッチフィードバック（`selectedColor` の Ripple 適用）、`isEnabled = false` 時の描画変換、ButtonCell の `baseColor` 解決順序、`KsImage` 派生ごとの解決ロジックを定義する。`settings-view-android-host`（ホスト層）と `settings-view-android-style`（スタイル・レイアウト層）はいずれも本 capability の変換結果を消費する立場であり、Theme 値の変換ロジック自体を含まない。

## Requirements
### Requirement: Theme / CellStyle の Android 変換

`Theme` および `CellStyle` の各フィールドは Compose `Color` / `TextStyle` を直接保持するため、**`KsColor` / `KsFont` からの `@ColorInt` / `Typeface` 変換ユーティリティは存在しない (MUST NOT 存在)**。本 Requirement の責務は「**実効スタイル合成**」と、必要に応じた「Compose `Color` → `@ColorInt` の内部変換（View 系 API へ橋渡しする際の `Color.toArgb()` 利用）」のみとする。

実効スタイル合成では、`CellStyle` の各フィールド（`titleColor` / `titleFont` / `descriptionColor` / `descriptionFont` / `valueTextColor` / `valueTextFont` / `iconSize` / `iconRadius` / `cellHeight` / `hintTextColor` / `hintTextFont` / `backgroundColor` / `accentColor`）が `null` のとき、対応する `Theme` フィールドで補完しなければならない (MUST)。`CellStyle.backgroundColor` 未指定時は `Theme.cellBackgroundColor`、`CellStyle.accentColor` 未指定時は `Theme.cellAccentColor` を採用する。`RecyclerView` 自体の背景色は `Theme.viewBackgroundColor` を採用しなければならない (MUST)。

タイトル色／フォントの合成は次の 3 段階優先順位でなければならない (MUST)：

1. `CellStyle.titleColor` が `null` でなければそれを採用（Compose `Color` をそのまま使う）
2. それ以外で `Theme.titleColor` が `null` でなければそれを採用
3. それ以外は `TextView` の既定色（`android.R.attr.textColorPrimary` 相当）にフォールバック

`titleFont` も同様に `CellStyle.titleFont` → `Theme.titleFont` → `TextView` 既定フォントの順序で解決する。

EffectiveStyle は「タイトル色が明示由来か（CellStyle または Theme のいずれかから指定されたか）プラットフォーム fallback 由来か」を判定できる Bool フラグ（例: `titleColorIsExplicit`）を提供しなければならない (MUST)。このフラグは ButtonCell の `baseColor` 解決で使用される。

#### Scenario: 実効スタイルの合成（Theme.titleColor 採用）

- **GIVEN** Cell の `CellStyle.titleColor = null`、`Theme.titleColor = Color(red = 0.2f, green = 0.4f, blue = 0.6f, alpha = 1.0f)`
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `effective.titleColor` は当該 Compose `Color`、`effective.titleColorIsExplicit == true` となる

#### Scenario: 実効スタイルの合成（プラットフォーム fallback）

- **GIVEN** Cell の `CellStyle.titleColor = null`、`Theme.titleColor = null`
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `effective.titleColor` は `TextView` 既定色相当、`effective.titleColorIsExplicit == false` となる

#### Scenario: 実効スタイルの合成（CellStyle 優先）

- **GIVEN** `CellStyle.titleColor = Color.Red`、`Theme.titleColor = Color.Blue`
- **WHEN** 実効スタイル合成関数を呼ぶ
- **THEN** `effective.titleColor` は赤、`effective.titleColorIsExplicit == true` となる

#### Scenario: CellStyle.backgroundColor の合成

- **GIVEN** `Theme(cellBackgroundColor = Color.White)` と `CellStyle(backgroundColor = Color.Yellow)` の Cell
- **WHEN** 実効スタイルを計算する
- **THEN** 当該 Cell の Cell コンテナ（`RippleDrawable` の content layer）の背景色は黄色（`CellStyle.backgroundColor` 優先）になる

#### Scenario: CellStyle.accentColor の合成（SwitchCell）

- **GIVEN** `Theme(cellAccentColor = Color.Blue)` と `CellStyle(accentColor = Color.Green)` の SwitchCell（`isOn = true`）
- **WHEN** ViewHolder が bind する
- **THEN** `MaterialSwitch.thumbTintList` / `trackTintList` が緑（`CellStyle.accentColor` 優先）に設定される

#### Scenario: viewBackgroundColor の反映

- **GIVEN** `Theme(viewBackgroundColor = Color(red = 0.95f, green = 0.93f, blue = 0.90f, alpha = 1.0f))` で初期化された `KsSettingsView`
- **WHEN** Android で表示される
- **THEN** `RecyclerView` の `setBackgroundColor` に当該色（内部で `Color.toArgb()` 変換した `@ColorInt`）が設定される

#### Scenario: valueTextColor / valueTextFont の合成

- **GIVEN** `Theme(descriptionColor = Color.Gray)` と `CellStyle(valueTextColor = Color(red = 0.2f, green = 0.2f, blue = 0.2f, alpha = 1.0f))` の LabelCell（`valueText = "オン"`）
- **WHEN** 実効スタイルを計算して LabelCellViewHolder が描画する
- **THEN** `valueTextView.setTextColor(...)` が darkGray 相当の `@ColorInt` で呼ばれる（`CellStyle.valueTextColor` 優先、Compose `Color.toArgb()` で変換）

#### Scenario: CellStyle.backgroundColor 適用と罫線描画の両立

- **GIVEN** `CellStyle(backgroundColor = Color.Yellow)` を持つ Cell が `ClassicSectionDecoration`（罫線描画用 `RecyclerView.ItemDecoration`）配下に表示される
- **WHEN** Cell の `applyCellBackground` で content layer の `ColorDrawable` を黄色に設定する
- **THEN** Cell 間の罫線が `setBackgroundColor` 相当の操作で消失しない。`ItemDecoration` は `onDrawOver` で罫線を描画し、children 描画後に重畳されるため、Cell コンテナの背景色変更によって罫線が上書きされてはならない (MUST NOT)

#### Scenario: KsColor 変換ユーティリティの不在

- **GIVEN** `ks-settingsview-ui` モジュール
- **WHEN** `KsColor.toColorInt()` 拡張関数を探す
- **THEN** 当該拡張関数は存在しない。`KsColor` 自体が存在しないため変換不要。Compose `Color → @ColorInt` の変換は Compose 標準の `Color.toArgb()` を直接利用する

### Requirement: タッチフィードバック（selectedColor の反映）

各 `CellViewHolder` は bind 時に `applyCellBackground(container, effective)` を呼び、`RippleDrawable` で `effective.selectedColor`（`Theme.selectedColor`）を ripple 色、`effective.backgroundColor`（`CellStyle.backgroundColor ?? Theme.cellBackgroundColor`）を content 背景色として設定しなければならない (MUST)。`container.isClickable = true` を設定し、`isEnabled = false` を除いて常に ripple を発生可能にしなければならない (MUST)。Compose `Color` から `@ColorInt` への変換は `Color.toArgb()` を用いる。

#### Scenario: タップ中の Ripple 発生

- **GIVEN** `Theme(cellBackgroundColor = Color.White, selectedColor = Color(red = 1.0f, green = 0.75f, blue = 0.0f, alpha = 0.3f))` の LabelCell が表示されている
- **WHEN** ユーザーが Cell をタップする
- **THEN** Cell の背景に Ripple エフェクトが `selectedColor` で発生し、リリース後に元の `cellBackgroundColor` に戻る

### Requirement: isEnabled 描画の反映

各 `CellViewHolder` は bind 時に `cell.isEnabled == false` の場合、以下を適用しなければならない (MUST)：

- container の `isClickable = false` / `setOnClickListener(null)`（タップを無効化）。
- コントロール要素（`MaterialSwitch` / `MaterialCheckBox` / `KsSimpleCheckView` / Button 風 `TextView` 等）の `isEnabled = false`。
- タイトル／説明文／値テキスト／ヒントテキストの `setTextColor` に `Theme.disabledTextColor.toArgb()` を適用。
- Cell 全体への `alpha` 適用や半透明化は行わない (MUST NOT)。

`cell.isEnabled == true`（既定）のときは、通常の bind ロジックを適用する。

#### Scenario: SwitchCell isEnabled = false の描画

- **GIVEN** `Theme(disabledTextColor = Color.LightGray)` と `SwitchCell(title = "通知", isOn = true, isEnabled = false)`
- **WHEN** Android で描画される
- **THEN** `MaterialSwitch.isEnabled = false`（標準の disabled 表示）になり、`titleView.setTextColor(lightGray.toArgb())` が呼ばれ、`container.setOnClickListener(null)` でタップ無効化される

#### Scenario: LabelCell isEnabled = false の描画

- **GIVEN** `LabelCell(title = "通知", description = "詳細", valueText = "オン", isEnabled = false)` と `Theme.disabledTextColor = Color.LightGray`
- **WHEN** Android で描画される
- **THEN** title / description / valueText の TextView すべてが lightGray で表示される

#### Scenario: CheckboxCell isEnabled = false で Ripple 無効

- **GIVEN** `CheckboxCell(title = "規約", isChecked = false, isEnabled = false)`
- **WHEN** ユーザーが Cell をタップする
- **THEN** `MaterialCheckBox.isEnabled = false` でチェック状態は変化せず、container の Ripple も発生しない（`isClickable = false` のため state_pressed が走らない）

### Requirement: ButtonCell の baseColor 解決順序

`ButtonCellViewHolder` はボタンテキストの基準色（disabled 適用前の色）を次の 4 段階優先順位で解決しなければならない (MUST)：

1. `ButtonCell.titleColor`（Cell 個別、型: `Color?`）が指定されていればそれを採用
2. それ以外で `CellStyle.titleColor` が指定されていれば `effective.titleColor` を採用
3. それ以外で `Theme.titleColor` が指定されていれば `effective.titleColor` を採用
4. それ以外は `MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)` 相当（Material primary 色）

「2 または 3 のいずれか」の判定は EffectiveStyle の `titleColorIsExplicit`（または同等のフラグ）で行ってよい。`cell.isEnabled == false` のときは、上記で解決した基準色ではなく `effective.disabledTextColor` を用いて `titleView.setTextColor(...)` を呼ばなければならない (MUST)。

#### Scenario: ButtonCell.titleColor 指定時

- **GIVEN** `ButtonCell(title = "削除", titleColor = Color.Red)`、`CellStyle.titleColor = null`、`Theme.titleColor = Color.Green`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(Color.Red.toArgb())` 相当が呼ばれる（Cell 個別 `titleColor` 優先）

#### Scenario: CellStyle.titleColor 指定時

- **GIVEN** `ButtonCell(title = "次へ", titleColor = null)`、`CellStyle.titleColor = Color.Magenta`、`Theme.titleColor = Color.Green`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(purple.toArgb())`（CellStyle 経由）が呼ばれる

#### Scenario: Theme.titleColor 指定時

- **GIVEN** `ButtonCell(title = "登録", titleColor = null)`、`CellStyle.titleColor = null`、`Theme.titleColor = Color(red = 0.8f, green = 0.6f, blue = 0.0f, alpha = 1.0f)`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(...)` が `Theme.titleColor.toArgb()` で呼ばれる（4 段階目の Material primary ではない）

#### Scenario: 全段階未指定時の Material primary

- **GIVEN** `ButtonCell(title = "OK", titleColor = null)`、`CellStyle.titleColor = null`、`Theme.titleColor = null`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(...)` が Material `colorPrimary` 属性から解決された色で呼ばれる

#### Scenario: isEnabled = false 時の disabledTextColor 適用

- **GIVEN** `ButtonCell(title = "削除", titleColor = Color.Red, isEnabled = false)`、`Theme.disabledTextColor = Color.LightGray`
- **WHEN** ButtonCellViewHolder が描画する
- **THEN** `titleView.setTextColor(lightGray.toArgb())`（disabledTextColor）が呼ばれる（baseColor の `.Red` は使われない）

### Requirement: KsImage 派生のアイコン解決

`ks-settingsview-ui` は `LabelCellViewHolder` / `CommandCellViewHolder` 等で Cell の `icon: KsImage?` を解決し、`ImageView.setImageDrawable(...)` に渡さなければならない (MUST)。解決優先順位は以下：

1. `icon == null` → アイコン領域を `View.GONE` で非表示
2. `icon is KsImage.Drawable` → `setImageDrawable(icon.drawable)` で直接設定
3. `icon is KsImage.Resource` → `ContextCompat.getDrawable(context, icon.resId)` で取得し `setImageDrawable(...)` で設定。取得失敗（`null`）は `View.GONE` でフォールバック
4. `icon is KsImage.SystemName` → 解決不可。`View.GONE` でフォールバックする (MUST)。エラーログや throw を行ってはならない (MUST NOT)

`KsImage` 型は `jp.kamusoft.kssettingsview.ui` パッケージに所属する。`jp.kamusoft.kssettingsview.core` には存在しない (MUST NOT 存在)。

#### Scenario: KsImage.Resource の解決

- **GIVEN** `LabelCell(icon = KsImage.Resource(R.drawable.ic_storage))`
- **WHEN** Android で描画される
- **THEN** Cell のアイコン領域に `ContextCompat.getDrawable(context, R.drawable.ic_storage)` が `setImageDrawable` で設定され、ImageView が `View.VISIBLE` で描画される

#### Scenario: KsImage.Drawable の解決

- **GIVEN** `LabelCell(icon = KsImage.Drawable(customDrawable))`
- **WHEN** Android で描画される
- **THEN** Cell のアイコン領域に渡された `customDrawable` がそのまま `setImageDrawable` で設定される

#### Scenario: KsImage.SystemName のフォールバック（Android）

- **GIVEN** `LabelCell(icon = KsImage.SystemName("bell"))`
- **WHEN** Android で描画される
- **THEN** UI 層は `SystemName` を解決できないため、アイコン領域は `View.GONE` でフォールバックし、Title が左寄せでアイコン領域分のインデントなしに配置される。エラーログや throw は発生しない

#### Scenario: icon = null

- **GIVEN** `LabelCell(icon = null)`
- **WHEN** Android で描画される
- **THEN** Cell のアイコン領域は `View.GONE` で非表示となる
