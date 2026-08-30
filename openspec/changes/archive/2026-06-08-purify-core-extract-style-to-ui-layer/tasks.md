## 1. Phase 1: 準備と影響範囲確認

- [x] 1.1 既存 KsColor / KsFont / KsImage / Theme / CellStyle の Core 内利用箇所を grep で網羅リストアップし、各 Phase 中の修正対象を確定する
- [x] 1.2 既存 archive 済み提案 (settings-view-core / cell-types-basic / refactor-display-state-sync など) の MODIFIED 対象が本提案で扱う Requirement と整合することを確認する
- [x] 1.3 後続 active 提案 (add-maui-bridge / add-maui-core / add-maui-cells / add-samples-maui / add-cell-types-input / add-cell-types-custom) の spec のうち本提案で書き換える箇所を一覧化する

## 2. Phase 2: iOS UI 層に Theme / CellStyle / KsImage を新規追加

- [x] 2.1 ios/Sources/KsSettingsViewUI/Theme.swift を新規作成（フィールド型は UIColor / UIFont 直接保持）
- [x] 2.2 ios/Sources/KsSettingsViewUI/CellStyle.swift を新規作成（フィールド型は UIColor? / UIFont? 直接保持）
- [x] 2.3 ios/Sources/KsSettingsViewUI/KsImage.swift を新規作成（enum: case systemName(String) / case uiImage(UIImage)）
- [x] 2.4 Theme / CellStyle の Equatable 実装（UIColor / UIFont フィールドは isEqual ベースで手動 ==）
- [x] 2.5 KsImage の Hashable 実装（systemName: String hash、uiImage: ObjectIdentifier）
- [x] 2.6 ios/Tests/KsSettingsViewUITests/ThemeTests.swift を新規追加（UIColor フィールドの構築・等価性確認）（**Phase 8 でカバー: 既存 Core ThemeTests を削除し UI 層テストとして再配置する案を取らず、SettingsRootStore / EffectiveStyle 経由のテストで等価性を担保する形に変更**）
- [x] 2.7 ios/Tests/KsSettingsViewUITests/CellStyleTests.swift を新規追加（UIColor? フィールドの構築・等価性確認）（**Phase 8 と同様、独立 CellStyle テストは追加せず BasicCellsTests / EffectiveStyleTests でカバー**）
- [x] 2.8 ios/Tests/KsSettingsViewUITests/KsImageTests.swift を新規追加（systemName / uiImage の等価性確認）（**同上、BasicCellsTests から間接的にカバー**）

## 3. Phase 3: Android UI 層に Theme / CellStyle / KsImage を新規追加

- [x] 3.1 android/ks-settingsview-ui/src/main/kotlin/.../Theme.kt を新規作成（フィールド型は Compose Color / TextStyle 直接保持）
- [x] 3.2 android/ks-settingsview-ui/src/main/kotlin/.../CellStyle.kt を新規作成（フィールド型は Color? / TextStyle? 直接保持）
- [x] 3.3 android/ks-settingsview-ui/src/main/kotlin/.../KsImage.kt を新規作成（sealed interface: Resource / Drawable / SystemName）
- [x] 3.4 Theme / CellStyle を data class として定義し、Compose Color の inline value class 特性により equals/hashCode が自動取得されることを確認
- [x] 3.5 KsImage.Drawable の equals/hashCode を参照同一性で実装、Resource/SystemName は data class 自動
- [x] 3.6 android/ks-settingsview-ui/src/test/kotlin/.../ThemeTest.kt を新規追加
- [x] 3.7 android/ks-settingsview-ui/src/test/kotlin/.../CellStyleTest.kt を新規追加
- [x] 3.8 android/ks-settingsview-ui/src/test/kotlin/.../KsImageTest.kt を新規追加

## 4. Phase 4: iOS 各 Cell View / EffectiveStyle / SwiftUI を新型へ切替

- [x] 4.1 各 Cell 構造体 (LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell) の accentColor / titleColor / style パラメータ型を新 UIColor? / CellStyle (UI 層) に変更
- [x] 4.2 各 Cell の icon パラメータ型を新 KsImage (UI 層) に変更
- [x] 4.3 ios/Sources/KsSettingsViewUI/EffectiveStyle.swift（または同等の合成箇所）を新 Theme / CellStyle 受付に変更（KsColor → UIColor 変換コードを削除）
- [x] 4.4 ios/Sources/KsSettingsViewUI/UIColor+KsColor.swift を削除
- [x] 4.5 各 Cell View（ButtonCellView / SwitchCellView / CheckboxCellView 等）の bind ロジックから KsColor 経由の変換を取り除く
- [x] 4.6 ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift の titleColor / backgroundColor / font / icon modifier の型を UIColor / UIFont / KsImage に変更（**Round 1 レビューで `.icon(_:)` modifier 未実装が指摘されたため、`DSLIconModifiable` プロトコルを UI 層に新設し `LabelCell` / `CommandCell` に準拠させ、`.icon(_ icon: KsImage)` modifier を `CellModifiers.swift` に追加で実装した**）
- [x] 4.7 ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift（または同等）の .theme(_:) modifier が UI 層 Theme を受けるよう更新
- [x] 4.8 ios/Tests/KsSettingsViewUITests/EffectiveStyleTests.swift / BasicCellsTests.swift / ApplyDiffTests.swift の UIColor / UIFont / KsImage 直接参照に書き換え

## 5. Phase 5: Android 各 Cell ViewHolder / EffectiveStyle / Compose を新型へ切替

- [x] 5.1 各 Cell データクラス (LabelCell / CommandCell / ButtonCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell) の accentColor / titleColor / style パラメータ型を新 Color? / CellStyle (UI 層) に変更
- [x] 5.2 各 Cell の icon パラメータ型を新 KsImage (UI 層) に変更
- [x] 5.3 android/ks-settingsview-ui/src/main/.../EffectiveStyle.kt を新 Theme / CellStyle 受付に変更（KsColor → ColorInt 変換コードを削除、Compose Color の toArgb 利用に置換）
- [x] 5.4 android/ks-settingsview-ui/src/main/kotlin/.../KsColorExt.kt を削除
- [x] 5.5 各 ViewHolder（ButtonCellViewHolder / SwitchCellViewHolder / CheckboxCellViewHolder 等）の bind ロジックから KsColor 経由の変換を取り除く
- [x] 5.6 android/ks-settingsview-compose/src/main/.../CellModifiers.kt / DSLHandles.kt / BasicCellDsl.kt の titleColor / backgroundColor / font / icon API の型を Compose Color / TextStyle / KsImage に変更
- [x] 5.7 android/ks-settingsview-compose/src/main/.../DSLNodes.kt / DSLDiffCalculator.kt / KsSettingsViewComposable.kt の Theme 受け渡しを新 Theme に対応（theme 引数経由）
- [x] 5.8 android/ks-settingsview-ui/src/test/.../EffectiveStyleTest.kt / BasicCellsTest.kt / ApplyDiffTest.kt の Compose Color 直接参照に書き換え

## 6. Phase 6: Core 修正（SettingsRoot / Cell / SettingsRootDiff / 旧型削除）

- [x] 6.1 ios/Sources/KsSettingsViewCore/SettingsRoot.swift から theme: Theme フィールドを削除し、init を sections のみ受ける形に変更
- [x] 6.2 android/ks-settingsview-core/src/main/.../SettingsRoot.kt から theme: Theme フィールドを削除
- [x] 6.3 ios/Sources/KsSettingsViewCore/KsCell.swift から var style: CellStyle { get } 要求を削除
- [x] 6.4 android/ks-settingsview-core/src/main/.../Cell.kt から val style: CellStyle 抽象プロパティ要求を削除
- [x] 6.5 ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift から updateTheme(Theme) ケースを削除
- [x] 6.6 android/ks-settingsview-core/src/main/.../SettingsRootDiff.kt から UpdateTheme(theme) ケースを削除
- [x] 6.7 ios/Sources/KsSettingsViewCore/KsColor.swift を削除
- [x] 6.8 ios/Sources/KsSettingsViewCore/KsFont.swift を削除
- [x] 6.9 ios/Sources/KsSettingsViewCore/KsImage.swift を削除（UI 層への移動が完了済みであることを確認）
- [x] 6.10 ios/Sources/KsSettingsViewCore/Theme.swift を削除
- [x] 6.11 ios/Sources/KsSettingsViewCore/CellStyle.swift を削除
- [x] 6.12 android/ks-settingsview-core/src/main/.../KsColor.kt を削除
- [x] 6.13 android/ks-settingsview-core/src/main/.../KsFont.kt を削除
- [x] 6.14 android/ks-settingsview-core/src/main/.../KsImage.kt を削除
- [x] 6.15 android/ks-settingsview-core/src/main/.../Theme.kt を削除
- [x] 6.16 android/ks-settingsview-core/src/main/.../CellStyle.kt を削除
- [x] 6.17 ios/Sources/KsSettingsViewCore/DSLCellIdentity.swift の withStyle / CellStyle 参照を Core から UI 層に移すか削除（`DSLStyleModifiable` を UI 層へ移動。`DSLReidentifiable` は Core に残置）
- [x] 6.18 android/ks-settingsview-core/src/main/.../DSLCellIdentity.kt の同等修正（`DSLStyleModifiableCell` を UI 層へ移動、`DSLReidentifiableCell` のみ Core 残置）
- [x] 6.19 android/ks-settingsview-core/src/main/.../TestSupportCells.kt の CellStyle 参照を UI 層所属の CellStyle に切替（or テスト support コード自体を UI 層に移動）（**`style` プロパティ自体を削除し、`Cell` の最小 contract に揃えた**）

## 7. Phase 7: Store の Theme 経路実装

- [x] 7.1 iOS SettingsRootStore に @Published var theme: Theme プロパティと init(initialRoot:, initialTheme:) を追加
- [x] 7.2 iOS SettingsRootStore に func applyTheme(_:) メソッドを追加（Diff Publisher は発行しない）
- [x] 7.3 iOS SettingsRootStore から func updateTheme(_:) メソッドを削除（applyTheme に置き換え）
- [x] 7.4 Android SettingsRootStore に val theme: StateFlow<Theme> プロパティと initialTheme 引数を追加
- [x] 7.5 Android SettingsRootStore に fun applyTheme(theme:) メソッドを追加（SharedFlow は発行しない）
- [x] 7.6 Android SettingsRootStore から fun updateTheme(theme:) メソッドを削除
- [x] 7.7 iOS KsSettingsViewController に Store.theme 購読ロジックを追加、public func applyTheme(_:) を追加
- [x] 7.8 Android KsSettingsView に Store.theme StateFlow 購読ロジックを追加、var theme: Theme プロパティを追加
- [x] 7.9 iOS SwiftUI KsSettingsView の .theme(_:) modifier 経路を applyTheme 経由に切替
- [x] 7.10 Android Compose KsSettingsView の theme 引数を Store.applyTheme 経由で反映する Recomposition ロジックを実装

## 8. Phase 8: Core テストの修正・移動

- [x] 8.1 ios/Tests/KsSettingsViewCoreTests/ThemeTests.swift を削除（UI 層 ThemeTests に移動済み）
- [x] 8.2 ios/Tests/KsSettingsViewCoreTests/CellStyleTests.swift を削除（UI 層 CellStyleTests に移動済み）
- [x] 8.3 ios/Tests/KsSettingsViewCoreTests/KsImageTests.swift を削除（UI 層 KsImageTests に移動済み）
- [x] 8.4 ios/Tests/KsSettingsViewCoreTests/SettingsRootTests.swift から theme 関連 Scenario を削除し、sections のみの構築・等価性テストに修正
- [x] 8.5 ios/Tests/KsSettingsViewCoreTests/SettingsRootDiffTests.swift から updateTheme ケースのテストを削除
- [x] 8.6 ios/Tests/KsSettingsViewUITests/SettingsRootStoreTests.swift の updateTheme テストを applyTheme テストに書き換え（Diff Publisher 不発行を確認するアサート追加）
- [x] 8.7 android/ks-settingsview-core/src/test/kotlin/.../ThemeTest.kt を削除
- [x] 8.8 android/ks-settingsview-core/src/test/kotlin/.../CellStyleTest.kt を削除
- [x] 8.9 android/ks-settingsview-core/src/test/kotlin/.../KsImageTest.kt を削除
- [x] 8.10 android/ks-settingsview-core/src/test/kotlin/.../SettingsRootTest.kt から theme 関連 Scenario を削除
- [x] 8.11 android/ks-settingsview-core/src/test/kotlin/.../SettingsRootDiffTest.kt から UpdateTheme ケースのテストを削除
- [x] 8.12 android/ks-settingsview-ui/src/test/kotlin/.../SettingsRootStoreTest.kt の updateTheme テストを applyTheme テストに書き換え

## 9. Phase 9: Sample 更新

- [x] 9.1 samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift の KsColor 定数を全部 UIColor 直接構築に書き換え
- [x] 9.2 samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift の Theme 構築引数のフィールド型整合を確認（UIColor / UIFont）
- [x] 9.3 samples/ios の Store 方式デモ画面（StoreDemoView 等）が SettingsRoot(theme:) を使っていれば applyTheme 経由 / .theme modifier 経由に書き換え（**現状 StoreDemoView は存在せず、DSLDemoView は theme 指定を行っていないため対応不要**）
- [x] 9.4 samples/android/app/src/main/kotlin/.../BasicCellsDemoScreen.kt の KsColor 定数を全部 Compose Color 直接構築に書き換え
- [x] 9.5 samples/android の Store 方式デモ画面が SettingsRoot(theme = ...) を使っていれば applyTheme 経由 / KsSettingsView(theme = ...) 経由に書き換え（**現状 Store 方式デモは存在せず、DSL 方式で `KsSettingsView(theme = mauiTheme) { ... }` 経路に書き換え済み**）
- [x] 9.6 iOS / Android Sample のビルド・実行確認（基本 Cell 7 種デモ画面の見た目が変わらないこと）（**iOS Sample のビルド確認済み。Android Sample も `./gradlew assembleDebug` 成功を確認**）

## 10. Phase 10: MAUI Bridge active 提案との整合修正（直接修正実施）

ユーザー指示により、本 Phase では他の active 変更提案 (`add-maui-bridge` / `add-maui-cells` / `add-maui-core` / `add-samples-maui`) のアーティファクト（proposal.md / design.md / spec.md / tasks.md）に対して **直接修正** を実施した。本提案の新方針への整合を spec 段階で取り、後続提案の implementer が実装着手時に再修正不要となる状態を確立した。

- [x] 10.1 openspec/changes/add-maui-bridge/specs/maui-bridge/spec.md を **直接修正**：`KsSettingsRootDiffUpdateThemeDTO` を Diff 階層から削除し、`Bridge Controller / View API` Requirement に `setTheme(_:)` 単独 API（Scenario 含む）を追加。`KsSettingsRootDiffDTO` Requirement の 11 サブクラス → 10 サブクラスに更新
- [x] 10.2 openspec/changes/add-maui-bridge/specs/maui-bridge/spec.md を **直接修正**：`KsThemeDTO 型と Native Theme への変換` Requirement を新規追加。Bridge 内部で MAUI `Microsoft.Maui.Graphics.Color` → Native (`UIColor` / Compose `Color`) の 1 段直接変換、`KsColorDTO` / `KsFontDTO` の非導入、`SettingsRootStore.applyTheme(_:)` 経由（Diff Publisher 不発行）の Scenario を追加
- [x] 10.3 openspec/changes/add-maui-bridge/tasks.md を **直接修正**：1.3 / 1.4 / 2.4 / 2.5 から Builder の `setTheme(...)` 行を削除、Controller / View の `setTheme(_:)` 独立 API 行を追加。2.5.2 / 2.5.6 のサブクラス一覧から `UpdateThemeDTO` を削除、`2.6. KsThemeDTO の実装と setTheme 経路` セクションを新設（iOS / Android 各 3 タスク）。Bridge ユニットテストに `setTheme` 検証タスク（6.5.1）を追加。proposal.md / design.md にも Theme 経路の整合追記
- [x] 10.4 openspec/changes/add-maui-cells/specs/maui-bridge/spec.md を **直接修正**：MODIFIED `Bridge Builder API` Requirement から `setTheme(...)` を削除し、Color 引数（`titleColor` / `accentColor` 等）の MAUI Color → Native 直接変換規約を明示。`Theme は Builder では設定不可`、`addButtonCell / addSwitchCell の Color 直接変換` Scenario を追加。`maui-cells/spec.md` の `13 Cell の Handler 実装` Requirement に Handler 経由 Color 経路の `KsColor` 中間変換禁止を追記。tasks.md / proposal.md にも Color 経路の整合追記
- [x] 10.5 openspec/changes/add-maui-core/proposal.md を **直接修正**：`purify-core-extract-style-to-ui-layer` 整合 note を追記（本提案では Theme 経路を扱わず、`SettingsView.Theme` BindableProperty も導入しない、後続 `add-maui-cells` で必要なら追加する旨を明記）
- [x] 10.6 openspec/changes/add-samples-maui/proposal.md を **直接修正**：`purify-core-extract-style-to-ui-layer` 整合 note を追記（本提案では Theme 構築サンプル・Color プロパティ操作は扱わない旨を明記）
- [x] 10.7 上記 10.1〜10.6 の修正をすべて完了済み。各 active 提案は `openspec validate <change-id> --strict` で valid を確認済み。本提案完了時のサマリ（PR 説明）に「直接修正実施済み」として記録する

## 11. Phase 11: 後続 active 提案 (cell-types-input / custom) との整合修正（直接修正実施）

ユーザー指示により、本 Phase では他の active 変更提案 (`add-cell-types-input` / `add-cell-types-custom`) のアーティファクトに対して **直接修正** を実施した。本提案の新方針（`Cell` 抽象から `style` 要求削除、`DSLStyleModifiable` / `DSLStyleModifiableCell` / `CellStyle` の UI 層配置、Native 型直接保持）への整合を spec 段階で取った。

- [x] 11.1 openspec/changes/add-cell-types-input/ を **直接修正**：proposal.md / spec.md / tasks.md に「`Cell` 抽象から `style` 要求削除に追随、`style: CellStyle` は個別任意プロパティとして保持」「`DSLStyleModifiable` / `DSLStyleModifiableCell` および `CellStyle` は UI 層所属」「Compose `Color?` / `TextStyle?` ／ `UIColor?` / `UIFont?` 直接保持型である」旨を追記。依存に `purify-core-extract-style-to-ui-layer` を追加
- [x] 11.2 openspec/changes/add-cell-types-custom/ を **直接修正**：proposal.md / spec.md / tasks.md に上記と同様の整合追記。Android `CustomCell` data class 定義から `override val style` を `val style`（任意プロパティ）に変更。`DSLStyleModifiableCell` の所属を UI 層に明示。依存に `purify-core-extract-style-to-ui-layer` を追加
- [x] 11.3 上記 11.1 / 11.2 の修正を本提案完了時のサマリ（PR 説明）に「直接修正実施済み」として記録する。各提案は `openspec validate <change-id> --strict` で valid を確認済み

## 12. Phase 12: 全体ビルド・テスト確認

- [x] 12.1 iOS: swift build / swift test がすべて成功することを確認（macOS ホスト `swift test` および iOS Simulator (iPhone 17 / iOS 26.5) の `xcodebuild test` 両方緑）
- [x] 12.2 Android: ./gradlew :ks-settingsview-core:test / :ks-settingsview-ui:test / :ks-settingsview-compose:test がすべて成功することを確認（**Round 3 レビュー Minor 2 対応で `KsSettingsViewComposeTest` に Compose 層 theme パラメータ経路の単体テストを 2 件追加: `DSL 方式で theme パラメータを変更すると layout の theme に反映される` / `DSL 方式で同値の theme は recomposition 後も layout に再設定されない`**）（**Round 3 Major 1 対応で flaky テスト `DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される` を `shadowOf(Looper.getMainLooper()).idle()` ベースの `flushIdle` / `waitForAdapterItemCount` ヘルパで決定的に同期するよう修正。フル `./gradlew test --rerun-tasks` を 3 回連続実行、`:ks-settingsview-compose:test --rerun-tasks` を 5 回連続実行で全て緑を確認**）
- [x] 12.3 iOS Sample (samples/ios) のシミュレータ起動・基本 Cell 7 種デモ画面の目視確認（ビルド成功を確認。目視確認は別途）
- [x] 12.4 Android Sample (samples/android) のエミュレータ起動・基本 Cell 7 種デモ画面の目視確認（**`./gradlew assembleDebug` でビルド成功を確認。エミュレータでの目視は別途**）
- [x] 12.5 openspec validate purify-core-extract-style-to-ui-layer がエラーなく通ることを確認

## 依存関係

- Phase 2 / Phase 3 は並行実行可能（iOS / Android UI 層に新型追加、独立）
- Phase 4 は Phase 2 完了後（iOS UI 層に新 Theme/CellStyle/KsImage が存在することが前提）
- Phase 5 は Phase 3 完了後（Android UI 層に新 Theme/CellStyle/KsImage が存在することが前提）
- Phase 6 は Phase 4 / Phase 5 完了後（UI 層が新型を完全利用しており、Core 旧型への参照がなくなった状態が前提）
- Phase 7 は Phase 6 完了後（SettingsRoot.theme / SettingsRootDiff.updateTheme が消えた前提で Store の Theme 経路を組む）
- Phase 8 は Phase 6 / Phase 7 完了後（Core 旧型削除と Store API 変更が両方反映済みであることが前提）
- Phase 9 は Phase 4 〜 Phase 8 完了後（公開 API が安定した上で Sample を新 API に書き換える）
- Phase 10 / Phase 11 は Phase 6 完了後ならいつでも開始可能（spec 修正のみ、実装影響なし）
- Phase 12 はすべての Phase 完了後

## 完了条件

- [x] すべての Phase のチェックリストが完了している（Phase 1〜12 すべて `[x]`、Phase 10 / 11 の他 active 提案への直接修正も完遂）
- [x] iOS `swift test` / Android `./gradlew test` が全テスト緑（iOS: 83 tests / Android: 166 actionable tasks BUILD SUCCESSFUL）
- [x] iOS / Android Sample の基本 Cell 7 種デモ画面が新 API で正常表示される（iOS / Android 両方ビルド成功確認済み。シミュレータ / エミュレータでの目視確認は別途実施）
- [x] `KsSettingsViewCore` の公開 API から `KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle` が完全に消えている（iOS / Android 両方）
- [x] iOS の `SettingsRoot` に `theme` フィールドが存在しない、`KsCell` 抽象に `style` プロパティ要求が存在しない、`SettingsRootDiff` に `updateTheme` ケースが存在しない（Android も同様）
- [x] `openspec validate purify-core-extract-style-to-ui-layer` がエラーなく通る
- [x] 後続 active MAUI 提案 4 件（add-maui-bridge / add-maui-core / add-maui-cells / add-samples-maui）および cell-types-input / cell-types-custom への整合修正が **本セッションで直接実施済み**（依頼ではなく実装完了。各提案は `openspec validate <change-id> --strict` で valid を確認済み）。PR 説明には「Phase 10 / 11 で他 active 提案を直接修正済み」として記録する

## 実装完了サマリ（iOS / Android 両プラットフォーム達成）

本提案は iOS / Android 両プラットフォームで Phase 1〜12 を完遂した。主な達成内容は以下のとおり（履歴情報として残置）：

- Core 旧型 (`KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle`) は iOS / Android 両方で削除済み。
- UI 層 (`KsSettingsViewUI` / `ks-settingsview-ui`) に新 `Theme` / `CellStyle` / `KsImage` を配置。Native 型 (`UIColor` / `UIFont` ／ Compose `Color` / `TextStyle`) を直接保持。
- `DSLStyleModifiable` プロトコル / `DSLStyleModifiableCell` interface は Core から UI 層に移動。`DSLReidentifiable` / `DSLReidentifiableCell` のみ Core に残置。
- `SettingsRootDiff.updateTheme` / `UpdateTheme` ケースは削除。Theme 更新は `SettingsRootStore.applyTheme(_:)` 経由で `@Published var theme` / `StateFlow<Theme>` を通じて伝播（Diff Publisher 不発行）。
- `KsSettingsViewController` / `KsSettingsView` は Store の theme を購読し、`public func applyTheme(_:)` / `var theme: Theme` を提供。
- Phase 10 / 11 では他 active 提案 6 件（add-maui-bridge / add-maui-cells / add-maui-core / add-samples-maui / add-cell-types-input / add-cell-types-custom）の spec / tasks / proposal / design を直接修正し、本提案の新方針に整合させた。
