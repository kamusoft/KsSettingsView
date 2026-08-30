# Verify 001: restore-pickercell-object-items

- 対象: デルタスペック 8 capability × 実装 (HEAD 205b9eb に対する未コミット working tree)
- 判定: **VALID**
- 検証日: 2026-08-28

---

## 1. 対応表

パスはリポジトリ相対。行番号は検証時点の working tree。

### 1.1 cell-types-input (MODIFIED / ADDED / REMOVED)

#### Requirement: PickerCell の候補モデル (MODIFIED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 生の PickerItem 列を渡す | `ios/Sources/KsSettingsViewUI/PickerItem.swift:12` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerItem.kt:12`、`PickerCell.items: [PickerItem]` (`ios/Sources/KsSettingsViewUI/PickerCell.swift:37`) / `List<PickerItem>` (`android/.../ui/PickerCell.kt:36`) | `ios/Tests/KsSettingsViewUITests/PickerCellItemsTests.swift:56` / `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellItemsTest.kt:64` | ✅ 一致 |
| ジェネリック縁の射影 | `ios/Sources/KsSettingsViewUI/PickerCell+ItemProjection.swift:457` (`projectItems`) / `android/.../ui/PickerCellItemProjection.kt:204` (`projectPickerItems`) | `PickerCellItemsTests.swift:67` / `PickerCellItemsTest.kt:77` | ✅ 一致 |
| String 特殊化 | `PickerCell+ItemProjection.swift:264,304,342,378,420` (`items: [String]` overload 群、`displayText: { $0 }`) / `PickerCellItemProjection.kt:122,160` | `PickerCellItemsTests.swift:82,264` / `PickerCellItemsTest.kt:90,241` | ✅ 一致 |
| 空文字列の subText は副表示なし | `PickerItem.swift:18` (init で `nil` 正規化) / `PickerItem.kt:16` (`takeIf { it.isNotEmpty() }`) | `PickerCellItemsTests.swift:89` / `PickerCellItemsTest.kt:98` | ✅ 一致 |
| 元コレクションの変更を観測しない | `PickerCell+ItemProjection.swift:45,130,184` (`let elements = items` で値コピー捕捉) / `PickerCellItemProjection.kt:46,96` (`items.toList()`)、`InputCellDsl.kt:247` (`items.toList()`) | `PickerCellItemsTests.swift:107` / `PickerCellItemsTest.kt:112` / `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/PickerCellObjectBindingTest.kt:80` | ✅ 一致 |

#### Requirement: PickerCell の value 自動表示 (MODIFIED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 複数選択の自動表示は主表示のみ | `PickerCell.swift:279` (`effectiveValueText`、`items[idx].text`) / `android/.../ui/PickerCell.kt:60` (`autoValueText`) | `PickerCellItemsTests.swift:125` / `PickerCellItemsTest.kt:144`、`InputCellsTest.kt` (`PickerCell multi 自動 valueText はカンマ連結で表示`) | ✅ 一致 |
| 範囲外 index の除外 | 同上 (`items.indices.contains` / `getOrNull`) | `PickerCellItemsTests.swift:147` / `PickerCellItemsTest.kt:157` | ✅ 一致 |
| (単一の自動表示 — Requirement 本文) | 同上 | `PickerCellItemsTests.swift:136` / `PickerCellItemsTest.kt:131` | ✅ 一致 |

#### Requirement: 単一選択の object 書き戻し (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で元要素が届く | `PickerCell+ItemProjection.swift:469` (`composeSingleSelection`、index → item の順) / `PickerCellItemProjection.kt:216` | `PickerCellItemsTests.swift:167` / `PickerCellItemsTest.kt:171,186` | ✅ 一致 |
| selectedItem の初期逆引き | `PickerCell+ItemProjection.swift:131` (`elements.firstIndex(of:)`) / `android/ks-settingsview-compose/.../InputCellDsl.kt:249` (`elements.indexOf(current)`) | `PickerCellItemsTests.swift:183` / `PickerCellObjectBindingTest.kt:103` | ✅ 一致 |
| 同値重複は最初の index | 同上 (`firstIndex(of:)` / `indexOf`) | `PickerCellItemsTests.swift:194` / `PickerCellObjectBindingTest.kt:118` | ✅ 一致 |
| 候補に無い要素は未選択 | 同上 (`flatMap` で `nil` / `takeIf { it >= 0 }`) | `PickerCellItemsTests.swift:206` / `PickerCellObjectBindingTest.kt:134` | ✅ 一致 |
| selectedItem TwoWay の書き戻し | `PickerCell+ItemProjection.swift:132-136` (setter) / `InputCellDsl.kt:264` (`selectedItem.value = elements.getOrNull(newIndex)`) | `PickerCellItemsTests.swift:217` / `PickerCellObjectBindingTest.kt:149,166` | ✅ 一致 |
| (型制約: iOS は `Sendable`、TwoWay はさらに `Equatable`。Kotlin は制約なし) | `PickerCell+ItemProjection.swift:26,69,164,209` (`<T: Sendable>`) / `:113` (`<T: Equatable & Sendable>`) / `PickerCellItemProjection.kt:27` (`<T>`) | `PickerCellItemsTests.swift:264` (全呼び出し形のコンパイル成立) / `PickerCellItemsTest.kt:241`、`PickerCellObjectBindingTest.kt:185` | ✅ 一致 |

#### Requirement: 複数選択の object 受け取り (ADDED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定で元要素列が index 昇順で届く | `PickerCell+ItemProjection.swift:485` (`composeMultiSelection`、`newIndices.sorted()`) / `PickerCellItemProjection.kt:234` | `PickerCellItemsTests.swift:231` / `PickerCellItemsTest.kt:204`、`PickerCellObjectBindingTest.kt:59` | ✅ 一致 |
| 範囲外 index は元要素列から除外 | 同上 (`compactMap` / `filter { it in elements.indices }`、index 集合の sink は無加工) | `PickerCellItemsTests.swift:244` / `PickerCellItemsTest.kt:220` | ✅ 一致 |
| (元要素集合の TwoWay は提供しない) | 複数選択の overload は `selectedIndices` のみで、`selectedItems` TwoWay は存在しない (`PickerCell+ItemProjection.swift:164,209` / `PickerCellItemProjection.kt:76`、`InputCellDsl.kt:316`) | — (不在の検査) | ✅ 一致 |

#### REMOVED Requirement: PickerCell の項目表示フォーマッタ (displayFormatter)

| 対象 | 削除の確認 | 状態 |
|---|---|---|
| iOS `PickerCell.displayFormatter` / `PickerListViewController.displayFormatter` | 削除済み。`grep -rn "displayFormatter" ios android maui samples` の結果は 0 件 | ✅ 一致 |
| Android `PickerCell.displayFormatter` / DSL `displayFormatter` | 同上 | ✅ 一致 |
| 旧挙動テストの残骸 | `InputCellsTests.swift` の `..._単一_displayFormatter` 削除、`InputCellsTest.kt` の `..._displayFormatter が適用される` 削除、`PickerSelectionSheetTest.kt` の該当ケースは素の列挙検証へ縮退 (deviation `[付随修正]` に記録済み) | ✅ 一致 |

### 1.2 settings-view-ios-ui (ADDED)

#### Requirement: 選択面候補行の副表示 (iOS)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| subText 付き候補の表示 | `ios/Sources/KsSettingsViewUI/PickerListItemCell.swift` (`.subtitle` 固定セル)、`PickerListViewController.swift:219,226-227` (`detailTextLabel` へ `subText` と description 系統の実効値) | `ios/Tests/KsSettingsViewUITests/PickerSelectionScreenTests.swift` (`test_選択面_subTextを持つ候補行は副表示を描画する` / `..._副表示はdescription系統の実効値で描画される` / `..._副表示はCellStyleがThemeより優先される`) | ✅ 一致 |
| 混在リスト | `PickerListViewController.swift:219` (`subText` が `nil` の行は `detailTextLabel` 空) | `PickerSelectionScreenTests.swift` (`test_選択面_混在リストは副表示のある行だけが2行構成になる` / `test_選択面_行の再利用で副表示が残らない`) | ✅ 一致 |
| 副表示混在時の初期スクロール | `PickerListViewController.swift:190,203-204` (既存の `scrollToRow(at:.middle)` を維持) | `PickerSelectionScreenTests.swift` (`test_初期スクロール_副表示混在でも選択中の項目が見える状態で開く`) | ✅ 一致 |
| VoiceOver への公開 | `PickerListViewController.swift:236-241` (主表示 + 副表示を連結して `accessibilityLabel`) | `PickerSelectionScreenTests.swift` (`test_選択面_副表示を持つ候補行は主表示と副表示と選択状態を公開する`) | ✅ 一致 |
| (副表示は1行・末尾省略 — ui/brief.md 承認モック plan-a) | `PickerListItemCell.swift` (`numberOfLines = 1` / `.byTruncatingTail`) | `PickerSelectionScreenTests.swift` (`test_選択面_長い副表示は1行に収めて末尾を省略する`) | ✅ 一致 |

### 1.3 settings-view-android-ui (ADDED)

#### Requirement: 選択面候補行の副表示 (Android)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| subText 付き候補の表示 | `android/.../ui/PickerSelectionSheet.kt:434-448` (副表示 TextView)、`:139` (`itemSubTextColor = effective.descriptionColor` 他を `PickerSheetStyle.from` で1回解決)、`:623-633` (`bindRow`) | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt` (`subText を持つ候補行は主表示と副表示の両方を表示する` / `副表示は description 系統の実効値で描画される` / `副表示の実効値は CellStyle が Theme より優先される`) | ✅ 一致 |
| 混在リスト | `PickerSelectionSheet.kt:629` (`visibility = if (subText == null) GONE else VISIBLE`) | `PickerSelectionSheetTest.kt` (`subText を持たない候補行は副表示を持たない` / `混在リストでは subText を持つ行だけが副表示を持つ` / `空文字の subText は副表示なしとして扱われる` / `再利用された行では前の候補の副表示が残らない` / `RecyclerView 経由で生成された行にも副表示が反映される`) | ✅ 一致 |
| 折り畳み高さの契約維持 | `PickerSelectionSheet.kt:544-` (`applySheetHeight` — 行数ではなく実測高で見積もるため可変行高でも成立) | `PickerSelectionSheetTest.kt:1391` (`副表示付きの候補でも折り畳み高さと内部スクロールの契約は保たれる`) | ✅ 一致 |
| 副表示混在時の初期スクロール | `PickerSelectionSheet.kt:324-329` (`initialScrollPosition`、位置指定で行高非依存) | `PickerSelectionSheetTest.kt:1415` (`副表示が混在しても選択中の項目が可視領域に入った状態で開く`) | ✅ 一致 |
| TalkBack への公開 | `PickerSelectionSheet.kt:631-632` (行コンテナの `contentDescription` に主表示 + 副表示)、副表示 View は `IMPORTANT_FOR_ACCESSIBILITY_NO` | `PickerSelectionSheetTest.kt:1432` (`候補行は副表示も含めてアクセシビリティへ公開される`) | ✅ 一致 |
| (副表示は1行・末尾省略 — 承認モック plan-a) | `PickerSelectionSheet.kt:444-445` (`isSingleLine` / `TruncateAt.END`) | `PickerSelectionSheetTest.kt` (`副表示は1行に収めて末尾を省略する` / `副表示の長さが変わっても副表示あり行の行高は変わらない`) | ✅ 一致 |

### 1.4 maui-bridge (MODIFIED)

#### Requirement: PickerCell の輸送

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 副表示の有無が往復で保存される | facade: `maui/KsSettingsView.Maui/Internals/KsCellSnapshots.cs:150` (`KsPickerItemSnapshot(Text, SubText)`)、gateway: `maui/KsSettingsView.Maui/Platforms/iOS/KsBridgeGateway.cs` / `Platforms/Android/KsBridgeGateway.cs` の `PickerItems`、DTO: `ios/Sources/KsSettingsViewBridge/KsBridgePickerItem.swift` / `android/.../bridge/KsBridgePickerItem.kt`、native 変換: `KsBridgeValueTransport.pickerItems` (両 platform) | `maui/KsSettingsView.Maui.Tests/PickerItemProjectionTests.cs:126` (`EmptyOrNullSubTextBecomesNoSubText`)、`CellShapeTests.cs` (`PickerItemsAreProjectedByDisplayMembers`)、`ios/Tests/KsSettingsViewBridgeTests/KsBridgeCellConversionTests.swift` (`test_PickerCellDTOの副表示が候補ごとに保存される`)、`android/.../bridge/KsBridgeCellConversionTest.kt` (`PickerCell DTO の副表示が候補ごとに保存される`) | ✅ 一致 |
| 副表示付き候補の輸送 (native 選択面に主表示 + 副表示) | 上記 + native 選択面 (1.2 / 1.3 の実装) | 上記 bridge 変換テスト + 1.2 / 1.3 の選択面テスト + `ui/verification/maui-sample-picker-object-selection.png` (MAUI sample 実機系での目視照合) | ✅ 一致 |
| 選択通知は index で戻る | 変更なし (index 経路は従来どおり)。`KsBridgePickerCell` の `selectedIndex` / `selectedIndices` は据え置き | `maui/KsSettingsView.Maui.Tests/NativeValueWritebackTests.cs` (`PickerSelectionIsWrittenBack`)、`KsBridgeInteractionDelegateTests.swift` / `KsBridgeInteractionListenerTest.kt` (`picker 選択と数値変更が対応するメソッドで通知される`)、`KsBridgeValueTransportTests` (順序違い / 範囲外 index) | ✅ 一致 |

> 注: `Platforms/{iOS,Android}/KsBridgeGateway.cs` は MAUI テストプロジェクト (net10.0) のコンパイル対象外という**既存**の検証境界にあり、この change で新たに生じた穴ではない。snapshot 側 (facade) と DTO 側 (native) の両端が固定されており、間の写しは 1:1 の逐次コピーである。

### 1.5 maui-cells (MODIFIED / REMOVED)

#### Requirement: PickerCell の候補と表示射影

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| DisplayMember による主表示の射影 | `maui/KsSettingsView.Maui/PickerCell.cs:29,39,365` (`ItemsSource: IList` / `DisplayMember` / `ProjectItems`)、`Internals/KsMemberProjection.cs:30` | `maui/KsSettingsView.Maui.Tests/PickerItemProjectionTests.cs:17` (`DisplayMemberProjectsMainText`) | ✅ 一致 |
| SubDisplayMember による副表示 | `PickerCell.cs:48`、`KsMemberProjection.cs:49` | `PickerItemProjectionTests.cs:34` (`SubDisplayMemberProjectsSubText`) | ✅ 一致 |
| 未指定時は ToString() | `KsMemberProjection.cs:30-37` / `:49-58` | `PickerItemProjectionTests.cs:52` (`UnspecifiedMembersFallBackToToString`) | ✅ 一致 |
| 解決不能なプロパティ名 | `KsMemberProjection.cs:88` (`Resolve` が null → 既定へフォールバック) | `PickerItemProjectionTests.cs:69,84,145` (`UnresolvableMemberFallsBackToToString` / `NonPublicAndStaticMembersAreNotResolved` / `AmbiguousMemberNameFallsBackInsteadOfThrowing`) | ✅ 一致 |
| string 以外・null のプロパティ値 | `KsMemberProjection.cs:36,57` (`?.ToString() ?? string.Empty` / 空は副表示なし) | `PickerItemProjectionTests.cs:103,126` | ✅ 一致 |
| null 要素は設定時に拒否 | `PickerCell.cs:33,291` (`validateValue` → `RejectNullElements` で `ArgumentException`) | `PickerItemProjectionTests.cs:181` (`NullElementIsRejectedOnAssignment`) | ✅ 一致 |
| 元コレクションの in-place 変更を観測しない | `PickerCell.cs:346` (`CaptureItems` で設定時に配列へ写す) | `PickerItemProjectionTests.cs:212` (`InPlaceMutationOfItemsSourceIsNotObserved`) | ✅ 一致 |
| (射影は ItemsSource / DisplayMember / SubDisplayMember の差し替えで反映) | `PickerCell.cs:39-55` (両 member の `propertyChanged` → `ProjectItems`) | `PickerItemProjectionTests.cs:193` (`ReplacingMembersReprojectsItems`) | ✅ 一致 |
| (getter の例外は伝播) | `KsMemberProjection.cs:69` (`BindingFlags.DoNotWrapExceptions`) | `PickerItemProjectionTests.cs:166` (`GetterExceptionIsPropagated`) | ✅ 一致 |

#### Requirement: PickerCell の選択項目の相互導出

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| ユーザー確定で SelectedItem が更新される | `PickerCell.cs:65,459,594` (行番号は working tree) (`SelectedIndex` 変更 → `SyncSelectedItemFromIndex` → `ResolveSelectedItem`) | `maui/KsSettingsView.Maui.Tests/PickerSelectedItemTests.cs:245,258` (`UserSelectionUpdatesBothIndexAndItem` / `UserSelectionReturnsOriginalObject`) | ✅ 一致 |
| SelectedItem 設定から index を導出 | `PickerCell.cs:86,459,606,652` (`SyncIndexFromSelectedItem` → `ResolveSelectedIndex` → `IndexOfItem` は最初一致) | `PickerSelectedItemTests.cs:19,275` (`SettingSelectedItemResolvesIndex` / `DuplicateItemResolvesToFirstIndex`) | ✅ 一致 |
| ユーザー確定で SelectedItems が更新される | `PickerCell.cs:75,504,613` (`SyncSelectedItemsFromIndices` → `ResolveSelectedItems` は index 昇順) | `PickerSelectedItemTests.cs:286,303` | ✅ 一致 |
| SelectedItems 設定で見つからない要素は保持されない | `PickerCell.cs:96,504,629` (`ResolveSelectedIndices` は `IndexOfItem >= 0` のみ収集し `SortedSet` へ) | `PickerSelectedItemTests.cs:317` (`SettingSelectedItemsKeepsOnlyResolvableElements`) | ✅ 一致 |
| 同値要素の重複設定は1件に揃う | `PickerCell.cs:629-650` (`SortedSet<int>` + 正からの再導出) | `PickerSelectedItemTests.cs:329` (`DuplicateSelectedItemsCollapseToSingleIndex`) | ✅ 一致 |
| (null 設定は空リストと同義) | `PickerCell.cs:631-636` | `PickerSelectedItemTests.cs:341,357` | ✅ 一致 |
| (TwoWay 既定) | `PickerCell.cs:90,100` (`defaultBindingMode: BindingMode.TwoWay`) | `CellShapeTests.cs` (`UserEditablePropertiesDefaultToTwoWayBinding`、10 → 12 件へ更新) | ✅ 一致 |
| **「ItemsSource 未設定は未選択へ揃える」** | 未設定 (候補 0 件) の間は相互導出を行わず選択を保留し、候補到着時に逆引きして復元 (`PickerCell.cs:312` (`HasItems` ガード) / `:322` (`OnItemsSourceChanged`) / `:342` (`HasPendingItemSelection`) / `:408` (`RestoreSelectionFromItems`)) | `PickerSelectedItemTests.cs:88,103,130,143,156,172` | ⚠️ deviation 記録済み (deviation.md 1件目 + 3件目、オーナー裁定 B) |
| **公開値の正規化範囲** | `PickerCell.cs:534` (`ApplySelectedItem` は参照一致でのみ書き戻しを止め、Cell の公開値まで snapshot 実体へ揃える。TwoWay 先の VM は値等価な別実体を持ち得る) | `PickerSelectedItemTests.cs:206,388,402,418,434` (`NormalizationStopsAtCellAndDoesNotReachViewModel` 他) | ⚠️ deviation 記録済み (deviation.md 2件目、オーナー裁定 B) |

#### REMOVED Requirement: PickerCell.DisplayFormatter

| 対象 | 削除の確認 | 状態 |
|---|---|---|
| `PickerCell.DisplayFormatter` | 削除済み (`maui/KsSettingsView.Maui/PickerCell.cs` に定義なし)。`CellShapeTests.cs` の既定値検査は `DisplayMember` / `SubDisplayMember` へ差し替え、`PickerItemsAreFormattedByDisplayFormatter` は `PickerItemsAreProjectedByDisplayMembers` へ置換 | ✅ 一致 |

### 1.6 samples-ios / samples-android / samples-maui (ADDED)

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| object 候補の選択 (iOS) | `samples/ios/KsSettingsViewSample/InputCellsDemoView.swift:196-227` (`selectedItem` TwoWay + `onItemsSelected`)、`samples/ios/KsSettingsViewSample/SampleMember.swift`、`.xcodeproj` へのファイル登録あり | `ui/verification/ios-sample-picker-object-selection.png` / `ios-sample-picker-object-rows.png`、`ui/brief.md` 照合結果 | ✅ 一致 |
| object 候補の選択 (Android) | `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/InputCellsDemoScreen.kt:203-236`、`SampleMember.kt` | `ui/verification/android-sample-picker-object-selection.png` / `android-sample-picker-object-rows.png` | ✅ 一致 |
| object 候補の選択 (MAUI) | `samples/maui/KsSettingsView.Sample.Maui/Pages/InputCellsDemoPage.xaml:70-86` (`DisplayMember` / `SubDisplayMember` / `SelectedItem` / `SelectedItems` TwoWay)、`ViewModels/InputCellsDemoViewModel.cs:168,171,188`、`SampleMember.cs` | `ui/verification/maui-sample-picker-object-selection.png`、`ui/brief.md` (VM 側の選択要素受け取りを目視確認) | ✅ 一致 |

---

## 2. 追加検査

### 2.1 tasks.md の完了状況 (虚偽チェック)

全 24 タスクが `[x]`。対応表と突き合わせて**未実装のままチェック済みのものは無い**。

- 1.1〜1.5 (iOS core) / 2.1〜2.5 (Android core): §1.1 で実装・テストとも確認
- 3.1〜3.4 (iOS 選択面) / 4.1〜4.4 (Android 選択面): §1.2 / §1.3 で確認
- 5.1〜5.2 (bridge): §1.4 で確認 (per-item DTO `KsBridgePickerItem` を両 platform に追加、`ApiDefinition.cs` も追随)
- 6.1〜6.4 (MAUI facade): §1.5 で確認
- 7.1〜7.3 (samples): §1.6 で確認
- 8.1 (視覚照合): `ui/verification/` に 9 枚のスクリーンショットが実在し、`ui/brief.md` に「照合結果」節として乖離なしの記録あり

### 2.2 逆流検査 (足場アーティファクトの書き換え)

`git status` / `git diff HEAD` で change ディレクトリ配下の変更を確認:

- `proposal.md` / `design.md` / `specs/*/spec.md` / `exploration.md` / `ui/mock/*`: **無変更** (HEAD 205b9eb のまま)
- `tasks.md`: チェックボックスの `[ ]` → `[x]` のみ。本文・タスク定義に改変なし
- `ui/brief.md`: 末尾に「照合結果」「トークン候補」節を追記。承認記録 (mock 採用・approved.png) 部分は無変更 — 実装中の記録として想定内
- 新規ファイル: `deviation.md` / `review-001..003.md` / `second-opinion-code-001.md` / `ui/verification/`

→ **逆流なし**。

### 2.3 未記録乖離の洗い出し

対応表に ❌ なし。deviation.md の記録と diff の突き合わせ:

| deviation.md の記録 | diff 上の対応 | 確認 |
|---|---|---|
| `ItemsSource` 未設定時の選択保留 (裁定 B) | `PickerCell.cs` の `HasItems` ガード / `RestoreSelectionFromItems` | ✅ |
| 正規化は Cell 公開値まで (裁定 B) | `PickerCell.cs:551` `ApplySelectedItem` | ✅ |
| 保留の実装範囲 (候補 0 件の間は相互導出しない) | 同上 | ✅ |
| [付随修正] iOS bridge doc コメントから displayFormatter 言及を除去 | `KsBridgePickerCell.swift` (doc コメント) | ✅ |
| [付随修正] Android bridge doc コメント同様 | `KsBridgePickerCell.kt` (doc コメント) | ✅ |
| [付随修正] `KsSimpleCheckView.onDraw` の `canvas.width/height` → `width/height` | `KsSimpleCheckView.kt:84-89`。テスト `KsSimpleCheckViewDrawTest.kt` 新設 | ✅ |
| [付随修正] `CellShapeTests.TwoWayProperties` に `SelectedItem` / `SelectedItems` を追加 (10 → 12) | `CellShapeTests.cs` (`TwoWayProperties` の 2 行追加と件数 12 への更新) | ✅ |
| [付随修正] Android 既存 PickerCell 呼び出しの factory 形への機械的追随 / displayFormatter テスト 2 件の削除・縮退 | `CellRowWidthAllocationTest.kt` / `InputCellsTest.kt` / `PickerSelectionSheetTest.kt` | ✅ |

diff 中に Scenario にも `[付随修正]` にも対応しない変更は見つからなかった。

**参考所見 (乖離ではない)**: `skills/ja|en/kssettingsview-maui/references/cells.md` と `skills/*/kssettingsview-aiforms-migration/references/api-mapping.md` に `DisplayFormatter` の記述が残っている。ただしこれは AGENTS.md / config.yaml の規約により `docs-refresh` スキル (ユーザー明示依頼) の責務であり、design.md の実装順序 3 でも「実装完了後に docs-refresh で行う」と明記されている。本 change のスコープ外。

### 2.4 UI 変更 (承認モック・妥協の記録)

- `ui/brief.md` に承認記録あり: **plan-a-subtext-single-line.html を採用 (approved.png)**、2026-08-28 オーナー承認。不採用案 (plan-b) も記録
- 承認後修正 (キャプション文言訂正 + approved.png 再撮影) の記録あり
- 照合結果に「乖離なし」を記録。合意済み妥協として、器・ヘッダーの色差 (sample の共有 Theme 由来 / mock の既定色) を本 change の対象外と明記
- 実装との一致: 承認案の「副表示は1行・末尾省略、副表示あり行の行高は長さに依存しない」は iOS `PickerListItemCell` (`numberOfLines = 1` / `.byTruncatingTail`) と Android `PickerSelectionSheet` (`isSingleLine` / `TruncateAt.END`) で実装され、両 platform ともテストで固定されている

### 2.5 テスト実行 (全件・実行件数の確認)

`kasane/concepts/cross/conventions/test-execution.md` に従い、絞り込みなしで実行:

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `cd ios && xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` | **973 tests / 0 failures** (Bridge 165 + Core 88 + SwiftUI 91 + UI 629)、`** TEST SUCCEEDED **` |
| Android | `cd android && ./gradlew test --rerun-tasks` | **2680 tests / 0 failures** (`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` の集計)、`BUILD SUCCESSFUL` |
| MAUI | `cd maui && dotnet test KsSettingsView.Maui.Tests/KsSettingsView.Maui.Tests.csproj` | **502 tests / 0 failures** |

いずれもオーケストレーターから渡された直近の件数と一致。

---

## 3. 判定

**VALID**

- 8 capability の全 Requirement / Scenario が「✅ 一致」または「⚠️ deviation 記録済み」
- ⚠️ は maui-cells「PickerCell の選択項目の相互導出」の 2 点のみで、いずれも deviation.md にオーナー裁定 (B) として記録済み。実装範囲の補足 (3 件目) も含めて記録と実装が対応している
- tasks.md に虚偽チェックなし
- 足場アーティファクト (proposal / design / specs) への逆流なし
- 未記録の欠落・乖離なし
- iOS / Android / MAUI の全テストを実行し、全件成功を確認 (973 / 2680 / 502、いずれも 0 failures)
