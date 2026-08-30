## Verification Report: purify-core-extract-style-to-ui-layer（iOS スコープ）

**検証日**: 2026-06-07
**検証スコープ**: iOS 関連の spec delta と実装の整合（Android 側は別セッション）

---

### Summary

| Dimension    | Status                                    |
|--------------|-------------------------------------------|
| Completeness | 49/104 タスク完了（iOS 分はすべて完了）    |
| Correctness  | iOS スコープ内の全 Requirement を実装確認  |
| Coherence    | design.md の全 Decision に従っている       |

---

### Completeness

**タスク完了状況**

- 完了タスク（`[x]`）: 49 件
- 未完了タスク（`[ ]`）: 55 件（すべて Android / MAUI Phase 3/5/6-Android/7-Android/8-Android/9-Android/10/11/12-Android 関連）
- iOS 側タスク（Phase 2/4/6-iOS/7-iOS/8-iOS/9-iOS/12-iOS）: **すべて完了**
- Android 側タスク: 意図的に未完了として残存（別セッション対応）

**スペック要件カバレッジ（iOS）**

| Spec                         | 確認結果                            |
|------------------------------|-------------------------------------|
| settings-view-core           | 全 Requirement 実装済み             |
| settings-view-ios-style      | 全 Requirement 実装済み             |
| settings-view-ios-host       | 全 Requirement 実装済み             |
| settings-view-ios-swiftui    | 全 Requirement 実装済み             |
| settings-view-ios-theme-bridge | 全 Requirement 実装済み           |
| cell-types-basic（iOS 部分）  | 全 Requirement 実装済み             |
| samples-ios                  | 全 Requirement 実装済み             |

---

### Correctness

**Requirement 実装マッピング（iOS）**

1. **SettingsRoot から theme フィールド削除**
   - 実装: `ios/Sources/KsSettingsViewCore/SettingsRoot.swift`（`sections` のみ保持）
   - spec: settings-view-core — "SettingsRoot ドメインモデル" MODIFIED
   - 判定: 整合

2. **KsCell から style プロパティ要求削除**
   - 実装: `ios/Sources/KsSettingsViewCore/KsCell.swift`（`id: UUID` のみ要求）
   - spec: settings-view-core — "Cell 抽象" MODIFIED
   - 判定: 整合

3. **SettingsRootDiff から updateTheme ケース削除**
   - 実装: `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift`（10 ケース、`updateTheme` なし）
   - spec: settings-view-core — "SettingsRootDiff 型" MODIFIED
   - 判定: 整合

4. **Core から KsColor/KsFont/KsImage/Theme/CellStyle 削除**
   - 実装: `ios/Sources/KsSettingsViewCore/` ディレクトリにこれらファイルは存在しない
   - spec: settings-view-core — "スタイル系型の Core 不在" ADDED
   - 判定: 整合

5. **UI 層に Theme 型を新規追加（UIColor/UIFont 直接保持）**
   - 実装: `ios/Sources/KsSettingsViewUI/Theme.swift`
   - 全フィールド（separatorColor / cellBackgroundColor / selectedColor / cellAccentColor / headerTextColor / headerBackgroundColor / footerTextColor / footerBackgroundColor / scrollIndicatorVisible / viewBackgroundColor / rowHeight / hasUnevenRows / disabledTextColor / headerFontSize / footerFontSize / titleColor / titleFont）を `UIColor` / `UIFont` 直接保持
   - Equatable 手動実装（`isEqual` ベース）
   - spec: settings-view-ios-style — "Theme 型 (UI 層)" ADDED
   - 判定: 整合

6. **UI 層に CellStyle 型を新規追加**
   - 実装: `ios/Sources/KsSettingsViewUI/CellStyle.swift`
   - 全フィールド（titleColor / titleFont / descriptionColor / descriptionFont / valueTextColor / valueTextFont / iconSize / iconRadius / cellHeight / hintTextColor / hintTextFont / backgroundColor / accentColor）を `UIColor?` / `UIFont?` / `CGFloat?` 直接保持
   - Equatable 手動実装
   - spec: settings-view-ios-style — "CellStyle 型 (UI 層)" ADDED
   - 判定: 整合

7. **UI 層に KsImage 型を新規追加**
   - 実装: `ios/Sources/KsSettingsViewUI/KsImage.swift`
   - `case systemName(String)` / `case uiImage(UIImage)` の 2 ケース
   - Hashable 手動実装（systemName: String hash、uiImage: ObjectIdentifier）
   - spec: settings-view-ios-style — "KsImage 型 (UI 層)" ADDED
   - 判定: 整合

8. **各 Cell の accentColor / titleColor が UIColor? に変更**
   - ButtonCell.titleColor: `UIColor?`
   - SwitchCell.accentColor: `UIColor?`
   - CheckboxCell.accentColor: `UIColor?`
   - LabelCell / CommandCell: `KsImage?` アイコンを保持
   - spec: cell-types-basic — "ButtonCell" / "SwitchCell" / "CheckboxCell" MODIFIED
   - 判定: 整合

9. **SettingsRootStore に @Published theme と applyTheme 追加**
   - 実装: `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift`
   - `@Published public private(set) var theme: Theme`
   - `public func applyTheme(_ theme: Theme)` — Diff Publisher を発行しない（同値抑制あり）
   - `init(initialRoot: SettingsRoot, initialTheme: Theme = Theme())`
   - spec: settings-view-ios-host — "SettingsRootStore（iOS）" MODIFIED
   - 判定: 整合

10. **KsSettingsViewController の applyTheme 経路**
    - 実装: `KsSettingsViewController.swift`
    - `internal private(set) var currentTheme: Theme`
    - `public func applyTheme(_ theme: Theme)` — `viewBackgroundColor` 反映・`reconfigureVisibleCells` 実行
    - Store.$theme を購読して自動反映
    - spec: settings-view-ios-host — "KsSettingsViewController の公開 API" MODIFIED
    - 判定: 整合

11. **SwiftUI KsSettingsView の .theme(_:) modifier**
    - 実装: `KsSettingsView.swift`
    - `.theme(_ theme: Theme) -> KsSettingsView` modifier が存在
    - Store 方式・DSL 方式両方で `store.applyTheme(theme)` 経路で反映
    - spec: settings-view-ios-swiftui — "SwiftUI ラッパ KsSettingsView" MODIFIED
    - 判定: 整合

12. **EffectiveStyle の変換コード削除（UIColor/UIFont 直接利用）**
    - 実装: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift`
    - `KsColor` / `KsFont` 変換コードは存在しない。`UIColor` 直接合成
    - 3 段階優先順位（CellStyle → Theme → プラットフォーム既定）実装済み
    - `titleColorIsExplicit` フラグ実装済み
    - spec: settings-view-ios-theme-bridge — "Theme / CellStyle の UIKit 変換" MODIFIED
    - 判定: 整合

13. **UIColor+KsColor.swift 削除**
    - 実装: `ios/Sources/KsSettingsViewUI/` に当該ファイルは存在しない
    - spec: settings-view-ios-theme-bridge — "KsColor 変換ユーティリティの不在" Scenario
    - 判定: 整合

14. **DSLStyleModifiable を UI 層に移動**
    - 実装: `ios/Sources/KsSettingsViewUI/DSLStyleModifiable.swift`
    - spec: settings-view-ios-swiftui — `DSLStyleModifiable` は KsSettingsViewUI モジュールに定義
    - 判定: 整合

15. **DSLReidentifiable は Core に残置**
    - 実装: `ios/Sources/KsSettingsViewCore/DSLCellIdentity.swift`（`DSLReidentifiable` のみ）
    - spec: settings-view-ios-swiftui — `DSLReidentifiable` は KsSettingsViewCore モジュールに定義
    - 判定: 整合

16. **DSLIconModifiable を UI 層に新設**
    - 実装: `ios/Sources/KsSettingsViewUI/DSLIconModifiable.swift`
    - `.icon(_ icon: KsImage)` modifier が `CellModifiers.swift` に実装済み
    - spec: settings-view-ios-swiftui — ".icon modifier の型" Scenario
    - 判定: 整合

17. **Cell modifier の型（UIColor / UIFont）**
    - `.titleColor(_ color: UIColor)` / `.backgroundColor(_ color: UIColor)` / `.font(_ font: UIFont)` が `CellModifiers.swift` に実装
    - spec: settings-view-ios-swiftui — "Cell modifier の型" Scenario
    - 判定: 整合

18. **Core テスト削除**
    - ThemeTests.swift / CellStyleTests.swift / KsImageTests.swift: KsSettingsViewCoreTests ディレクトリに存在しない
    - SettingsRootTests.swift: theme 関連 Scenario を削除し `sections` のみの構築・等価性テストに修正済み
    - SettingsRootDiffTests.swift: updateTheme ケースのテストなし
    - spec: Phase 8 タスク
    - 判定: 整合

19. **SettingsRootStoreTests.swift の applyTheme テスト**
    - `test_applyTheme_storeのthemeが更新されDiffは発行されない` 実装済み
    - `test_applyTheme_同値ならtheme通知を抑制する` 実装済み
    - spec: settings-view-ios-host — "applyTheme メソッド呼び出し" Scenario
    - 判定: 整合

20. **Sample の UIColor 直接構築**
    - `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift`
    - 全色定数が `UIColor(red:green:blue:alpha:)` 形式（`KsColor` 参照なし）
    - Section 構成・セクション名・Cell タイトル・icon/CellStyle 適用が spec と一致
    - spec: samples-ios — "基本 Cell を含むデモ画面" MODIFIED
    - 判定: 整合

---

### Coherence

**design.md の Decision 準拠状況**

| Decision | 内容                                      | 実装確認    |
|----------|-------------------------------------------|-------------|
| 1        | KsColor / KsFont の Core 削除             | 確認済み    |
| 2        | Theme / CellStyle の UI 層移動            | 確認済み    |
| 3        | SettingsRoot.theme 削除、View 側引数化    | 確認済み    |
| 4        | KsCell から style 要求削除               | 確認済み    |
| 5        | SettingsRootDiff.updateTheme 除外         | 確認済み    |
| 6        | KsImage を Core から UI 層に移動          | 確認済み    |
| 7        | Cell Color パラメータ型の Native 化       | 確認済み    |
| 8        | iOS の色型を UIColor 1 本に統一           | 確認済み    |
| 10       | theme-bridge capability 縮小・継続        | 確認済み    |

---

### テスト・ビルド状況

- `swift test`（macOS ホスト、Core テスト）: **76 件すべて成功**（2026-06-07 実行確認）
- UI テスト（シミュレータ必須）: **83 件すべて成功**（直近レビュー `review-result_002.md` より）
- `openspec validate purify-core-extract-style-to-ui-layer`: **エラーなし**（当検証で確認）

---

### Issues

**CRITICAL（アーカイブ前に必須修正）**: なし

**WARNING（推奨修正）**: なし

**SUGGESTION（任意改善）**: なし

---

### Final Assessment

iOS スコープのすべてのタスク（Phase 2/4/6-iOS/7-iOS/8-iOS/9-iOS/12-iOS）が完了しており、
仕様（spec delta）と実装が完全に整合している。

- Core から KsColor / KsFont / KsImage / Theme / CellStyle が完全に削除されていることを確認。
- UI 層に UIColor / UIFont 直接保持の Theme / CellStyle / KsImage が正しく配置されていることを確認。
- SettingsRootStore.applyTheme / KsSettingsViewController.applyTheme 経路が仕様通り実装されていることを確認。
- テスト（Core 76 件 + UI 83 件）がすべてグリーン、openspec validate もエラーなし。
- Android 側タスク 55 件は意図的に未完了として残存（別セッション対応）。

**判定: VALID**（iOS スコープ内に限定した判定）
