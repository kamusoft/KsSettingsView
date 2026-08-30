# 一致検証結果: harden-update-accessory-unknown-id (001 回目)

**日付**: 2026-08-09
**判定**: VALID

デルタスペック 3 本 (`ios-store` / `android-store` / `maui-bridge`) の全 Requirement / Scenario と実装・テストの対応を突き合わせた。❌ は 0 件。

## 対応表

### specs/ios-store — ADDED: updateAccessory の未知 sectionID no-op

Requirement 本文の実装: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:274-292` (section 系 target のみ `updateSectionAccessory` の戻り値で guard、Root 系は素通しで `diffSubject.send`)。ヘルパの戻り値化は `:334-365`。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未知 sectionID の section header 更新は no-op | `SettingsRootStore.swift:282-284` | `ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift:398` | ✅ 一致 |
| 未知 sectionID の section footer 更新は no-op | `SettingsRootStore.swift:287-289` | `ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift:415` | ✅ 一致 |
| 既知 sectionID は header / footer とも従来どおり反映される | `SettingsRootStore.swift:282-291` | `ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift:237` | ✅ 一致 |
| Root 系 target は header / footer とも従来どおり Diff を発行する | `SettingsRootStore.swift:276-279, 291` | `ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift:258` | ✅ 一致 |
| Host 表示中の未知 sectionID 呼び出しは表示に影響しない | 同上 (Store が発行しないため Host 未到達) | `ios/Tests/KsSettingsViewUITests/SectionAccessoryRenderingTests.swift:456` | ✅ 一致 |

観測基準「現在状態ストリーム・Diff ストリームの両方が無発行」: `SettingsRootStoreTests.swift:398` / `:415` が `store.$root.dropFirst()` の件数と `diffPublisher` の件数を両方 0 で検証。

### specs/android-store — ADDED: updateAccessory の未知 sectionId no-op

Requirement 本文の実装: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt:244-267`。ヘルパの `Boolean` 戻り値化は `:288-315`。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 未知 sectionId の section header 更新は no-op | `SettingsRootStore.kt:249-256` | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStoreTest.kt:564` | ✅ 一致 |
| 未知 sectionId の section footer 更新は no-op | `SettingsRootStore.kt:257-264` | `.../SettingsRootStoreTest.kt:597` | ✅ 一致 |
| 既知 sectionId は header / footer とも従来どおり反映される | `SettingsRootStore.kt:249-266` | `.../SettingsRootStoreTest.kt:306` | ✅ 一致 |
| Root 系 target は header / footer とも従来どおり Diff を emit する | `SettingsRootStore.kt:246-248, 266` | `.../SettingsRootStoreTest.kt:348` | ✅ 一致 |
| strictMode 既定のまま未知 sectionId 呼び出しでも Host は沈黙しない | 同上 (Store が emit しないため `reportMissingId` 未到達) | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/UnknownSectionAccessoryHostTest.kt:145` (新規) | ✅ 一致 |

`strictMode = true` 維持・表示不変・後続 `replaceCell` の到達 (Diff 購読の生存) の 3 点をいずれも同テスト内で検証している。iOS 側 Scenario との対称性 (core/ADR-0018) も成立。

### specs/maui-bridge — MODIFIED: Store 操作 1:1 の更新 API

変更後の Requirement 本文が要求するのは「Bridge は Store 公開操作へ素通しし、Store の現行契約 (未知 ID no-op を含む) がそのまま適用される」こと。Bridge 実装 (`ios/Sources/KsSettingsViewBridge/KsSettingsBridge.swift:231-255` / `android/.../bridge/KsSettingsBridge.kt:275-304`) は無変更であり、素通し構造が維持されていることを確認した (`git status` 上、両 Bridge の main ソースに変更なし)。Root 系 target が `sectionID` 引数を参照しないことも実装で確認。

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell の構造操作が表示へ反映される | Bridge 無変更 | `ios/Tests/KsSettingsViewBridgeTests/KsBridgeUpdateTests.swift:18` / `android/.../KsBridgeUpdateTest.kt:32` | ✅ 一致 |
| Section の構造操作が表示へ反映される | Bridge 無変更 | `KsBridgeUpdateTests.swift:32` / `KsBridgeUpdateTest.kt:47` | ✅ 一致 |
| replaceCell は行の identity を維持する | Bridge 無変更 | `KsBridgeUpdateTests.swift:159` / `KsBridgeUpdateTest.kt:171` | ✅ 一致 |
| replaceCells は1バッチで反映される | Bridge 無変更 | `KsBridgeUpdateTests.swift:185` / `KsBridgeUpdateTest.kt:194` | ✅ 一致 |
| 未知 sectionID の updateAccessory は no-op | Store 側ガード (上記2表) が透過 | `KsBridgeUpdateTests.swift:283` / `KsBridgeUpdateTest.kt:312` (新規、canonical UUID を `KsBridgeFixture.unusedIdentifier()` で生成)。契約表にも `KsBridgeOperationContractTests.swift:335-362` / `KsBridgeOperationContractTest.kt:268-291` を追加 | ✅ 一致 |
| 全12操作が契約どおりに反映される | Bridge 無変更 | `KsBridgeOperationContractTests.swift:441` / `KsBridgeOperationContractTest.kt:353` | ✅ 一致 |

補足 (品質側の所見であり一致判定には影響しない): Android の契約表に追加した2ケースは Store の `diffs` を観測しないため本変更の回帰を検出しない (review-001.md の Minor 指摘)。当該 Scenario の担保は `KsBridgeUpdateTest.kt:312` が担っており、こちらはミューテーション検査で実際に落ちることを確認済み。

## 追加検査

- **tasks.md の虚偽チェック**: 3 セクション 7 タスクすべて `[x]`。上記対応表と突き合わせて未実装のチェック済みは **なし**
  - 1.1 / 2.1 の「実装箇所に `core/ADR-0020` コメントを付記」も `SettingsRootStore.swift:268-272` / `SettingsRootStore.kt:237-243` で履行済み
  - 3.1 の「Bridge 実装は素通しのまま変更しない」も `git status` で確認済み
- **逆流検査**: 足場 (`proposal.md` / `specs/*/spec.md`) は未 commit の新規ファイルのため git 履歴での差分比較は不可。代替として mtime を確認したところ、spec 3 本は 17:55、実装・テストは 18:04 以降で、実装開始後に足場が書き換えられた形跡はない (`tasks.md` の 18:15 はチェックボックス更新で正常)
- **未記録乖離**: ❌ が 0 件のため該当なし。`deviation.md` は存在せず、必要性もない
- **UI 変更**: 該当なし (`ui/` アーティファクトなし)
- **テスト実行**: レビュー側で実行して確認
  - iOS: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → **TEST SUCCEEDED / 646 tests / 0 failures**
  - Android: `ANDROID_HOME=... ./gradlew test --rerun-tasks` → **BUILD SUCCESSFUL / 2024 tests / 0 failures**

## 判定

**VALID** — 全 Requirement / Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、テスト全件成功。

(品質面の指摘は review-001.md を参照。判定は CHANGES_REQUESTED だが、いずれもデルタスペックの充足を損なうものではない。)
