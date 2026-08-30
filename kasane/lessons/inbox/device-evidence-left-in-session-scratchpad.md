---
scope: impl
kind: pain
severity: normal
count: 2
first-seen: 2026-08-11
last-seen: 2026-08-28
evidence:
  - fix-maui-entrycell-focus-loss (実機検証のスクリーンショット 17 点と jdb / viewdump ログが session scratchpad にのみ置かれ、change 配下に 1 件も無い状態で完了報告。review-001 Major-2 が runtime-behavior-verification 規約の完了条件 3「証跡を change 配下に残す」への抵触として差し戻し)
  - customcell-android-maui-perf (Pixel 6a 実機の gfxinfo 実測 5 構成の生ログが会話ログと scratchpad にのみ存在する状態で summary を確定しレビュー提出。数値は concepts の長命文書へ転記済みだった。review-001 Major-2 が同じ規約条件への抵触として差し戻し、evidence/gfxinfo-pixel6a.md の作成で解消)
---

## ルール文

実機検証・実行時挙動確認の証跡 (スクリーンショット・ログ) は、取得したその場で `kasane/changes/<change-id>/` 配下 (evidence/ 等) へ保存し、確認項目との対応を索引 (evidence.md) に 1 行ずつ残す。session scratchpad は一時領域であり、そこにしか無い証跡はアーカイブ後に検証不能になる — scratchpad への保存は「証跡を残した」に数えない。

## 経緯

- 2026-08-11 fix-maui-entrycell-focus-loss: 完了条件を満たす実機証跡 (ASCII 連続入力 / BackSpace / IME 変換・確定 / 回帰確認) は取得済みだったが、所在が session scratchpad のみで change 配下に無く、review-001 が Major-2 で差し戻し。移送 + 索引 (evidence.md) の追加で解消。round-1 の crop 画像は連番のみの命名で内容が判別できず、索引の必要性も review-002 Minor で重ねて指摘された。
- 2026-08-28 customcell-android-maui-perf: ksn-live のライブ調整でメインエージェント自身が計測を実施したケース。計測値を concepts へ恒久転記までしていながら生ログは会話にしか無く、review-001 が同じ規約条件で差し戻し。実装ワーカーだけでなく、メインが自ら計測する編成 (ライブ調整・切り分け検証) でも同じ規律が要る。
