# 一致検証結果: add-maui-native-bridge (001 回目)

**日付**: 2026-08-05
**判定**: **VALID**

デルタスペック 2 件 (maui-bridge 6 Requirements / ios-store 2 Requirements) の全 Scenario について、実装とテストの対応を確認した。❌ (未記録の欠落・乖離) は 0 件。虚偽チェックなし、足場の逆流なし、テストは再実行して全通過。

---

## 1. デルタスペック: maui-bridge

### Requirement: Bridge の生成と Root 構築

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| LabelCell を含む root の表示 | `ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:72` (`setRoot`) / `KsBridgeRootBuilder.swift:32,53` / `android/.../bridge/KsSettingsBridge.kt:87` / `KsBridgeRootBuilder.kt` | iOS `KsBridgeRootTests.test_setRoot_構築どおりのSectionとLabelCellが表示される` / Android `KsBridgeRootTest.setRoot で構築どおりの Section と LabelCell が表示される` | ✅ 一致 |
| setRoot の再呼び出しは全置換 | 同上 (`store.replaceAll`) | iOS `test_setRoot_再呼び出しで表示が全置換される` / Android `setRoot の再呼び出しで表示が全置換される` | ✅ 一致 |
| 採番された ID で後続操作ができる | `KsBridgeSection.swift:50` / `KsBridgeLabelCell.swift` (生成時に canonical UUID 採番) + `KsSettingsBridge.swift:169` (`replaceCell`) / Android `KsBridgeIdentifier.kt:25` + `KsSettingsBridge.kt:203` | iOS `test_採番されたcellIDでreplaceCellが反映される` / Android `採番された cellID で replaceCell が反映される` | ✅ 一致 |
| 不正な ID は no-op | `KsBridgeIdentifier.swift:27` (`UUID(uuidString:)`) / `KsBridgeIdentifier.kt:20-32` (8-4-4-4-12 の厳密 regex。`UUID.fromString` の短縮形受理を避けて iOS と厳密さを揃えている) | iOS `test_不正なIDのremoveCellはno_op` + 契約表の全「未知 ID は no-op」ケース / Android 同名テスト + 契約表 | ✅ 一致 (iOS / Android で同一結果) |

- ID の interop 契約 (Bridge 採番・呼び出し側は返却 ID のみ使用) は Builder / insert 系の戻り値で担保。`addLabelCell` は Builder 内に存在しない sectionID で `nil`/`null` を返す (両 OS でテスト済み)
- スレッド契約 (UI スレッド呼び出し) は呼び出し側契約であり、実装は marshal しない旨を doc comment に明記 (iOS `KsSettingsBridge.swift:22-23` / Android `KsSettingsBridge.kt:29`)

### Requirement: Native Host の生成と接続

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Host 生成 → setRoot の順で表示される | `KsSettingsBridge.swift:51` (`makeHostViewController`) / `KsSettingsBridge.kt:62` (`makeHostView(context)`) | iOS `KsBridgeHostTests.test_Host生成後のsetRootが表示へ反映される` / Android `KsBridgeHostTest.Host 生成後の setRoot が表示へ反映される` | ✅ 一致 |
| setRoot → Host 生成の順でも表示される | 同上 (Host は接続時点の Store 現在状態から復元) | iOS `test_setRoot後に生成したHostが現在状態を復元する` / Android `setRoot 後に生成した Host が現在状態を復元する` | ✅ 一致 |

- 「同時に 1 つの Host」: 生成 API の再呼び出しで同一 Host を返すことを両 OS でテスト (`makeHostViewController は同じ Host を返す` / `makeHostView は同じ Host を返す`)
- Android の `Context` はフィールド保持なし (`KsSettingsBridge.kt` に Context 型のフィールドが存在しないことを確認)

### Requirement: Bridge の lifecycle

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 破棄は冪等 | `KsSettingsBridge.swift:62` / `KsSettingsBridge.kt:74` (`isDisposed` early return) | iOS `KsBridgeLifecycleTests.test_dispose_は冪等` / Android `dispose は冪等` | ✅ 一致 |
| 破棄後の操作は no-op | 全公開 API 先頭の `guard !isDisposed` / `if (isDisposed) return` | iOS `test_破棄後のreplaceCellとsetThemeは表示を変えない` `test_破棄後のsetRootと構造操作は状態を変えない` / Android 同名 2 件 + `破棄後の makeHostView は null を返す` | ✅ 一致 |

### Requirement: Store 操作 1:1 の更新 API

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell の構造操作が表示へ反映される | `KsSettingsBridge.swift:135,144` / `KsSettingsBridge.kt:160,173` | iOS `KsBridgeUpdateTests.test_insertCellとremoveCellが表示へ反映される` / Android 同等 | ✅ 一致 |
| Section の構造操作が表示へ反映される | `KsSettingsBridge.swift:85,93,102` / `KsSettingsBridge.kt:101,112,124` | iOS `test_insertSectionとmoveSectionとremoveSectionが表示へ反映される` / Android 同等 | ✅ 一致 |
| replaceCell は行の identity を維持する | `KsSettingsBridge.swift:169` (`newCell.makeCell(id: uuid)` で対象 ID を維持) / `KsSettingsBridge.kt:203` | iOS `test_replaceCell_は行のidentityを維持する` / Android `replaceCell は行の identity を維持する` | ✅ 一致 |
| replaceCells は1バッチで反映される | `KsSettingsBridge.swift:184` → `SettingsRootStore.replaceCells` / `KsSettingsBridge.kt:222` | iOS `test_replaceCells_が1バッチで反映される` / Android `replaceCells が 1 バッチで反映される` (Adapter 通知件数で検証) | ✅ 一致 |
| 全12操作が契約どおりに反映される | 12 API (`setRoot` / `insertSection` / `removeSection` / `moveSection` / `replaceSection` / `insertCell` / `removeCell` / `moveCell` / `replaceCell` / `updateAccessory` / `replaceCells` / `setTheme`) | iOS `KsBridgeOperationContractTests.test_全12操作が契約どおりに反映される` (24 ケース: 代表引数 + 未知 ID + index 丸め境界。実描画タイトル・header text・構造 Diff 件数・バッチ件数で検証) / Android `KsBridgeOperationContractTest.全 12 操作が契約どおりに反映される` (同構成) | ✅ 一致 |

- accessory の text 限定 / clear (null) は `updateAccessory(target:sectionID:text:)` のシグネチャ自体で担保。root header / footer / section header / footer の 4 target を両 OS で提供
- 契約表は 12 操作すべてを網羅していることを1件ずつ確認済み

### Requirement: Theme 適用

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Theme 変更が表示へ反映される | `KsBridgeTheme.swift:97` (`resolve()`) → `store.applyTheme` / `KsBridgeTheme.kt:116` | iOS `KsBridgeThemeTests.test_setTheme_の輸送値がThemeへ変換される` `test_setTheme_で構造とidentityは変化しない` / Android `setTheme の輸送値が Theme へ変換される` `setTheme で構造と identity は変化しない` | ✅ 一致 |
| 同値 Theme は再適用されない | Store の Theme 契約へ素通し | iOS `test_同値ThemeでのsetTheme再呼び出しは通知されない` / Android `同値 Theme での setTheme 再呼び出しは通知されない` | ✅ 一致 |

- 輸送 DTO と Theme 公開項目の 1:1 対応を実数で確認: iOS `Theme` 公開 29 項目 ↔ `KsBridgeTheme` 29 プロパティ、Android `Theme` 29 項目 ↔ `KsBridgeTheme` 29 プロパティ (名前も一致)
- 未指定 (nil / null) が Theme 側の未指定 (既定値) になることは両 OS で専用テストあり (`未指定項目は Theme 側の未指定になる`)
- 構造 Diff を発行しないことは契約表の setTheme ケース (`diffCount: 0`) でも押さえている

### Requirement: .NET binding からの呼び出し

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| C# からの参照とビルド | `maui/macios/KsSettingsView.Binding.iOS/` (net10.0-ios / `XcodeProject` + `ApiDefinition.cs`) / `maui/android/KsSettingsView.Binding.Android/` (net10.0-android / `AndroidLibrary Bind=true` + gradlew Exec) / `maui/KsSettingsView.slnx` に 4 プロジェクト登録 | `DEVELOPER_DIR=…Xcode-26.1.1… dotnet build maui/KsSettingsView.slnx` → **成功 / 0 警告 0 エラー** (本検証で再実行) | ⚠️ deviation 記録済み (Android の csproj 形式) |
| C# からの実行時疎通 | `maui/tests/KsSettingsView.IntegrationHost.iOS/` `…Android/` + 共有シナリオ `maui/tests/shared/KsBridgeScenario.cs` (Builder・setRoot・更新 API 10 種・setTheme・破棄をすべて C# から呼ぶ) | iOS: 本検証で simulator (iPhone 17 Pro / iOS 26.0) へ install → launch → スクリーンショットで LabelCell 表示と各更新の反映を確認 (詳細は下記) / Android: ホストは存在しビルド成功。実行時確認は実装者記録 (tasks 5.4) に依拠 | ✅ 一致 (Android 実行は本検証では未再現。下記「補足」参照) |

**iOS 実行時疎通の確認内容** (今回のスクリーンショットで観察):
Builder で構築した Section「一般」/「ストレージ」と LabelCell が表示され、`insertSection`「通知」、`insertCell`/`moveCell`「バージョン」、`replaceCell`「テーマ→ダーク」、`replaceCells`「言語→English」「キャッシュ→0 MB」、`replaceSection`「同期 (無効)」、`updateAccessory` の root header / root footer / Section header「通知設定」、`setTheme` の header 文字色まで反映済み。C# → Bridge → Store → Native Host の縦の疎通が実機相当環境で成立している。

---

## 2. デルタスペック: ios-store

### Requirement: 複数 Cell 内容更新のバッチ適用 (replaceCells)

実装: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:201-239` (`replaceCells`) + `:53-59` (`contentUpdateBatchSubject` / `contentUpdateBatchPublisher`)

| Scenario | テスト | 状態 |
|---|---|---|
| 複数 Cell の更新が1バッチで配信される | `SettingsRootStoreTests.test_replaceCells_複数Cellの更新が1バッチで配信される` (配信時点で購読者が更新後状態を参照できることも同時に検証) | ✅ 一致 |
| 既知・未知 ID の混在では既知だけが適用・配信される | `test_replaceCells_既知と未知IDの混在では既知だけが適用配信される` | ✅ 一致 |
| 存在しない ID は無視され適用0件なら配信しない | `test_replaceCells_存在しないIDのみでは状態変更も配信もされない` | ✅ 一致 |
| 空リストは no-op | `test_replaceCells_空配列は何もしない` | ✅ 一致 |
| 同一 ID の重複指定は最後の値が残る | `test_replaceCells_同一IDの重複指定は最後の値が残る` (配信 ID 群が `[cellID, cellID]` = 適用ごとに含まれることも検証) | ✅ 一致 |

**Android との対称性**: Android `SettingsRootStore.kt:194-215` と実装構造を突き合わせた。入力順適用・未知 ID スキップ・適用0件なら未配信・状態更新後に emit・重複 ID を適用ごとに含める、の 5 点すべて同一。要件本文が求める「観察可能挙動の対称」を満たす。

### Requirement: Native Host のバッチ内容更新反映

実装: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift` の `applyContentUpdateBatch(_:)` (新設) + `connectStore` でのバッチ購読 + `rebuildModelIndexes()` の切り出し

| Scenario | テスト | 状態 |
|---|---|---|
| バッチ更新が表示へ反映される | `ContentUpdateBatchTests.test_replaceCells_バッチ更新が表示へ反映され構造変更は発生しない` (実描画セルのタイトル、snapshot の section/item 集合の不変、行インスタンス同一性まで検証) + `test_replaceCells_存在しないIDのみでは表示が変化しない` | ✅ 一致 |

---

## 3. tasks.md との突き合わせ

| タスク | 成果物 | 状態 |
|---|---|---|
| 1.1 iOS binding spike | `maui/spike/macios/**` + `maui/spike/app/KsBindingSpikeApp/` + `maui/spike/README.md` の成功ゲート表 (4 ゲートすべて OK と記録) | ✅ 実在 |
| 1.2 Android binding spike | `maui/spike/android/**` + 同 README | ✅ 実在 |
| 2.1 / 2.2 | `SettingsRootStore.replaceCells` + `SettingsRootStoreTests` 5 件 | ✅ 実在 |
| 2.3 / 2.4 | `applyContentUpdateBatch` + `ContentUpdateBatchTests` 2 件 | ✅ 実在 |
| 3.1〜3.3 | `ios/Sources/KsSettingsViewBridge/` 9 ファイル + `ios/Package.swift` への target 追加 | ✅ 実在 |
| 3.4 / 3.5 | `ios/Tests/KsSettingsViewBridgeTests/` 28 テスト (契約表 24 ケース含む) | ✅ 実在 |
| 4.1〜4.3 | `android/ks-settingsview-bridge/src/main/**` 9 ファイル + `settings.gradle.kts` への include 追加 | ✅ 実在 |
| 4.4 / 4.5 | `android/ks-settingsview-bridge/src/test/**` 29 テスト (契約表含む) | ✅ 実在 |
| 5.1 / 5.2 | binding csproj 2 本 | ✅ 実在 (5.2 は deviation 記録済み) |
| 5.3 | `maui/KsSettingsView.slnx` に 4 プロジェクト。ソリューションビルド成功 | ✅ 実在 |
| 5.4 | 検証ホスト 2 本 + 共有シナリオ。iOS は本検証で実行確認 | ✅ 実在 |

**虚偽チェックなし** — 全 `[x]` に対応する成果物が実在する。

---

## 4. 追加検査

- **逆流検査**: `proposal.md` / `design.md` / `specs/maui-bridge/spec.md` / `specs/ios-store/spec.md` は `4d6802e` 以降 未変更 (`git log` / `git status` で確認)。`tasks.md` の未コミット差分はチェックボックスの `[ ]`→`[x]` のみ。**逆流なし**
- **未記録乖離**: なし。下記「合意済み差分・許容範囲の所見」はいずれも deviation.md 記録済みか、spec 本文の「Store の現行契約がそのまま適用される」に収まる
- **UI 変更**: 本 change に `ui/` アーティファクトはなく (interop 境界の新設で新規 UI デザインを伴わない)、モック承認ゲートは対象外
- **テスト再実行結果** (本検証で実行):

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS Bridge | `xcodebuild test -scheme KsSettingsView-Package -only-testing:KsSettingsViewBridgeTests` | **28 tests / 0 failures** / TEST SUCCEEDED |
| iOS Store + Host バッチ | 同 `-only-testing:KsSettingsViewUITests/SettingsRootStoreTests -only-testing:…/ContentUpdateBatchTests` | **30 tests / 0 failures** / TEST SUCCEEDED |
| Android Bridge | `./gradlew :ks-settingsview-bridge:test` | **BUILD SUCCESSFUL** / 29 tests × (debug/release) = 58 実行 / 0 failures |
| MAUI ソリューション | `DEVELOPER_DIR=…Xcode-26.1.1… dotnet build maui/KsSettingsView.slnx` | **成功 / 0 警告 0 エラー** |
| iOS 実行時疎通 | `simctl install` + `launch` (iPhone 17 Pro / iOS 26.0) | 表示確認 OK |

---

## 5. 合意済み差分・許容範囲の所見 (❌ ではない)

1. **Android Binding csproj の gradlew Exec 方式** — `deviation.md` に実測根拠つきで記録済み・オーナー承認済み。spec の「AndroidGradleProject 形式」との差は合意済み差分として扱う (⚠️)
2. **replace 系 API の戻り値 `String?`** — spec は戻り値を規定していない additive な追加。Store へ渡す前の存在確認は判定条件が Store 側と同一 (`id` 一致) で、観察可能な挙動 (未知 ID は no-op) は変わらない。review-002 で妥当と結論済み
3. **契約表 `replaceSection` で header text を変えない回避** — Host 側の既存の再描画不具合を避けたもので、別 change `kasane/changes/fix-replace-section-header-refresh/` として起票済み。spec は「Store の現行契約がそのまま適用される」ため、既存挙動の継承であり本 change の乖離ではない
4. **`updateAccessory` に canonical だが未知の sectionID を渡した場合** — spec は未知 ID の no-op を「Cell / Section 操作」について述べ、`updateAccessory` は「現行通知挙動」がそのまま適用されると明記している。Store 現行挙動への素通しであり契約内。強化は `kasane/changes/harden-update-accessory-unknown-id/` に起票済み
5. **Bridge 破棄と Host の寿命** — Host 単独解放の経路は `kasane/changes/release-host-without-bridge-dispose/` として起票済み。spec の「破棄後に Host の表示が更新されることはない」は満たしている (破棄後は Store を操作しない)

## 6. 補足 (判定に影響しない所見)

- **Android 実行時疎通の再現**: 本検証の環境に Android エミュレータが存在せず (物理デバイスは接続されているが、オーナーの実機へのアプリ install は独断で行わない判断)、Scenario「C# からの実行時疎通」の Android 側は**今回は再現していない**。ビルド成功と検証ホストの実在は確認済みで、実行確認は tasks 5.4 の実装者記録に依拠している。再現したい場合はエミュレータ起動後に `dotnet build maui/tests/KsSettingsView.IntegrationHost.Android -t:Run` で確認できる
- **一時ファイルの観察**: 検証開始時点 (02:0x) の `git status` に 0 バイトの `ios/Sources/KsSettingsViewCore/Theme.swift` が untracked で存在していたが、検証終了時点では消えている。現在のワーキングツリーに残骸はない
- 開発者向け README (`maui/README.md`) は `DEVELOPER_DIR` 必須の旨を記載しており、今回のビルド失敗→成功の再現もその記述どおりだった
