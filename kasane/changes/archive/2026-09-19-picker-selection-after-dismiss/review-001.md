# レビュー結果: picker-selection-after-dismiss (001 回目)

**日付**: 2026-09-19
**判定**: CHANGES_REQUESTED

## サマリー

4 能力・3 プラットフォームにまたがる閉じ切り通知の配線は、デルタスペックの Requirement / Scenario をほぼ全面的に満たしている。構築経路 (iOS 公開 init 4 本 + 射影 init 10 本 + `with*` 3 本、Android data class + 射影 factory 4 本 + Compose DSL 7 本) に漏れはなく、非確定 dismiss で発火しないことが構造 (completion / 控えを確定経路だけが立てる) で保たれている点も design Decision 3・5 のとおり。テストは iOS 1088 / Android 2980 / MAUI 595 をレビュー側で再実行して全件成功を確認し、Swift 6 言語モード相当のビルドも error 0 件だった。

差し戻すのは 1 点のみ。deviation.md の `[付随修正]` のうち「構成変更後のカレンダー復元経路で `onValueCompleted` を呼ぶ」だけが、ksn-core 付随修正の同梱条件④ (テストで担保) を満たしていない。復元経路は本 change が新しく開けた分岐で、壊れても無言で通知が落ちるため、既存テストファイルに 1 件足して閉じてほしい。

## 照合した規約

- `kasane/handbook/cross/comment-policy.md` (always) — 新規コメントに禁止参照・禁止類型なし。`scripts/comment-policy-lint.py` 禁止 0 件 (要確認 170 件はすべて本 change の差分行の外)。公開 doc コメント (iOS `PickerCell` / Android `PickerCell` `DatePickerCell` / `IKsInteractionSink` / `PickerCell.cs` / `ApiDefinition.cs`) に内部用語 (change 名・spec・Kasane・Decision 番号) の混入なし
- `kasane/handbook/cross/test-execution.md` (テスト実行・結果報告) — 3 platform とも規約どおりのコマンドで実行し件数まで確認。新規の待機ヘルパ `awaitMainLooperCondition` は実時間 deadline + `Thread.sleep(1)` + 実測値付き `fail()` の 3 条件を満たし、不変性確認用は `drainMainLooperForUnchangedCheck` と名前で判別できる形 (cross/ADR-0027)
- `kasane/handbook/cross/runtime-behavior-verification.md` (実行時挙動の完了判定) — Simulator / Emulator での再現確認と証跡 (`evidence/integration-host/`) が実在し、提出コードと対応する
- `kasane/handbook/maui/integration-host-verification.md` (binding / facade の end-to-end) — 完了条件 (IntegrationHost 固定シナリオ・解放→再生成・MauiHost の更新と再訪問・外観追随) の確認が README に記録されている
- `kasane/handbook/ios/swift6-language-mode-check.md` (`ios/Sources/` を触る完了判定) — `ios/Package.swift` に差分・`swiftLanguageVersions` の残置なし。レビュー側で `OTHER_SWIFT_FLAGS='-swift-version 6'` によるパッケージ全体ビルドを行い error 0 件を確認
- 参照した決定: `kasane/decisions/core/0034-*.md` (proposed。本 change の上位決定と整合)、`kasane/decisions/maui/0002-*.md` (1 事象 1 メソッドの流儀に沿う)、`kasane/decisions/maui/0012-*.md` (書き戻しは確定通知のまま・複数選択は昇順重複なし正規化。両方とも維持されている)

## 検証したこと (指摘に至らなかった確認)

- **テスト再実行**: iOS `xcodebuild test` (iPhone 17 Pro Simulator) 1088 件 / 0 失敗、Android `./gradlew test --rerun-tasks` 2980 件 / 0 失敗 (JUnit XML 集計)、MAUI `dotnet test` 595 件 / 0 失敗。実装者の報告と一致
- **lint**: `scripts/comment-policy-lint.py` (禁止 0 件) / `local-path-lint.py` / `identity-lint.py` いずれも exit 0
- **証跡の実在と対応**: `evidence/integration-host/` の 2 枚は両 OS の「MAUI 固有 Cell 機能デモ」で `完了通知: 1 回` を示しており、README の記述 (キャンセル後の撮影・回数が増えない) と画面が一致する。`evidence/dismiss-timing/README.md` は提案段階の計測で、実機節は未実施と明記されている
- **構築経路の網羅**: iOS は `PickerCell.swift` の公開 init 4 本 + internal 全部入り init + `withDSLID` / `withStyle` / `withIcon` の 3 箇所、`PickerCell+ItemProjection.swift` の 10 init、`DatePickerCell.swift` の 2 init + 転送 3 箇所すべてに通っている。Android は data class + `PickerCellItemProjection.kt` 4 factory + `compose/InputCellDsl.kt` の picker 6 overload・datePicker 1 overload
- **非確定不発火の構造**: iOS は確定経路だけが `dismissModal(completion:)` / `dismissSheet(completion:)` に completion を渡す。Android は確定経路だけが `completedSelection` / `completedMultiSelection` / `completedDate` を立て、`setOnDismissListener` の所有は `HostAnchoredDialog` に残っている (design Decision 5 のとおり)
- **ホイールの順序保証** (deviation 1): `WheelsCompletionWatch` は「値 callback 送出済み」「非表示完了 報告済み」の 2 記録がそろってから発火するため、`resignFirstResponder()` の中で `keyboardDidHideNotification` が同期に届く環境でも Changed → Completed の順が崩れない。1 秒打ち切り・再 Done での張り直し・`prepareForReuse` / 解放時の破棄も実装されており、`InputCellsTests` が実 window で通知経路ごと検証している
- **MAUI の引数解決**: `NotifySelectionCompleted(mode)` の `mode` が届いた通知の種類で決まり、引数は閉じ切り時点の現値であることが `SingleCompletionUsesSelectedItemAfterModeChangesToMultiple` / `CompletionPassesCurrentValueWhenSelectionChangedInBetween` で固定されている
- **deviation の網羅性** (lessons/process L-009): `git status` に現れて tasks.md が名指ししていないファイルは 4 つ (`android/.../ui/KsSettingsView.kt`、`android/.../test/.../KsSettingsViewTestSupport.kt`、`android/.../test/.../compose/PickerCellObjectBindingTest.kt` の Robolectric 化、`maui/KsSettingsView.Maui/PickerCell.cs` の doc コメント) で、すべて deviation.md に行がある
- **足場の凍結**: `proposal.md` / `design.md` / `specs/` に差分なし。`tasks.md` の差分はチェック状態と 7.3 の進捗注記のみで、スコープの書き換えではない
- **Android binding 再生成 (tasks 4.2)**: `Transforms/Metadata.xml` に差分がなく、Android Emulator で MAUI Sample が動いた証跡が「追記なしで C# に露出した」ことの裏取りになっている

## 指摘事項

### [🟡 Minor / 優先度高] 構成変更後のカレンダー復元経路の閉じ切り通知に回帰テストがない

**該当箇所**: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:1280`-`1283`
(関連: `deviation.md` の `[付随修正] .../ui/KsSettingsView.kt の構成変更後のカレンダー復元経路`)

**問題点**:
この付随修正は「提示し直したカレンダーの dismiss でも `onValueCompleted` を呼ぶ」という**新しい観測可能な挙動**を足しているが、これを固定するテストが 1 件もない。`onValueCompleted` に触れるテストは `DateSelectionSheetTest` と `DateCalendarDialogTest` だけで、どちらも復元経路 (`KsSettingsView.restoreCalendarDialog` 相当) を通らない。復元経路を持つ `DateCalendarRecreationTest` は `onValueChanged` しか観測していない (`DateCalendarRecreationTest.kt:132` の `再生成後の選択面の確定は維持した選択日を1回だけ通知する`)。

ksn-core「付随修正」の同梱条件④は「既存テストの通過と、必要なら 1 件のテスト追加で担保できる」ことを同梱の要件にしている。ここは既存テストでは担保されず、追加もされていないため条件を満たしていない。

実害の形も具体的で、`showAnchoredTo(this, forgetDialog)` の形へ戻す・`completedDate` の読み出しを落とす、といった変更が入ると**回転をまたいだときだけ閉じ切り通知が無言で落ちる**。本 change が解こうとしている症状 (通知が来ないので利用側が固定待ちを書く) と同じ形の退行が、テストに引っかからずに通る。

該当する Requirement / Scenario: `specs/settings-view-android-ui/spec.md` の Requirement「DatePickerCell の閉じ切り通知 (Android)」Scenario「カレンダーの OK で値 callback の後に閉じ切り callback が届く」は、復元後のカレンダーでも同じ契約であることを前提にしている (Requirement 本文の「uiStyle に関係なく同じ契約である SHALL」と、Requirement「閉じ切り通知を足しても選択面の再提示と参照解放は従来どおり働く (Android)」の趣旨)。復元経路だけがその担保を欠いている。

**推奨修正**:
`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt` に 1 件足す。既存の `dateCell(...)` ヘルパに `onValueCompleted` を通し、`再生成後の選択面の確定は維持した選択日を1回だけ通知する` と同じ手順 (launch → openDialog → moveTo → recreate → `restored.confirmSelection()`) のあとに `awaitMainLooperCondition` で閉じ切り通知の到着を待って、`changed` → `completed` の順と同じ日付を確かめる。あわせて、復元後の取消では発火しないことも同じテスト内か隣接テストで 1 行確かめておくと、控えの立て忘れ / 誤発火の両方向が固定される (`DateCalendarDialogTest.kt:247` 付近の書き方をそのまま流用できる)。

### [🔵 Suggestion] Handler 切断後の閉じ切り通知テストが「通知先が外れたこと」までしか見ていない

**該当箇所**: `maui/KsSettingsView.Maui.Tests/InteractionLifetimeTests.cs:62` (`ReleasingHostLeavesNoDestinationForSelectionCompletion`)

**問題点**:
`specs/maui-cells/spec.md` の Scenario「Handler 切断後は閉じ切り通知でコマンドが実行されない」の WHEN は「破棄前の選択面が閉じ切る」だが、テストは `ReleaseHost()` 後に `scope.Gateway.Sink` が null であることを確かめるだけで、切断前に取っておいた sink へ閉じ切り通知を流す動作をしていない。通知が facade へ渡る経路が gateway の sink 参照だけである限りこの assert で足りるが、同じファイルの `NotificationForRemovedCellIsDropped` は「解除前に sink を控えて、解除後に流して何も起きない」形で書かれており、Scenario の WHEN により近い。

**推奨修正**:
必須ではない。揃えるなら `IKsInteractionSink sink = scope.Gateway.Sink!;` を解放前に控え、`view.ReleaseHost()` の後に `sink.PickerCellSelectionCompleted(cellId, 1)` を呼んで `executed` が 1 のままであることを確かめる形にすると、`NotificationForRemovedCellIsDropped` と流儀が揃い、Scenario の文言とも一対一になる。

### [🔵 Suggestion] iOS の「順序と同値」テストは提示なし経路を通るため、順序だけを担保している (所見)

**該当箇所**: `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:514`、`ios/Sources/KsSettingsViewUI/PickerListViewController.swift:342`

**問題点**:
deviation.md 2 点目で合意済みの「提示元が無い場合は閉じ切り済みとして completion を即時に呼ぶ」分岐により、`test_閉じ切り_単一選択は値callbackの後に同じindexで届く` 系の**肯定側**テストは dismiss アニメーションを一度も経由しない。したがってこれらが担保しているのは配線・発火順序・運ぶ値であって、「dismiss 完了の後に届く」ことではない。この点はテスト本文の先頭コメントに明記されており、`..._選択面が出ている間は届かない` の否定側テストと `evidence/integration-host/` が補っているので、合意済みの差分として指摘ではなく所見として残す。

**推奨修正**:
なし。ただし tasks 6.1 / 7.3 の実機分をオーナーが実施する際、「dismiss 完了の後に届く」ことの実機確認はこの 3 点 (否定側テスト・統合ホスト・実機導線) が唯一の根拠である旨を証跡側に一言残しておくと、蒸留時に検証範囲を誤読しない。

## アクションプラン

1. **[必須]** `DateCalendarRecreationTest.kt` に復元後カレンダーの閉じ切り通知 (確定で発火 / 取消で不発火) のテストを足す。追加後は `cd android && ./gradlew test --rerun-tasks` で全件を回し直し、件数と 0 失敗を報告する
2. **[任意]** `InteractionLifetimeTests.ReleasingHostLeavesNoDestinationForSelectionCompletion` を、解放前に控えた sink へ閉じ切り通知を流す形へ揃える
3. 上記 1 を deviation.md の該当行へ反映する必要はない (テスト追加は付随修正の担保であり、新たな乖離ではない)。既知の先送り (tasks 6.1 / 7.3 の iOS 実機分) は本レビューの指摘対象外として扱った
