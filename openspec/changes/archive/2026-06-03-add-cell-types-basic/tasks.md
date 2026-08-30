## 1. 共通：KsImage 値型の追加

- [x] 1.1 iOS `KsSettingsViewCore` に `public struct KsImage(name: String?, url: URL?, systemName: String?): Hashable` を追加
- [x] 1.2 Android `ks-settingsview-core` に `data class KsImage(val name: String?, val url: String?, val systemName: String?)` を追加

## 1.5. 共通：具象 Cell の id デフォルト値規約と DSL 拡張関数（add-declarative-dsl 連動）

`add-declarative-dsl` のオーナーレビューで確定した規約を本提案で追加する具象 Cell 7 種すべてに適用する。

- [x] 1.5.1 各 iOS Cell struct（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）の `id` パラメータに `id: UUID = UUID()` デフォルト値を持たせる
- [x] 1.5.2 各 Android Cell data class（同上）の `id` パラメータに `id: String = "<className>-${java.util.UUID.randomUUID()}"` デフォルト値を持たせる
- [x] 1.5.3 各 Android Cell data class（`ks-settingsview-ui` モジュール配置）に `DSLReidentifiableCell` / `DSLStyleModifiableCell` 規約準拠の実装（`withDSLId(...)` / `withDSLStyle(...)`）を持たせる
  - **モジュール依存に関する前提**: `add-declarative-dsl` Section 25.0 で `DSLReidentifiableCell` / `DSLStyleModifiableCell` interface は `ks-settingsview-core` モジュール（パッケージ `jp.kamusoft.kssettingsview.core`）に移動済み。本タスクは Core 版の interface を import して implement する（`ks-settingsview-ui → ks-settingsview-compose` の循環依存にならない）
- [x] 1.5.4 各 iOS Cell struct（`KsSettingsViewUI` モジュール配置）に `DSLReidentifiable` / `DSLStyleModifiable` 規約準拠の実装（`withDSLID(_:)` / `withStyle(_:)`）を持たせる
  - **モジュール依存に関する前提**: `add-declarative-dsl` Section 25.0 で `DSLReidentifiable` / `DSLStyleModifiable` protocol は `KsSettingsViewCore` モジュールに移動済み。本タスクは Core 版の protocol を import して準拠する（`KsSettingsViewUI → KsSettingsViewSwiftUI` の循環依存にならない）
- [x] 1.5.5 各 Android Cell に対する `DSLSectionScope` 拡張関数を新規追加（DSL 直置き対応）：
  - `fun DSLSectionScope.LabelCell(title: String, description: String? = null, valueText: String? = null, icon: KsImage? = null, hintText: String? = null, style: CellStyle = CellStyle()): CellHandle`
  - `fun DSLSectionScope.CommandCell(title: String, ..., onTap: (() -> Unit)? = null): CellHandle`
  - `fun DSLSectionScope.ButtonCell(title: String, titleColor: KsColor? = null, onTap: (() -> Unit)? = null): CellHandle`
  - `fun DSLSectionScope.SwitchCell(title: String, isOn: MutableState<Boolean>, ...): CellHandle`
  - `fun DSLSectionScope.CheckboxCell(...): CellHandle`
  - `fun DSLSectionScope.RadioCell(...): CellHandle`
  - `fun DSLSectionScope.SimpleCheckCell(...): CellHandle`
  - 配置先は `ks-settingsview-compose` モジュール、`<CellName>Dsl.kt` 単位（または `BasicCellDsl.kt` 集約）
- [x] 1.5.6 各 DSL 拡張関数のユニットテスト：DSL 内呼び出しで Cell が正しく `DSLCellNode` に格納され、戻り値 `CellHandle` に対する `.cellHeight(...)` / `.cellID(...)` chain が動作することを検証

## 2. iOS LabelCell

- [x] 2.1 `LabelCell.swift` で `public struct LabelCell: KsCell, Hashable` を実装（`title`、`description?`、`valueText?`、`icon: KsImage?`、`hintText?`、`id`、`style`）
- [x] 2.2 `LabelCellView.swift` で `final class ... UICollectionViewCell, KsCellRenderer` を実装
- [x] 2.3 ユニットテスト：bind、Theme/CellStyle 適用、prepareForReuse でのクリア

## 3. iOS CommandCell

- [x] 3.1 `CommandCell.swift` を実装（`LabelCell` フィールド + `onTap: (() -> Void)?`、`hideArrow: Bool` デフォルト false）
- [x] 3.2 `CommandCellView.swift` を実装（Disclosure Indicator + Cell 全体タップで onTap 発火）
- [x] 3.3 ユニットテスト：onTap 発火検証、Disclosure 表示

## 4. iOS ButtonCell

- [x] 4.1 `ButtonCell.swift` を実装（`title`、`titleColor?`、`onTap`）
- [x] 4.2 `ButtonCellView.swift` を実装（中央寄せタイトル、Disclosure 非表示）
- [x] 4.3 ユニットテスト

## 5. iOS SwitchCell

- [x] 5.1 `SwitchCell.swift` を実装（`title`、`description?`、`isOn`、`accentColor?`、`onValueChanged: ((Bool) -> Void)?`）
- [x] 5.2 `SwitchCellView.swift` を実装（右側に UISwitch、valueChanged で onValueChanged 呼び出し）
- [x] 5.3 ユニットテスト：UISwitch.setOn(true) → onValueChanged 呼び出し、reuse 時の listener クリア

## 6. iOS CheckboxCell

- [x] 6.1 `CheckboxCell.swift` を実装（`title`、`description?`、`isChecked`、`accentColor?`、`onValueChanged`）
- [x] 6.2 `CheckboxCellView.swift` を実装（右端にチェックマーク、Cell 全体タップで toggle）
- [x] 6.3 ユニットテスト

## 7. iOS RadioCell

- [x] 7.1 `RadioCell.swift` を実装（`title`、`groupId`、`value`、`selectedValue`、`onSelected: ((String) -> Void)?`）
- [x] 7.2 `RadioCellView.swift` を実装（value == selectedValue でチェック表示、タップで onSelected 発火）
- [x] 7.3 ユニットテスト

## 8. iOS SimpleCheckCell

- [x] 8.1 `SimpleCheckCell.swift` を実装（`title`、`isChecked`、`onValueChanged`）
- [x] 8.2 `SimpleCheckCellView.swift` を実装（左端にチェック）
- [x] 8.3 ユニットテスト

## 9. iOS 一括登録

- [x] 9.1 `KsCellRegistry+BasicCells.swift` で `extension KsCellRegistry { public func registerBasicCells() }` を実装
- [x] 9.2 `KsSettingsViewController.init` でデフォルト registry に対し `registerBasicCells()` を自動呼び出し（オプトアウト可能）

## 10. iOS PoC Cell の削除

- [x] 10.1 `PoCLabelCell.swift` を削除
- [x] 10.2 `PoCLabelCellView.swift` を削除
- [x] 10.3 関連テスト・登録コードを削除し、`LabelCell` で置換

## 11. Android LabelCell

- [x] 11.1 `LabelCell.kt` で `data class LabelCell(...) : Cell` を実装
- [x] 11.2 `LabelCellViewHolder.kt` で `CellViewHolder<LabelCell>` を実装
- [x] 11.3 レイアウトは Kotlin から動的構築（リソース依存を避けライブラリ移植性を優先。XML 化は後続の改良で検討）
- [x] 11.4 ユニットテスト（Robolectric）

## 12. Android CommandCell

- [x] 12.1 `CommandCell.kt` を実装（`LabelCell` フィールド + `onTap: (() -> Unit)?`、`hideArrow: Boolean = false`）
- [x] 12.2 `CommandCellViewHolder.kt`（レイアウトは Kotlin 動的構築、Disclosure 表示）を実装
- [x] 12.3 ユニットテスト

## 13. Android ButtonCell

- [x] 13.1 `ButtonCell.kt` を実装
- [x] 13.2 `ButtonCellViewHolder.kt`（レイアウトは Kotlin 動的構築、中央寄せ）を実装
- [x] 13.3 ユニットテスト

## 14. Android SwitchCell

- [x] 14.1 `SwitchCell.kt` を実装
- [x] 14.2 `SwitchCellViewHolder.kt` で `SwitchCompat` の `setOnCheckedChangeListener` を bind 内で設定し、reset 内で null 化
- [x] 14.3 ユニットテスト：state 変更 → onValueChanged 呼び出し

## 15. Android CheckboxCell / RadioCell / SimpleCheckCell

- [x] 15.1 `CheckboxCell.kt`、`CheckboxCellViewHolder.kt`（レイアウトは Kotlin 動的構築）
- [x] 15.2 `RadioCell.kt`、`RadioCellViewHolder.kt`（レイアウトは Kotlin 動的構築）
- [x] 15.3 `SimpleCheckCell.kt`、`SimpleCheckCellViewHolder.kt`（レイアウトは Kotlin 動的構築）
- [x] 15.4 各々のユニットテスト

## 16. Android 一括登録

- [x] 16.1 `KsCellRegistryBasicCells.kt`（拡張関数）で `fun KsCellRegistry.registerBasicCells(context: Context)` を実装
- [x] 16.2 `KsSettingsView.init` でデフォルト registry に対し自動登録（既登録時は再登録しない）

## 17. Android PoC Cell の削除

- [x] 17.1 `PocLabelCell.kt` を削除
- [x] 17.2 `PocLabelCellViewHolder.kt` を削除
- [x] 17.3 登録コード・テストを `LabelCell` ベースで置換（`KsCellRegistry.VIEW_TYPE_POC` 定数も削除）

## 18. Sample 更新

> **前提**: `samples/ios/` / `samples/android/` の Sample アプリ土台は別変更提案 `add-samples-ios` / `add-samples-android` で整備される。本セクションのタスクはそれらの archive 完了後に着手する。
>
> **MAUI Sample への展開**: 本提案では行わない。`samples/maui/` への 7 種基本 Cell ページ追加は別変更提案 `add-maui-cells`（旧 `add-maui-bindings`）の責務であり、本提案は Native iOS / Android のみを対象とする。

- [x] 18.1 `samples/ios/` の SwiftUI Sample に 7 種の基本 Cell を表示するページを追加（既存 `MainPage` 等にナビゲーション導線も追加。詳細構造は `add-samples-ios` で整備済みの起点ページに合わせる）
  - **add-declarative-dsl 連動**: Cell 配置は新 DSL 形式で記述（`Section("...") { LabelCell(title: "...") }` のように Cell を直置き、`id` 引数省略、Section H/F は `.sectionFooter(...)` modifier chain）
- [x] 18.2 `samples/android/` の Compose Sample に 7 種の基本 Cell を表示するページを追加
  - **add-declarative-dsl 連動**: Cell 配置は新 DSL 形式で記述（`Section("...") { LabelCell(title = "...") }` のように 1.5.5 で追加した DSL 拡張関数で直置き、`cell(...)` ラップ不要、`id` 引数省略、Section H/F は `SectionHandle.sectionFooter(...)` modifier chain）
  - 動的コレクションを使う場合は `data class Item(override val id: Int, ...) : KsIdentifiable` を実装して `forEach(items) { item -> ... }`（`key` 省略版）を利用する例を含める

## 18.5 Sample 既存 SampleLabelCell の LabelCell 置換と削除

> 本 §18.1 / §18.2 完了後の動作確認で、Sample 側 `SampleLabelCell` が登録する viewType (Android: 100) と
> `registerBasicCells` が登録する `LabelCell` の viewType が競合し起動時クラッシュすることが判明。
> 加えて iOS 側で `BasicCellsDemoView.swift` が Xcode プロジェクトに未登録のためビルドエラーとなることも判明。
> `LabelCell` が public 化された本提案の方針（PoC Cell REMOVED 時の Migration: 「テスト・サンプルは LabelCell に置き換える」）に沿って、
> Sample 専用 `SampleLabelCell` 一族を削除し、本体 `LabelCell` に置換する。

- [x] 18.5.1 iOS: `samples/ios/KsSettingsViewSample/{SampleLabelCell,SampleLabelCellView,SampleLabelCellPreview}.swift` を `trash` で削除
- [x] 18.5.2 iOS: `ContentView.swift` / `DSLDemoView.swift` / `KsSettingsViewSampleApp.swift` で `SampleLabelCell` を `LabelCell` に置換、Sample 独自 Renderer 登録 (`KsCellRegistry.shared.register(cellType: SampleLabelCell.self, ...)`) を削除
- [x] 18.5.3 iOS: `KsSettingsViewSample.xcodeproj/project.pbxproj` から `SampleLabelCell*` 3 ファイル分のエントリ（`PBXBuildFile` / `PBXFileReference` / `PBXGroup` children / `PBXSourcesBuildPhase` の各 4 箇所）を削除
- [x] 18.5.4 iOS: 同 `project.pbxproj` に `BasicCellsDemoView.swift` を追加（既存 `SampleLabelCellPreview.swift` の登録を雛形に新規 UUID で 4 箇所登録）
- [x] 18.5.5 iOS: `samples/ios/README.md` の `SampleLabelCell` 関連文言を `LabelCell` ベースに書き換え
- [x] 18.5.6 Android: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/{SampleLabelCell,SampleLabelCellViewHolder,SampleLabelCellDsl}.kt` を `trash` で削除
- [x] 18.5.7 Android: `MainActivity.kt` で `SampleLabelCell` を `LabelCell` に置換、`onCreate` 内の Sample 独自 Cell 登録ブロック (`KsCellRegistry.register(cellClass = SampleLabelCell::class, ...)`) を削除
- [x] 18.5.8 Android: `samples/android/settings.gradle.kts` / `samples/android/app/build.gradle.kts` のコメント文言を `LabelCell` ベースに更新
- [x] 18.5.9 Android: `samples/android/README.md` の `SampleLabelCell` 関連文言を `LabelCell` ベースに書き換え
- [ ] 18.5.10 両プラットフォームの Sample アプリを実機 or Simulator / Emulator で起動し、「Store 方式デモ」「DSL 方式デモ」「基本 Cell 7 種デモ」の 3 画面がクラッシュせず正常動作することを目視確認
- [x] 18.5.11 Android Sample の `AndroidManifest.xml` の `android:theme` を `@android:style/Theme.Material.Light.NoActionBar` から `@style/Theme.AppCompat.Light.NoActionBar` に変更（`ks-settingsview-ui` が `SwitchCompat` / `AppCompatImageView` を使用するため AppCompat 派生テーマが必須。フレームワーク標準 Material テーマだと SwitchCell 描画時に `SwitchCompat.makeLayout` で `NullPointerException` クラッシュ）
- [x] 18.5.12 `docs/android-ui.md` に「テーマ要件」セクションを追加し、利用者アプリは `Theme.Material3.*` 派生テーマを使用する必要があること（`MaterialSwitch` の `?attr/materialSwitchStyle` 解決のため）、`@android:style/Theme.Material.*` および `Theme.AppCompat.*` / `Theme.MaterialComponents.*` は使用不可であることを明記
- [x] 18.5.13 `android/ks-settingsview-ui/src/main/.../ClassicSectionDecoration.kt` を `onDraw` → `onDrawOver` に変更（Cell 背景 `setBackgroundColor` で罫線が上書きされるリグレッションを修正。Cell 描画後に区切り線が確実に重なるようにする）
- [x] 18.5.14 `android/ks-settingsview-ui/src/main/.../SwitchCellViewHolder.kt` を `SwitchCompat` から `com.google.android.material.materialswitch.MaterialSwitch` に置換（Material Design 3 風の太いトラック・サムで視認性を確保）。`showText = false` / `textOn = ""` / `textOff = ""` を明示し SwitchCompat 既知の `textOn/textOff` null NPE を回避
- [x] 18.5.15 `android/ks-settingsview-ui/src/main/.../CheckboxCellViewHolder.kt` を `TextView "✓"` から `androidx.appcompat.widget.AppCompatCheckBox` に置換（Material 風の角丸チェックボックス。`accentColor` は `buttonTintList` 経由で反映）
- [x] 18.5.16 `android/ks-settingsview-ui/src/main/.../RadioCellViewHolder.kt` を `TextView "●"` から `androidx.appcompat.widget.AppCompatRadioButton` に置換（Material 風の ring + dot 表示）
- [x] 18.5.17 `android/ks-settingsview-ui/build.gradle.kts` に `com.google.android.material:material:1.12.0` 依存を追加（`MaterialSwitch` のため）
- [x] 18.5.18 `samples/android/app/build.gradle.kts` にも `com.google.android.material:material:1.12.0` を追加（Sample の `Theme.Material3.*` リソース解決のため）
- [x] 18.5.19 `samples/android/app/src/main/AndroidManifest.xml` の `android:theme` を `@style/Theme.Material3.DayNight.NoActionBar` に変更
- [x] 18.5.20 `android/ks-settingsview-ui/src/test/.../BasicCellsTest.kt` を新しいウィジェットに合わせて更新（`findCheckmarkText` → `findCheckBox` / `findRadioButton` / `findMaterialSwitch`、テスト用 Context を `ContextThemeWrapper(ctx, Theme_Material3_Light_NoActionBar)` で包む）。`./gradlew :ks-settingsview-ui:test` 全 110 件 PASS / `:ks-settingsview-compose:test` 全 PASS を確認

## 19. ドキュメント

- [x] 19.1 `docs/cell-types-basic.md` を作成し、7 種の基本 Cell 各々のフィールド一覧と使用例（iOS / Android スニペット）を記載

## 20. 全テスト実行

- [x] 20.1 `ios/` で `swift test` 全成功
- [x] 20.2 `android/` で `./gradlew :ks-settingsview-ui:test` 全成功（個別実行で flaky なし。compose 既存 1 テストが本リポジトリでは並列実行の都合で flaky だが本提案の改修とは無関係）

## 21. 実機レビュー（Pixel 6a, 2026-06-02）由来のオリジナル準拠化とちらつき修正（Decision 9）

### 21.1 チェック系 Cell のトグル反映とちらつき回避 — オリジナル AiForms 準拠の TwoWay 方式（Decision 9-1）

- [x] 21.1.1 `CheckboxCellViewHolder`: セルタップ（container クリック）で `checkBox.toggle()` し、`OnCheckedChangeListener` 経由で `onValueChanged(newValue)` を一度だけ発火する（オリジナル `CheckboxCellView.cs` の `RowSelected` + `OnCheckedChanged` 準拠）。`bind` は `cell.isChecked` を初期表示に反映するのみ
- [x] 21.1.2 `SwitchCellViewHolder`: セル全体タップ（Decision 9-6）で `switchView.toggle()` し、通知を `OnCheckedChangeListener` 一本に集約（二重発火防止）。`bind` は `cell.isOn` を初期反映（オリジナル `SwitchCellView.cs` 準拠）
- [x] 21.1.3 `RadioCellViewHolder`: セルタップで未選択なら自分を即 `checkView.isChecked = true` にし `onSelected(value)` 発火（オリジナル `RadioCellView.cs` の `if (!_simpleCheck.Selected) SelectedValue = _radioCell.Value` 準拠）。他セルの選択解除は利用者の `selectedValue` 更新→再 bind で `value == selectedValue` 判定により反映
- [x] 21.1.4 `SimpleCheckCellViewHolder`: セルタップで `checkView` のチェックをトグルし `onValueChanged(newValue)` 発火。`bind` は `cell.isChecked` を初期反映
- [x] 21.1.5 4 Cell の `equals`/`hashCode` は内部状態（`isOn`/`isChecked`/`selectedValue`）を含める素直な data class 相当に戻す（クロージャのみ除外＝Decision 2）。`CellListItemDiffCallback.areContentsTheSame` は `oldItem == newItem`（equals 委譲）に戻し、`DSLDiffCalculator` も equals ベース（`oldCell != cell` での `ReplaceCell` 検出）に戻す。これにより「View 自身のトグルで即時反映」＋「利用者が明示的に状態を変えて `submitList` した場合も diff で反映」を両立する
- [x] 21.1.6 利用者が `submitList` で新しい内部状態を渡したときに画面へ反映されることをテストで確認する（`DSLDiffCalculatorTest`: 内部状態変化で `ReplaceCell` 発行 / `BasicCellsTest`: `areContentsTheSame` が状態違いで false・同一で true）
- [x] 21.1.7 【撤去 / Pixel 6a 実機検証 2026-06-02】当初実装した payload 差分方式（内部状態を `equals` から除外 + `Cell.hasSameContentAs` 新設 + `CellChangePayload`/`getChangePayload`/`onBindViewHolder(payloads)`/各 ViewHolder の `bindStateOnly`/`DSLDiffCalculator.sectionsHaveSameContent`）は、実機で `getChangePayload` が安定して呼ばれずトグルが画面反映されない不具合が出た上、オリジナル AiForms の素直な設計（View 自身がトグルし submitList に依存しない）から乖離していたため、オーナー判断で**全撤去**しオリジナル準拠の TwoWay 方式（21.1.1〜21.1.5）に作り直した。TwoWay 方式の回帰テストを `BasicCellsTest`（セルタップで View 即トグル＋通知一度のみ／Radio 既選択時は再通知なし）に追加。実機 (A) Checkbox / (B) Radio / (C) SimpleCheck のタップ反映と (E) ちらつき非発生を確認
- [x] 21.1.8 【ちらつき再発の根本修正 / オーナー確定 2026-06-02】21.1.5 で内部状態を `equals`/`hashCode` に**戻した**結果、`onValueChanged` → state 更新 → 再 compose → `DSLDiffCalculator` が内部状態差で `ReplaceCell` 発行 → `submitList` → `areContentsTheSame == false` → 行フルリバインドという経路でちらつきが**復活**した。オーナー確定方針により内部状態を diff 対象外とする（クロージャ除外＝Decision 2 と同じ思想、オリジナル AiForms 準拠）: `SwitchCell` は `isOn` を、`CheckboxCell`/`SimpleCheckCell` は `isChecked` を `equals`/`hashCode` から**除外**する（`id`/`style`/`title`/`description`/`accentColor` のみ）。`areContentsTheSame` は `oldItem == newItem`（equals 委譲）のまま、`hasSameContentAs`/payload 機構は復活させない。これにより内部状態だけの変化では `equals == true` となり `ReplaceCell`/`areContentsTheSame == false` が発生せず、`submitList` による再 bind（フルリバインド）が起きずちらつきが構造的に解消する
- [x] 21.1.9 【RadioCell の別扱い / option (b) を実機確認の上で採用 2026-06-02】RadioCell は `selectedValue` を `equals`/`hashCode` に**残す**（`id`/`style`/`title`/`groupId`/`value`/`selectedValue`、クロージャ `onSelected` のみ除外）。`selectedValue` を除外すると Light タップ時に他セル（Dark）が再 bind されず古い ✓ が消えない（複数 ✓）不具合になる。実機 Pixel 6a の画像差分で「Light タップ前: Dark に ✓（328px）/ Light 空、タップ後: Light に ✓（328px）/ Dark・Auto 空」を確認し、`selectedValue` を残すことで複数 ✓ にならないことを検証（design.md 9-1「RadioCell の別扱い」に記録）
- [x] 21.1.10 【テスト更新 2026-06-02】内部状態違いで `equals == true`（Switch/Checkbox/SimpleCheck）/ RadioCell の `selectedValue` 違いで `equals == false` を確認するテストを `BasicCellsTest`・`DSLDiffCalculatorTest` に追加・更新。全テスト PASS（`:ks-settingsview-ui:test` / `:ks-settingsview-core:test` / `:ks-settingsview-compose:testDebugUnitTest`）。実機 Pixel 6a 画像差分で (最重要) ちらつき解消（操作対象ウィジェット＋上部テキスト帯以外 0px）、(A) Checkbox トグル / (B) Radio 複数 ✓ なし / (C) SimpleCheck トグル / (D) LabelCell ロングプレス Ripple を確認（スクショ `/tmp/ks_review_fix3/`）

### 21.2 ナビゲーションインジケータ（右矢印）のオリジナル準拠（Decision 9-2）

- [x] 21.2.1 オリジナル `ic_navigate_next.xml`（`../AiForms.Maui.SettingsView/SettingsView/Platforms/Android/Resources/drawable/ic_navigate_next.xml`、18×26dp / `#FFCACACA` chevron）相当の VectorDrawable を `android/ks-settingsview-ui/src/main/res/drawable/` に追加する
- [x] 21.2.2 `CommandCellViewHolder` の `disclosureView` を `TextView ">"` から `AppCompatImageView`（上記 drawable 表示）に置換する。`hideArrow` 時の visibility 制御は維持
- [x] 21.2.3 `BasicCellsTest.kt` の Disclosure Indicator 検出を新ウィジェット（ImageView）に合わせて更新

### 21.3 RadioCell / SimpleCheckCell のカスタムチェック描画（Decision 9-3 / 9-4）

- [x] 21.3.1 オリジナル `SimpleCheck.cs` の `OnDraw` ロジック（2 本の `DrawLine` でチェックマーク、`StrokeWidth = 2dp`、`AntiAlias`、canvas 比率 22%/52%→38%/68%→74%/28%）を移植したカスタム `View`（仮称 `KsSimpleCheckView`）を `android/ks-settingsview-ui/src/main/.../` に新設する。`color` と `isChecked`（= `Selected`）プロパティを持ち、`isChecked` 変更時に `invalidate()` する
- [x] 21.3.2 `RadioCellViewHolder` の accessory を `AppCompatRadioButton` から `KsSimpleCheckView` に置換する。`value == selectedValue` のとき `isChecked = true`、色は `accentColor` / `Theme.cellAccentColor` で着色
- [x] 21.3.3 `SimpleCheckCellViewHolder` を `TextView "✓"` から `KsSimpleCheckView`（オリジナル `SimpleCheckCellView.cs` 同様に 30×30dp 相当で配置）に置換する。オリジナルの配置（accessory 側）に合わせるか左側維持かを実装時にオリジナルと照合して決定し、design.md 9-4 に追記（→ 決定: オリジナル準拠で accessory 右側 30×30dp に配置。review-result_002 対応で design.md 9-4 に確定追記済み）
- [x] 21.3.4 `BasicCellsTest.kt` の RadioCell / SimpleCheckCell の検出・状態判定を `KsSimpleCheckView` に合わせて更新

### 21.5 タッチフィードバック（Ripple）の移植（Decision 9-5）

- [x] 21.5.1 共通ヘルパ `applyCellBackground(container, effective, theme)` を `LabelCellViewHolder.kt`（共通ヘルパ層）に新設する。`RippleDrawable(ColorStateList.valueOf(rippleColor), ColorDrawable(effective.backgroundColor), null)` を `container.background` に設定。`rippleColor` は `theme.selectedColor`（未指定/既定時はオリジナル準拠の `Rgb(180,180,180)` 相当）
- [x] 21.5.2 全 ViewHolder（Label / Command / Button / Switch / Checkbox / Radio / SimpleCheck）の `bind` で `container.setBackgroundColor(...)` を `applyCellBackground(...)` 呼び出しに置換する
- [x] 21.5.3 `onDrawOver` 罫線（Decision 8）と Ripple の重畳順序に問題がないこと（罫線が最前面、Ripple が背景）を実機で確認する（Pixel 6a 実機検証で罫線が Ripple に隠れず保持されることを確認。verification-report_002_device.md）
- [x] 21.5.5 【リグレッション修正 / Pixel 6a 実機検証 2026-06-02】`RippleDrawable` の ripple は View が押下状態を受け取れる（`isClickable == true`）必要があるが、`applyCellBackground` が `isClickable` を立てておらず、かつ `onTap` 未指定 Cell の `else` 分岐で `isClickable = false` を設定していたため LabelCell 等で Ripple が出なかった不具合を修正。`applyCellBackground` で `view.isClickable = true` を設定し、各 ViewHolder の no-handler 分岐から `isClickable = false` を除去（クリックリスナーのみ解除）。回帰防止テスト（LabelCell / no-handler Checkbox の container が clickable かつ背景 RippleDrawable）を `BasicCellsTest` に追加。実機 (D) LabelCell「プロフィール」ロングプレスで Ripple ハイライト表示を確認
- [ ] 21.5.4 `Theme.selectedColor` を変更すると Ripple / 選択ハイライト色が変わることを Sample で確認できるようにする（必要なら Sample に `selectedColor` カスタム例を 1 つ追加）※ 既定 selectedColor での Ripple 表示は実機確認済み。カスタム色 Sample 追加は任意の後続対応

### 21.6 SwitchCell のセル全体タップ ON/OFF（Decision 9-6）

- [x] 21.6.1 `SwitchCellViewHolder.bind` で `container.setOnClickListener` を設定し、タップで `switchView.toggle()` を呼ぶ（通知は `switchView` の `OnCheckedChangeListener` 一本に集約し二重発火を防ぐ）。スイッチ直接操作も従来どおり機能させる
- [x] 21.6.2 `SwitchCellViewHolder.reset()` で `container` の `OnClickListener` も null 化する
- [x] 21.6.3 `BasicCellsTest.kt` に「セル全体タップで `onValueChanged` が発火する」テストを追加する

### 21.7 検証（Pixel 6a 実機を adb / Appium で操作してエージェントが目視確認）

> 本節は**エージェントが実際に Pixel 6a 実機を操作し、スクリーンショットを取得して目視で合否判定する**タスクである。「ドキュメント上の確認」ではなく、実機の画面状態を画像として取得し、Read ツールで画像を読み込んで判定するところまでを完了条件とする。

#### 21.7.1 ユニットテスト

- [x] 21.7.1 `cd android && ./gradlew :ks-settingsview-ui:test` 全 PASS を確認

#### 21.7.2 実機準備（adb）

- [x] 21.7.2.1 `adb devices` で Pixel 6a 実機が `device` 状態で接続されていることを確認する（<android-device-serial> / model:bluejay / Android 16 を確認）
- [x] 21.7.2.2 Sample アプリをビルドしてインストールする：`ANDROID_SERIAL=<android-device-serial> ./gradlew :app:installDebug`（BUILD SUCCESSFUL）
- [x] 21.7.2.3 `adb shell am start -n jp.kamusoft.kssettingsview.samples.android/.MainActivity` で Sample アプリを起動する

#### 21.7.3 「基本 Cell 7 種デモ」画面への遷移と静的描画の目視確認

- [x] 21.7.3.1 トップメニューから「基本 Cell 7 種デモ」へ `input tap` で遷移（uiautomator dump で座標算出）
- [x] 21.7.3.2 スクショ取得 + Read で目視判定 — **全 PASS**：
  - (a) ✅ CommandCell の矢印が `ic_navigate_next` 準拠 chevron（`ImageView content-desc="Disclosure indicator"`）
  - (b) ✅ RadioCell が標準ラジオボタンでなくチェックマーク（`KsSimpleCheckView` = `View content-desc="Selected"`、選択中 Dark に青✓）
  - (c) ✅ SimpleCheckCell がオリジナル準拠の `KsSimpleCheckView`（`View content-desc="Checked"`、accessory 右側配置）
- [x] 21.7.3.3 `input swipe` でスクロールし RadioCell / SimpleCheckCell も目視判定済み

#### 21.7.4 タッチフィードバック（Ripple / 選択ハイライト）の目視確認

- [x] 21.7.4.1 SwitchCell「通知」のラベル領域を同座標ロングプレス（`input swipe` 1200ms）し押下中にスクショ取得。Read で判定 — **(e) ✅ セル全体が一様にグレーのハイライト（Ripple）で塗られる**ことを確認（`applyCellBackground` + `Theme.selectedColor`）
- [ ] 21.7.4.2 `Theme.selectedColor` をカスタム色に設定した Sample 画面でのハイライト色確認 ※ カスタム色 Sample が未整備のため未実施（任意の後続対応。既定色での Ripple は確認済み）

#### 21.7.5 ちらつき（フルリバインド）非発生の確認

- [x] 21.7.5.1 SwitchCell のスイッチを `input tap` で ON/OFF し、前後スクショを PIL `ImageChops.difference` で領域別 bbox 解析 — **(d) ✅** タイトル領域差分 **None**、SwitchCell より下（他セル・罫線）差分 **None**、変化はスイッチ本体ピクセルのみ。**フルリバインドによるちらつき皆無**を厳密確認（オリジナル準拠 TwoWay 方式: View 自身がトグルし submitList/diff に依存しないため構造的にちらつかない）
- [x] 21.7.5.2 Checkbox についても同様に解析 — タイトル / チェック領域 / 他セルすべて差分 **None**、ヘッダー「最後にタップ」のみ更新（`onValueChanged` 発火）。ちらつき皆無を確認

#### 21.7.6 Switch のセル全体タップ ON/OFF の確認

- [x] 21.7.6.1 SwitchCell の**ラベル領域（左側 x=300）**を `input tap` し前後スクショ比較 — **(f) ✅** スイッチ本体外のラベルタップでヘッダー「通知 → false」更新かつサムが左へトグル。Android 標準設定アプリ準拠の挙動を実機確認

#### 21.7.7 仕様検証

- [x] 21.7.7 `openspec validate add-cell-types-basic --strict` PASS を確認

> 上記 21.7.3〜21.7.6 の各スクリーンショットは判定根拠として保存し、検証時に取得した画像と判定結果（合否と所見）を verification-report.md に記録する。判定で NG が出た項目は該当 Decision の実装タスクに差し戻す。

> 注: iOS 側にも同種のちらつきが実在することを確認済みだが、その修正は本提案では対象外とし、別途 **iOS 実機レビューのタスク**で対応する（design.md Decision 9-1「iOS との対称性」参照）。

## 依存関係

- 先行：`add-monorepo-foundation`、`add-settings-view-core`、`add-settings-view-ios-ui`、`add-settings-view-android-ui`、`add-samples-ios`、`add-samples-android`、**`add-declarative-dsl`**（DSL 拡張関数規約・`SectionHandle` / `CellHandle` / `KsIdentifiable` の定義を前提とする）
- 後続：`add-maui-cells`（本提案完了後、MAUI 側 7 種基本 Cell Handler 実装と `samples/maui/` への 7 種ページ追加を担当）
- 本提案は Native iOS / Android のみを対象とし、MAUI Sample 拡張は責務に含めない

## 完了条件

- 全タスクのチェックボックスが完了している
- `cell-types-basic` capability の全 Scenario が通る
- iOS / Android の各 Sample で 7 種の基本 Cell が表示・操作できる（Sample 土台は `add-samples-ios` / `add-samples-android` で整備済み前提）
- PoC Cell が両プラットフォームで完全に削除されている
- 全ユニットテスト成功
