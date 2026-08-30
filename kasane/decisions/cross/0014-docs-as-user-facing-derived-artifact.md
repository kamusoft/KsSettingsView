---
id: 0014
title: docs/ は利用者向け派生ドキュメントとして維持し、concepts から追従させる
status: superseded
date: 2026-07-19
---

## Context

remigrate-concepts で concepts を全面再生成した結果、concepts は開発者・エージェント向けの契約文書 (責務境界・保証・禁止事項・公開契約) として完成した。一方で当初方針は「docs/ の内容を concepts へ吸収し、docs はスタブ化する」だったが、再生成された concepts をオーナーが確認した結果、**concepts はライブラリ利用者が読んで理解できる文書ではない**ことが明確になった。読者が異なる (開発知識 vs 利用者ガイド) ため、concepts は docs の代替にならない。

docs-refresh スキルは openspec/specs 追従を前提としており、openspec 凍結に伴い一時撤去されていた。

## Decision

- `docs/` はライブラリ利用者向けドキュメントとして維持し、凍結もスタブ化もしない。
- 知識の正本は `kasane/concepts/` とコード・テストとし、docs はそこから**利用者向けに翻訳した派生物**とする。
- docs の追従更新は docs-refresh スキル (v2: concepts 追従版・自動発動禁止・オーナー承認フロー付き) の責務とする。
- エージェントは開発時に docs を知識参照先にしない (AGENTS.md に明記)。docs と concepts が食い違う場合は docs 側を直す。

## Alternatives Considered

- **docs を concepts へ吸収しスタブ化する (当初方針)**: 再生成された concepts の実物確認により、concepts は契約文書であって利用者が読める文書にならないと判明したため撤回 (2026-07-19 オーナー判断)。
- **docs を歴史資料として凍結する**: 利用者向けの生きたドキュメントが失われ、API 変更のたびに docs が陳腐化していくため採用しない。
- **concepts 自体を利用者向けの読みやすさで書く**: エージェント向けの契約密度と利用者向けの平易さは同一文書で両立せず、どちらの読者にも最適でなくなるため採用しない。

## Consequences

- 正: 読者ごとに最適化した文書を両立できる。concepts は契約密度を、docs は利用者向けの分かりやすさを、互いに妥協せず保てる。
- 正: docs の鮮度は manifest ハッシュ差分 + docs-refresh で機械的に管理できる。
- 負: concepts → docs の二段メンテナンスが残り、concepts 更新後に docs-refresh を実行する運用負担が発生する。
- 負: docs は正本ではないため一時的に古くなり得る。エージェントが docs を知識参照先にしない規律 (AGENTS.md) が前提になる。

---
出典: 2026-07-19 オーナー判断 (remigrate-concepts Batch E レビュー後の方針転換。当初の docs 吸収方針を撤回)
