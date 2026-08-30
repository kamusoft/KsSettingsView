# Exploration: datepickercell-color-adjust

## 課題 / 動機

Android 版 DatePickerCell (カレンダーモード、`MaterialDatePicker` / material 1.12.0) のダイアログに 2 つの問題がある:

1. **ヘッダの重なり**: `setTitleText` のタイトル (「予約日を選択」) と選択日テキスト (「2026年6月1日」) が上下に重なり、タイトルがほぼ読めない
2. **テーマ非追従の配色**: OK/キャンセルだけ Activity テーマ色を拾い、他は Material 既定色のままの混色状態。KsSettingsView のテーマを踏襲させたい

配色の考え方は archive/timepickercell-color-adjust (TimePickerCell) と同じでよい、がユーザー指示。同 proposal の Non-Goals でも「DatePickerCell への横展開は別変更」と予告されていた変更にあたる。

## 調査結果 (2026-08-02、ksn-scout 委譲)

- 重なりの構造要因: `mtrl_picker_header_dialog.xml` はタイトルと選択日を**同一 FrameLayout に重ね置き**し、ベースライン 28dp / 100dp の固定 dp オフセットだけで縦に積む。選択日側は `textAppearanceHeadlineLarge` + `autoSizeTextType="uniform"`。日本語グリフの大きい描画ボックスで上に食い込むというのが最有力仮説 (**中自信度** — 実装冒頭スパイクで実機確定する)
- material-components の既知 issue に完全一致なし (同種の設計限界の傍証として issue #3642)
- サンプルのテーマは `Theme.Material3.DayNight.NoActionBar` で `materialCalendarTheme` は解決済み。「テーマ未解決」は原因ではない
- `DatePickerCell.accentColor` は定義済み・未接続 (DatePickerCell.kt:44)。TimePickerCell 変更前と同じ状態
- `MaterialTimePicker` で同種の重なりが起きないのは、巨大な選択値をタイトルと同一コンテナに重ねる構造を持たないため

## 検討した選択肢 (却下案と理由を含む)

レイアウト修正の方式:

- **A 案 (採用): DatePickerColorizer (仮) の View 走査フックにレイアウト補正も同居** — 配色でどのみち ADR-0006 方式の走査フックを作るので、同フックでヘッダ TextView の `firstBaselineToTopHeight` / テキストサイズを補正。新規リソースゼロ、ヘルパ 1 クラスに閉じる
- **B 案 (却下): カスタム ThemeOverlay + `setTheme`** — inflate 時点で効く安心感はあるが、ライブラリ res へのスタイル追加・依存の増加・レイアウトだけ別機構になる
- **選択日フォーマット短縮 (却下)** — 対症療法。構造要因が残る
- **material-components バージョンアップ確認 (追わない)** — 1.12.0 固定は ADR-0006 の前提。スコープ外

## 決定事項

- レイアウト修正は A 案 (走査フックで補正) — ユーザー確定 (2026-08-02)
- 配色は TimePickerCell と同じ考え方: ADR-0006 の走査方式 + `TimePickerColors` / `ColorRoles` の色ロール導出規則を再利用、`DatePickerCell.accentColor` を接続
- スコープ想定 (TimePicker 変更との対称性から。propose 時に最終確認):
  - カレンダー UI + 鉛筆アイコンで切り替わるテキスト入力モードも配色対象に含める
  - `DatePickerUIStyle.Spinner` (AlertDialog 系) は対象外

## ADR 候補

- 作成済み: android/ADR-0008 (accepted、2026-08-02 ユーザー承認) — DatePicker ダイアログの動的配色とヘッダ重なり補正は View 走査で行う

## 未決の論点

- 重なり原因の実機確定 (CJK 大フォント仮説の検証) と補正パラメータの決定 → 実装タスク冒頭のスパイクへ
- テキスト入力モード側の着色部位の洗い出し (TextInputLayout 系。TimePicker のキーボードモードの知見が流用できる見込み) → propose のデルタスペック/brief で確定

## UI 素材 (ui/references/)

- `current-calendar-dialog-broken.webp` — 現状の不具合スクショ (ユーザー提供、2026-08-02)。**採用するデザインではなく不具合の現状記録**: タイトルと選択日の重なり、OK/キャンセルのピンク色などテーマ非追従の配色を示す

## 変更級の推奨: M (理由)

範囲は DatePickerCell 1 系統 + ヘルパ 1 クラスと狭いが、外部ライブラリ内部依存の追加 (MaterialDatePicker の内部 ID・ヘッダ構造)、実機検証で確定すべき仮説 (重なり原因) があり、UI 変更としてモック承認ゲートを通すため。timepickercell-color-adjust (M 級) と対称。
