---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-09-01
last-seen: 2026-09-01
evidence:
  - add-android-maven-distribution (design Decision 6 / spec が「公開 ABI に露出する外部型の依存は `api`」という原理と具体列挙 4 件 (compose runtime / compose-ui / kotlinx-coroutines-core / androidx.annotation) を併記したが、公開宣言の全走査をせずに列挙を確定したため、公開クラス `CellViewHolder` が継承する `RecyclerView.ViewHolder` の依存 (androidx.recyclerview) が列挙から漏れた。実装ワーカーが公開宣言の全走査で検出しオーナー判断へ — `api` 追加 (B 案) を承認、deviation 記録で解消)
---

## ルール文 (候補)

spec / design に「原理 + それを適用した具体列挙」を併記するときは、列挙を確定する前に原理の適用対象 (公開宣言・公開 API 面など) を機械的に全走査し、列挙が原理の適用結果と一致することを検算する。走査せずに既知の代表例だけで列挙すると、spec 文面上は満たせても原理が守られない漏れが生まれ、実装フェーズでオーナー判断と deviation 記録の往復が発生する。走査コストが高い場合は列挙を書かず原理だけを規定し、適用判定を実装タスクに落とす。

## 経緯

- 2026-09-01 add-android-maven-distribution: Maven 発行メタデータの依存スコープ設計 (design Decision 6) で、`api` 対象の列挙が公開 ABI 走査を経ずに確定され、androidx.recyclerview が漏れた。実装ワーカーが `.ui` の public 宣言全走査で検出し停止・報告。オーナーが `api` 追加を承認し deviation で記録。列挙どおりの実装でも spec の Scenario は VALID になるため、verify では検出できない種類の漏れだった。
- 2026-09-01 同 change (カウント外・同一 change 内の再発): 修正後もなお同型の漏れが 1 件残っていた — 公開 `Theme.sectionMargin` が露出する `PaddingValues` の宣言元 `androidx.compose.foundation:foundation-layout`。独立レビュー (review-001) が発行 aar の javap 全走査で検出し Major 指摘、`api` 追加で解消。個別発見の逐次追加では収束せず、原理の適用対象 (公開宣言全体) の機械走査を 1 回で完了させることが必要だったという裏付け。恒久策として Explicit API mode の別 change (adopt-android-explicit-api-mode) が簡易起票済み。
