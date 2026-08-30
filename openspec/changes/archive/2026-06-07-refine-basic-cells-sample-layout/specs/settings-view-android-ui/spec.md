## ADDED Requirements

### Requirement: SwitchCell の Thumb / Track 色分離

`settings-view-android-ui` は `SwitchCellViewHolder` で `MaterialSwitch` の `thumbTintList` と `trackTintList` を独立に設定しなければならない (MUST)。Track 側も状態別 `ColorStateList` で設定しなければならない (MUST)。Material 3 標準の MaterialSwitch オフ時挙動に揃え、**オフ時の Track と Thumb には異なる Material トークンを使い、両者が視覚的に分離して見える色にしなければならない (MUST)**。

- `trackTintList`: 状態別 `ColorStateList`
  - `android.R.attr.state_checked = true` → 実効 accent 色（`CellStyle.accentColor ?? Theme.cellAccentColor`）
  - `android.R.attr.state_checked = false` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorSurfaceContainerHighest, Color.LTGRAY)` 相当（薄いグレー）。古い Material ライブラリで `colorSurfaceContainerHighest` が未解決の場合は `Color.LTGRAY` にフォールバックする。
- `thumbTintList`: 状態別 `ColorStateList`
  - `android.R.attr.state_checked = true` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorOnPrimary, Color.WHITE)` 相当（白系）
  - `android.R.attr.state_checked = false` → `MaterialColors.getColor(view, com.google.android.material.R.attr.colorOutline, Color.GRAY)` 相当（中間〜濃いグレー）

**オフ時の Track（`colorSurfaceContainerHighest` ≒ 薄いグレー）と Thumb（`colorOutline` ≒ 中間グレー）は明確に異なる色でなければならない (MUST)**。両方を `colorOutline` にしてはならない (MUST NOT)。

**Rationale**: 前回実装では Track / Thumb のオフ時を両方とも `colorOutline` にしていたため、Image #6 のスクリーンショットで両者が同色化し「Switch がのっぺりした 1 枚のグレー帯」に見える状態だった。Material 3 標準（Image #7 の Google Play 通知設定など）に揃え、オフ時 Track を `colorSurfaceContainerHighest`（薄いグレー）、オフ時 Thumb を `colorOutline`（中間グレー）にすることで、Thumb / Track の輪郭が明確に分離して見える。

#### Scenario: checked = true 時の Thumb 白系・Track accent

- **GIVEN** `SwitchCell(title: "通知", isOn: true)` で `Theme(cellAccentColor: KsColor.orange)` を適用
- **WHEN** Android で描画される
- **THEN** Track はオレンジ色（実効 accent）で、Thumb は白系（`colorOnPrimary` 相当）で描画され、Thumb と Track が視覚的に分離して見える

#### Scenario: checked = false 時の Thumb と Track が分離した色になる

- **GIVEN** `SwitchCell(title: "通知", isOn: false)`
- **WHEN** Android で描画される
- **THEN** Track は薄いグレー（`colorSurfaceContainerHighest` 相当）、Thumb は中間グレー（`colorOutline` 相当）で描画される。Track と Thumb の色は明確に異なり、Switch の輪郭が視覚的に判別できる。Track は accent 色（オレンジ）にはならない。

#### Scenario: オフ時 Track と Thumb の色は等しくない

- **GIVEN** `SwitchCell(title: "通知", isOn: false)` を任意の Theme で描画
- **WHEN** `trackTintList.getColorForState(unchecked, ...)` と `thumbTintList.getColorForState(unchecked, ...)` を取得する
- **THEN** 2 つの色は等しくない（`!=`）。

#### Scenario: CellStyle.accentColor の優先

- **GIVEN** `SwitchCell(title: "通知", isOn: true, style: CellStyle(accentColor: KsColor.green))` を `Theme(cellAccentColor: KsColor.orange)` 下で表示
- **WHEN** Android で描画される
- **THEN** Track は緑（`CellStyle.accentColor` 優先）、Thumb は白系で描画される

### Requirement: セクション罫線の描画位置と太さ

`settings-view-android-ui` の `ClassicSectionDecoration`（`onDrawOver` で描画）は、AiForms.Maui.SettingsView オリジナル `Platforms/Droid/SVItemdecoration.cs` および `drawable/divider.xml` (`<size android:height="1px"/>`) の挙動に揃え、次の規則で罫線を描画しなければならない (MUST)。

- **線の太さ**: 1 ピクセル固定（`density` による dp 換算は行わない）。`Paint.strokeWidth` または描画矩形の高さで 1px を保証する。
- **描画位置**: 各 Section の上端と下端、および Section 内の Cell 間。
  - セクション最初の Cell の上端 → 罫線を描画する (MUST)
  - セクション最後の Cell の下端 → 罫線を描画する (MUST)
  - セクション内 Cell 間 → 罫線を描画する (MUST)
- **線の色**: `Theme.separatorColor`。
- **左インセット規則**（iOS と視覚的に揃える。AiForms オリジナル準拠）:
  - セクション最初 Cell の **上端罫線** → 左インセット 0（`paddingLeft` から `width - paddingRight` まで、端から端で描画）
  - セクション最後 Cell の **下端罫線** → 左インセット 0（端から端で描画）
  - セクション内中間 Cell の **下端罫線** → 左インセット 16dp 相当（`paddingLeft + 16 * displayMetrics.density` から `width - paddingRight` まで描画）
  - アイコン有無に関わらず固定（iOS と同様、AiForms オリジナル準拠の挙動）

**Rationale**: iOS 側の「罫線インセット規則」Requirement（セクション境界 = 0、セクション内中間 = 16pt）とクロスプラットフォームで視覚的に揃えるため、Android 側も同じ規則を採用する。AiForms.Maui.SettingsView オリジナル Android のスクリーンショットでも、セクション内 Cell 間罫線に約 16dp の左インセットが見えており、本実装はこれに準拠する。

#### Scenario: セクション上端の罫線

- **GIVEN** 2 つ以上のセクションを持つ SettingsView の 2 番目のセクション
- **WHEN** Android で描画される
- **THEN** 2 番目のセクションの最初の Cell の **上端**にも `Theme.separatorColor` で 1px の罫線が描画される（iOS と同じ「セクション境界では端から端」の見た目）

#### Scenario: 罫線の太さが 1px

- **GIVEN** 任意の Cell 行
- **WHEN** Android で描画される
- **THEN** Cell 下端の罫線は **1 ピクセル**（density による dp 換算なし）で描画され、端末密度に関わらず細い hairline として見える

#### Scenario: セクション境界の罫線は端から端で描画される

- **GIVEN** 3 つの Cell を持つセクションがあり、その前後に別の Section が存在する
- **WHEN** Android で描画される
- **THEN** セクション最初 Cell の上端罫線とセクション最後 Cell の下端罫線は、左インセット 0（`paddingLeft` から `width - paddingRight` まで）で描画される

#### Scenario: セクション内中間 Cell の下端罫線は 16dp インセットで描画される

- **GIVEN** 3 つの Cell を持つセクション
- **WHEN** Android で描画される
- **THEN** セクション内中間 Cell（最初でも最後でもない Cell）の下端罫線は、左に 16dp 相当のインセット（`paddingLeft + 16 * displayMetrics.density` から `width - paddingRight` まで）で描画され、iOS の `bottomSeparatorInsets.leading = 16pt` と視覚的に揃う

#### Scenario: アイコン有り Cell の中間下端罫線も同じ 16dp インセット

- **GIVEN** アイコン有り Cell とアイコン無し Cell が混在する 1 つのセクション
- **WHEN** Android で描画される
- **THEN** セクション内中間 Cell の下端罫線はアイコンの有無に関わらず一律 16dp 相当のインセットで描画され、Cell ごとにインセット幅が変動してはならない（iOS の固定 16pt と同じ方針）

### Requirement: CheckboxCell の右端整列強化

`settings-view-android-ui` は `CheckboxCellViewHolder` で `MaterialCheckBox` に明示サイズ（24dp × 24dp）の `LayoutParams` を設定しなければならない (MUST)。`setPadding(0, 0, 0, 0)` および `minimumWidth = 0` / `minimumHeight = 0` の設定は維持する。さらに必要に応じて `marginEnd` の微調整を行い、`SwitchCell` / `RadioCell` / `SimpleCheckCell` の各右端 X 座標と ±1px 以内に一致させなければならない (MUST)。

#### Scenario: 24dp 明示サイズの適用

- **GIVEN** `CheckboxCell(title: "規約に同意する", isChecked: true)`
- **WHEN** Android で描画される
- **THEN** `MaterialCheckBox` の `layoutParams.width == (24 * density).toInt()` かつ `height == (24 * density).toInt()` となる

#### Scenario: 4 種アクセサリの右端整列

- **GIVEN** 同じ画面に `SwitchCell`、`CheckboxCell`、`RadioCell`、`SimpleCheckCell` を順に並べた状態
- **WHEN** Android で描画される
- **THEN** 各 Cell の右端アクセサリ（Switch / CheckBox / チェックマーク / SimpleCheck）の右端 X 座標は ±1px 以内で一致する

### Requirement: Section Header / Footer の垂直配置

`settings-view-android-ui` の Section Header / Footer 描画（`SectionTextAccessoryViewHolder` 経路）は、AiForms.Maui.SettingsView オリジナル iOS 側 `TextHeaderView.cs` の挙動（既定 = `LayoutAlignment.End`）と視覚的に揃え、以下の垂直配置を実装しなければならない (MUST)。

- **Section Header** の TextView は `Gravity.BOTTOM or Gravity.START` で **下端揃え**にする (MUST)。
- **Section Footer** の TextView は `Gravity.TOP or Gravity.START` で **上端揃え**にする (MUST)。

これにより、Header テキストは直下の Cell とぴったり接する位置に表示され、Footer テキストは直上の Cell とぴったり接する位置に表示される。iOS との視覚的な整合性も担保される。

#### Scenario: Section Header の下揃え

- **GIVEN** `Section(header = "CommandCell", headerHeight = 60.0, ...)` を持つセクション
- **WHEN** Android で描画される
- **THEN** Header の "CommandCell" テキストは固定 60dp の TextView 領域の **下端**に張り付くように配置される

#### Scenario: Section Footer の上揃え

- **GIVEN** `Section(footer = "You can select either TypeA or TypeB.", ...)`
- **WHEN** Android で描画される
- **THEN** Footer テキストは TextView 領域の **上端**に張り付くように配置される

### Requirement: Section.headerHeight の UI 反映

`settings-view-android-ui` は `Section.headerHeight: Double` の値を Header の描画高さに反映しなければならない (MUST)。

- `headerHeight > 0` → `SectionTextAccessoryViewHolder` の `itemView.layoutParams.height = (headerHeight * displayMetrics.density).toInt()` を明示設定して **固定高さ**で描画する (MUST)。
- `headerHeight == -1`（既定）→ `itemView.layoutParams.height = WRAP_CONTENT` のまま、テキスト寸法に応じた自動高さで描画する。

実装の最小要件として、`CellListItem.SectionHeader` データクラスに `headerHeight: Double` フィールドを追加し、`KsSettingsView.flatten()` から `section.headerHeight` を伝搬する。`SectionTextAccessoryViewHolder.bind()` で受け取った値を `layoutParams.height` に反映する。

**Rationale**: 従来は `Section.headerHeight` が Core 側に存在するだけで UI 層に伝搬しておらず、Sample で `headerHeight = 40.0` / `60.0` を指定しても他セクションと同じ自動高さで描画されていた（実機画像 Image #10 で確認）。本要件で UI 層への伝搬経路と高さ適用を明示する。

#### Scenario: headerHeight 正値による固定高さ

- **GIVEN** `Section(header = "CommandCell", headerHeight = 60.0, ...)`
- **WHEN** Android で描画される
- **THEN** Header の表示高さが 60dp に density を掛けた px 値で固定され、他セクション（既定 `-1`）と明らかに異なる高さで描画される

#### Scenario: headerHeight = -1 既定値の自動高さ

- **GIVEN** `Section(header = "LabelCell", ...)`（既定 `headerHeight = -1.0`）
- **WHEN** Android で描画される
- **THEN** Header の `itemView.layoutParams.height` は `WRAP_CONTENT` のままで、テキスト寸法に応じた自動高さで描画される

### Requirement: 基本 Cell 共通の垂直パディング

`settings-view-android-ui` の基本 Cell 7 種（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）のコンテナ View（`LinearLayout`）は、AiForms.Maui.SettingsView オリジナル `Platforms/Android/Resources/layout/cellbaseview.axml` の `paddingTop="4dp"` / `paddingBottom="4dp"` に揃え、**上下パディングを 4dp**（density を掛けた px 値）に設定しなければならない (MUST)。

- 横方向のパディングは標準左マージン 16dp を維持する。
- 各 Cell の最低高さ（`minimumHeight` 経由）は `EffectiveStyle.effectiveHeightDp`（既定 44dp = `MinRowHeight`）で保証されるため、視覚的な行高さは大きく変わらず、AiForms オリジナルの密度に揃う。

これにより iOS スクリーンショット（Image #8）および AiForms.Maui.SettingsView オリジナル（Image #11）と Android（Image #10）の上下密度差が解消される。

#### Scenario: 基本 Cell の垂直パディングが 4dp

- **GIVEN** 任意の基本 Cell（例: `LabelCell`）
- **WHEN** Android で描画される
- **THEN** `container.paddingTop == (4 * density).toInt()` かつ `container.paddingBottom == (4 * density).toInt()` であり、`paddingLeft` / `paddingRight` は引き続き `(16 * density).toInt()`

### Requirement: KsImage 派生のアイコン解決

`settings-view-android-ui` は `LabelCellViewHolder` / `CommandCellViewHolder` 等で Cell の `icon: KsImage?` を解決し、`ImageView.setImageDrawable(...)` に渡さなければならない (MUST)。解決優先順位は以下：

1. `icon == null` → アイコン領域を `View.GONE` で非表示
2. `icon is KsImage.Drawable` → `setImageDrawable(icon.drawable)` で直接設定
3. `icon is KsImage.Resource` → `ContextCompat.getDrawable(context, icon.resId)` で取得し `setImageDrawable(...)` で設定。取得失敗（`null`）は `View.GONE` でフォールバック
4. `icon is KsImage.SystemName` → 解決不可。`View.GONE` でフォールバックする (MUST)。エラーログや throw を行ってはならない (MUST NOT)

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
