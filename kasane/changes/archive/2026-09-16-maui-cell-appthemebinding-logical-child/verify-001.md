# Verify 001: maui-cell-appthemebinding-logical-child

- 判定: **VALID**
- 検証日: 2026-09-16
- 対象: 作業ツリーの未コミット変更すべて (`git status --short` の 46 エントリ)
- テスト: `dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` → 失敗 0 / 合格 557 / スキップ 0 / 合計 557 (本検証で実行。提示された客観的事実と一致)

凡例: ✅ 一致 / ⚠️ deviation 記録済み / ❌ 欠落・乖離

---

## 1. specs/maui-core — ADDED Requirements

### Requirement: Section / Cell は SettingsView の論理子である

主実装: `maui/KsSettingsView.Maui/Internals/KsLogicalChildOwnership.cs` (器全体)、
`maui/KsSettingsView.Maui/SettingsView.cs:39,493,916` (Section 側の器)、
`maui/KsSettingsView.Maui/Section.cs:138,178,179` (Cell 側の器)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| XAML 構築時 (Host 未接続) に Section と Cell が論理子になる | `KsLogicalChildOwnership.cs:148-168` (`Adopt`)、`SettingsView.cs:493`・`Section.cs:179` (構築子で `OnTargetChanged`) | `LogicalChildTests.SectionAndCellBecomeLogicalChildrenWithoutHost` | ✅ |
| Root から Section を除去すると Section と配下 Cell の論理子が解除される | `KsLogicalChildOwnership.cs:196-204` (`ReleaseIfDetached`)、`:230-238` (`Release`) | `LogicalChildTests.RemovingSectionFromRootReleasesOnlyThatSection` | ✅ |
| Section から Cell を除去すると論理子が解除される | 同上 | `LogicalChildTests.RemovingCellFromSectionReleasesIt` | ✅ |
| Root コレクションの差し替えで旧 Section の論理子が解除され新 Section が論理子になる | `KsLogicalChildOwnership.cs:53-71` (`OnTargetChanged`)、`SettingsView.cs:39` | `LogicalChildTests.ReplacingRootCollectionMovesOwnership` | ✅ |
| Reset で所属から外れた要素の論理子が解除され、再追加で再び論理子になる | `KsLogicalChildOwnership.cs:105-110` (Reset → `ReleaseAbsent` + `Apply`)、`:207-217` | `LogicalChildTests.ResetReleasesAllAndReaddingAdoptsAgain` | ✅ |
| Cells コレクションの差し替えで旧 Cell の論理子が解除され新 Cell が論理子になる | `KsLogicalChildOwnership.cs:53-71` (購読解除 → `ReleaseAll`)、`Section.cs:138` | `LogicalChildTests.ReplacingCellsCollectionMovesOwnershipAndStopsWatchingTheOldOne` (旧コレクションへの後追い Add も検証) | ✅ |
| Host 未接続で同じ Cell を 2 回追加して 1 件除去しても所属が残る間は論理子のまま | `KsLogicalChildOwnership.cs:196-204` (`ContainsReference` で所属の残りを判定) | `LogicalChildTests.CellAddedTwiceStaysAdoptedUntilEveryPlacementIsRemoved` | ✅ |
| ItemsSource の再生成で除去された Cell の論理子が解除される | `Section.cs:138` (器を `_cellBinder` より先に購読)、`KsLogicalChildOwnership.cs:103-138` | `LogicalChildTests.RegeneratingFromItemsSourceMovesOwnership` (`BindingContext` が item のままも検証) | ✅ |
| Host 接続中の除去でも論理子が解除される | `KsLogicalChildOwnership.cs:121` / 変換経路は現行のまま | `LogicalChildTests.RemovingCellWhileConnectedReleasesItAndStillDeliversTheStructureUpdate` (`RemoveCell` 1 件を併せて検証) | ✅ |

注 (実装の欠落ではない): Scenario 見出し「Root から Section を除去すると Section と**配下 Cell**の論理子が解除される」に対し、
同 Scenario の THEN は「配下 Cell の `Parent` は `section` のまま」。実装とテストは THEN に従っている。
見出しと THEN の言い回しの食い違いは spec 側の表記の問題で、契約 (THEN) との乖離はない。

### Requirement: 論理子化は既存の BindingContext 継承契約を変えない

実装: `KsLogicalChildOwnership.cs:160` (`SetInheritedBindingContext` を `AddLogicalChild` より先に呼ぶ。順序の理由は `:17-20` のコメント)、
`SettingsView.cs:937`・`Section.cs:330` (`OnBindingContextChanged` → `Apply`)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 直接並べた Cell に BindingContext が継承される | `KsLogicalChildOwnership.cs:160`、`SettingsView.cs:937` | `BindingContextTests.BindingContextReachesDirectlyPlacedSectionAndCell` / `.CellBindingResolvesAgainstTheViewBindingContext` / `.BindingContextReachesLaterAddedElements` / `.BindingContextReachesReplacedCollections` | ✅ |
| ItemsSource 生成 Cell は item を BindingContext として保つ | `Section.cs:138` (器 → `_cellBinder` の順)、`KsLogicalChildOwnership.cs:160` | `BindingContextTests.GeneratedCellKeepsItsItemAsBindingContext` | ✅ |

`BindingContextTests` 5 件は無改変のまま緑 (tasks 1.3 の確認条件)。

### Requirement: Section / Cell に設定した binding は外観変更と Resources の変更で再評価される

実装: 論理子化そのもの (`KsLogicalChildOwnership.cs:164` の `AddLogicalChild`) が祖先 (ページ / アプリ) への経路を作る。
解除は `:230-238`。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell の色プロパティの AppThemeBinding が外観変更で再評価される | `KsLogicalChildOwnership.cs:164` | `AppearanceBindingTests.CellColorFollowsTheAppearanceChange` | ✅ |
| Section のプロパティの AppThemeBinding が外観変更で再評価される | 同上 (`SettingsView.cs:916` の器) | `AppearanceBindingTests.SectionPropertyFollowsTheAppearanceChange` | ✅ |
| Cell の DynamicResource がページ Resources の差し替えで再評価される | 同上 | `AppearanceBindingTests.CellColorFollowsThePageResourceChange` | ✅ |
| Cell の DynamicResource がアプリ Resources の差し替えで再評価される | 同上 | `AppearanceBindingTests.CellColorFollowsTheApplicationResourceChange` | ✅ |
| 所属を解除された Cell は旧所属先の Resources 変更に追随しない | `KsLogicalChildOwnership.cs:230-238` (`RemoveLogicalChild`) | `AppearanceBindingTests.DetachedCellStopsFollowingTheOldPageResources` | ✅ |

端末外観の経路 (単体テストで `UserAppTheme` と区別できない) は
`evidence/appearance-switch-verification.md` §1 と `evidence/mauihost-{ios,android}-{light,dark}-{before,after}.png` で確認済み
(MauiHost の `SettingsPage.xaml:48` が `AppThemeBinding` 単体になり、購読コードは `SettingsPage.xaml.cs` から削除されている)。

非 observable コレクション経路は deviation 記録済みの追加挙動で、
`AppearanceBindingTests.CellAddedToPlainCellsListBeforeConnectFollowsTheAppearanceChange` が固定している (⚠️ deviation 記録済み・Scenario 外の追加)。

### Requirement: 再評価されたプロパティ値は通常のプロパティ変更と同じ経路で反映される

実装: 既存の反映経路に手を入れていない (プロパティ変更ハンドラは現行のまま)。論理子化により
binding の再評価が「通常のプロパティ変更」として同じ口へ入る。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell の色の再評価が内容更新として配信される | 現行の内容更新経路 (`Internals/KsSettingsController.cs` の `ReplaceCell` / `ReplaceCells` 経路。本 change で変更なし) | `AppearanceBindingTests.ReevaluatedCellColorIsDeliveredAsAContentUpdate` (届いた色まで検証し、`RemoveCell` / `InsertCell` が空であることを表明) | ✅ |
| Section の HeaderText の再評価が accessory 更新として配信される | 現行の accessory 更新経路 | `AppearanceBindingTests.ReevaluatedSectionHeaderTextIsDeliveredAsAnAccessoryUpdate` (`UpdateAccessory.Text` が dark 値) | ✅ |

構造プロパティ / 表示値でないプロパティが従来経路のままであることは、既存の `ConversionPathTests` / `ItemsSourceTests` が変更なしで緑であることで担保 (経路に手が入っていない)。

### Requirement: 論理子化は SettingsView の回収を妨げない

実装: `KsLogicalChildOwnership.cs:21-23` (コレクション購読は弱参照、子→親は MAUI 本体の `Parent` の弱保持に委ねる)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 外部が root を保持したまま SettingsView が回収される | `KsLogicalChildOwnership.cs:44,63-68` (`KsWeakCollectionSubscription`) | `LeakTests.FacadeAndGatewayAreCollectedWhileExternalHoldsModel` / `.FacadeAndGatewayAreCollectedWhileExternalHoldsCustomCell` (`LeakTests.cs:196-197` で論理子であることを回収前に表明) | ✅ |

tasks 2.4 の「論理子 (`Parent` あり) の状態で回収される」意図は、remarks と `LeakTests.cs:193-197` のアサートで明記されている。

---

## 2. specs/maui-core — MODIFIED Requirements

### Requirement: 同一インスタンスの重複配置の禁止

実装: `maui/KsSettingsView.Maui/Internals/KsPlacementDiagnostics.cs:23,32-35,39-40` (判定と文言の共有)、
`KsLogicalChildOwnership.cs:155-158` (追加時に送出)、`:175-189` (`EnsureAdoptable` — コレクション差し替え時に旧側へ触れる前に全件検査)、
`Internals/KsSettingsController.cs:2143-2206` (同一コレクション内の数えあげは現行のまま維持)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一 Cell の二重追加 (表示中の別 Section へ) | `KsLogicalChildOwnership.cs:155-158` | `ConversionPathTests.AddingCellPlacedElsewhereThrowsWithoutTouchingDisplay` (gateway 呼び出しが空であることを表明) | ✅ |
| 別の SettingsView に所有された Cell の追加は Host 未接続でも追加時に例外 | 同上 | `LogicalChildTests.AddingCellOwnedByAnotherSettingsViewThrowsOnAdd` (`Parent` / `BindingContext` / gateway の無傷を表明) | ✅ |
| 別の SettingsView に所有された Section の追加は追加時に例外 | 同上 (`SettingsView.cs:916` の器、kind = "Section") | `LogicalChildTests.AddingSectionOwnedByAnotherSettingsViewThrowsOnAdd` / `ConversionPathTests.AddingSectionPlacedElsewhereThrowsWithoutTouchingDisplay` | ✅ |
| SettingsView から外れた Section が所有したままの Cell の追加は例外 | `KsPlacementDiagnostics.cs:32-35` (facade 所有者なら外れていても他所扱い) | `LogicalChildTests.AddingCellStillOwnedByADetachedSectionThrows` | ✅ |
| 除去してから別の Section へ追加した Cell は新しい所属先の論理子になる | `KsLogicalChildOwnership.cs:196-204` → `:148-168` | `LogicalChildTests.CellMovedAfterRemovalIsAdoptedByTheNewOwner` / `.CellMovedAfterHostReleaseIsConvertedWithTheNewOwnersValue` (最初の snapshot が新所属先の値) | ✅ |
| 同じコレクションへの二重追加は変換時に例外 | `Internals/KsSettingsController.cs:2143-2206` (`EnsureTreeHasNoDuplicates`)、`:2205-2262` | `ConversionPathTests.ConnectingTreeWithDuplicateCellThrows` (⚠️ 前提を「別 Section」→「同じコレクションへ二重」へ変更。deviation 記録済み)、非配信とロールバックは `ConversionPathTests.FailedConnectRollsBackAndAllowsReconnect` (同じく deviation 記録済み)、`ItemsSourceTests.TemplateCreatingAlreadyPlacedCellThrows` | ⚠️ deviation 記録済み |
| (箇条書き) コレクション差し替え時に旧側の配置を動かさない | `KsLogicalChildOwnership.cs:53-61,175-189` | `LogicalChildTests.ReplacingRootWithASectionOwnedElsewhereLeavesTheOldPlacementUntouched` | ✅ |

### Requirement: 同一 View インスタンスの多重配置は例外になる

実装: `Internals/KsSettingsController.cs:2304-2316` (`EnsureViewIsFree` に期待所有者を渡し `IsOwnedElsewhere` を追加)、
`:2339` (accessory guard)、`:2366` (Cell 内容 guard 経路)、`:2378-2383` (`EnsureNotOwnedElsewhere`) と
`:2165,2167,2184,2280` の呼び出し点 (検査を数えあげより前に置く)。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 別の SettingsView に置かれた View を accessory に置くと変換時に例外 | `KsSettingsController.cs:2165,2167` (`EnsureNotOwnedElsewhere` を `AddSeenView` の前へ)、`:2304-2316` | `LogicalChildTests.PlacingAccessoryViewOwnedByAnotherSettingsViewThrowsOnConversion` (設定時は非例外・`Parent` 不動・X 側 gateway 無音を表明) | ✅ |
| 別の SettingsView の CustomCell に置かれた View を Content に置くと変換時に例外 | `KsSettingsController.cs:2184,2280` | `LogicalChildTests.PlacingContentViewOwnedByAnotherSettingsViewThrowsOnConversion` | ✅ |
| SettingsView から外れた Section が所有したままの View を accessory に置くと例外 | `KsSettingsController.cs:2339` (accessory 設定時の guard)、`KsPlacementDiagnostics.cs:32-35` | `LogicalChildTests.PlacingAccessoryViewStillOwnedByADetachedSectionThrows` (A 側不動・`target.HeaderView` が null のままを表明) | ✅ |

---

## 3. specs/samples-maui — ADDED Requirements

### Requirement: MAUI Sample は XAML の AppThemeBinding だけで外観に追随する

実装: `samples/maui/KsSettingsView.Sample.Maui/SampleStyles.xaml` (新規。`SampleSettingsViewStyle` / `SampleSectionDecorationSettingsViewStyle`、
色は `SampleTheme` の定数を `x:Static` で参照)、`SampleStyles.xaml.cs` (新規)、`App.cs:24` (Resources へ併合)、
7 ページの `Style="{StaticResource ...}"` 参照 (`Pages/BasicCellsDemoPage.xaml:22` / `InputCellsDemoPage.xaml:24` /
`MauiSpecificCellFeaturesDemoPage.xaml:13` / `AccessoryViewsDemoPage.xaml:14` / `SectionDecorationDemoPage.xaml:50` /
`CustomCellDemoPage.xaml:24` / `CustomCellMauiSpecificDemoPage.xaml:17`)、
`Pages/BasicCellsDemoPage.xaml:81` (ButtonCell「ログアウト」の `TitleColor`)、
`SampleThemeFollower.cs` 削除 + `SampleTheme.cs` から `Apply` / `MauiTitleText` / `IsDark` / `ApplySectionDecorationDemo` を削除。

| Scenario | 実装 | 証跡 / 検査 | 状態 |
|---|---|---|---|
| 表示中のデモ画面で外観を切り替えると SettingsView と Cell の色が追随する | `SampleStyles.xaml` の `AppThemeBinding` Setter 群、`BasicCellsDemoPage.xaml:81` | `evidence/appearance-switch-verification.md` §2「表示中の外観切替への追随」+ `sample-{ios,android}-buttoncell-{light,dark}-live.png`。実装前後の描画同一性は `evidence/sample-appearance-pixel-diff.md` (32 組すべて diff_px = 0) | ✅ |
| Sample に外観変更の購読コードが無い | `SampleThemeFollower.cs` 削除 | `grep -rn "RequestedThemeChanged" samples/maui/` → 該当 0 件 (本検証で実行)。`SampleThemeFollower` / `SampleTheme.Apply` / `MauiTitleText` / `IsDark` の参照も 0 件 | ✅ |

注: 表示中切替の実機確認は端末外観 (Simulator / Emulator 設定) の切替で行われており、Scenario 文面の
「ルートメニューの外観選択で dark にする」そのものの操作記録ではない。ルートメニューの選択は
`SampleAppearanceStore.cs:44` で `Application.UserAppTheme` へ反映される経路で、同経路の再評価は
`AppearanceBindingTests.CellColorFollowsTheAppearanceChange` が固定し、両外観の描画同一性は
`sample-appearance-pixel-diff.md` (外観はルートメニュー選択で固定) が押さえている。契約 (表示したまま色が描き直される) は
両経路で満たされているため乖離としない。

---

## 4. 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md 全タスク完了 | ✅ 0.1 / 1.1-1.4 / 2.1-2.4 / 3.1-3.2 / 4.1-4.4 / 5.1-5.4 / 6.1-6.3 すべて `[x]`。対応表と突き合わせて**虚偽のチェックなし** |
| 逆流検査 (足場の書き換え) | ✅ `proposal.md` / `design.md` / `specs/**` は `263e300` 以降コミット・未コミットとも変更なし (`git log` / `git diff --stat`)。change 配下の未コミット差分は `tasks.md` のチェックボックス 23 行のみ |
| 未記録乖離 | ✅ なし。`deviation.md` は 13 件 (乖離 2 件 + 付随修正 11 件) を記録。diff 中で Scenario に対応しない変更 (`KsPlacementDiagnostics.cs` 新規、`SettingsView.Root` の購読順、`SampleStyles.xaml` 新設、`SampleTheme.ApplySectionDecorationDemo` 削除、`ConversionPathTests` の前提変更、`handbook/maui/integration-host-verification.md`、`concepts/core/styling/style-resolution.md`、`concepts/maui/index.md` の追加 2 行) はすべて `[付随修正]` として記録済み |
| 旧クラス名の残骸 | ✅ `KsBindingContextBinder` の参照は `maui/` `samples/` `kasane/concepts` `kasane/handbook` に 0 件 (`KsBindingContextBinder.cs` は削除済み) |
| 旧契約の記述の残骸 | ✅ concepts / handbook に「再評価されない」「logical tree に載らない」「RequestedThemeChanged」の記述なし (`kasane/concepts/log.md` の過去エントリのみ。append-only の履歴で対象外) |
| UI 変更 | 該当なし (本 change に `ui/` アーティファクトはない。実機描画の同一性は `evidence/` の画素比較で担保) |
| テスト全件成功 | ✅ 失敗 0 / 合格 557 / 合計 557 (本検証で実行。tasks 6.1 の「528 件 + 追加分」と整合) |

---

## 5. 判定

**VALID**。デルタスペックの全 30 Scenario (maui-core: ADDED 5 Requirement / 19 Scenario、MODIFIED 2 Requirement / 9 Scenario、
samples-maui: ADDED 1 Requirement / 2 Scenario) がすべて「✅ 一致」または「⚠️ deviation 記録済み」。
❌ は 0 件。虚偽チェックなし、逆流なし、テスト 557 件全緑。

補足 (判定に影響しない観察):

1. コンテキストパッケージは maui-core を「ADDED 4 Requirement」としているが、`specs/maui-core/spec.md` の ADDED は 5 Requirement。
   本検証は 5 件すべてを対象にした。
2. Requirement「Section / Cell は SettingsView の論理子である」の 2 番目の Scenario は、見出しが「配下 Cell の論理子が解除される」、
   THEN が「配下 Cell の `Parent` は `section` のまま」で、見出しと THEN が食い違っている。実装・テストは THEN に従っており契約上の乖離はない。
   蒸留で concepts へ写す際に見出し側の言い回しを直すのが妥当。
