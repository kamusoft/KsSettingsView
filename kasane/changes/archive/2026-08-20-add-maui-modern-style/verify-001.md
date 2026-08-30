# 検証結果: add-maui-modern-style (001 回目)

**日付**: 2026-08-20
**判定**: VALID

デルタスペック 5 capability の全 Requirement / Scenario を実装・テストへ機械的に突き合わせた。
❌ (未記録の欠落・乖離) は 0 件。tasks.md の虚偽チェックなし、足場アーティファクトへの逆流なし、
テストは 3 platform とも全件成功。

deviation.md は存在しない (記録済み乖離なし)。したがって表中の「⚠️ deviation 記録済み」は 0 件。

---

## 1. maui-core

### Requirement: ListStyle の公開

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 既定値では現行挙動と一致する | `maui/KsSettingsView.Maui/SettingsView.cs:135` (`ListStyleProperty` 既定 `Classic`) / `maui/KsSettingsView.Maui/SettingsViewStyle.cs` | `maui/KsSettingsView.Maui.Tests/ListStyleTests.cs:16` `ListStyleDefaultsToClassic` / `:25` `DefaultListStyleIsDeliveredAsClassic` | ✅ |
| Modern 指定が native へ伝わる | `SettingsView.cs:135` propertyChanged → `Internals/KsSettingsController.cs:311` `SetStyle` → `Platforms/{iOS,Android}/KsBridgeGateway.cs:157` → Bridge `setStyle` | `ListStyleTests.cs:36` `ListStyleSetBeforeConnectIsDeliveredOnConnect` + native `KsBridgeStyleTests.swift:35` / `KsBridgeStyleTest.kt:49` (Host へ反映されるところまで) | ✅ |
| 実行時の切替が反映される | 同上 (可変プロパティへの即時適用: `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:372` / `android/.../bridge/KsSettingsBridge.kt:450`) | `ListStyleTests.cs:48` / `:60` (逆方向) / `:72` `ListStyleChangeDoesNotTouchTree` (設定内容・identity 不変)、native `KsBridgeStyleTests.swift:35,54` / `KsBridgeStyleTest.kt:49,69` | ✅ |
| 切替が gateway へ伝わる (net10.0 ユニットテスト) | `KsSettingsController.cs:311` | `ListStyleTests.cs:48` `ListStyleChangeWhileConnectedIsDelivered` | ✅ |
| (要件文) プロパティ名に `Style` を使わない | `SettingsView.cs:571` `ListStyle` | — (命名は静的に確認) | ✅ |
| (要件文) style は Theme 経路に載せない | `Internals/KsThemeSnapshot.cs` に style フィールドなし | `ListStyleTests.cs:87` `ListStyleIsNotCarriedInThemeSnapshot` | ✅ |

### Requirement: Theme の Section 装飾4属性の公開

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未指定では platform 既定で描画される | `SettingsView.cs:443,452,461,469` (既定すべて null) → `SettingsView.cs:1021-1027` snapshot | `maui/KsSettingsView.Maui.Tests/SectionDecorationThemeTests.cs:18` `UnsetSectionDecorationCarriesNothing` + native 既定解決 `SectionBoxMetricsTest.kt` / `SectionBoxDecorationTests.swift` (既存) | ✅ |
| 指定値が Theme として native へ伝わる | `SettingsView.cs:1021-1027` / 両 OS `KsBridgeGateway.cs:505` 付近の DTO 写像 | `SectionDecorationThemeTests.cs:36` `SectionDecorationIsCarriedAsTheme` | ✅ |
| 実行時の属性変更が反映される | 4 プロパティとも `propertyChanged → ApplyTheme()` | `SectionDecorationThemeTests.cs:60` `SectionDecorationChangeWhileConnectedIsApplied` | ✅ |
| 範囲外の値でも例外を投げず素通しする | `validateValue` / `coerceValue` を付けていない (`SettingsView.cs:443-475`) | `SectionDecorationThemeTests.cs:115` `OutOfRangeSectionDecorationIsCarriedAsIs` + Bridge 実経路 `KsBridgeThemeTest.kt:138` / `KsBridgeThemeTests.swift:145` | ✅ |
| 非有限数も例外を投げず素通しする | 同上 | `SectionDecorationThemeTests.cs:138` `NonFiniteSectionDecorationIsCarriedAsIs` + Bridge 実経路 `KsBridgeThemeTest.kt:138,169` / `KsBridgeThemeTests.swift:145,170` | ✅ |

「facade は platform 既定値の定数を持たない」は `SettingsView.cs` に既定寸法の定数が無いことで満たす。
正規化の委譲先 (Native 描画時) までの経路が例外なく通ることは、Bridge を通す
`負値と非有限の Section 装飾でも描画時に 0 へ正規化される` (両 OS) が担保する。

### Requirement: SectionMargin の論理方向解釈

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Left / Right が leading / trailing として輸送される | `Internals/KsWireValues.cs:172` `MarginLeading` = `Left` / `:179` `MarginTrailing` = `Right` | `SectionDecorationThemeTests.cs:88` `SectionMarginHorizontalComponentsAreCarriedAsLogicalDirections` | ✅ |
| Classic でも全成分が伝搬される | `SettingsView.cs:1021-1024` (style で分岐しない) | `SectionDecorationThemeTests.cs:101` `SectionMarginHorizontalComponentsAreCarriedUnderClassic` | ✅ |
| (SHALL NOT) `FlowDirection` を監視・変換しない | `SettingsView.cs` / `KsWireValues.cs` に `FlowDirection` 参照なし (grep 済み) | — | ✅ |
| (要件文) Classic の上下のみ適用を doc に明記 | `SettingsView.cs:836-848` の `SectionMargin` XML doc | — | ✅ |

---

## 2. maui-bridge

### Requirement: Theme DTO の Section 装飾4属性輸送

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 4成分が方向対応型へ組み立てられる | iOS `ios/Sources/KsSettingsViewBridge/KsBridgeTheme.swift:158` `resolvedSectionMargin()` / Android `android/.../bridge/KsBridgeTheme.kt:188` `resolveSectionMargin()` + `KsBridgeSectionMargin.kt:20` | `KsBridgeThemeTests.swift:91` / `KsBridgeThemeTest.kt:78` (leading→start, trailing→end を明示検証) | ✅ |
| null は未指定として resolve される | 同上 (`NSNumber?` / `Double?` を map) | `KsBridgeThemeTests.swift:115` / `KsBridgeThemeTest.kt:105` | ✅ |
| 部分 null の margin は全体を未指定として解決する | `KsBridgeTheme.swift:158-172` (guard let 4連) / `KsBridgeTheme.kt:188-196` (`?: return null` 4連) | `KsBridgeThemeTests.swift:128` / `KsBridgeThemeTest.kt:119` | ✅ |
| borderColor が platform 色へ変換される | `KsBridgeTheme.swift:151` `KsBridgeColor.uiColor(...)` / `KsBridgeTheme.kt:177` `KsBridgeColor.color(...)` | `KsBridgeThemeTests.swift:91` / `KsBridgeThemeTest.kt:78` (末尾のアサーション) | ✅ |
| (要件文) facade 側 all-or-none | `SettingsView.cs:1021-1024` (`margin?.X` の 4 連) | `SectionDecorationThemeTests.cs:72` `SectionMarginIsCarriedAllOrNone` | ✅ |
| (要件文) 7 フィールド・入れ子 DTO なし | `Internals/KsThemeSnapshot.cs:102-120` / `KsBridgeTheme.swift:96-108` / `KsBridgeTheme.kt:120-138` / `maui/macios/.../ApiDefinition.cs:967-1002` | 上記各テスト | ✅ |

### Requirement: style の設定操作の輸送

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| SetStyle が native の style へ適用される | `KsSettingsBridge.swift:372` / `KsSettingsBridge.kt:450` (Host の可変プロパティへ直接適用) | `KsBridgeStyleTests.swift:35` / `KsBridgeStyleTest.kt:49` (箱の水平余白が行位置に現れることまで観察) | ✅ |
| 序数の対応が両 OS で一致する | `ios/.../KsBridgeStyle.swift:21` / `android/.../KsBridgeStyle.kt:22` (どちらも 1→Modern) | `KsBridgeStyleTests.swift:22` / `KsBridgeStyleTest.kt:34` | ✅ |
| 定義域外の序数は Classic へ正規化される | 同上 (`default` / `else` 分岐) | `KsBridgeStyleTests.swift:28,97` / `KsBridgeStyleTest.kt:41,116` | ✅ |
| (要件文) gateway が `SetStyle` を持つ | `Internals/IKsSettingsGateway.cs:122` / `Platforms/{iOS,Android}/KsBridgeGateway.cs:157` / `ApiDefinition.cs:1126` | fake gateway (`Tests/Fakes/FakeSettingsGateway.cs:241`) | ✅ |
| (要件文) Store 非経由である旨の注記 | `IKsSettingsGateway.cs:115-121` / `KsSettingsBridge.swift:366-371` / `KsSettingsBridge.kt:441-449` | — | ✅ |

### Requirement: style の Host 再生成をまたぐ保持

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Host 生成前の style 設定が生成時に適用される | `KsSettingsBridge.swift:44,87` / `KsSettingsBridge.kt:57,111` (Host 外フィールド + 生成時適用) | `KsBridgeStyleTests.swift:69` / `KsBridgeStyleTest.kt:85` | ✅ |
| Host 再生成をまたいで style が維持される | 同上 (`releaseHost()` はフィールドを触らない) | `KsBridgeStyleTests.swift:81` / `KsBridgeStyleTest.kt:98` | ✅ |
| gateway 初回接続時に style が配信される | `Internals/KsSettingsController.cs:121` `_style` / `:204` `Connect` 内で `SetStyle` | `ListStyleTests.cs:36` `ListStyleSetBeforeConnectIsDeliveredOnConnect` / `:25` (既定でも配信) | ✅ |

---

## 3. samples-maui

### Requirement: SectionDecoration デモページ

| Scenario | 実装 | 証跡 | 状態 |
|---|---|---|---|
| メニューからデモページへ遷移できる | `samples/maui/KsSettingsView.Sample.Maui/SampleScreen.cs:68-71` (Demo 区分の末尾に登録) / `Pages/SectionDecorationDemoPage.xaml(.cs)` | `screenshots/maui-{ios,android}-*.png` (画面到達) / `parity-table.md` 1 節 | ✅ |
| style 切替操作で表示が切り替わる | `SectionDecorationDemoPage.xaml` の `RadioButtonGroup` → `ListStyle` バインド / `ViewModels/SectionDecorationDemoViewModel.cs:11` (初期 Modern) | `screenshots/` の classic / modern 各 6 枚 (2 OS × 3 preset) | ✅ |
| preset 切替で装飾が変わる | `SectionDecorationPreset.cs` (3 preset) → XAML の 4 属性バインド | `screenshots/` の standard / wide-margin / bordered 各 4 枚 | ✅ |
| (要件文) 文言・画面構成の native 一致 (sample-parity) | `SectionDecorationDemoPage.xaml` / `SampleTheme.cs:135` `ApplySectionDecorationDemo` / `SampleIconBadge.cs` | `parity-table.md` (メニュー文言・操作部文言・preset 値・Section/Cell 構成・下地 Theme・初期状態・許容差分) | ✅ |
| (要件文) 新しい色既定を追加しない | `SampleTheme.cs:78` `DemoSectionBorder` (#C7C7CC) | native 側に同名同値の定数が既存 (`samples/ios/.../SampleTheme.swift:95` / `samples/android/.../SampleTheme.kt:121`)。sample-parity が要求する同値の写しであり、MAUI 独自の新色ではない | ✅ |

---

## 4. settings-view-ios-ui

### Requirement: Section 装飾値の非有限数正規化

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 非有限の装飾値でも例外なく描画される | `ios/Sources/KsSettingsViewUI/SectionBoxMetrics.swift:44` `normalized(_:)` (margin 4 成分・cornerRadius・borderWidth に適用) | `ios/Tests/KsSettingsViewUITests/SectionBoxDecorationTests.swift:192` `test_非有限の寸法は0として扱う` / `:209` `test_非有限の値を持つThemeでもModernの表示が破綻しない` | ✅ |
| (要件文) Theme 構築時には拒否しない | `ios/Sources/KsSettingsViewUI/Theme.swift:139` (検証なし) / Bridge も素通し | `KsBridgeThemeTests.swift:145` `負値と非有限のSection装飾は素通しされる` | ✅ |

## 5. settings-view-android-ui

### Requirement: Section 装飾値の非有限数正規化

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 非有限の装飾値でも例外なく描画される | `android/ks-settingsview-ui/src/main/kotlin/.../SectionBoxMetrics.kt:96` `px(...)` の非有限ガード | `.../ui/SectionBoxMetricsTest.kt:137` `非有限の寸法は 0 として扱う` / `.../ui/ModernSectionDecorationTest.kt:645` `非有限の寸法を持つ Theme でも 0 として描画され例外を出さない` | ✅ |
| (要件文) Theme 構築時には拒否しない | `.../ui/Theme.kt:111` (検証なし) + Bridge が非検証の `PaddingValues` 実装 `KsBridgeSectionMargin.kt` を使う | `KsBridgeThemeTest.kt:138` `負値と非有限の Section 装飾は素通しされる` / `:169` (描画まで通す) | ✅ |

前回検証で欠けていた「MAUI → Bridge → 描画」の通し経路が、両 OS の Bridge テスト
(`負値と非有限の Section 装飾でも描画時に 0 へ正規化される` / `test_負値と非有限のSection装飾でも描画時に0へ正規化される`)
で塞がれている。

---

## 追加検査

| 検査 | 結果 |
|---|---|
| tasks.md の全タスク完了 | 1.1〜5.1 すべて `[x]`。対応表と突き合わせて**虚偽チェックなし**。4.1 の成果物は `parity-table.md`、5.1 の証跡は `screenshots/` 24 枚 (12 組) |
| 逆流検査 (足場の書き換え) | `git diff HEAD -- kasane/changes/add-maui-modern-style/` は tasks.md の**チェックボックス 20 行のみ**。proposal.md / specs/ は未変更。実装は未コミットのため、実装期間中の足場改変なし |
| 未記録乖離 | ❌ 0 件のため該当なし。deviation.md は不要 |
| UI 変更の扱い | `ui/` は proposal で省略を明記 (見た目の正は implement-modern-style の native 実装)。承認モックの代替として task 5.1 の native 比較を要求しており、`screenshots/` + `parity-table.md` がその証跡 |
| テスト全件実行 (検証側で再実行) | net10.0 facade **439 tests / 0 failures**、iOS Simulator (iPhone 17) **560 tests / 0 failures**、Android `./gradlew test --rerun-tasks` **2522 tests / 0 failures** (test-results XML 集計)。`dotnet build -f net10.0-ios` / `-f net10.0-android` いずれも 0 警告 / 0 エラー |

## 判定

**VALID** — 5 capability・9 Requirement・24 Scenario (要件文からの派生検査を含む) のすべてが
「✅ 一致」。虚偽チェック・逆流・未記録乖離・テスト失敗のいずれもない。
