# レビュー結果 - add-declarative-dsl (3 回目)

**レビュー日時**: 2026年05月17日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-declarative-dsl
**前回レビュー**: `review-result_002.md`（CHANGES_REQUESTED）

## サマリー

### 前回 Major-A 指摘への対応状況

| ID | 内容 | 対応 |
|----|------|------|
| Major-A | Compose `Cell.cellID(id)` が DSL Node 経路に乗らず明示 ID が無視される | **解消**。spec.md:256-265 / 387-390 が要求する **メソッドチェーン形式**（`LabelCell(...).cellID("dynamic-cell-1")`）を尊重して **案 B（仕様準拠の ID 経路実装）** を採用。`DSLExplicitIdCell` という sentinel `data class` を新設し、`Cell.cellID(id)` がそれでラップ、`DSLSectionScope.cell()` が `is DSLExplicitIdCell` 分岐で unwrap して `DSLCellNode.identityHint = DSLIdentityHint.Explicit(id)` に転写する経路に変更されている。 |
| Minor-A | Sample / docs の `Cell.cellID(...)` 利用記述の誤誘導 | **解消**。`Cell.cellID(...)` が正規 API として動作するため、Sample (`MainActivity.kt:144`) / `docs/declarative-dsl-guide.md:152` の記述はそのまま正しい挙動を示すようになった。 |
| Minor-B | `DSLDiffCalculator.swift` の `_ = newByID` dead store | **解消**。`grep` で残存なしを確認。 |
| Minor-C | `DSLBackedRepresentableView.init` の builder 早期評価 | **解消**。`StateObject(wrappedValue:)` の autoclosure 内で `Self.makeInitialBookkeeper(builder:rootHeader:rootFooter:theme:)` を呼ぶ形に整理され、`init` 再評価で builder() が毎回呼ばれることはなくなった。`makeInitialBookkeeper` 自体は static func で `initialNodes = builder()` を 1 度だけ評価する。 |
| Suggestion-B | iOS / Compose の `.cellID` API 形不揃いをドキュメント明文化 | **解消**。`docs/declarative-dsl-guide.md:165-172` に「メソッドチェーン形式」「スコープ関数形式」両方の使い方と等価性、クロスプラットフォーム開発時の推奨が明記された。 |

### 全体評価

`DSLExplicitIdCell` sentinel ラッパは **侵襲最小**（既存 `Cell` インターフェース・既存具象 Cell に変更不要）かつ **spec 準拠**（`Cell.cellID(...)` メソッドチェーン形式を正規 API として実装）の優れた解法である。さらに `DSLReidentifiableCell` と `DSLStyleModifiableCell` の両規約を transparent に委譲する実装になっており、

```kotlin
LabelCell(...).cellID("x").font(...)        // ラップ後に style modifier
LabelCell(...).font(...).cellID("x")        // style modifier の後にラップ
LabelCell(...).cellID("x").cellID("y")      // 上書き（最新の "y" が採用される）
```

のいずれのチェーン順序でも明示 ID ヒントが保持される設計になっている（`DSLNodes.kt:147-170`、`CellModifiers.kt:78-81`）。これは spec の `Cell modifier の連鎖適用` Scenario（spec.md:244-254）と `明示 cellID による Cell 同一性指定` Scenario（spec.md:256-265）の両方を整合的に満たす。

regression テスト `cellID 明示指定で位置移動を跨いでも Cell ID が安定する`（`DSLIntegrationTest.kt:269-300`）も意図通り：
- 1回目: `[cell-x]`（cell-x が index=0）
- 2回目: `[cell-y, cell-x]`（cell-x が index=1 に位置移動）
- `first[0].cells[0].id == second[0].cells[1].id` を assert

旧実装（Positional フォールバックが採用される実装）では `cellType + indexInSection` がハッシュに含まれるため index=0 と index=1 で別 ID になり **必ず失敗** する。新実装では `identityHint = Explicit("cell-x")` が最優先採用されるため必ず pass する。バグの存在と修正の有効性を **直接観測** できる良いテストである。

ビルド・テストはすべて pass：
- iOS: 117 tests, 0 failures
- Android Compose: 全タスク UP-TO-DATE / BUILD SUCCESSFUL（テスト追加分も含めて pass 済み）

### 残存する未解消の懸念

- **[Minor-D]**（前回継続）: 実機目視（tasks.md 22.2-22.6）が依然未消化。本提案範囲では受容できるが、archive 前には必ず行う必要がある（コード品質的にはマージ可能）。
- **[Suggestion-A]**（前回継続）: iOS の `DSLHintRegistry.shared` シングルトン。コメントで「インスタンス ID（UUID）衝突しない限り問題なし」と明記されており実害は無視できるレベル。将来 TabView / SplitView 等の並列描画ユースケースが本格化したら見直し対象。

### 判定

**APPROVED**。前回最大の懸念だった Major-A は spec 準拠の経路（案 B）で **構造的に解消** され、regression テストも適切に追加された。Minor-B / Minor-C / Minor-A / Suggestion-B もすべて適切に対応されている。残る Minor-D（実機目視）と Suggestion-A（シングルトン）は本提案の archive 前に必ず確認する必要があるが、コード品質的にはマージ可能な水準である。

## 指摘事項

### 🟡 Minor

#### [Minor-D] 実機目視検証（tasks.md 22.2-22.6）が引き続き未消化（前回継続）

**該当箇所**: `openspec/changes/add-declarative-dsl/tasks.md:212-216`

**問題点**:

- 22.2 両 Sample アプリの DSL 方式画面の起動確認
- 22.3 動的追加・削除での Native UI 部分更新アニメーション確認
- 22.4 Cell modifier 連鎖適用の視覚確認
- 22.5 Root H/F の任意 View 描画確認
- 22.6 Section H/F の任意 View 描画確認

Major-A 修正後、`Cell.cellID(...)` の挙動も含めた部分更新の振る舞いを実機で目視する必要がある。コード上の挙動は Integration テストで確認済みだが、Cell ID 安定性は Native 側の部分更新アニメーション（`UICollectionViewDiffableDataSource` / `RecyclerView.DiffUtil`）の挙動に直接影響するため、実機確認のメリットは大きい。

**推奨修正**: archive 前に必ず 22.2-22.6 を実施し、tasks.md にチェックを入れる。本指摘自体はマージ阻害要因ではない（コードレビュー観点では問題なし）。

### 🔵 Suggestion

#### [Suggestion-A] `DSLHintRegistry` プロセスローカル シングルトン（前回継続）

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift:31-34`

**問題点**:

引き続きシングルトン。コメント `// プロセスローカルなため、複数 KsSettingsView が同時に評価されても インスタンス ID（UUID）が衝突しない限り正しく動作する` の通り、実害は無視できる確率（UUID 衝突）。`evaluateAndApplyDiff` 冒頭で `reset()` を呼ぶ設計のため、同一 RunLoop ターン内に並列描画される複数 `KsSettingsView` 同士でヒントが「巻き戻る」リスクが理論上はある。

**推奨修正**: 将来 TabView / SplitView 等で同一画面内に複数 `KsSettingsView { ... }` が並列に存在するユースケースが顕在化した時点で：

- 各 `DSLBookkeeper` インスタンスが非シングルトンの `HintRegistry` を持つ、または
- ThreadLocal / TaskLocal で `body` 評価スコープに閉じる

形に移行する。本提案では受容可能。

#### [Suggestion-C] Compose Integration テストは `TestCell` のみで検証している

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/DSLIntegrationTest.kt:336-346`

**問題点**:

`DSLIntegrationTest.kt` の全テストは `TestCell`（`data class` で `DSLReidentifiableCell` / `DSLStyleModifiableCell` を実装）でのみ検証されている。実 Cell（`add-cell-types-basic` で追加される `LabelCell` 等）が両規約を実装し損ねた場合、本提案の DSL は silently no-op する（`rebindCellId` も `mutateStyle` も「規約未実装なら自身を返す」設計のため、検出できずに位置依存 ID が漏れる）。

**推奨修正**: 後続 `add-cell-types-basic` の archive 時に、具象 `LabelCell` / `SwitchCell` 等が両規約を実装していることを CI レベルで保証する（regression テスト or `interface` 強制）。本提案では DSL 側の責務ではないため Suggestion レベルだが、`add-cell-types-*` 提案レビュー時の checklist に含めるとよい。

### ✅ 解消した前回指摘（参考）

- **Major-A**: `DSLExplicitIdCell` sentinel パターンで spec 準拠の経路を実装。**案 B 採用は spec.md:256-265 と spec.md:387-390 と整合**（spec が `LabelCell("動的Cell").cellID("dynamic-cell-1")` というメソッドチェーン形式を明示要求しているため、案 A の `Cell.cellID()` 廃止は spec 違反になっていた）。
- **Minor-A**: Major-A の修正に伴い Sample / docs の記述もそのまま正しい挙動を示すようになった。
- **Minor-B**: `_ = newByID` 削除確認済。
- **Minor-C**: `StateObject(wrappedValue:)` autoclosure + `static func makeInitialBookkeeper` で `builder()` 初回限定評価に整理。
- **Suggestion-B**: `docs/declarative-dsl-guide.md:165-172` で両形式の使い分けを明文化。

## アクションプラン

優先度順：

1. **[Minor-D]** archive 前に実機目視（tasks.md 22.2-22.6）を実施する。
2. **[Suggestion-C]** 後続 `add-cell-types-basic` レビュー時に具象 Cell が `DSLReidentifiableCell` / `DSLStyleModifiableCell` を実装していることを CI レベルで保証する。
3. **[Suggestion-A]** TabView / SplitView 等の並列描画ユースケースが本格化したら `DSLHintRegistry` シングルトンを見直す。

## 判定結果

**ステータス**: `APPROVED`

理由：

- 前回 Major-A は spec 準拠（案 B）の経路で **構造的に解消** された。
- `DSLExplicitIdCell` sentinel パターンは spec.md:256-265 の `LabelCell("動的Cell").cellID("dynamic-cell-1")` というメソッドチェーン形式の Scenario と spec.md:387-390 の Cell ID 判定優先順位 2「`Cell.cellID(id: Any)` 拡張関数で明示指定されている場合：その値を採用」の両方を満たす。
- regression テスト `cellID 明示指定で位置移動を跨いでも Cell ID が安定する` は旧実装で必ず失敗、新実装で必ず pass するため、バグの不在と修正の有効性を直接観測できる優れたテスト。
- Minor-B / Minor-C / Minor-A / Suggestion-B もすべて適切に対応された。
- iOS 117 tests / Android 全テスト pass、両 Sample ビルド成功。
- 残る Minor-D（実機目視）と Suggestion-A（シングルトン）は archive 前のチェックポイントとして残すが、コードレビュー観点ではマージ可能。
- 新たに観測した Suggestion-C は後続提案（`add-cell-types-*`）側のレビュー観点であり、本提案の DSL コードに直接の問題なし。

Compose API 二重化バグが解消され、iOS / Compose 双方の API が spec と整合した状態になった。実装品質は高く、archive 直前まで進める準備が整っている。
