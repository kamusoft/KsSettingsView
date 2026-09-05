---
type: reference
title: Android Native Host の利用と更新境界
description: SettingsRootStore と KsSettingsView を使って Android View の設定画面を構築・更新・拡張する方法
tags: [android, views, host, public-api]
timestamp: 2026-08-29
---

この文書は、Android View から KsSettingsView を使うための公開 API 利用契約と責務境界を整理した reference である。読むと、`SettingsRootStore` と `KsSettingsView` の役割、表示後の更新方法、独自 Cell の登録方法、ホスト側に前提が無いこと (テーマ・Activity 型) が分かる。Jetpack Compose から使う場合は [Android Compose Bridge と宣言 DSL](android-compose.md) を参照する。設定ツリーと差分の型自体は [SettingsRoot・Section・Cell の設定ツリー](../../core/core-model/settings-tree.md) と [SettingsRootDiff による構造変更](../../core/core-model/structural-changes.md) を先に読む。

## 目的

`jp.kamusoft.kssettingsview.ui.KsSettingsView` は Core の `SettingsRoot` を `RecyclerView` へ接続する公開 Host である。`SettingsRootStore` が hidden 要素を含む現在状態と更新通知を持ち、Host が visible projection、平坦な表示リスト、Cell 描画を担当する。

この Host は XML / Android View から直接利用できるほか、Compose の `AndroidView` と将来の外部バインディングから再利用される。Core モデルの定義、宣言ツリー同士の比較、Theme と CellStyle の実効値解決は Host の外側にある。

## 状態と所有者

| 用語 | 所有者 | 意味と担当する更新 |
|---|---|---|
| model | `SettingsRootStore` と Host の内部状態 | hidden 要素を含む完全な `SettingsRoot`。Store の公開操作で更新する |
| visible projection | `KsSettingsView` | model から表示対象だけを元の順序・IDで取り出した派生状態 |
| 表示リスト | `KsSettingsView` と内部 Adapter | Section Header / Footer と Cell を平坦化し、Root Header / Footer と接続した `RecyclerView` の行列 |
| Host | `KsSettingsView` | Store の状態と通知を visible projection・Adapter・Cell 描画へ接続する境界 |

## 公開 API

### SettingsRootStore

`SettingsRootStore(initialRoot, initialTheme)` は現在の `SettingsRoot` と `Theme` を読み取り専用 `StateFlow` として公開する。表示後の変更は次の公開操作を使う。

| 対象 | 操作 |
|---|---|
| Root 全体 | `replaceAll` |
| Section | `insertSection`、`removeSection`、`moveSection`、`replaceSection` |
| Cell | `insertCell`、`removeCell`、`replaceCell`、`replaceCells`、`moveCell` |
| Accessory | `updateAccessory` |
| Theme | `applyTheme` |

Section / Cell 操作の index は、非表示要素を含む model 配列上の位置である。挿入先と移動先は有効範囲へ clamp される。対象 ID が見つからない remove / move / replace、および挿入先 Section が見つからない Cell insert は、状態を変えず構造 Diff も発行しない。

`replaceCells` は存在する Cell だけを一回の状態更新にまとめ、同時に変更した Cell ID 群を一つの内容更新バッチとして流す。RadioCell の選択変更など、複数行を同時に再描画するときに使う。対象が一つなら `replaceCell` を使う。どちらも Cell ID を変更する操作ではない。

`applyTheme` は構造 Diff を発行しない。同値の Theme なら通知しない。購読開始時は一過性の通知ではなく `state.value` と `theme.value` から現在値を復元する。

### KsSettingsView

XML またはコードで `KsSettingsView` を生成し、`bind(store)` で Store へ接続する。`bind` は Store の現在 root と Theme を直ちに反映する。ViewTree に `LifecycleOwner` がまだない場合は Store を保持し、attach 後に購読開始を再試行する。

`style` は `Classic` / `Modern`、`rootHeader` / `rootFooter` は Root Header / Footer を表す `RootAccessory` である。Root Header / Footer は `SettingsRoot` に含まれず、`null` は対応行を表示しない。Section Header / Footer は各 `Section` の `SectionAccessory` から描画する。どちらも文字列と `KsAnyView.Compose` / `.AndroidView` の任意 View を利用できる。

通常の Store 方式では、Theme の唯一の正は `store.theme` である。初期値は `SettingsRootStore(initialTheme = ...)`、表示後の変更は `store.applyTheme(...)` を使う。`bind(store)` は、その前に `view.theme` へ直接設定した値を `store.theme.value` で上書きする。bind 後の `view.theme = ...` は View だけを一時的に変更して Store を更新せず、次の Store Theme 通知や再 bind で上書きされるため、Store 方式では使わない。

公開 `view.theme` は、外部バインディングや Preview が Store を使わず `view.applyDiff(SettingsRootDiff.Full(root))` から Host を直接駆動する場合の入口である。この高度な方式と `bind(store)` を同じ View で併用しない。

空の `SettingsRoot` も有効で、空の `RecyclerView` として表示できる。detach 時は Store 購読を停止し、内部 RecyclerView から Adapter 参照を切る (メモリリーク防止)。再 attach 時は Adapter を戻し、Store の現在状態を取り込み直してから購読を再確立する。ViewPager2 のオフスクリーンページや Compose `AndroidView` の付け外しのように View を作り直さず detach / attach するホストでも、detach 中の Store 更新を含む最新内容で復帰する。detach 中の更新が Diff として再送されるわけではない — Store の更新通知は replay を持たないため、購読停止中に発行された Diff は消え、復帰は `store.state` / `store.theme` の現在値から行われる。スクロール位置は復元対象外。

初回の attach でも同じ復元が働く — `bind(store)` 後・attach 前に Store へ適用した更新 (構造・Cell 内容・Section accessory・theme) は、attach 後に表示へ反映される。Host 生成・Store 操作・view 階層への取り付けの順序を利用側が意識する必要はない ([core/ADR-0019](../../../decisions/core/0019-host-restores-from-store-on-attach.md))。収束の観測境界は「attach 後、メインスレッドのキューが空になった時点」である (theme の collect 開始と `submitList` が非同期のため、`onAttachedToWindow` 完了時点の同期一致は保証しない)。`rootHeader` / `rootFooter` は Store の現在状態に含まれないため復元対象外で、所有者 (呼び出し側) が attach 後に適用する。

## model と表示の同期

Host は hidden 要素を含む model と、表示対象だけの visible projection を分けて保持する。visible projection は `Section.isVisible` と、Cell が `VisibilityAware` の場合の `isVisible` から作る。`VisibilityAware` に準拠しない独自 Cell は visible として扱う。

| 変更 | Android の反映 |
|---|---|
| Section / Cell の追加・削除・移動 | 現在 model を平坦化し、`ListAdapter.submitList` へ渡す |
| 同じ ID の Cell 内容更新 | item identity を保ち、`submitList` の反映完了後に対象 ViewHolder を再 bind する |
| 複数 Cell の連動内容更新 | 一回の `submitList` の反映完了後に対象行をまとめて再 bind する |
| 可視性の変更 | model から visible projection を再構築する |
| Theme の変更 | 構造を変えず、背景・行・Accessory・装飾を再評価する |

stable item ID は行種別と Section / Cell ID から決まり、title、選択値、style などの内容を含めない。同じ ID の内容更新には `replaceCell` / `replaceCells` を使い、ID 自体を変える場合は remove + insert で表す。

hidden な Section / Cell は model から削除しない。hidden 対象への更新は model に保持され、再表示時に更新済みの値が現れる。部分操作の index を visible projection の位置として渡してはならない。

## Cell Renderer Registry

`KsCellRegistry` は具象 `Cell` 型を `viewType` と `CellViewHolder` factory へ対応付ける。Host は Registry から型を解決して `bind(cell, theme)` を呼ぶため、独自 Cell を追加しても Host に型分岐を加えない。

標準 Cell 12 種は Host の構築時に自動登録される。利用者定義 Cell は表示前に登録し、Root / Section Accessory の予約値を避けるため `KsCellRegistry.CELL_VIEW_TYPE_MIN` 以上の `viewType` を使う。

```kotlin
KsCellRegistry.strictMode = BuildConfig.DEBUG

KsCellRegistry.register(
    cellClass = MyCell::class,
    viewType = KsCellRegistry.CELL_VIEW_TYPE_MIN + 50,
) { parent ->
    MyCellViewHolder(parent)
}
```

同じ Cell 型の再登録は後勝ちで factory を置き換える。別の Cell 型に同じ `viewType` を割り当てると失敗する。未登録 Cell は `strictMode == true` で例外として早期検出し、`false` では高さ0の placeholder へ退避する。`strictMode` の既定値は `true` であり、アプリの build 種別へ自動追従しない。

独自 `CellViewHolder` は bind ごとに最新 Cell と Theme を反映し、再利用時の `reset()` で listener、Job、画像、埋め込み View などを解放する。内容更新のたびに同一 ViewHolder へ `bind` が再実行されるため、フォーカス中の入力 View の IME 接続を再起動させる属性 (`EditText.inputType` など) は値が変わったときだけ適用する — 無条件に再代入すると入力中の未確定文字列が確定される ([android/ADR-0001](../../../decisions/android/0001-content-update-preserves-viewholder.md))。

## スタイルと視覚状態

画面全体の既定値は `Theme`、単一 Cell の上書きは `CellStyle` が持つ。通常属性は `CellStyle`、Theme、Android / Material の既定値の順で解決する。`Theme.backgroundColor` が RecyclerView の canvas、`Theme.cellBackgroundColor` が Cell の既定背景、`CellStyle.backgroundColor` が個別 Cell の背景であり、互いに代用しない。

Cell 個別高さは Theme の行高さより優先され、Android の最終行高は60dpを下回らない。`Theme.hasUnevenRows == true` では内容に応じて伸び、`false` では解決済み高さへ固定する。無効化は Cell initializer の `isEnabled` で指定し、無効時は Theme の disabled text 色と Native control の disabled 表現を使う。

Theme 属性の未指定時に使われるライブラリ既定値は、`Theme` companion の public 定数として公開される。利用者は「既定へ戻す」「既定値を基準に派生値を作る」用途でこれらを参照できる。

| 定数 | 既定値の対象 |
|---|---|
| `DEFAULT_SEPARATOR_COLOR` | 罫線色 |
| `DEFAULT_SELECTED_COLOR` | 選択中背景色 |
| `DEFAULT_ACCENT_COLOR` | アクセント色 |
| `DEFAULT_BACKGROUND_COLOR` | list 背景色 |
| `DEFAULT_DISABLED_TEXT_COLOR` | 無効時テキスト色 |
| `DEFAULT_HEADER_BACKGROUND_COLOR` | Header 背景色 |
| `DEFAULT_FOOTER_BACKGROUND_COLOR` | Footer 背景色 |
| `DEFAULT_HEADER_TEXT_COLOR` | Header テキスト色 |
| `DEFAULT_FOOTER_TEXT_COLOR` | Footer テキスト色 |
| `DEFAULT_CELL_TITLE_COLOR` | Cell タイトル色 |
| `DEFAULT_CELL_DESCRIPTION_COLOR` | Cell 説明文色 |
| `DEFAULT_BUTTON_TITLE_COLOR` | ButtonCell タイトル色 |
| `DEFAULT_CELL_ICON_SIZE_DP_VALUE` | icon サイズ (dp 値) |
| `DEFAULT_CELL_ICON_RADIUS_DP_VALUE` | icon 角丸半径 (dp 値) |

`Classic` は Cell へ1物理 pixelの hairline を描き、Section 内の中間線だけ左16dp inset とする。`Modern` は Theme の Section 装飾4属性 (`sectionMargin` 等。未指定はライブラリ既定) に従い、Section の Cell のみを角丸背景・Border の Container でまとめ、Section H/F 行は Container の外に置く ([設定 list の外観と補助領域](../../core/styling/list-appearance.md))。Style の切替は model、stable ID、Registry を変えない。Theme の変更時は現在の Style の装飾も再構築される。

## ホストのテーマと Activity 型 (前提なし)

Host はライブラリ同梱の Material3 派生テーマ (DayNight) でラップした Context から自前の UI を生成するため、ホストアプリの XML テーマに前提はない — 最小構成のテーマ・AppCompat 系・MAUI テンプレート既定 (`Maui.SplashTheme`) のいずれでも、全 Cell と選択面が例外なく表示・動作する ([android/ADR-0020](../../../decisions/android/0020-bundled-theme-always-wrap-host-independent.md))。ホストテーマの色 (カスタム色・dynamic color を含む) はライブラリ UI へ反映されず、見た目の調整はライブラリの `Theme` / `CellStyle` で行う。利用者所有コンテンツ (CustomCell の content・`KsAnyView` 経由の利用者 View) は隔離の対象外で、従来どおりホストの Context (ホストテーマ) で解決される。

ライト / ダークは端末の夜間モードとアプリの uiMode 制御 (`AppCompatDelegate.setDefaultNightMode` / `UiModeManager.setApplicationNightMode`) で決まる。ホストが XML テーマで Dark 系を明示するだけの指定は反映されない ([スタイルの所有と実効値解決](../../core/styling/style-resolution.md))。

ホスト Activity の型にも前提はない — `ComponentActivity` を含む任意の Activity で、`TimePickerCell` / `DatePickerCell` の選択面を含む全 Cell が動作する。FragmentActivity / FragmentManager への依存は存在しない ([android/ADR-0018](../../../decisions/android/0018-timepickercell-bottom-sheet-wheel-unification.md) / [android/ADR-0019](../../../decisions/android/0019-datepickercell-calendar-compose-datepicker.md))。

## カレンダー選択面の回転復元

`DatePickerCell` (uiStyle `Material`) のカレンダーダイアログは、`KsSettingsView` の View インスタンス状態で回転をまたいで表示継続する ([android/ADR-0021](../../../decisions/android/0021-calendar-dialog-restore-via-view-instance-state.md))。復元が効く条件は次の2つ:

- **再生成の前後で `Cell.id` が同じであること**。明示 id の指定、または Compose DSL の identity hint 由来の安定 ID で成立する。再生成時に Cell を再構築すると既定のランダム id は変わり、成立しない。回転復元を効かせたい画面では安定 id を推奨する
- **View インスタンス状態の保存先が一意であること**。`KsSettingsView` は ID 未設定のときライブラリ既定 ID を自前付与するため通常は追加作業なしで成立するが、既定 ID のインスタンスが同一階層に複数ある構成では保存先が衝突するため復元しない — ホストが個別の ID を与えれば成立する

条件を満たさない場合は再表示せず、別の Cell へ確定値が書き込まれることはない — 復元できないときは常に閉じる側へ倒れる。構成変更を in-place で処理するホスト (MAUI テンプレート既定等) では Activity 再生成自体が起きず、ダイアログは開いたまま生存する。ボトムシート系の選択 UI (PickerCell / NumberPickerCell / DatePickerCell (Spinner) / TimePickerCell) は回転で閉じる挙動のままで、この復元の対象外である。挙動契約の全体は [DatePickerCell の選択面](../../core/cells/date-picker-selection-surface.md) を正とする。

## 保証すること

- 初期状態と後続更新は同じ `SettingsRootStore → KsSettingsView` 経路へ流れる。
- Root / Section Accessory が空なら意味のない行を生成しない。
- Theme 更新を SettingsRoot の構造変更として扱わない。
- Registry の解決後は Cell 固有の bind / reset を ViewHolder へ委譲する。
- Cell の内容更新と可視性変更を別の表示同期経路へ流す。
- 同じ ID の内容更新は ViewHolder を再生成せず同一 ViewHolder への再 bind として届く。フォーカスや IME の未確定文字列 (composing) を破壊しない ([android/ADR-0001](../../../decisions/android/0001-content-update-preserves-viewholder.md))。
- **フォーカス中の EntryCell 入力欄は値の SSoT** ([android/ADR-0014](../../../decisions/android/0014-entrycell-focused-editor-owns-text.md))。同一 Cell への内容更新はフォーカス中の text とキャレットを差し替えず、フォーカス喪失時に最後にバインドされた `cell.text` へ再同期する (再同期は `onTextChanged` を発火させない)。書き戻しの往復より速い連続入力でも欠落・並び替えは起きない。裏返しの利用側契約: `onTextChanged` を受けて `cell.text` を更新しない構成では、フォーカス喪失時に入力欄が最後の bind 値へ戻る — 値 + callback 経路の利用側は callback を受けて `cell.text` を更新すること。
- カレンダー選択面は Activity 再生成後、安定 id と一意な保存先の構成なら選択状態 (選択日・表示月・表示モード) を維持して再提示され、配色・今日ジャンプ・確定/破棄の契約も有効なまま復元される。対応付けできない場合は再表示せず、別の Cell へ値を書き込まない ([android/ADR-0021](../../../decisions/android/0021-calendar-dialog-restore-via-view-instance-state.md))。
- detach → 再 attach をまたいでも表示は Store の現在値と一致して復帰する。detach 中に発行された Store 更新も、再 attach 時の Store 現在状態の取り込み直しにより失われない (スクロール位置は保証しない)。
- `bind` から attach までの間の Store 更新も、attach 後にメインスレッドのキューが空になった時点までに表示へ収束する (取り付け順序に依存しない。[core/ADR-0019](../../../decisions/core/0019-host-restores-from-store-on-attach.md))。

## してはいけないこと

- Host の内部 root や module-internal の `setRootDirect` を利用者コードから操作しない。
- Theme 更新を `SettingsRootDiff` に混ぜない。
- Cell 具象型ごとの分岐を `KsSettingsView` へ追加しない。
- hidden 要素を model から削除して可視性を表現しない。
- Diff の index を visible projection 上の位置として渡さない。
- Cell の内容値を stable item ID に含めない。
- 利用者定義 Cell の `viewType` に100未満の予約領域を使わない。

## 利用例

```kotlin
import androidx.compose.ui.graphics.Color
import jp.kamusoft.kssettingsview.core.Section
import jp.kamusoft.kssettingsview.core.SectionAccessory
import jp.kamusoft.kssettingsview.core.RootAccessory
import jp.kamusoft.kssettingsview.core.SettingsRoot
import jp.kamusoft.kssettingsview.ui.KsSettingsView
import jp.kamusoft.kssettingsview.ui.LabelCell
import jp.kamusoft.kssettingsview.ui.SettingsRootStore
import jp.kamusoft.kssettingsview.ui.Theme

val section = Section(
    id = "general",
    header = SectionAccessory.Text("一般"),
    cells = listOf(LabelCell(title = "バージョン", valueText = "1.0.0")),
)
val store = SettingsRootStore(
    initialRoot = SettingsRoot(sections = listOf(section)),
    initialTheme = Theme(),
)
val updatedTheme = Theme(cellTitleColor = Color.DarkGray)

findViewById<KsSettingsView>(R.id.settings_view).apply {
    rootHeader = RootAccessory.Text("プロフィール")
    bind(store)
}
store.insertCell(
    cell = LabelCell(title = "ライセンス"),
    sectionId = section.id,
    at = section.cells.size,
)
store.applyTheme(updatedTheme)
```

## 関連

- [Android Compose Bridge と宣言 DSL](android-compose.md)
- [SettingsRoot・Section・Cell の設定ツリー](../../core/core-model/settings-tree.md)
- [SettingsRootDiff による構造変更](../../core/core-model/structural-changes.md)
- [基本 Cell](../../core/cells/basic-cells.md)
- [入力 Cell](../../core/cells/input-cells.md)
- [KsImage](../../core/cells/ks-image.md)
