# Verify 001: add-maui-accessory-views

- 検証日: 2026-08-12
- 対象: 未コミット working tree 全体 (branch `claude/maui-accessory-views-ba7a2e`, base `develop` @ 713ab07)
- 検証対象デルタスペック: `specs/maui-core/spec.md` (7 Requirement / 19 Scenario) / `specs/maui-bridge/spec.md` (3 Requirement / 6 Scenario) / `specs/samples-maui/spec.md` (2 Requirement / 3 Scenario)
- 合意済み差分: `deviation.md` の 4 件を「違反として扱わない」前提で判定した

## 判定

**VALID**

全 28 Scenario が「✅ 一致」または「⚠️ deviation 記録済み」。❌ は 0 件。虚偽チェックなし、足場の逆流なし、テスト全件成功。

---

## 対応表: maui-core

パスは `maui/KsSettingsView.Maui/`、テストは `maui/KsSettingsView.Maui.Tests/` を基点とする
(ネイティブ側は `ios/` / `android/`、E2E は `verification-screenshots.md` の記録)。

### Requirement: Root / Section の Header・Footer に任意 View を設定できる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Section.HeaderView の設定で View が表示される | `Section.cs:48-68` (`HeaderViewProperty` / `FooterViewProperty`) → `Internals/KsSettingsController.cs:1043-1052` → `SetAccessoryView` (`:410`) → `DeliverAccessory` (`:514`) | `AccessoryViewTests.cs:33` `ViewIsDeliveredAsAccessoryView` (4 対象 TestCaseSource) | ✅ |
| RootHeaderView の設定で View が表示される | `SettingsView.cs:61-80` (`RootHeaderViewProperty` / `RootFooterViewProperty`) → `KsSettingsController.cs:331` `SetRootAccessoryView` | 同上 (`RootHeader` / `RootFooter` ケース) | ✅ |
| null 設定でクリアされる | `KsSettingsController.cs:410-435` (view null 時は `_accessories` から除去し `UpdateAccessory(text)` を配信) | `AccessoryViewTests.cs:66` `NullViewWithoutTextClearsAccessory` (4 対象) / `:761` `ClearingTheViewRemovesItFromTheLogicalTree` | ✅ |

> 「4 対象すべてに適用」の SHALL は `AccessoryViewTests.cs:21-27` の `Targets` 配列によるマトリクスで担保
> (`TestCaseSource(nameof(Targets))` を付けたテストは 14 本 × 4 対象)。

### Requirement: text と view の併存は View 優先で解決される

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 両方設定時は View が表示される | `KsSettingsController.cs:514-532` `DeliverAccessory` (実体があれば view のみ配信) / `:1073-1085` `DeliverSectionAccessoryText` と `:309-326` `SetRootAccessoryText` (view 配置中は text を配信しない) | `AccessoryViewTests.cs:110` `ViewTakesPrecedenceOverText` (4 対象) / `:82` `TextIsNotDeliveredWhileViewIsPlaced` (4 対象) | ✅ |
| View を null に戻すと text へフォールバックする | `KsSettingsController.cs:531` (`AccessoryTextOf` を配信) | `AccessoryViewTests.cs:49` `NullViewFallsBackToText` (4 対象) | ✅ |
| View 表示中の text 変更は保持され解除後に反映される | `KsSettingsController.cs:309-326` (root は `_rootHeaderText` / `_rootFooterText` に控える) / `:628-634` `AccessoryTextOf` (Section は Section の状態を都度読む) | `AccessoryViewTests.cs:95` `TextChangedWhileViewIsPlacedIsAppliedAfterRelease` (4 対象) | ✅ |

### Requirement: accessory View は所有者の BindingContext を継承する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Root accessory は SettingsView の BindingContext を継承する | `Internals/KsAccessoryViewOwnership.cs:43-55` `Reassign` / `KsSettingsController.cs:423`・`:439` `OwnerOf` | `AccessoryViewTests.cs:224` `RootAccessoryInheritsTheViewBindingContext` | ✅ |
| Section accessory は所有 Section の BindingContext を継承する | 同上 (`OwnerOf` が `slot.Section` を優先) / `Section.cs:53-56`・`:65-68` (`ReassignIfFree`) | `AccessoryViewTests.cs:238` `SectionAccessoryInheritsTheOwningSectionBindingContext` (ItemsSource + ItemTemplate 生成 Section で item を継承することを確認) | ✅ |
| BindingContext の変更が accessory View へ伝播する | `KsAccessoryViewOwnership.cs:52-54` (`AddLogicalChild` により論理ツリーの継承に載る) | `AccessoryViewTests.cs:260` `BindingContextChangePropagatesToTheAccessory` / `:725` `BindingContextChangeReachesTheAccessoryWhileTheHostIsReleased` | ✅ |
| View の明示的な BindingContext は継承で上書きされない | `KsAccessoryViewOwnership.cs:53` (`SetInheritedBindingContext` = MAUI 標準の継承規則) | `AccessoryViewTests.cs:275` `ExplicitBindingContextIsNotOverwritten` | ✅ |

> ⚠️ 論理所有を BindableProperty の寿命へ切り出した設計変更は **deviation 3 件目 (2026-08-12)** に記録済み。
> tasks 3.4 の記述 (wrapper 生成の一部として `AddLogicalChild`) との差はこの合意に含まれる。
> 併せて `AccessoryViewTests.cs:703` `AccessoryInheritsTheBindingContextWithoutAHost` /
> `:746` `AccessoryStaysInTheLogicalTreeWhileTheHostIsReleased` が Host 非依存の契約を固定している。

### Requirement: View の差し替えと内容変化が表示に反映される

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 新しいインスタンスへの差し替え | `KsSettingsController.cs:410-435` (Store 更新 → native 配信 → 旧 lease 破棄の順序) / `Platforms/iOS/KsViewMaterializer.cs:34-40` (`RemoveFromSuperview` → `DisconnectHandlers`) / `Platforms/Android/KsViewMaterializer.cs` | `AccessoryViewTests.cs:126` `ReplacingViewReissuesTheAccessory` (4 対象) / `:143` `PreviousViewIsDisposedAfterTheNewOneIsDelivered` (4 対象) / `:161` `PreviousViewIsDisposedAfterTheClearIsDelivered` (4 対象)。ネイティブ側は `ios/Tests/KsSettingsViewUITests/AccessoryViewDetachDiagnosticTests.swift` (6 本) / `android/.../AccessoryViewSwapProbeTest.kt` (5 本) | ✅ |
| 同一インスタンスの内容変化が反映される | live な MAUI View がそのまま描き替える構造 (`Materialize` は差し替え時のみ。`KsSettingsController.cs:480-491`) | `AccessoryViewTests.cs:181` の末尾アサーション (`UpdateAccessoryView` を送り直さない) / E2E `e2e-*-02-grow.png` / サンプル項目 (2) `sample-*-02-bind-and-text-fallback.png` | ✅ |
| サイズが変わる内容変化に領域高さが追従する | `Internals/IKsViewMaterializer.cs:21` (`measureInvalidated` コールバック) → `KsSettingsController.cs:615-624` `OnAccessoryMeasureInvalidated` → flush で `InvalidateAccessoryMeasurement` (`:1361`/`:1410`)。native: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の対象限定 `invalidateLayout(with:)` + `SettingsRootStore.swift` の一過性通知 / `android/.../KsSettingsView.kt` の `requestLayout` | `AccessoryViewTests.cs:181` `MeasureInvalidationIsCoalescedIntoOneRequest` (4 対象) / `:205` `MeasureInvalidationOfARemovedViewIsIgnored` (4 対象)。native: `AccessoryMeasureInvalidationTests.swift` (7 本) / `AccessoryMeasureInvalidationTest.kt` (6 本) / `AccessoryViewLiveProbeTests.swift` (6 本) / `AccessoryViewLiveResizeProbeTest.kt` (1 本)。E2E `e2e-*-02-grow.png` | ⚠️ deviation 記録済み |

> ⚠️ **deviation 1 件目 (2026-08-11)**: 高さ再計算のために native へ再計算口 (`invalidateAccessoryMeasurement`) を
> 追加する案 A をスコープに含める裁定。本 change の `ios/` / `android/` 差分 (Store・Controller・Bridge) はこの合意の範囲。
> **deviation 2 件目 (2026-08-11)**: iOS の旧 view 剥がしに「native 配信後に platform view を superview から外す」
> ステップを追加する扱い。`Platforms/iOS/KsViewMaterializer.cs:39` の `RemoveFromSuperview()` がこれに当たる。

### Requirement: Handler 切断・再接続をまたいで view accessory は保持される

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 再訪問で view accessory が復元される | `KsSettingsController.cs:256-264` `ReleaseHost` → `ReleaseAccessoryViews` (`:542`) / `:288-303` `ApplyAccessories` (Root 2 slot + 全 Section の 2 slot を再実体化) / `Handlers/SettingsViewHandler.cs:120` `view.ApplyAccessories()` (Loaded 後) | `AccessoryViewTests.cs:542` `AccessoryViewIsRebuiltForANewHost` (4 対象) / `:683` `ReattachingTheHostKeepsTheNewAccessoryHandlerConnected` / `:500` `ViewIsOwnedBeforeItIsRematerializedForANewHost` (4 対象)。E2E `e2e-*-07-reconnect.png` | ✅ |
| 切断中の変更が再接続後に反映される | `KsSettingsController.cs:480-485` (実体化の口が無い間は置き場所だけ確定) → 再接続後 `ApplyAccessories` で実体化・配信 | `AccessoryViewTests.cs:586` `ViewPlacedWhileDetachedIsAppliedOnReattach` (4 対象) / `:568` `SectionAccessoryIsWrittenBackAsTextWhenTheHostIsReleased`。E2E: MenuPage「離脱中に Root Header View を差し替え」→ `e2e-*-07-reconnect.png` | ✅ |

### Requirement: 同一 View インスタンスの多重配置は例外になる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一インスタンスを2箇所へ設定すると例外 | `KsSettingsController.cs:1822-1830` `EnsureAccessoryViewIsNotPlaced` (プロパティ経路) / `:1726-1764` `EnsureTreeHasNoDuplicates` + `:1783-1796` `EnsureSectionsAreNotPlaced` (変換経路参加時) / `:1843` `DuplicatePlacement` → `InvalidOperationException` | `AccessoryViewTests.cs:291` `PlacingTheSameViewTwiceThrows` / `:302` `PlacingTheSameViewInHeaderAndFooterThrows` / `:313` `AddingASectionThatReusesAPlacedViewThrows` / `:345` `GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt` / `:374`・`:396`・`:413` 既存配置の無傷性 / `:460` `DuplicatePlacementIsDetectedWithoutAGateway` | ⚠️ deviation 記録済み |
| null 解除後の再利用は許容される | `KsSettingsController.cs:415-421` (`_placedViews` から除去) / `KsAccessoryViewOwnership.cs:70-79` `ReassignIfFree` | `AccessoryViewTests.cs:523` `ViewCanBePlacedAgainAfterItIsReleased` / `:440` `UnregisteredSectionClaimsAViewThatWasReleased` | ✅ |

> ⚠️ **deviation 4 件目 (2026-08-12)**: 未接続 Section の fallback 経路で「既配置 View は所有を確定しない」ガードを
> 追加し、例外のタイミングを変換経路参加時 (Host 未接続なら Host 接続時) に倒す裁定。
> spec の「既存の Section / CellBase 検出と同一の契約」に対する明示的な差分としてこの合意でカバーされる。

### Requirement: HeaderHeight と view accessory の相互作用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| HeaderHeight 正値で固定高さになる | facade は `Section.HeaderHeight` を既存経路 (`KsBridgeSection.headerHeight`) で輸送するのみ。固定高さの clip は native 側の既存契約 (先行 change align-view-accessory-header-height で OS 対称化済み) | native: `AccessoryMeasureInvalidationTests.swift:135` `固定高さのheaderは再計測要求で変化しない` / `KsBridgeAccessoryViewTest.kt:445` `固定高さの header は再計測要求で変化しない`。E2E `e2e-*-03-headerheight-fixed.png` / サンプル項目 (6) `sample-ios-04-bottom.png`・`sample-android-05-bottom.png` | ✅ |
| 未指定なら内容の自動高さになる | 同上 (未指定時は native の Section 既定 = 自動高さ。`KsBridgeSection.kt:79` / `KsBridgeSection.swift:96`) | E2E `e2e-*-03b-headerheight-auto-restored.png` (再実行記録) / 上記の測り直し系テスト群 | ✅ |

> 本 Requirement は facade に新しい分岐を持たないため net10.0 のユニットテストに専用ケースはない。
> 実挙動は native ユニットテストと両 OS の E2E スクリーンショットで固定されている。

---

## 対応表: maui-bridge

### Requirement: updateAccessoryView で view accessory を更新できる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| section header へ view を設定すると表示される | `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:294-323` / `android/.../KsSettingsBridge.kt:354-390` (`KsBridgeAccessoryView.anyView` で `KsAnyView` 化し Store の既存 accessory 更新経路へ) | iOS `KsBridgeAccessoryViewTests.swift:41`・`:60`・`:76` / Android `KsBridgeAccessoryViewTest.kt:62`・`:80`・`:98` | ✅ |
| null で view accessory がクリアされる | 同上 (`anyView` を `nil`/`null` にすると `accessory: nil` を渡す) | iOS `:91` `nilで解除される` / Android `:123` `null で解除される` | ✅ |
| 未知の sectionID は no-op | `KsBridgeIdentifier.uuid(from:)` / `KsBridgeIdentifier.canonical()` の失敗で早期 return、Store 側も未知 ID は no-op | iOS `:118` 未使用 sectionID / `:147` 不正 sectionID / `:162` 破棄後 no-op。Android `:149` 未使用 / `:184` 非 canonical / `:200` 破棄後 | ✅ |

### Requirement: KsBridgeSection は headerView / footerView を輸送する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| setRoot で view accessory 付き Section が表示される | `ios/.../KsBridgeSection.swift:33-42`・`:96-97` / `android/.../KsBridgeSection.kt:30-43`・`:77-78` (`KsBridgeAccessoryView.sectionAccessory(view:text:)` が view 優先で解決) | iOS `:180` / Android `:219` | ✅ |
| replaceSection でも view accessory が輸送される | 同上 (`makeSection(id:relay:)` を共有) | iOS `:199` / Android `:239` | ✅ |
| (view 優先の SHALL) | `KsBridgeAccessoryView.sectionAccessory` (view があれば `.view`、無ければ text) | iOS `:215` `textとviewの両指定はviewが優先される` / Android `:256` | ✅ |

### Requirement: 同一 view インスタンスの再バインドが安全である

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| リサイクルを挟んだ再表示が失敗しない | `ios/Sources/KsSettingsViewBridge/KsBridgeAccessoryView.swift` (`KsAnyView.uiKit { view.removeFromSuperview(); return view }`) / `android/.../KsBridgeAccessoryView.kt` (親からの detach 付き factory) | iOS `KsBridgeAccessoryViewTests.swift:237` / Android `KsBridgeAccessoryViewTest.kt:280`・`:318` `Host 作り直しでも同一 view が再表示される`。背景実証として `AccessoryViewSwapProbeTest.kt:199` (detach なし factory は `IllegalStateException`) | ✅ |

---

## 対応表: samples-maui

### Requirement: AccessoryViewsDemoPage を MAUI 固有区分に追加する

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 一覧の MAUI 固有区分から遷移できる | `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs` (`SampleScreenCategory.MauiSpecific` 追加 / 区分名「MAUI 固有」/ `All` へ「Header / Footer への View 配置デモ」1 件追加。cross/ADR-0016 のパリティ例外を doc comment に明記) / `Pages/AccessoryViewsDemoPage.xaml` (タイトルは `SampleScreen` の一元定義から与えるため XAML では設定しない = 文言一致が構造的に保証される) | 目視: `verification-screenshots.md` 「一覧」行 / `sample-ios-00-menu.png`・`sample-android-00-menu.png` | ✅ |

### Requirement: AccessoryViewsDemoPage は本 change の公開挙動を確認できる

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 7項目が1ページで確認できる | `Pages/AccessoryViewsDemoPage.xaml` に ①〜⑦ の区画 (① Root Header/Footer View / ② Section Header・Footer View + バインド / ③ text 併存と View 優先・フォールバック / ④ 別インスタンス差し替え / ⑤ 行数切替による高さ追従 / ⑥ `HeaderHeight = 44` の切り詰め / ⑦ 切断・再接続) + `Pages/AccessoryViewsDemoPage.xaml.cs` / `ViewModels/AccessoryViewsDemoViewModel.cs` | 目視: `verification-screenshots.md` の 7 項目表 (両 OS 分のスクリーンショット 8 枚ずつ) | ✅ |
| 復元確認は同一インスタンスで行われる | `AccessoryViewsDemoPage.xaml.cs:82-100` `OnReconnectTapped` — 同一 Page インスタンスを `PopAsync` → `PushAsync` で押し直し、`Settings.Handler is null` を読んで切断を記録し、離脱中に `RootHeaderView` を差し替える。`InstanceText` (`LabelCell「このページ」`) でインスタンス同一性を画面表示 | 目視: `verification-screenshots.md` 項目 (7) の読み方 3 点 / `sample-*-06-reconnect-top.png`・`sample-*-07-reconnect-bottom.png` | ✅ |

> spec は「子ページの push → pop、または保持した同一 Page インスタンスへの再遷移など」を許容しており、
> 実装の pop → push は後者に当たる。

---

## 追加検査

### tasks.md の完了状況と虚偽チェック

全 17 タスクが `[x]`。対応表と突き合わせた結果、**未実装のままチェックされているタスクはない**。

| タスク | 実体の確認 |
|---|---|
| 1.1 / 1.2 (先行検証) | 検証結果が probe テストとして残存 (`AccessoryViewLiveProbeTests.swift` 6 本 / `AccessoryViewLiveResizeProbeTest.kt` / `AccessoryViewDetachDiagnosticTests.swift` 6 本 / `AccessoryViewSwapProbeTest.kt` 5 本)。裁定は deviation 1・2 件目に記録済み |
| 2.1〜2.5 | 両 OS Bridge に `updateAccessoryView` / `invalidateAccessoryMeasurement` / `KsBridgeSection.headerView`・`footerView`、Binding は `ApiDefinition.cs:638-647`・`:1011-1024`、テストは iOS 14 本 / Android 17 本 |
| 3.1〜3.4 | `IKsViewMaterializer` / `IKsViewLease` / `IKsAccessoryViewStore` の seam、`Platforms/iOS/KsAccessoryHostView.cs` (`MauiView` + `ICrossPlatformLayout`)、`Platforms/Android/KsAccessoryHostView.cs`、`ConnectGateway` への注入 (`SettingsView.cs:799-830`) |
| 4.1〜4.6 | 上記対応表のとおり全経路が実装済み。4.5 の `ApplyRootAccessory` は `ApplyAccessories` に改名のうえ拡張 |
| 5.1 / 5.2 | `SampleScreen.cs` の `MauiSpecific` 区分 + `AccessoryViewsDemoPage` 一式 |
| 6.1 | `AccessoryViewTests.cs` 860 行 / 41 メソッド (うち 14 本が 4 対象マトリクス) |
| 6.2 | `LeakTests.cs:66-141` に差し替え / null 化 / Section 削除 / Root 再構築 / Host 解放 / root accessory の 6 経路 |
| 6.3 / 7.1 / 7.2 | `maui/tests/KsSettingsView.MauiHost/SettingsPage.xaml`(.cs) に 4 対象配置と操作ボタン、`MenuPage.cs` に離脱中差し替え。記録は `verification-screenshots.md` (初回 + 2026-08-12 再実行) とスクリーンショット 35 枚 |

### 逆流検査 (足場の凍結)

- `proposal.md` / `design.md` / `specs/**` は最終コミット `212678e` (提案作成時) 以降 **未変更** (`git diff HEAD` が空)
- working tree で変更されている kasane 配下は `tasks.md` (進捗チェック) と新規追加の `deviation.md` / `review-*.md` / `second-opinion-code-*.md` / `verification-screenshots.md` / `screenshots/` のみ
- 逆流なし ✅

### 未記録乖離

対応表に ❌ はないため、**未記録の乖離は 0 件**。

### テスト実行結果 (本検証で再実行)

| スイート | コマンド | 結果 |
|---|---|---|
| MAUI net10.0 | `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj -f net10.0` | **342 passed / 0 failed** |
| Android | `./gradlew :ks-settingsview-bridge:testDebugUnitTest :ks-settingsview-ui:testDebugUnitTest` (BUILD SUCCESSFUL) | 全モジュール test-results 集計 **2280 tests / 0 failures** |
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<iPhone 17 Pro>'` | **TEST SUCCEEDED** / **750 tests / 0 failures** (Bridge 115 + Core 83 + SwiftUI 76 + UI 476)。新規 4 スイート (`KsBridgeAccessoryViewTests` / `AccessoryMeasureInvalidationTests` / `AccessoryViewLiveProbeTests` / `AccessoryViewDetachDiagnosticTests`) の実行をログ上で確認 |
| E2E (両 OS) | `maui/tests/KsSettingsView.MauiHost` の目視 | `verification-screenshots.md` の 2026-08-12 再実行記録で全項目 green |

### orchestrator 依頼の追加確認: doc comment 3 箇所の実挙動一致

| 箇所 | 記述 | 実装との照合 | 判定 |
|---|---|---|---|
| `maui/KsSettingsView.Maui/Internals/KsAccessoryViewOwnership.cs:23-27` (型 remarks) | 「引き取らなかった配置は、所有者が変換経路に加わった時点 (Native Host 未接続のまま設定ツリーへ入った場合は Host 接続時) で多重配置として弾かれる」 | 変換経路参加時 = `HandleSectionsChanged` → `EnsureTreeHasNoDuplicates`(`:862`) + `EnsureSectionsAreNotPlaced`(`:863`)。Host 未接続時は `OnObservedCollectionChanged` が `_gateway is null` で早期 return (`:357`) し `RebuildRoot` も `:706` で早期 return するため検査が走らず、`Connect` (`:177`) からの `RebuildRoot` → `EnsureTreeHasNoDuplicates`(`:712`) が Host 接続時の検査点になる | ✅ 一致 |
| 同 `:60-66` (`ReassignIfFree` remarks) | 同趣旨 + 「他所に所有されている View は引き取らずに置く (外す方の後始末だけは行う)」 | `KsAccessoryViewOwnership.cs:72-76` — `newView.Parent` が非 null かつ owner 以外なら `Detach(owner, oldView)` のみ実行して return。`Reassign` を呼ばないため `AddLogicalChild` / `SetInheritedBindingContext` は走らない | ✅ 一致 |
| `maui/KsSettingsView.Maui/Section.cs:40-47` (`HeaderViewProperty` remarks) | 「変換経路に載っている間は変換経路が先に所有を確定させる」「受け皿には検査を行う相手がいないため既に他所へ置かれている View は引き取らない (この Section が変換経路に加わった時点 — Native Host 未接続のまま設定ツリーへ入れた場合は Host 接続時 — で例外になる)」 | 前段: 登録済み Section では `HandleSectionPropertyChanged` (`:1043-1052`) → `SetAccessoryView` → `EnsureAccessoryViewIsNotPlaced`(`:412`) → `Reassign`(`:423`) が先に走り、続く `propertyChanged` の `ReassignIfFree` は `newView.Parent == owner` により no-op (`KsAccessoryViewOwnership.cs:47-50`)。後段: 上表と同じ検査点 | ✅ 一致 |

対応するテストによる裏づけ: `AccessoryViewTests.cs:313` `AddingASectionThatReusesAPlacedViewThrows` (Section 組み立ては例外にならず、`Root.Add` の時点で例外。既存配置の `Parent` / `BindingContext` / lease / 輸送はいずれも無傷)、`:345` `GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt`、`:413` `FailedPlacementLeavesTheExistingPlacementIntactWithoutAHost`、`:460` `DuplicatePlacementIsDetectedWithoutAGateway`。

---

## 所見 (判定に影響しないもの)

1. **deviation.md の件数**: コンテキストパッケージでは「5 件」と伝えられたが、ファイル上の項目は **4 件** (2026-08-11 × 2 / 2026-08-12 × 2)。4 件で本 change の全乖離をカバーできており未記録乖離は検出されなかったため判定には影響しないが、5 件目を記録したつもりの合意が漏れていないか蒸留前に確認しておくとよい。
2. **`maui-core` R7 (HeaderHeight) に facade 層の専用テストがない**: 当該挙動は native 側の既存契約 (先行 change で OS 対称化済み) に完全委譲されており、native ユニットテストと両 OS の E2E で固定されている。spec の SHALL を満たす証跡としては十分と判断した。
3. **iOS の `swift test` (macOS 実行) は Core の 88 テストのみ**: UIKit 依存の Bridge / UI テストは iOS Simulator 宛の `xcodebuild test` でしか走らない (750 テストの内訳のうち 667 がシミュレータ限定)。再検証時はシミュレータ destination の指定が必須。
