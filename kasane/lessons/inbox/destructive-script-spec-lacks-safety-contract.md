---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-01
last-seen: 2026-09-01
evidence:
  - add-spm-distribution (相方 spec-review Critical: スナップショット同期スクリプトの spec 初版が「同期先の `.git` 以外を除去して 5 点を配置する」と破壊的操作を規定しながら、同期先の正当性検証・検証失敗時の無変更保証・git 非操作の観測範囲を持たず、誤指定 1 回で任意の作業ツリーを消去できる形のまま自己レビューを通過した。design.md Decision 1 (4 段の事前検証 + 失敗時は同期先を一切変更しない) を SHALL / Scenario に起こして解消)
---

## ルール文 (候補)

破壊的操作 (ファイル・ディレクトリの削除、既存内容の全置換) を含むスクリプト・工程を spec に立てるときは、「何を配置・生成するか」の規定だけで終えず、(1) 破壊的操作の**前**に対象の正当性を機械検証する SHALL、(2) 検証失敗時に対象を一切変更しない SHALL、(3) 誤指定 (無関係のディレクトリ・自分自身・祖先) を拒否する Scenario、の 3 点を含めてから自己レビューを通す。正常系の配置内容がどれだけ精密でも、誤対象への発火は防げない。

## 経緯

- 2026-09-01 add-spm-distribution: 同期スクリプトの spec は配置するホワイトリスト 5 点と冪等性を精密に規定していたが、同期先引数の検証がなく、monorepo 本体や無関係の作業ツリーを渡すと `.git` 以外を全削除する構造だった。相方 (codex) の spec-review が Critical として捕捉し、コピー元全件検証 / git top-level 確認 / origin remote 照合 / 自己・祖先拒否の 4 段検証と「検証失敗時は同期先を一切変更しない」を spec 化。実装ではミューテーション (ガード無効化) で拒否テストの回帰検出力も実証された。

## 関連

- [failure-contract-missing-public-state-and-recovery](failure-contract-missing-public-state-and-recovery.md) — 同族 (失敗系契約の欠落)。あちらは API の失敗後の公開状態と回復経路、こちらは破壊的操作の発火前提と無変更保証。
