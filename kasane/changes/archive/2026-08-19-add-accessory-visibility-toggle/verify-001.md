# Verify 001: add-accessory-visibility-toggle

- 日付: 2026-08-19
- 対象: `kasane/changes/add-accessory-visibility-toggle/specs/` の 6 capability (Requirement 15 / Scenario 40)
- 検証対象実装: 未コミットの作業ツリー変更 (`git diff` 31 ファイル + untracked 新規テスト 6 件)
- 合意済み乖離: `deviation.md` の 2 件 (違反として扱わない)

## 判定: VALID

- ❌ (未記録の欠落・乖離): **0 件**
- ⚠️ (deviation 記録済み): 2 件
- 虚偽チェック: なし / 足場の逆流: なし / テスト: 全件成功

---

## 1. 対応表

### 1.1 settings-view-ios-ui (Requirement 5 / Scenario 17)

パス略記: 実装 `ios/Sources/...`、テスト `ios/Tests/...`

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R1 Section の Header / Footer 表示トグル** | | | |
| 内容がある Header をトグルで隠す | `KsSettingsViewCore/Section.swift:61,64` (フィールド) / `:79-80,92-93` (init) / `KsSettingsViewUI/KsSettingsViewController.swift:716` `shouldShowHeader` | `KsSettingsViewUITests/SectionAccessoryVisibilityTests.swift:87` `test_内容があるHeaderをトグルで隠すとHeader領域が生成されない` | ✅ |
| 内容がある Footer をトグルで隠す | 同上 / `KsSettingsViewController.swift:721` `shouldShowFooter` | `SectionAccessoryVisibilityTests.swift:107` `test_内容があるFooterをトグルで隠すとFooter領域が生成されない` | ✅ |
| 非空 accessory では既定値で現行挙動と一致する | `Section.swift` 既定 `true` | `SectionAccessoryVisibilityTests.swift:125` `test_非空accessoryではトグル既定値で従来どおり表示される` | ✅ |
| replaceSection でトグル変更が反映される | `KsSettingsViewController.swift` 既存 replaceSection 経路 (Section 値をそのまま採用) | `SectionAccessoryVisibilityTests.swift:167,214` `test_replaceSectionでHeader/Footerトグル変更が両方向に反映される` | ✅ |
| トグルは値等価性に参加する | `Section.swift:107-108` (`==`) / `:122-123` (`hash(into:)`) | `KsSettingsViewCoreTests/SectionTests.swift` `test_isHeaderVisible_は等価性判定に含まれる` / `test_isFooterVisible_…` / `test_HeaderトグルとFooterトグルは独立して等価性へ参加する` | ✅ |
| **R2 トグルの独立性と保持** | | | |
| Header を隠しても Footer と Cell は表示されたまま | AND 判定が header / footer 別 | `SectionAccessoryVisibilityTests.swift:145` `test_Headerを隠してもFooterとCellは表示されたまま` / `KsSettingsViewControllerTests.swift` `test_HeaderトグルはFooterの表示判定に影響しない` | ✅ |
| Cell 操作をまたいでトグルが保持される | `SettingsRootStore.swift` の Section 再構築 6 箇所 / `KsSettingsViewController.swift` の再構築 5 箇所 (`:250` visible projection 含む) / `KsSettingsViewSwiftUI/SectionModifiers.swift:68,82` | `SectionAccessoryVisibilityTests.swift:418` `test_StoreのCell操作をまたいでトグルが保持される` (insert/move/replace/replaceCells/remove/updateAccessory×2) / `:468` `test_Cell挿入をまたいでHeaderが非表示のまま` / `:494` `test_visible_projectionでトグルが保持される` / `KsSettingsViewSwiftUITests/SectionModifiersTests.swift:39` | ✅ (⚠️ deviation-1 を含む。後述) |
| 非表示中の内容更新が再表示に反映される | `KsSettingsViewController.swift:1868` 付近 / `SettingsRootStore.swift:389` 付近 (updateAccessory の再構築がトグル保持) | `SectionAccessoryVisibilityTests.swift:250` `test_非表示中にupdateAccessoryしたHeader内容が再表示に反映される` | ✅ |
| **R3 内容不在の統一判定** | | | |
| 空 text の Header は領域を生成しない | `KsSettingsViewController.swift:706` `hasAccessoryContent` (header にも適用) | `SectionAccessoryVisibilityTests.swift:288` `test_空textのHeaderは領域を生成しない` / `KsSettingsViewControllerTests.swift` `test_headerが空文字列の場合supplementaryModesはheaderNoneになる`・`test_shouldShowHeader_はトグルと内容ありのANDになる` | ✅ |
| 空 text の Footer は領域を生成しない | 同上 | `SectionAccessoryVisibilityTests.swift:303` `test_空textのFooterは領域を生成しない` / `test_shouldShowFooter_はトグルと内容ありのANDになる` | ✅ |
| **R4 高さ解決は存在判定の後に適用する** | | | |
| header 不在なら Section.headerHeight 正値でも領域を生成しない | `KsSettingsViewController.swift:666` `guard shouldShowHeader(for: section) else { return nil }` (高さ分岐より前) / `:784` `supplementaryModes` | `SectionAccessoryVisibilityTests.swift:320` / `KsSettingsViewControllerTests.swift` `test_makeHeaderBoundaryItem_headerHeight40でも_header_nilなら_nilを返す` (**旧・逆契約テストを反転済み**) | ✅ |
| 空 text の header は Section.headerHeight 正値でも領域を生成しない | 同上 | `SectionAccessoryVisibilityTests.swift:336` / `test_makeHeaderBoundaryItem_headerHeight40でも_header空textなら_nilを返す` | ✅ |
| header 不在なら Theme.headerHeight があっても領域を生成しない | 同上 (guard が theme 分岐より前) | `SectionAccessoryVisibilityTests.swift:352` / `test_makeHeaderBoundaryItem_themeHeaderHeight指定でも_header_nilなら_nilを返す` | ✅ |
| トグル false なら高さ指定があっても領域を生成しない | 同上 | `SectionAccessoryVisibilityTests.swift:371` / `test_makeHeaderBoundaryItem_トグルfalseなら高さ指定があっても_nilを返す`。逆条件は `:395` `test_内容がありトグルtrueならheaderHeightの固定高さが効く` で固定 | ✅ |
| **R5 宣言 DSL のトグル指定と Store 経路との対称性** | | | |
| DSL でトグルを指定して構築する | `KsSettingsViewSwiftUI/SectionBuilder.swift` (`ksSection` 2 種 + `Section` init 2 種) / `DSLNodes.swift:160-161` (resolved へ転写) | `KsSettingsViewSwiftUITests/DSLAccessoryVisibilityTests.swift:102,113,124,133,150` (転写) / `:270` `test_DSLでトグルを指定して構築するとHeaderが表示されない` | ✅ |
| DSL 再評価でトグル変更が反映される | `DSLDiffCalculator.swift:172` `containsAccessoryVisibilityChange` → `:73` で `.full` | `DSLAccessoryVisibilityTests.swift:164,184,204,225,244` (preflight) / `:285` `test_DSL再評価でトグル変更が両方向に反映される` | ✅ |
| Store 経路と DSL 経路の表示結果が一致する | 同上 | `DSLAccessoryVisibilityTests.swift:323,370` `test_Store経路とDSL経路でHeader/Footerトグルの表示結果が一致する` | ✅ |

### 1.2 settings-view-android-ui (Requirement 4 / Scenario 13)

パス略記: 実装 `android/ks-settingsview-*/src/main/...`、テスト `.../src/test/...`

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R1 Section の Header / Footer 表示トグル** | | | |
| 内容がある Header をトグルで隠す | `core/Section.kt:51-52` (data class フィールド) / `ui/KsSettingsView.kt:1045` `shouldShowHeader` + `:999` flatten | `ui/SectionAccessoryVisibilityTest.kt:106` `内容がある Header をトグルで隠す` | ✅ |
| 内容がある Footer をトグルで隠す | 同上 / `ui/KsSettingsView.kt:1050` `shouldShowFooter` + `:1016` | `ui/SectionAccessoryVisibilityTest.kt:116` `内容がある Footer をトグルで隠す` | ✅ |
| 非空 accessory では既定値で現行挙動と一致する | 既定 `true` | `ui/SectionAccessoryVisibilityTest.kt:126` `非空 accessory ではトグル未指定で従来どおり表示される` | ✅ |
| replaceSection でトグル変更が反映される | 既存 replaceSection 経路 | `ui/SectionAccessoryVisibilityTest.kt:203` `replaceSection でトグル変更が両方向に反映され Cell は保持される` (Cell ID 保持も検証) | ✅ |
| トグルは値等価性に参加する | data class 自動 `equals`/`hashCode` | `core/SectionAccessoryVisibilityTest.kt:44` `equality_isFooterVisible_only_differs` / `:36` header 側 / `:52` 独立性 | ✅ |
| **R2 トグルの独立性と保持** | | | |
| Footer を隠しても Header と Cell は表示されたまま | header / footer 別判定 | `ui/SectionAccessoryVisibilityTest.kt:116` / `:135` `トグルは Header と Footer で独立して効く` | ✅ |
| Cell 操作をまたいでトグルが保持される | data class `copy` (Store の Cell 操作は copy 経由) | `ui/SectionAccessoryVisibilityTest.kt:233` `Cell 操作をまたいでトグルが保持される` (insert/move/replace/remove) / `core/SectionAccessoryVisibilityTest.kt:69` `copy_preserves_toggles` | ✅ |
| 非表示中の内容更新が再表示に反映される | 同上 | `ui/SectionAccessoryVisibilityTest.kt:264` `非表示中の内容更新が再表示に反映される` | ✅ |
| **R3 内容不在の統一判定 (iOS への対称化)** | | | |
| 空 text の Header は行を生成しない | `ui/KsSettingsView.kt:1030` `hasAccessoryContent` | `ui/SectionAccessoryVisibilityTest.kt:156` `空 text の Header は行を生成しない` / `:170` `:181` (View accessory) / `:193` (高さ指定は存在を作らない) | ✅ |
| 空 text の Footer は行を生成しない | 同上 | `ui/SectionAccessoryVisibilityTest.kt:163` `空 text の Footer は行を生成しない` | ✅ |
| **R4 宣言 DSL のトグル指定と Store 経路との対称性** | | | |
| DSL でトグルを指定して構築する | `compose/DSLScope.kt:40-41,67-68` / `compose/DSLNodes.kt:61-65,106-107` (resolved へ転写) | `compose/DSLAccessoryVisibilityTest.kt:38,55,66` (転写) / `compose/DSLAccessoryVisibilityRenderingTest.kt:149` `DSL でトグルを指定して構築すると Header 行が現れない` | ✅ |
| DSL 再評価でトグル変更が反映される | `compose/DSLDiffCalculator.kt:338` `containsAccessoryVisibilityChange` → `:292` で `Full` | `compose/DSLAccessoryVisibilityTest.kt:113,123,133,146,161` / `DSLAccessoryVisibilityRenderingTest.kt:157` (両方向) | ✅ |
| Store 経路と DSL 経路の表示結果が一致する | 同上 | `DSLAccessoryVisibilityRenderingTest.kt:174,203` `Store 経路と DSL 経路で Header/Footer トグルの表示結果が一致する` | ✅ |

### 1.3 maui-bridge (Requirement 1 / Scenario 2)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| DTO のトグルが core Section へ伝搬する | `ios/Sources/KsSettingsViewBridge/KsBridgeSection.swift:49,52` + `makeSection` `:109-110` / `android/ks-settingsview-bridge/.../KsBridgeSection.kt:52,55` + `makeSection` `:93-94` | iOS `Tests/KsSettingsViewBridgeTests/KsBridgeSectionVisibilityTests.swift` `test_DTOのisHeaderVisibleはcoreのSectionへ伝搬する` / `…isFooterVisible…` / `test_replaceSectionで表示トグルを切り替えるとHeaderが消える`。Android 同名テスト (`KsBridgeSectionVisibilityTest.kt:63,75,87`) | ✅ |
| 既定値は true | 同上 (フィールド既定 `true`) | iOS `test_Section_DTOの表示トグル既定はtrue` / `test_トグル未指定のDTOはcoreのSectionへtrueを伝搬する`。Android 同名 (`:42,51`) | ✅ |
| (専用 bridge 操作を追加しない — SHALL) | 新規 bridge 操作なし (diff に追加なし)。`replaceSection` 相乗り | 上記 replaceSection テスト | ✅ |

### 1.4 maui-core (Requirement 2 / Scenario 5)

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| **R1 Section.IsHeaderVisible / IsFooterVisible の公開** | | | |
| 初期構築時のトグルが反映される | `maui/KsSettingsView.Maui/Section.cs:101-114` (BindableProperty) / `:234,242` (CLR プロパティ) / `Platforms/{iOS,Android}/KsBridgeGateway.cs` の `BuildRoot` へ転写 | `KsSettingsView.Maui.Tests/SectionVisibilityTests.cs` `AccessoryVisibilityIsCarriedBySetRoot`。実表示は `evidence/maui-02,04,06,08` | ✅ (注記 O-2) |
| 既定値では現行挙動と一致する | 既定 `true` | `HeaderAndFooterDefaultToVisible` | ✅ |
| **R2 実行時トグル変更の反映経路** | | | |
| 実行時のトグル変更が native へ配信される | `Internals/KsSettingsController.cs:1336-1337` (`nameof(Section.IsHeaderVisible)` / `IsFooterVisible`) (`IsVisible`/`HeaderHeight` と同じ `_replacePendingSections` 分岐) | `HeaderVisibilityChangeIsDeliveredAsSingleReplaceSection` / `HeaderVisibilityRestoreIsDeliveredAsReplaceSection` (逆方向)。実表示は `evidence/maui-01`〜`08` | ✅ |
| トグルが gateway の Section 置換に反映される | 同上 + `KsBridgeGateway.ReplaceSection` | `FooterVisibilityChangeIsDeliveredAsSingleReplaceSection` (置換 1 回・`IsFooterVisible == false`) / `VisibilityAndAccessoryToggleChangesShareOneReplaceSection` | ✅ |
| トグル変更の Section 置換で Cell の identity と内容が保持される | `KsSettingsController` の retained cell id 経路 | `AccessoryVisibilityChangeKeepsCellIdentityAndContent` | ✅ |
| (専用 gateway 操作を追加しない — SHALL) | `IKsSettingsGateway` に新規操作なし | — | ✅ |

### 1.5 samples-ios / samples-android / samples-maui (各 Requirement 1 / Scenario 1)

| Requirement / Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| samples-ios: デモ操作で Header / Footer の表示が独立に切り替わる | `samples/ios/KsSettingsViewSample/VisibilityDemoView.swift:20-21` (state) / `:51-61` (2 トグル) / `:105-116` (Section D) (独立 2 トグル + 観察対象 Section D) | `evidence/ios-01`〜`ios-08` (4 組み合わせ) | ✅ |
| samples-android: 同上 | `samples/android/.../VisibilityDemoScreen.kt:29-30` (state) / `:62-72` (2 トグル) / `:116-127` (Section D) | `evidence/android-01`〜`android-08` | ✅ |
| samples-maui: 同上 | `samples/maui/.../Pages/VisibilityDemoPage.xaml:31-36` (2 トグル) / `:72-82` (Section D) + `ViewModels/VisibilityDemoViewModel.cs:42,49` (ShowHeader / ShowFooter) | `evidence/maui-01`〜`maui-08` | ✅ |
| sample-parity (文言・画面構成の一字一句一致) | — | 機械照合済み: `ヘッダー表示` / `フッター表示` / `「観察対象 Section D」の Header だけを出し入れ` / `…Footer だけを出し入れ` / `観察対象 Section D（Header / Footer）` / `Header / Footer は内容を保持したまま隠れます` / `D-1: 常時表示` / `D-2: 常時表示` が 3 platform で完全一致。Section D の挿入位置 (Section C の直前) も一致 | ✅ |

---

## 2. 追加検査

### 2.1 tasks.md の虚偽チェック

全 24 タスクが `[x]`。対応表と突き合わせた結果、**未実装のままチェック済みのものはなし**。特に確認した項目:

- 1.4 「逆契約を固定していた既存テストを新契約へ反転」→ `KsSettingsViewControllerTests.swift` の `test_makeHeaderBoundaryItem_headerHeight40_header_nilでも_absolute40になる` → `…_nilを返す` へ、`test_headerHeight正値のsectionがあればsupplementaryModesはheaderSupplementaryになる` → `test_headerHeight正値でもheaderが不在ならsupplementaryModesはheaderNoneになる` へ実際に反転済み
- 1.5 「Section 手動再構築箇所の全列挙」→ `SettingsRootStore.swift` 6 箇所・`KsSettingsViewController.swift` 5 箇所・`SectionModifiers.swift` 2 箇所すべてに 2 フィールドが追加されている (diff で確認)
- 3.3 「生成される managed API を検証」→ `maui/macios/KsSettingsView.Binding.iOS/obj/Debug/net10.0-ios/compiled-api-definitions.xml` に `IsHeaderVisible` / `IsFooterVisible` の生成を再確認
- 3.4 「Android Binding の managed プロパティ名を確認・固定」→ `Platforms/Android/KsBridgeGateway.cs` が `dto.HeaderVisible` / `dto.FooterVisible` (Kotlin `isHeaderVisible` → Java `setHeaderVisible` 由来) を使用
- 6.1 → 本検証で再実行 (下記 2.4)
- 6.2 → `evidence/README.md` と 28 枚の PNG が存在 (⚠️ deviation-2 の合意済み手段による)

### 2.2 足場の逆流検査

- `kasane/changes/add-accessory-visibility-toggle/` へのコミットは `1486f45` (提案化) の 1 件のみ
- 作業ツリーで `proposal.md` / `specs/**` に差分なし (`git diff --stat` が空)
- `tasks.md` の差分はチェックボックス `[ ]` → `[x]` のみ (本文の書き換えなし)
- → **逆流なし**

### 2.3 未記録乖離の洗い出し

対応表に ❌ なし。`deviation.md` の 2 件は以下の位置で確認でき、いずれも合意済み差分として扱った:

- ⚠️ deviation-1: `SectionModifiers.swift:63-79` が spec の要求 (トグル保持) に加えて `isVisible` も保持。固定テスト `SectionModifiersTests.test_モディファイアはid以外の状態フィールドを保持する` あり → 記録済み
- ⚠️ deviation-2: 対称化 3 件の視覚証跡をサンプル一時改変で取得 (撮影後 revert)。`samples/` の作業ツリー差分に検証用構成の残骸がないことを diff で確認済み (追加は Section D デモのみ) → 記録済み

### 2.4 テスト実行 (本検証で再実行)

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS 全件 | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` | `** TEST SUCCEEDED **` (exit 0)、失敗 0 |
| iOS 本変更の関連クラス | 上記 + `-only-testing` 6 クラス | KsBridgeSectionVisibilityTests 13 / SectionTests 16 / DSLAccessoryVisibilityTests 14 / SectionModifiersTests 5 / KsSettingsViewControllerTests 42 / SectionAccessoryVisibilityTests 17 = **107 件、失敗 0** |
| Android 全件 | `cd android && ./gradlew test` | BUILD SUCCESSFUL。test-results XML 集計で **tests=2418 / failures=0 / errors=0**。新規 4 クラス (core 6 / ui 12 / compose DSL 8 / compose DSL rendering 4) すべて緑 |
| MAUI | `cd maui && dotnet test KsSettingsView.Maui.Tests` | **424 件合格 / 失敗 0**。`SectionVisibilityTests` 単独で 13 件 (新規 7 件を含む) 合格 |

### 2.5 UI アーティファクト

`proposal.md` の「級: M」節で **`ui/` は作成しない** ことがオーナー承認済みと明記されている (トグルは既存要素の表示・非表示でデザインすべき固有の見た目が無いため)。よって `ui/brief.md` の不在は欠落ではない。

---

## 3. 観察事項 (判定に影響しない注記)

呼び出し元・ユーザーの判断材料として記録する。いずれも Scenario の欠落ではないため ❌ にはしていない。

- **O-1: MAUI net10.0-ios TFM のサンプル実行時証跡が無い。** `evidence/README.md` に記載のとおり、.NET for iOS SDK と Xcode のバージョン不一致 (環境要因) でサンプルがパッケージングできず、`Platforms/iOS/KsBridgeGateway.cs` の 2 フィールド転写 (tasks 4.3 の iOS 分) は静的検証 (生成 API の確認 + 既存 `IsVisible` と同型のコードレビュー) にとどまる。デルタスペックの Scenario はいずれも iOS TFM での実行時観測を要求していないため VALID を妨げないが、**環境が整い次第の実機確認は残課題**。deviation.md には未記載 (記載するか否かは要判断)。
- **O-2: maui-core「初期構築時のトグルが反映される」の THEN (「Header は表示されない」) は、net10.0 ユニットテストでは gateway へ渡る Section の値までしか観測できない。** 実表示の確認は `evidence/maui-02/04/06/08` (Android TFM 実機) が担っている。これは maui-core capability のテスト戦略上の既存構造 (fake gateway) に沿ったもので、本変更固有の弱点ではない。
- **O-3: 変更範囲外の同時変更が 1 件ある** — `kasane/lessons/inbox/ios-incremental-build-runs-stale-binary.md` の count 更新とルール文への Android 項追記。教訓機構 (ksn-lesson) の即時捕捉であり、デルタスペックの検証対象外。
