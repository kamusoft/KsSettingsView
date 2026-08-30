# レビュー結果 - add-partial-update-core

**レビュー日時**: 2026年05月13日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-partial-update-core

## サマリー

本変更提案は、`KsSettingsView` の Core 層（iOS / Android）に部分更新を可能にするための共通基盤型（`SettingsRootDiff` / `AccessoryTarget` / `SettingsAccessory`）を追加し、`SettingsRoot` から `header` / `footer` を削除（破壊的変更）するものである。

### 検証結果

| 観点 | 結果 |
|------|------|
| `swift test`（iOS Core 含む全モジュール） | 78 件すべて成功 |
| `./gradlew :ks-settingsview-core:test` | failures=0 で成功（Debug / Release ともに） |
| `swift build` フルビルド | 成功 |
| `./gradlew build` フルビルド | BUILD SUCCESSFUL（lint / check 含む） |
| spec.md の全 Scenario に対応するテストの存在 | 全シナリオ対応確認済 |
| tasks.md チェックボックス | 全項目チェック済（11 章 / 全 56 タスク完了） |

### 全体評価

- 仕様書（proposal.md / design.md / specs/settings-view-core/spec.md）と実装が高い精度で整合している。
- iOS / Android の型構成（11 ケース × Diff、4 ケース × AccessoryTarget、2 ケース × SettingsAccessory）が完全に一致。
- 破壊的変更（`SettingsRoot.header/footer` 削除）に対する UI 層（SwiftUI / Compose / iOS Controller / Android RecyclerView）の整合化が、後続提案 `add-partial-update-native` を見据えた「常に null / 空」運用として一貫的に処理されており、参照漏れがない。
- `KsCellID` の Core 層への移動は spec/design に明記されていないが、`SettingsRootDiff` が `KsCellID` を Core 層で参照する必要があるための合理的な対応であり、コメントで意図が明示されている。
- 既存テスト（`SettingsRootTests` の Root H/F 関連 Scenario）が適切に削除/書き換えされており、削除した Scenario と新規 Scenario が spec.md のコメントで対応関係を保っている。

**判定**: `APPROVED`

## 指摘事項

### 🔵 Suggestion: `SettingsRootDiff.replaceCell` の id 不一致時の挙動が暗黙的

**該当箇所**: `ios/Sources/KsSettingsViewCore/SettingsRootDiff.swift:35`、`android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRootDiff.kt:38`

**問題点**:
`replaceCell(cellID, new: Cell)` で、`cellID` と `new.id` が一致しない場合の挙動（identity 保証）は design.md の Open Questions に「Native UI 層の責務とする」と明記されているが、ドメインコメント側には明示が無い。Core 型としては Diff 表現の整合性を担保する立場として、コメントレベルでも「identity 一致は呼び出し側の責務」を明文化しておくと利用者にとって安全。

**推奨修正**:
`SettingsRootDiff.swift` / `SettingsRootDiff.kt` の `replaceCell` / `ReplaceCell` ケースの DocComment に以下趣旨を追加：

> `cellID.id` と `new.id`（または `cellId` と `newCell.id`）が一致することを呼び出し側で保証すること。不一致時の挙動は Native UI 層（applyDiff 実装）の責務である（design.md Open Questions 参照）。

任意の改善であり、本提案の archive 阻害要因ではない。

---

### 🔵 Suggestion: `KsCellID.contentHash` のプロセス間非永続性に関する設計コメントの強化

**該当箇所**: `ios/Sources/KsSettingsViewCore/KsCellID.swift:25-29`

**問題点**:
`KsCellID.swift` には既に `contentHash` の「per-process ランダム化」と「偶発衝突」について丁寧なコメントがある。一方、Android 側 `Cell.id: String` のみで `contentHash` 相当の中身ハッシュは保持しない非対称性（Open Questions に記載）について、設計が iOS と Android で意図的に異なることがコード側からは読み取りにくい。後続 `add-partial-update-native` で Diff 適用時の identity 取り扱いを実装する際の混乱を避けるためにも、ファイル先頭コメントで「Android 側は `Cell.id: String` のみで識別」する旨を 1 行追加しておくと親切。

**推奨修正**:
`KsCellID.swift` 冒頭コメント末尾に下記の趣旨を追記：

> Android 側は `Cell.id: String` のみを Diff identity に使用しており、`contentHash` 相当のフィールドは保持しない（design.md Open Questions: "Cell ID の表現"）。Cell の内容変更検出は Android 側では `Cell` 自体の `equals` / `hashCode` に依存する。

任意の改善であり、機能上の問題ではない。

---

### 🔵 Suggestion: `Section.swift` のドキュメンテーション内仕様参照が古い変更提案を指す

**該当箇所**: `ios/Sources/KsSettingsViewCore/Section.swift:8`

**問題点**:
`Section.swift` のファイル先頭コメントが `openspec/changes/refactor-accessory-and-root-hf/specs/...` を参照しているが、当該変更提案は既に archive 済み（`openspec/changes/archive/2026-05-09-refactor-accessory-and-root-hf/`）であり、現行の有効な spec は `openspec/specs/settings-view-core/spec.md`。本変更提案の対象ファイルではないので必須変更ではないが、参照を archive 直前の状態として最新化しておくと保守上有利。

**推奨修正（任意）**:
本提案の対象外であるため、archive 後に別途整理対象とすればよい。CHANGES_REQUESTED とはしない。

---

### 🟡 Minor: `tasks.md` 完了条件 「`./gradlew :ks-settingsview-core:test` が成功する」 の検証時の留意点

**該当箇所**: `openspec/changes/add-partial-update-core/tasks.md:10`

**問題点**:
レビュー時に `./gradlew :ks-settingsview-core:test` を実行したところ、初回は全タスク UP-TO-DATE で表示され、テストが実際には再実行されないことに留意。`--rerun-tasks` を付けて再実行したところ failures=0 で成功した。完了条件としては問題なし（test-results XML は実際にテストが実行されていることを示している）が、CI などで完了確認する場合は `--rerun-tasks` あるいは clean → test の運用を推奨。これは指摘というより運用ノートだが、後続提案アーカイブ時の混乱を避けるために記録する。

**推奨修正**:
本提案の `tasks.md` への記載は不要。後続提案または CI 整備時の検討事項とする。

---

## アクションプラン

### 必須対応（Critical / Major）

**なし**

### 推奨対応（Minor / Suggestion）

1. 🔵 `SettingsRootDiff.replaceCell` / `ReplaceCell` ケースに「cellID と new.id の一致は呼び出し側責務」コメント追加（任意）
2. 🔵 `KsCellID.swift` 冒頭コメントに iOS / Android の Diff identity 取り扱い非対称性を追記（任意）
3. 🔵 `Section.swift` のドキュメンテーション参照を最新 spec に更新（本提案対象外、別途）

いずれも archive 阻害要因ではなく、本提案単体としては承認可能。

## 判定結果

**ステータス**: `APPROVED`

### 判定根拠

- ✅ `swift test` 78 件、`./gradlew :ks-settingsview-core:test` failures=0 で全テスト成功
- ✅ フルビルド（`swift build`, `./gradlew build`）が成功
- ✅ proposal.md / design.md / spec.md の Requirement / Scenario と実装テストが完全に対応
- ✅ tasks.md の全 56 タスクが完了済み
- ✅ iOS / Android の型構成（11 ケース Diff、4 ケース AccessoryTarget、2 ケース SettingsAccessory）が一致
- ✅ 破壊的変更（`SettingsRoot.header/footer` 削除）について、UI 層（SwiftUI / Compose / iOS Controller / Android RecyclerView）が「常に null / 空」運用で整合化されており、参照漏れがない
- ✅ `KsCellID` の Core 層移動が `SettingsRootDiff` の依存関係から論理的に必要であり、コメントで意図が明示されている
- ✅ Core 設計（sealed enum / sealed interface、Hashable / equals 契約、`any KsCell` 手動 Hashable）がモダンな Swift / Kotlin イディオムに準拠
- ✅ design.md の Decision 1〜7 がすべて実装に反映されており、`SettingsAccessory` の独立性（Decision 4）も spec で明文化

Critical / Major 指摘は存在しない。Suggestion レベルの 3 件はいずれも改善余地としての提案であり、本提案単体のマージを妨げない。

design.md / proposal.md の Migration Plan に従い、本提案単体での archive ではなく `add-partial-update-native` と同時に archive することが強く推奨される（既存サンプルコードの破壊回避のため）が、その判断は OpenSpec 運用上の問題でありレビュー判定とは独立である。
