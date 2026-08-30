# セカンドオピニオン: fix-dsl-header-height-diff (002 — code-review)
**相方**: codex / **日付**: 2026-08-05 / **対象**: 実装 diff (HEAD=5f7d97e からの未コミット変更 5 ファイル) + 提案一式 + verification/
---
提示されたテスト成功結果と A/B 証跡は確認できましたが、仕様逸脱につながる分岐と未充足のテスト要件があるため、判定は `CHANGES_REQUESTED` です。

## Critical

なし。

## Major

### iOS で可視性変更が同時に起きると Cell 内容更新が欠落する

**該当箇所**: [DSLDiffCalculator.swift:62](ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:62)、[DSLDiffCalculator.swift:75](ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:75)

**問題点**: 可視性 preflight が headerHeight preflight より先に早期 return します。そのため、同じ再評価で以下が重なると `.full` だけが発行されます。

- いずれかの Section／Cell の可視性変更
- `headerHeight` の変更
- 同一 ID Cell の内容変更

iOS の `.full` は同一 ID Cell を再構成しないため、header の高さは更新されても Cell 表示は古いまま残ります。これは [iOS デルタスペック:9](kasane/changes/fix-dsl-header-height-diff/specs/settings-view-ios-ui/spec.md:9) の「同一再評価内なら `.full` に続けて `.replaceCell`」という無条件の要求に反します。現在のテストは可視性変更を組み合わせていないため検出できません。

**推奨修正**: preflight 判定を統合し、headerHeight が変化した再評価では、可視性変更も同時に存在する場合を含めて必要な `.replaceCell` を保持してください。「別 Section の可視性変更＋headerHeight＋同一 ID Cell title 変更」の回帰テストも追加してください。

### 「Store 経由」の自動テストが Store を通っていない

**該当箇所**: [SectionAccessoryRenderingTests.swift:382](ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:382)、[SectionAccessoryRenderingTests.swift:537](ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:537)、[SectionAccessoryRenderingTests.swift:573](ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:573)

**問題点**: テスト用 Controller は `root:` initializer で生成され、更新も `controller.applyDiff(...)` を直接呼んでいます。したがって、デルタスペックの「Store 接続で表示し、Store の公開操作から反映する」シナリオや、[tasks.md:19](kasane/changes/fix-dsl-header-height-diff/tasks.md:19) で完了扱いにした Store→Publisher→Controller 経路は実際には検証されていません。

目視証跡は `replaceSection` を補っていますが、タスク自身が「目視はテストの代替にしない」と定めており、`replaceAll`／`.full` の Store 経路証跡もありません。

**推奨修正**: `SettingsRootStore` を渡して Controller をホストし、テスト操作をそれぞれ `store.replaceSection(...)` と `store.replaceAll(...)` に変更してください。その後、現行同様に layout attributes と表示中 supplementary の frame を検証してください。

## Minor

### 実機の一意識別子が証跡へ記録されている

**該当箇所**: [verification/README.md:10](kasane/changes/fix-dsl-header-height-diff/verification/README.md:10)

**問題点**: Android 実機のシリアルらしき値が、アーカイブ予定の証跡へ記載されています。検証環境の再現には機種と OS バージョンで十分であり、一意な端末識別子は不要な資産情報・個人識別情報になり得ます。

**推奨修正**: シリアルを削除し、`Pixel 6a / Android 16` のみ残してください。再現性に寄与しない Simulator UUID も同様に省略するのが安全です。

## Suggestion

なし。

## 総合判定

**CHANGES_REQUESTED**

件数: Critical 0 / Major 2 / Minor 1 / Suggestion 0。

## 突き合わせ結果 (ホスト側 review-001.md / verify-001.md との照合)

ホスト側 ksn-reviewer は APPROVED (Critical 0 / Major 0 / Minor 2 / Suggestion 4)、ksn-verifier は VALID。相方は CHANGES_REQUESTED で、判定が割れた。

| # | 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|---|
| 1 | Major: 可視性 preflight が headerHeight preflight より先に early return するため、両者 + 同一 ID Cell 内容変更が同時に起きると `.replaceCell` が欠落する | 相方のみ | **採用** | オーケストレーターが `DSLDiffCalculator.swift:62` / `:75` の順序をコードで確認し事実と確定。iOS spec:9 の SHALL は「headerHeight が変化している場合」に無条件でかかるため spec 違反。ホスト側レビューは可視性との併発ケースを検討しておらず、見逃しと判断。ホスト側 Minor-2 (同時変更 Scenario の自動テストが diff 列の形しか見ていない) が検出漏れの原因と整合する |
| 2 | Major: 「Store 経由」テストが `controller.applyDiff(...)` 直呼びで Store→Publisher→Controller 経路を通っていない | 相方 + ホスト verify (注記扱い) | **採用** | 事実関係は 3 者一致 (verify も同じ箇所を注記)。評価のみ割れたが、iOS spec:30-33 の Scenario は GIVEN「Store 接続で表示されている」WHEN「`replaceSection` する」と明記し、tasks.md:19 も「Store `replaceSection` および `.full` で」と要求している。spec の文言に忠実な相方判定を採る。ホスト側 ksn-reviewer の「対称テスト義務を満たす」は観測点 (実 frame) のみを見た評価で、経路の GIVEN/WHEN を見落としている |
| 3 | Minor: 実機シリアル / Simulator UUID が証跡 README に記録されている | 相方のみ | **採用** | 証跡はアーカイブされ長期に残る。機種と OS バージョンで再現性は足り、一意識別子は不要。修正コスト極小 |

確定 0 / 採用 3 / 降格 0 / 未解決 0。

ホスト側のみの指摘 (相方未検出) のうち修正サイクルに含めたもの: Minor-1 (Android preflight 条件の二重定義を `requiresFullRefresh` へ抽出)、Minor-2 (同時変更 Scenario の表示結果観測 — 上記 #1 と同根)、Suggestion-3 (`DSLDiffCalculator.kt` に残る lint 未検出のコメント債務)。Suggestion-1 (`contentUpdateDiffs` の内容比較重複) / Suggestion-2 (内部関数直叩きテストの包含) は構造の好みの域で実害がないため降格。
