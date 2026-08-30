# レビュー結果 - refactor-display-state-sync

**レビュー日時**: 2026年06月03日  
**レビュワー**: sdd-reviewer  
**変更提案ID**: refactor-display-state-sync  

## サマリー

「表示状態同期の二層分離」（構造同期=id 同一性のみ／内容更新=同一セルの部分更新）を core / Android / iOS に適用する変更提案。Android 側（`areContentsTheSame`=同一 id で常に true、`getItemId`=id ベース FNV-1a、`DSLDiffCalculator.compute`=構造のみ＋`contentUpdates` 列挙、ViewHolder 部分更新）は spec / design に良く整合しており、値型 equals への内部状態の復帰、Radio グループ連動の `contentUpdates` 経由反映も適切。検証ゲート（Android gradle test PASS、iOS swift test 123 PASS、`openspec validate --strict` PASS）はすべて再現確認できた。

しかし **iOS の構造同期識別子 `KsCellID` が内容ハッシュ（`contentHash`）を含んでおり、本提案 core 仕様の中核要件「構造同期は id 同一性のみを用いる（内容を用いてはならない / MUST NOT）」に正面から違反している**。さらにこの設計は、同一 id のセルに対して **内容更新が 2 回連続で発生すると 2 回目が必ず失敗する**（DEBUG: `assertionFailure` でクラッシュ、Release: 更新が黙って破棄される）リグレッションを生む。これは本提案が解消しようとしている「チェック操作・値更新」のユースケースそのものを iOS で壊す。`reloadItems → reconfigureItems` の置換自体は正しいが、その土台となる識別子が内容依存のままであるため、本提案の目的（構造同期の id 限定）が iOS では達成されていない。

加えて、iOS UI 層テスト（`KsSettingsViewUITests`、`applyReplaceCell` の reconfigure 検証を含む）は `#if canImport(UIKit)` ガードのため macOS ホストでは **コンパイルされるだけで実行されない**。すなわち iOS の reconfigure / replaceCell 経路は実行されたテストカバレッジがゼロであり、上記バグがテストをすり抜けている。

**判定: `CHANGES_REQUESTED`**

設計判断（「禁止されているのは DSL の構造 Diff 列に ReplaceCell を含めることのみで、ReplaceCell Diff 自体は内容更新を表す」）という implementer の解釈自体は spec / design に合致しており妥当です（Android の `contentUpdates → store.replaceCell → notifyItemChanged` 経路は問題なし）。問題はその一段下の層、iOS の `KsCellID.contentHash` にあります。

---

## 指摘事項

### 🔴 Critical: iOS `KsCellID` が内容ハッシュを含み、構造同期の id 限定要件に違反 + 連続内容更新を破壊

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/KsCellID.swift:44-58`（`contentHash` を含む `Hashable` 識別子）
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:845-894`（`applyReplaceCell`、`snapshot.itemIdentifiers.contains(cellID)`）
- `ios/Sources/KsSettingsViewUI/SettingsRootStore.swift:199-219`（`replaceCell`、`KsCellID(cell:) == cellID` 照合）
- `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:205-211`（`.replaceCell(cellID: KsCellID(cell: oldCell), ...)`）

**問題点**:

`KsCellID` は `DiffableDataSource<UUID, KsCellID>` の **Item 識別子**であり、これがまさに「構造同期の同一性」を担う。しかし `KsCellID` は

```swift
public struct KsCellID: Hashable, Sendable {
    public let id: UUID
    public let contentHash: Int   // ← Cell 全体の内容ハッシュ
    public init(cell: any KsCell) {
        self.id = cell.id
        self.contentHash = AnyHashable(cell).hashValue
    }
}
```

と `contentHash` を `==` / `hashValue` に含む。つまり **同一 id・内容違いの 2 つの Cell は異なる `KsCellID` になる**。これは本提案 core 仕様の中核要件：

> spec/settings-view-core: 構造同期の同一性判定は **id（識別子）の同一性のみ** を用いなければならず (MUST)、Cell の内容プロパティを判定に用いてはならない (MUST NOT)
> spec/settings-view-ios-ui: スナップショットの構造同期は `KsCellID`（id）の同一性のみで算出されなければならない (MUST)

に正面から違反する。spec は "`KsCellID`（id）" と書いており「KsCellID = id ベースの識別子」を前提にしているが、実装は内容も混ぜている。

**さらに実害（リグレッション）**: 同一 id のセルに内容更新が 2 回連続で起きると 2 回目が破綻する。

1. 初期: cell C0（id=X, "A"）。snapshot item = `KsCellID(id=X, h"A")`。`bookkeeper.lastTree` = C0。
2. 更新1: C1（id=X, "B"）。`compute` → `.replaceCell(cellID: KsCellID(C0)=(X,h"A"), new: C1)`。snapshot に `(X,h"A")` が在り `reconfigureItems` 成功。**reconfigure は識別子を変えないので snapshot は `(X,h"A")` のまま**。`lastTree` = C1。
3. 更新2: C2（id=X, "C"）。`compute(from: C1, to: C2)` → `.replaceCell(cellID: KsCellID(C1)=(X,h"B"), new: C2)`。`applyReplaceCell` は `snapshot.itemIdentifiers.contains((X,h"B"))` を見るが snapshot は `(X,h"A")` しか持たない → **`false`** → `reportMissingID` → DEBUG では `assertionFailure` クラッシュ、Release では更新が黙って消える。

この「同一 id セルの繰り返し内容更新」は、まさに本提案が直そうとしているチェック ON/OFF・スイッチ・値更新の典型シナリオ（外部 @State 駆動の再評価、ステッパー、テキスト入力など）であり、iOS で確実に踏む。`SettingsRootStore.replaceCell` も `KsCellID(cell:) == cellID`（contentHash 込み）で照合しており、Store 内部状態と Controller snapshot の `KsCellID` が時間差でドリフトして整合が崩れる。

なお本欠陥は変更前（`reloadItems` 時代）から潜在していたが、本提案が「構造同期は id のみ」を **確定仕様に格上げした** ことで、`KsCellID.contentHash` は明確な spec 違反となった。`reloadItems → reconfigureItems` の置換は正しいが、土台の識別子を id 限定にしない限り本提案の目的は iOS で達成されない。

**推奨修正**:

`KsCellID` を **id（UUID）のみ**で同一性判定する識別子に変更する。具体的には以下のいずれか:

- 案A（推奨）: `KsCellID` の `Hashable` を `id` のみに依存させる。`contentHash` は保持するとしても `==` / `hash(into:)` の対象から外す（または `contentHash` フィールド自体を削除）。
  ```swift
  public struct KsCellID: Hashable, Sendable {
      public let id: UUID
      public static func == (l: KsCellID, r: KsCellID) -> Bool { l.id == r.id }
      public func hash(into h: inout Hasher) { h.combine(id) }
      public init(cell: any KsCell) { self.id = cell.id }
  }
  ```
- これにより snapshot 識別子が内容変化で変わらなくなり、`applyReplaceCell` の `contains` も `applyMoveCell` / `removeCell` も常に id で一致する。`reconfigureItems([cellID])` は同一識別子に対して安定して効く。
- `SettingsRootStore.replaceCell` / `moveCell` / `removeCell` 内の `KsCellID(cell: $0) == cellID` 照合も id 比較に統一され、Store と Controller のドリフトが解消する。

修正後、必ず「同一 id セルへの **2 回連続** replaceCell が両方反映される」テストを追加すること（現行 `ApplyDiffTests.test_applyDiff_replaceCell` は 1 回のみで本バグを検出できない）。

---

### 🟠 Major: iOS UI 層テストがホストで実行されず、reconfigure / replaceCell 経路の実行カバレッジがゼロ

**該当箇所**: `ios/Tests/KsSettingsViewUITests/ApplyDiffTests.swift:9`（`#if canImport(UIKit)`）、verification-report.md:19

**問題点**:

`KsSettingsViewUITests`（`applyReplaceCell` の reconfigure 検証 `test_applyDiff_replaceCell` を含む）は `#if canImport(UIKit)` でガードされ、macOS ホストの `swift test` では **コンパイルのみ・実行されない**（本レビューで実機確認: フィルタ指定でも `Executed 0 tests`）。「123 tests PASS」には iOS UI 層の reconfigure / replaceCell / moveCell / removeCell の **実行検証が一切含まれない**。tasks.md 6.3「`ApplyDiffTests.test_applyDiff_replaceCell` を強化し reconfigure 経路を検証」は記述上は実装されているが、CI/ホストでは実行されないため Critical 指摘 #1 をすり抜けた。

**推奨修正**:

- iOS シミュレータ（`xcodebuild test -destination 'platform=iOS Simulator,...'`）での UI 層テスト実行を CI / 検証手順に組み込む。少なくとも本提案のマージ前に一度シミュレータで `KsSettingsViewUITests` を実行し、reconfigure 経路（特に連続 replaceCell）を実機相当で検証する。
- これは tasks.md 7 章（実機検証、未チェック）と重なるが、7 章は目視確認であり、ユニットテストのシミュレータ実行は別途必要。Critical #1 の修正検証はシミュレータ実行で担保すること。

---

### 🟡 Minor: `DSLDiffCalculator.swift` のコメントに過去レビュー由来の不整合な記述

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:150`（`// ## Cell の内容比較規約（review-result_001.md Major-4 対応）`）、161-170

**問題点**:

本提案の `review-result_001.md`（本ファイル）はまだ存在しなかったにもかかわらず、コードに「review-result_001.md Major-4 対応」という別文脈のレビュー参照が残っている。また同コメント群（規約4 `@Binding` / `MutableState` の `wrappedValue` を Hashable に含める等）は、本提案で Critical #1 を修正し `KsCellID` を id 限定にすると **内容比較の前提が変わる**ため、内容比較規約の説明が `KsCellID` の役割と矛盾しないよう更新が必要。

**推奨修正**: Critical #1 修正に合わせてコメントを整理し、「内容比較は `.replaceCell` 発行の判定に使うが、構造同期（KsCellID）には使わない」点を明示する。古いレビュー参照（review-result_001.md Major-4）は本提案の文脈に合わせて書き換えるか削除する。

---

### 🔵 Suggestion: Android `submitContentUpdate` の `notifyItemChanged` を payload 付きにできる余地

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsListAdapter.kt:64-74`

**問題点（軽微）**:

`submitContentUpdate` は `notifyItemChanged(position)`（payload なし）で部分更新する。これは spec を満たすが、`onBindViewHolder` 全体（背景・罫線含む）を再実行する。design.md Decision 3 は「`setBackgroundColor` 等の重い処理を避ける部分 bind」に言及している。現状でも `notifyItemChanged` はセルを破棄しないためちらつきは出ない見込みだが、より軽量にするなら payload 付き `notifyItemChanged(position, payload)` + `onBindViewHolder(holder, position, payloads)` でチェック状態のみ更新する余地がある。

**推奨修正（任意）**: 実機検証（tasks 7.1）で `notifyItemChanged`（payload なし）でもちらつき・性能に問題がなければ現状維持で可。問題があれば payload 経路を検討。本提案範囲では必須ではない。

---

## スペック / タスク整合の確認結果

- **core spec（二層分離・equals 契約・SettingsRootDiff.replaceCell 意味論）**: ドキュメント/KDoc/Swift doc は良く更新されており整合（`SettingsRootDiff.kt:37-62`、`Cell.kt`、`KsCell.swift`）。値型 equals は全フィールド比較を維持（core テスト PASS）。
- **Android spec（areContentsTheSame / getItemId / DSL→Diff / CellViewHolder / TwoWay / Radio 連動）**: すべて整合。`compute` は内容変化で ReplaceCell を発行せず、`contentUpdates` が列挙して `store.replaceCell → applyDiff(ReplaceCell) → submitContentUpdate(notifyItemChanged)` の部分更新へ流す。implementer の設計解釈は spec に合致。`DSLDiffCalculatorTest` / `ListAdapterDiffTest` の検証も適切で、Android 側は実行カバレッジあり。
- **iOS spec（reconfigureItems / id 限定構造同期 / replaceCell＝reconfigure）**: `reloadItems → reconfigureItems` の置換（`applyReplaceCell` / `applyUpdateTheme`、iOS 15 ガード付き）は spec どおり。しかし「構造同期は `KsCellID`（id）同一性のみ」要件は `KsCellID.contentHash` により **未達**（Critical #1）。
- **tasks.md**: 0〜6 章・8 章は実装されチェック済み。ただし 6.3（reconfigure 経路テスト）はホスト未実行（Major）。7 章（実機検証 4 件）は未チェックで残置（実機操作必要、妥当）。完了条件「内容変化でセルが再生成されない（両プラットフォーム）」は Critical #1 により iOS で未達。

## アクションプラン（優先度順）

1. **(Critical)** `ios/Sources/KsSettingsViewCore/KsCellID.swift`: `KsCellID` の `==` / `hash` を `id`（UUID）のみに限定し、`contentHash` を同一性判定から除外（推奨は `contentHash` フィールド自体を削除）。これで構造同期が id 限定となり、連続内容更新の破綻（DEBUG クラッシュ / Release 更新欠落）が解消する。
2. **(Critical)** 1 に合わせ `SettingsRootStore.replaceCell` / `moveCell` / `removeCell` の `KsCellID(cell:) == cellID` 照合が id 比較で一貫することを確認。`applyReplaceCell` の `cellIndex` 更新も id キーで安定することを確認。
3. **(Critical)** 「同一 id セルへの 2 回連続 replaceCell が両方反映される」テストを追加（現行 1 回のみのテストでは検出不能）。
4. **(Major)** iOS シミュレータで `KsSettingsViewUITests` を実行する手順を検証に追加し、reconfigure / replaceCell / moveCell / removeCell の実行カバレッジを確保。最低限マージ前に 1 回シミュレータ実行する。
5. **(Minor)** `DSLDiffCalculator.swift` の内容比較規約コメントと古いレビュー参照（review-result_001.md Major-4）を本提案の文脈・KsCellID 修正に合わせて更新。
6. **(Suggestion)** Android `submitContentUpdate` の payload 化は実機検証結果次第で検討（任意）。

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

理由: Critical 1 件（iOS `KsCellID` の内容ハッシュによる spec 違反 + 連続内容更新の破綻）および Major 1 件（iOS UI 層テストがホスト未実行で reconfigure 経路の実行カバレッジゼロ）。Android 側は spec / design に整合し問題なし。Critical #1 を修正し、id 限定の `KsCellID` で連続内容更新が両方反映されることをテスト（できればシミュレータ実行）で担保したうえで再レビューを推奨する。
