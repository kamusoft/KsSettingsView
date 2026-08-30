---
scope: code-review
kind: success
severity: normal
count: 6
first-seen: 2026-08-01
last-seen: 2026-08-04
evidence:
  - fix-decoration-theme-not-applied-on-initial-bind (実装 (tasks 4.4) とレビュー双方が独立に applyDecoration 1 行の除去を実測し、追加 3 テストのみが落ちることを確認。原状復帰は shasum 一致まで照合)
  - fix-ios-separator-color-not-applied (実装 (tasks 3.4) とレビュー双方が config.color 1 行の除去を実測し、新規 3 テストのみ FAIL・同ファイル既存 30 テスト green の分離を確認)
  - fix-android-cell-width-allocation (review-003 がミューテーション 4 種 + 単独 probe でアサーションの回帰検出力を実証し、トートロジー疑いを決着させた)
  - android-picker-selection-sheet (review-005〜007 が「そのテストは旧実装なら落ちるか」を反証値の構成 — 互いに異なる backgroundColor / accentColor、絶対値 + 相対値の2本立て固定 — で確認し、退行検出力を担保した)
  - fix-picker-dialog-recreation (review-001 がミューテーション 3 種を実測し、検出力に加えて「設計要素が load-bearing であること」までテストが固定していることを示した)
  - fix-adapter-not-restored-on-reattach (review-002 がミューテーション 3 種で追加テスト 3 件の検出力を「狙ったテストだけが落ちる」形で分離実証。うち M2 は review-001 自身の推奨修正形を注入するもので、推奨が誤りだったことまで同じ手法で決着させた)
---

> 2026-08-04 に `lessons/code-review.md` の L-001 へ昇格済み。本ファイルは実測例の記録 (2段開示の詳細側)。

## ルール文

「このアサーションはトートロジーではないか / 回帰検出力があるか」が争点になったレビューでは、静的読解で議論を続けず、実装へ一時的なミューテーションを入れてテストが実際に落ちることを実測する。前提アサーションが通過し争点のアサーションだけが落ちるミューテーションを構成できれば、検出力の証明として決定的。使った一時変更は backup との shasum 一致で原状復帰を確認する。

## 経緯

- 2026-08-04 fix-decoration-theme-not-applied-on-initial-bind / fix-ios-separator-color-not-applied: 双子の 1 行修正 (Android の `applyDecoration` 呼び出し / iOS の `config.color` 設定) で、実装者がタスク規定の変異注入を実測し、独立レビュアーも同じ変異を自分で再実行して検出力を確認した (5〜6例目)。争点化する前にタスク定義へ組み込まれた形で機能しており、パターンが BAU 化している。
- 2026-08-03 fix-adapter-not-restored-on-reattach: 実装 (tasks 4.3) とレビュー双方がミューテーションを実測。review-002 の M1〜M3 は 3 件の追加テストそれぞれについて「狙ったテストだけが落ちる」分離を確認し、否定形アサーション (「差分通知を出さない」) の空振り懸念も M3 で外から検証した。特筆すべきは M2 — review-001 が推奨した修正形 (`setRootDirect` に Theme を渡す) をミューテーションとして注入し、`ItemDecoration` が古い Theme のまま残る退行をテストが捕まえることを示した。**レビュー推奨案の正誤の決着**にも同じ手法が使えた4例目。
- 2026-08-03 fix-picker-dialog-recreation: review-001 が 3 種のミューテーション (`runRestoreScan()` 即 return → 24 件中 18 件 FAILED / `hasUniqueOwner` 常時 true → 複数インスタンス Scenario のみ FAILED / `post` を同期呼び出しに変更 → 同 Scenario が FAILED) を実測。3 つ目が特に有用で、実装者が「複数インスタンス検出のため走査を次のメッセージへ回した」という設計判断を、テストが load-bearing な要素として固定できていることを示した (同期化しただけで落ちる = 遅延が偶然ではなく仕様として守られている)。検出力の証明に加え、**設計上の意図がテストに焼き付いているかの確認**にも同じ手法が使える。実装は shasum 一致で原状復帰を確認。
- 2026-08-02 android-picker-selection-sheet: review-006 が新テストの入力値設計 (backgroundColor=#F2EFE6 / cellAccentColor=#CC9900 と互いに異なる値) により旧実装 (輝度導出) では必ず落ちることを確認。review-007 は絶対値 (22sp→23/21/21) と相対 (±1sp) の2本立てで両側から固定されていることを確認した。ミューテーション実挿入ではないが「退行を実際に検出できるか」を検証する同型の手法。
- 2026-08-01 fix-android-cell-width-allocation: review-002 が「実位置アサーションは前提が通れば必ず通るトートロジー」と指摘 → 修正後の review-003 がミューテーション 4 種 (幅退行 / gravity 喪失 / compound drawable / preDraw 除去) を実測し、「前提 1・2 が通過し実位置アサーションだけが落ちる」ケース (M3) を構成して検出力を直接証明した。各アサーションの役割分担 (幅退行は前提 1、描画位置は前提 3 が捕まえる) も同時に判明し、KDoc へ残された。一時変更は shasum 一致 + 全テスト再実行で原状復帰を確認。
