# レビュー結果: harden-update-accessory-unknown-id (001 回目)

**日付**: 2026-08-09
**判定**: CHANGES_REQUESTED

## サマリー

Store 側のガード追加そのものは両 OS とも `moveCell` / Cell / Section 操作の既存パターンに正しく揃っており、宣言 UI 経路 (両 OS の `DSLDiffCalculator`) を含めて取りこぼしになる呼び出し経路は見当たらない。テストも Store / Host / Bridge の3層で両 OS 対称に追加されており、ミューテーション検査でガードを無効化すると 4 件が実際に落ちることを実測した (回帰検出力あり)。

一方で、本変更が確立した契約と**正反対の記述**が MAUI 層の C# コメント3箇所に残っている。`maui-bridge` は本変更の capability の1つであり、そのデルタスペック (MODIFIED) が明示的に書き換えた文言の裏返しがコードに残るのは看過できないため CHANGES_REQUESTED とする。

## 指摘事項

### [🟠 Major] MAUI 層の3箇所のコメントが新契約と正反対のまま残っている

**該当箇所**:
- `maui/KsSettingsView.Maui/Internals/IKsSettingsGateway.cs:68-72`
- `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:521-522`
- `maui/KsSettingsView.Maui.Tests/RemovedElementNotificationTests.cs:10-13`

**問題点**:
`IKsSettingsGateway.UpdateAccessory` の XML doc は「この操作だけは未知 ID の no-op 契約の対象外で、削除済み Section の ID を渡すとそのまま Store の更新通知になる。呼び出し側が ID の生存を確認してから呼ぶ」と述べている。他の2箇所も同じ理由付けを繰り返している。

これは core/ADR-0020 の適用後は事実として誤りであり、`specs/maui-bridge/spec.md` の MODIFIED Requirement 本文 (「Cell / Section 操作および `updateAccessory` の section 系 target における未知 ID の no-op (core/ADR-0020) がそのまま適用される」) と真逆の記述になる。gateway インターフェースの doc は MAUI 側実装者にとっての契約記述であり、「呼び出し側の事前確認が安全上必須」という誤った前提で将来の設計判断が行われうる。

なお、`KsSettingsController.HandleSectionPropertyChanged` の対応表ガードと購読解除、および `RemovedElementNotificationTests` のアサーション自体は引き続き妥当 (ID を得るために対応表参照は必要であり、gateway 呼び出しを一切発行しないという性質も変わらない)。**壊れているのは理由付けの記述だけ**であり、実装やテストの構造を変える必要はない。

ソースコメント規約は適用契機に「コードレビューのとき」を含み、「現在の仕様を現在形で書く」ことを求めている。本変更が生んだ乖離であるため、本変更の中で解消するのが筋。

**推奨修正**: 3箇所を現行契約に沿って書き換える (例: 「未知 ID の `updateAccessory` は Store で no-op になる (core/ADR-0020)。ここで対応表を参照するのは Section の ID を得るためであり、除去済み Section へは通知自体を発行しない」)。`core/ADR-0020` の参照はコメント規約の許容形式。

### [🟡 Minor] Android の操作契約表に足した2ケースは本変更の回帰を検出できない (実測)

**該当箇所**: `android/ks-settingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeOperationContractTest.kt:268-291`

**問題点**:
Android の `OperationCase` は観測点が「実描画された行テキスト」と `KsBridgeAdapterRecorder` の Adapter 通知件数 (`structuralCount` / `contentChangeCount`) のみで、Store の `diffs` Flow を見ていない。未知 sectionId の Diff が emit された場合、Host の `reportMissingId` が strictMode で投げる `IllegalStateException` は Diff 購読コルーチンの内側で握られて購読ごと停止するだけなので、行テキストも Adapter 通知件数も**変化しないまま**アサーションが通る。

実測 (`SettingsRootStore.kt` のガードを一時的に無効化して android 全件実行): 落ちたのは以下の 4 件で、`KsBridgeOperationContractTest` は**通過した**。

- `UnknownSectionAccessoryHostTest :: strictMode 既定のまま未知 sectionId の updateAccessory を呼んでも Host は沈黙しない`
- `SettingsRootStoreTest :: updateAccessory_未知sectionIdのSectionHeaderはstate変更もDiff発行もされない`
- `SettingsRootStoreTest :: updateAccessory_未知sectionIdのSectionFooterはstate変更もDiff発行もされない`
- `KsBridgeUpdateTest :: updateAccessory の未使用 sectionID は no-op になる`

(検査後、`SettingsRootStore.kt` は backup と shasum 一致で原状復帰済み。)

対する iOS の同名2ケースは `store.diffPublisher` の件数を直接見ているため検出力がある。core/ADR-0018 (両 OS 対称テスト) の観点でも、同じラベルのケースが片方だけ空振りする状態は残したくない。

なお、Scenario 自体は `KsBridgeUpdateTest` 側が検出力を持って担保しているため、デルタスペックの充足には影響しない。

**推奨修正**: Android の `OperationCase` 検証ループに Store の `diffs` 件数の観測を足して iOS 側と観測点を揃える。ハーネス改修を本変更に含めたくない場合は、この2ケースを表から落として `KsBridgeUpdateTest` 側に一本化する (「表にあるのに落ちない」状態を残さない) のでも可。

### [🟡 Minor] `@discardableResult` が契約を型で守る機会を潰している

**該当箇所**: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:334`

**問題点**:
`updateSectionAccessory` の戻り値は本変更で「state 更新が成立したか」という契約上の意味を持つようになり、現在の呼び出し元 2 箇所 (`:282` / `:287`) はいずれも `guard` で結果を使っている。`@discardableResult` はこの状況で「将来3つ目の呼び出し元が戻り値を無視する」ときのコンパイラ警告を消してしまう — つまり本変更が防ごうとしている「state 更新の成否を見ずに Diff を流す」再発をコンパイラが指摘できなくなる。

**推奨修正**: `@discardableResult` を外す。現在の呼び出し元はどちらも結果を使うためビルドに影響しない。

### [🔵 Suggestion] Bridge の `updateAccessory` doc が未知 ID の no-op に触れていない

**該当箇所**: `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:220-222` / `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:264-265`

**問題点**: どちらも「canonical UUID 文字列として解釈できない場合は no-op」しか述べておらず、「妥当な UUID だが未知の sectionID でも no-op」に触れていない。誤りではないが、interop 表面の契約記述としては片手落ち。Bridge 実装を変えないという方針 (proposal の Non-Goals) はコメント追記を妨げない。

**推奨修正**: 1文追記する (例: 「Store の状態に存在しない sectionID でも no-op になる (core/ADR-0020)」)。

### [🔵 Suggestion] Robolectric テストの待機ヘルパが5ファイル目の重複になった

**該当箇所**: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnknownSectionAccessoryHostTest.kt:88-136`

**問題点**: `idle()` / `awaitConvergence()` / `committedTexts()` / `visibleRowTexts()` は `AttachOrderRestoreTest` / `AdapterReattachTest` / `StoreUnbindTest` 等と実質同一で、本変更で5ファイル目の複製になった。既存の書き方に揃えたこと自体は妥当だが、共有ヘルパへの抽出は今後さらに増える前に検討したい。

**推奨修正**: 本変更の責務外。別変更として起票を検討する。

## 確認した観点 (問題なし)

- **ビルド・テスト**: iOS `xcodebuild test` → TEST SUCCEEDED / 646 tests / 0 failures (iPhone 17 Pro)。Android `./gradlew test --rerun-tasks` → BUILD SUCCESSFUL / 2024 tests / 0 failures。いずれもレビュー側で実行して確認
- **宣言 UI 経路の取りこぼし**: 両 OS の `DSLDiffCalculator` は old/new 双方に存在する Section にのみ `UpdateAccessory` を積み、Section レベル Diff を先に適用する順序なので、guard 追加で落ちる更新経路はない
- **Bridge 実装の無変更**: `ios/Sources/KsSettingsViewBridge/` / `android/ks-settingsview-bridge/src/main/` に変更なし (proposal どおり)
- **旧挙動を固定するテストの残骸**: 未知 ID で Diff 発行を期待する Store テストは存在しない
- **Host の missing ID 検出の温存**: iOS `KsSettingsViewController.swift:1737` / Android `KsSettingsView.kt:697` は据え置き (core/ADR-0020 の決定どおり)
- **コメント規約**: `python3 scripts/comment-policy-lint.py` → 禁止 0 件 (466 ファイル)。新規コメントの `core/ADR-0020` 参照は許容形式、デルタスペック構文キーワードの混入なし
- **no-op の観測基準**: 状態ストリーム無発行まで観測する要求は両 OS で満たされている (iOS は `@Published` が同値でも発行するため `$root` の件数観測が実質的、Android は StateFlow の同値 conflate があるため相対的に緩いが、実装上そもそも代入が起きないため要求は充足)

## アクションプラン

1. **[Major]** MAUI 層3箇所のコメントを core/ADR-0020 後の契約に書き換える
2. **[Minor]** Android の操作契約表2ケースに検出力を与える (Store `diffs` 件数の観測を足す)、または表から落として `KsBridgeUpdateTest` に一本化する
3. **[Minor]** `SettingsRootStore.swift:334` の `@discardableResult` を外す
4. **[Suggestion]** 両 OS Bridge の `updateAccessory` doc に未知 ID no-op を1文追記する
5. **[Suggestion]** Robolectric 待機ヘルパの共通化は別変更として起票を検討する
