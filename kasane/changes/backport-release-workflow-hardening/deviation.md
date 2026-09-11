# Deviation: backport-release-workflow-hardening

- [付随修正] `.github/workflows/ci.yml` の lint job: tasks 4.3 が名指しするのは `wait-for-registries.sh --selftest` の追加だけだが、tasks 3.4 で新設した `deployment-handover.sh --selftest` も同じ並びへ追加した。理由: 自己テストを CI に繋がないと腐り、既存の selftest (central-portal / set-readme-version / sync-snapshot) はすべて lint job に並んでいるため (2026-09-11)
- [付随修正] `.github/workflows/ci.yml` の lint job: レビュー指摘 3 (順序検査の前後関係) の修正で `check-publish-step-order.py --selftest` を新設し、同じ並びへ追加した。tasks 3.5 が求めるのは検査を lint job で走らせることまでで、検査自身の自己テストは文面外。理由: 破壊テストを手動確認で済ませず自動化するため (2026-09-11)
- `scripts/release/check-time-budget.py` / `.github/workflows/release.yml`: spec は反映待ち job (`wait-for-registries`) の時間予算を規定していないが、オーナー判断により「スクリプトの上界 < job の `timeout-minutes`」を機械検査に含めた。理由: Requirement「公開レジストリへの反映待ち」の「上限に達して失敗するときは、対象ごとに保持している分類を出力に含める SHALL」は、job の打ち切りが先に来ると成立しない。この関係は SHALL の前提条件であり、仕様設計フェーズで書き漏らしたもの (実際に修正サイクル 1 でこの関係が破れ、Major として検出された)。spec の 240 分の予算表 (publish job) には手を入れていない (2026-09-11)
- tasks 5.2 の検証手段: `dry-run` は `develop` の HEAD から直接起動できないため (validate の `Verify install examples` が README 2 枚と Skill 8 枚の計 14 行の version 一致を要求する)、検証専用ブランチ `verify/dry-run-backport-hardening` を作り、`AGENTS.md` が認める機械置換で インストール例を未使用の `0.1.0-beta.3` へ揃えたうえで実行した。この置換コミットは本 change の成果物ではなく、`develop` へはマージしない。証跡は [evidence/dry-run-verification.md](evidence/dry-run-verification.md) (2026-09-11)

## 蒸留への申し送り

- 反映待ち job の時間予算の関係 (待機の上限 + 応答上限 1 件ぶん + job のオーバーヘッド余裕 < `timeout-minutes`) を `kasane/concepts/cross/architecture/release-pipeline.md` へ残す。「期限を跨げる照会は常に 1 件まで」という上界の根拠は `wait-for-registries.sh` の巡回構造に依存するため、構造が変われば式も変わる。コードから再導出しにくい設計判断であり、concepts 側に置く価値がある
