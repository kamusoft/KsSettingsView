# lessons 却下台帳 (append-only)

同型パターンの再提案ループを断つための台帳。捕捉時に必ず照合する。

- 2026-08-02 / impl / 新規コメントが comment-policy の禁止参照に違反する (count 3、出典: fix-cell-accessory-vertical-fill / ios-picker-selection-parity / timepickercell-color-adjust) — lint/hook 化済み (→ `.claude/hooks/comment-policy-check.py`、Edit/Write の PostToolUse で機械検出)。機械検査を優先し文章ルールとしては保持しない
- 2026-08-03 / impl / **上記の却下を取り消す** — add-cell-types-custom で新規 11 ファイルに再発したため調査したところ、hook は (1) 除外パス `/.claude/` が worktree (`.claude/worktrees/<name>/`) 配下の全編集に一致して丸ごと無検査、(2) PostToolUse のため発火しても書き込み後の警告止まり、(3) 既存ファイルを走査する手段が無い、の 3 点で機能していなかった。hook を PreToolUse のラチェット方式に作り替え (`scripts/comment_policy_rules.py` へ検出ロジックを分離、`scripts/comment-policy-lint.py --selftest` で疎通を検証可能) たうえで、実装ワーカーが `concepts/` を読み込まない配線であることも判明したため、`lessons/impl.md` L-001 / L-002 として昇格した。**機械検査があるパターンでも、規約がワーカーに届く導線が無い場合は lessons を併置してよい** (この却下理由の再適用時はこの前提を確認する)
- 2026-09-01 / impl, ui-impl / **コメント規約 lessons を撤去** — 併置の前提だった「規約がワーカーに届く導線が無い」が解消した: `kasane/handbook/cross/comment-policy.md` が handbook index で「常時 (always)」指定になり、ksn-implement / ksn-ui のワーカー規律が作業開始前に always 文書を必読ロードする配線が kasane 本体に入った。PreToolUse hook (`scripts/comment-policy-lint.py --hook`、`.claude/settings.json` に配線) も稼働中。これにより `lessons/impl.md` L-001 / L-002 を削除 (欠番のまま保持)、内容が同一のみだった `lessons/ui-impl.md` はファイルごと削除。同型パターンを再捕捉する場合は、まず導線 (handbook index の常時指定とワーカーの必読ステップ) と hook が機能しているかを確認する
