---
scope: impl
kind: pain
severity: normal
count: 1
first-seen: 2026-09-02
last-seen: 2026-09-02
evidence:
  - docs-refresh-worker-dispatch (`.agents/skills/` 配下の docs-refresh に、サブエージェントの器指定を Claude Code 固有の語 `subagent_type` だけで書き、独立レビュー (review-001 / 002) も通過した後にオーナーが「codex も考慮が必要」と指摘。`.agents/` は Claude Code と codex の両方から読まれる共有配置であり、codex はエージェント name で spawn する。指揮側のコンテキストパッケージ自体が `subagent_type` の語で指示していたのが起点)
---

## ルール文 (候補)

複数の CLI から共有される配置 (`.agents/skills/` / `~/.agents/skills/`) にあるスキル本文へ、サブエージェント起動・ツール呼び出し・設定ファイルの場所など**実行環境に依存する手順**を書くときは、書く前にそのスキルの配置先を確認し、共有配置なら環境ごとの対応 (Claude Code / codex の両方) を併記するか、環境非依存の語 (「器を指定して起動する」等) で書いて具体の指定方法を括弧書きに降格する。1 環境の固有語 (`Task` / `subagent_type` / `~/.claude/agents/`) だけで書かない。レビューはスキルの配置先が共有かを最初に確認し、共有なら「もう一方の CLI で読んだときに手順が成立するか」を判定項目に含める。事後判定: 共有配置のスキル本文を grep して、1 環境の固有語が対応表記なしに現れない。

## 経緯

- 2026-09-02 docs-refresh-worker-dispatch: 指揮側が委譲パッケージで「Task の subagent_type を ksn-implementer に固定」と Claude Code の語で指示し、実装・レビュー (2 サイクル) ともその語のまま通過。オーナー指摘で SKILL.md 5 箇所とテンプレート 2 本を「器を指定する (Claude Code: subagent_type / codex: エージェント name)」の形に修正、Step 1 の器存在確認に codex 側の器定義パス (`~/.codex/agents/*.toml`) を追加した。ksn-core の worker-dispatch には両環境の書き分けが既にあり、それを参照しない (Kasane 非依存) 方針を取った時点で書き分けの知識も一緒に落ちた。
