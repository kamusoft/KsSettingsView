# Verify 001: clarify-host-attach-order-contract

- 検証日: 2026-08-08
- 検証対象: 未コミットの working tree 変更全体 (`git diff HEAD` + 未追跡ファイル)
- デルタスペック: `specs/ios-host/spec.md` (ADDED Requirement 1 / Scenario 6) / `specs/android-host/spec.md` (ADDED Requirement 1 / Scenario 2)
- deviation.md: 不在 (記録された乖離なし)

## 判定

**VALID**

全 8 Scenario が「✅ 一致」。虚偽チェックなし、逆流なし、テスト全件成功。

---

## 対応表: ios-host

### ADDED Requirement: view load 時の Store 現在状態からの復元

実装本体: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:351-385`
(`viewDidLoad` が `configureDataSource()` の直後に `resyncFromStore()` を呼び、`applyFullSnapshot` / `applyBackgroundColor` の前に接続中 Store の `root` / `theme` を取り込む)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| view load 前の構造操作が load 時に反映される | `KsSettingsViewController.swift:353` (`resyncFromStore`) / `:381-385` (`self.root = store.root`) | `ios/Tests/KsSettingsViewUITests/HostViewLoadRestoreTests.swift:96-134` | ✅ 一致 |
| view load 前の Cell 内容更新が load 時に反映される (`replaceCell` / `replaceCells` の両経路) | 同上 (`root` の pull で内容も取り込まれる) | `HostViewLoadRestoreTests.swift:138-158` (`replaceCell`) / `:160-184` (`replaceCells`) | ✅ 一致 |
| view load 前の Section accessory / theme 変更が load 時に反映される | `:381-385` (`root` + `currentTheme` の pull) / `:359` (`applyBackgroundColor(theme:)`) | `HostViewLoadRestoreTests.swift:188-223` | ✅ 一致 |
| Store 接続中の直接 `applyTheme` は view load 時に Store theme で上書きされる | `:384` (`self.currentTheme = store.theme` が無条件で上書き) | `HostViewLoadRestoreTests.swift:227-257` | ✅ 一致 |
| Root accessory は復元対象外で、所有者の再適用により反映される | `:381-385` に `rootHeader` / `rootFooter` の取り込みがない (意図的な除外。doc コメント `:370-372` で明記) | `HostViewLoadRestoreTests.swift:261-295` | ✅ 一致 |
| Store 非接続 init は従来どおり init 時の root で表示する | `:382` (`guard let store = connectedStore else { return }` の early return) | `HostViewLoadRestoreTests.swift:299-325` | ✅ 一致 |

**GIVEN/WHEN/THEN の表現**

- GIVEN「view 未 load」: 全テストが `XCTAssertFalse(controller.isViewLoaded)` で前提を明示的に固定している
- WHEN「`loadViewIfNeeded()` で view load」: 全テストが Store 操作の**後**に `loadViewIfNeeded()` を呼ぶ順序 (design Decision 4 の契約トリガーと一致)
- THEN「viewDidLoad 完了時点の表示」: 構造は `internalDataSource?.snapshot()` の item 数、表示は window に載せた実物の Cell (`cellForItem` → `titleLabel.text`) / supplementary / `UICollectionView.backgroundColor` で確認しており、内部状態の読み替えで済ませていない

### 個別の照合メモ

- Scenario 1 は Requirement 本文の「構造」を `insertCell` / `removeCell` / `replaceSection` の3操作すべてで動かしており、Scenario の WHEN 列挙を取りこぼしていない
- Scenario 3 の THEN は Section accessory (`visibleHeaderLabel`) と theme の2系統 (背景色 `cv.backgroundColor` + Cell 描画色 `titleLabel.textColor`) の両方を見ている
- Scenario 5 の再適用は `store.updateAccessory` の再発行で表現されており、Requirement の「所有者 (呼び出し側) が view load 後に適用する」経路と一致する

## 対応表: android-host

### ADDED Requirement: window attach 時の Store 現在状態からの復元

Requirement 本文が「現行実装が既に満たす挙動の契約化であり、実装変更を伴わない (回帰テストで固定する)」と明記しており、実装変更がないことが契約どおり。
実装本体 (既存): `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:239-260` (`onAttachedToWindow` → `resyncFromStore(store)`) / `:333`

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| attach 前の更新が attach 後に反映される | `KsSettingsView.kt:239-260` (既存・変更なし) | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/AttachOrderRestoreTest.kt:188-263` | ✅ 一致 |
| detach 中の更新が再 attach で反映される | 同上 | `AttachOrderRestoreTest.kt:265-305` | ✅ 一致 |

**GIVEN/WHEN/THEN の表現**

- GIVEN「bind 済みで未 attach」: `KsSettingsView(activity)` → `bind(store)` を `container.addView` の前に実施。さらに attach 前に「更新が Host に届いていない」ことを対照アサーション (`AttachOrderRestoreTest.kt:231-232`) で固定しており、収束が attach 時の再取り込みによることを示している
- WHEN: Scenario 1 は構造 (`insertSection` / `insertCell` / `removeCell`)・Section accessory・`replaceCells` バッチ・`applyTheme` をひととおり適用。Scenario 2 は detach 中に `replaceCell` (単数) と `replaceCells` (バッチ) の両方を適用
- THEN「メインスレッドのキューが空になった時点」(収束の観測境界): `awaitConvergence` が `shadowOf(Looper.getMainLooper()).idle()` を回しながら条件成立を待ち、時間切れは明示的に `fail` させる。黙って「収束前の状態」を検証したことにならない設計になっている (design Decision 3 と一致)
- Requirement 本文の復元対象4種のうち、構造・Cell 内容・Section accessory は `visibleRowTexts` (実際に生成された RecyclerView の行) で、theme は `internalTheme` + RecyclerView 背景色 + `ItemDecoration` + 表示中 Cell の文字色で確認している
- Root header / footer が対象外である点も、テストの検証対象から除外することで契約と整合している (`AttachOrderRestoreTest.kt:43-44` のクラス doc に明記)

## 追加検査

| 検査項目 | 結果 |
|---|---|
| tasks.md の全タスク完了 | ✅ 1.1 / 1.2 / 1.3 / 2.1 / 3.1 および実装前ゲートすべて `[x]` |
| 虚偽チェック (未実装なのにチェック済み) | ✅ なし。全タスクに対応する実装・テスト・証跡を上表で特定済み |
| 逆流検査 (足場アーティファクトの書き換え) | ✅ なし。`proposal.md` / `design.md` / `specs/` に未コミット差分ゼロ (`git diff HEAD --stat` が空)。`git log -- kasane/changes/clarify-host-attach-order-contract/` も提案作成コミット `bb37110` のみで、実装期間中の書き換えコミットなし。`tasks.md` の差分はチェックボックスの `[ ]`→`[x]` のみで本文の書き換えなし |
| 未記録乖離 | ✅ なし (対応表に ❌ が1件もないため、deviation.md 不在と整合) |
| UI 変更 (ui/ アーティファクト) | 対象外。本変更に `ui/` はなく、デルタスペックにも視覚パラメータの記述なし (UI lint 違反なし) |
| テスト全件成功 | ✅ 後述 |

### タスク 3.1 (検証ホスト E2E) の証跡確認

デルタスペックの Scenario ではないが tasks.md の完了主張として確認した。

- 実装: `maui/tests/KsSettingsView.IntegrationHost.iOS/AppDelegate.cs` / `maui/tests/KsSettingsView.IntegrationHost.Android/MainActivity.cs` から回避策 (「取り付け → 操作」) が外れ、両 OS とも「Host 生成 → `KsBridgeScenario.Apply` → 取り付け」の順序になっている
- Root header / footer は `KsBridgeScenario.ApplyRootAccessory` (`maui/tests/shared/KsBridgeScenario.cs:108-121`) へ分離され、取り付け・view 構築後に呼ばれる (Decision 1 の所有者責務)
- 証跡: `kasane/changes/clarify-host-attach-order-contract/verification/` の4点。`02` (iOS) と `04` (Android) を実見し、`maui/README.md:79-87` の「期待される表示」表 (root header / 3 Section / 同期=無効 / root footer / Section header が緑) と一致することを確認した。`03` は Root accessory が復元対象外であることの対照 (root header / footer のみが欠けた表示) で、README の記述どおり
- 合否基準「事前操作後の Section / Cell 列と theme が表示され、再適用後に Root header / footer が表示される」を満たす

### テスト実行結果

`kasane/concepts/cross/conventions/test-execution.md` の規約に従い、絞り込みなしの全件実行で確認した。

| platform | コマンド | 結果 |
|---|---|---|
| iOS | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17'` (`DEVELOPER_DIR=/Applications/Xcode-26.1.1.app/Contents/Developer`) | **Executed 444 tests, with 0 failures** / `** TEST SUCCEEDED **` |
| Android | `./gradlew test --rerun-tasks` (`ANDROID_HOME` 指定) | **1990 tests / 0 failures / 0 skipped** (debug + release の両 variant。`build/test-results/*/*.xml` 集計) / `BUILD SUCCESSFUL` |

新規テストの内訳:

- `HostViewLoadRestoreTests` 7件すべて passed (iOS)
- `AttachOrderRestoreTest` 2件 × 2 variant すべて passed (Android)

## 注記 (判定に影響しない所見)

- iOS Scenario「Root accessory は復元対象外で…」の spec 文言は「view load 直後の root header 表示は**保証されず**」(非保証) だが、テストは `XCTAssertNil` で「復元されない」ことを固定している。契約より強い固定であり違反ではないが、将来 Root accessory を復元対象へ移す変更をした場合、契約より先にこのテストが落ちる点は認識しておくとよい
