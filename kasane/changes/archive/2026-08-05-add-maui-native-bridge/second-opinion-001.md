# セカンドオピニオン: add-maui-native-bridge (001 回目)
**相方**: codex / **日付**: 2026-08-04 / **対象**: 提案一式 (proposal / design / specs/ios-store / specs/maui-bridge / tasks) — spec-review モード

---

# レビュー結果: add-maui-native-bridge

**日付**: 2026-08-04  
**判定**: **NEEDS_DISCUSSION**  
**指摘件数**: Critical 1 / Major 8 / Minor 3 / Suggestion 0

## サマリー

Bridge の基本方針は accepted ADR と概ね整合していますが、中心経路である `C# → Bridge → Store → Native Host` の Host 接続方法が仕様から欠落しており、現状のままでは表示 Scenario を一意に実装・検証できません。

加えて、公開 API の型、ID 変換、Theme / Accessory の輸送表現、`replaceCells` の可視性変更、永続的な実行時検証が未確定です。実装上の判断だけでは解消できないため、実装開始前の仕様修正・設計合意が必要です。

## 指摘事項

### [🔴 Critical] 内部 Store と Native Host を接続する公開経路が存在しない

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:5`、`kasane/changes/add-maui-native-bridge/design.md:13`、`kasane/changes/add-maui-native-bridge/tasks.md:17`

**問題点**: Bridge が Store を内部所有し、Host がその Store を購読するとされていますが、Bridge から Host を生成・取得・接続する API が定義されていません。現行 iOS Host は `KsSettingsViewController(store:)` を要求し（`ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:137`）、Android Host は `bind(store)` を要求します（`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:298`）。Store を Bridge 内部に隠したままでは、外部コードから既存 Host へ接続できません。

このため「`setRoot` 後に Native list が表示される」という中心 Scenario は、Host の所有者・生成者・View 階層への取り付け方を実装者が創作しない限り成立しません。

**推奨修正**: 次のいずれかを仕様として確定してください。

- Bridge が Store 接続済みの `KsSettingsViewController` / `KsSettingsView` を生成・所有し、Native Host handle を公開する。
- Native 側 factory が Bridge と Host を同時生成し、内部 Store を接続する。
- Host が Bridge 自体を受け取って内部 Store に接続する。

あわせて、C# が保持・破棄すべきオブジェクト、Host と Bridge の寿命関係、`setRoot` が Host 接続の前後どちらでも成立するかを Scenario 化してください。

### [🟠 Major] Builder と公開 API のシグネチャが定義されていない

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:5`、`kasane/changes/add-maui-native-bridge/proposal.md:12`、`kasane/changes/add-maui-native-bridge/tasks.md:17`

**問題点**: 「Builder」「`addLabelCell`」「12メソッド」は列挙されていますが、Section を生成・追加する操作、Root の完成条件、DTO のフィールド、null/default、戻り値、失敗方法がありません。`addLabelCell` だけでは「Section 1個と LabelCell 複数個」の GIVEN を構築できません。

また、現行 ID は iOS が `UUID`、Android が `String` であり（`kasane/concepts/core/core-model/settings-tree.md:25`）、既存契約も Bridge に明示的変換を要求しています（`kasane/concepts/core/core-model/structural-changes.md:42`）。共通 C# ID の形式、無効な UUID 文字列、重複 ID、自動採番の扱いが未定です。

**推奨修正**: iOS / Android Native API と生成される C# APIについて、クラス・namespace・全メソッドのシグネチャ表を追加してください。最低限、以下を規定する必要があります。

- Bridge / RootBuilder / SectionBuilder / LabelCell DTO の所有・生成関係
- Section の作成・追加・終了方法
- Section / Cell ID の共通形式と変換規則
- LabelCell の公開フィールドと既定値
- null、無効 ID、重複 ID、破棄済み Builder のエラー契約
- Android の `@JvmStatic` が factory のみか、Store 操作にも適用されるか

### [🟠 Major] Theme の公開型が proposal・design・ADR 間で矛盾している

**該当箇所**: `kasane/changes/add-maui-native-bridge/proposal.md:12`、`kasane/changes/add-maui-native-bridge/design.md:39`、`kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:38`、`kasane/decisions/maui/0004-maui-idiomatic-types-for-styling.md:16`

**問題点**: proposal は MAUI 慣例型を interop 境界で受けるように読めますが、design は Bridge が primitive DTO を受け、MAUI 型の公開は phase-2 としています。一方、spec は Binding 経由で `setTheme` を含む全 Bridge API を C# から呼べることを要求しています。

別の MAUI facade が存在しない phase-1 では、生成された C# Binding の primitive DTO が事実上の公開 API になります。これは「MAUI 公開 API は `Microsoft.Maui.Graphics` 等、interop DTO は非公開」という ADR-0004 と両立する保証がありません。また ADR-0004 が spec の責務とする項目対応表も存在しません。

**推奨修正**: 次のどちらかを明示してください。

- phase-1 Binding は内部輸送 assembly とし、公開可視性を metadata 等で制限する。MAUI 型の `setTheme` は phase-2 まで公開しない。
- phase-1 に最小 MAUI facade を含め、MAUI 型から非公開 DTO への変換を実装する。

さらに全 Theme フィールドについて、色、font、寸法、nullable、platform 固有値、既定値、同値判定の対応表と変換 Scenario を追加してください。

### [🟠 Major] `updateAccessory` の interop 表現がなく、12操作を公開できない

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:19`、`kasane/changes/add-maui-native-bridge/design.md:22`

**問題点**: `updateAccessory` は必須の Store 1:1 API に含まれますが、現行 `SettingsAccessory` / `AccessoryTarget` は associated value を持つ union 型です。さらに Accessory は任意 SwiftUI/UIKit/Compose/Android View を内包できます。これらをそのまま `@objc` / JVM Binding 境界へ公開することはできません。

SettingsRootDiff の union DTO を避けた設計と同じ問題が、`updateAccessory` と Section Builder 内の header/footer に残っています。

**推奨修正**: phase-1 で扱う Accessory の範囲を確定してください。例えば text/null のみを扱う専用 transport DTO または位置別メソッドを規定し、任意 View は Non-Goal に入れます。`updateAccessory` 自体を後続へ送る場合は accepted ADR-0002との扱いを再合意してください。

### [🟠 Major] `replaceCells` の「構造変更なし」は `isVisible` を持つ LabelCell と両立しない

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/ios-store/spec.md:7`、`kasane/changes/add-maui-native-bridge/specs/ios-store/spec.md:24`、`kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:33`

**問題点**: spec は `replaceCells` を構造変更なしの内容更新としますが、LabelCell 自体が `isVisible` を持ちます。現行 Android `replaceCells` は新しい Cell オブジェクトを無条件に Store へ格納し（`SettingsRootStore.kt:194`）、Host は更新後 root を再 flatten します（`KsSettingsView.kt:383`）。したがって `isVisible` が変われば表示構造も変わり得ます。

単体の `replaceCell` では iOS / Android とも可視性変更を検出して full 更新へフォールバックしていますが、バッチ時の契約が決まっていません。

**推奨修正**: 次のどちらかを Requirement に追加してください。

- `replaceCells` の事前条件を「対象 ID、新 Cell の ID、`isVisible` が同一」に限定し、Bridge/Store が違反を拒否する。
- 可視性変更を含む場合は、状態を1回更新した後に visible projection を1回再構築する構造更新へフォールバックする。

visible→hidden、hidden→visible、可視・不可視混在バッチの Scenario が必要です。また、Host が新しい root を読めるよう「状態 commit 後にバッチ通知する」順序も明記してください。

### [🟠 Major] 「未知 ID は no-op」が現行 Store の `updateAccessory` と衝突する

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:21`

**問題点**: Requirement は列挙した Store 操作全体について未知 ID を no-op としています。しかし現行 iOS / Android の `updateAccessory` は Section が見つからなくても helper から戻った後に Diff を発行します（`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:223`、`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:243`）。Bridge が単純転送すると spec 違反になります。

**推奨修正**: no-op 契約が適用される操作を個別に列挙してください。`updateAccessory` も no-op に統一するなら、iOS/Android Store 両方の変更と回帰テストを tasks に追加します。現行挙動を維持するなら、その通知挙動を明記してください。

### [🟠 Major] 10更新 API の大半に Scenario がなく、変換誤りを検出できない

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:19`、`kasane/changes/add-maui-native-bridge/tasks.md:19`

**問題点**: Requirement は10更新 APIを要求しますが、Scenario は `insertCell` / `removeCell`、`replaceCell`、`replaceCells` しか直接扱っていません。`insertSection`、`removeSection`、`moveSection`、`replaceSection`、`moveCell`、`updateAccessory` の変換が欠落・誤配線しても、現行 Scenario と task 3.3 / 4.3 を満たせます。

**推奨修正**: 全12 Store 操作について、Native API→内部 Store 操作の対応表を規範化し、少なくとも各操作の正常系を parameterized test で検証する task を追加してください。ID 不在、index clamp、identity 維持など、操作ごとの異常・境界契約も表に含めてください。

### [🟠 Major] C# 実行時疎通の検証器が削除され、受け入れ結果を再現できない

**該当箇所**: `kasane/changes/add-maui-native-bridge/design.md:61`、`kasane/changes/add-maui-native-bridge/specs/maui-bridge/spec.md:61`、`kasane/changes/add-maui-native-bridge/tasks.md:32`

**問題点**: C#→Native の実行時疎通はこの変更の中心的な受け入れ基準ですが、task は検証用ホストを成果物に残さないとしています。これでは後続レビュー、CI、SDK更新時に Scenario を再実行できず、Binding がコンパイルするだけの回帰を検出できません。

**推奨修正**: Sample ではなくテスト資産として、最小の iOS / Android Binding integration host を `maui/tests` 等に残してください。C# から root を構築し、Host 接続後の Native model/list に LabelCell が存在するところまで自動判定できる形が望まれます。

### [🟠 Major] interaction delegate の phase-1 スコープが矛盾している

**該当箇所**: `kasane/changes/add-maui-native-bridge/proposal.md:12`、`kasane/changes/add-maui-native-bridge/design.md:31`

**問題点**: proposal は単一 interaction delegate/listener を今回の公開 API に含めていますが、design は LabelCell に対話がないため phase-4 へ送ると明記し、spec/tasks も delegate を要求していません。公開表面と実装スコープのどちらが正か判定できません。

**推奨修正**: design の方針を採るなら、proposal の What Changes から phase-1 実装対象であるかのような記述を削除し、「方式のみ ADR-0003 で確定、実装は phase-4」と統一してください。今回実装するなら登録・解除・寿命管理の Requirement / Scenario / tasks を追加してください。

### [🟡 Minor] `replaceCells` の混在・重複入力の結果が未定義

**該当箇所**: `kasane/changes/add-maui-native-bridge/specs/ios-store/spec.md:14`

**問題点**: 未知 ID のみの場合は定義されていますが、既知・未知 ID の混在、同一 ID の複数指定、`cellID` と新 Cell の ID 不一致が未定義です。現行 Android は入力順で適用し、重複 ID も重複したまま通知し、最後の値を状態に残します。

**推奨修正**: 少なくとも次を Scenario 化してください。

- 既知・未知の混在では既知だけを適用・通知する。
- 同一 ID の重複は最後勝ちかエラーか。
- 通知 ID は入力順か model 順か、重複排除するか。
- 対象 ID と新 Cell ID の一致を事前条件にするか、Bridge が検証するか。

### [🟡 Minor] toolchain spike の成功条件と失敗時の状態が曖昧

**該当箇所**: `kasane/changes/add-maui-native-bridge/design.md:52`、`kasane/changes/add-maui-native-bridge/tasks.md:3`

**問題点**: 「最小スケルトンをビルドし疎通確認」「問題があれば差し戻す」では、何をもって成功とするか判定できません。Native project の単体ビルドだけなのか、Binding assembly 生成、C# compile/link、Simulator/Emulator 起動まで含むのかが不明です。

**推奨修正**: 対象 workload / framework、Debug/Release、対象 architecture、Native artifact生成、C# compile/link の各ゲートを列挙してください。失敗時は後続 task を実行せず change を blocked とすることも明記してください。

### [🟡 Minor] モジュール境界を定める Decision 5 が ADR 候補から除外されている

**該当箇所**: `kasane/changes/add-maui-native-bridge/design.md:46`、`kasane/changes/add-maui-native-bridge/design.md:72`

**問題点**: Native Bridge を既存 UI module から分離し、iOS / Android / MAUI の各 build root に新しい公開成果物境界を置く判断は、コンポーネント境界を越え、将来の依存・配布・公開識別子を制約します。「局所的な構成判断」とするには影響が大きく、Kasane の ADR 選別基準に該当する可能性があります。

**推奨修正**: Decision 5 を ADR 候補として再評価してください。候補外とする場合も、なぜ既存 ADR だけで十分に拘束されているかを `ADR 候補` 節へ記録してください。

## アクションプラン

1. Host の生成・接続・所有・破棄モデルを確定する。
2. Bridge / Builder / DTO / ID の完全な公開 API 表を追加する。
3. Theme と Accessory の公開層・輸送層を ADR-0002/0004と整合させる。
4. `replaceCells` の可視性、identity、混在・重複入力契約を確定する。
5. 全 Store 操作の変換 Scenario と永続的な C# runtime integration test を追加する。
6. proposal の delegate スコープ、spike 成功条件、Decision 5 の ADR 扱いを整理する。

依頼どおり静的レビューのみ実施し、ビルド・テストおよびファイル書き込みは行っていません。

---

## 突き合わせ結果 (ホスト側判定: 2026-08-04)

ホスト側自己レビュー (2周・指摘1件: capability 名不整合→修正済み) との突き合わせ。以下は全て「相方のみ」の指摘であり、根拠の強さで採否判定した。主要根拠 (Android `bind(store)`・iOS `updateAccessory` の無条件 Diff 発行・`SettingsAccessory` の union 型) はコードで実在を検証済み。

| # | 指摘 | 採否 | 対応方針 |
|---|---|---|---|
| 1 | [Critical] Host 接続経路の欠落 | **採用** | Bridge が内部 Store 接続済みの Native Host (view controller / view) を生成・公開する Requirement/Scenario を追加 |
| 2 | [Major] Builder/API シグネチャ未定義 | **部分採用** | ID 契約 (C# は String、iOS 側 UUID 変換・無効/重複時挙動) と Section 構築 API を spec に追加。完全シグネチャ表は降格 (デルタスペックは挙動契約であり実装方法を書かない規約のため) |
| 3 | [Major] Theme 公開型の矛盾 | **採用** | proposal の文言修正 + design に「phase-1 binding は輸送層 (MAUI 慣例型の公開 facade は phase-2)」を明記。項目対応表は phase-2 スコープと明記 |
| 4 | [Major] updateAccessory の union 表現 | **採用** | phase-1 の accessory 輸送は text ベースに限定し、任意 View accessory を Non-Goals に追加 |
| 5 | [Major] replaceCells と isVisible の両立 | **採用** | 「可視性変更は replaceCells の対象外 (可視性は full 更新経路)」を Requirement 化 — display-state-synchronization の既存契約と整合 |
| 6 | [Major] 未知 ID no-op と updateAccessory の衝突 | **採用** (コード検証済み) | no-op 契約の適用範囲を Cell/Section 操作に限定し、updateAccessory は現行 Store 挙動に従うと明記 |
| 7 | [Major] 10更新 API の Scenario 不足 | **採用** | 全12操作の parameterized test タスクと Section/move 系 Scenario を追加 |
| 8 | [Major] C# 実行時疎通の検証器が使い捨て | **採用** | 使い捨てをやめ、maui/ 配下のテスト資産として維持する形に tasks を修正 |
| 9 | [Major] delegate スコープの矛盾 | **採用** | proposal から phase-1 実装対象と読める記述を削除し「方式のみ ADR-0003 で確定、実装は phase-4」に統一 |
| 10 | [Minor] replaceCells 混在・重複入力 | **採用** | Android 現行挙動 (入力順適用・既知のみ適用/通知) を契約化する Scenario を追加 |
| 11 | [Minor] spike 成功条件の曖昧さ | **採用** | 成功ゲート (Native artifact 生成 / binding assembly 生成 / C# compile+link) を tasks に列挙、失敗時は blocked |
| 12 | [Minor] Decision 5 の ADR 候補除外 | **部分採用** | ADR 候補節に「cross/ADR-0001 (ビルドルート)・cross/ADR-0002 (公開識別子) で既に拘束済み」の理由を記録。ADR 起票自体は降格 |

- 確定 (双方一致): 0 / 採用: 10 / 部分採用: 2 / 降格: 0 (部分降格2件は上記に含む) / 未解決: 0
- 判定: 相方の NEEDS_DISCUSSION を受け、Critical #1 の解消方針 (Host 生成・公開 API の形) を含む修正方針をオーナーに提示して承認後に反映する
