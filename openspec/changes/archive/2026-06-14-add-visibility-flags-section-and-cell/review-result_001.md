# レビュー結果 - add-visibility-flags-section-and-cell

**レビュー日時**: 2026年06月14日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-visibility-flags-section-and-cell

## サマリー

「オリジナル `AiForms.Maui.SettingsView` 移植漏れ対応シリーズ Change 3」として `Section.isVisible` / `Cell.isVisible` を Core + UI 層に導入する変更。Core ドメインモデル / UI 層 7 Cell / iOS・Android のホスト層 / SwiftUI・Compose DSL / Sample アプリの全層に変更が及ぶ大規模追加だが、レビューした範囲では **spec delta と実装が高い精度で一致** している。

**強み**:
- `VisibilityAware` プロトコル/interface を **opt-in 抽象** として UI 層に置く設計が Decision 2 と完全に一致し、Core 純化方針を破壊していない。
- 「visible projection の二重管理」「部分 Diff の index 規約（model 配列基準）」「hidden 対象 no-op」「`ReplaceCell` / `ReplaceSection` での visibility 切替 → Full フォールバック」のすべてが iOS / Android ホスト層に実装されており、spec の各 Scenario と対応関係がトレースできる。
- preflight 検出が iOS / Android 両 DSL Diff calculator の **冒頭** に配置されており、可視性差分が `replaceCell`（reconfigure）経路に漏れない。`contentUpdates` が空リストを返す Android 側の振る舞いも spec 通り。
- 既定値 `true` で互換維持されており、既存呼び出しを破壊しない。
- iOS 274 / Android 全モジュールのテストが成功。テストは spec の MUST/SHOULD/Scenario に良くマップしている。

**判定**: `APPROVED`

Critical/Major 指摘なし。Minor / Suggestion レベルの観察事項はあるが、いずれも本 change の archive を妨げる性質ではない。

---

## 検証結果サマリー

| 観点 | 結果 |
|------|------|
| iOS `swift test`（88 + UI tests） | PASS |
| Android `:ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test` | PASS（BUILD SUCCESSFUL） |
| openspec proposal/design/tasks/specs と実装の整合 | 整合 |
| 7 Cell の `isVisible` 追加 + `VisibilityAware` 準拠 + `Hashable`/`equals` 反映 + `withDSLID`/`copy()` 保持 | 全て確認 |
| Core 抽象 (`KsCell` / `Cell`) への isVisible 要求追加なし（純化方針維持） | OK |
| DSL preflight (`containsVisibilityChange`) → Full 発行 | iOS / Android 両方で確認 |
| `replaceCell` での visibility 切替 → Full フォールバック | iOS / Android 両方で確認 |
| `replaceSection` 常に Full 経路 | iOS / Android 両方で確認 |
| 部分 Diff `index` は model 配列基準で受け、UI 層で visible projection に変換 | iOS で確認 |
| hidden 対象の部分 Diff は no-op (UI 操作)、model は更新 | iOS / Android 両方で確認 |
| `SettingsRootStore.swift` 内の Section 再構築 5 箇所で `isVisible` 保持 | 確認 |
| layout mode 再評価ロジック（visible projection 基準） | 確認 |
| 7 Cell の Sample 互換 + サンプル新規追加（VisibilityDemoView / VisibilityDemoScreen） | 確認 |
| archive 済み change `refactor-display-state-sync` への文言改変なし | OK（diff に含まれない） |

---

## 指摘事項

### 🔵 Suggestion 1: ソースコメント中の「二層分離」表記の整合（live spec rename 反映時）

**該当箇所**:
- `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:24, 196`
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLDiffCalculator.kt:15-21, 176`
- 他、計 26 箇所のソースコメント / KDoc 内に旧呼称「表示状態同期の二層分離」が残存

**問題点**:
本 change で live spec の Requirement 名は `表示状態同期の三層分離` に rename されるが、ソースコードの doc コメント中の「二層分離」表記は更新されていない。実装ロジック自体は仕様に沿っている（preflight で可視性変化を `Full` に倒す）ため動作上の問題はないが、archive 後に live spec を参照する読み手が混乱する可能性がある。

**推奨修正**:
本 change の archive 完了時、または近い時期の cleanup change で、ソース doc コメントの「二層分離」を「三層分離」（または「二層 + 可視性の三層分離」など本実装に即した呼称）に置換することを推奨。**本 change の archive 妨げにはならない**（実装は仕様準拠）が、フォローアップタスクとして検討の価値あり。

---

### 🔵 Suggestion 2: iOS `applyMoveCell` 内の `let _ = s` 似の冗長コード対応

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:478`

ホスト全体には影響しないが、`_ = s` のようなダミー代入（Footer 再構成内）が複数箇所散見される。本 change が直接導入したものではない（既存コードの継承）ため指摘のみ。

**推奨修正**: 別 change で cleanup する余地あり。本 change での対応不要。

---

### 🔵 Suggestion 3: `applyInsertCell` の visible projection 上挿入位置算出の計算量

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:1303-1307`

```swift
let visibleInsertIdx = cells.prefix(cellsClamped).filter {
    ($0 as? VisibilityAware)?.isVisible ?? true
}.count
```

**問題点**: 1 Section 内の Cell 数が大きい場合、O(N) で visible 数を数え直すが、`applyInsertCell` は通常 1 回の呼出につき 1 Cell の追加なので影響は無視できる。spec 上の要件は満たしている。

**推奨修正**: 不要（数百 Cell までは無視できる範囲、spec の Risks にも記載されている）。将来「数千 Cell の頻繁な挿入」がユースケースになったら index キャッシュ化を検討する余地あり。

---

### 🔵 Suggestion 4: Sample アプリの目視確認 (tasks.md 12.3) はユーザー実施

**該当箇所**: `openspec/changes/add-visibility-flags-section-and-cell/tasks.md:97`

```
- [ ] 12.3 iOS / Android sample アプリで「条件付き非表示」サンプルが期待通りに動作することを目視確認
```

ビルド成功・テスト成功は確認済みだが、実機/シミュレータでの目視確認のみ未チェックの状態。tasks.md 上に「（実機/シミュレータでの目視確認はユーザー実施対象。ビルドは両プラットフォームで成功確認済み）」と記載があり、自動レビューでは検証不能。

**推奨修正**: archive 前にユーザーが iOS / Android Sample を 1 回起動して visibility toggle が動作することを目視確認することを推奨。アクションプランに含める。

---

## アクションプラン（優先度順）

1. **（任意・ユーザー操作）** Sample アプリで `VisibilityDemoView` / `VisibilityDemoScreen` の目視確認を行い、tasks.md 12.3 をチェック。
2. **（任意・フォローアップ）** ソースコメント中の「二層分離」表記を「三層分離」に統一する cleanup change を将来検討（必須ではない）。
3. 上記が満たされれば本 change を archive 可能。

---

## レビュー観点チェックリスト結果

### 正確性・機能性
- [x] openspecの仕様・要件(proposal/design/spec) を正しく満たしている
- [x] openspecの仕様・要件・タスクを勝手に書き換えていない
- [x] tasks.md の全完了項目を実装と照合し未実装なしを確認（12.3 のみユーザー目視待ち、これは仕様上「ユーザー実施対象」と明記）
- [x] エッジケース考慮（全 hidden / hidden 連続 / hidden 配下 partial Diff / visibility toggle 同時に内容変化）はテスト済み
- [x] エラーハンドリング（`reportMissingID` / `Log.w`）はオリジナル既存挙動を踏襲

### テスト容易性
- [x] 注入されていない時刻ソース不使用
- [x] DI 経路（`SettingsRootStore` 経由・テスト用 internal init）は既存設計を維持
- [x] `computeVisibleSections(from:)` が `internal static` で純粋関数化されており単体テスト可能
- [x] `containsVisibilityChange` も internal で個別テスト可能

### セキュリティ
- [x] 入力値バリデーション該当なし（UI フラグ）
- [x] 機密情報なし

### パフォーマンス
- [x] 妥当な計算量（spec の Risks に記載済み、Suggestion 3 参照）
- [x] リソース解放（既存 `deinit` / `onDetachedFromWindow` 経路を破壊していない）

### 可読性・保守性
- [x] 命名一貫（`VisibilityAware` / `isVisible` / `visibleSections` / `computeVisibleSections`）
- [x] doc コメントが Requirement / Decision を参照する形で充実
- [x] 抽象化レベル妥当（opt-in protocol で 7 型網羅 switch 不要）

### 一貫性
- [x] 既存パターン（`isEnabled` の追加経路）と対称的に実装
- [x] iOS / Android 両プラットフォームで設計が対称（preflight / Full フォールバック / flatten フィルタ）

### 多言語対応
- 非該当（UI 文字列追加なし、Sample 内のラベルのみ）

### テスト
- [x] 全テスト成功（iOS swift test PASS / Android gradle test PASS）
- [x] spec 各 Scenario に対応するテストが存在
  - Section/Cell isVisible 既定値・等価性・既存呼び出し互換: `SectionVisibilityTests` / `CellVisibilityTests` / `CellVisibilityTest`（Android）
  - visible projection からの除外: `VisibilityProjectionTests` / `FlattenVisibilityTest`
  - hidden 対象 part Diff の no-op: `VisibilityProjectionTests.test_hidden_Cell_への_removeCell` / `VisibilityApplyDiffTest`
  - `replaceCell` visibility 切替の Full フォールバック: `VisibilityProjectionTests.test_isVisible_true_to_false_は構造同期上の削除` / `VisibilityApplyDiffTest`
  - `replaceSection` 常に Full: `VisibilityProjectionTests.test_replaceSection_は_常に_Full_経路で処理される` / `VisibilityApplyDiffTest`
  - DSL preflight: `DSLVisibilityPreflightTests` / `DSLVisibilityPreflightTest`
  - 全 hidden でクラッシュなし: `VisibilityProjectionTests.test_全Section全Cell_hidden_でも_空表示_クラッシュなし`
  - `insertCell` の index = model 配列基準: `VisibilityProjectionTests.test_insertCell_index_は_model_配列基準_hidden_を跨いで挿入`
- [x] 手抜き実装なし（skip / コメント外し回避なし）
- [x] スタブは未使用、本物の Controller / View を使ったテスト
- [x] 境界値（hidden を挟む `insertCell`、hidden Section 先頭、全 hidden）も網羅

---

## 判定結果

**ステータス**: ✅ **APPROVED**

- Critical 指摘なし
- Major 指摘なし
- Minor 指摘なし（あるのは Suggestion レベルのみ）
- Suggestion はいずれも本 change の archive を妨げない（ユーザー目視確認 1 件のみ残）

仕様（proposal.md / design.md / tasks.md / specs/ 配下 6 件）と実装の整合性は高く、テストも豊富で MUST/SHOULD/Scenario に良く対応している。両プラットフォームのテストが全件成功している。Core 純化方針 / 既存「表示状態同期」原則 / 既存呼び出し互換性のいずれも破壊していない。

ユーザーは Sample アプリ目視確認を 1 回行ったうえで `openspec apply --archive add-visibility-flags-section-and-cell` 相当の archive を進めてよい。
