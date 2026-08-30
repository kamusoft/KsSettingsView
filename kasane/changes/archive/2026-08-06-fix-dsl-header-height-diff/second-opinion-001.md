# セカンドオピニオン: fix-dsl-header-height-diff (001 — spec-review)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 提案一式 (proposal.md / specs/ ×2 / tasks.md)
---
静的レビュー結果です。ビルド・テスト・書き込みは実施していません。

- **Critical**
  - **該当箇所:** [proposal.md:9](kasane/changes/fix-dsl-header-height-diff/proposal.md:9)、[proposal.md:19](kasane/changes/fix-dsl-header-height-diff/proposal.md:19)、[iOS spec.md:9](kasane/changes/fix-dsl-header-height-diff/specs/settings-view-ios-ui/spec.md:9)、[tasks.md:12](kasane/changes/fix-dsl-header-height-diff/tasks.md:12)、[KsSettingsViewController.swift:1173](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1173)
  - **問題点:** 「headerHeight 検出時は `.full` のみ」「変更は preflight 追加のみ」と、iOS の「headerHeight と同一 ID Cell 内容の同時反映」が両立していません。現行 `applyFullSnapshot` は同一 ID Cell を reconfigure せず、Section reload 条件も header/footer accessory の変化だけで `headerHeight` を含みません。既存テストも、header accessory 不変の `.full` では Cell を再構成しない契約を固定しています（[SectionAccessoryRenderingTests.swift:466](ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:466)）。したがって `.full(newRoot)` だけではモデルは更新されても、表示中の Cell title は古いままになる可能性が高く、明記された Scenario を満たせません。一方、これを host の `.full` 全般で修正すると Non-Goal の `fix-ios-full-content-refresh` と衝突します。
  - **推奨修正:** 実装前に方式を確定してください。例えば、headerHeight preflight 時だけ `.full` に続けて内容変更 Cell の `.replaceCell` を発行するなら、「Full のみ」を撤回して順序と期待 diff 列を仕様化します。host 側で当該ケースだけ changed Cell を reconfigure するなら、`KsSettingsViewController.swift` とそのテストを明示的にスコープへ加え、Non-Goal・Impact・tasks を整合させてください。

- **Major**
  - **該当箇所:** [iOS spec.md:26](kasane/changes/fix-dsl-header-height-diff/specs/settings-view-ios-ui/spec.md:26)、[iOS spec.md:30](kasane/changes/fix-dsl-header-height-diff/specs/settings-view-ios-ui/spec.md:30)、[tasks.md:17](kasane/changes/fix-dsl-header-height-diff/tasks.md:17)
  - **問題点:** Store Requirement は `replaceSection` と `.full` の両方を SHALL 対象にしていますが、Scenario と task は `replaceSection` しか扱っていません。今回 DSL preflight が実際に利用するのは `.full` なので、`replaceSection` の確認だけでは DSL 経路の適用先を保証できません。
  - **推奨修正:** 「Store `.full` で同一 Section ID・同一 header accessory のまま headerHeight のみ変更」という Scenario と自動テスト task を追加してください。表示中 supplementary の layout attributes/frame 高さを更新後に観測する必要があります。

- **Major**
  - **該当箇所:** [ADR-0018:18](kasane/decisions/core/0018-store-dsl-path-result-symmetry.md:18)、[proposal.md:13](kasane/changes/fix-dsl-header-height-diff/proposal.md:13)、[tasks.md:19](kasane/changes/fix-dsl-header-height-diff/tasks.md:19)
  - **問題点:** ADR は Store/DSL 両経路の「反映テスト」を義務化していますが、task 3.1 は単体観測が難しければ目視で代替可能としています。目視証跡は将来の回帰を自動検出できず、「対称テスト義務の初適用」「4象限を閉じる」という提案目的を満たしません。
  - **推奨修正:** シミュレータ XCTest を必須にし、目視 A/B は補助証跡に限定してください。テストでは `controller.root` や `.full` payload だけでなく、表示中 header の実高さと表示中 Cell の title を観測してください。

- **Minor**
  - **該当箇所:** [Android spec.md:9](kasane/changes/fix-dsl-header-height-diff/specs/settings-view-android-ui/spec.md:9)、[iOS spec.md:11](kasane/changes/fix-dsl-header-height-diff/specs/settings-view-ios-ui/spec.md:11)
  - **問題点:** 「headerHeight が変わる」としか規定されておらず、どの遷移を受け入れ対象にするか不明です。既存モデル契約は `-1 = 自動`、`> 0 = 固定` ですが、Scenario の「新しい固定高さ」は `-1 → 正値`、`正値 → 正値`、`正値 → -1` のどれを指すのか判定できません。
  - **推奨修正:** 最低限 `-1 → 正値` と `正値A → 正値B` を明示し、固定高さ解除も対象なら `正値 → -1` を追加してください。テスト値と期待する表示モードも記載してください。

総合判定: CHANGES_REQUESTED

## 突き合わせ結果 (ホスト側自己レビューとの照合)

| # | 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|---|
| 1 | Critical: iOS「.full のみ」と「同時変更の内容反映」が現行 host では両立しない | 相方のみ | **採用** | ホスト側も fix-ios-full-content-refresh として同じ機構欠如を起票済みで整合。既存テスト (SectionAccessoryRenderingTests.swift:466) が「.full で Cell 非再構成」の契約を固定している指摘は具体的。「担保方法は実装判断」の逃げでは proposal の「.full のみ」と矛盾したまま — spec で diff 列を確定する必要がある |
| 2 | Major: Store Requirement の SHALL に `.full` が入っているのに Scenario/task は replaceSection のみ | 相方のみ | **採用** | DSL preflight の実適用先は `.full` であり、replaceSection の確認だけでは対称を閉じられない。実害シナリオ明確 |
| 3 | Major: task 3.1 の「目視代替可」は ADR-0018 の対称テスト義務と矛盾 | 相方のみ | **採用** | ADR は自動テストを義務化しており、目視は回帰検出にならない。上位層 (accepted ADR) との整合が優先 |
| 4 | Minor: headerHeight の遷移範囲 (-1 ↔ 正値 / 正値間) が未規定 | 相方のみ | **採用** | モデル契約 (-1 = 自動) が存在する以上、受け入れ対象の遷移は spec で確定すべき。修正コスト小 |

確定 0 / 採用 4 / 降格 0 / 未解決 0。総合判定 CHANGES_REQUESTED を受け、提案を修正のうえユーザー確認へ。
