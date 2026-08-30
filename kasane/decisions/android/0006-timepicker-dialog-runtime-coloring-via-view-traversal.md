---
id: 0006
title: TimePicker ダイアログの動的配色は MaterialTimePicker を維持し表示後の内部 View 走査で行う
status: superseded
date: 2026-08-02
---

## Context

Android 版 TimePickerCell の時刻選択ダイアログ (MaterialTimePicker、material 1.12.0) は、キーボード入力 UI / 時計文字盤 UI とも Material 規定の配色のままで、KsSettingsView のテーマ色 (accentColor / backgroundColor / titleColor) が反映されない。サンプルアプリでは OK/キャンセルだけ Activity テーマの colorPrimary を拾い、文字盤・針・選択枠は既定の紫のままという混色状態になっていた (ui/references/ の実機スクショ参照)。

要件は「アプリが実行時に持つ**任意の** Color 値」の反映 (背景=backgroundColor、選択枠・キャレット・OK/キャンセル=accentColor、その他文字=titleColor)。しかし MaterialTimePicker の配色は静的な XML テーマ属性で決まる設計であり、実行時カラー注入の公式 API は存在しない:

- `Builder.setTheme(@StyleRes int)` はコンパイル済み style リソース専用 (1.12.0 ソースで確認)
- OK/キャンセルを個別スタイリングする API すら無い (material-components-android feature request #2091 が未対応)

一方、1.12.0 の sources.jar / aar の全文精読 (master との差分照合込み) により、内部 View の ID (`material_timepicker_ok_button` / `material_clock_face` 等) が R.id に存在し、表示後にプログラム的に色を上書きする経路がほぼ全部位で成立することを確認した。壊れやすい 2 部位への対処も机上確定済み: 針・ノブ (色 setter 無し → `setLayerType` + `PorterDuffColorFilter(SRC_IN)` による単色置換)、文字盤の数字 (ライブラリが操作のたびに塗り戻し・選択数字を shader で塗る → `OnPreDrawListener` での冪等再適用 + shader クリア)。なお `TimePickerCell` には `accentColor` プロパティが定義済みだが ViewHolder 側で未接続だった。

## Decision

MaterialTimePicker を維持し、ダイアログ表示後に内部 View を走査して実行時テーマ色をプログラム的に上書きする。実装は次の方針で単一ヘルパー (TimePickerColorizer 相当) に閉じる:

1. `FragmentLifecycleCallbacks` (onFragmentViewCreated) で着色フックを取り、window 背景は `MaterialShapeDrawable.fillColor` 差し替えで角丸を維持する
2. ID 重複 (AM/PM トグルが時計/キーボード両モードに同 ID で存在) のため findViewById 単発ではなく**全走査方式**で塗る
3. キーボード側 View の遅延生成 (ViewStub) と文字盤数字の塗り戻しに対し、`OnPreDrawListener` による冪等再適用 (shader クリア込み) で追随する。再適用は「View ごと 1 回の静的適用」と「文字盤数字だけの動的適用」に分離する — 走査内容を毎フレームそのまま適用すると `setBoxStrokeColorStateList` 等が無条件に再描画を要求し続け描画が 60fps で回るため (2026-08-02 実機計測による補正)
4. 針・ノブ・中心ドットは `setLayerType` + `PorterDuffColorFilter(SRC_IN)` で色置換する (リフレクション不使用)
5. キャレットは `TextInputLayout.cursorColor` で着色する (2026-08-02 実機検証による補正。当初案の API 29 `textCursorDrawable` への直接 tint は、`TextInputLayout` が状態変化のたびに `cursorColor` — 未指定時は `colorControlActivated` — で `textCursorDrawable` を塗り直すため上書きされて効かない)
6. ColorStateList の状態キーは部位ごとに異なる (チップ・文字盤数字=`state_selected` / AM-PM=`state_checked`。キーボード入力欄の枠は `state_selected` ではなく内部 EditText のフォーカス `{state_focused, state_enabled}` で駆動される — 2026-08-02 実機検証による補正) ため定数化して管理する

## Alternatives Considered

- **静的テーマバリアント切替 (`Builder.setTheme`)** — 却下。事前定義した有限パターンの切り替えしかできず、「任意の実行時 Color」という要件を満たせない
- **届く範囲だけ着色 (背景・ボタン・ヘッダ文字のみ)** — 却下。枠・キャレット・文字盤が規定の紫のまま残り、現状の混色状態 (ボタンだけピンク・文字盤は紫) が解消されない
- **自前ダイアログへの置き換え** — 却下。完全制御できるが、時計/キーボードの 2 モード UI を自前再現する工数が大きい。View 走査方式が Material アップグレードで破綻した場合の最後の砦として温存する
- **`android.widget.TimePicker` / NumberPicker の AlertDialog 埋め込み** — 却下。NumberPicker の文字色等に公開 API が無く、private フィールドへのリフレクションが必須で、View 走査方式より保守リスクが高い
- **DynamicColors (`setContentBasedSource`)** — 却下。API 31+ 限定で、Activity テーマ全体を書き換える副作用があり、トーナルパレット生成を経るため指定色がそのまま出る保証もない

## Consequences

- 正: 要望マッピング (背景 / 枠・キャレット・ボタン・針 / 文字) を任意の実行時 Color で完全に実現できる
- 正: 走査・再適用ロジックがヘルパー 1 クラスに隔離され、ViewHolder からは attach 1 行で済む
- 負: material-components の内部実装 (private な R.id・View 階層・針 View の単色描画前提) に依存する。ライブラリを 1.12.0 から上げる際は追随確認が必須 (master 時点では構造同一を確認済み)
- 負: 内部 ID の参照で lint `PrivateResource` が出るため抑制が必要
- 負: 選択数字のグラデーション遷移演出が失われ、色がスナップ切替になる (M3 既定の見た目との微差)
- 正: 机上確定のみだった 3 点 (針の ColorFilter 描画 / pre-draw 再適用のちらつき / 入力欄枠の駆動 state) は実装フェーズ冒頭のスパイクで実機検証済み。針の色置換は成立、pre-draw は静的/動的分離により素の描画頻度 (キャレット点滅由来の 2〜3 回/秒) を維持、枠の駆動源は Decision 6 のとおり補正 (2026-08-02)
- 負: ライブラリを 1.12.0 から上げる際の追随確認項目: 内部 R.id と View 階層の同一性 / 針 View が単色描画のみであること / `ClockHandView` が自前で layerType を変えないこと (着色側は既に `LAYER_TYPE_HARDWARE` の View をスキップするガードを持つため、ここが変わると針が既定色のまま残る)

出典: kasane/changes/archive/2026-08-02-timepickercell-color-adjust/exploration.md (検討した選択肢・調査結果) / 同 ui/references/ (現状の実機スクショ) / 同 ui/brief.md「実機で判明した material-components の挙動」・review-002.md (2026-08-02 実装検証による補正)
現行照合: 2026-08-02 確認。TimePickerColorizer.kt・TimePickerColors.kt・TimePickerCellViewHolder.kt と照合し、Decision 1〜6 が実装と一致 (3・5・6 は実機検証による補正を反映)。判定: 維持
