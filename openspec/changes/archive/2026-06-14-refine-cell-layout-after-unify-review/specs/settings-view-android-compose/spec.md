## MODIFIED Requirements

### Requirement: 共通行レイアウト関数 applyCellBaseLayout（View ベース）

`ks-settingsview-ui`（Android）は、全 CellViewHolder が共通で保持する **View ベースの行レイアウト関数 `applyCellBaseLayout(views, ...)`** を `internal` 可視性で提供しなければならない (SHALL)。この関数は `cell-types-basic` の「全 Cell 共通の description / valueText / icon / hintText フィールド」Requirement で規定された 2 系統のレイアウト規約（本体行 `[icon][title / description][valueText (title 行の右寄せ)][accessory (右側中央)]` + `hintText` の右上 float 配置）を `ConstraintLayout` ベースの `CellBaseViews` 構造体に対して反映する責務を持つ。

本 Requirement では、**Compose（`androidx.compose.runtime`）を用いない** ことを明確に定める (MUST NOT)。すなわち、ViewHolder の `bind(...)` 内で `ComposeView.setContent { ... }` を呼び出して `KsCellRow` Composable を組む実装方式は採用してはならない (MUST NOT)。理由はオリジナル `AiForms.Maui.SettingsView` の Android 実装が `RelativeLayout` ベースの View ヒエラルキーで構成されており、本 change の目的は「共通フィールドの単一化」であって UI 実装の Compose 化ではないこと、また MAUI 移植も視野に入れたパフォーマンス・互換性の観点で View ベースが優位なことによる（unify change の `design.md` Decision 11 参照）。

過渡的に存在した Compose 版 `KsCellRow.kt`（`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRowLayout.kt` 等）は、本 Requirement 適用時に **完全に削除** しなければならない (MUST)。

#### CellBaseViews 構造体

`CellBaseViews` は、全 CellViewHolder が共通で保持する View 参照群を束ねた構造体（`internal class` または `data class`）でなければならない (MUST)。少なくとも以下のフィールドを持つ：

- `root: ConstraintLayout` — セル全体のルート ViewGroup
- `iconView: ImageView` — アイコン表示
- `titleView: TextView` — タイトル表示
- `descriptionView: TextView` — 説明文表示
- `valueTextView: TextView` — 値テキスト表示（title 行の右寄せ）
- `accessoryHolder: FrameLayout` — Cell 種別固有の trailing コントロールを差し込むためのコンテナ
- `hintTextView: TextView` — ヒントテキスト表示（右上 float）

`CellBaseViews` の構築は programmatic（Kotlin コードによる動的構築）で行わなければならない (MUST)。XML レイアウトファイル（`layout/*.xml`）への切り出しは行わない。

#### ConstraintLayout root の minimumHeight 下限保証

`buildCellBaseViews` の `root` View は、標準 `androidx.constraintlayout.widget.ConstraintLayout` ではなく **`MinHeightConstraintLayout`**（`ConstraintLayout` を継承し `onMeasure` 後に `measuredHeight` を `minimumHeight` で下限ガードする internal サブクラス）を使用しなければならない (MUST)。

`MinHeightConstraintLayout` は `onMeasure(widthMeasureSpec, heightMeasureSpec)` 内で、まず `super.onMeasure(widthMeasureSpec, heightMeasureSpec)` を呼んで標準の制約解決を行い、その結果 `measuredHeight < minimumHeight` の場合は `heightMeasureSpec` を `MeasureSpec.EXACTLY(minimumHeight)` に差し替えて **再度** `super.onMeasure(...)` を呼ぶことで、内部の制約解決を `minimumHeight` 高さで再実行しなければならない (MUST)。これにより `measuredHeight` だけでなく **子 View の縦位置**（chain bias / `TOP=parent.TOP` + `BOTTOM=parent.BOTTOM` の CenterVertical 等）も新しい高さに合わせて再配置される。`setMeasuredDimension(measuredWidth, minimumHeight)` で `measuredHeight` 値のみを上書きする方式は、`accessoryHolder` の CenterVertical 等が元の小さい高さに対して計算されたまま残るため採用してはならない (MUST NOT)。

`measuredHeight >= minimumHeight` のとき（内容が下限を超える Cell）は intrinsic な測定結果をそのまま維持し、可変高さの上方向伸縮を阻害してはならない (MUST NOT)。`minimumHeight <= 0` のときも再 measure を行ってはならない (MUST NOT)。

この MUST を導入する根拠：
- 標準 `ConstraintLayout` は、`layoutParams.height = WRAP_CONTENT` かつ親（`RecyclerView` + `LinearLayoutManager`）から `heightSpec = UNSPECIFIED` で measure される実機シナリオで、`setMinimumHeight()` を尊重しない測定結果を返すケースが知られている（[Common ConstraintLayout Pitfalls](https://blog.ostebaronen.dk/2018/12/common-constraintlayout-mistakes.html) / [androidx/constraintlayout#855](https://github.com/androidx/constraintlayout/issues/855) / [b/136492486](https://issuetracker.google.com/issues/136492486)）。
- オリジナル `AiForms.Maui.SettingsView` の `SettingsViewRecyclerAdapter.cs:483-487` も `holder.Body` と `nativeCell` の両方に `SetMinimumHeight` を呼ぶ回避策を取っている（コメント `// it is neccesary to set both`）。
- Robolectric テストでは `root.minimumHeight == 60dp 相当 px` が観測できる一方、実機では `Theme()` 既定の `applyEffectiveHeight(isFixedHeight = false)` で設定した `minimumHeight = 60dp` が measure に反映されず Cell が詰まる事象が `refine-cell-layout-after-unify-review` のオーナー実機確認で確認されている。

`applyEffectiveHeight(view, effective)` の `isFixedHeight = false` 経路（`Theme.hasUnevenRows == true` 既定経路）では、`layoutParams.height = WRAP_CONTENT` のまま `view.minimumHeight = effectiveHeightPx` を設定すれば、`MinHeightConstraintLayout.onMeasure` の下限ガードによって実機 measure 結果も `effectiveHeightPx` 以上に保証される。

#### ConstraintLayout 配置規約

`CellBaseViews` のルートは `ConstraintLayout` でなければならず、内部 View の制約は以下の構造を満たさなければならない (MUST)：

- **iconView**: 左端中央 — `Start=parent.Start`, `Top=parent.Top`, `Bottom=parent.Bottom`（CenterVertical）。**`iconView` には `End` 制約を持たせず、右側余白は後段 `titleView` / `descriptionView` の `Start=iconView.End` 接続に margin を渡して与えなければならない (MUST)**。すなわち `iconView.layoutParams.marginEnd` を設定しても ConstraintLayout は対応 anchor が無いと無視するため、その方法は採用してはならない (MUST NOT)。
- **titleView と descriptionView は本体行の縦中央寄せ vertical chain を構成しなければならない (MUST)**:
  - **titleView**: icon の右、accessoryHolder の左、本体行 vertical chain の **head** — `Start=iconView.End` に margin `iconMarginEnd`（16dp 相当 px）を渡し、`Top=parent.Top`, `End=accessoryHolder.Start`, `Bottom=descriptionView.Top`。`iconView` が `GONE` のときに余白を潰すため `setGoneMargin(titleView.id, ConstraintSet.START, 0)` を明示的に設定しなければならない (MUST)。
  - **descriptionView**: icon の右、accessoryHolder の左、本体行 vertical chain の **tail** — `Start=iconView.End` に margin `iconMarginEnd`（16dp 相当 px）を渡し、`Top=titleView.Bottom`, `End=accessoryHolder.Start`, `Bottom=parent.Bottom`。同じく `setGoneMargin(descriptionView.id, ConstraintSet.START, 0)` を設定しなければならない (MUST)。
  - 両者の vertical chain は `ConstraintSet.CHAIN_PACKED` で結ばれ、`verticalBias = 0.5f` により本体行が cell 縦中央に packed 配置されなければならない (MUST)。
  - `description == null` で `descriptionView.visibility = GONE` のとき、ConstraintLayout は GONE chain member をスペース 0 として扱うため、`titleView` 単独でも縦中央寄せ配置が維持される。
- **valueTextView**: title 行の右寄せ — `End=accessoryHolder.Start`, `Baseline=titleView.Baseline`（title と同じ行で右端に配置）。titleView が vertical chain により cell 縦中央付近に配置されるため、valueTextView もその縦中央付近に位置する。
- **accessoryHolder**: 右端中央 — `End=parent.End`, `Top=parent.Top`, `Bottom=parent.Bottom`（CenterVertical）
- **hintTextView**: セル右上に float 配置 — `End=parent.End`, `Top=parent.Top` (セル上端から数 dp のマージン)、Z 順は accessoryHolder より後ろに `addView` することで前面（最前面）に置く

`hintTextView` と `accessoryHolder` は両者とも右端揃いとなるため物理的に重なり得るが、`hintTextView` がセル上端基準・`accessoryHolder` がセル縦中央基準で配置されるため通常は干渉しない。`hintTextView` は `accessoryHolder` より後に `addView` することで Z 順の前面に置かれ、万一の干渉時にも `hintText` が前面に見える状態を保証しなければならない (MUST)。

#### applyCellBaseLayout 関数

`applyCellBaseLayout` 関数のシグネチャは次の形でなければならない (MUST)：

```kotlin
internal fun applyCellBaseLayout(
    views: CellBaseViews,
    title: String,
    description: String?,
    valueText: String?,
    icon: KsImage?,
    hintText: String?,
    effective: EffectiveStyle,
    isEnabled: Boolean,
)
```

実装上の振る舞いは以下を満たさなければならない (MUST)：

- `title` は `views.titleView.text` に反映し、フォントを `effective.titleFont` で、文字色を `effective.titleColor`（`isEnabled == false` のときは `effective.disabledTextColor`）で設定する。
- `description` が `null` のときは `views.descriptionView.visibility = GONE`、`null` でないときは `VISIBLE` + テキスト反映 + `effective.descriptionFont` / `effective.descriptionColor`（disabled 時は `disabledTextColor`）。
- `valueText` が `null` のときは `views.valueTextView.visibility = GONE`、`null` でないときは `VISIBLE` + テキスト反映 + `effective.valueTextFont` / `effective.valueTextColor`（disabled 時は `disabledTextColor`）。
- `icon` の `KsImage` 派生（`Resource` / `Drawable` / `SystemName`）を網羅して `views.iconView` に反映する。`Resource` は `setImageResource(resId)`、`Drawable` は `setImageDrawable(drawable)`、`SystemName` 派生は Android では解決不可のため `iconView.visibility = GONE`。`icon == null` のときも `iconView.visibility = GONE`。
- `hintText` が `null` のときは `views.hintTextView.visibility = GONE`、`null` でないときは `VISIBLE` + テキスト反映 + `effective.hintTextFont` / `effective.hintTextColor`（disabled 時は `disabledTextColor`）。`hintTextView` の `singleLine = true` / `ellipsize = END` / `gravity = END` を設定する（小さなテキスト・右寄せ・1 行・末尾省略のオリジナル挙動を踏襲）。
- `views.root` の背景色を `effective.cellBackgroundColor` で適用する。
- `isEnabled` を `views.root.isEnabled` に反映し、サブ View にも適切に伝播する。

#### 各 CellViewHolder からの利用

各 CellViewHolder（`LabelCellViewHolder` / `CommandCellViewHolder` / `SwitchCellViewHolder` / `CheckboxCellViewHolder` / `RadioCellViewHolder` / `SimpleCheckCellViewHolder` / `ButtonCellViewHolder`）は、内部で `CellBaseViews` を 1 個保持し、`bind(cell, theme)` 内で `applyCellBaseLayout(views, ...)` を呼び出して共通フィールドを描画しなければならない (MUST)。`title` / `description` / `valueText` / `icon` / `hintText` のレイアウト構築コード（テキスト反映・色反映・フォント反映・visibility 制御）を各 ViewHolder 内に重複実装してはならない (MUST NOT)。

各 CellViewHolder は、自身固有の trailing コントロール（例: `SwitchCellViewHolder` の `com.google.android.material.materialswitch.MaterialSwitch`、`CheckboxCellViewHolder` の `com.google.android.material.checkbox.MaterialCheckBox`、`RadioCellViewHolder` の `KsCheckmarkAccessoryView` 相当、`SimpleCheckCellViewHolder` の checkmark View、`CommandCellViewHolder` の chevron `ImageView`）を `views.accessoryHolder` に `addView` して配置しなければならない (MUST)。`LabelCellViewHolder` および `ButtonCellViewHolder` は `accessoryHolder` を空のまま使用する（addView しない）。

`MaterialCheckBox` の右端整列規約（`cell-types-basic` の「右端アクセサリ位置の整列（Android）」Scenario）は、`CheckboxCellViewHolder` が `MaterialCheckBox` を `accessoryHolder` に追加する際に `setPadding(0, 0, 0, 0)` / `minimumWidth = 0` / `minimumHeight = 0` を設定することで満たされなければならない (MUST)。

#### ButtonCellViewHolder の aux 切替

`ButtonCellViewHolder` は、`cell.valueText` / `cell.icon` / `cell.hintText` のいずれかが指定されている場合は **通常レイアウト**（上記 `applyCellBaseLayout` を経由したレイアウト）で描画し、`titleAlignment` は title 列の中での揃え位置（`titleView.gravity`）のみに反映しなければならない (MUST)。すべて `null` の場合は **ボタンスタイル**（`titleAlignment` を Cell 全体の中央寄せ／左寄せ／右寄せに反映）で描画しなければならない (MUST)。ボタンスタイル時にも `CellBaseViews` を使うが、`iconView` / `descriptionView` / `valueTextView` / `accessoryHolder` / `hintTextView` は全て `GONE` とし、`titleView` のみを Cell 全体に広げて配置する。

#### Scenario: CellBaseViews 経由で SwitchCell が描画される

- **GIVEN** `SwitchCellViewHolder` の bind 内で `applyCellBaseLayout(views, title = "通知", description = "プッシュ通知", valueText = "オン", icon = KsImage.Resource(R.drawable.ic_notifications), hintText = "推奨", effective = effective, isEnabled = true)` を呼び、その後 `views.accessoryHolder.addView(materialSwitch)` を呼ぶ
- **WHEN** Cell が表示される
- **THEN** 本体行は左端にベルアイコン（CenterVertical）、その右に「通知」「プッシュ通知」が縦中央寄せの vertical chain で配置され（cell 縦中央付近に packed）、title 行右端寄せに「オン」、右端中央に `MaterialSwitch`（ON 状態）が配置される。`hintText` 「推奨」はセル右上に float 表示され、`MaterialSwitch` とは上端 vs 縦中央で位置が分かれるため通常は重ならない

#### Scenario: 本体行 vertical chain で title / description / valueText が cell 縦中央付近に配置される

- **GIVEN** `Theme(hasUnevenRows = true)` の `KsSettingsView` に、十分な cell 高さ（例: 80dp）を持つ `SwitchCell(title = "通知", description = "プッシュ通知", valueText = "オン")` を含むセクションが描画されている
- **WHEN** レイアウト後の `titleView` / `descriptionView` / `valueTextView` / `accessoryHolder` の Y 座標を測定する
- **THEN** `titleView` と `descriptionView` は vertical chain で packed 配置され、両者を結合した縦中心が `root.height / 2` 付近にある。`valueTextView` は `titleView.BASELINE` に紐付くため `titleView` の縦中心 Y と同じ位置にある。`accessoryHolder` の縦中心 Y も `root.height / 2` 付近にあり、本体行 / accessory が cell 縦中央付近で揃って配置される

#### Scenario: description が GONE のときも titleView が縦中央寄せ

- **GIVEN** `ButtonCell(title = "ログアウト")`（description / valueText / icon / hintText いずれも null）を描画する。`descriptionView.visibility = GONE`
- **WHEN** レイアウト後の `titleView.top` / `titleView.bottom` を測定する
- **THEN** `titleView` の縦中心 Y が `root.height / 2` 付近にある（GONE の `descriptionView` は chain member としてスペース 0 で扱われるため、titleView 単独でも packed bias 0.5 が機能し、縦中央寄せが維持される）

#### Scenario: 各 ViewHolder が applyCellBaseLayout を経由する

- **GIVEN** `ks-settingsview-ui` ソース内の `LabelCellViewHolder.kt` / `CommandCellViewHolder.kt` / `SwitchCellViewHolder.kt` / `CheckboxCellViewHolder.kt` / `RadioCellViewHolder.kt` / `SimpleCheckCellViewHolder.kt` / `ButtonCellViewHolder.kt`
- **WHEN** これらのファイルから `bind(...)` の本体を grep する
- **THEN** 各 ViewHolder は `applyCellBaseLayout(views, ...)` を呼び出しており、テキスト反映（`textView.text = ...`）・色反映・フォント反映・visibility 制御を各 ViewHolder 内で個別に書いている箇所はない。各 ViewHolder 内に残るのは「accessoryHolder への trailing コントロールの addView」と Cell 種別固有のイベントハンドラ（`OnCheckedChangeListener` 等）のみである

#### Scenario: applyCellBaseLayout が internal 可視性

- **GIVEN** `ks-settingsview-ui` の外部モジュール（例: `ks-settingsview-core` / サンプルアプリ / 後続 change で追加される未来の Cell）
- **WHEN** `import jp.kamusoft.kssettingsview.ui.applyCellBaseLayout` 後に直接呼び出そうとする
- **THEN** `internal` 可視性のためコンパイルエラーになる（同モジュール内からは呼べる）

#### Scenario: iconView と titleView の右側余白は ConstraintSet.connect の margin で与える

- **GIVEN** アイコン付きの `SwitchCell(title = "通知", icon = KsImage.Resource(...), isOn = true)` を `CellBaseViews` で描画する
- **WHEN** Robolectric / 実機で `iconView.right` と `titleView.left` を測定する
- **THEN** `titleView.left - iconView.right` は `iconMarginEnd = 16dp 相当の px` に一致する。これは `iconView.layoutParams.marginEnd` ではなく、`set.connect(titleView.id, ConstraintSet.START, iconView.id, ConstraintSet.END, iconMarginEnd)` で margin を渡すことで成立する

#### Scenario: アイコン無しのとき titleView の左端余白は潰される

- **GIVEN** アイコン無しの `SwitchCell(title = "通知", isOn = true)` を `CellBaseViews` で描画する（`iconView.visibility = GONE`）
- **WHEN** Robolectric / 実機で `titleView.left` を測定する
- **THEN** `titleView.left` は `root.paddingLeft` 付近に張り付く。これは `set.setGoneMargin(titleView.id, ConstraintSet.START, 0)` により GONE 時に `iconMarginEnd` 余白が消失するためである

#### Scenario: hintTextView は右上 float 配置で accessoryHolder と重ならない

- **GIVEN** `CellBaseViews` を `ConstraintLayout` で構築し、`SwitchCellViewHolder` で `applyCellBaseLayout(views, title = "通知", hintText = "推奨", ...)` を呼び `accessoryHolder` に `MaterialSwitch` を追加して描画した状態
- **WHEN** 実機・エミュレータでセルをレイアウトして座標を取得する
- **THEN** `hintTextView` の `top` は `root.top` から数 dp（マージン分）のオフセットで配置され、`hintTextView` の `right` は `root.right` から数 dp のオフセットで配置される。`accessoryHolder` の縦中央 Y 座標は `root` の縦中央付近で、`hintTextView` の bottom よりも下にある（通常 hint テキスト 1 行分の高さ程度のクリアランスが空く）。両者は物理的に重ならない

#### Scenario: MinHeightConstraintLayout が minimumHeight を measure に反映する

- **GIVEN** `MinHeightConstraintLayout` を `minimumHeight = 60dp 相当 px` で構築し、子要素として高さ 10dp 相当の TextView を 1 個だけ持たせる
- **WHEN** 親から `widthSpec = EXACTLY 400dp 相当 px` / `heightSpec = UNSPECIFIED` で `measure(...)` を呼ぶ
- **THEN** `measuredHeight == 60dp 相当の px` になる（標準 `ConstraintLayout` だと子要素の合計高さ 10dp 程度を返す場合があるが、`MinHeightConstraintLayout` は `onMeasure` 後に下限ガードする）

#### Scenario: MinHeightConstraintLayout は intrinsic 値を阻害しない

- **GIVEN** `MinHeightConstraintLayout` を `minimumHeight = 60dp 相当 px` で構築し、子要素として高さ 120dp 相当の TextView を `TOP=parent.TOP` で配置する
- **WHEN** 親から `heightSpec = UNSPECIFIED` で `measure(...)` を呼ぶ
- **THEN** `measuredHeight >= 120dp 相当 px` になる（intrinsic な測定結果が `minimumHeight` を超えるときは super 由来の値を維持し、上方向の伸縮を阻害しない）

#### Scenario: buildCellBaseViews の root は MinHeightConstraintLayout 実装である

- **GIVEN** `buildCellBaseViews(ctx)` を呼ぶ
- **WHEN** 返り値の `views.root` の Kotlin クラスを確認する
- **THEN** `views.root is MinHeightConstraintLayout == true`（標準 `ConstraintLayout` のままではない）

#### Scenario: Theme 未指定時に Cell の measuredHeight が 60dp 相当 px 以上になる

- **GIVEN** `SwitchCellViewHolder` を `Theme()`（デフォルト、`hasUnevenRows = true`）で bind し、root を `LinearLayoutManager` 相当の親から `heightSpec = UNSPECIFIED` で `measure(...)` する
- **WHEN** `views.root.measuredHeight` を確認する
- **THEN** `views.root.measuredHeight >= 60dp 相当の px`（`MinHeightConstraintLayout.onMeasure` の下限ガードが効いており、実機でも `Theme()` 既定の最低高さ保証 60dp が成立する）

#### Scenario: Compose 版 KsCellRow.kt が削除されている

- **GIVEN** 本 change 適用後の `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/` ディレクトリ
- **WHEN** `KsCellRow` / `KsCellRowLayout.kt` を grep / find する
- **THEN** ファイル自体が存在せず、`@Composable fun KsCellRow(...)` の定義も削除されている。プロダクションコード内に `import androidx.compose.runtime.Composable` を通じた共通行レイアウト Composable は存在しない

#### Scenario: ButtonCellViewHolder の aux 切替

- **GIVEN-1** `ButtonCell(title: "ログアウト", titleAlignment: .center, onTap: {...})`（`icon` / `valueText` / `hintText` すべて `null`）
- **WHEN-1** `ButtonCellViewHolder.bind(...)` が描画する
- **THEN-1** ボタンスタイルが選択され、`iconView` / `descriptionView` / `valueTextView` / `accessoryHolder` / `hintTextView` は全て `GONE`、`titleView` のみが Cell 全体に広がり、`titleAlignment = .center` により中央寄せで「ログアウト」が表示される
- **GIVEN-2** `ButtonCell(title: "登録", valueText: "送信", icon: KsImage.Resource(R.drawable.ic_send), titleAlignment: .start, onTap: {...})`
- **WHEN-2** `ButtonCellViewHolder.bind(...)` が描画する
- **THEN-2** 通常レイアウトが選択され、`applyCellBaseLayout` 経由で左端アイコン、`titleView`（左寄せ／`titleAlignment = .start` を title 列内 gravity に反映）、title 行右寄せに valueText「送信」が配置される
