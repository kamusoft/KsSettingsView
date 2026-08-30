# レビュー結果: harden-update-accessory-unknown-id (002 回目)

**日付**: 2026-08-09
**判定**: APPROVED

## サマリー

review-001 の Major 1 件・Minor 2 件・Suggestion 1 件 (Suggestion 2 は別変更へ切り出し合意済み) はいずれも解消を確認した。とくに Minor 1 (Android 操作契約表の検出力不足) は、ガードを一時的に反転させるミューテーションで**追加したアサーションが実際に落ちる**ことを実測しており、指摘の趣旨どおり回帰検出力が入っている。修正はテストとコメントのみで、Store 実装・Bridge 実装・足場アーティファクトに変更はなく、デルタスペックの Scenario 対応 (verify-001) を壊す変更もない。

新規の指摘は Suggestion 1 件のみ (非ブロッキング) のため APPROVED とする。

## 前回指摘の解消状況

### [🟠 Major → 解消] MAUI 層3箇所のコメント

- `maui/KsSettingsView.Maui/Internals/IKsSettingsGateway.cs:69-71` — 「未知の Section ID は Store 側で no-op になり、state 更新も更新通知も発生しない (core/ADR-0020)。Root 対象は ID を参照せず、従来どおり通知される」。`specs/maui-bridge/spec.md` の MODIFIED Requirement 本文 (section 系 target の未知 ID no-op / Root 系は対象外) と一致する
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:521-522` — 新契約を述べたうえで「除去済み Section の通知は MAUI 層で先に遮断する」とガードが残る理由を書いており、`HandleSectionPropertyChanged` の early return と整合する
- `maui/KsSettingsView.Maui.Tests/RemovedElementNotificationTests.cs:11-12` — 同様に、テストが守っている契約 (MAUI 層での先行遮断) を現在形で述べている

3箇所とも「旧仕様の否定」ではなく現在の契約の記述になっており、コメント規約 (許容参照は `<domain>/ADR-NNNN`、履歴記述禁止、delta spec キーワード禁止) に適合する。リポジトリ全体を `契約の対象外` / `素通し` で再走査したが、旧契約を述べる記述の残りはコード側に存在しない。

### [🟡 Minor → 解消] Android 操作契約表の検出力

`KsBridgeOperationContractTest.assertDiffDeliveryAlive` (`android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeOperationContractTest.kt:406-421`) を全ケース共通の後段検証として追加し、操作後に先頭 Section を挿入して表示へ届くことを見ている。Store が未知 ID の Diff を発行すると `reportMissingId` → strictMode の `IllegalStateException` が Diff 購読コルーチンごと停止させるため、この後続操作が表示へ届かなくなる。

**ミューテーションによる実測** (`lessons/code-review.md` L-001):

`android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` の `updateSectionAccessory` で `if (index < 0) return false` → `return true` (state 更新なしのまま Diff だけ発行される旧挙動と等価) に変えて `KsBridgeOperationContractTest` のみ実行:

```
KsBridgeOperationContractTest > 全 12 操作が契約どおりに反映される FAILED
java.lang.AssertionError: 後続操作が表示へ届く（Host の Diff 購読が生きている）:
  updateAccessory: 未使用 sectionID の Section header は no-op
  expected:<[PROBE-S, PROBE-C, S1, A, B, S2, C]> but was:<[S1, A, B, S2, C]>
```

前回「表から落とすか観測点を足すか」と示した選択肢のうち後者が取られ、落ちるのは狙いどおり新設のアサーションだった。検査後、`SettingsRootStore.kt` は backup と shasum 一致 (`de847c8e...`) で原状復帰済み、全件再実行で緑を確認している。

観測手段は iOS (`diffCount` を直接見る) と Android (Diff 配信の生存を後続操作で見る) で異なるが、どちらも Diff 無発行を検出できる。Android 側は「検証は内部状態ではなく実描画と Adapter 通知で行う」という同ハーネスの設計方針を保っており、core/ADR-0018 の求める観察可能挙動の対称性は満たされている。

### [🟡 Minor → 解消] `@discardableResult`

`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift` の `updateSectionAccessory` (`:334`) に `@discardableResult` は付いていない。同ファイル `:321` の `@discardableResult` は既存の `mutateCellList` のものであり、本変更とは無関係。呼び出し元 2 箇所は `guard` で戻り値を使っており、ビルドも通っている。

### [🔵 Suggestion → 解消] 両 OS Bridge の doc

`ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:222-223` と `android/.../bridge/KsSettingsBridge.kt:266-267` に、canonical UUID でも未知 sectionID なら Store 側で no-op になる旨が対称に 1 文ずつ追記されている。文面 (「状態・表示・通知は変化しない」) は `specs/maui-bridge/spec.md` の Requirement 本文と一致し、Bridge 実装は無変更のまま (proposal の Non-Goals を維持)。

### [🔵 Suggestion → 対象外] Robolectric 待機ヘルパの重複

別変更として起票済みという合意に従い、本変更では扱わない。

## 新規の指摘事項

### [🔵 Suggestion] 新設 probe の検出力は `KsCellRegistry.strictMode` の既定値に依存する

**該当箇所**: `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeOperationContractTest.kt:406-421`

**問題点**: `assertDiffDeliveryAlive` が未知 ID の Diff 発行を捕まえられるのは、`KsCellRegistry.strictMode` が既定 `true` で `reportMissingId` が `error()` を投げ、Diff 購読が停止するからである (`KsCellRegistry.kt:87` / `KsSettingsView.kt:870-876`)。`strictMode` は公開の `var` であり、既定が将来 `false` に変わる、あるいは同一 Robolectric sandbox 内の別テストが復元し忘れて `false` を残すと、この 2 ケースは**失敗ではなく黙って空振りに戻る** (今回の実測時点では `strictMode = false` にする既存テストは別モジュール `ks-settingsview-ui` 側の 2 ファイルのみで、いずれも after で復元しているため実害はない)。

probe の KDoc が strictMode への依存を明記しているため発見可能性はあり、Scenario 自体は `KsBridgeUpdateTest` と Store テストが独立に担保している。したがって非ブロッキング。

**推奨修正**: 強化するなら probe の冒頭に前提アサーション 1 行 (`assertTrue("strictMode 既定を前提とする", KsCellRegistry.strictMode)`) を足すと、前提が崩れた瞬間に空振りではなく失敗として顕在化する。本変更で対応しなくてもよい。

## 確認した観点 (問題なし)

- **ビルド・テスト (レビュー側で実行)**:
  - Android `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL / 2024 tests / 0 failures (test-results XML 集計)
  - iOS `xcodebuild test -scheme KsSettingsView-Package` (iPhone 17 Pro) → TEST SUCCEEDED / Bridge 36 + Core 83 + SwiftUI 76 + UI 451 = 646 tests / 0 failures
  - MAUI `dotnet build` → 0 エラー (NU1608 警告は本変更と無関係の既存パッケージ制約)、`dotnet test KsSettingsView.Maui.Tests` → 115 tests / 0 failures。今回 MAUI 側の変更はコメントのみだが、XML doc の構造を壊していないことを含めて実行で確認した
- **足場の不変性**: `specs/` `proposal.md` `tasks.md` に修正サイクル中の書き換えなし。`kasane/` 側の差分は `exploration.md` と `decisions/core/index.md` (ADR-0020 の行追加) のみで、いずれも提案・決定の記録として妥当
- **実装コードの無変更**: 今回のサイクルで両 OS の `SettingsRootStore` のロジック、Bridge 実装 (`ios/Sources/KsSettingsViewBridge` / `android/ks-settingsview-bridge/src/main`) のコード、MAUI の `KsSettingsController` ロジックはいずれも変わっていない (差分はコメントと doc のみ)
- **Scenario 対応の維持**: `specs/maui-bridge/spec.md` の「全12操作が契約どおりに反映される」は THEN で「観察可能な結果 (Host の表示内容と通知)」を求めており、今回の追加は観測点の**上乗せ**であって要求の置き換えではない。「未知 sectionID の updateAccessory は no-op」Scenario の対応先も変わっていないため、verify-001 の対応表は有効
- **コメント規約**: `python3 scripts/comment-policy-lint.py` → 禁止 0 件 (検査対象 466 ファイル)。新規コメントの外部参照は `core/ADR-0020` のみで許容形式、delta spec キーワード・変更提案 ID・履歴記述の混入なし
- **テストハーネスの副作用**: probe は `KsBridgeAdapterRecorder.detach` の後に実行されるため既存の通知件数アサーションに影響しない。fixture はケースごとに再生成され、probe 失敗時も `attachment` は非 null のまま残るので `tearDown` で Activity が閉じられる (リーク経路なし)。実行時間の増加も全件 1m34s の範囲に収まっている
- **Kotlin / Swift のコード品質**: 追加された Kotlin は `private companion object` の `const val` と private ヘルパのみで可変状態を持たず、null 安全・コルーチン観点の新規リスクなし (android は ktlint / detekt 未導入のため lint 追加検査は不要)。Swift 側は doc 追記と `@discardableResult` 削除のみ

## アクションプラン

1. (任意) 新設 probe に `strictMode` 前提のアサーションを 1 行足す — 本変更で対応しなくてよい
2. Robolectric 待機ヘルパの共通化は起票済みの別変更で扱う
3. 蒸留時に proposal の Impact に挙がっている concepts 3 件 (`core/core-model/structural-changes.md` / `maui/api/native-bridge.md` / `core/architecture/store-and-update-streams.md`) の追随を行う
