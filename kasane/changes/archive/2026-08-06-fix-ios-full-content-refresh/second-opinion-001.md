# セカンドオピニオン: fix-ios-full-content-refresh (001 回目)
**相方**: codex / **日付**: 2026-08-06 / **対象**: 提案一式 (proposal.md / specs/settings-view-ios-ui/spec.md / tasks.md / exploration.md)
---
# レビュー結果: fix-ios-full-content-refresh

**日付**: 2026-08-06  
**判定**: **NEEDS_DISCUSSION**

## サマリー

根本原因と `applyFullSnapshot` で内容差分を補う方向性は、コードおよび repro テストと整合しています。しかし、行 identity の保証範囲、既存 DSL との二重 reconfigure、対象選定の検証方法が未確定です。このまま実装すると仕様違反の判定ができないため、実装前に仕様判断が必要です。

指摘件数: Critical 0 / Major 5 / Minor 0 / Suggestion 0  
依頼どおりビルド・テストは実行していません。

## 指摘事項

### [🟠 Major] `reloadSections` と行 identity の保証が矛盾している

**該当箇所**: [spec.md:7](kasane/changes/fix-ios-full-content-refresh/specs/settings-view-ios-ui/spec.md:7)、[tasks.md:6](kasane/changes/fix-ios-full-content-refresh/tasks.md:6)、[KsSettingsViewController.swift:1173](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1173)、[display-state-synchronization.md:62](kasane/concepts/core/architecture/display-state-synchronization.md:62)

**問題点**: Requirement は内容変更された全同一 ID Cell について Native cell インスタンス維持を要求しています。一方、tasks は `reloadSections` 対象 Cell を reconfigure から除外します。既存仕様では Section reload により全 Cell が再構成され、first responder を失い得ることが明記されています。header/footer 変更と Cell 内容変更が同時に起きた場合、現在の Requirement を満たせません。

**推奨修正**: 次のいずれかを仕様で選択してください。

- identity 保証を「`reloadSections` の対象外かつ Renderer 型不変の場合」に限定し、reload 対象では Cell 交換を許容する。
- supplementary 更新方式を変更し、Section 内 Cell を reload しない設計へ拡張する。

併せて「header 変更 + 同一 ID Cell 内容変更」と「`.view` accessory を含む `replaceSection` + Cell 内容変更」の Scenario を追加し、表示内容・identity・first responder の期待値を明記してください。

### [🟠 Major] 同一 ID で Cell/Renderer 型が変わる場合の挙動が未定義

**該当箇所**: [spec.md:7](kasane/changes/fix-ios-full-content-refresh/specs/settings-view-ios-ui/spec.md:7)、[KsSettingsViewController.swift:787](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:787)、[KsCellRegistry.swift:54](ios/Sources/KsSettingsViewUI/KsCellRegistry.swift:54)

**問題点**: `Section.cells` は異種の `any KsCell` を格納でき、full replacement は同一 UUID のまま `LabelCell` から `SwitchCell` などへ型を変えることを禁止していません。Renderer は Cell の具象型から解決されるため、Renderer クラスが変われば同じ Native cell インスタンスは維持できません。それでも現 Requirement は旧・新 projection に同一 ID があれば一律に identity 維持を要求します。

**推奨修正**: 同一 ID で許される変更の前提を明文化してください。推奨は「Renderer/reuse type が同じ場合だけ内容更新として reconfigure し、型が変わる場合は remove + insert 相当の構造変更として Native cell 交換を許容する」です。型変更 Scenario も追加してください。

### [🟠 Major] 対象選定の主要契約を現在のテスト計画では検証できない

**該当箇所**: [spec.md:9](kasane/changes/fix-ios-full-content-refresh/specs/settings-view-ios-ui/spec.md:9)、[tasks.md:10](kasane/changes/fix-ios-full-content-refresh/tasks.md:10)

**問題点**: 「旧∩新」「値変更 Cell のみ」「新規・削除・hidden・reload Section は対象外」が変更の中心ですが、予定されているテストは最終表示と行数が中心です。実装が全 Cell を reconfigure しても、表示値と Native cell identity は同じままなので、多くのテストが通ります。初回対象空、変更なし Cell、hidden Section、reload Section との重複も直接検証されません。

**推奨修正**: 次のいずれかで reconfigure 対象 ID を直接検証してください。

- 対象選定を純粋 helper に分離し、返却 ID 集合をテストする。
- 独自 Registry/Renderer の render 回数を記録し、変更 Cell だけが追加 render されることを検証する。

少なくとも初回、完全同値、挿入、削除、Cell/Section の表示・非表示切替、移動＋内容変更、reload Section 除外を含め、誤って全件 reconfigure するミューテーションでテストが落ちることを確認すべきです。

### [🟠 Major] DSL 経路で同じ Cell が二重 reconfigure される

**該当箇所**: [proposal.md:20](kasane/changes/fix-ios-full-content-refresh/proposal.md:20)、[DSLDiffCalculator.swift:73](ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:73)、[KsSettingsViewController.swift:1577](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1577)

**問題点**: headerHeight preflight は現在 `.full` に続けて `.replaceCell` を発行します。今回 `.full` 自体が内容更新を内包すると、同じ Cell が連続して二度 reconfigure されます。proposal はこれを「無害」としていますが、利用者定義 Renderer や `CustomCell` builder の二重実行、編集中 Cell の状態への影響は検証されていません。既存テストも最終タイトルしか観測しないため、二重実行を検出できません。

**推奨修正**: `.full` が内容更新を内包した後は `contentUpdateDiffs` の続発を止めるのが自然です。DSL 変更を Non-Goal に維持するなら、二重 render を許容する契約とその理由を明記し、Renderer 呼び出し回数・Native cell identity・編集中状態を検証する Scenario を追加してください。

### [🟠 Major] 全件検証コマンドでは UIKit テストの実行を保証できない

**該当箇所**: [tasks.md:18](kasane/changes/fix-ios-full-content-refresh/tasks.md:18)、[test-execution.md:18](kasane/concepts/cross/conventions/test-execution.md:18)

**問題点**: tasks の `xcodebuild test -scheme KsSettingsView-Package` には `ios/` への移動と iOS Simulator destination がありません。リポジトリルートでは失敗し、destination 次第では UIKit テストが対象外になるため、「全テスト成功」が空振りし得ます。また実行件数確認もタスク化されていません。

**推奨修正**: 検証タスクを次の契約にしてください。

```sh
cd ios
xcodebuild test \
  -scheme KsSettingsView-Package \
  -destination 'platform=iOS Simulator,name=<利用可能な機種名>'
```

終了コードだけでなく、末尾の `Executed N tests, with M failures` を記録・確認することも受け入れ条件へ含めてください。

## アクションプラン

1. `reloadSections` と Renderer 型変更時の identity 保証範囲を決定する。
2. DSL の `.full + .replaceCell` 二重適用を廃止するか、許容契約として検証する。
3. exact target selection を観測可能にし、境界 Scenario を追加する。
4. Simulator 全件実行と実行件数確認へ検証タスクを修正する。

## 突き合わせ結果 (2026-08-06)

ホスト側自己レビュー (2周・指摘なし) との突き合わせ。全件が相方のみの指摘のため、根拠の強さで採否を判定した。

| 指摘 | 採否 | 根拠 |
|---|---|---|
| M1: reloadSections と行 identity 保証の矛盾 | **採用** | concepts に「Section reload は全 Cell 再構成 (first responder 喪失)」が明記されており、header 変更 + 内容変更の同時発生で Requirement が満たせないのは実在の仕様穴。spec の identity 保証を「supplementary 再構成対象外の Section かつ具象型同一」に限定し、同時変更 Scenario を追加 |
| M2: 同一 ID で具象型が変わる場合が未定義 | **採用** | `KsCellID` は UUID のみで等価判定とコード裏取り済み — 型変更は同一 item と扱われ、reconfigure では Native cell を維持できない。spec に「具象型が変わる場合は cell 交換を許容し内容は最新で表示」を明文化し Scenario 追加 |
| M3: 対象選定契約がテストで検証できない | **採用** | 「全件 reconfigure しても最終表示テストは通る」の指摘は正しい。対象選定を純粋 helper に分離し、返却 ID 集合の境界ケース単体テストを tasks に追加 |
| M4: DSL 経路の二重 reconfigure | **採用 (ユーザー裁定 2026-08-06)** | 指摘は事実 (headerHeight preflight の `.full` + `.replaceCell` 続発と本修正で同一 Cell が2回 reconfigure される)。ユーザー裁定: 二重発火は許容しない。続発廃止を本 change に含める。実装済み fix-dsl-header-height-diff 側の spec 改訂は不要 (実装完了後の spec は足場として役目を終えている)。spec に MODIFIED Requirement「SwiftUI DSL の headerHeight 変更の表示反映」を追加し、tasks 1.3 / 2.9 で実装・テスト更新を規定 |
| M5: 全件検証コマンドが UIKit テスト実行を保証しない | **採用** | `cd ios` + Simulator destination が必要なのは事実 (過去 change の検証記録とも一致)。tasks 3.1 を修正し「Executed N tests」確認を受け入れ条件へ追加 |

採用 5 / 降格 0 / 未解決 0 (M4 はユーザー裁定で採用に確定)
