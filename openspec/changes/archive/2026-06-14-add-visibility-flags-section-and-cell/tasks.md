## 1. 着手前チェック

- [x] 1.1 並行 in-progress change（`add-cell-types-input` / `add-cell-types-custom` / `add-maui-*`）が `isVisible` / `VisibilityAware` 命名を先取りしていないか確認
- [x] 1.2 archive 済み `refactor-display-state-sync` の「二層分離」rename について、archive ファイル本体には触らず live spec のみ rename する方針を確認
- [x] 1.3 オリジナル `AiForms.Maui.SettingsView/SettingsView/Section.cs`（`IsVisible` L193-210）および `Cells/CellBase.cs`（`IsVisible` L397-414）を読み、移植元の意図・既定値・挙動を確認

## 2. Core ドメインモデル拡張

- [x] 2.1 iOS `KsSettingsViewCore/Section.swift` に `isVisible: Bool`（既定 `true`）を追加し、`Hashable` 手動実装（`==` / `hash(into:)`）に含める
- [x] 2.2 Android `ks-settingsview-core/.../Section.kt` に `isVisible: Boolean = true` を追加（data class の自動 equals / hashCode に含まれる）
- [x] 2.3 iOS / Android Core の `Section` テスト追加：`isVisible` 既定値、値型としての等価性、`isVisible` のみ異なるインスタンスが等価とみなされないこと

## 3. UI 層 `VisibilityAware` 抽象の新規追加

- [x] 3.1 iOS `KsSettingsViewUI/VisibilityAware.swift` を新規作成：`public protocol VisibilityAware { var isVisible: Bool { get } }`
- [x] 3.2 Android `ks-settingsview-ui/.../VisibilityAware.kt` を新規作成：`interface VisibilityAware { val isVisible: Boolean }`

## 4. iOS UI 層 7 Cell の拡張

- [x] 4.1 `LabelCell.swift` に `isVisible: Bool`（既定 `true`）追加 + `VisibilityAware` 準拠 + `Hashable` / `withDSLID` / `withStyle` / `withIcon` 経路で保持
- [x] 4.2 `CommandCell.swift` 同上
- [x] 4.3 `ButtonCell.swift` 同上
- [x] 4.4 `SwitchCell.swift` 同上
- [x] 4.5 `CheckboxCell.swift` 同上
- [x] 4.6 `RadioCell.swift` 同上
- [x] 4.7 `SimpleCheckCell.swift` 同上
- [x] 4.8 iOS Cell モデルの単体テスト追加：各 Cell の `isVisible` 既定値、`Hashable` 等価性、DSL 経路（`withDSLID` / `withStyle`）での `isVisible` 保持

## 5. Android UI 層 7 Cell の拡張

- [x] 5.1 `LabelCell.kt` に `isVisible: Boolean = true` 追加 + `VisibilityAware` 準拠
- [x] 5.2 `CommandCell.kt` 同上
- [x] 5.3 `ButtonCell.kt` 同上
- [x] 5.4 `SwitchCell.kt` 同上
- [x] 5.5 `CheckboxCell.kt` 同上
- [x] 5.6 `RadioCell.kt` 同上
- [x] 5.7 `SimpleCheckCell.kt` 同上
- [x] 5.8 Android Cell モデルの単体テスト追加：各 Cell の `isVisible` 既定値、equals / hashCode の整合性、`copy()` 経路での `isVisible` 保持

## 6. iOS ホスト層の visible projection 化

- [x] 6.1 `KsSettingsViewController` の snapshot 構築経路で `Section.isVisible` および各 Cell の `VisibilityAware.isVisible` に基づくフィルタを実装し、snapshot / 内部状態を visible projection で構成する
- [x] 6.2 `indexPath` 経由の描画系（layout / supplementary view / separator）が visible projection ベースの sections を参照するように修正
- [x] 6.3 `Section.isVisible` の状態に応じた `supplementaryModes` / layout mode の再評価ロジックを visible projection ベースで実装
- [x] 6.4 部分 Diff の各ケース（`insertCell` / `removeCell` / `replaceCell` / `moveCell` / `insertSection` / `removeSection` / `moveSection` / `replaceSection` / `updateAccessory`）で `index` 引数を model 配列基準として解釈し、hidden 対象を no-op として処理する経路を実装
- [x] 6.5 `applyReplaceCell` で旧→新の `isVisible` 切替を検出した場合に Full 経路へフォールバックする防御ロジックを実装
- [x] 6.6 `applyReplaceSection` を Full 経路フォールバック（内部 cells / accessory / visibility の任意変化に対応）として実装
- [x] 6.7 iOS ホスト層の `Section(...)` 手動再構築箇所（`SettingsRootStore.swift` / `KsSettingsViewController.swift`）を棚卸しし、すべてで `isVisible` を保持するように更新

## 7. Android ホスト層の flatten フィルタと防御挙動

- [x] 7.1 `KsSettingsView.kt` の `flatten()` で `Section.isVisible` および各 Cell の `VisibilityAware.isVisible` に基づくフィルタを実装
- [x] 7.2 部分 Diff の各ケースで hidden 対象が flatten 経路で自然に no-op になることをコメントで明示
- [x] 7.3 `applyReplaceCell` で旧→新の `isVisible` 切替を検出した場合に `setRootDirect(internalRoot, internalTheme)` 相当（Full 経路）へフォールバック
- [x] 7.4 `applyReplaceSection` を `setRootDirect(internalRoot, internalTheme)` 相当の Full 経路フォールバックとして実装

## 8. iOS SwiftUI DSL の拡張

- [x] 8.1 SwiftUI DSL の Section ヘルパに `isVisible: Bool = true` 引数を追加し、生成される `Section` ドメインモデルに反映
- [x] 8.2 SwiftUI DSL の 7 Cell ヘルパ（`LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell`）に `isVisible: Bool = true` 引数を追加し、生成される Cell モデルに反映
- [x] 8.3 `DSLDiffCalculator.compute` 冒頭で `containsVisibilityChange(from:to:)` preflight を実施し、可視性変化を検出した場合は `.full(newRoot)` のみを発行する経路を実装

## 9. Android Compose DSL の拡張

- [x] 9.1 Compose DSL の Section ヘルパに `isVisible: Boolean = true` 引数を追加し、生成される `Section` ドメインモデルに反映
- [x] 9.2 Compose DSL の 7 Cell ヘルパに `isVisible: Boolean = true` 引数を追加し、生成される Cell モデルに反映
- [x] 9.3 `DSLDiffCalculator.compute` / `contentUpdates` 冒頭で `containsVisibilityChange(from, to)` preflight を実施し、可視性変化を検出した場合は `compute` が `Full(newRoot)` のみ発行・`contentUpdates` が空リストを返す経路を実装

## 10. テスト

- [x] 10.1 `Section.isVisible = false` で当該セクション（header / footer / cells すべて）が非表示になるテスト（iOS / Android）
- [x] 10.2 `Cell.isVisible = false` で当該セルのみ非表示になるテスト（iOS / Android）
- [x] 10.3 `isVisible` を `true ⇄ false` で toggle したときに構造同期上の挿入・削除として検出されるテスト（iOS / Android）
- [x] 10.4 既存呼び出し（`isVisible` 未指定）が既定 `true` でビルド・実行できる互換性テスト（iOS / Android）
- [x] 10.5 `isEnabled = false, isVisible = false` の Cell が描画されず色置換も発生しないテスト（iOS / Android）
- [x] 10.6 `isVisible: false → true` toggle で `isEnabled = false` がそのまま視覚効果として反映されるテスト（iOS / Android）
- [x] 10.7 hidden Cell を含む状態で `InsertCell(index: model 配列基準)` が正しい visible 順序で挿入されるテスト（iOS / Android）
- [x] 10.8 hidden Cell / Section に対する `RemoveCell` / `ReplaceCell` / `MoveCell` / `UpdateAccessory` が model 更新のみで UI 操作 no-op となるテスト（iOS / Android）
- [x] 10.9 hidden section の `UpdateAccessory` 後に当該 section を `isVisible = true` に戻すと、更新された accessory が反映されるテスト（iOS / Android）
- [x] 10.10 `ReplaceCell` で旧→新の `isVisible` 切替時に Full 経路フォールバックが発火するテスト（iOS / Android）
- [x] 10.11 `ReplaceSection` で任意の内部変化（cells 集合変化 / 内部 cell visibility 変化 / H/F 変化）が Full 経路フォールバックで反映されるテスト（iOS / Android）
- [x] 10.12 全 Section / 全 Cell が hidden の状態で空表示・クラッシュなしのテスト（iOS / Android）
- [x] 10.13 hidden Section が先頭にある状態で separator / supplementary / accessory view が正しい visible section を参照するテスト（iOS）
- [x] 10.14 hidden Section が中間にある状態（複数 hidden 連続含む）で同上のテスト（iOS）
- [x] 10.15 partial Section 操作で visibility 変化時に section ごとの header / footer 表示が visible projection に追従するテスト（iOS）
- [x] 10.16 DSL 経由の `Section.isVisible` / `Cell.isVisible` toggle が `Full` 経路として処理されるテスト（4 パターン：visibility のみ / visibility + 内容変化 / visibility + Section H/F 変化 / Section visibility + Cell 内容変化）（iOS / Android）

## 11. サンプル

- [x] 11.1 iOS sample アプリに「条件付き非表示」サンプルページを追加（フォーム状態に応じた段階表示）
- [x] 11.2 Android sample アプリに同等のサンプルページを追加

## 12. ビルド・最終検証

- [x] 12.1 iOS `swift test` が全件成功
- [x] 12.2 Android `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` が全件成功
- [x] 12.3 iOS / Android sample アプリで「条件付き非表示」サンプルが期待通りに動作することを目視確認（iOS は案 A 適用後の修正 + 4 トグル構成サンプル、Android は元から正常動作を実機目視で確認済み）

## 依存関係

- フェーズ 2（Core）はフェーズ 1 完了後に実施
- フェーズ 3（VisibilityAware 抽象）はフェーズ 4, 5 の前に実施
- フェーズ 4, 5（UI 層 Cell 拡張）はフェーズ 2, 3 完了後に並列実施可能
- フェーズ 6, 7（ホスト層）はフェーズ 4, 5 完了後に並列実施可能
- フェーズ 8, 9（DSL）はフェーズ 4, 5 完了後に並列実施可能（フェーズ 6, 7 とも並列可能）
- フェーズ 10（テスト）はフェーズ 6, 7, 8, 9 完了後に実施
- フェーズ 11（サンプル）はフェーズ 8, 9 完了後に実施
- フェーズ 12（最終検証）はフェーズ 10, 11 完了後に実施

## 完了条件

- iOS / Android 両プラットフォームで `Section.isVisible` / `Cell.isVisible` が機能し、本 change の spec delta に記載されたすべての Scenario がテストで検証される
- 既存呼び出しが破壊されない（既定値 `true` で互換維持）
- フェーズ 12 のビルド・テスト・目視確認がすべてパス
