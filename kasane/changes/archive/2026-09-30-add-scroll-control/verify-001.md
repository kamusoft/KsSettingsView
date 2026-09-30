# 検証結果: add-scroll-control (001 回目)

**日付**: 2026-09-30
**判定**: INVALID

**対象**: コミット 38a6c74 (HEAD) からの作業ツリーの差分のうち、`kasane/changes/add-scroll-control/` を除くもの (`ios/`、`android/`、`maui/`、`samples/`)。突き合わせたのは、デルタスペック 7 本 (Scenario 97 件)、`tasks.md`、`deviation.md` である。

## サマリー

実装とデルタスペックの対応に欠落・未記録の乖離は無い。97 件の Scenario はすべて、実装とテスト (実行時の挙動を問うものは `evidence/` の実機観測) に対応している。deviation.md の各行は合意済みの差分として扱い、違反としていない。足場の逆流は無く、テストは全件成功している。

INVALID の理由は、未了の検証タスク 1 件である。**tasks.md 6.3 (3 platform の Sample の実機観測) が完了していない**。これに伴い、Sample の Scenario 2 件で実機観測の一部が欠けている。iOS 系の「Section へ」の到着を、修正後のビルドで観測していない。ksn-verify の VALID は「全タスク完了・全 Scenario が一致」を条件にしている。検証タスクが未了のまま VALID とすると、アーカイブの可否の判定を前倒しすることになる。そのため、未了は ❌ として数えた。見立ては末尾の「❌ の一覧と見立て」に書く。いずれも実装の修正は要らない見込みで、撮り直しを済ませれば解消する。

## 事前条件の検査

| 検査 | 結果 | 根拠 |
|---|---|---|
| 逆流 (足場の書き換え) | ✅ なし | `git status` / `git diff` で、`proposal.md`・`design.md`・`specs/` に HEAD からの差分は無い。`tasks.md` の差分は 39 行で、すべてチェックの `[ ]` → `[x]` だけ (`--word-diff` で確認) |
| tasks.md の虚偽チェック | ✅ なし | チェック済み 39 件は、対応表・テスト実行・`evidence/` で裏付けを確かめた。3.3 の「要れば `Transforms/Metadata.xml` を直す」は差分が無い。IntegrationHost の Android (`maui/tests/KsSettingsView.IntegrationHost.Android/MainActivity.cs`) が managed の `ScrollToEnd` / `ScrollToStart` を呼び、`evidence/integrationhost-android-2-scroll-to-end.png` で動作している。したがって、変更不要で自動束縛されたと判断した。6.4 の証跡は `evidence/maui-*-reconnect-verify-*.png`・`evidence/android-native-recreate-verify-*.png` にある |
| tasks.md の未完了 | ❌ 1 件 | 6.3 が未チェック (下記 ❌-1) |
| 未記録乖離 | ✅ なし | 対応表に「乖離」の行は無い。diff にあって Scenario に対応しない変更は次のとおりで、いずれも deviation.md に `[付随修正]` または実装判断として記録済みである: `KsBridgeScrollPosition` の切り出し、`KsWireValues.ScrollPosition`、`KsSettingsController` の partial 化、setStyle の doc コメント、IntegrationHost の「末尾へ」「先頭へ」ボタンと「スクロール確認」Section、`captureScrollAnchor()` の未実行の控え、Bridge の `KsBridgeHostAnchorTracker`、iOS の `CADisplayLink` による一方向の着地、DSL の受け口が命令を捨てる扱い |
| UI 変更の brief / モック | 対象外 | proposal.md の「級」節で、`ui/` を作成しないと明記されている (見た目の変更が無い) |
| テストの全件成功 | ✅ | 下記「テストの実行」 |

## テストの実行

| platform | 実行 | 結果 |
|---|---|---|
| Android | 本検証で `./gradlew test --rerun-tasks` を実行した。件数は `build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` を集計した | kssettingsview 1380 × 2、kssettingsview-bridge 193 × 2 の計 **3146 tests / 0 failures / 0 errors** |
| MAUI | 本検証で `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` を実行した | **624 tests / 0 failures** |
| iOS | 制約 (Simulator を起動しない) のため、テストは実行していない。代わりに `xcodebuild build-for-testing -scheme KsSettingsView -destination 'generic/platform=iOS Simulator'` で、テストを含むパッケージ全体のビルドを確かめた (Simulator を起動しない) | `** TEST BUILD SUCCEEDED **`、error 0。実行結果は、ホスト側の最新の報告 **1184 tests / 0 failures** (Bridge 189 + Core 88 + SwiftUI 122 + TestSupport 7 + UI 778) を事実として扱った。review-003.md でも同じ扱いである |
| Swift 6 言語モード | 手順が `ios/Package.swift` の一時的な書き換えを伴うため、本検証では行っていない (コードを書き換えない制約) | ホスト側の報告 error 0 を事実として扱った |

## 対応表

凡例: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離・未了。

テストのパスは次のように略す。いずれもリポジトリルートからの相対パスである。

- iOS
  - `iUI` = `ios/Tests/KsSettingsViewUITests/ScrollControlTests.swift`
  - `iDSL` = `ios/Tests/KsSettingsViewSwiftUITests/DSLScrollControlTests.swift`
  - `iBr` = `ios/Tests/KsSettingsViewBridgeTests/KsBridgeScrollControlTests.swift`
- Android
  - `aUI` = `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/ui/ScrollControlTest.kt`
  - `aRel` = `.../ui/ScrollControllerReleaseTest.kt`
  - `aRec` = `.../ui/ScrollRecreationTest.kt`
  - `aC` = `android/kssettingsview/src/test/kotlin/jp/kamusoft/kssettingsview/compose/ComposeScrollControlTest.kt`
  - `aCRec` = `.../compose/ComposeScrollRecreationTest.kt`
  - `aBr` = `android/kssettingsview-bridge/src/test/kotlin/jp/kamusoft/kssettingsview/bridge/KsBridgeScrollControlTest.kt`
- MAUI
  - `mT` = `maui/KsSettingsView.Maui.Tests/ScrollControlTests.cs`
- 実装
  - `iSC` = `ios/Sources/KsSettingsViewUI/KsSettingsViewController+ScrollControl.swift`
  - `iVC` = `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift`
  - `iRes` = `ios/Sources/KsSettingsViewSwiftUI/DSLScrollCommandResolver.swift`
  - `aSC` = `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsViewScrollControl.kt`
  - `aV` = `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt`
  - `aRes` = `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScrollCommandResolver.kt`

### settings-view-ios-ui (29 件)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ハンドルの公開 / 未接続のハンドルへの命令は何も起こさない | `ios/Sources/KsSettingsViewUI/KsScrollController.swift:31-45` | `iUI:140` | ✅ |
| ハンドルの公開 / protocol 型の変数からも同じ命令を出せる | `ios/Sources/KsSettingsViewUI/KsScrollControlling.swift:15-80` | `iUI:151` (既定値 8 通りは `iUI:187`) | ✅ |
| 接続 / 最後に接続した Host だけが命令を受ける | `KsScrollController.swift:48-57`、`iVC:98-106` | `iUI:214` | ✅ (差し替え時に未実行の命令を捨てる扱いは ⚠️ deviation 記録済み) |
| 接続 / 接続を外したハンドルの命令は何も起こさない | `iVC:98-106` | `iUI:233` | ✅ |
| 接続 / Store から切断すると接続が外れる | `iVC:361-362` | `iUI:250` | ✅ |
| 接続 / ハンドルは Host の寿命を延ばさない | `KsScrollController.swift:24` (weak) | `iUI:267` | ✅ |
| 反映の後 / Cell の追加と同じ処理で出した末尾への命令が新しい末尾へ届く | `iSC:130-183`、`iVC:1153-1161` | `iUI:287` (反映中は実行しないことを `iUI:310` で確認) | ✅ |
| 反映の後 / 続けて出した命令は最後の命令の位置で止まる | `iSC:174-183`、`iSC:257-262` | `iUI:337` (先行のアニメーションを止めることを `iUI:748` で確認) | ✅ |
| 反映の後 / 実行前に対象が削除された命令は飛ばされ、次の命令は実行される | `iSC:189-223` (実行の直前に解決) | `iUI:355` | ✅ (注 1) |
| 反映の後 / 画面の読み込み前に出した命令は初回の表示の後に実行される | `iSC:160-171`、`iVC:512-516` | `iUI:372` | ✅ |
| Cell の位置 / 表示範囲の外の Cell を中央に合わせる | `iSC:361-406` | `iUI:413` | ✅ (行も `setContentOffset` で送る点は ⚠️ deviation 記録済み) |
| Cell の位置 / 末尾付近の Cell を上端に合わせようとすると端で止まる | `iSC:385` | `iUI:444` | ✅ |
| Cell の位置 / 非表示の Cell と存在しない ID への命令は位置を変えない | `iSC:208-223`、`iSC:235-242` | `iUI:459` | ✅ |
| Section / start で見出しが表示範囲の上端に来る | `iSC:226-233`、`iSC:417-453` | `iUI:478` | ✅ |
| Section / 見出しの無い Section は最初の Cell が上端に来る | `iSC:428-443` | `iUI:492` | ✅ |
| Section / 非表示の Section と表示される要素の無い Section への命令は位置を変えない | `iSC:226-233`、`iSC:451` | `iUI:558` | ✅ |
| 両端 / 末尾への命令で Root Footer の下端まで見える | `iSC:199-200`、`iSC:373-374` | `iUI:577` | ✅ |
| 両端 / 先頭への命令で Root Header の上端まで戻る | `iSC:197-198`、`iSC:371-372` | `iUI:596` | ✅ |
| 一方向の着地 / 中央合わせのアニメーションが行き過ぎて戻らない | `iSC:264-348`、`ios/Sources/KsSettingsViewUI/KsActiveScrollAnimation.swift` | `iUI:679` (24 フレームが単調に進む。`iUI:700`・`iUI:725` で補強)。実機: `evidence/ios-native-frames-cell-center-down-fixed.png` | ⚠️ (送り方を `CADisplayLink` の毎フレーム補間に変えたことは deviation 記録済み) |
| 一方向の着地 / アニメーションなしの命令は即座に最終位置へ移る | `iSC:280-295` | `iUI:816` | ✅ |
| SwiftUI / 明示 ID で Cell を指せる | `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:204-208`、`iRes:92-119` | `iDSL:269` | ✅ |
| SwiftUI / ForEach の key で Section を指せる | `iRes:101-119` | `iDSL:281` | ✅ |
| SwiftUI / 同じ値が明示 ID と key の両方にあるときは明示 ID の要素を採る | `iRes:113-119` | `iDSL:293` | ✅ |
| SwiftUI / 状態の変更と同じ処理から出した命令は新しいツリーで解決される | `iRes:64-88` | `iDSL:323` | ✅ |
| SwiftUI / Store 方式では Store の Cell ID で指せる | `KsSettingsView.swift:221-294` | `iDSL:479` | ✅ |
| 控える・戻す / 控えた位置を新しい Host で戻す | `iSC:42-80`、`iSC:460-494`、`ios/Sources/KsSettingsViewUI/KsScrollAnchor.swift` | `iUI:866` | ✅ |
| 控える・戻す / 控えた後に上に項目が増えても同じ要素へ戻る | `iSC:463-464` | `iUI:880` | ✅ |
| 控える・戻す / 控えた要素が消えていたら何もしない | `iSC:382` | `iUI:895` | ✅ |
| 控える・戻す / 戻した後に出した命令が最終位置を決める | `iSC:78-80`、`iSC:130-141` | `iUI:909` | ✅ |

Scenario 外の SHALL は次のとおり実装とテストがある。

- 位置の既定 start・アニメーションの既定 true: `KsScrollControlling.swift:41-80`。テストは `iUI:187`
- 置き換え時と存在しない ID の debug 警告: `KsScrollController.swift:49-55`、`iSC:235-242`、`iRes:121-128`
- 未知の値への DSL の命令は何もしない: `iDSL:307`
- window から外れた後の `captureScrollAnchor()` は `nil` を返す: ⚠️ deviation 記録済み

### settings-view-android-ui (35 件)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ハンドルの公開 / 未接続のハンドルへの命令は何も起こさない | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsScrollController.kt:63-65` | `aUI:100` | ✅ |
| ハンドルの公開 / interface 型の変数からも同じ命令を出せる | `.../ui/KsScrollControlling.kt:11-54` | `aUI:113` | ✅ |
| ハンドルの公開 / rememberScrollController は再コンポジションで同じハンドルを返す | `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/RememberScrollController.kt:17` | `aC:141` | ✅ |
| ハンドルの公開 / debug ビルドでメインスレッド以外からの命令を検出する | `KsScrollController.kt:66-72` | `aUI:124` | ⚠️ (「debug ビルド」を `KsCellRegistry.strictMode` で判定すること、strictMode でないときは post して実行することは deviation 記録済み。テストは `aUI:143`・`aUI:157`) |
| 接続 / 最後に接続した Host だけが命令を受ける | `KsScrollController.kt:80-92`、`aV:323-333` | `aUI:190` (置き換えの警告ログも確認) | ✅ |
| 接続 / 接続を外したハンドルの命令は何も起こさない | `aV:323-333` | `aUI:210` | ✅ |
| 接続 / unbind で接続が外れ、bind し直した後は再代入で命令できる | `aV:692-693` | `aUI:223` | ✅ |
| 接続 / ハンドルは Host の解放を妨げない | `KsScrollController.kt:36-37` (WeakReference) | `aRel:42` | ✅ |
| 反映の後 / Cell の追加と同じ処理で出した末尾への命令が新しい末尾へ届く | `aSC:94-136`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:150-156` | `aUI:270` | ✅ |
| 反映の後 / 反映を続けて提出しても命令は最後の反映の後に実行される | `KsSettingsListAdapter.kt:36-51` (世代) | `aUI:285` | ✅ |
| 反映の後 / 続けて出した命令は最後の命令の位置で止まる | `aSC:130-136`、`aSC:159` | `aUI:304` | ✅ |
| 反映の後 / 実行前に対象が削除された命令は飛ばされ、次の命令は実行される | `aSC:156-157`、`aSC:207-212` | `aUI:317` (削除後に解決された警告も確認) | ✅ |
| 反映の後 / 画面に取り付ける前に出した命令は初回の表示の後に実行される | `aSC:119-127`、`aV:591-598` | `aUI:353` | ✅ |
| Cell の位置 / 表示範囲の外の Cell を中央に合わせる | `aSC:286-305` | `aUI:387` | ✅ |
| Cell の位置 / 末尾付近の Cell を上端に合わせようとすると端で止まる | `aSC:324-337` | `aUI:411` | ✅ |
| Cell の位置 / 非表示の Cell と存在しない ID への命令は位置を変えない | `aSC:207-219`、`aSC:252-255` | `aUI:424` | ✅ |
| Section / Start で見出しが表示範囲の上端に来る | `aSC:221-234` | `aUI:447` | ✅ |
| Section / 見出しの無い Section は最初の Cell が上端に来る | `aSC:230-234` | `aUI:459` | ✅ |
| Section / 非表示の Section と表示される行の無い Section への命令は位置を変えない | `aSC:230-232` | `aUI:511` | ✅ |
| 両端 / 末尾への命令で Root Footer の下端まで見える | `aSC:240-244`、`aSC:274` | `aUI:528` (Store 経由の Root Footer は `aUI:562`) | ✅ |
| 両端 / 先頭への命令で Root Header の上端まで戻る | `aSC:236-239`、`aSC:273` | `aUI:543` | ✅ |
| 一方向の着地 / 中央・末尾合わせのアニメーションが行き過ぎて戻らない | `aSC:161-171`、`aSC:484-513` | `aUI:578` (Center)・`aUI:594` (End)。実機: `evidence/android-native-frames-cell-center-down.png`・`evidence/android-native-frames-scroll-to-end.png` (移動量が負か 0 だけで着地) | ⚠️ (送り方を `smoothScrollBy` と解き直しの scroller に変えたことは deviation 記録済み) (注 2) |
| 一方向の着地 / アニメーションなしの命令は即座に最終位置へ移る | `aSC:173-194` | `aUI:664` | ✅ |
| Compose / 明示 ID で Cell を指せる | `aRes:110-136`、`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt:145-148` | `aC:179` | ✅ |
| Compose / forEach の key で Section を指せる | `aRes:119-136` | `aC:196` | ✅ |
| Compose / 同じ値が明示 ID と key の両方にあるときは明示 ID の要素を採る | `aRes:130-135` | `aC:218` | ✅ |
| Compose / 状態の変更と同じ処理から出した命令は新しいツリーで解決される | `aRes:83-104`、`KsSettingsViewComposable.kt:124-132` | `aC:243` | ✅ |
| Compose / Store 方式では Store の Cell ID で指せる | `KsSettingsViewComposable.kt:45-58`、`KsSettingsViewComposable.kt:193-211` | `aC:160` | ✅ |
| 控える・戻す / 控えた位置を新しい Host で戻す | `aV:611-624`、`aSC:202-205`、`aSC:377-396` | `aUI:709` | ✅ |
| 控える・戻す / 控えた後に上に項目が増えても同じ要素へ戻る | `aSC:426-441` | `aUI:721` | ✅ |
| 控える・戻す / 控えた要素が消えていたら何もしない | `aSC:203` | `aUI:735` | ✅ |
| 控える・戻す / 戻した後に出した命令が最終位置を決める | `aSC:94-136` | `aUI:747` | ✅ |
| 作り直し / Activity を作り直しても同じ要素が上端にかかる | `aV:1346-1378`、`aV:1553-1603` (SavedState) | `aRec:104`。実機: `evidence/android-native-recreate-verify-*.png` | ✅ (復元と `onCreate` 内の命令の順序は ⚠️ deviation 記録済み) |
| 作り直し / Compose の KsSettingsView でも Activity を作り直すと同じ要素が上端にかかる | `aV:545-558` (`scrollAnchorAtDetach`)、`KsSettingsViewComposable.kt:193-212` | `aCRec:195` (`NavHost` と同じ保存の持ち方は `aCRec:205`・`aCRec:217`) | ⚠️ (window から外れる直前の控えと、`NavHost` の別画面から戻ったときの位置の保持は deviation 記録済み・オーナー合意済み) |
| 作り直し / 既定 id の View Host が複数あるときは復元しない | `aV:1348-1389` | `aRec:118`、`aCRec:245` | ✅ |

Scenario 外の SHALL は次のとおり実装とテストがある。

- 位置の既定 Start・アニメーションの既定 true: `KsScrollControlling.kt:21-53`
- 存在しない ID の debug 警告: `aSC:252-255`、`aRes:138-141`
- Compose の `scrollController` 引数の既定 `null`: `KsSettingsViewComposable.kt:45`・`:89`
- `KsScrollAnchor` の `Parcelable`: `android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsScrollAnchor.kt`。テストは `aUI:795`

### maui-bridge (8 件)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Bridge API / Host 生成後の命令が Host をスクロールさせる | `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:443-485`、`android/kssettingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:532-575` | `iBr:162`、`aBr:196` (Section・両端は `iBr:177,189`・`aBr:212,222`) | ✅ |
| Bridge API / Host が無いときの命令は何も起こさない | 同上 | `iBr:225,240`、`aBr:258,272` | ✅ |
| Bridge API / 破棄後の命令は何も起こさない | 同上 | `iBr:260`、`aBr:290` | ✅ |
| Bridge API / C# の binding から命令を呼べる | `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs` (4 つの Export。selector は Swift の `@objc` 名と一致)、Android は自動束縛 | `maui/tests/shared/KsBridgeScenario.cs` (`ScrollToEnd(false)`)。実機: `evidence/integrationhost-ios-2-scroll-to-end.png`・`evidence/integrationhost-android-2-scroll-to-end.png` | ✅ |
| 位置の保持 / Host を作り直しても同じ要素が上端にかかる | Bridge の `releaseHost` / `makeHost*`、`ios/Sources/KsSettingsViewBridge/KsBridgeHostAnchorTracker.swift`、`android/.../bridge/KsBridgeHostAnchorTracker.kt` | `iBr:295`、`aBr:323` (window から外れた後に手放す順序は `iBr:313,335`・`aBr:338,355`)。実機: `evidence/maui-*-reconnect-verify-*.png` | ⚠️ (window から外れる直前の位置を控える形に改めたことは deviation 記録済み) |
| 位置の保持 / Host が無い間に上に項目が増えても同じ要素へ戻る | 同上 | `iBr:359`、`aBr:372` | ✅ |
| 位置の保持 / 作り直しの直後に出した命令が戻した位置より優先される | 同上 | `iBr:378`、`aBr:388` | ✅ |
| 位置の保持 / 控えは一度だけ使われる | 同上 | `iBr:394`、`aBr:409` | ✅ |

Scenario 外の SHALL は次のとおり実装とテストがある。

- 位置の整数 0/1/2 と範囲外の start: `ios/Sources/KsSettingsViewBridge/KsBridgeScrollPosition.swift`・`android/.../bridge/KsBridgeScrollPosition.kt` (付随修正として記録済み)。テストは `iBr:203,212`・`aBr:238,248`
- 控えられない Host を手放したときは控えを持たない: ⚠️ deviation 記録済み。テストは `iBr:421`・`aBr:435`

### maui-core (18 件)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| インターフェース / fake のインターフェース実装で ViewModel をテストできる | `maui/KsSettingsView.Maui/IScrollController.cs`、`maui/KsSettingsView.Maui/ScrollPosition.cs` | `mT:31` (既定の引数 Start / true も確認) | ✅ |
| ハンドル / Binding で ViewModel に実体が届く | `maui/KsSettingsView.Maui/SettingsView.cs` (`ScrollController`、OneWayToSource・defaultValueCreator) | `mT:63` (XAML の `{Binding Scroll}`)、`mT:82` | ✅ |
| ハンドル / SettingsView ごとに別の実体を持つ | 同上、`maui/KsSettingsView.Maui/Internals/KsSettingsScrollController.cs` | `mT:94` | ✅ |
| ハンドル / 外から代入した値は採用されない | 同上 | `mT:107` | ✅ (実体が SettingsView を弱参照で持つことは ⚠️ deviation 記録済み。テストは `mT:119`) |
| 明示 ID / 明示 ID を付けても表示は変わらない | `maui/KsSettingsView.Maui/Section.cs` (`SectionId`)、`maui/KsSettingsView.Maui/CellBase.cs` (`CellId`) | `mT:141` (Bridge への配信が同一)、`mT:154` | ✅ |
| 解決 / 手で並べた Cell を明示 ID で指せる | `maui/KsSettingsView.Maui/Internals/KsSettingsController.Scroll.cs` | `mT:171` (Section の明示 ID は `mT:197`) | ✅ |
| 解決 / ItemsSource の項目で生成した Cell を指せる | 同上、`maui/KsSettingsView.Maui/Internals/KsItemsSourceBinder.cs` | `mT:213` | ✅ |
| 解決 / ItemsSource の項目で生成した Section を指せる | 同上 | `mT:229` | ✅ |
| 解決 / 明示 ID と項目の両方に当たるときは明示 ID の要素を採る | 同上 | `mT:250` (表示順で最初の要素は `mT:264`) | ✅ |
| 解決 / 生成物の BindingContext を差し替えても元の項目で指せる | `KsItemsSourceBinder.cs` (生成元の並び) | `mT:278` | ✅ |
| 解決 / 項目の移動と置換の後も元の項目で指せる | 同上 | `mT:296` (追加・削除・作り直しは `mT:325`) | ✅ |
| 解決 / 解決できない対象は gateway を呼ばない | `KsSettingsController.Scroll.cs` | `mT:377` (英語の Debug 警告も確認) | ✅ (`null` の対象と、手で外した生成物の扱いは ⚠️ deviation 記録済み) |
| gateway / 先頭・末尾への命令が gateway に届く | `maui/KsSettingsView.Maui/Internals/IKsSettingsGateway.cs`、`maui/KsSettingsView.Maui/Platforms/{iOS,Android}/KsBridgeGateway.cs:160-171`、`maui/KsSettingsView.Maui/Internals/KsWireValues.cs` | `mT:405` (整数の写像は `mT:52`) | ✅ |
| gateway / Host の初回生成前の命令は何もしない | `KsSettingsController.Scroll.cs` | `mT:422` (解放中の命令が gateway へ渡ることは `mT:445`) | ✅ (未接続時の英語警告は ⚠️ deviation 記録済み) |
| 準備完了 / Host の生成と取り付けの後に合図が来る | `maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs` | `mT:467` | ✅ |
| 準備完了 / Host を作り直すたびに合図が来る | 同上 | `mT:500` (同じ Host では 1 回だけであることは `mT:484`) | ⚠️ (Host の世代ごとに 1 回・生成に失敗した世代では出さない扱いは deviation 記録済み。テストは `mT:563`) |
| 準備完了 / 実行できないコマンドは呼ばない | 同上 | `mT:519` | ✅ |
| 準備完了 / 合図の中で出した命令は戻した位置より後に実行される | 同上 + Bridge の戻しの順序 | `mT:542` (合図の命令は Host の取り付け後に gateway へ届く)、`iBr:378`・`aBr:388` (Bridge で戻しの後に命令が実行される) | ✅ (注 3) |

### samples-ios / samples-android / samples-maui (7 件)

実装を突き合わせた。3 platform で文言・Section 構成・デモデータが一致している。

- 実装: `samples/ios/KsSettingsViewSample/ScrollControlDemoView.swift`、`samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/ScrollControlDemoScreen.kt`、`samples/maui/KsSettingsView.Sample.Maui/Pages/ScrollControlDemoPage.xaml`・`ViewModels/ScrollControlDemoViewModel.cs`、各 `SampleScreen` のメニュー項目
- 一致を確かめた項目: 画面タイトル「スクロール制御デモ」、操作 4 つと「先頭へ」、Section 1〜5 × 項目 5 と Footer、「追加した項目 N」、Root Footer の文言
- 画面タイトル・対象・アニメーションの有無の決め方は ⚠️ deviation 記録済み。Android の Root Footer を Compose の `Text` で出す platform 差も ⚠️ deviation 記録済み

| Requirement / Scenario | 実機観測 (`evidence/`) | 状態 |
|---|---|---|
| samples-ios / 各操作で対象の位置へ移る | 修正後のビルド: 末尾へ `ios-native-fixed-2-scroll-to-end.png`、Cell を中央へ `ios-native-fixed-5-cell-to-center.png`・`ios-native-fixed-7-cell-to-center-from-end.png`、先頭へ `ios-native-fixed-3-scroll-to-start-noanim.png`。**Section へは修正前のビルドの `ios-native-4-section-to-start.png` だけで、修正後のビルドでの観測が無い** | ❌ (❌-1) |
| samples-ios / 追加して末尾へで追加した Cell が見える | `ios-native-fixed-6-add-to-end-{1,2,3}.png` | ✅ |
| samples-android / 各操作で対象の位置へ移る | `android-native-2-scroll-to-end.png`、`android-native-4-section-to-start.png`、`android-native-5-cell-to-center.png`・`android-native-7-cell-to-center-from-end.png`、`android-native-3-scroll-to-start-noanim.png` | ✅ (到着位置。連続フレームの未了は ❌-1 に含める) |
| samples-android / 追加して末尾へで追加した Cell が見える | `android-native-6-add-to-end-{1,2,3}.png`、`android-native-6-add-to-end-rapid-2-taps.png` | ✅ |
| samples-maui / 各操作で対象の位置へ移る | Android: `maui-android-{2,3,4,5}-*.png`。iOS (修正後のビルド): `maui-ios-fixed-{2,3,5}-*.png`。**iOS の Section へは修正前のビルドの `maui-ios-4-section-to-start.png` だけ** | ❌ (❌-1) |
| samples-maui / 追加して末尾へで追加した Cell が見える | `maui-android-6-add-to-end-verify-*.png`、`maui-ios-fixed-6-add-to-end-*.png` | ✅ |
| samples-maui / ViewModel が SettingsView の命令ハンドルを受け取る | 実装: `ScrollControlDemoPage.xaml` の `ScrollController="{Binding Scroll}"`・`ScrollControllerReadyCommand`、ViewModel は準備完了まで命令を出さない。実機: 準備完了の後にしか命令を出さない ViewModel で、各操作が到着している (`maui-*-2-scroll-to-end.png` ほか)。facade の同じ経路は `mT:63`・`mT:467` | ✅ |

### 注記

- **注 1** (iOS「実行前に対象が削除された命令」): `iUI:355` は、対象が表示から消えたことと、後続の末尾への命令が実行されたことをアサートしている。「Cell A の命令は何もしない」は最終位置からは区別できない。ただし、実行の直前に解決して対象が無ければ戻る実装 (`iSC:189-223`) で担保されている。Android の `aUI:317` は、削除後に解決された警告まで確かめている。THEN の観測可能な結果は満たしているので一致とした
- **注 2** (Android「高さの異なる行が混在」): テストデータは見出し・Footer の行と Cell 行の高さが異なる構成である (`ScrollControlTestSupport.kt:34` のコメント)。高さが異なること自体はアサートしていない。実機の連続フレームも同じ構成の Sample で取っている
- **注 3** (maui-core「合図の中で出した命令は戻した位置より後に実行される」): MAUI から Native Host までを通して最終位置を見るテストは無い。この Scenario は 2 つのテストの組み合わせで担保されている。1 つ目は `mT:542` で、Handler が Host を作る時点 (Bridge の `makeHost*` が戻しを待ち行列に積む時点) では命令がまだ無く、取り付けの後の合図で `ScrollToStart(false)` が gateway に届くことを確かめる。2 つ目は Bridge の `iBr:378`・`aBr:388` で、`makeHost*` の後に出した命令が戻しより後に実行され、最終位置が内容の先頭になることを確かめる。`evidence/maui-*-reconnect-verify-*.png` は位置の戻しを示すものである。Sample の合図は準備完了を控えるだけで命令を出さないため、合図の中の命令が最終位置を決めることの実機観測にはならない。実装の順序 (`maui/KsSettingsView.Maui/Handlers/SettingsViewHandler.cs` で Host 生成の後に取り付けの合図) は Scenario と一致しているので、一致とした

## ❌ の一覧と見立て

| # | 内容 | 該当 | 見立て |
|---|---|---|---|
| ❌-1 | **tasks.md 6.3 が未完了**。3 platform の Sample の実機観測のうち、次が残っている。(a) Android の着地の連続フレーム 3 か所: MAUI Android の「末尾へ」「Cell を中央へ」の下向き、Android Native の上向き中央合わせ。到着位置の静止画は取得済み。(b) iOS の「Section へ」の、修正後のビルド (`CADisplayLink` による一方向の着地、deviation 記録済み) での撮り直し。Native と MAUI iOS の両方で、修正後の到着の静止画も無い | tasks 6.3。Scenario: samples-ios「各操作で対象の位置へ移る」、samples-maui「各操作で対象の位置へ移る」(iOS 側の Section へ)。(a) は Scenario 単位では一致とした観測の補強 (settings-view-android-ui の一方向の着地は、テストと Native の下向きの連続フレームで対応済み) | **実装の修正は不要。未了の観測を完了させる**のが妥当。(b) の修正は、アニメーションの送り方だけを変えた。Section の位置の解決 (`iSC:417-453`) は変えていない。修正後のテスト (`iUI:478` ほか) も通っている。同じ修正後のビルドで、Cell・末尾・先頭の到着は観測済みである。そのため、撮り直しで乖離が出る可能性は低い。撮影環境の都合 (Mac の画面ロック) で進行中の既知の未了で、deviation として合意する性質のものではない。観測が済んで 6.3 にチェックが付けば、verify-002 では `evidence/` の追加分と tasks.md の差分を確かめるだけでよい。撮り直しで一方向でない着地や到着位置のずれが見つかった場合は、実装の修正に戻る |

## 申し送り (判定に含めない)

- 長命層 (decisions / handbook / concepts) への反映は、proposal.md の「蒸留時に反映」の行と deviation.md の「蒸留時に反映 (handbook)」の行による蒸留時の申し送りである。未反映は不一致としていない
- iOS のテスト実行と Swift 6 言語モードの確認は、制約によりホスト側の報告に依っている。本検証で確かめたのは、テストを含むパッケージ全体のビルドの成功だけである
