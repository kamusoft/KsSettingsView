# 一致検証: release-host-without-bridge-dispose (001 回目)

**日付**: 2026-08-08
**判定**: VALID

対象デルタスペック: `specs/maui-bridge/spec.md` (ADDED 1 Requirement / 7 Scenario、MODIFIED 2 Requirement / 6 Scenario)
deviation.md: 存在しない (記録済みの合意乖離なし)

## 対応表

### ADDED — Requirement: Host の単独解放

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 解放後の再生成は Store 現在状態を復元する | `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:56-75` / `android/ks-settingsview-bridge/src/main/kotlin/jp/kamusoft/kssettingsview/bridge/KsSettingsBridge.kt:68-91` | iOS `KsBridgeHostReleaseTests.test_解放後の再生成はStore現在状態を復元する` / Android `KsBridgeHostTest.解放後の再生成は Store 現在状態を復元する` | ✅ 一致 |
| 解放中の更新は再生成時に反映される | 同上 (`makeHost*` が接続時に Store 現在状態から復元) | iOS `KsBridgeHostReleaseTests.test_解放中の更新は再生成時に反映される` / Android `KsBridgeHostTest.解放中の更新は再生成時に反映される` (replaceCell / updateAccessory(sectionHeader) / setTheme を検証) | ✅ 一致 (注1) |
| 解放後の Store 更新は旧 handle に反映されない | `KsSettingsBridge.swift:71-75` → `KsSettingsViewController.disconnectStore()` (`KsSettingsViewController.swift:310-318`) / `KsSettingsBridge.kt:86-91` → `KsSettingsView.unbind()` (`KsSettingsView.kt:329-333`) | iOS `KsBridgeHostReleaseTests.test_解放後のStore更新は旧handleに反映されない` / Android `KsBridgeLifecycleTest.解放後の Store 更新は旧 handle に反映されない` / UI 層単体 iOS `StoreDisconnectionTests` 2 件・Android `StoreUnbindTest` 3 件 | ✅ 一致 |
| releaseHost は冪等で Store を維持する | `KsSettingsBridge.swift:72` (`guard ... let controller = hostController`) / `KsSettingsBridge.kt:87-88` (`hostView ?: return`) | iOS `KsBridgeHostReleaseTests.test_releaseHostは冪等でStoreを維持する` (3 回連続 → root と Theme の復元まで assert) / Android `KsBridgeLifecycleTest.releaseHost は冪等で Store を維持する` | ✅ 一致 |
| dispose 後の releaseHost は no-op | `KsSettingsBridge.swift:72` (`guard !isDisposed`) / `KsSettingsBridge.kt:87` (`if (isDisposed) return`) | iOS `KsBridgeHostReleaseTests.test_dispose後のreleaseHostはno_op` / Android `KsBridgeLifecycleTest.dispose 後の releaseHost は no-op` | ✅ 一致 |
| Android は解放後に別の Context で再生成できる | `KsSettingsBridge.kt:68-75` (`Context` は引数受け取りのみ、フィールド保持なし) | Android `KsBridgeHostTest.解放後は別の Context で再生成できる` (`ContextThemeWrapper` を渡し `assertSame` で確認) | ✅ 一致 |
| 解放後、Bridge は旧 Host への参照を保持しない | `KsSettingsBridge.swift:74` (`hostController = nil`) / `KsSettingsBridge.kt:90` (`hostView = null`)。Android は `unbind()` が `pendingStore` も手放す | iOS `KsBridgeHostReleaseTests.test_解放後に旧Hostへの参照を保持しない` (weak + autoreleasepool + runloop) / Android `KsBridgeLifecycleTest.解放後に旧 Host への参照を保持しない` (`WeakReference` で Host と `Context` の両方、`System.gc()` ループで回収待ち) | ✅ 一致 |

「Host 不在時 (未生成) の no-op」は Requirement 本文の一部で独立 Scenario を持たないが、iOS `test_Host未生成でのreleaseHostはno_op` / Android `Host 未生成での releaseHost は no-op` で追加検証されている。

### MODIFIED — Requirement: Native Host の生成と接続

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Host 生成 → setRoot の順で表示される | `KsSettingsBridge.swift:56-62` / `KsSettingsBridge.kt:68-75` (変更前から不変) | iOS `KsBridgeHostTests.test_Host生成後のsetRootが表示へ反映される` / Android `KsBridgeHostTest.Host 生成後の setRoot が表示へ反映される` | ✅ 一致 |
| setRoot → Host 生成の順でも表示される | 同上 | iOS `KsBridgeHostTests.test_setRoot後に生成したHostが現在状態を復元する` / Android `KsBridgeHostTest.setRoot 後に生成した Host が現在状態を復元する` | ✅ 一致 |
| 生きている Host がある間は同じ handle を返す | `KsSettingsBridge.swift:58` (`if let hostController { return hostController }`) / `KsSettingsBridge.kt:70` (`hostView?.let { return it }`) | iOS `KsBridgeHostTests.test_makeHostViewController_は同じHostを返す` / Android `KsBridgeHostTest.makeHostView は同じ Host を返す` | ✅ 一致 |

変更後全文の「解放後の `makeHost*` は新しい handle を返す」は、上表 ADDED の「解放後の再生成は…」テストで別インスタンスであることを assert 済み (`XCTAssertFalse(first.controller === second.controller)` / `assertNotSame`)。旧契約 (「再呼び出しは常に同じ handle」) を前提とした古いテストは残っていない。

### MODIFIED — Requirement: .NET binding からの呼び出し

| Scenario | 実装 | テスト / 証跡 | 状態 |
|---|---|---|---|
| C# からの参照とビルド | iOS `maui/macios/KsSettingsView.Binding.iOS/ApiDefinition.cs:346-353` (`[Export("releaseHost")] void ReleaseHost()`)、Android は自動生成 (`maui/android/KsSettingsView.Binding.Android/obj/Debug/net10.0-android/generated/type-mapping.txt` に `releaseHost()V = ReleaseHost()`) | `dotnet build maui/KsSettingsView.slnx` で Binding.iOS / Binding.Android / IntegrationHost.Android がビルド成功。IntegrationHost.iOS のみ当環境の Xcode 26.5 と .NET for iOS 26.1 の版差ゲートで失敗 (コード起因ではない — 下記「追加検査」参照) | ✅ 一致 |
| C# からの実行時疎通 | `maui/tests/shared/KsBridgeScenario.cs` `Apply` / `maui/tests/KsSettingsView.IntegrationHost.iOS/AppDelegate.cs` / `.Android/MainActivity.cs` | 証跡 `evidence/ios-01-before-release.png` / `evidence/android-01-before-release.png` | ✅ 一致 |
| C# からの解放と再生成 | `AppDelegate.ReleaseAndRecreateHost()` / `MainActivity.ReleaseAndRecreateHost()` + `KsBridgeScenario.ApplyWhileReleased` (`maui/tests/shared/KsBridgeScenario.cs`) | 証跡 `evidence/ios-02-after-recreate.png` / `evidence/android-02-after-recreate.png` — 「解放中に更新」「Français / 解放中に説明を追加」「通知設定 (解放中に更新)」および Theme のオレンジ化が再生成後の表示に出ている | ✅ 一致 |

## 追加検査

- **tasks.md の虚偽チェック**: なし。全 9 タスク (1.1 / 1.2 / 2.1 / 2.2 / 2.3 / 3.1 / 3.2 / 3.3 / 4.1 / 4.2 + 実装前ゲート) に対応する実装・テスト・証跡が上表で特定できた。`tasks.md` の diff はチェックボックスのみで本文の書き換えはない
- **逆流検査**: なし。`git status` 上、変更のある足場アーティファクトは `tasks.md` (チェックボックスのみ) だけ。`proposal.md` / `specs/maui-bridge/spec.md` / `exploration.md` / `second-opinion-001.md` は未変更。実装期間中のコミットも存在しない (全変更が未コミット)
- **未記録乖離**: ❌ が 0 件のため、deviation.md に記録すべき未記録の欠落・乖離はない
- **UI 変更**: 本変更に `ui/` アーティファクトはない (Bridge の公開 API 追加であり、承認モックを要する UI 変更ではない)。E2E 証跡はタスク 4.2 の合否基準どおり両 OS 分揃っている
- **テスト全件成功**:
  - iOS: `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` → **Executed 439 tests, with 0 failures**
  - Android: `cd android && ./gradlew test --rerun-tasks` → `build/test-results/**/TEST-*.xml` 集計で **total=2008 / failures=0 / errors=0 / skipped=0** (debug + release の 2 variant 合算)
  - 新規テストが実際に実行されていることを個別に確認済み (`KsBridgeHostReleaseTests` 7 件 / `StoreDisconnectionTests` 2 件 / `KsBridgeHostTest` 7 件中 3 件が新規 / `KsBridgeLifecycleTest` 8 件中 5 件が新規 / `StoreUnbindTest` 3 件)

## 注記

**注1 — root header / footer は「Store 現在状態」の外側にある**

Scenario「解放中の更新は再生成時に反映される」の WHEN は `replaceCell` / `updateAccessory` / `setTheme` を挙げ、THEN は「Store 現在状態になる」と定めている。`updateAccessory` のうち **root header / footer は両 OS とも `SettingsRoot` 値型に含まれない UI 層プロパティ**であり (`ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:270-284` / `android/.../SettingsRootStore.kt:238-242`)、Store 状態を変えず Diff の発行だけを行う。したがって再生成した Host には復元されない — 解放中の更新が届かないだけでなく、解放前に表示されていた root H/F ごと消える (証跡 `*-02-after-recreate.png` で確認できる)。

THEN が「Store 現在状態」に限定されている以上、これは **spec 違反ではない** ため ❌ とはせず ✅ とした。ただし ADR-0007 の狙い (再接続をまたいだ表示内容の保持) に対する穴であり、記録が `maui/tests/shared/KsBridgeScenario.cs` の doc コメントに閉じている。扱いの決定は `review-001.md` の Major 指摘として上げてある。

## 判定

**VALID** — 全 13 Scenario が「✅ 一致」。❌ 0 件、虚偽チェックなし、逆流なし、テスト全件成功。

アーカイブ可能な状態だが、注1 の扱い (root H/F の消失をどう記録・処理するか) は蒸留前にオーナー裁定を通すことを推奨する。
