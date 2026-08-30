---
scope: spec-review
kind: pain
severity: normal
count: 1
first-seen: 2026-08-07
last-seen: 2026-08-07
evidence:
  - clarify-host-attach-order-contract (ios-host / android-host の2能力にまたがる共通ライフサイクル契約 + 公開挙動変更を M として提案。相方 spec-review が Kasane 基準との不一致を指摘し L へ再分類、design.md を追加)
---

## ルール文

提案の級判定は What Changes を ksn-core の S/M/L 基準と明示的に突き合わせて確定する。複数 capability にまたがる契約・公開挙動の変更、または復元対象・優先順位・境界定義などの未決定の設計判断を含む変更は、触るファイル数が少なくても L と判定して design.md で決定を確定する。

## 経緯

- 2026-08-28 align-timepicker-hour-cycle-across-platforms (境界の観測、count には数えない): 8 capability 横断の is24Hour 追加を M で提案し、相方 spec-review (second-opinion-spec-001 Major) の「L + design 追加」指摘に対しオーナーが M 維持を裁定 (同形の前例 add-entrycell-placeholder-color の M 運用に準拠、未決の設計判断なし。domain は core → cross へ指摘どおり修正)。「未決定の設計判断を含むか」を発動本質とする本ルールの適用どおりの結果で、proposal の級セクションに裁定根拠を明記して確定した。
- 2026-08-09 harden-update-accessory-unknown-id (境界の観測、count には数えない): 3 capability (ios-store / android-store / maui-bridge) 横断の契約変更を M として提案し、相方 spec-review (second-opinion-001 Major) の「L へ変更 or M の理由明記」指摘に対し、「実体は単一契約 (core/ADR-0020) の platform 投影で未決の設計判断ゼロ」を proposal の級セクションに明記して M を維持 (前例: fix-dsl-header-height-diff)。add-maui-samples-foundation の境界整理 (未決ゼロの横断はオーナー裁定で M があり得る) と同型で、明記対応が突き合わせで採用された。

- 2026-08-09 add-maui-samples-foundation (境界の観測、count には数えない): 2 capability (samples-maui 新設 + maui-bridge の依存整合 Requirement 1件) の横断だが、設計判断がフェーズ議論 (agenda + maui/ADR-0010) で全て確定済み・未決ゼロのケース。相方 spec-review の「L 化 or 分割」指摘に対しオーナーは M 維持を裁定した。本ルールの発動本質は「未決定の設計判断を含む」ことにあり、決定済み・低リスクの横断はオーナー裁定で M があり得る — L 側へ倒す機械的判定にしない。

- 2026-08-07 clarify-host-attach-order-contract: 両 OS Host の共通ライフサイクル契約の確定 (ADR 付き・MAUI E2E 影響あり) を M 級で提案した。相方 spec-review (second-opinion-001 Major) が「複数能力横断／アーキテクチャ変更は L」との不一致を指摘し、L へ昇格して design.md (Decision 1〜4) を追加。未決定だった復元対象・Theme 優先順位・収束境界はいずれも design での確定が必要だったことが裏付けとなった。
