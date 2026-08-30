# UI Brief: fix-cell-accessory-vertical-fill

## 画面と状態

対象は Sample アプリの設定リスト画面 (基本 Cell / 入力 Cell)。画面遷移や loading / empty / error 状態は本 change の対象外で、Cell 1 行内の trailing 配置のみを扱う。

- 行の構成要素: icon (任意) / title / description (任意) / valueText (任意) / Cell 級アクセサリ (Switch・checkbox・checkmark・chevron) / hintText (任意)
- 状態: description 有無 × アクセサリ有無 × valueText 有無 の組み合わせ (mock で代表ケースを網羅)

## リファレンス注釈

- `references/original-settingsview-maui.png` (オリジナル SettingsView.Maui):
  - **採用**: SwitchCell のアクセサリ垂直センター配置。description がアクセサリの左までで折り返す幅制限。CheckableCell 群・LabelCell の trailing 配置
  - 対象外: 配色・フォント・アイコン画像そのもの (Sample アプリの既存テーマに従う)
- `references/current-kssettingsview.png` (現 KsSettingsView):
  - **問題箇所**: SwitchCell "Notification" — description がセル全幅で折り返し Switch の下に回り込んでいる。これを修正する
  - **維持**: それ以外のセル種の見た目 (LabelCell の value 右寄せ、RadioCell の checkmark 等) は現状の視覚を維持

## デザイントークン参照

- スタイル解決順序・トークン: [concepts/core/styling/style-resolution.md](../../../concepts/core/styling/style-resolution.md)
- 行寸法・icon 枠・最低行高: [concepts/core/styling/cell-row-layout.md](../../../concepts/core/styling/cell-row-layout.md)
- mock の配色は Sample アプリ既存テーマ (amber 系 accent) の近似。生値はトークンの代用であり実装の正ではない
- **mock の規範範囲**: mock が「見た目の正」として規定するのは trailing 2 系統の**配置関係** (アクセサリの垂直センター・description の折り返し幅・valueText の行内配置) のみ。spacing・寸法・フォント・配色の生値は非規範で、現行実装のトークン解決値を維持する。実装後の視覚照合は配置関係を比較する

## 承認モック

mock/plan-a.html を採用 (approved.png、2026-08-01 ユーザー承認)。

- valueText は title と同じ主行の右寄せ、Cell 級アクセサリ (Switch / checkbox / checkmark / chevron) はセル全体に対し垂直センター、description はアクセサリ列の左までで折り返す
- plan-b.html (valueText をアクセサリ列に置く案) は不採用 — concepts の「title と valueText は同じ主行へ置く」規約と衝突するため
