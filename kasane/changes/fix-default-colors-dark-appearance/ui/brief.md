# UI Brief: fix-default-colors-dark-appearance

## 画面と状態

- **Theme を渡さない設定 list** (状態: ライト / ダーク): 3 platform の Native Host と MAUI facade が、Theme 未指定 (または一部だけ上書き) のときに描く既定の見た目。ライトは現行のまま変えない。ダークはライブラリ所有の dark セットで描く。見た目の正は承認モックの「ダーク (新規)」パネルと色ロール対応表
- **Classic / Modern の両 style** (状態: ライト / ダーク): 既定色は style に依らず同じセットから解決する。Modern の箱は Cell 背景と list 下地の対比で見える (箱の枠線の既定は透明のまま)
- **選択面 (PickerCell のシート / DatePickerCell のカレンダー)** (状態: ライト / ダーク): Android は開いた時点の解決済み Theme の値で地色・罫線・文字を描く。表示中の外観切替では描き直さず、閉じて開き直したときから新しい外観 (表示中の外観切替の対象外)。iOS は提示元の外観を引き継ぐ既存契約のまま
- **表示中の外観切替** (Android で Activity が再生成されない構成 / iOS の trait 変更): 未指定の色だけが新しい外観のセットへ描き直される。明示指定した色は変わらない
- loading / empty / error 状態なし (静的な描画契約のため)

## リファレンス注釈

- `references/ios-visibility-dark.png` / `references/ios-section-decoration-dark.png` / `references/android-visibility-dark.png` / `references/android-section-decoration-dark.png` / `references/maui-ios-visibility-dark.png` / `references/maui-android-visibility-dark.png`: 起票元 change (add-sample-dark-mode-toggle) の照合で撮った**現状の症状** (白地に淡色文字)。採用する要素はなし。本 change 後の照合で「同じ画面が判読できる」ことを比べる基準として使う
- 参考にする OS 側の見た目: iOS の設定アプリのダーク配色 (canvas 黒・Cell 濃灰・separator・二次文字の灰色)。画像は持たず、モックの dark 値の出所として色ロール対応表に記す

## デザイントークン参照

- Theme の色ロール (backgroundColor / cellBackgroundColor / separatorColor / selectedColor / cellAccentColor / disabledTextColor / headerTextColor / footerTextColor / headerBackgroundColor / footerBackgroundColor / cellTitleColor / cellDescriptionColor) の意味と解決順は [スタイルの所有と実効値解決](../../../concepts/core/styling/style-resolution.md)
- ライブラリ既定色を中立に保つ方針 (Sample の AiForms 互換色は製品契約ではない) は同文書「Sample の AiForms 互換色」
- concepts/ に dark 用のトークン定義はまだ無い。dark セットの生値は承認モックの色ロール対応表が持ち、蒸留で concepts へ昇格させる (exploration.md 論点 8)
- 3 platform の既定値の platform 差の許容範囲は [Sample のプラットフォーム間一致](../../../handbook/cross/sample-parity.md) の「本体既定値の platform 差」

## 承認モック

mock/plan-a.html を採用 (approved.png)。2026-09-05 オーナー承認。案A = iOS の dark パレットに忠実 (canvas 黒 / Cell 背景 濃灰 / Header・Footer 背景は canvas と同色 / accent は dark systemBlue)。却下: 案B (ライトの色同士の関係を保つ: canvas = Cell 背景、accent はライトと同値) — OS 標準の設定画面との整合と Modern の箱の視認性で案A を優先。dark セットの生値の正は approved.png 内 (plan-a.html 下部) の色ロール対応表。approved.png はモックの描画のみで個人要素は含まない (確認済み)。

候補だったもの: `mock/plan-a.html` (iOS の dark パレットに忠実: canvas と Cell 背景を分け、accent は dark systemBlue) / `mock/plan-b.html` (ライトの色同士の関係を保つ: canvas = Cell 背景、Header / Footer 背景だけ僅かに明るい、accent はライトと同値)。両案とも light 側は現行値で同一。

## 現行との照合 (lessons spec-review L-003)

- light 列の各値は現行ソースと照合済み (2026-09-05): iOS `Theme.swift` の `defaultBackgroundColor` / `defaultSeparatorColor` / `defaultSelectedColor` / `defaultAccentColor` / `defaultHeaderBackgroundColor` / `defaultHeaderTextColor` / `defaultFooterTextColor` / `defaultDisabledTextColor` と `init` の `cellBackgroundColor` 既定、Android `Theme.kt` の `DEFAULT_SEPARATOR_COLOR` / `DEFAULT_SELECTED_COLOR` / `DEFAULT_ACCENT_COLOR` / `DEFAULT_HEADER_BACKGROUND_COLOR` / `DEFAULT_HEADER_TEXT_COLOR` / `DEFAULT_FOOTER_TEXT_COLOR` / `DEFAULT_BACKGROUND_COLOR` / `DEFAULT_DISABLED_TEXT_COLOR` / `DEFAULT_CELL_TITLE_COLOR` / `DEFAULT_CELL_DESCRIPTION_COLOR` / `DEFAULT_BUTTON_TITLE_COLOR` と `cellBackgroundColor` 既定。approved.png の light 列はこれらの現行値と一致する (iOS の Header / Footer 文字は Android と僅差の現状の platform 差のまま。モックの表に注記)
- モックの行構成・文言は説明用の架空データで、Sample の画面を写したものではない (規範は配色のみ。行構成は非規範)
