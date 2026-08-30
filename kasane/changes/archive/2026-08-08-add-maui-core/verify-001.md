# Verify 001: add-maui-core

- **判定**: **VALID**
- **検証日**: 2026-08-08
- **対象デルタスペック**: `kasane/changes/add-maui-core/specs/maui-core/spec.md` (ADDED Requirements 15件 / Scenario 29件)
- **実装**: `maui/KsSettingsView.Maui/`
- **テスト**: `maui/KsSettingsView.Maui.Tests/` — `dotnet test -f net10.0` **115件 成功 / 0件 失敗** (実行して確認)
- **E2E 検証ホスト**: `maui/tests/KsSettingsView.MauiHost/` (`SettingsPage.xaml` / `MenuPage.cs`)

凡例: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離
「E2E」= 実機/シミュレータでの確認が一次証拠である行 (単体テストは補助的に周辺を固定)。

---

## 1. Requirement / Scenario 対応表

### R1. 公開コンテナ形状

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| XAML 直置き | `SettingsView.cs:18` `[ContentProperty(nameof(Root))]` / `Section.cs:20` `[ContentProperty(nameof(Cells))]` / `SettingsView.cs:26` `Root` defaultValueCreator / `Section.cs:42` `Cells` defaultValueCreator | `SettingsViewShapeTests.ContentPropertyIsRoot` / `.RootDefaultsToObservableSettingsRoot` / `.RootDefaultIsNotSharedBetweenInstances`、`SectionShapeTests.ContentPropertyIsCells` / `.CellsDefaultsToObservableCollection` / `.CellsDefaultIsNotSharedBetweenInstances`、`BindingContextTests.BindingContextReachesDirectlyPlacedSectionAndCell`。XAML 実パースは **E2E** (`SettingsPage.xaml` で SettingsView > Section > LabelCell を直置き) | ✅ |
| Root の差し替え | `SettingsView.cs:31-37` (propertyChanged → `SetRootCollection` + binder 再配線) / `KsSettingsController.cs:167` `SetRootCollection` → `:212` `RebuildRoot` → `:233` `SubscribeSections` (旧購読 `Unsubscribe`) | `ConversionPathTests.ReplacingRootRebuildsAndDropsOldCollection` (新コレクションで setRoot 再構築 / 旧コレクション操作は無配信 / 新コレクションは追随) | ✅ |

Requirement 本文の追加条項 (`Cells` 差し替えも同様に旧購読解除+再構築): `KsSettingsController.cs:536` `nameof(Section.Cells)` → `:542` `RebuildSectionCells` (`:557-566` で旧 Cell の登録解除と再購読)。テスト `ConversionPathTests.ReplacingCellsIssuesReplaceSectionAndKeepsSectionId` / `.ReplacingCellsDropsOldCollection`。✅

### R2. UI スレッド契約

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| UI スレッド上の操作 | marshal コードが存在しないことが実装。`MainThread` / `BeginInvokeOnMainThread` / `InvokeOnMainThread` / `lock` は `maui/KsSettingsView.Maui/` 配下に**0件** (grep 確認)。契約は公開 doc コメントに明記 (`SettingsView.cs:12-17`、`Section.cs:12-19`、`CellBase.cs:9-13`、`KsSettingsController.cs:15`、`IKsSettingsGateway.cs:12`) | 全ユニットテスト (115件) が UI スレッド相当の単一スレッド上でコレクション操作・プロパティ変更を行い反映を確認 | ✅ |

補足: `KsMauiDispatcher.cs:16` の `IDispatcher.Dispatch` は R8 (バッチ flush の予約) が要求する**同一スレッド内の遅延**であり、スレッド marshal ではない。SHALL NOT に抵触しない。

### R3. Handler 接続時の表示反映

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 初回表示 | `Handlers/SettingsViewHandler.cs:65` `CreatePlatformView` → `Platforms/iOS/SettingsViewHandler.cs:19` / `Platforms/Android/SettingsViewHandler.cs:20` `CreateHost` → `SettingsView.cs:177` `ConnectGateway` → `KsSettingsController.cs:84` `Connect` → `:212` `RebuildRoot` (`SetRoot` 1回)。Section accessory は setRoot の DTO に同梱 (`Platforms/iOS/KsBridgeGateway.cs:34`)、root accessory は attach 後に `:110` `OnHostAttached` → `ApplyRootAccessory` | `ConversionPathTests.ConnectSendsWholeTreeAsSetRoot` / `.ConnectRegistersGatewayAssignedIds` / `.StructureBuiltBeforeConnectIsSentOnlyBySetRoot`、`HandlerTests.ConnectingHandlerCreatesHost`。native 表示の目視は **E2E** | ✅ |

### R4. Handler 切断と再接続の復元

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 再接続で最新状態を復元 | `SettingsViewHandler.cs:90` `DisconnectHandler` → `SettingsView.cs:202` `ReleaseHost` (gateway と購読は維持)。`SettingsView.cs:182` により再接続時は既存 gateway を再利用し setRoot を再送しない — 状態は Bridge の Store が保持 | `HandlerTests.DisconnectingHandlerReleasesHost` / `.ReconnectingHandlerKeepsTheConnectedGateway` / `.ConnectGatewayReusesTheFirstGateway`。切断中変更の復元表示は **E2E** (`MenuPage.cs` の「離脱中に ValueText を更新」「離脱中に Cell を追加」→ 再訪問) | ✅ |
| root accessory の再適用 | `KsSettingsController.cs:43-44` で所有値を保持 / `:131` `ApplyRootAccessory` / `SettingsViewHandler.cs:80,84,110-120` (`Loaded` 経由と取り付け済み即時の両経路) | `AccessoryTests.RootAccessoryIsReappliedAfterHostRelease` / `.RootAccessorySetBeforeConnectIsAppliedOnApply`、`HandlerTests.AttachConfirmsContainmentBeforeApplyingRootAccessory`。両 OS の表示は **E2E** | ✅ |
| iOS の親子関係 | `Platforms/iOS/KsHostContainment.cs:22` `AddToParent` (`AddChildViewController`) / `:38` `ConfirmAdded` (`DidMoveToParentViewController`) / `:50` `Remove` (`WillMoveToParentViewController(null)` → `RemoveFromSuperview` → `RemoveFromParentViewController`)。親 VC 解決は `Platforms/iOS/SettingsViewHandler.cs:43-87`。切断時の参照破棄は `SettingsViewHandler.cs:95-97` (`Containment = null`) | 順序の固定は `HandlerTests.HostIsAddedToParentBeforeThePlatformViewIsHandedOver` / `.AttachConfirmsContainmentBeforeApplyingRootAccessory` / `.DisconnectRemovesContainmentBeforeReleasingHost` (共通部の呼び出し順)、参照残存なしは `LeakTests.HandlerAndHostAreCollectedAfterDisconnect`。UIKit 実体の親子成立は **E2E** | ✅ |

注記 (品質観点・判定には影響しない): `KsHostContainment` の UIKit 実呼び出しは net10.0 のテストからは検証できず、回帰の自動検出手段がない。review-002 で既出の Minor 指摘。親 VC 解決方式の design との差は deviation.md 記録済み。

### R5. 静的コレクションの描画

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| List への追加は反映されない | `KsSettingsController.cs:238` (`_root is not INotifyCollectionChanged` なら購読しない) / `:741` (Cells 側も同様) | `ConversionPathTests.StaticRootCollectionIsNotObserved` / `.StaticCellsCollectionIsNotObserved` | ✅ |

### R6. 構造変更の反映

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Section の追加 | `KsSettingsController.cs:275` `HandleSectionsChanged` → `:309` `AddSections` (`InsertSection`) | `ConversionPathTests.AddingSectionIssuesInsertSection` | ✅ |
| Cell の移動 | `:385` `HandleCellsChanged` → `:498` `MoveCells` (`MoveCell`) / 範囲移動の分解は `KsRangeMove.cs` | `ConversionPathTests.MovingCellIssuesMoveCell` / `.ForwardRangeCellMoveKeepsOrder` / `.BackwardRangeCellMoveKeepsOrder` | ✅ |
| Clear で再構築 | `:291-293` (Reset → `RebuildRoot`)。Cells の Reset も `:406-408` で `RebuildRoot` | `ConversionPathTests.ClearingRootRebuildsWithSetRoot` / `.ClearingCellsRebuildsWithSetRoot` | ⚠️ |

⚠️ の理由: Cells の Reset を `replaceSection` ではなく `setRoot` 全再構築で配信する判断は spec が Root についてのみ記述しているため、deviation.md「Cells の Reset の配信形」に記録済み (合意済み差分)。Root の Reset 自体は spec 文言どおりで一致。

Requirement 本文の Add / Remove / Move / Replace 全網羅:

| 操作 | 実装 | テスト | 状態 |
|---|---|---|---|
| Section Remove | `:327` `RemoveSections` | `ConversionPathTests.RemovingSectionIssuesRemoveSection` | ✅ |
| Section Move | `:301` `MoveSections` | `.MovingSectionIssuesMoveSection` / `.ForwardRangeSectionMoveKeepsOrder` / `.BackwardRangeSectionMoveKeepsOrder` | ✅ |
| Section Replace | `:342` `ReplaceSections` | `.ReplacingSectionIssuesReplaceSectionAndKeepsId` | ✅ |
| Cell Add | `:412` `AddCells` | `.AddingCellIssuesInsertCell` | ✅ |
| Cell Remove | `:431` `RemoveCells` | `.RemovingCellIssuesRemoveCell` | ✅ |
| Cell Replace | `:447` `ReplaceCells` | `.ReplacingCellIssuesReplaceCellAndKeepsId` | ✅ |

### R7. 同一インスタンスの重複配置の禁止

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一 Cell の二重追加 | `KsSettingsController.cs:815` `EnsureCellsAreNotPlaced` / `:802` `EnsureSectionsAreNotPlaced` / `:753` `EnsureTreeHasNoDuplicates` / `:826` `DuplicatePlacement` | `ConversionPathTests.AddingCellPlacedElsewhereThrowsWithoutTouchingDisplay` (例外 + 無配信)、`.AddingSectionPlacedElsewhereThrowsWithoutTouchingDisplay`、`.ConnectingTreeWithDuplicateCellThrows`、`.FailedConnectRollsBackAndAllowsReconnect` | ✅ |

Requirement 本文の追加条項「ItemsSource のテンプレートが既配置のインスタンスを返した場合も同様」: 生成物は通常の構造操作として `Cells` へ挿入されるため同じ検出経路を通る (`KsItemsSourceBinder.cs:111-126`)。テスト `ItemsSourceTests.TemplateCreatingAlreadyPlacedCellThrows`。✅

例外送出後にモデル側コレクションへ要素が残る点は deviation.md「重複配置例外の後状態」に記録済み (spec は「表示は変化しない」のみ要求し、その点は満たしている)。

### R8. Cell 内容更新のバッチ配信

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 複数 Cell の変更が 1 バッチになる | `KsSettingsController.cs:581` `HandleCellPropertyChanged` (dirty 登録) / `:603` `ScheduleFlush` (`_flushScheduled` で 1回だけ予約) / `:621` `Flush` → `:660` `ReplaceCells` | `BatchDeliveryTests.MultipleContentChangesAreDeliveredAsOneBatch` / `.BatchBoundaryIsTheFirstScheduledFlush` / `.SingleContentChangeIsDeliveredAsReplaceCell` | ✅ |
| 複数 Cell の可視性変更 | `:588-591` (`IsVisible` を `_visibilityDirtyCells` へ分離) / `:644-647` (単発 `ReplaceCell`) | `BatchDeliveryTests.VisibilityChangesAreDeliveredIndividually` / `.VisibilityChangeIsSplitFromContentBatchAndSentFirst` / `.CellWithVisibilityAndContentChangeIsSentOnceAsReplaceCell` | ✅ |
| 保留中に削除された Cell の更新は安全に破棄される | `:722` `UnregisterCell` (dirty からも除去) / `:639` (flush 直前に対応表を引き直して skip) | `BatchDeliveryTests.PendingUpdateForRemovedCellIsDropped` (例外なし + 残りのみ配信) | ✅ |

### R9. 削除済み要素からの通知遮断

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 削除済み Section の HeaderText 変更 | `KsSettingsController.cs:695` `UnregisterSection` (購読の同期解除) / `:519-527` (対応表の生存ガード。`updateAccessory` は未知 ID の no-op 契約外のため二重防御) | `RemovedElementNotificationTests.RemovedSectionHeaderTextChangeIsSilent` / `.RemovedSectionFooterTextChangeIsSilent` / `.ReplacedSectionAccessoryChangeIsSilent` / `.SectionRemovedByClearIsSilent` / `.RemovedCellPropertyChangeIsSilent` / `.CellOfRemovedSectionIsSilent` / `.CellDroppedByCellsReplacementIsSilent` | ✅ |

### R10. Root header / footer テキスト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 設定と反映 | `SettingsView.cs:40-57` (`RootHeaderText` / `RootFooterText`) → `KsSettingsController.cs:145` `SetRootAccessoryText` | `AccessoryTests.RootHeaderTextIsDeliveredAsRootAccessory`、`SettingsViewShapeTests.RootAccessoryTextCanBeSetAndCleared` / `.RootAccessoryTextDefaultsToNull` | ✅ |
| null でクリア | `:156` (`text` を null のまま `UpdateAccessory` へ通す) | `AccessoryTests.RootFooterTextNullClearsRootAccessory`、`.RootAccessorySetBeforeConnectIsNotDeliveredImmediately` | ✅ |

### R11. Section header / footer テキスト

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 表示中の変更 | `Section.cs:24-35` (対称対) / `KsSettingsController.cs:530-535` (`SectionHeader` / `SectionFooter` の `UpdateAccessory`) | `AccessoryTests.SectionFooterTextIsDeliveredWithGatewayId` / `.SectionHeaderTextNullClearsSectionAccessory`、`SectionShapeTests.HeaderAndFooterTextCanBeSetAndCleared` | ✅ |

### R12. CellBase / LabelCell の公開プロパティ

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ValueText の反映 | `LabelCell.cs:12` `ValueTextProperty` / `:26` `CreateSnapshot` / `:30` `AffectsSnapshot` | `BatchDeliveryTests.SingleContentChangeIsDeliveredAsReplaceCell` (ValueText 変更が配信される)、`CellShapeTests.SnapshotCarriesPublicProperties` / `.SnapshotOfBaseCellHasNoValueText` | ✅ |
| IsVisible の反映 | `CellBase.cs:45` `IsVisibleProperty` / `KsSettingsController.cs:588-591,644` | `BatchDeliveryTests.VisibilityChangesAreDeliveredIndividually` | ✅ |

Requirement 本文の公開面 (`Title` / `Description` / `HintText` / `IsEnabled` / `IsVisible` + LabelCell の `ValueText`、輸送可能な範囲に限る): `CellBase.cs:17-49` に 5件、`LabelCell.cs:12` に 1件のみ宣言。輸送は `CellBase.cs:93` `CreateSnapshot` / `:110` `AffectsSnapshot` の列挙と 1:1。テスト `CellShapeTests.CellDefaultsToEnabledAndVisible` / `.SnapshotCarriesPublicProperties` / `.SnapshotResolvesNullTitleToEmpty`、`BatchDeliveryTests.ChangeOfNonTransportedPropertyIsNotDelivered`。✅

- `Title` の null 解決 (既定 `string.Empty`・null は空文字として輸送) は deviation.md 記録済み。
- `Description` / `HintText` / `IsEnabled` の「変更 → 配信」を単独で確かめるテストはなく、`AffectsSnapshot` の列挙テストと snapshot テストの組み合わせで担保している (テスト粒度の所見。spec 一致としては充足)。
- 基底型を `Element` としたことによる継承プロパティの露出は deviation.md「CellBase / Section の基底型」記録済み。

### R13. ItemsSource / ItemTemplate による生成

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| items から Cell を生成 | `Section.cs:55-79` (3プロパティ) → `KsItemsSourceBinder.cs:111` `Generate` / `:321` `Create` (`CreateContent` + `BindingContext = item`) | `ItemsSourceTests.CellsAreGeneratedWithItemAsBindingContext`、`BindingContextTests.GeneratedCellKeepsItsItemAsBindingContext` | ✅ |
| Template の後付け | `KsItemsSourceBinder.cs:113` (`_itemTemplate is null` の間は生成しない) / `:55` `SetItemTemplate` → `:105` `Regenerate` | `ItemsSourceTests.TemplateSetAfterItemsSourceGeneratesThroughConversionPath` (生成が `InsertCell` 経路を通ることも確認) | ✅ |
| item 追加のミラー | `:78` `OnObservedCollectionChanged` → `:146` `MirrorAdd` | `ItemsSourceTests.ItemAddIsMirrored` / `.ItemInsertAtHeadIsMirrored` | ✅ |
| 生成区間へ手動挿入後の Reset | `:27` `_generated` (provenance) / `:99-101` (Reset → `Regenerate`) / `:128` `RemoveGenerated` (生成物のみ除去) | `ItemsSourceTests.ResetKeepsManuallyInsertedCell` / `.ResetWithNewItemsKeepsManuallyInsertedCell` | ✅ |
| ItemsSource の null 化 | `:37` `SetItemsSource(null)` → `Regenerate` → 生成物のみ除去 | `ItemsSourceTests.NullItemsSourceKeepsManualCells` / `.ItemsAreNotMirroredAfterItemsSourceIsCleared` | ✅ |

Requirement 本文の箇条書き 5件と「Section 生成側も同一パターン」「生成物は通常の構造変更と同じ経路」:

| 条項 | 実装 | テスト | 状態 |
|---|---|---|---|
| ItemTemplate 未設定の間は生成しない | `KsItemsSourceBinder.cs:113` | `.TemplateSetAfterItemsSourceGeneratesThroughConversionPath` | ✅ |
| ItemTemplate / TemplateStartIndex の表示中変更で再生成 | `:55` / `:63` → `:105` `Regenerate` | `.ChangingTemplateWhileConnectedRegenerates` / `.ChangingTemplateStartIndexMovesGeneratedRange` / `.TemplateStartIndexPlacesGeneratedAfterManualCells` | ✅ |
| Add / Remove / Replace / Move / Reset のミラー | `:146` / `:164` / `:185` / `:227` / `:99` | `.ItemAddIsMirrored` / `.ItemRemoveIsMirrored` / `.ItemReplaceIsMirrored` / `.ItemMoveIsMirrored` / 範囲 Move 4件 | ✅ |
| 期待型以外の生成で InvalidOperationException | `:321-328` | `.TemplateCreatingWrongTypeForCellsThrows` / `.TemplateCreatingWrongTypeForSectionsThrows` | ✅ |
| SettingsView 直下の Section 生成 (同一パターン) | `SettingsView.cs:60-84,93` | `.SectionsAreGeneratedWithItemAsBindingContext` / `.ForwardRangeItemMoveKeepsGeneratedSectionOrder` / `.BackwardRangeItemMoveKeepsGeneratedSectionOrder` | ✅ |
| 生成物は通常の構造変更と同じ経路で反映 | 生成先 `IList` への `Insert` / `Remove` として実施 (`:122`,`:139`) | `.TemplateSetAfterItemsSourceGeneratesThroughConversionPath` (InsertCell 3件)、`.NullItemsSourceKeepsManualCells` (RemoveCell 3件)、`.ReplacingCellsRegeneratesIntoNewCollection` | ✅ |

`DataTemplateSelector` 非対応は deviation.md 記録済み (spec 未記述領域)。

### R14. Handler 登録

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 登録して利用 | `MauiAppBuilderExtensions.cs:21-29` (`AddHandler<SettingsView, SettingsViewHandler>()` 1件のみ) | `HandlerTests.AddKsSettingsViewRegistersOnlyTheSettingsViewHandler` (SettingsView の Handler が解決でき、`LabelCell` / `Section` の Handler 型が null であることを確認)。XAML 配置ページの実表示は **E2E** (`MauiProgram.cs` + `SettingsPage.xaml`) | ✅ |

### R15. 切断後の資源回収

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 切断後の回収 | `SettingsViewHandler.cs:90-100` `DisconnectHandler` (`Loaded` 解除 → `Containment` 破棄 → `ReleaseHost`) / `KsWeakCollectionSubscription.cs` / `KsWeakPropertySubscription.cs` (弱参照 proxy 購読) | `LeakTests.HandlerAndHostAreCollectedAfterDisconnect`、`.GcProbeFailsWhenReferenceSurvives` (ヘルパ自体の検出力を担保) | ✅ |
| 外部保持があっても facade は回収される | `KsItemsSourceBinder.cs:43-48` / weak 購読一式 (親への強参照を作らない。`Element.Parent` を使わない方式) | `LeakTests.FacadeAndGatewayAreCollectedWhileExternalHoldsModel` | ✅ |

`Parent` 配線を採らず `SetInheritedBindingContext` で BindingContext を配る方式は deviation.md 記録済み (この SHALL を守るための選択)。

---

## 2. 追加検査

### 2.1 tasks.md の虚偽チェック

全 24 タスク (1.1〜7.7) がチェック済み。対応表と突き合わせた結果、**未実装なのにチェック済みの項目はなし**。

- 1.1〜1.3: `maui/KsSettingsView.Maui/KsSettingsView.Maui.csproj`、`maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj`、`maui/KsSettingsView.slnx` に 2プロジェクト登録済み (確認)
- 2.1〜2.4: `Internals/IKsSettingsGateway.cs` (`setTheme` は含まれない — 非ゴールどおり)、`Platforms/iOS/KsBridgeGateway.cs`、`Platforms/Android/KsBridgeGateway.cs`、`Tests/Fakes/FakeSettingsGateway.cs` / `FakeDispatcher.cs` (fake 自体のテストも存在)
- 4.4 の「エコー抑止フックの口」: `KsSettingsController.cs:672` `ShouldPublish` として実在
- 7.7 (E2E): 検証ホスト `maui/tests/KsSettingsView.MauiHost/` に XAML 直置きページ・離脱中更新の導線 (`MenuPage.cs`) が実在。実機/シミュレータでの確認結果はオーケストレーターの報告に依拠する (本検証で再実行はしていない)

### 2.2 逆流検査 (足場の書き換え)

- この変更一式は未コミット (`kasane/changes/add-maui-core/` は untracked、`maui/KsSettingsView.Maui*` も untracked/未ステージ) のため、git 履歴による差分検査は不能。代替として mtime を確認した。
- 足場: `specs/maui-core/spec.md` 18:09、`design.md` 18:08、`proposal.md` 18:10
- 実装: 最古 `SettingsRoot.cs` 18:49 〜 最新 `KsSettingsController.cs` 20:37
- **実装開始 (18:49) 以降に足場が更新された形跡はなし。逆流なし。**
- 実装期間中に更新されているのは `tasks.md` (20:03、進捗記録)・`deviation.md` (20:58)・レビュー証跡のみで、いずれも凍結対象外。

### 2.3 未記録乖離

対応表に ❌ なし。⚠️ 2件 (Cells の Reset 配信形、重複配置例外の後状態) はいずれも deviation.md に記録済み。deviation.md の 7件はすべて実装と一致していることを確認した (基底型 `Element` / Title の null 解決 / BindingContext 配布方式 / Cells の Reset / 重複例外の後状態 / DataTemplateSelector 非対応 / iOS 親 VC 解決方式)。**未記録の乖離は検出されなかった。**

### 2.4 UI アーティファクト

本変更に `ui/` は存在しない。デルタスペックは観察可能な状態遷移のみを記述しており、px 値・色・レイアウト配置・特定コントロール名への言及はない (UI lint 上の問題なし)。native の見た目は既存 Bridge / native 実装の責務であり本変更のスコープ外。

### 2.5 テスト実行

```
dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj -f net10.0
成功!  失敗: 0、合格: 115、スキップ: 0、合計: 115
```

---

## 3. 判定

**VALID**

- 全 15 Requirement / 29 Scenario に実装とテストの対応が存在し、❌ (未記録の欠落・乖離) は 0件
- ⚠️ 2件はいずれも deviation.md に記録済みの合意済み差分
- tasks.md に虚偽のチェックなし
- 足場アーティファクトへの逆流なし
- テスト 115件 全件成功 (実行して確認)

### 呼び出し元への申し送り (判定には影響しない所見)

1. E2E でのみ確認済みの Scenario (初回表示・再訪問復元・root accessory 再適用・iOS 親子関係・XAML 直置きの実パース) は自動回帰検出の外にある。とくに `KsHostContainment` の UIKit 実呼び出しは net10.0 テストから到達不能 (review-002 で既出)。
2. `Description` / `HintText` / `IsEnabled` は「変更 → 配信」の単独テストを持たず、`AffectsSnapshot` 列挙テストと snapshot テストの組み合わせで担保している。
3. 変更一式が未コミットのため、逆流検査は mtime による代替判定である。コミット後に改めて履歴ベースで確認できると望ましい。
