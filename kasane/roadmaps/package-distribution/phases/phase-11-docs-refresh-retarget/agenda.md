# phase-11-docs-refresh-retarget

docs-refresh (`.agents/skills/docs-refresh/`) の対象を `docs/` から `skills/` + README 群へ移し、manifest 方式 (concepts ハッシュ + 影響マップ) を踏襲して Skill × 言語の単位で差分更新できるように改修する。

## 論点

(すべて解消 — 決定事項へ移動済み)

## phase-10 からの申し送り (2026-08-25)

設計は確定済み ([phase-10 決定事項](../phase-10-skills-design/agenda.md) / [cross/ADR-0022](../../../../decisions/cross/0022-user-docs-as-agent-skills.md))。実装で従う骨子:

- 対象は `skills/{en,ja}/<name>/` の 4 Skill (`kssettingsview-{ios,android,maui,aiforms-migration}`) × 2 言語 + README 群 (`skills/README.md` / `README_ja.md` を含む)
- manifest v3: concepts ハッシュ + `targets` (Skill ファイル → 源泉 concepts) + `excluded` (理由つき) + `readmes`。targets のパスは言語抜きで、en/ja は同一構成・同時更新 (翻訳ロックステップ) — 論点「2 言語の生成方式」はこの制約下で詰める
- 網羅検査 (未参照かつ未除外の concept の検出で失敗) を差分更新フローに組み込む
- 生成物の内容規約: 能力マップ + レシピ形式 + 段階開示、frontmatter は標準 6 フィールド内・en/ja 同名 (ADR-0022 Decision 参照)

## 決定事項

- **起動規律と AGENTS.md / CLAUDE.md の更新 (2026-08-25)**: 自動発動禁止 (ユーザーの明示依頼でのみ起動) は改修後スキルの description と Guardrails にそのまま引き継ぐ。AGENTS.md (CLAUDE.md はその写し) の docs-refresh 関連記述は phase-11 の change で「skills/ と README 群の書き換えは docs-refresh 経由のみ」へ書き換え、docs/ への言及は除去する。docs/ の凍結注記は置かない — docs/ は phase-12 で完全削除されるため移行ギャップ中の保護は不要
- **整合性チェックの一式とコード例規約 (2026-08-25)**: 機械検査は ①網羅検査 (manifest の targets / excluded 突合、未参照かつ未除外 concept で失敗)、②en/ja 節構成一致 (見出し階層の並びとコードブロックの数・順序)、③コードブロックの byte 一致、④frontmatter 検査 (標準 6 フィールド内・en/ja 同名・`metadata.language` とパスの一致)、⑤旧名残 grep (廃止 API・`docs/` 参照新設・openspec 参照)、⑥内部リンク解決、⑦ローカル絶対パス検査、⑧配信識別子の表記ゆれ grep。**コード例は原則コメントレス** — 説明はレシピの見出しとリード文が担う (生成プロンプトの制約に明記)。やむを得ない最小限のコメント (省略プレースホルダ等) は英語統一とし、③の byte 一致検査を成立させる
- **サブエージェント委譲の単位 (2026-08-25)**: 「メインは concepts 本文を読まない」原則と委譲方式は現行から維持。委譲単位は **Skill 単位** — 1 つの Skill の更新対象ファイル群 (両言語) を 1 サブエージェントが処理し、`SKILL.md` の能力マップと references のレシピの整合を同一文脈で保証する。README 群は Skill に属さないため対象 README (en/ja ペアがあればペア) ごとの委譲。並列上限 (最大 3 並列バッチ) は据え置き。プロンプトテンプレート本文は ksn-propose 側で確定する
- **差分更新フローの細部 (2026-08-25)**: コードを正とする機械チェック 3 種 (モジュール一覧・Sample デモ画面一覧・ツール最低バージョン) は維持し、突合先を README 群 (ルート `README.md` / `README_ja.md`、`samples/*/README.md`、記載がある場合は各 `SKILL.md` 導入節) へ付け替える。これらは源泉がコードなので manifest (源泉 = concepts のみ) には載せず、スキル本文の手順として持つ。`--readme-only` は「manifest の `readmes` に列挙された README 群のみ追従する軽量チェック」として維持 (skills/ 本体はスキップ)
- **初期生成の分担 (2026-08-25)**: docs-refresh は差分更新専用とし、skills/ 一式と manifest 初期版の生成 (concepts の配置判断・レシピ設計という創作を含む) は phase-12 の変更フローが承認つきで担う。改修後スキルは manifest (`skills/.manifest.json` version 3) 不在・破損時にフルリフレッシュへフォールバックせず、停止して初期生成を案内する。`--all` の意味は「manifest の targets / excluded を前提に本文を全再生成」に確定。旧 `docs/.manifest.json` と `docs/` の削除は phase-12 側の作業で、phase-11 はスキル本文の改修のみ。phase-11 完了〜phase-12 完了の間は docs-refresh が実行不能になるが、直後に phase-12 が続くため許容
- **2 言語の生成方式 (2026-08-25)**: 同一のサブエージェントが源泉 concepts を読み、en/ja のペアを同一文脈で同時生成する。知識の正本 (kasane/concepts/) は日本語なので、両言語とも concepts から直に書ける。構成・コード例・用語の一致は生成時点で保証され、翻訳ロックステップ ([cross/ADR-0022](../../../../decisions/cross/0022-user-docs-as-agent-skills.md)) が構造的に守られる。差分更新の基本作業単位は「言語ペア 1 組 = 1 サブエージェントタスク」。英語正→日本語翻訳派生 (往復翻訳で日本語版が劣化)、言語別の独立生成 (構成一致が事後チェック頼み) は却下

## 実装結果 (2026-08-26 反映)

変更: [changes/archive/2026-08-26-retarget-docs-refresh-to-skills](../../../../changes/archive/2026-08-26-retarget-docs-refresh-to-skills/proposal.md) (M 級)。決定事項 6 件はすべて改修後スキル本文に反映され、review-003 APPROVED / verify-003 VALID (12 Requirement / 20 Scenario 全件一致) で完了。

- **決定の追加確定 (deviation 1 件)**: `--readme-only` 実行中にコード正の機械チェック (ツール最低バージョン) の差分が Skill 導入節へ及ぶ交差ケースで、「差分は要追従リストへ SHALL」と「`--readme-only` は skills/ 本体をスキップ SHALL」が衝突した。オーナー判断で**報告のみ** (要追従リストに載せず完了サマリで次回の通常実行へ誘導) に確定。実行フラグ側を優先する解釈
- **実装で確定した細部** (議論時に未確定だったもの): `--all` がスキップするのはハッシュ差分検出のみで網羅検査は実行する / `--readme-only` は manifest を更新しない / 削除済み concept は承認済みの整理が完了して初めて manifest の `concepts` から落とす / Step 6 の整合性チェック 8 種はモード分岐表で扱いを行ごとに確定する
- **レビューの経過**: 相方 spec-review が Major 8 件 (ホスト側自己レビューは 2 周とも指摘 0)、実装レビューは 3 サイクル。いずれも `--readme-only` という横断モードと各手順の交差に集中した。教訓 2 件を lessons/inbox に捕捉済み

### 申し送り

- **ADR-0022 の accepted 昇格と cross/ADR-0014 の superseded 化**: 受け皿は [phase-12 agenda](../phase-12-skills-rollout/agenda.md) の phase-10 申し送り (既記載)。蒸留時に同 ADR の Decision へ責務分界 (初期生成・構成見直しは変更フロー / docs-refresh は追従専用・manifest 不在時は停止) を追記済みのため、昇格時はこの項も含めて確認する → phase-12 へ追記済み
- **docs/ と `docs/.manifest.json` の削除、`lint.exclude` の docs/ 除外解除、`lint.identity.scope` からの `docs` 除去**: 受け皿は phase-12 agenda の論点 (既記載)
- **deviation で確定した「報告のみ」の運用**: 初期生成後に docs-refresh を初めて通常運用するのは phase-12 のため、phase-12 agenda へ申し送り済み
- **docs-refresh の実行不能期間**: 本フェーズ完了から phase-12 完了までのギャップは議論時に許容と合意済み。追加の受け皿は設けない (phase-12 完了で解消)

## TODO

- [x] 論点の解消 (2026-08-25)
- [x] ksn-propose で変更提案を起こす (2026-08-25)
- [x] 実装完了・蒸留・アーカイブ (2026-08-26)
