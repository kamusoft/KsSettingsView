---
id: 0008
title: DatePicker ダイアログの動的配色とヘッダ重なり補正は MaterialDatePicker を維持し表示後の内部 View 走査で行う
status: superseded
date: 2026-08-02
---

## Context

Android 版 DatePickerCell (カレンダーモード、`MaterialDatePicker` / material 1.12.0) のダイアログには 2 つの問題がある (ui/references/current-calendar-dialog-broken.webp 参照):

1. **ヘッダの重なり**: `setTitleText` で渡したタイトル (「予約日を選択」) と選択日テキスト (「2026年6月1日」) が上下に重なり、タイトルがほぼ読めない
2. **テーマ非追従の配色**: OK/キャンセルだけ Activity テーマの colorPrimary を拾い、選択日の丸・ヘッダ文字等は Material 既定色のままという混色状態で、KsSettingsView のテーマ色が反映されない

重なりの構造要因 (2026-08-02 調査): `mtrl_picker_header_dialog.xml` はタイトルと選択日テキストを**同一 FrameLayout に重ね置き**し、「タイトルはベースライン 28dp・選択日はベースライン 100dp」という固定 dp オフセットだけで縦に積む設計になっている。選択日側は `textAppearanceHeadlineLarge` + `autoSizeTextType="uniform"` で、英語の短い文字列なら 72dp の余白に収まる前提が、日本語グリフの大きい描画ボックスで上に食い込む — というのが最有力仮説 (中自信度。実装フェーズ冒頭の実機検証で確定する)。material-components の既知 issue としては完全一致するものは未発見 (同種の「固定 dp レイアウトが大きい文字に弱い」報告として issue #3642 あり)。

サンプルのテーマは `Theme.Material3.DayNight.NoActionBar` で `materialCalendarTheme` は正しく解決されており、「テーマ未解決」は原因ではない。また `DatePickerCell.accentColor` は TimePickerCell のときと同じく「定義済みだが ViewHolder 側で未接続」だった。

配色要件は TimePickerCell (android/ADR-0006) と同一: アプリが実行時に持つ任意の Color 値 (背景 / アクセント / 通常文字) の反映であり、`Builder.setTheme(@StyleRes)` などの静的テーマ指定では満たせない。

## Decision

MaterialDatePicker を維持し、ダイアログ表示後に内部 View を走査して実行時テーマ色をプログラム的に上書きする (ADR-0006 と同方式)。加えて、**同じ走査フックでヘッダの重なり補正も行う**:

1. 動的配色は ADR-0006 の方式 (FragmentLifecycleCallbacks で着色フック・全走査・冪等再適用) を DatePicker 用ヘルパ (DatePickerColorizer 相当) 1 クラスに閉じて横展開する。色ロールの導出は `TimePickerColors` / `ColorRoles` の導出規則を再利用する
2. ヘッダのタイトル/選択日テキストの重なりは、同じフックでヘッダ 2 つの TextView の `firstBaselineToTopHeight` やテキストサイズをプログラム的に補正する。新規リソース (スタイル XML) は追加しない
3. 補正パラメータと重なり原因の確定 (CJK 大フォント仮説の実機検証) は実装タスク冒頭のスパイクで行う

## Alternatives Considered

- **カスタム ThemeOverlay + `setTheme` でヘッダスタイルを上書き (B 案)** — 却下。inflate 時点で確実に効く安心感はあるが、ライブラリ res へのスタイル XML 追加が必要で、スタイル名・属性名への依存が別途増える。配色はどのみち走査が必要なので、レイアウトだけ別機構になり修正の局所性 (ヘルパ 1 クラスに閉じる) を損なう
- **選択日フォーマットの短縮で重なりを緩和** — 却下。視覚的な重なりを緩和できる可能性はあるが対症療法であり、タイトルと選択日が固定 dp で重ね置きされる構造要因は残る
- **material-components のバージョンアップで解消を確認** — 今回は追わない。1.12.0 固定は ADR-0006 の前提であり、バージョン変更はこの変更のスコープ外

## Consequences

- 正: レイアウト補正と配色が同一ヘルパに閉じ、ViewHolder からは attach 1 行で済む (TimePickerCell と対称の構造)
- 正: 未接続だった `DatePickerCell.accentColor` が接続され、TimePickerCell と挙動が揃う
- 負: material-components の内部実装への依存が MaterialDatePicker 分 (内部 R.id・ヘッダレイアウト構造) 増える。1.12.0 からライブラリを上げる際の追随確認項目が増える
- 負: 表示後補正のため、補正前の 1 フレームが見えるリスクがある (TimePicker で実績のある pre-draw 方式で対処する前提)
- 注: 重なり原因の「CJK 大フォント仮説」は中自信度のまま決定している。実装冒頭スパイクで棄却された場合、補正手段 (何を再設定するか) は見直すが、「走査フックで補正する」という方式自体は維持する

出典: kasane/changes/datepickercell-color-adjust/exploration.md (2026-08-02 の探索議論・ksn-scout 調査) / 同 ui/references/current-calendar-dialog-broken.webp (現状スクショ) / android/ADR-0006 (走査方式の原決定)
