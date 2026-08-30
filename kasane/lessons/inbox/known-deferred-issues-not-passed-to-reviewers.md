---
scope: process
kind: pain
severity: normal
count: 2
first-seen: 2026-08-02
last-seen: 2026-08-03
evidence:
  - timepickercell-color-adjust (second-opinion-002 指摘#2: 相方 codex がダイアログ再生成の既存構造問題を Major 評価 → オーナー決定で fix-picker-dialog-recreation へ切り出し)
  - datepickercell-today-shortcut (second-opinion-002 Major 1: 同じ再生成問題の今日ボタン版を相方 codex が再び Major 評価し CHANGES_REQUESTED → オーナー確認で再び先送り維持。ホスト側レビュアーは既知先送りを認識して Minor 申し送り扱いにできていた)
---

## ルール文 (候補)

オーケストレーターはレビュー・セカンドオピニオンの入力に、deviation.md と並べて「既知の切り出し済み先送り問題」(kasane/changes/ にスタブがある関連 change の一覧と1行サマリ) を渡す。渡さないと、プロジェクト文脈を持たない相方が同じ既知問題を毎回 blocking Major として再指摘し、判定 (CHANGES_REQUESTED) と突き合わせ・オーナー確認の往復コストが変更のたびに再発する。

## 経緯

- 2026-08-02 timepickercell-color-adjust: ダイアログ再生成でリスナー・着色が失われる既存構造問題を相方が Major 評価。オーナーが独立変更 (fix-picker-dialog-recreation) への切り出しを決定。
- 2026-08-03 datepickercell-today-shortcut: 同じ構造問題の「今日ボタンが消える」形を相方が再び Major 評価し CHANGES_REQUESTED。オーナー確認の結果は前回決定の踏襲 (先送り維持 + 申し送り追記)。ホスト側 ksn-reviewer は変更ディレクトリの文脈から既知先送りを認識し Minor 扱いにできていた — 差は入力に既知先送りの文脈があったかどうか。
