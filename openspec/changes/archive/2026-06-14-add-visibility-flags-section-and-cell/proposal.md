## Why

オリジナル `AiForms.Maui.SettingsView` には `Section.IsVisible` / `CellBase.IsVisible` があり、「データには残しつつ UI から非表示にする → 再表示で元の位置に復活する」というパターンを提供していた。本リポジトリの KsSettingsView ではこれが未移植であり、条件付き表示（フォーム状態に応じた段階表示）の頻出パターンを宣言的に書けない。

DSL 上で `if (condition) { Cell(...) }` と書けば描画から除外はできるが、Cell インスタンスは配列から消えるため、id 保持・元位置復帰・内部状態保持・将来のアニメーション拡張の余地が失われる。`isVisible` フラグはこれらを宣言的に表現する標準パターンとして、本リポジトリでも提供する。

本変更は「オリジナル移植漏れ対応」シリーズ（`openspec/drafts/05-port-gap-change-plan-roadmap.md`）の Change 3 として位置付けられる。Change 1（Theme/CellStyle 移植漏れ補完）と Change 2（共通行レイアウト + 全 Cell 共通フィールド統一）は既にアーカイブ済み。

## What Changes

- **Section に `isVisible: Bool`（既定 `true`）フィールドを追加** — Core ドメインモデル `Section` に追加し、`Hashable` / `equals` に含める。
- **全 7 種 Cell に `isVisible: Bool`（既定 `true`）フィールドを追加** — `LabelCell` / `CommandCell` / `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` に追加し、`Hashable` / `equals` に含める。既存 `isEnabled` と並列するフィールドとして扱う。
- **`VisibilityAware` opt-in 抽象を UI 層に新規追加** — `var isVisible: Bool { get }` を要求するプロトコル / interface を UI 層（iOS: `KsSettingsViewUI`、Android: `ks-settingsview-ui`）に置く。7 Cell が opt-in 準拠する。Core 抽象 `Cell` / `KsCell` には乗せない（純化方針維持）。将来追加される Cell も準拠宣言だけでフィルタ層に乗せられる構造とする。
- **可視性の意味論を仕様化** — `isVisible = false` の Section / Cell は UI から完全除外され、データソース（`SettingsRoot`）には保持される。`true` に戻したとき元の位置に復活する。
- **`isEnabled` との独立性を仕様化** — 両者は独立フラグ。`isVisible = false` のとき `isEnabled` の値はモデル値として保持されるが視覚効果は発生しない。
- **「表示状態同期の二層分離」原則を「表示状態同期の三層分離」に rename** — 既存の (1) 構造同期 / (2) 内容更新 に加え、第三カテゴリ「(3) 可視性変化」を追加。UI 層は model（hidden 含むフル状態）と visible projection（visible のみ）を分離管理する規約とする。
- **DSL diff 算出ロジックの可視性検出規約を追加** — DSL で可視性変化を検出した場合、SwiftUI / Compose の DSL diff 算出ロジックは `SettingsRootDiff.Full(newRoot)` を発行する規約とする。通常の `ReplaceCell`（reconfigure 経路）に visibility 差分を流してはならない。
- **`ReplaceCell` / `ReplaceSection` での visibility 切替を MUST NOT 化 + 防御挙動規約** — DSL / アプリ層からの visibility だけを変える `Replace*` 操作を MUST NOT とし、UI 層は受け取った場合に Full 経路へフォールバックすべき (SHOULD)。
- **部分 Diff の `index` 規約** — `InsertCell` / `MoveCell` / `Insert/MoveSection` 等の `index` 引数は model 配列基準（hidden 含む）とし、UI 層が visible projection への変換を行う。hidden 対象を指す部分 Diff の UI 操作は no-op とする。
- **DSL に `isVisible` 引数追加** — iOS SwiftUI DSL / Android Compose DSL の Section / 各 Cell ヘルパに `isVisible: Bool = true` 引数を追加。
- **破壊的変更なし** — 既定値が `true` のため既存呼び出しは引数省略で互換維持される。

## Capabilities

### New Capabilities

なし。

### Modified Capabilities

- `settings-view-core`: `Section` ドメインモデル Requirement に `isVisible` フィールドを追加。「表示状態同期の二層分離」Requirement を「表示状態同期の三層分離」へ rename し、第三カテゴリ「可視性変化」を追加。
- `cell-types-basic`: 「全 Cell 共通の isVisible」Requirement を新規追加（7 種 Cell の `isVisible` フィールド契約、`isEnabled` との独立性、Scenario 群）。
- `settings-view-ios-host`: 「visible projection の二重管理」「部分 Diff の index 規約」「hidden 対象の no-op 規約」「DSL/アプリ層からの `Replace*` visibility 切替 MUST NOT + 防御フォールバック」Requirement を追加。
- `settings-view-android-host`: `flatten` 経路での visibility フィルタ規約、`ReplaceCell` / `ReplaceSection` の visibility 切替時の Full 経路フォールバック規約を追加。
- `settings-view-ios-swiftui`: 既存「DSL → SettingsRootDiff 算出ロジック」Requirement に可視性差分の `Full` 発行規約を追加。Section / 各 Cell DSL ヘルパに `isVisible` 引数追加 Requirement を追加。
- `settings-view-android-compose`: 既存「DSL → SettingsRootDiff 算出ロジック（Compose）」Requirement に同等の規約を追加。Section / 各 Cell DSL ヘルパに `isVisible` 引数追加 Requirement を追加。

## Impact

### 影響モジュール

- **Core**: `ios/Sources/KsSettingsViewCore/Section.swift` / `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Section.kt`
- **UI 層 Cell モデル**: 7 Cell の struct / data class（iOS / Android 両方）
- **UI 層抽象**: `VisibilityAware.swift` / `VisibilityAware.kt`（新規）
- **iOS ホスト層**: `KsSettingsViewController.swift`（visible projection 化、部分 Diff の変換層、layout/supplementary/separator の visible projection 参照化）
- **Android ホスト層**: `KsSettingsView.kt` の `flatten()` フィルタ追加、`applyDiff` の `ReplaceCell` / `ReplaceSection` 防御挙動追加
- **DSL**: `DSLDiffCalculator.swift` / `DSLDiffCalculator.kt` に可視性差分検出ロジック追加、Section / 各 Cell DSL ヘルパに `isVisible` 引数追加
- **Samples**: `samples/ios` / `samples/android` に「条件付き非表示」サンプルページを追加

### 影響仕様（spec delta）

上記「Modified Capabilities」の 6 spec。

### 既存 in-progress change との関係

`add-cell-types-input` / `add-cell-types-custom` / `add-maui-*` が並行中だが、本 change の追加フィールド（`isVisible` / `VisibilityAware` 命名）が先取りされていないことを着手前に確認する。

### 破壊的変更とリスク

- **破壊的変更**: なし。`isVisible` の既定値が `true` のため既存呼び出しは引数省略で互換維持。
- **`Hashable` / `equals` への `isVisible` 追加に伴うリスク**: 同一 id・他フィールド同一・`isVisible` だけ異なるインスタンスが Store の `distinctUntilChanged` 相当で識別される必要があるため、`equals` に含める。これは値型としての等価性を保つ範疇であり、既存原則「構造同期は id 同一性のみ」と矛盾しない（構造同期は `Hashable` を判定に使わず、id のみで判定する）。
- **「表示状態同期の二層分離」rename に伴うリスク**: archive 済み change（`refactor-display-state-sync`）の文言は変更しない。live spec の Requirement 名のみ rename する。
