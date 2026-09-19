# Verify 001: picker-selection-after-dismiss

判定: **VALID**

- 検証対象: 作業ツリーの未コミット差分 (ベース `HEAD` = `9bc8b04`)。47 ファイル / +1904 / -86
- 作業ドメイン: cross (ios / android / maui)
- デルタスペック: 4 能力 (settings-view-ios-ui / settings-view-android-ui / maui-bridge / maui-cells)
- `deviation.md` は合意済み差分として扱う
- 検証スナップショット: 2026-09-19 12:45 以降の作業ツリー (後述「検証中の作業ツリーの変動」を参照)

## テスト実行結果

| platform | コマンド | 件数 / 失敗 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView -destination 'platform=iOS Simulator,id=…(iPhone 17 Pro)'` | 1088 / 0 |
| Android | `./gradlew test --rerun-tasks` | 2984 / 0 |
| MAUI | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | 595 / 0 |

- iOS の内訳 (バンドル集計行): Bridge 173 / Core 88 / SwiftUI 98 / TestSupport 7 / UI 722
- Android の内訳: `kssettingsview` debug 1315 + release 1315、`kssettingsview-bridge` debug 177 + release 177。すべて失敗 0
- Android の全件実行は最後に `:kssettingsview-bridge:testDebugUnitTest` で `NoSuchFileException: …/binary/in-progress-results-generic.bin` を出して BUILD FAILED になったが、結果 XML は 4 タスク分そろって失敗 0 である。`kasane/handbook/cross/test-execution.md` が「テストの失敗ではなく実行環境の問題」として記述している形で、当該モジュール単独の再実行 (`:kssettingsview-bridge:testDebugUnitTest --rerun-tasks`) は BUILD SUCCESSFUL だった
- lint: `scripts/local-path-lint.py` / `scripts/identity-lint.py` / `scripts/comment-policy-lint.py` いずれも exit 0 (comment-policy は検査対象 811 ファイル / 禁止 0 件)

## 対応表

### settings-view-ios-ui

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **PickerCell 単一選択の閉じ切り通知 (iOS)** | | | |
| 候補タップで値 callback の後に閉じ切り callback が届く | `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:287-293`、`ios/Sources/KsSettingsViewUI/PickerCellView.swift:93` | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:514`、順序は同:531 | ✅ |
| Cancel では閉じ切り callback が発火しない | `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:341` (completion を渡さない経路) | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:599` | ✅ |
| 対話的 dismiss では閉じ切り callback が発火しない | 同上 (提示元からの dismiss は選択面の確定経路を通らない) | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:637` | ✅ |
| callback を渡さない既存の呼び出しはそのまま動く | `ios/Sources/KsSettingsViewUI/PickerCell.swift:76` (省略可能引数) | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:675` | ✅ |
| **PickerCell 複数選択の閉じ切り通知 (iOS)** | | | |
| 確定操作で値 callback の後に閉じ切り callback が届く | `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:326-336`、`ios/Sources/KsSettingsViewUI/PickerCellView.swift:96` | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:555`、順序は同:577 | ✅ |
| 候補のトグルだけでは閉じ切り callback が発火しない | `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:296` (multiple はトグルのみ) | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:657` | ✅ |
| Cancel では閉じ切り callback が発火しない | `ios/Sources/KsSettingsViewUI/PickerListViewController.swift:341` | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:617` | ✅ |
| **PickerCell の閉じ切り callback はすべての構築経路と再構築で保持される (iOS)** | | | |
| Binding 経路で閉じ切り callback を指定できる | `ios/Sources/KsSettingsViewUI/PickerCell.swift:116,133`、同:200,218 | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1094`、複数は同:1112 | ✅ |
| object 射影経路で閉じ切り callback を指定できる | `ios/Sources/KsSettingsViewUI/PickerCell+ItemProjection.swift:42,86,131,187,233,281,322,361,402,445` (ジェネリック / String 特殊化の全 init) | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1133,1155,1174` | ✅ |
| 再構築後も閉じ切り callback が残る | `ios/Sources/KsSettingsViewUI/PickerCell.swift:375,400,423` (`with*` 系の転送) | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift:689` | ✅ |
| **DatePickerCell の閉じ切り通知 (iOS)** | | | |
| カレンダーの Done で値 callback の後に閉じ切り callback が届く | `ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift:175-183`、`ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:163` | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1211`、順序は同:1234 | ✅ |
| カレンダーの Cancel と対話的 dismiss では発火しない | `ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift:171` (Cancel は completion なし) / 同:87 (`presentationControllerDidDismiss` は `onDismissed` のみ) | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1269` (Cancel のみ) | ✅ 備考: 対話的 dismiss 側のテストは無い (後述「所見」1) |
| ホイールの Done で入力面が閉じ切った後に閉じ切り callback が届く | `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:207-227,232-268` (`WheelsCompletionWatch`) | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1289,1312` | ✅ 備考: 手順は deviation.md 1 件目 (合意済み) |
| 派生後も DatePickerCell の閉じ切り callback が残る | `ios/Sources/KsSettingsViewUI/DatePickerCell.swift:174,185,196` (`withDSLID` / `withStyle` / `withIcon`) | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1386` | ✅ |
| ホイールの Cancel では発火しない | `ios/Sources/KsSettingsViewUI/DatePickerCellView.swift:194` (`reset` で待ち受けを畳む) / Cancel 経路は待ち受けを張らない | `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1366` | ✅ |

補足: Requirement 本文の「値を渡す構築経路と `Binding` を渡す構築経路の両方で指定でき」は `ios/Sources/KsSettingsViewUI/DatePickerCell.swift:65`(値) / :106,118(Binding) が満たし、`ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:1405` が Binding 経路を覆う。

### settings-view-android-ui

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **PickerCell 単一選択の閉じ切り通知 (Android)** | | | |
| 候補タップで値 callback の後に閉じ切り callback が届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:524-526`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:77-78` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:255` | ✅ |
| 非確定 dismiss のどの経路でも閉じ切り callback が発火しない | 同上 (`completedSelection` は確定経路でのみ立つ) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:270` (取消ボタン / cancel = 外側タップ・Back / dismiss = 下スワイプ) | ✅ |
| **PickerCell 複数選択の閉じ切り通知 (Android)** | | | |
| OK で値 callback の後に閉じ切り callback が届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt:253-258`、`…/PickerCellViewHolder.kt:79-81` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:289` | ✅ |
| 候補のトグルだけでは閉じ切り callback が発火しない | 同上 (トグルは `workingSelection` のみ更新) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:304` | ✅ |
| キャンセルでは閉じ切り callback が発火しない | 同上 | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:316` | ✅ |
| **DatePickerCell の閉じ切り通知 (Android)** | | | |
| Spinner の OK で値 callback の後に閉じ切り callback が届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheet.kt:320,670-674`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCellViewHolder.kt:143-145` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheetTest.kt:489` | ✅ |
| カレンダーの OK で値 callback の後に閉じ切り callback が届く | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialog.kt:210,288-293`、`…/DatePickerCellViewHolder.kt:109-112` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarDialogTest.kt:251` | ✅ |
| 非確定 dismiss では発火しない | 同上 (`completedDate` は確定経路でのみ立つ) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateSelectionSheetTest.kt:505` / `…/DateCalendarDialogTest.kt:267` | ✅ |
| **閉じ切り callback はすべての構築経路で指定できる (Android)** | | | |
| 射影 factory で構築した PickerCell に閉じ切り callback を渡せる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellItemProjection.kt:44,63 / 97,117 / 143,162 / 184,204` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellItemsTest.kt:131,150` | ✅ |
| Compose DSL で構築した PickerCell に閉じ切り callback を渡せる | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:126,167,209,250,298,341` | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/PickerCellObjectBindingTest.kt:67,92,116` | ✅ |
| **閉じ切り通知を足しても選択面の再提示と参照解放は従来どおり働く (Android)** | | | |
| 確定して閉じた後に同じ Cell の選択面を再び開ける | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt:77-82` (`setOnDismissListener` の所有は `HostAnchoredDialog.kt:35` に残る) | `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt:328` | ✅ |

補足: Requirement 本文の「DatePickerCell の閉じ切り callback は data class の直接構築と Compose DSL の overload で指定できる」は `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/DatePickerCell.kt:53` と `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:469,489` が満たす (対応する Scenario は無く、DSL datePicker 経路の専用テストも無い)。`copy` での保持は Kotlin data class の生成 `copy` による。

### maui-bridge

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **PickerCell の閉じ切り通知を interaction 経路で中継する** | | | |
| 単一選択の閉じ切りが cellID と index 付きで届く | iOS: `ios/Sources/KsSettingsViewBridge/KsBridgeInteractionDelegate.swift:96`、`…/KsBridgeInteractionRelay.swift:76`、`…/KsBridgePickerCell.swift:63` / Android: `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeInteractionListener.kt:102`、`…/KsBridgeInteractionRelay.kt:74`、`…/KsBridgePickerCell.kt:96` | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeInteractionDelegateTests.swift:164` / `android/kssettingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeInteractionListenerTest.kt:214` | ✅ |
| 複数選択の閉じ切りが正規化された indices 付きで届く | iOS: `…/KsBridgeInteractionDelegate.swift:103`、`…/KsBridgeInteractionRelay.swift:81` (`sorted`)、`…/KsBridgePickerCell.swift:86` / Android: `…/KsBridgeInteractionListener.kt:110`、`…/KsBridgeInteractionRelay.kt:79` (`KsBridgeValueTransport.indexList`)、`…/KsBridgePickerCell.kt:77` | `ios/Tests/…/KsBridgeInteractionDelegateTests.swift:164` / `android/…/KsBridgeInteractionListenerTest.kt:246` | ✅ |
| 破棄後は閉じ切り通知も届かない | relay の `delegate` / `listener` が `dispose` で外れる既存経路をそのまま使う | `ios/Tests/…/KsBridgeInteractionDelegateTests.swift:190` / `android/…/KsBridgeInteractionListenerTest.kt:266` | ✅ |
| **DatePickerCell の閉じ切り通知は interaction 経路に含めない** | | | |
| DatePicker を確定して閉じても追加の通知は届かない | `ios/Sources/KsSettingsViewBridge/KsBridgeDatePickerCell.swift:75` / `android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeDatePickerCell.kt:75` — いずれも `onValueChanged` のみ結線 (`onValueCompleted` は未結線) | `ios/Tests/…/KsBridgeInteractionDelegateTests.swift:210` / `android/…/KsBridgeInteractionListenerTest.kt:288` | ✅ |

iOS binding の公開面: `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:684,691` に 2 メソッドを `[Abstract]` + `[Export]` で追加済み。

### maui-cells (MODIFIED: PickerCell の選択完了 Command)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定通知の時点ではコマンドが実行されない | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2213`、同:2235 (`NotifySelectionCompleted` 呼び出しを削除、書き戻しのみ) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:31` | ✅ |
| 閉じ切り通知でコマンドが SelectedItem を引数に 1 回実行される | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2238-2244`、`maui/KsSettingsView.Maui/PickerCell.cs:315-321` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:58` | ✅ |
| 複数選択の閉じ切り通知でコマンドが SelectedItems を引数に 1 回実行される | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2247-2253` | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:88` | ✅ |
| 選択面の表示中に SelectionMode が変わっても届いた通知の種類で引数を選ぶ | `maui/KsSettingsView.Maui/PickerCell.cs:315` (引数 `mode` は通知の種類) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:296` (逆向きは同:318) | ✅ |
| SelectedCommand 未設定なら閉じ切り通知で何も起きない | `maui/KsSettingsView.Maui/PickerCell.cs:320` (`SelectedCommand?.Execute`) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:191` | ✅ |
| PickerCell 以外の cellId の閉じ切り通知は無視される | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2240,2249` (`FindCell(cellId) is PickerCell`) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:254` (未知 ID は同:211,232) | ✅ |
| 確定から閉じ切りまでの間に選択が変わっても閉じ切り時点の現値を渡す | `maui/KsSettingsView.Maui/PickerCell.cs:317-319` (現値を都度読む) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:276` | ✅ |
| 同じ選択の再確定も閉じ切り通知で完了として通知 | `maui/KsSettingsView.Maui/Internals/KsSettingsController.cs:2213` (書き戻しは `Write` の同値判定に従い、実行は閉じ切り側) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:120` | ✅ |
| CanExecute が false でも閉じ切り通知で実行する | `maui/KsSettingsView.Maui/PickerCell.cs:320` (`Execute` 直呼び) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:151` | ✅ |
| 公開プロパティの直接設定では実行しない | setter 経路に `NotifySelectionCompleted` が無い (呼び出しは `KsSettingsController.cs:2242,2251` の 2 箇所のみ) | `maui/KsSettingsView.Maui.Tests/PickerSelectedCommandTests.cs:171` | ✅ |
| Handler 切断後は閉じ切り通知でコマンドが実行されない | `maui/KsSettingsView.Maui/Internals/IKsInteractionSink.cs:72,80` の sink が解放で外れる既存経路 | `maui/KsSettingsView.Maui.Tests/InteractionLifetimeTests.cs:62` | ✅ |

sink → facade の配線: `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs:629,633` / `maui/KsSettingsView.Maui/Platforms/Android/KsBridgeGateway.cs:619,623`。

## 追加検査

| 項目 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 未チェックは 6.1 と 7.3 の 2 件のみ。いずれもオーナー担当の実機分で、コンテキストパッケージの申し送りどおり |
| 虚偽チェック (未実装なのにチェック済み) | 無し。1.1〜5.3 の各タスクは上表の実装列と 1 対 1 で対応する。7.2 の「`ios/Package.swift` の差分が無い」も `git status` で確認 (差分なし) |
| 逆流検査 (足場の書き換え) | 無し。`proposal.md` / `design.md` / `specs/` は `9bc8b04` のまま未変更。作業ツリーで変更されている change 配下のファイルは `tasks.md` (チェック更新) と新規の `deviation.md` / `review-001.md` / `second-opinion-code-001.md` / `evidence/integration-host/` のみ |
| 未記録乖離 | 無し。diff のうち Scenario に対応しない変更 (Android 復元経路の結線、テスト支援の待機ヘルパ、`PickerCellObjectBindingTest` の Robolectric 化、`PickerCell.cs` の doc コメント) はすべて `deviation.md` の `[付随修正]` に記録済み。`DateCalendarRecreationTest.kt` に足した 2 件は復元経路の付随修正に対するテストで、同じ記録の範囲 |
| deviation の内容 | 2 件の合意済み乖離 (ホイールの待ち受け手順 / 提示元が無い場合の即時 completion) はいずれも実装に現れており、spec の Scenario の契約 (順序・1 回・非発火) は保たれている |
| UI 変更のモック照合 | 本変更に `ui/` アーティファクトは無い (通知経路の追加であり見た目の変更を伴わない)。`proposal.md` / `tasks.md` もモックを求めていない |
| テスト全件成功 | 上記「テスト実行結果」のとおり 3 platform とも失敗 0 |

## 所見 (INVALID の根拠にはしない)

1. **iOS カレンダーの対話的 dismiss のテストが無い** — Scenario「カレンダーの Cancel と対話的 dismiss では発火しない」のうち、対話的 dismiss 側を直接駆動するテストが無い (`DatePickerCalendarSheetController` に `_simulateCancel` はあるが対話的 dismiss の hook が無い)。実装は `ios/Sources/KsSettingsViewUI/DatePickerCalendarSheetController.swift:87` の `presentationControllerDidDismiss` が `onDismissed` だけを呼ぶ構造で、`onDoneCompleted` は `handleDone` の completion からしか渡らないため契約は満たす。PickerCell 側には対話的 dismiss のテスト (`PickerSelectionScreenTests.swift:637`) がある。見立て: 追加するならテスト側だけの追補で、実装修正も deviation も要さない。

2. **tasks.md 5.1 が参照する Requirement 名が spec に無い** — 5.1 は「(→ Requirement: PickerCell の SelectedCommand は選択面が閉じ切った後に実行する)」と書くが、`specs/maui-cells/spec.md` の MODIFIED Requirement 名は「PickerCell の選択完了 Command」。実装・テストは spec 側の Requirement を満たしているため乖離ではなく、tasks の参照名の食い違い。足場は凍結中なので修正はしない。

3. **実機分は未実施** — tasks 6.1 (iPhone 11 実機での dismiss 実時間計測) と 7.3 の iOS 実機導線確認はオーナー担当で未実施。いずれも spec の Scenario に対応しない実機証跡であり、判定には含めていない。Simulator / Emulator 分の end-to-end は `evidence/integration-host/README.md` に記録済み。

4. **検証中の作業ツリーの変動** — 検証の最中に第三者 (本セッション外) が `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt` を 12:34 と 12:45 の 2 回書き換えた。12:34〜12:45 の一時的な内容は `cell.onValueCompleted?.invoke(dialog.completedDate ?: cell.date)` で、非確定 dismiss でも閉じ切り callback を発火するため Android の DatePickerCell Requirement に反する形だった。12:45 に元の `dialog.completedDate?.let { … }` へ戻っており、本報告の対象 (現在の作業ツリー) はこの戻した後の内容。全件テストを回した版と同一であることは diff の一致で確認した。**再検証時は同じ churn が起きていないか作業ツリーを先に確認すること。**

5. **`DateCalendarRecreationTest` の flaky 疑い** — 12:34 に第三者が回したと見られる実行の結果 XML に「再生成後の選択面の確定でも閉じ切り callback が値 callback の後に届く」の失敗が残っていた (`expected:<[changed, completed]> but was:<[changed]>`、Robolectric の `UnExecutedRunnablesException` 付き)。自分の全件実行 (Android 2984 / 0) と、12:52 時点の `:kssettingsview` debug 全件 (1315 / 0) ではいずれも緑。当該テスト (`android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/DateCalendarRecreationTest.kt:171`) はカレンダーの選択面を提示した後に `idle()` を呼んでおり、`kasane/handbook/cross/test-execution.md` が避けるよう定めている書き方にあたる。同ファイルの他の閉じ切りテスト (同:184) も同様。見立て: 実装の欠落ではなく待機の書き方の問題。条件ベース待機 (`KsSettingsViewTestSupport` の `awaitMainLooperCondition` 等) への置き換えが要るが、本変更の判定は変えない。
