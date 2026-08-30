# Proposal: retarget-docs-refresh-to-skills

## Why

利用者向けドキュメントの提供形態が `docs/` (章立て型の読み物) から `skills/` (Agent Skills、en/ja 2 版) へ移ることが確定した ([cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md)、proposed)。しかし追従道具である docs-refresh スキル ([.agents/skills/docs-refresh/SKILL.md](../../../.agents/skills/docs-refresh/SKILL.md)) は `docs/` + manifest v2 向けのままで、skills/ を concepts に追従させる手段が存在しない。skills/ の初期生成 (phase-12) が始まる前に、追従道具を skills/ 対象へ改修しておく。

## What Changes

- **docs-refresh スキル本文の改修** (`.agents/skills/docs-refresh/SKILL.md`、場所と名前は据え置き):
  - 対象を `skills/{en,ja}/kssettingsview-{ios,android,maui,aiforms-migration}/` の 4 Skill × 2 言語 + README 群へ切り替え、manifest を `skills/.manifest.json` (version 3: concepts ハッシュ + `targets` 逆引き + `excluded` + `readmes`) へ移す
  - 差分更新は Skill 単位でサブエージェントへ委譲し (メインは concepts 本文を読まない・最大 3 並列)、1 サブエージェントが en/ja ペアを同一文脈で同時生成する (翻訳ロックステップ)
  - 初期生成は対象外: manifest 不在・破損時はフルリフレッシュへフォールバックせず、停止して初期生成 (phase-12 の変更フロー) を案内する
  - `--all` = manifest の targets / excluded を前提とした本文の全再生成、`--readme-only` = manifest の `readmes` 列挙分のみの軽量チェック、と再定義する
  - コード正の機械チェック 3 種 (モジュール一覧・Sample デモ一覧・ツール最低バージョン) の突合先を README 群へ付け替えて維持する
  - 整合性チェックを 8 種 (網羅検査 / en/ja 節構成一致 / コードブロック byte 一致 / frontmatter / 旧名残 grep / 内部リンク解決 / ローカル絶対パス / 配信識別子表記ゆれ) に刷新する
  - 生成プロンプトの内容規約 (能力マップ + レシピ形式 + 段階開示、コード例は原則コメントレス・例外は英語統一) を組み込む
- **規約記述の更新**: AGENTS.md (CLAUDE.md はその写し) と `kasane/config.yaml` の context にある docs-refresh / docs/ 関連記述を「skills/ と README 群の**継続的な追従更新**は docs-refresh 経由のみ (初期生成・構成の見直しは承認済み change の実装として行う)」へ書き換え、docs/ への言及を除去する (凍結注記は置かない — docs/ は phase-12 で完全削除)。あわせて `lint.identity.scope` に `skills` を追加し、公開対象の成果物を個体・個人・秘密情報の検査範囲に入れる (`docs` の除去と `lint.exclude` の解除は phase-12)

影響する能力: docs-refresh (利用者向けドキュメントの追従更新) のみ。

## Non-Goals

- **skills/ 一式と manifest 初期版の生成**: 初期生成は配置判断・レシピ設計という創作を含み、phase-12 の変更フローが承認つきで担う (phase-11 の決定事項)
- **docs/ と旧 `docs/.manifest.json` の削除、lint.exclude の docs/ 除外解除**: docs/ 廃止は phase-12 の作業 (roadmap の フェーズ分担)
- **ルート README への Skills 導線・インストール手順の執筆**: phase-9 の責務
- **Kasane ハーネス側スキルの変更**: ロードマップの非ゴール

## Impact

- 製品コード・公開 API への影響なし。変更対象はスキル文書と規約記述のみ
- 本 change 完了から phase-12 完了までの間、docs-refresh は実行不能になる (skills/ 未生成のため停止案内のみ)。合意済みのギャップ
- 旧 manifest (docs/.manifest.json v2) は読まなくなるが、ファイル自体は phase-12 まで残置

## 級: M

1 能力 (docs-refresh) 内の改修だが、manifest スキーマ刷新とフロー再設計を含むため S ではない。

domain: cross
roadmap: package-distribution/phase-11-docs-refresh-retarget
