# レビュー結果 - refactor-display-state-sync

**レビュー日時**: 2026年06月03日  
**レビュワー**: sdd-reviewer  
**変更提案ID**: refactor-display-state-sync  
**再レビュー**: review-result_001.md（CHANGES_REQUESTED）の修正反映後

## サマリー

前回（review-result_001.md）で指摘した Critical 1 件・Major 1 件・Minor 1 件が、本ラウンドの修正で**確実に解消されている**ことをコードと実行テストで再検証した。

- **Critical（iOS `KsCellID` の内容ハッシュによる spec 違反 + 連続内容更新の破綻）**: 解消。`KsCellID` の `==` / `hash(into:)` は `id`（UUID）のみを対象とし、`contentHash` フィールド自体が削除された（リポジトリ全体に `contentHash` 参照は 0 件）。`SettingsRootStore` / `KsSettingsViewController` の Cell 照合はすべて `$0.id == cellID.id` に統一。`applyReplaceCell` の `snapshot.itemIdentifiers.contains(cellID)` は id 限定識別子に対して安定し、同一 id への 2 回以上連続の内容更新でも snapshot とドリフトしない。core spec の中核要件「構造同期は id 同一性のみを用いる（MUST）／内容を用いてはならない（MUST NOT）」に正しく整合した。
- **Major（reconfigure 経路の実行カバレッジゼロ）**: 実質解消。Critical の**根本原因である identity ロジック**を、UIKit に依存しない Core 層テスト `KsCellIDTests`（6 件、`#if canImport(UIKit)` ガードなし）で**macOS ホストの `swift test` で実際に実行**して担保。`test_同一idへの連続内容更新で_KsCellID_が常に同一` が前回の連続更新破綻を直接検出する。本レビューで `swift test --filter KsCellIDTests` → Executed 6 tests, 0 failures を実機確認した。UI 層（`ApplyDiffTests` / `SettingsRootStoreTests`）にも連続 replaceCell の回帰テストが追加されている（後述の残課題参照）。
- **Minor（DSLDiffCalculator の古いレビュー参照コメント）**: 解消。`review-result_001.md Major-4` 参照は削除され、二層分離（構造同期=`KsCellID` は id 限定 / 内容比較=`.replaceCell` 発行判定）を明示するコメントに整理された。

独立に再現確認した検証結果:

- `swift build`: Build complete（成功）
- `swift test`: **Executed 130 tests, 0 failures**（前回 123 → KsCellID 系 6 件 + 連続更新テストで増加）
- `swift test --filter KsCellIDTests`: **Executed 6 tests, 0 failures**（ホスト実行を確認）
- Android `:ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:testDebugUnitTest`: **BUILD SUCCESSFUL**（前回承認済みの状態から無変更・回帰なし）
- `openspec validate refactor-display-state-sync --strict`: **valid**

Android 側は本ラウンドの修正対象外（iOS の identity 修正のみ）であり、回帰の影響は及んでいない。spec / design / tasks.md とも整合している。

**判定: `APPROVED`**

---

## 前回指摘の解消状況（再検証）

### ✅ [解消] 🔴 Critical: iOS `KsCellID` の内容ハッシュによる spec 違反 + 連続内容更新の破綻

**該当箇所**: `ios/Sources/KsSettingsViewCore/KsCellID.swift:51-79`

**修正内容と検証**:

- `KsCellID` は `id: UUID` のみを保持し、`contentHash` フィールドは削除された。`==` は `lhs.id == rhs.id`、`hash(into:)` は `hasher.combine(id)` のみ。`grep -rn "contentHash" ios/Sources` の結果は **0 件**。
- `SettingsRootStore.replaceCell` / `moveCell` / `removeCell`（`SettingsRootStore.swift:177, 209, 239`）はすべて `target.cells.firstIndex(where: { $0.id == cellID.id })` で id 照合に統一。`KsCellID(cell:) == cellID`（contentHash 込み）照合は完全に排除された。
- `KsSettingsViewController.applyReplaceCell`（`KsSettingsViewController.swift:851, 861, 889`）は `snapshot.itemIdentifiers.contains(cellID)`（id 限定）→ `cellIndex[cellID] = new`（id キーは安定）→ Section モデルを `$0.id == cellID.id` で更新 → `snapshot.reconfigureItems([cellID])`。reconfigure は識別子を変えないため、**2 回目以降の連続 replaceCell でも `contains` が外れない**。前回シナリオ（更新2 で `(X,h"B")` が snapshot に無く `false` → クラッシュ／更新欠落）は構造的に発生し得なくなった。
- 連続更新の回帰検証は host 実行可能な `KsCellIDTests.test_同一idへの連続内容更新で_KsCellID_が常に同一` で担保され、本レビューで実行確認済み。

core spec（`specs/settings-view-core/spec.md:9`）・iOS spec（`specs/settings-view-ios-ui/spec.md:7,11`）の MUST / MUST NOT に正しく整合。

### ✅ [解消] 🟠 Major: reconfigure 経路の実行カバレッジゼロ

**該当箇所**: `ios/Tests/KsSettingsViewCoreTests/KsCellIDTests.swift`（新設、ガードなし）

**修正内容と検証**:

- Critical の根本原因（identity ロジック）を UIKit 非依存の Core 層テストに切り出し、`#if canImport(UIKit)` ガードを付けないことで macOS ホストの `swift test` で**実際に実行**されるようにした。本レビューで `swift test --filter KsCellIDTests` → Executed 6 tests, 0 failures を確認。
- 6 件は「同一 id・内容違いで等価」「同一 id・型違いで等価」「id 違いで非等価」「`init(id:)` と `init(cell:)` の整合」「**連続内容更新で常に同一**」「Cell 自身の `==` は内容差を検出（別レイヤ）」を網羅し、二層分離の identity 契約を過不足なく検証している。
- 加えて UI 層 `ApplyDiffTests.test_applyDiff_replaceCell_同一idへの2回連続更新が両方反映される` と `SettingsRootStoreTests.test_replaceCell_同一idへの2回連続更新が両方反映される` を追加し、Store / Controller レベルの連続更新も検証対象に含めた。

### ✅ [解消] 🟡 Minor: DSLDiffCalculator の古いレビュー参照コメント

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:148-181`

**修正内容と検証**:

- `// ## Cell の内容比較規約（review-result_001.md Major-4 対応）` の文言は削除され、`// ## 二層分離における Cell 内容比較の位置づけ（refactor-display-state-sync）` に置換。
- 「構造同期の identity は `KsCellID`（id のみ）で内容を用いない（MUST NOT）」「内容比較は `.replaceCell` 発行判定にのみ使う」点が明示され、`KsCellID` の役割と矛盾しない説明に整理された。

---

## 新規リグレッション・整合性の確認

- **Store と Controller snapshot の identity 整合**: 両者とも `KsCellID = id` を識別子とし、Cell 照合は `$0.id == cellID.id` に統一。`cellIndex[KsCellID:]` のキーは内容変化で変わらないため、reconfigure 後も最新 Cell が同一キーで引ける。ドリフトの余地は解消。
- **DSLDiffCalculator との役割分担**: `DSLDiffCalculator.cellLevelDiffs` は `AnyHashable(oldCell) != AnyHashable(cell)`（内容全体比較）で `.replaceCell` 発行を判定し、構造同期（item 集合・順序）には `KsCellID`（id 限定）を使う。二層が正しく分離されている。move + 内容変化時に `.moveCell` と `.replaceCell` を別々に発行する経路（`DSLDiffCalculator.swift:215-220`）も id 限定識別子で安定。
- **Android への影響**: 本ラウンドの修正は iOS の identity に限定。Android のユニットテストは前回承認済みの状態から無変更（BUILD SUCCESSFUL / UP-TO-DATE）で回帰なし。Android 側の二層分離（`areContentsTheSame`=同一 id で true、`getItemId`=id ベース、`DSLDiffCalculator` は内容変化で ReplaceCell 非発行、ViewHolder 部分更新）は前回どおり spec 準拠。
- **spec / design / tasks.md 整合**: core / iOS / Android の spec は MUST / MUST NOT を満たす。tasks.md 0〜6 章・8 章は完了チェック済みで実装と一致。7 章（実機・シミュレータ検証 4 件）は未チェックのまま残置されており、これは実機/シミュレータ操作が必要なオーナー側タスクとして妥当（ユニットテスト・ビルドは PASS 済み）。

---

## 指摘事項（残課題）

### 🔵 Suggestion: UI 層の連続 replaceCell テストはホスト未実行（マージ前にシミュレータ 1 回実行を推奨）

**該当箇所**: `ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift:9`（`#if canImport(UIKit)`）、`SettingsRootStoreTests.swift:10`

**内容**: 追加された UI 層の連続 replaceCell 回帰テストは `#if canImport(UIKit)` ガードのため macOS ホストでは依然コンパイルのみ（非実行）。ただし Critical の**根本原因である identity ロジック自体**は host 実行される `KsCellIDTests` で担保されており、reconfigure の実描画挙動は tasks.md §7.3（シミュレータ/実機検証、オーナー実施）でカバーされる。本提案の判定を妨げるものではない。マージ前または §7 検証時に一度シミュレータで `KsSettingsViewUITests` を実行し、`reconfigureItems` の実挙動（連続更新・ちらつき非発生）を確認することを推奨する（任意）。

### 🔵 Suggestion: Android `submitContentUpdate` の payload 化（前回からの継続・任意）

前回 review-result_001.md の Suggestion を継続。実機検証（tasks §7.1）で `notifyItemChanged`（payload なし）にちらつき・性能問題がなければ現状維持で可。本提案範囲では必須ではない。

---

## アクションプラン

1. （任意）マージ前または §7 検証時に iOS シミュレータで `KsSettingsViewUITests` を 1 回実行し、reconfigure 連続更新の実描画挙動を確認する。
2. （任意）tasks.md §7（実機・シミュレータ検証 4 件）をオーナー側で実施し、ちらつき解消を最終確認する。
3. （任意）Android `submitContentUpdate` の payload 化は実機検証結果次第で検討。

いずれも APPROVED を妨げる項目ではない。

---

## 判定結果

**ステータス**: `APPROVED`

理由: 前回の Critical（iOS `KsCellID` の内容ハッシュによる spec 違反 + 連続内容更新の破綻）・Major（reconfigure 経路の実行カバレッジゼロ）・Minor（古いレビュー参照コメント）はいずれもコードと実行テストで解消を確認した。`KsCellID` は id 同一性のみに限定され core / iOS spec の MUST / MUST NOT に整合。連続内容更新の回帰は host 実行される `KsCellIDTests` で担保され、`swift test` 130 件・Android テスト・`openspec validate --strict` はすべて PASS。新たなリグレッション・矛盾は検出されなかった。残る Suggestion 2 件はいずれも任意であり、マージを妨げない。実機/シミュレータ検証（tasks §7）はオーナー側タスクとして残置が妥当。
</content>
</invoke>
