---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-31
last-seen: 2026-08-31
evidence:
  - add-verification-ci (Requirement「マージ保護」が `develop` / `main` 両ブランチへの branch protection 設定を要求し tasks も両方を指示していたが、リポジトリに `main` ブランチが存在しなかった。public 化で履歴を引き継がない新規リポジトリとして作られ `develop` 1 本だけが push された状態で、デフォルトブランチも `develop`。実装フェーズの設定操作で 404 として露呈し、適用範囲を `develop` のみに縮小してオーナー裁定を仰ぐ往復が生じた。フェーズ議論の決定事項の時点で両ブランチを前提にしており、提案・spec レビューのどちらも実在を確認していない)
---

## ルール文

デルタスペックの Requirement が、コードではなく**リポジトリ外部の状態**を対象にするとき (ブランチ・リポジトリ設定・CI の登録内容・外部サービス上のリソース) は、提案の段階でその対象が実在することを確認し、確認結果を proposal または exploration に記録する。実在しない対象への要求は実装フェーズの設定操作まで露呈せず、そこで適用範囲の縮小と裁定の往復が発生する。

対象が「これから作られる」ものである場合は、Requirement をその作成を担うフェーズ・変更へ寄せるか、作成を前提条件として proposal に明記する。

事後判定: spec が外部状態を対象とする Requirement を持つ場合、その対象の実在 (または作成の担当) が proposal / exploration のいずれかに記録されている。

## 経緯

- 2026-08-31 add-verification-ci: 検証 CI の 4 job を必須 status check とするマージ保護を `develop` / `main` に設定する Requirement を立てたが、`main` は未作成だった。classic branch protection はブランチ名で解決するため設定自体が不可能で、`develop` のみへの適用と `main` 分の後続フェーズ (リリース) への申し送りをオーナー裁定で決めた (deviation 記録済み)。守るべき対象 (`main` への PR・push) が存在しない時点だったため実害は生じていないが、実装を止めて判断を仰ぐ往復が発生した。関連: 参照先の状態を確認せず spec に書く型として [[proposed-decision-treated-as-settled-in-spec]] (ADR の `status` 未確認) と同族。
