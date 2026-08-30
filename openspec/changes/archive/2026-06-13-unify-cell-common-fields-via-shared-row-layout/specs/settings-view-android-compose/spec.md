## ADDED Requirements

### Requirement: 共通行レイアウト関数 applyCellBaseLayout（View ベース）

`ks-settingsview-ui`（Android）は、全 CellViewHolder が共通して使う **View ベースの行レイアウト関数 `applyCellBaseLayout(views, ...)`** を `internal` 可視性で提供しなければならない (SHALL)。この関数は `cell-types-basic` の「全 Cell 共通の description / valueText / icon / hintText フィールド」Requirement で規定された 2 系統のレイアウト規約（本体行 `[icon][title / description][valueText (title 行の右寄せ)][accessory (右側中央)]` + `hintText` の右上 float 配置）を `ConstraintLayout` ベースの `CellBaseViews` 構造体に対して反映する責務を持つ。

本 Requirement では、**Compose（`androidx.compose.runtime`）を用いない** ことを明確に定める (MUST NOT)。すなわち、ViewHolder の `bind(...)` 内で `ComposeView.setContent { ... }` を呼び出して `KsCellRow` Composable を組む実装方式は採用してはならない (MUST NOT)。理由はオリジナル `AiForms.Maui.SettingsView` の Android 実装が `RelativeLayout` ベースの View ヒエラルキーで構成されており、本 change の目的は「共通フィールドの単一化」であって UI 実装の Compose 化ではないこと、また MAUI 移植も視野に入れたパフォーマンス・互換性の観点で View ベースが優位なことによる（`design.md` Decision 11 参照）。

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

#### ConstraintLayout 配置規約

`CellBaseViews` のルートは `ConstraintLayout` でなければならず、内部 View の制約は以下の構造を満たさなければならない (MUST)：

- **iconView**: 左端中央 — `Start=parent.Start`, `Top=parent.Top`, `Bottom=parent.Bottom`（CenterVertical）
- **titleView**: icon の右、accessoryHolder の左、上端 — `Start=iconView.End`, `Top=parent.Top` (セル上端パディング分のマージン), `End=accessoryHolder.Start`
- **descriptionView**: icon の右、accessoryHolder の左、title の下 — `Start=iconView.End`, `Top=titleView.Bottom`, `End=accessoryHolder.Start`, `Bottom=parent.Bottom` (セル下端パディング分のマージン)
- **valueTextView**: title 行の右寄せ — `End=accessoryHolder.Start`, `Baseline=titleView.Baseline`（title と同じ行で右端に配置）
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

- **GIVEN** `SwitchCellViewHolder` の bind 内で `applyCellBaseLayout(views, title = "通知", description = "プッシュ通知", valueText = "オン", icon = KsImage.Resource(R.drawable.ic_bell), hintText = "推奨", effective = effective, isEnabled = true)` を呼び、その後 `views.accessoryHolder.addView(materialSwitch)` を呼ぶ
- **WHEN** Cell が表示される
- **THEN** 本体行は左端にベルアイコン（CenterVertical）、その右に「通知」「プッシュ通知」が縦並び、title 行右端寄せに「オン」、右端中央に `MaterialSwitch`（ON 状態）が配置される。`hintText` 「推奨」はセル右上に float 表示され、`MaterialSwitch` とは上端 vs 縦中央で位置が分かれるため通常は重ならない

#### Scenario: 各 ViewHolder が applyCellBaseLayout を経由する

- **GIVEN** `ks-settingsview-ui` ソース内の `LabelCellViewHolder.kt` / `CommandCellViewHolder.kt` / `SwitchCellViewHolder.kt` / `CheckboxCellViewHolder.kt` / `RadioCellViewHolder.kt` / `SimpleCheckCellViewHolder.kt` / `ButtonCellViewHolder.kt`
- **WHEN** これらのファイルから `bind(...)` の本体を grep する
- **THEN** 各 ViewHolder は `applyCellBaseLayout(views, ...)` を呼び出しており、テキスト反映（`textView.text = ...`）・色反映・フォント反映・visibility 制御を各 ViewHolder 内で個別に書いている箇所はない。各 ViewHolder 内に残るのは「accessoryHolder への trailing コントロールの addView」と Cell 種別固有のイベントハンドラ（`OnCheckedChangeListener` 等）のみである

#### Scenario: applyCellBaseLayout が internal 可視性

- **GIVEN** `ks-settingsview-ui` の外部モジュール（例: `ks-settingsview-core` / サンプルアプリ / 後続 change で追加される未来の Cell）
- **WHEN** `import jp.kamusoft.kssettingsview.ui.applyCellBaseLayout` 後に直接呼び出そうとする
- **THEN** `internal` 可視性のためコンパイルエラーになる（同モジュール内からは呼べる）

#### Scenario: hintTextView は右上 float 配置で accessoryHolder と重ならない

- **GIVEN** `CellBaseViews` を `ConstraintLayout` で構築し、`SwitchCellViewHolder` で `applyCellBaseLayout(views, title = "通知", hintText = "推奨", ...)` を呼び `accessoryHolder` に `MaterialSwitch` を追加して描画した状態
- **WHEN** 実機・エミュレータでセルをレイアウトして座標を取得する
- **THEN** `hintTextView` の `top` は `root.top` から数 dp（マージン分）のオフセットで配置され、`hintTextView` の `right` は `root.right` から数 dp のオフセットで配置される。`accessoryHolder` の縦中央 Y 座標は `root` の縦中央付近で、`hintTextView` の bottom よりも下にある（通常 hint テキスト 1 行分の高さ程度のクリアランスが空く）。両者は物理的に重ならない

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
