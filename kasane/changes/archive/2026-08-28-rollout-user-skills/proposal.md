# Proposal: rollout-user-skills

## Why

公開リポジトリ化 (package-distribution) に向けて、利用者向けドキュメントを `docs/` (章立て型読み物) から Agent Skills (`skills/`、利用者が自分のプロジェクトへコピーして使う形式) へ置き換えると決定済みだが ([cross/ADR-0022](../../decisions/cross/0022-user-docs-as-agent-skills.md))、その実体がまだ存在しない。公開リポジトリの履歴に旧 `docs/` を一度も載せないため、public 化 (phase-2) と README 改訂 (phase-9) の前に `skills/` 一式の初回生成と `docs/` の廃止を完了させる必要がある。初期生成は改修済み docs-refresh の守備範囲外 (manifest 不在時は停止する) であり、本 change が直接行う (phase-11 決定)。

## What Changes

対象の能力 (capability) は **user-skills** (利用者向けドキュメントの提供形態) の 1 つ。

- `skills/{en,ja}/kssettingsview-{ios,android,maui,aiforms-migration}/` の 8 部を新規生成する (`SKILL.md` + `references/`。platform Skill は references 4 本 (cells / updates / styling / custom-cells)、移行 Skill は api-mapping 1 本)。生成は Skill 単位 fan-out で各ワーカーが en/ja ペアを同一文脈で同時生成する
- `skills/.manifest.json` (v3) の初期版を書き出す。規範スキーマは phase-11 のデルタスペック ([specs/docs-refresh/spec.md](../archive/2026-08-26-retarget-docs-refresh-to-skills/specs/docs-refresh/spec.md)) が正。`targets` / `excluded` が全 concept を覆う網羅不変条件を満たす
- 索引 `skills/README.md` + `skills/README_ja.md` (Skill 一覧表 / コピー手順 / 片言語コピーの前提の 3 要素)
- ルート README への導線追記 (主な特徴直後の 1 文 + モノレポ構成表の `skills/` 行)、および README 群 (ルート / android / samples) の既存 docs/ リンクの差し替え・除去 (相方スペックレビュー指摘によるスコープ追加、オーナー承認 2026-08-26)
- `docs/legacy-aiforms-reference.md` を `kasane/concepts/cross/conventions/aiforms-spec-summary.md` へ改名移送 (type: reference・凍結注記・aiforms-origin-reference との相互リンク・cross index 登載。manifest では移行 Skill の targets の源泉)
- `docs/` の廃止 (`trash docs/`、.manifest.json 含む) と残記述整理: `kasane/config.yaml` の `lint.exclude` docs/ 除外と `identity.scope` の docs 除去、concepts の docs/ 参照 2 箇所 (comment-policy 対象外リスト / test-execution の docs 節) の skills/ 体制への差し替え

レビューは 4 段 (機械検査 → ksn-review → 初見レビュー → オーナー検収)。詳細は agenda 決定事項のとおり。

## Non-Goals

- **skills/ と README 群の継続的な追従更新** — 改修済み docs-refresh の責務 (ユーザーの明示依頼で起動)。本 change は初期生成のみ
- **ルート README の本文大幅改訂・英語化・インストール手順・状態表記** — phase-9 の責務 (導線 1 文 + 表 1 行のみ本 change)
- **cross/ADR-0022 の accepted 昇格と cross/ADR-0014 の superseded 化** — 蒸留時 (ksn-distill) の責務
- **Skill の配布パッケージング (plugin / marketplace 形式)** — ロードマップの非ゴール
- **concepts 本体の内容改訂** — 本 change が触る concepts は legacy 移送と docs/ 参照のパス差し替えのみ。知識の追加・改訂は別フロー

## Impact

- 破壊的変更: `docs/` の廃止 (ディレクトリごと trash)。private リポジトリのため外部参照者はおらず、git 履歴から復元可能。コード・公開 API・テストへの変更なし
- 影響範囲: `skills/` (新規)、README 群 (ルートは導線追記 2 箇所 + docs/ リンク解消、android / samples はリンク解消のみ)、`kasane/config.yaml` (2 箇所)、`kasane/concepts/cross/conventions/` (3 ファイル)、`docs/` (廃止)
- リスク: 生成 8 部の品質のばらつき → 4 段レビュー (機械検査 / 独立レビュー / 初見レビュー / オーナー検収) で担保。manifest の網羅漏れ → 網羅不変条件の機械検査で担保 (漏れると初回 docs-refresh が停止する)

## 級: M

コード・公開 API に触れない 1 能力 (user-skills) 内の生成・置き換えで、設計判断は phase-10/11 と cross/ADR-0022 で確定済み (本 change は実施)。

domain: cross
roadmap: package-distribution/phase-12-skills-rollout
