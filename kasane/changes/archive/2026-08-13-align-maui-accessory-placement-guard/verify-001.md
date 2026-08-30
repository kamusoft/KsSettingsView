# Verify 001: align-maui-accessory-placement-guard

- 検証対象: HEAD (3cb2f92) に対する未コミット working tree 変更のうち `maui/` 配下
- デルタスペック: `kasane/changes/align-maui-accessory-placement-guard/specs/maui-core/spec.md`
- 判定: **VALID**

## 対応表

デルタスペックは MODIFIED Requirement 1件・Scenario 13件。パスは worktree root からの相対。

### Requirement: 同一 View インスタンスの多重配置は例外になる (MODIFIED)

要件本文の SHALL 節ごとの対応:

| SHALL 節 | 実装 | テスト | 状態 |
|---|---|---|---|
| 4対象 (Root/Section × Header/Footer) すべてに失敗時契約が適用され、テストは4対象をマトリクスで張る | `Section.cs:52` / `Section.cs:78` (validateValue)、`SettingsView.cs:69` / `SettingsView.cs:86` (validateValue) | `AccessoryViewTests.cs:503` `[TestCaseSource(nameof(Targets))]`、`Targets` は 4 対象すべて (`AccessoryViewTests.cs:22-28`) | ✅ 一致 |
| プロパティ設定の失敗時契約: 検査は値が確定する前に行われる | `Section.cs:52,78` / `SettingsView.cs:69,86` の `validateValue` → `KsSettingsController.EnsureAccessoryViewCanBePlaced` (`KsSettingsController.cs:386`) / `EnsureRootAccessoryViewCanBePlaced` (`KsSettingsController.cs:396`) → `EnsureAccessoryViewIsNotPlaced` (`KsSettingsController.cs:2204`) | `AFailedViewReplacementKeepsTheCurrentView` (公開値・論理親・lease・native の 4 点を検証) | ✅ 一致 |
| 検査失敗は `InvalidOperationException` (validateValue の false 返却は使わない) | `KsSettingsController.cs:2263` `DuplicatePlacement`、validateValue は常に `true` を返し例外で表す (`Section.cs:53-61`) | 全該当テストが `Assert.Throws<InvalidOperationException>` | ✅ 一致 |
| guard の差し込み / 取り外し (未参加の所有者には検査相手がいない) | `KsSettingsController.cs:1953` (`RegisterSection` で `section.AccessoryGuard = this`)、`:1961` (`UnregisterSection` で null)、`:1022` (`ClearRegistrations` で null)。`Section.AccessoryGuard` は `Section.cs:264` | `ASectionRemovedFromTheRootStopsConsultingThePlacement` (`:742`)、`ASectionDroppedByARootRebuildStopsConsultingThePlacement` (`:760`)、`AddingASectionThatReusesAPlacedViewThrows` (`:314` — 未参加 Section への設定が例外にならないことを固定) | ✅ 一致 |
| 構造変更バッチ: バッチ内の相互重複 (Content との交差含む) と既存配置との重複を native 更新前に全件検査 | `EnsureSectionsAreNotPlaced` (`KsSettingsController.cs:2103-2124`) に `HashSet<View> seen` + `AddSeenView` を追加、`EnsureCellsAreNotPlaced(cells, seen)` (`:2138`) へ数えあげを引き継ぐ。`AddSeenView` は重複で送出 (`:2255`) | `AddingSectionsThatShareAnAccessoryViewThrowsBeforeAnyInsert` (`:575`)、`AddingASectionWhoseAccessoryAndCellContentShareAViewThrowsBeforeAnyInsert` (`:616`) | ✅ 一致 |
| Root 再構築: 新ツリー内部の相互重複と再構築をまたいで残る root accessory との重複を、現在のツリーへ触れる前に全件検査 | `EnsureTreeHasNoDuplicates` (`KsSettingsController.cs:2041`)、root accessory を先に数える (`:2048-2049`)。呼び出しは `:984` / `:1100` / `:1144` の native 前 | `RebuildingWithADuplicateAccessoryThrowsWithoutTouchingTheCurrentTree` (`:779`)、`RebuildingWithASectionThatCollidesWithTheRootAccessoryThrows` (`:807`) | ✅ 一致 |
| 再構築での同一 Section 継続配置 (同じ Section・同じ slot・同じ View) は例外にならない | `EnsureAccessoryViewIsNotPlaced` の `placed != slot` 判定 (`KsSettingsController.cs:2211`) | `RebuildingWithTheSameSectionKeepsItsAccessory` (`:829`) | ✅ 一致 |
| 旧所有者→新所有者へ null を経ず移す再構築は保証対象外 (現行挙動に委ねる) | 規定なし (実装追加なし) | — (非規定のためテスト不要) | ✅ 一致 |
| 失敗時に native・対応表・実体・論理所有のいずれも変更されない / native gateway 呼び出しが発生しない | 上記の値確定前検査・バッチ前検査 | 全該当テストが `Gateway.Calls, Is.Empty` + 対応表 (`FindSectionId` / `FindCellId`) + lease (`IsDisposed`) + 論理親 (`Parent`) を検証 | ✅ 一致 |
| 公開コレクションはロールバックされない (現行契約の明文化) | 実装変更なし (現行挙動の固定) | `AddingSectionsThatShareAnAccessoryViewThrowsBeforeAnyInsert` の `Assert.That(root, Is.EqualTo(new[] { placedSection, first, second }))` | ✅ 一致 |
| 別 SettingsView / 通常 Layout 配下との重複は対象外 | 検査は `_placedViews` / `_placedContentViews` (単一 controller 内) のみ参照 | — (Non-Goal) | ✅ 一致 |

### Scenario 対応表

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 同一インスタンスを2箇所へ設定すると例外 | `KsSettingsController.cs:2204` `EnsureAccessoryViewIsNotPlaced` | `AccessoryViewTests.cs:292` `PlacingTheSameViewTwiceThrows`、`:303` `PlacingTheSameViewInHeaderAndFooterThrows` | ✅ 一致 |
| 失敗した差し替えでは公開値と旧状態が一切動かない | `Section.cs:52,78` / `SettingsView.cs:69,86` validateValue | `AccessoryViewTests.cs:504` `AFailedViewReplacementKeepsTheCurrentView` (4対象 parameterized。公開値 `ViewOf` / `Parent` / lease 同一・未 dispose / `Gateway.Calls` 空 / 衝突相手の `Parent`・lease・輸送値も無傷) | ✅ 一致 |
| Root accessory の失敗した差し替えでも同様に旧状態が残る | `SettingsView.cs:69` validateValue → `EnsureRootAccessoryViewCanBePlaced` (`KsSettingsController.cs:396`)、`_placedContentViews` 判定 (`:2216`) | `AccessoryViewTests.cs:541` `AFailedRootViewReplacementWithACellContentKeepsTheCurrentView` | ✅ 一致 |
| 追加バッチ内で accessory View が重複すると 1 件も入れないまま例外 | `EnsureSectionsAreNotPlaced` の in-batch `seen` (`KsSettingsController.cs:2110-2122`) / `EnsureTreeHasNoDuplicates` (`:2041`) | `AccessoryViewTests.cs:575` `AddingSectionsThatShareAnAccessoryViewThrowsBeforeAnyInsert`、`:616` `AddingASectionWhoseAccessoryAndCellContentShareAViewThrowsBeforeAnyInsert` (Content との交差) | ✅ 一致 |
| 追加バッチの後続要素だけが既存配置と衝突しても先頭要素は入らない | `EnsureViewIsFree` を全件通してから native (`KsSettingsController.cs:2118-2122`、呼び出しは `:1144` の前段) | `AccessoryViewTests.cs:646` `AddingSectionsWhereOnlyTheLastCollidesLeavesTheFirstOut` | ✅ 一致 |
| 差し替えバッチの重複でも同様に 1 件も適用されない | 差し替え入口の `EnsureTreeHasNoDuplicates` (`KsSettingsController.cs:1100`) | `AccessoryViewTests.cs:679` `ReplacingSectionsWhereOnlyTheLastCollidesAppliesNothing` + 新規 `Support/RangeReplaceCollection.cs` (複数件 Replace 通知の再現) | ✅ 一致 |
| 失敗したバッチは Root の全体再構築で再収束できる | `SetSections` 経路 (`KsSettingsController.cs:984` 以降) が対応表非掲載要素も含め作り直す | `AccessoryViewTests.cs:713` `AFailedAddBatchIsRecoveredByRebuildingTheRoot` | ✅ 一致 |
| Root 再構築内の重複は現在の木に触れないまま例外 | `EnsureTreeHasNoDuplicates` (`:2041`) を `:984` で native/対応表操作の前に実行 | `AccessoryViewTests.cs:779` `RebuildingWithADuplicateAccessoryThrowsWithoutTouchingTheCurrentTree` | ✅ 一致 |
| 同一 Section を含む Root 再構築は引き続き成立する | `EnsureAccessoryViewIsNotPlaced` の同一 slot 許容 (`:2211`)、`EnsureTreeHasNoDuplicates` の Section 単位数えあげ | `AccessoryViewTests.cs:829` `RebuildingWithTheSameSectionKeepsItsAccessory` | ✅ 一致 |
| Root 再構築の新ツリーが root accessory と衝突すると現在の木に触れないまま例外 | `EnsureTreeHasNoDuplicates` が root accessory を先に数える (`:2048-2049`) | `AccessoryViewTests.cs:807` `RebuildingWithASectionThatCollidesWithTheRootAccessoryThrows` | ✅ 一致 |
| 未参加の所有者に持ち越された重複は参加時点で弾かれる | `Section.AccessoryGuard` が null の間は素通し (`Section.cs:264`)、参加時に `EnsureSectionsAreNotPlaced` で検出 | `AccessoryViewTests.cs:314` `AddingASectionThatReusesAPlacedViewThrows` (`Gateway.Calls` 空を追加検証)、`:369` `GeneratedSectionThatReusesAPlacedViewThrowsWithoutStealingIt` | ✅ 一致 |
| 未接続のまま構築された重複は Host 接続時に弾かれる | Host 接続時の `EnsureTreeHasNoDuplicates` (`:1144` 経路) | `AccessoryViewTests.cs:347` `ADuplicateBuiltWhileDisconnectedThrowsWhenTheHostConnects` (新規)、`:484` `DuplicatePlacementIsDetectedWithoutAGateway` | ✅ 一致 |
| null 解除後の再利用は許容される | `EnsureAccessoryViewIsNotPlaced` は `_placedViews` 未登録なら素通し (`:2211`) | `AccessoryViewTests.cs:895` `ViewCanBePlacedAgainAfterItIsReleased`、`:464` `UnregisteredSectionClaimsAViewThatWasReleased` | ✅ 一致 |

## 追加検査

### tasks.md の完了と虚偽チェック

全 14 タスク (1.1〜1.4 / 2.1 / 3.1〜3.9 / 4.1) がチェック済み。対応表と突き合わせて未実装のチェックはなし。

| タスク | 裏付け |
|---|---|
| 1.1 accessory 用の内部 guard の口 | `maui/KsSettingsView.Maui/Internals/IKsAccessoryViewGuard.cs` (新規。Section 用は target 付き)、Root 用は controller 直結の `EnsureRootAccessoryViewCanBePlaced` (`KsSettingsController.cs:396`) |
| 1.2 Section の validateValue (未設定時素通し) | `Section.cs:52`, `:78` (`section.AccessoryGuard?.` の null 条件) |
| 1.3 SettingsView の validateValue (`_controller` 未初期化に備え null 条件) | `SettingsView.cs:69`, `:86` (`._controller?.`) |
| 1.4 controller の guard 実装と配線 | `KsSettingsController.cs:28` (`IKsAccessoryViewGuard` 実装宣言)、`:386`、`:1953` / `:1961` / `:1022` |
| 2.1 バッチ検査の対称化 | `KsSettingsController.cs:2103-2153` |
| 3.1〜3.9 回帰テスト | 上記 Scenario 対応表のテスト列 (全項目に対応テストあり) |
| 4.1 全通し | 下記「テスト実行」 |

### 逆流検査 (足場の凍結)

`kasane/changes/align-maui-accessory-placement-guard/` は未コミットのため git 履歴では追えず、mtime で確認した。

- `specs/maui-core/spec.md`: 00:10:43、`proposal.md`: 00:11:18
- 実装ファイルの最古 mtime: `Internals/IKsAccessoryViewGuard.cs` 00:16:25

足場 (proposal / specs) は実装開始より前が最終更新で、実装期間中の書き換えなし。`tasks.md` (00:27:07) と `deviation.md` (00:29) の更新は進捗・乖離記録であり凍結対象外。**逆流なし**。

### 未記録乖離

対応表に ❌ なし。deviation.md の 2 件はいずれもスペック沈黙領域の記録で、Requirement / Scenario の欠落を伴わない。

- `Section.AccessoryGuard` の弱参照化: スペックは参照強度を規定していない。挙動 (差し込み・取り外し・検査の成立) は上表の該当テストで固定済み
- `CustomCell.ContentGuard` の既存リーク: 本変更の Non-Goals (Content 側の挙動変更なし) の範囲外。スペックの Requirement / Scenario に対応項目なし

### UI 変更

`ui/` アーティファクトなし。UI 変更を伴わない変更のため該当なし。

### テスト実行

```
dotnet test maui/KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj
成功!   -失敗: 0、合格: 416、スキップ: 0、合計: 416
```

tasks 4.1 のベースライン 400 件に対し +16 件。追加テストの内訳 (4対象 parameterized の `AFailedViewReplacementKeepsTheCurrentView` が 4 件、他 12 メソッドが各 1 件) と一致し、退行ゼロ。

## 判定

**VALID**

全 13 Scenario および Requirement 本文の全 SHALL 節が「✅ 一致」。tasks.md の虚偽チェックなし、足場への逆流なし、未記録乖離なし、テスト全件成功 (416/416)。
