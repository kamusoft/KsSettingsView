# deviation: docs-refresh-worker-dispatch

- 実装の見当 (2)-1「機械的な切り出し」: 予定 manifest 生成 (`scripts/planned-manifest.py`) は、元のインライン版がスクリプト本文の `ADD_TARGETS` / `ADD_EXCLUDED` / `DROP_CONCEPTS` を毎回書き換える形だった → 共有ファイル化に伴い、環境変数 `DOCS_REFRESH_DECISIONS` が指す JSON から判断を読む形に改めた。理由: 切り出したスクリプト本体を実行のたびに改変させないため。判断なしの実行では出力一致を確認済み (parity-check.md) (2026-09-02)
- 実装の見当 (2)-3「SKILL.md は 20KB 前後を目標」: 切り出せる Python はすべて外に出したが 44KB (56KB から) にとどまった。残りは散文の手順・注記・Guardrails で、意味を変える要約はしない制約のため削っていない。理由: これ以上の圧縮は手順の取捨選択という別判断になる (2026-09-02)
