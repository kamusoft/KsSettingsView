# Exploration: android-picker-sheet-maximize-on-overflow

## 課題 / 動機

`../ColorAnalyzer` の移行作業 (`../ColorAnalyzer/kasane/changes/migrate-remaining-pages-to-kssettingsview/session.md`) で、オーナーが Android 実機 (Pixel 6a) の `PickerCell` を操作して観察した事象。

**選択肢が多いと選択面 (ボトムシート) の中がスクロールになるが、初見ではスクロールがあるのか無いのか分かりにくい。**

### 現状の仕様 (起票時点で実物を確認済み)

- 初期の高さは `min(コンテンツの自然な高さ, 画面高 × 0.5)`。`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt` の `applySheetHeight` (544-586 付近)、比率定数は `SHEET_INITIAL_HEIGHT_RATIO = 0.5f` (682 付近)
- 超過時は候補リストの高さを可視領域 (折り目) ぴったりに制約して内部スクロールさせる (561-567 付近)。行高の倍数には丸めないため、折り目で行が途中で切れて見えるかは候補の構成次第で偶然任せ
- 決定の記録: `kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md` — 高さは「コンテンツ高で表示し、画面約半分を上限に内部スクロール。ドラッグで全展開可」。却下案「常にほぼ全画面」「固定比率 50%」の理由は、項目が少ないときに空っぽの面が立ち上がるのが Android では不自然だから。スクロールの発見しやすさには言及がない
- 概念: `kasane/concepts/core/cells/picker-selection-surface.md` — 高さの行に「全展開 (高さ上限の解除) はハンドル・ヘッダー起点のドラッグのみ (候補リストのスクロールでは展開しない)」とある

### 起票時点の方向 (オーナー、2026-09-17。決定事項ではない)

**高さを決めるときに、画面の半分に収まらないなら最初から最大化する**のが一番良さそう。収まるときは今までどおりコンテンツ高。

- ADR-0005 の却下理由 (項目が少ないときの空っぽの面) とは衝突しない見込み (最大化するのは収まらないときだけ)
- ただし ADR-0005 本文の「画面約半分を上限に内部スクロール」は改訂が要る

### 関連 (このスコープ外)

- 選択面にもスクロールバーを出す件は、進行中の `scroll-indicator-visible-not-applied` で Picker にも付ける予定 (オーナー明言)。この change では扱わない

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: ADR-NNNN / 未起票: ...)

## 未決の論点

- **未探索 (簡易起票)**
- ADR-0005 (`kasane/decisions/android/0005-pickercell-selection-ui-bottom-sheet.md`) の改訂候補: 高さの決め方「画面約半分を上限に内部スクロール」を「収まらなければ最初から最大化」へ改める
- 最大化したときの閉じ方・ドラッグの振る舞い (指への追従、候補リスト上のスワイプでシートが動くか) は実機で確認する。コード読みからは候補リスト上のスワイプがシートを動かさないように読める箇所があるが、オーナーの実機操作ではシートの高さを動かせており、実機の観察を正とする
- iOS は未確認 (提示形態がページシートで異なるため同じ問題は起きにくい見込みだが推測)

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定
