# セカンドオピニオン: clarify-host-attach-order-contract (001)
**相方**: codex / **日付**: 2026-08-07 / **対象**: 提案一式 (proposal / specs / tasks、spec-review モード)
---
静的レビューのみ実施し、ビルド・テストは実行していません。

### Critical — Root Header / Footer は現在状態から復元できない

**該当箇所**: iOS Requirement「view load 時の Store 現在状態からの復元」、[proposal.md:16](kasane/changes/clarify-host-attach-order-contract/proposal.md:16)、[tasks.md:15](kasane/changes/clarify-host-attach-order-contract/tasks.md:15)

**問題点**: iOS Requirement は「accessory 更新」を種類によらず復元するとしていますが、Root Header / Footer は `SettingsRoot` に含まれません。Store はこれらを現在状態として保持せず、一過性 Diff を発行するだけです（[SettingsRootStore.swift:269](ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:269)）。したがって、提案された root/theme の pull では復元不能です。

さらに、タスク3.1で操作を attach 前へ移す共通 E2E シナリオは Root Header / Footer も更新します（[KsBridgeScenario.cs:91](maui/tests/shared/KsBridgeScenario.cs:91)）。現在の Non-Goals（Diff のイベント保全なし、Store 変更なし）と両立せず、仕様どおり実装してもE2E目標を満たせません。

**推奨修正**: 次のどちらかを明示的に選択してください。

- 保証対象を Store に保持される Section Header / Footer に限定し、Root Header / Footer を除外する。
- Root Header / Footer も順序非依存にするなら、復元可能な状態の所有先を設計し直し、Store変更をスコープへ含め、iOS・Android双方にScenarioを追加する。

### Major — attach/load 完了時点が定義されていない

**該当箇所**: Android Requirement「window attach 時の Store 現在状態からの復元」

**問題点**: Scenario は theme 変更も attach 時に反映されるとしますが、現行Androidは `onAttachedToWindow` で rootだけを同期し、Themeはその後に開始する `StateFlow.collect` へ委ねています（[KsSettingsView.kt:258](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:258)、[KsSettingsView.kt:326](android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:326)）。`submitList` も非同期です。

「`onAttachedToWindow` の終了時」「メインループ待機後」「最初の描画完了後」のどこでTHENを判定するか不明で、「Androidはコード変更なし」の妥当性をテストで一意に判断できません。

**推奨修正**: 収束の観測境界をScenarioへ追加してください。同期完了を要求するならAndroidも実装変更対象にし、eventual consistencyなら「メインループがアイドルになった後」などテスト可能な条件を定義してください。

### Major — iOSの公開 `applyTheme` との優先順位が未決定

**該当箇所**: iOS Requirement、[proposal.md:22](kasane/changes/clarify-host-attach-order-contract/proposal.md:22)

**問題点**: `KsSettingsViewController.applyTheme(_:)` は公開APIです（[KsSettingsViewController.swift:305](ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:305)）。Host生成後・view load前にこれを呼んだ場合、現行では指定Themeが残りますが、viewDidLoadでStore themeをpullすると上書きされます。ProposalのImpactにはこの公開挙動変更が記載されていません。

**推奨修正**: Store接続中はStore themeを常に正とし直接適用との併用を非保証にするのか、最後の直接適用を維持するのか決定してください。前者ならImpact・公開契約・回帰Scenarioへ明記してください。

### Major — Cell内容更新のScenarioとテストタスクが欠落

**該当箇所**: iOS Requirement、[tasks.md:7](kasane/changes/clarify-host-attach-order-contract/tasks.md:7)

**問題点**: RequirementはCell内容更新も保証対象としていますが、Scenarioは構造操作とaccessory/themeだけです。タスク1.3もCell内容更新を列挙していません。`replaceCell` と `replaceCells` は異なる通知経路であるため、構造操作テストでは保証できません。

**推奨修正**: view load前の `replaceCell` と `replaceCells` をそれぞれ扱うScenarioとテストタスクを追加してください。タスク1.3の「全Scenario」という表記も実際の対応範囲へ修正してください。

### Major — 変更級MがKasane基準と一致しない

**該当箇所**: [proposal.md:26](kasane/changes/clarify-host-attach-order-contract/proposal.md:26)

**問題点**: iOS/Androidの2 capabilityを横断し、共通Hostライフサイクル契約とADRを定め、MAUI E2Eにも影響します。Kasaneの「複数能力横断／アーキテクチャ変更はL」に該当します。未解決のRoot Accessory所有権やTheme優先順位も、design判断が必要であることを示しています。

**推奨修正**: L級へ再分類し、`design.md` で復元対象、状態所有者、同期完了境界、直接Theme適用との優先順位を決定してください。

### Minor — proposed ADRへのソースコメントを必須化している

**該当箇所**: [tasks.md:5](kasane/changes/clarify-host-attach-order-contract/tasks.md:5)、[ADR-0019:4](kasane/decisions/core/0019-host-restores-from-store-on-attach.md:4)

**問題点**: タスクは `core/ADR-0019` コメントを要求していますが、ADRはまだ `proposed` です。コメント規約が許容するADR参照は確定した設計判断への参照です。

**推奨修正**: 実装前にADRをacceptするか、タスクから参照必須を外して自己完結した理由コメントだけを要求してください。

### Minor — iOSでview loadとview階層attachを同一視している

**該当箇所**: iOS Scenario「view load 前の構造操作が load 時に反映される」

**問題点**: UIKitでは `loadViewIfNeeded()` や `.view` 参照だけでもview loadされ、windowへのattachとは別イベントです。「取り付ける（view load）」ではテストがどちらを検証すべきか曖昧です。

**推奨修正**: 契約トリガーを `viewDidLoad` とするなら、WHENを「Store更新後に `loadViewIfNeeded()` を呼ぶ」と明記し、window attachは別条件として扱ってください。

### Minor — Android specに非観察的かつ常に真でない実装記述がある

**該当箇所**: Android Requirement 7行目

**問題点**: 「attach前は個々のDiffイベントとして購読されない」は実装詳細です。`bind` 時点で `LifecycleOwner` を解決できればattach前でもcollect開始が可能で、常に真ではありません。契約上重要なのは通知受信方法ではなく最終状態への収束です。

**推奨修正**: 「通知を受信できたかにかかわらず、attach後にStore現在状態へ収束する」と観察可能な保証だけに書き換えてください。

### Minor — E2Eタスクの合否基準と証跡がない

**該当箇所**: [tasks.md:15](kasane/changes/clarify-host-attach-order-contract/tasks.md:15)

**問題点**: 「自然な順序で動作することを確認する」だけでは、何を表示確認するか、Theme・Root/Section Accessoryまで対象か、どの証跡を残すかが決まっていません。

**推奨修正**: 期待するSection/Cell列、各Accessory、Themeの観測点と、Simulator上での確認手順・証跡をタスクへ明記してください。

総合判定: CHANGES_REQUESTED

## 突き合わせ結果 (2026-08-07)

| # | 指摘 | 採否 | 対応 |
|---|---|---|---|
| 1 | Critical: Root H/F は Store pull で復元不能 | **採用** (SettingsRootStore.swift:269 のコメントで裏取り確定) | オーナー裁定で「復元保証から除外 + 所有者再適用責務」を採用 (design.md Decision 1)。両 spec・E2E タスクに反映 |
| 2 | Major: attach/load 完了時点が未定義 | **採用** | 収束の観測境界を定義 (iOS: viewDidLoad 完了時点 / Android: メインループ空時点)。design.md Decision 3 |
| 3 | Major: applyTheme との優先順位未決定 | **採用** | オーナー裁定で「Store 接続中は Store が正」(design.md Decision 2)。spec Scenario と proposal Impact に反映 |
| 4 | Major: Cell 内容更新の Scenario 欠落 | **採用** | replaceCell / replaceCells 両経路の Scenario とテストタスクを追加 |
| 5 | Major: 級が Kasane 基準の L | **採用** | オーナー裁定で L へ昇格、design.md (Decision 1〜4) を作成 |
| 6 | Minor: proposed ADR へのコメント必須化 | **採用** | ADR 参照コメントは accepted 後に付与とし、accepted 化を実装前ゲート化 |
| 7 | Minor: view load と attach の同一視 | **採用** | トリガーを viewDidLoad と定義し WHEN を loadViewIfNeeded に統一 (design.md Decision 4) |
| 8 | Minor: Android spec の実装記述 | **採用** | 観察可能な保証 (収束) だけに書き換え |
| 9 | Minor: E2E の合否基準なし | **採用** | 観測点・合否基準・スクリーンショット証跡をタスクに明記 |

採用 9 / 降格 0 / 未解決 0。CHANGES_REQUESTED の全指摘を反映済み。
