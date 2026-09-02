# レビュー結果: docs-refresh-worker-dispatch (003 回目)

**日付**: 2026-09-02
**判定**: APPROVED

## サマリー

オーナー指摘 (器の指定が Claude Code 固有の語だけで書かれていた) は解消された。SKILL.md とテンプレート 2 本に残る環境固有語は 6 箇所で、いずれも同一行に両環境の対応が併記されている。codex 実行環境での成立性も、器定義ファイル・配置スクリプト・器の自衛ルールの 3 点を実物で突き合わせて確認でき、Step 1 → Step 5 の手順は codex でもそのまま通る。review-002 までの合意内容 (Kasane 非依存・器なし起動の禁止・編成の切り離しという根拠) はいずれも崩れておらず、器なし起動の禁止はむしろ環境非依存の表現になって適用範囲が広がっている。

新規の指摘なし。停止案内テキストにも環境固有の表現は残っていない。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 今サイクルの修正は `.md` のみで新設 `.py` に変更なし。前サイクルの手読み結果を維持
- `kasane/handbook/cross/user-skill-api-listing.md` (きっかけ: docs-refresh の 3e 検査の記述に触れるため) — 3e 周辺に変更なし
- ドメイン: cross。追加のドメインスキルなし

## 確認結果

### (a) 1 環境の固有語が対応表記なしに残っていないか → 残っていない

`subagent_type` / `Task` / `spawn` / `Claude Code` / `codex` / `~/.claude` / `~/.codex` を SKILL.md とテンプレート 2 本に対して grep したところ、ヒットは 6 行のみで、すべて同一行内に両環境の対応が書かれている。

| 箇所 | 表記 |
|---|---|
| `SKILL.md:50` (コンテキスト節約方針) | 「起動の指定方法は実行環境ごとに異なる — Claude Code では Task ツールの `subagent_type`、codex ではエージェント name を指定して spawn する。以降「器を指定する」はこの意味」 |
| `SKILL.md:64` (Step 1) | 器定義ファイルの存在確認先を Claude Code / codex で併記 |
| `SKILL.md:239` (Step 5) | 「Claude Code: `subagent_type: ksn-implementer` / codex: エージェント name `ksn-implementer` で spawn」 |
| `SKILL.md:457` (Guardrails) | 「Claude Code: `subagent_type` / codex: エージェント name」 |
| `references/prompt-skill.md:3` / `prompt-readme.md:3` | 同上の併記 |

`:50` が「以降「器を指定する」はこの意味」と環境非依存の語を定義しているため、これ以外の箇所 (Step 5 の本文・Guardrails の主文・`:250` の器の自衛ルール説明) は環境固有語なしで読める。`:64` の「利用可能なサブエージェント種別の一覧」も特定製品の用語ではなく、両環境で意味が通る。

**停止案内テキスト (`:68`〜`:78`) には環境固有の表現なし**。「サブエージェントの器 ksn-implementer がこの環境に配置されていません」「メインが代わりに読んで書くフォールバックを持ちません」「deploy.sh を実行して器を配置してから再実行してください」のいずれも環境を選ばない。指摘事項なし。

### (b) codex 実行環境で Step 1 → Step 5 が成立するか → 成立する

実物を突き合わせて確認した。

- **器の確認手段** — `:64` が挙げる `~/.codex/agents/ksn-implementer.toml` は実在し、Kasane の配置スクリプト (`../Kasane/scripts/deploy.sh`) が `~/.codex` の存在を見て `agents/codex/ksn-*.toml` を `~/.codex/agents` へ配る設計と一致する。Claude Code 側だけユーザースコープとプロジェクトスコープの 2 つを挙げ、codex 側はユーザースコープのみなのも、配置スクリプトの実装 (codex はユーザースコープにしか配らない) と整合しており正しい非対称である
- **未配置時の復旧経路** — 停止案内が示す deploy.sh は Claude Code 用 `.md` と codex 用 `.toml` の両方を配るため、codex 利用者がこの案内に従って復旧できる。案内が Claude Code 専用の手段を指してしまう問題はない
- **起動指定** — codex の器定義は `name = "ksn-implementer"` を持ち、`:239` の「エージェント name `ksn-implementer` で spawn」と一致する
- **器の自衛ルール** — `:250` は「器 `ksn-implementer` は…パッケージなしで起動されたら作業しない」ことを前提にテンプレートのコンテキストパッケージ節を必須としている。codex 側の器定義の `developer_instructions` は Claude Code 側と同一の 3 項目 (読むべきスキルの解決、パッケージなしなら作業しない、規律の正はスキル側) を持つため、この前提は両環境で成立する
- **根拠文の妥当性** — codex 側の器定義はモデルと推論エフォートを宣言しており、`:50` の「器定義が持つモデル・エフォートで走り、メインの編成から切り離せる」は codex 環境でも (むしろメインとモデル系統が異なりうる分、より明瞭に) 成立する

なお、テンプレートの環境併記はどちらも**コードフェンスの外側** (`prompt-skill.md:3` はフェンス開始 `:5` の前、`prompt-readme.md:3` は同 `:7` の前) にあり、サブエージェントへ渡るプロンプト本文には混入しない。委譲先が読む内容は前サイクルから変わっていない。

### (c) review-002 までの合意内容が崩れていないか → 崩れていない

- **Kasane 非依存** — `:50` の「外部の編成定義 (Kasane の worker-dispatch や `kasane/config.yaml` の `workers:` 節) は参照しない」はそのまま残る (grep 1 件)。今回追加された `~/.codex/agents/` は実行環境の器の置き場であって Kasane の編成定義ではないため、非依存の決定に抵触しない
- **器なし起動の禁止** — 固定は 6 箇所 (SKILL.md 4 + テンプレート 2) で維持。Guardrails は「器の指定なしで Task を起動しない」から「器の指定なしでサブエージェントを起動しない」へ一般化され、Claude Code 以外の起動経路も禁止対象に入った。規律は弱まらず強まっている
- **根拠文 (編成の切り離し)** — `:50` の本文は環境併記の括弧が前置きされただけで、条件付きの前提・編成の切り離し・「文脈隔離自体は器の有無によらず成立する」の 3 点はそのまま。「コンテキスト節約が成立しなくなる」は SKILL.md 全体で 0 件を維持
- **足場** — `exploration.md` と `deviation.md` は未変更

## 回帰確認 (実施記録)

- 切り出しスクリプト 8 本は今サイクルも未変更。出力一致を再実測し、`concepts-coverage-check.py` / `api-coverage-check.py` / `heading-parity-check.py` / `planned-manifest.py` (判断なし) の 4 本すべてで標準出力・終了コードが一致
- テンプレート 2 本のフェンス内本文は不変 (コンテキストパッケージ 4 項目・内容規約・言語の扱い・指示が揃っており、前サイクルで自己完結化した箇所に `Step 3d` / `[6-③]` の解決不能な参照は再混入していない — grep 0 件)
- SKILL.md にインライン Python の再混入なし (`grep "python3 - <<"` 0 件)
- `python3 scripts/local-path-lint.py` / `identity-lint.py` — 触った全ファイルと change アーティファクト一式で exit 0。追加された `~/.codex/...` はチルダ相対でローカル絶対パスに当たらない

## 指摘事項

新規の指摘なし。review-001 の 🔵 Suggestion 2 件 (`concepts-coverage-check.py:5` の `kasane/` パス参照、`link-resolution-check.py:20` の入力パスハードコードと不在ガード) はオーナーが対応不要と判断済みで、今サイクルでも状況は変わっていない。再提起はしない。

## アクションプラン

対応必須の項目なし。蒸留に進んでよい。review-002 の申し送り (トークン削減の主因は SKILL.md の減量側、器の固定が担うのは編成の切り離し。長命層へ残す場合は SKILL.md の記述を正とする) はそのまま有効で、これに 1 点足す: docs-refresh は Claude Code と codex の両方から読まれる共有スキルであり、器の指定・器の存在確認・復旧案内は片方の環境の語だけで書かない — この制約は本スキル固有ではなく `.agents/skills/` 配下の共有スキル全般に効くため、長命層へ残す価値がある。
