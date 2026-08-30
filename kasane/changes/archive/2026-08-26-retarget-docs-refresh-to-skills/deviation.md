# Deviation: retarget-docs-refresh-to-skills

- Requirement「コード正の機械チェック」×「実行フラグ」の交差 (`--readme-only` 時に 3d③ の差分が Skill 導入節へ及ぶケース): spec では「差分があれば該当 README / Skill ファイルを要追従リストに追加する SHALL」と「`--readme-only` は skills/ 本体をスキップする SHALL」が衝突し未解決 → オーナー指示により「報告のみ (要追従リストに載せず、完了サマリで次回の通常実行へ誘導)」で確定。実行フラグ側の SHALL を優先する解釈であり spec 本文の修正は不要 (review-001.md 指摘 2 の選択肢 A)。理由: 軽量チェックの意味と manifest 整合の維持 (2026-08-26)
