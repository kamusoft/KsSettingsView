# A/B 視覚証跡 対応表 (置換前後)

本変更で置き換えた3つの選択面・テーマ土台について、「前 (A)」と「後 (B)」の静止画を対応づける。

- **A (前)** は旧コードをビルドせずに調達した。過去 change (`kasane/changes/archive/`) に残る当時の実機スクリーンショットをパス参照する (コピーはしない)。
- **B (後)** は本 change の `ui/verification/` と `evidence/` の証跡。
- パスはすべてリポジトリ相対。
- A 側画像は、`2026-08-02-timepickercell-color-adjust` の時計ダイヤル・`2026-08-03-datepickercell-color-adjust` の Material カレンダー・同ランドスケープ・`2026-08-03-datepickercell-today-shortcut` の今日ジャンプの4点を目視で内容確認済み。同じ change 配下の残りはファイル名で対応づけた。
- 画像に個人情報の写り込みがないことは目視で確認した (デモデータの氏名・メールは `example.com` のサンプル値。identity-lint はバイナリ非対応のため機械検査は不可)。

## 1. 時刻選択面 (android-timepicker)

A: `MaterialTimePicker` (時計ダイヤルダイアログ) / B: ボトムシート + 時分ホイール

| 観点 | A (前) | B (後) |
| --- | --- | --- |
| 24 時間制の提示 | `kasane/changes/archive/2026-08-02-timepickercell-color-adjust/ui/verification/clock-24h-hour.png`<br>`.../clock-24h-minute.png` | `kasane/changes/relax-android-host-prerequisites/ui/verification/timepicker-24h-ja.png`<br>`kasane/changes/relax-android-host-prerequisites/evidence/verify-samples-android-07-timepicker-24h-sheet.png` |
| 12 時間制の提示 (午前/午後) | `kasane/changes/archive/2026-08-02-timepickercell-color-adjust/ui/verification/clock-12h-am.png` | `kasane/changes/relax-android-host-prerequisites/ui/verification/timepicker-12h-ja.png`<br>`.../ui/verification/timepicker-12h-en.png`<br>`.../evidence/samples-componentactivity-03-timepicker-12h.png` |
| キーボード入力モード | `kasane/changes/archive/2026-08-02-timepickercell-color-adjust/ui/verification/keyboard-24h.png`<br>`.../keyboard-12h-pm.png` | **B 側なし** — 新選択面はホイール1系統で、テキスト入力モードを持たない (シート系選択面の共通契約に合わせた意図的な機能減) |
| 確定の反映 | (A 側なし — 当時の change は配色調整が主題で確定後の行の静止画を残していない) | `kasane/changes/relax-android-host-prerequisites/evidence/verify-samples-android-08-timepicker-24h-confirmed.png` |
| 回転時の挙動 | `kasane/changes/archive/2026-08-03-fix-picker-dialog-recreation/ui/verification/after-timepicker-dialog-after-rotate.png` (旧: Fragment 復元で再提示) | **B 側なし (画像不要)** — 新契約は「回転で閉じる」。閉じた状態の静止画は情報量がないため撮っていない。契約はテストで担保 (spec android-timepicker「構成変更で閉じる」) |

## 2. カレンダー選択面 (android-datepicker)

A: `MaterialDatePicker` (DialogFragment) / B: ComponentDialog + ComposeView + Compose M3 DatePicker

| 観点 | A (前) | B (後) |
| --- | --- | --- |
| カレンダー表示 | `kasane/changes/archive/2026-08-03-datepickercell-color-adjust/ui/verification/01-calendar.png`<br>`.../02-calendar-reselected.png`<br>`.../03-calendar-month-moved.png` | `kasane/changes/relax-android-host-prerequisites/ui/verification/datepicker-m3-140-calendar-light.png`<br>`.../evidence/samples-componentactivity-04-datepicker-calendar.png` |
| 年選択 | `kasane/changes/archive/2026-08-03-datepickercell-color-adjust/ui/verification/04-year-grid.png` | `kasane/changes/relax-android-host-prerequisites/ui/verification/datepicker-m3-140-yearselect-light.png`<br>`.../ui/verification/datepicker-m3-140-yearselect-dark.png` |
| テキスト入力モード | `kasane/changes/archive/2026-08-03-datepickercell-color-adjust/ui/verification/05-text-input.png`<br>`.../06-text-input-invalid.png` | `kasane/changes/relax-android-host-prerequisites/ui/verification/datepicker-m3-140-textinput-light.png`<br>`.../datepicker-m3-140-textinput-dark.png`<br>`.../datepicker-m3-140-textinput-light-keyboard-hidden.png` |
| 範囲外日付の disabled | (A 側なし — 当時の change に範囲制限の静止画がない) | `kasane/changes/relax-android-host-prerequisites/ui/verification/datepicker-m3-140-outofrange-light.png`<br>`.../datepicker-m3-140-outofrange-nightmode.png`<br>`.../datepicker-m3-140-calendar-outofrange-dark.png` |
| ランドスケープ | `kasane/changes/archive/2026-08-03-datepickercell-color-adjust/ui/verification/07-calendar-landscape.png` | `kasane/changes/relax-android-host-prerequisites/ui/verification/datepicker-landscape-fixed-opened.png`<br>`.../datepicker-landscape-fixed-scrolled.png`<br>`.../datepicker-landscape-fixed-monthnav.png`<br>`.../datepicker-landscape-fixed-yearselect.png`<br>`.../datepicker-landscape-fixed-selected.png`<br>`.../datepicker-landscape-fixed-confirmed.png` |
| 今日ジャンプ (前後) | `kasane/changes/archive/2026-08-03-datepickercell-today-shortcut/ui/verification/material-dialog-before-today-tap.png`<br>`.../material-dialog-after-today-tap.png` | `kasane/changes/relax-android-host-prerequisites/evidence/verify-samples-android-10-datepicker-today-jump-nofire.png`<br>`.../evidence/verify-samples-android-11-datepicker-confirmed.png` |
| 今日が範囲外のセーフガード | `kasane/changes/archive/2026-08-03-datepickercell-today-shortcut/ui/verification/material-dialog-today-out-of-range-no-change.png` | **B 側なし (画像不要)** — 同契約はテストで担保。静止画では「変化しない」ことを示せない |
| 回転をまたぐ復元 | `kasane/changes/archive/2026-08-03-fix-picker-dialog-recreation/ui/verification/before-datepicker-material-after-rotate.png` (旧: 復元後は配色対象外の既知問題あり)<br>`.../after-datepicker-material-after-rotate.png` | `kasane/changes/relax-android-host-prerequisites/evidence/datepicker-rotation-01-before-row.png` 〜 `.../datepicker-rotation-07-after-confirmed.png` (連番7点) |

## 3. テーマ土台 (android-theming)

| 観点 | A (前) | B (後) |
| --- | --- | --- |
| 非 Material3 ホスト (`ComponentActivity` + フレームワーク標準テーマ) での全 Cell 表示 | **A 側なし (画像不要)** — 旧実装ではこの構成自体が成立しない (ホストテーマが `Theme.Material3.*` 派生であることが利用前提。MaterialSwitch の `materialSwitchStyle` 解決に失敗して膨張時に例外となり、画面が出ない)。よって「前」のスクリーンショットは原理的に存在しない | `kasane/changes/relax-android-host-prerequisites/evidence/samples-componentactivity-01-basic-cells.png`<br>`.../samples-componentactivity-02-basic-cells-scrolled.png` |
| 非 Material3 ホストでのシート系選択面 | **A 側なし (同上)** | `kasane/changes/relax-android-host-prerequisites/evidence/verify-samples-android-05-pickercell-sheet.png` (PickerCell)<br>`.../verify-samples-android-06-numberpicker-sheet.png` (NumberPickerCell)<br>`.../verify-samples-android-09-datepicker-wheel-sheet.png` (DatePickerCell ホイール) |
| MAUI テンプレート既定テーマ (`Maui.SplashTheme`) での全 Cell 表示 | **A 側なし (画像不要)** — 旧実装では samples/maui の MainActivity を Material3 テーマへ書き換えて回避していたため、「テンプレート既定のまま」の状態が存在しない | `kasane/changes/relax-android-host-prerequisites/evidence/verify-maui-01-basic-cells.png`<br>`.../verify-maui-02-basic-cells-scrolled.png`<br>`.../verify-maui-03-timepicker-sheet.png`<br>`.../verify-maui-04-timepicker-confirmed.png` |
| ButtonCell タイトル色 (ホスト `colorPrimary` 追従 → 固定既定色) | (A 側なし — 当時のサンプルはホストテーマの primary がライブラリ既定色と近く、差が視覚的に出ない) | `kasane/changes/relax-android-host-prerequisites/evidence/verify-maui-02-basic-cells-scrolled.png` (「ログアウト」がライブラリ固定既定色) |

| MAUI テンプレート既定テーマでのカレンダー選択面 | **A 側なし (画像不要)** — 旧実装は `MaterialDatePickerPresenter` (Fragment 依存) 経路で、MAUI 既定テーマのホストでは成立しない | `kasane/changes/relax-android-host-prerequisites/evidence/verify-maui-05-datepicker-calendar.png` (提示)<br>`.../verify-maui-06-datepicker-selected.png` (操作)<br>`.../verify-maui-09-datepicker-confirmed.png` (確定1回発火)<br>`.../verify-maui-10-datepicker-before-cancel.png` → `.../verify-maui-11-datepicker-cancelled-nofire.png` (非確定破棄) |
| MAUI ホストでの in-place 構成変更 (回転) 中のカレンダー選択面 | **A 側なし (画像不要)** — 同上 | `kasane/changes/relax-android-host-prerequisites/evidence/verify-maui-06-datepicker-selected.png` (回転前)<br>`.../verify-maui-07-datepicker-rotate-landscape.png` (横。開いたまま生存・選択日維持)<br>`.../verify-maui-08-datepicker-rotate-back-portrait.png` (縦へ復帰。選択日維持) |

## 未取得

- なし。

## 注記

- 上記の MAUI カレンダー系証跡は、Compose の版整合 (`android/gradle/libs.versions.toml` の compose BOM を 2025.11.01 へ、binding csproj の NuGet 側と一致させる) を入れた状態で取得した。版整合前は `evidence/verify-maui-datepicker-crash-logcat.txt` のとおり `NoSuchMethodError` でクラッシュし撮影できなかった。
- 撮影画面に表示される氏名・メール・電話番号はサンプルアプリに埋め込まれた架空の demo 値 (`Tanaka Taro` / `tanaka.taro@example.com` / `090-0000-0000`) であり、実在の個人情報ではない (目視確認済み)。
