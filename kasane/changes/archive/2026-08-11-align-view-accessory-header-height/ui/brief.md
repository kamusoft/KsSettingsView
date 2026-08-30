# UI Brief: align-view-accessory-header-height

## 画面と状態

対象は Section Header 領域 (view accessory) の高さ解決のみ。画面遷移・loading / empty / error 状態は対象外。

- 状態A: `headerHeight = -1` (自動) — 内容なりの高さ
- 状態B: `headerHeight` 正値・内容が収まる — 指定高さちょうど
- 状態C: `headerHeight` 正値・内容がはみ出す — 指定高さを維持し、はみ出し分は clip (描画されない)
- 動的変更 (A↔B↔C の遷移): 高さのみが更新され、view の内部状態は維持される (視覚上は高さの変化のみ)

## リファレンス注釈

- references/ なし — 本 change は iOS の現行挙動 (view accessory にも固定高さが効く) を Android へ対称化するもので、視覚の正は iOS 実機の現行表示。実装後の検証で iOS / Android のスクリーンショットを `ui/verification/` に並べて照合する

## デザイントークン参照

- 高さの意味論: `Section.headerHeight` / `Theme.headerHeight` (dp)。優先順位はデルタスペックが規定
- mock の配色・寸法は Sample テーマの近似で**非規範**。規範は各状態の高さの決まり方と clip の有無のみ

## mock 案数について

config の `ui.mock-variants: 2` に対し本 change は **1案のみ** — 挙動契約 (phase-6 論点⑤裁定) が確定済みで、mock で試行錯誤すべきデザイン選択肢が存在しないため (状態図解としての mock)。

## 承認モック

mock/height-states.html を採用 (approved.png、2026-08-11 ユーザー承認)。

- 規範は3状態の高さの決まり方のみ: A 自動 (内容なり) / B 固定 (指定高さちょうど・縮伸しない) / C 固定 + oversized (はみ出し分は clip、指定高さ維持)。動的変更 (A↔B↔C) は高さのみが変化し view の内部状態は維持される

## 視覚照合結果 (実装後検証)

- **hosted view の領域占有 (deviation.md の追加契約) 反映後の再照合 (2026-08-11、エミュレータ API 35 / density 2.0)**: `verification/` の 3 枚 (android-header-height-states.png / -scrolled.png / -rebound.png) を approved.png と照合し、A/B/C の高さの決まり方・C の clip が一致することを確認。合意済み妥協は 0 件
  - 状態B: Header 領域は 96px = 48.0dp ちょうどで、accessory の背景色が領域**全面**を占有 (下部に未描画の帯なし)。approved.png の状態B の全面塗りと**一致**。修正前に生じていた下部 32px の未描画帯は解消
  - 状態A: 内容なりの自動高さ (3 行すべて描画)。状態C: 領域は 48.0dp を維持し、はみ出した行は途中で切断され 3 行目は非描画 — approved.png 状態C の切れ方と一致
  - clip の客観確認: 48dp 固定に 168dp の内容 (40dp 緑帯 + 8dp 余白 + 60dp 赤帯 ×2) を入れた検証ケースで、境界外に置いた赤帯の色が 3 枚のいずれにも 1px も出現しないことを画素計数で確認 (出現数 0)
  - rebound (スクロール往復後の再表示) は states とバイト同一 (MD5 一致)。再表示後の画面が states と同一表示になるためで、ViewHolder 再利用後も A の自動高さ・B/B2/C/D の固定高さが入れ替わらないことを示す
- iOS 側スクリーンショットとの並置照合は未実施 (tasks 3.3 の範囲は Android の clip 確認のみ。iOS は挙動無変更で、対称性はユニットテストで固定)
