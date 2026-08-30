# Candidate: docs platform guide sweep

## 概念候補

### Android composite build の SDK 解決境界（合流先提案: `architecture/repository-boundaries.md`）

`samples/android/` は単一 Gradle build の subproject ではなく、`samples/android/settings.gradle.kts` から `../../android` を `includeBuild` する consumer 側の build root である。Sample と library の両 build が Android Gradle Plugin を初期化するため、Android SDK の場所は一方の root だけでなく両方から解決できなければならない。

#### 目的・責務境界

- `samples/android/` は公開 module 座標を Gradle composite build の dependency substitution で `android/` の project へ置換する。
- composite build は source 参照を共有するが、Android SDK の環境解決まで親 build から included build へ継承する契約ではない。
- SDK の在り処はリポジトリへ固定値として記録せず、各 build root の `local.properties` または開発環境の `ANDROID_HOME` で解決する。

#### 保証すること

- `samples/android/` と `android/` を独立した Android build root として扱い、どちらから開始しても SDK の場所を解決できる開発環境にする。
- Sample は `jp.kamusoft.kssettingsview:*` の公開座標を使い、composite build が開発中の library project へ置換する consumer 境界を維持する。

#### してはいけないこと

- `samples/android/local.properties` の設定だけで、included build の `android/` も必ず SDK を解決できると仮定しない。
- 個人環境の `sdk.dir` を長命文書や version 管理対象の固定パスとして記録しない。

価値 lint: Sample build の入口で毎回必要になる一方、失敗原因は二つの独立 build root と composite build の関係を読まないと再導出しづらい。個別の SDK 絶対パスは高腐食なので残さず、解決境界だけを記載する。

出典: `samples/android/settings.gradle.kts`（`pluginManagement.includeBuild("../../android")`、`includeBuild("../../android")` と dependency substitution） / `samples/android/app/build.gradle.kts` / `android/settings.gradle.kts` / `docs/platform-guide-android.md` §13

## ADR 候補

なし。

- Android Host が XML Theme に `Theme.Material3.*` を要求する制約は、`platforms/android-native-host.md` と `styling/style-resolution.md` に回収済みであり、Batch D で ADR 化を deferred として扱っているため重複起票しない。
- composite build ごとの SDK 解決は開発環境の入口に関する運用知識であり、library architecture や公開 API の将来を制約する決定ではない。

## drift 所見

1. **両 platform guide が凍結済み OpenSpec spec を「正本」と案内している。** 現在の SSoT はコードとテストであり、長命な利用知識の入口は `kasane/concepts/index.md` である。`openspec/` は歴史資料として凍結されている。
   出典: `docs/platform-guide-ios.md` 冒頭・「関連 spec」 / `docs/platform-guide-android.md` 冒頭・「関連 spec」 ↔ `AGENTS.md` / `kasane/config.yaml` / `kasane/concepts/index.md`

2. **iOS のクイックスタートと基本 Cell 例は現行 API では build できない。** `SwitchCell("...", isOn: $state)`、`CheckboxCell("...", isChecked: $state)`、`RadioCell(... selectedValue: $state)`、`SimpleCheckCell(... isChecked: $state)` という `Binding` initializer は現行の基本 Cell 4種に存在しない。基本 Cell は値と callback を渡す。さらに `import SwiftUI` と併用する bare `Section` は名前衝突し得るため、現行 guide は `ksSection` / `KsSection` を案内すべきである。
   出典: `docs/platform-guide-ios.md` §1・§3・§4 ↔ `ios/Sources/KsSettingsViewUI/SwitchCell.swift` / `CheckboxCell.swift` / `RadioCell.swift` / `SimpleCheckCell.swift` / `ios/Sources/KsSettingsViewSwiftUI/SectionBuilder.swift` / `kasane/concepts/cells/basic-cells.md`

3. **iOS guide の「操作後は値が一致するため `.replaceCell` を発行しない」という説明がコード・テストと逆である。** 外部 state が変わると、前回 tree と新 tree の同一 ID Cell に内容差が生じ、iOS は `.replaceCell` を発行する。無限ループを避ける根拠を「内容更新が発行されないこと」に置いてはならない。
   出典: `docs/platform-guide-ios.md` §4 ↔ `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift` / `ios/Tests/KsSettingsViewSwiftUITests/KsSettingsViewDSLIntegrationTests.swift`（Cell 内容変更） / `ios/Tests/KsSettingsViewSwiftUITests/DSLDiffCalculatorTests.swift`

4. **両 guide が `.disabled(true)` を有効な Cell modifier として chain 例へ含めているが、現行実装は常に no-op である。** 無効化は各 Cell initializer の `isEnabled` を使う。
   出典: `docs/platform-guide-ios.md` §9 / `docs/platform-guide-android.md` §9 ↔ `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift` / `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/CellModifiers.kt` / `DSLHandles.kt` / `kasane/concepts/styling/cell-visual-states.md`

5. **両 guide の identity 優先順位は安全な現行利用契約ではない。** guide は collection key を第1、明示 ID を第2と断定するが、accepted ADR と現行 iOS / Android 実装で併用時の優先順位に drift がある。利用者向け契約は「同じ要素で collection key と明示 ID を併用しない」である。
   出典: `docs/platform-guide-ios.md` §6 / `docs/platform-guide-android.md` §6 ↔ `kasane/concepts/architecture/declarative-tree-identity.md` / `kasane/concepts/platforms/ios-swiftui.md` / `kasane/concepts/platforms/android-compose.md` / `kasane/decisions/0008-stable-declarative-tree-identity.md`

6. **iOS guide は利用者定義 Renderer に internal の `KsListCellBase` 継承を推奨している。** 外部 module から継承できないため、公開 `UICollectionViewCell` subclass として `KsCellRenderer` に準拠する必要がある。また `samples/ios/` に利用者定義 Renderer の実装例はない。
   出典: `docs/platform-guide-ios.md` §10 ↔ `ios/Sources/KsSettingsViewUI/KsListCellBase.swift` / `KsCellRenderer.swift` / `KsCellRegistry.swift` / `kasane/concepts/architecture/cell-renderer-registry.md`

7. **Android guide の利用者定義 Cell DSL 例は、再評価ごとに random ID を生成しながら `DSLReidentifiableCell` を要求しておらず、identity が安定しない。** `cellID` hint を Cell の実 ID へ反映するには `DSLReidentifiableCell` と copy API が必要であり、非準拠 Cell は自身で安定 ID を維持しなければならない。
   出典: `docs/platform-guide-android.md` §10 ↔ `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLNodes.kt` / `CellModifiers.kt` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/DSLCellIdentity.kt` / `kasane/concepts/platforms/android-compose.md`

8. **Android Store 方式の例とメソッド一覧で挿入位置の引数名が古い。** guide は `insertCell(..., index = 0)` と `insertSection(section, index)` / `insertCell(cell, sectionId, index)` を示すが、現行 public API は `at` である。
   出典: `docs/platform-guide-android.md` §11 ↔ `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt`

9. **両 guide の Sample 収録画面一覧が現行 menu と一致しない。** iOS は Store 方式・入力 Cell 5種を含む現行 destination を一覧へ反映しておらず、Android は入力 Cell 5種デモを落としている。画面数や表示文字列は concept 契約にはしないが、guide の実行手順としては drift である。
    出典: `docs/platform-guide-ios.md` §13 ↔ `samples/ios/KsSettingsViewSample/ContentView.swift` / `docs/platform-guide-android.md` §14 ↔ `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt`

10. **Android guide は DSL 再評価結果を一括して `SettingsRootDiff` 経由と説明しているが、同一 ID Cell の内容更新は別の batch stream である。** 構造 Diff は `SettingsRootDiff`、内容変更は `DSLDiffCalculator.contentUpdates` → `SettingsRootStore.replaceCells` → `contentUpdateBatches` を通る。
    出典: `docs/platform-guide-android.md` §2 ↔ `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` / `DSLDiffCalculator.kt` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SettingsRootStore.kt` / `kasane/concepts/architecture/display-state-synchronization.md`

## 用語

- **build root**: ecosystem の build と test を独立して開始する入口。Android library は `android/settings.gradle.kts`、Android Sample は `samples/android/settings.gradle.kts`。
- **composite build**: 独立した Gradle build を `includeBuild` で組み合わせ、module 座標を included project へ置換する仕組み。
- **SDK 解決境界**: 各 Android build root が Android SDK の場所を自身の環境から解決する責務の境目。

## 抽出メモ

- 新しい独立 concept は不要。概念候補1件は既存 `architecture/repository-boundaries.md` の「Sample の consumer 境界」へ短く合流させる粒度が適切である。
- Store / DSL の選択、Native Host への収束、任意 View Accessory の比較不能性、Theme / CellStyle、Registry、Sample の非 SSoT 性は、現行 `platforms/`、`architecture/`、`cells/`、`styling/`、`core-model/settings-tree.md` に回収済みである。
- guide にある画面構成、デモ文字列、生の色値、個々の initializer / method の網羅一覧は腐りやすく、概念候補へ写さない。
- Android の `Theme.Material3.*` 前提は既存 concepts に回収済みで、ADR 化は既存 deferred を維持する。
