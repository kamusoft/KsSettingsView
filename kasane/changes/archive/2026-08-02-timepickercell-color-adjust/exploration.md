# Exploration: timepickercell-color-adjust

## 課題 / 動機

Android 版 TimePickerCell の時刻選択ダイアログ (MaterialTimePicker) が、キーボード入力 UI / 時計文字盤 UI とも Material 規定の配色のままで、KsSettingsView のテーマ色が反映されない。要望のマッピング:

- 背景 ← backgroundColor
- 選択枠・キャレット・OK/キャンセル ← accentColor
- その他文字色 ← titleColor

現状はサンプルアプリで OK/キャンセルだけ Activity テーマの colorPrimary (ピンク) を拾い、文字盤・針・選択枠が既定の紫のままという混色状態 (ui/references/ 参照)。

## 現状把握 (コード)

- ダイアログ生成: `android/ks-settingsview-ui/.../TimePickerCellViewHolder.kt` の `showTimePicker()` (`MaterialTimePicker.Builder`)。色設定は一切なし
- `TimePickerCell.accentColor` プロパティは定義済みだが ViewHolder 側で**未接続**
- テーマ側は `Theme.kt` に `cellAccentColor` / `backgroundColor` / `cellTitleColor` が定義済み。ただし `showTimePicker()` に theme が渡っていない (シグネチャ変更が必要)
- Compose 系統 (`InputCellDsl.kt` の `TimePickerCell` DSL) も内部では同じ Native ダイアログを呼ぶため、直す場所は実質 1 系統
- 先行事例: DatePickerCell の Spinner モードのみ `AlertDialog.getButton().setTextColor()` でボタン文字色を動的反映 (`androidButtonColor`)。Material 系ダイアログ本体への動的配色の前例は無し

## 検討した選択肢 (却下案と理由を含む)

1. **静的テーマバリアント切替 (`Builder.setTheme`)** — 却下: 静的 style リソース専用で任意の実行時 Color に対応できない
2. **届く範囲だけ着色 (背景・ボタン・ヘッダのみ)** — 却下: 枠・キャレット・文字盤が紫のまま残り混色が解消されない
3. **自前ダイアログへの置き換え** — 却下 (温存): 2 モード UI の再現工数が大。走査方式が将来破綻した場合の最後の砦
4. **`android.widget.TimePicker` / NumberPicker 埋め込み** — 却下: NumberPicker の色変更は公開 API 無し・リフレクション必須
5. **DynamicColors (`setContentBasedSource`)** — 却下: API 31+ 限定・Activity テーマ全体書き換えの副作用・任意色がそのまま出ない
6. **【採用】MaterialTimePicker 維持 + 表示後の内部 View 走査で動的着色** — ADR-0006 参照

## 調査結果の要点 (ksn-scout / ksn-researcher による裏取り)

- MaterialTimePicker に実行時カラー注入の公式 API は無い (1.12.0 ソース精読 + 公式リファレンス + issue #2091 で確定)
- 内部 View 走査でほぼ全部位に到達可能。技が要るのは 2 箇所:
  - 針・ノブ・中心ドット: 色 setter 無し → `setLayerType` + `PorterDuffColorFilter(SRC_IN)` (針 View は単色描画のみなので純粋な色置換になる)
  - 文字盤の数字: 操作のたびにライブラリが塗り戻し + 選択数字は shader 塗り潰し → `OnPreDrawListener` で冪等再適用 + shader クリア
- キーボード側 View は ViewStub で**モード切替時に遅延生成** → 再適用フック必須 (pre-draw フックが兼ねる)
- 24 時間フォーマット時は**既定でキーボードモード起動** (`CLOCK_24H` → `INPUT_MODE_KEYBOARD`)
- ID 重複あり (AM/PM トグルが両モードに同 ID) → findViewById 単発禁止、全走査方式
- CSL 状態キー: チップ・文字盤数字=`state_selected` / AM-PM=`state_checked`
- キャレット: minSdk 29 なので `textCursorDrawable` tint が無条件で使える。`EntryCellViewHolder.kt:149-165` に同手法の実装済み前例
- ダイアログ背景: `window.decorView.background as MaterialShapeDrawable` の `fillColor` 差し替えで角丸維持
- 内部 ID は aapt2 上 private (public.txt に id 0 件) → lint `PrivateResource` の抑制が必要
- master との差分照合済み: ID・階層・塗り戻し挙動は同一。近々壊れる兆候なし
- 実装構造の素案: `TimePickerColorizer` ヘルパー 1 クラスに全て閉じ、ViewHolder からは attach 1 行

## 決定事項

- 案1 (MaterialTimePicker 維持 + 内部 View 走査) を採用 → ADR-0006 起票 → accepted 昇格済み
- 実装は TimePickerColorizer 相当のヘルパーに隔離する
- ダイアログ背景は `Theme.backgroundColor` (モック A案) を採用。運用で背景色の競合が問題になったら、その時点でダイアログ背景の独立プロパティ追加を検討する (オーナー方針、2026-08-02)
- アクセント上の文字色は輝度による白/黒自動選択 (未決論点 1 の解決)
- アクセント色の解決順は `TimePickerCell.accentColor` → `CellStyle.accentColor` → `Theme.cellAccentColor` (未決論点 2 の解決。デルタスペックに契約として記載)

## ADR 候補

- 作成済み: android/ADR-0006 (proposed — オーナー確認待ち)

## 未決の論点

1. **accent 上に載る文字色** (選択チップの文字・選択中の文字盤数字・ノブ上の数字) をどう決めるか — titleColor 固定か、accentColor との自動コントラストか、Theme に onAccent 相当を足すか
2. **色の解決順序** — `TimePickerCell.accentColor` (セル単位) と `Theme.cellAccentColor` (テーマ) のフォールバック関係。`EffectiveStyle` の既存解決ロジックに載せるか
3. **実機検証 3 点** (実装フェーズ冒頭で最小検証必須): 針の ColorFilter 描画 / pre-draw 再適用のちらつき有無 / キーボード入力欄枠の駆動 state
4. **横展開のスコープ** — DatePickerCell (MaterialDatePicker / カレンダー) も同じ問題を持つはず。本変更は TimePickerCell に限定し、横展開は別変更とするのが妥当か
5. ダークテーマ時の elevation overlay で背景指定色がわずかに明るくなる件の扱い

## UI 素材 (ui/references/ の一覧と注釈)

- `current-keyboard-input-ui.webp` — 現状のキーボード入力 UI (24h 起動時の既定モード)。選択枠・キャレットが紫、ボタンはピンク
- `current-clock-input-ui.webp` — 現状の時計文字盤 UI。文字盤・針・ノブ・時刻チップが紫のまま
- いずれも「修正前」の実機証跡。目標状態のモックは ksn-propose で作成する

## 変更級の推奨: M (理由)

- 触るのは TimePickerCell 1 系統 + 新規ヘルパー 1 クラスで範囲は狭いが、外部ライブラリ内部構造への依存という質的リスクがあり、実機検証ループ (3 点) を検証項目として明文化したい
- UI の見た目が変わる変更であり、目標配色のモック承認ゲートを通すべき
- 公開 API 変更は小 (既存 `accentColor` の接続 + テーマフォールバック) だが、色解決順序という契約の論点が残っている
- S だと提案・デルタスペック無しで実装に入ることになり、上記の未決論点と検証項目が口頭合意のまま消えるリスクがある。「迷ったら 1 段上」の原則にも従い M を推奨
