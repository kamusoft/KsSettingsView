# UI Brief: android-datepicker-spinner-wheel

## 画面と状態

構造階層 (ボトムシート — NumberSelectionSheet と同系の器):

```
BottomSheetDialog (DatePicker 専用シート、新設)
├── ドラッグハンドル
├── ヘッダー: キャンセル | タイトル (pickerTitle ?: title) | OK
│     (文字列は OS リソース: android.R.string.cancel / android.R.string.ok — 自前文字列の同梱なし)
├── 3連ホイール (年 | 月 | 日、各列とも縦スクロールのスナップ式)
│     └── 中央の選択中行を accent 淡色帯で強調 (numberpicker 承認モックの意匠を踏襲)
└── 「今日」ボタン (todayText 非 null のときのみ。配置は variant A/B で比較)
```

状態:

- **通常**: `date` の年/月/日が各列の選択中位置で開く。範囲外の date は最も近い範囲端へ丸め
- **月/年変更**: 日列の候補数が実日数に追随し、超過した選択日は末日へ丸め
- **min/max 境界**: 境界年・境界月では候補が範囲内に制限される
- **todayText 未指定**: 「今日」ボタンは非表示 (シート構成から除かれる)
- **無効 (`isEnabled = false`)**: 行タップでシートを提示しない

## リファレンス注釈

- 貼付画像なし。意匠の先例は archive/android-numberpicker-modern-ui/ui/mock/approved.png (plan-a 系: accent 淡色帯 + 減衰ホイール + フェード) — **この意匠を3連に拡張して踏襲する**
- モックの候補表記 (「2026年 / 8月 / 2日」) は**日本語 Locale の例**。表示文字列は端末 Locale から導出する (spec「候補表示の Locale 追随」参照)。列順は Locale によらず年→月→日で固定
- iOS の `.wheels` + todayText (Toolbar 配置) は挙動の参照点であり、見た目の踏襲対象ではない

## デザイントークン参照

- シート面: `Theme.cellBackgroundColor` / ヘッダー・区切り: `Theme.separatorColor` ([list-appearance](../../../concepts/core/styling/list-appearance.md))
- 選択中候補の強調・「今日」ボタン: `DatePickerCell.accentColor`、未指定時は「CellStyle → Theme」の段階解決 ([style-resolution](../../../concepts/core/styling/style-resolution.md))
- ヘッダー構成 (キャンセル=低強調テキスト / OK=accent の filled pill) は numberpicker 承認モックと同一
- 生値はここに書かない。具体レイアウトは mock が正

## 検証条件 (動的挙動の判定基準)

視覚照合では静的な mock 照合に加えて以下を判定する:

- 各列とも、スクロールを離すと必ずいずれか1候補が中央 (選択中位置) にスナップして静止する
- 開いた時点で `date` の年/月/日が各列の選択中位置にある
- 月を 31日月 → 2月に変えると日列の候補数が変わり、選択中の日が末日へ丸まる
- 「今日」タップで3列が今日の位置へ移動する (callback は発火しない)
- 確定 (OK) でのみ callback が発火し、キャンセル・外側タップ・Back・下スワイプでは発火しない

## 承認モック

mock/variant-b-today-below-wheels.html を採用 (approved.png、2026-08-02 オーナー承認)。

- 構成: ドラッグハンドル + ヘッダー (キャンセル=テキストボタン / タイトル中央 / OK=accent の filled pill — numberpicker 承認モックと同一) + 3連ホイール (年 | 月 | 日) + 「今日」outline chip (ホイール下・中央、todayText 非 null 時のみ)
- ホイール意匠: numberpicker 承認モック (plan-a) を3連に拡張 — 中央の accent 淡色帯 (3列を横断する1本の帯)、選択中行は accent 太字、周辺行は距離に応じて減衰、上下端はシート面色へフェード
- todayText 未指定時は chip 行 (区切り線含む) ごと非表示
- variant-a-today-in-header.html (ヘッダー内・OK 左に「今日」) は不採用の対案として保存

## 照合結果 (2026-08-02、実装フェーズ)

実機 Pixel 6a (Android 16 / 1080x2400 / ja Locale) で samples/android の「入力 Cell 5 種デモ」→
DatePickerCell (ホイール) 行から選択面を開き、`ui/verification/` の各画像と approved.png を照合した。

| # | 画像 | 判定 |
|---|------|------|
| 01 | verification/01-sheet-initial-pixel6a.png | 構造・トークン一致 (ハンドル / ヘッダー3要素 / 区切り線 / 3連ホイール + 横断帯 / 「今日」chip / 下余白)。初期選択 = `date` (1990/01/01) |
| 02 | verification/02-today-jump-pixel6a.png | 「今日」タップで3列が今日 (2026/08/02) へ移動。callback 非発火 (画面上部「最後のイベント: (none)」)。`maxDate = today` の境界で月候補が 8月まで・日候補が 2日までに制限されている |
| 03 | verification/03-day-31-selected-pixel6a.png | 各列ともスクロールを離すと候補位置へスナップ静止 (1990年 / 1月 / 29日) |
| 04 | verification/04-month-to-feb-rounded-pixel6a.png | 月を 2月へ変更すると日候補が 28日までに追随し、選択中の日が 28日へ丸まる |
| 05 | verification/05-confirmed-row-pixel6a.png | OK でのみ callback が1回発火 (誕生日 → 1990/02/28)。ヘッダー領域の下方向ドラッグによる dismiss では発火しない (04 の直前に確認済み) |

- 検証条件 5 項目 (スナップ静止 / 初期=date / 日追随 / 今日ジャンプ / 確定契約) はいずれも判定 OK
- 合意済み妥協: 0 件
- トークン候補: なし (色・タイポグラフィは既存の `PickerSheetStyle` / `KsWheelStyle` 経由で解決。承認モック由来の寸法 —「今日」chip の padding 20/6dp、chip 行の padding 10/4dp、下余白 14dp — は選択面のローカル定数として保持)
- 2026-08-02 オーナー最終承認済み (approved.png と verification 画像の比較提示に対し「承認する」— 修正箇所の指摘なし)
