## 依存関係

- 先行 archive: `add-monorepo-foundation`、`add-settings-view-core`
- 並行作業: 本提案単体での archive は推奨されず、`add-partial-update-native` と同時 archive を行う（既存サンプルコードが `SettingsRoot.header/footer` 削除で破壊されるため）

## 完了条件

- 全タスクのチェックボックスが完了している
- `swift test`（iOS Core）が成功する
- `./gradlew :ks-settingsview-core:test`（Android Core）が成功する
- spec.md の全 Scenario に対応するテストが存在する

## 1. iOS Core: SettingsRoot から header/footer 削除

- [x] 1.1 [SettingsRoot.swift](ios/Sources/KsSettingsViewCore/SettingsRoot.swift) から `header: RootAccessory?` プロパティを削除する
- [x] 1.2 [SettingsRoot.swift](ios/Sources/KsSettingsViewCore/SettingsRoot.swift) から `footer: RootAccessory?` プロパティを削除する
- [x] 1.3 `SettingsRoot` の `init` から `header` / `footer` 引数を削除する
- [x] 1.4 `SettingsRoot` の `Hashable` 手動実装から `header` / `footer` のハッシュ計算を削除する（`sections` と `theme` のみで等価性判定）
- [x] 1.5 `SettingsRootBuilder`（@resultBuilder）の `header` / `footer` 受け取りロジックを削除する
- [x] 1.6 既存テスト `SettingsRootTest`（または相当）の Root H/F 関連 Scenario を削除する

## 2. iOS Core: SettingsRootDiff 型の新規追加

- [x] 2.1 新規ファイル `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift` を作成する
- [x] 2.2 `public enum SettingsRootDiff: Hashable` を定義する
- [x] 2.3 `case full(SettingsRoot)` を実装する
- [x] 2.4 `case insertSection(at: Int, section: Section)` を実装する
- [x] 2.5 `case removeSection(sectionID: UUID)` を実装する
- [x] 2.6 `case moveSection(from: Int, to: Int)` を実装する
- [x] 2.7 `case replaceSection(sectionID: UUID, new: Section)` を実装する
- [x] 2.8 `case insertCell(sectionID: UUID, at: Int, cell: any KsCell)` を実装する（`any KsCell` の Hashable 取り扱いに注意）
- [x] 2.9 `case removeCell(cellID: KsCellID)` を実装する
- [x] 2.10 `case replaceCell(cellID: KsCellID, new: any KsCell)` を実装する
- [x] 2.11 `case moveCell(cellID: KsCellID, to: Int)` を実装する
- [x] 2.12 `case updateAccessory(target: AccessoryTarget, accessory: SettingsAccessory?)` を実装する
- [x] 2.13 `case updateTheme(Theme)` を実装する
- [x] 2.14 `any KsCell` を含むケースの手動 `Hashable` 実装（`AnyHashable` 経由などで Cell の hash を取り込む）

## 3. iOS Core: AccessoryTarget 型の新規追加

- [x] 3.1 新規ファイル `ios/Sources/KsSettingsViewCore/AccessoryTarget.swift` を作成する
- [x] 3.2 `public enum AccessoryTarget: Hashable` を定義する
- [x] 3.3 `case rootHeader` / `case rootFooter` を実装する
- [x] 3.4 `case sectionHeader(sectionID: UUID)` / `case sectionFooter(sectionID: UUID)` を実装する

## 4. iOS Core: SettingsAccessory 型の新規追加

- [x] 4.1 新規ファイル `ios/Sources/KsSettingsViewCore/SettingsAccessory.swift` を作成する
- [x] 4.2 `public enum SettingsAccessory: Hashable` を定義する
- [x] 4.3 `case root(RootAccessory)` を実装する
- [x] 4.4 `case section(SectionAccessory)` を実装する
- [x] 4.5 `Hashable` 実装が `RootAccessory` / `SectionAccessory` の既存実装（`.view` ケースは中身を hash しない）を継承することを確認する

## 5. iOS Core: ユニットテスト

- [x] 5.1 新規テスト `ios/Tests/KsSettingsViewCoreTests/SettingsRootDiffTests.swift` を作成する
- [x] 5.2 `SettingsRootDiff` の全 11 ケースの生成・payload 取り出しテストを追加する
- [x] 5.3 `SettingsRootDiff` の等価性テスト(同一ケース同一 payload は等価)を追加する
- [x] 5.4 新規テスト `ios/Tests/KsSettingsViewCoreTests/AccessoryTargetTests.swift` を作成する
- [x] 5.5 `AccessoryTarget` の全 4 ケースの等価性・hashValue テストを追加する
- [x] 5.6 新規テスト `ios/Tests/KsSettingsViewCoreTests/SettingsAccessoryTests.swift` を作成する
- [x] 5.7 `SettingsAccessory.root` / `SettingsAccessory.section` の等価性テストを追加する
- [x] 5.8 既存 `SettingsRootTest` を「sections + theme のみで等価性判定」になるよう修正する
- [x] 5.9 `swift test` 実行で全テストが成功することを確認する

## 6. Android Core: SettingsRoot から header/footer 削除

- [x] 6.1 [SettingsRoot.kt](android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt) から `header: RootAccessory?` プロパティを削除する
- [x] 6.2 [SettingsRoot.kt](android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt) から `footer: RootAccessory?` プロパティを削除する
- [x] 6.3 `SettingsRoot` を `data class SettingsRoot(val sections: List<Section>, val theme: Theme)` に修正する
- [x] 6.4 `SettingsRootBuilder` DSL の `header` / `footer` 受け取りロジックを削除する
- [x] 6.5 既存テスト `SettingsRootTest` の Root H/F 関連 Scenario を削除する

## 7. Android Core: SettingsRootDiff 型の新規追加

- [x] 7.1 新規ファイル `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt` を作成する
- [x] 7.2 `sealed interface SettingsRootDiff` を定義する
- [x] 7.3 `data class Full(val root: SettingsRoot) : SettingsRootDiff` を実装する
- [x] 7.4 `data class InsertSection(val index: Int, val section: Section) : SettingsRootDiff` を実装する
- [x] 7.5 `data class RemoveSection(val sectionId: String) : SettingsRootDiff` を実装する
- [x] 7.6 `data class MoveSection(val from: Int, val to: Int) : SettingsRootDiff` を実装する
- [x] 7.7 `data class ReplaceSection(val sectionId: String, val newSection: Section) : SettingsRootDiff` を実装する
- [x] 7.8 `data class InsertCell(val sectionId: String, val index: Int, val cell: Cell) : SettingsRootDiff` を実装する
- [x] 7.9 `data class RemoveCell(val cellId: String) : SettingsRootDiff` を実装する
- [x] 7.10 `data class ReplaceCell(val cellId: String, val newCell: Cell) : SettingsRootDiff` を実装する
- [x] 7.11 `data class MoveCell(val cellId: String, val toIndex: Int) : SettingsRootDiff` を実装する
- [x] 7.12 `data class UpdateAccessory(val target: AccessoryTarget, val accessory: SettingsAccessory?) : SettingsRootDiff` を実装する
- [x] 7.13 `data class UpdateTheme(val theme: Theme) : SettingsRootDiff` を実装する

## 8. Android Core: AccessoryTarget 型の新規追加

- [x] 8.1 新規ファイル `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/AccessoryTarget.kt` を作成する
- [x] 8.2 `sealed interface AccessoryTarget` を定義する
- [x] 8.3 `object RootHeader : AccessoryTarget` / `object RootFooter : AccessoryTarget` を実装する
- [x] 8.4 `data class SectionHeader(val sectionId: String) : AccessoryTarget` を実装する
- [x] 8.5 `data class SectionFooter(val sectionId: String) : AccessoryTarget` を実装する

## 9. Android Core: SettingsAccessory 型の新規追加

- [x] 9.1 新規ファイル `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsAccessory.kt` を作成する
- [x] 9.2 `sealed interface SettingsAccessory` を定義する
- [x] 9.3 `data class Root(val accessory: RootAccessory) : SettingsAccessory` を実装する
- [x] 9.4 `data class Section(val accessory: SectionAccessory) : SettingsAccessory` を実装する
- [x] 9.5 `RootAccessory` / `SectionAccessory` の既存 `equals` / `hashCode`（`.view` ケースは中身を hash しない）が継承されることを確認する

## 10. Android Core: ユニットテスト

- [x] 10.1 新規テスト `android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiffTest.kt` を作成する
- [x] 10.2 `SettingsRootDiff` の全 11 ケースの生成・payload 取り出しテストを追加する
- [x] 10.3 `SettingsRootDiff` の等価性テストを追加する
- [x] 10.4 新規テスト `android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/AccessoryTargetTest.kt` を作成する
- [x] 10.5 `AccessoryTarget` の全 4 ケースの等価性・hashCode テストを追加する
- [x] 10.6 新規テスト `android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/SettingsAccessoryTest.kt` を作成する
- [x] 10.7 `SettingsAccessory.Root` / `SettingsAccessory.Section` の等価性テストを追加する
- [x] 10.8 既存 `SettingsRootTest` を「sections + theme のみで等価性判定」になるよう修正する
- [x] 10.9 `./gradlew :ks-settingsview-core:test` 実行で全テストが成功することを確認する

## 11. 整合性確認

- [x] 11.1 iOS / Android の `SettingsRootDiff` ケース構成が一致していることを確認する
- [x] 11.2 iOS / Android の `AccessoryTarget` ケース構成が一致していることを確認する
- [x] 11.3 iOS / Android の `SettingsAccessory` ケース構成が一致していることを確認する
- [x] 11.4 旧 `SettingsRoot.header/footer` を参照する既存コードが Core モジュール内に残っていないことを確認する
- [x] 11.5 spec.md の全 Scenario に対応するテストが存在することを確認する
