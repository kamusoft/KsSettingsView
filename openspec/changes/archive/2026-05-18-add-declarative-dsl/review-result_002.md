# レビュー結果 - add-declarative-dsl (2 回目)

**レビュー日時**: 2026年05月17日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-declarative-dsl
**前回レビュー**: `review-result_001.md`（CHANGES_REQUESTED）

## サマリー

### 前回 Critical 指摘への対応状況

| ID | 内容 | 対応 |
|----|------|------|
| Critical-1 | iOS DSL 安定 ID パイプライン未接続 | **解消**。新規 `@KsSettingsViewBuilder` を `[DSLSectionNode]` Component 型に変更し、`Section` 値を `KsSettingsViewBuilder.promoteToNode(_:)` で `DSLSectionNode` に自動昇格。`DSLBackedRepresentable.evaluateAndApplyDiff()` で `DSLRootTree.resolvedSections()` を経由する経路が組まれた。 |
| Critical-2 | DSL Integration テスト欠落 | **解消**。`ios/Tests/.../KsSettingsViewDSLIntegrationTests.swift`（8 ケース）と `android/.../DSLIntegrationTest.kt`（7 ケース）が追加。静的構造の空 Diff / Cell 内容変更で `replaceCell` のみ / `ForEach`/`forEach` append で既存 ID 不変・新規のみ `insertCell` / 明示 ID 指定での安定化 / Root H/F 変更で `updateAccessory` / Cell modifier 適用後の Cell ID 不変、を網羅。 |
| Critical-3 | SwiftUI `body` 内副作用違反 | **解消**。`DSLBackedRepresentableView`（SwiftUI View 層、`@StateObject` 保持）と `DSLBackedRepresentable`（`UIViewControllerRepresentable`）の 2 層に分割。Diff 適用は `updateUIViewController(_:context:)` 内に移動し、`body` は副作用ゼロで Representable を返すだけとなった。 |

Major-1〜Major-4、Minor の大半も適切に対応されている（後述）。テスト総数は iOS 117 件 / Android Compose モジュールすべて pass。

### 全体評価

iOS については前回 Critical 指摘が **構造的に解消** され、安定 ID パイプラインが意図通り動作することを Integration テストで検証できる粒度になった。前回最大の致命傷だった「DSL 評価のたびに全 Section / 全 Cell が remove → insert で流れる」挙動は、iOS Integration テスト `test_静的DSL_2回評価でDiffが空になる` で「同一 DSL の再評価で Diff が空」になることが直接検証されており、安定 ID が成立している証拠となる。

Android も DSL ノード経路（`DSLSettingsRootScope.build() → DSLRootTree.resolvedSections()`）が正しく組まれ、Integration テストも iOS と対応するケースで揃えられている。

ただし、**新規に検出した Compose 固有のバグが 1 件存在する**（後述 Major-A）。これは Sample で利用されているパスであり、仕様 `Cell ID 判定の優先順位 2`（`.cellID(_:)` 明示指定 → それを採用）と明確に乖離しているため、**マージ前に必ず修正すべき** と判断する。

その他、実機目視（22.2-22.6）が未実施なのは前回からの継続課題で、本提案範囲では受容できる範囲だが、修正後に必ず行う必要がある。

### 判定

**CHANGES_REQUESTED**：前回 Critical はすべて解消されたが、新規に検出した Compose の `Cell.cellID()` modifier 不整合（Major-A）が仕様違反かつ Sample で利用されている経路であるため、修正が必要。

## 指摘事項

### 🟠 Major

#### [Major-A] Compose `Cell.cellID(id: Any)` 拡張関数の実装が DSL Node 経路に乗らず、明示 ID が **実質的に無視される**

**該当箇所**:
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/CellModifiers.kt:66-73`
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLScope.kt:120-122`（`cell(cell)` の DSLCellNode 構築）
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/DSLNodes.kt:31-41`（`DSLCellNode.resolvedId(...)` のフォールバック計算）
- `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:143-144`（このバグの上でユーザに「明示 ID 推奨」と案内しているサンプル）

**問題点**:

`Cell.cellID(id)` 拡張関数（`CellModifiers.kt`）は以下の実装：

```kotlin
fun Cell.cellID(id: Any): Cell {
    return if (this is DSLReidentifiableCell) {
        withDSLId(stableIdString(id))
    } else {
        this
    }
}
```

これは **Cell.id を書き換えるだけ** で、後段の `DSLCellNode` 構築（`DSLSectionScope.cell(cell)` で `DSLCellNode(cell = cell)` と作られる）に `identityHint` を渡さない。

その後 `DSLRootTree.resolvedSections()` が `DSLCellNode.resolvedId(sectionId, indexInSection)` を呼ぶと、`identityHint == null` のため `DSLIdentityHint.Positional(...)` フォールバックが採用され、`rebindCellId(cellNode.cell, resolvedCellId)` で **Cell.id がさらに Positional ハッシュ値に書き換えられる**。

結果として：
1. 利用者が `.cellID("static-a")` と書いても、最終的な `Cell.id` は `"static-a"` ではなく Positional ハッシュ値（`stableHash` 32 桁 hex）になる
2. **位置移動した時に同一性が破壊される**（同じ `.cellID("static-a")` を指定しても、Section 内位置や型が変われば別 ID に解決される）
3. これは仕様 `settings-view-android-ui/spec.md:387-390` の Cell ID 判定優先順位 2 「`Cell.cellID(id: Any)` 拡張関数で明示指定されている場合：その値を採用」に **違反** する
4. 仕様 Scenario「明示 cellID による Cell 同一性指定」（spec.md:256-265）の THEN 句「Cell ID が `"dynamic-cell-1"` として固定され、Section 内位置や Cell 型に依存しない安定 ID となる」も成立しない

正規の経路として `DSLSectionScope.cellID(id: Any)`（`SectionModifiers.kt:29`）が存在し、こちらは `overrideLastCellId(DSLIdentityHint.Explicit(id))` で正しく Node にヒントを設定する。**つまり API が 2 系統あり、片方は正しく機能し、もう片方は機能しない**。利用者にとっては紛らわしく、Sample でも誤った方が使われている。

Compose Integration テスト（`DSLIntegrationTest.kt`）にも `Cell.cellID()` を「位置移動を跨いで」検証するテストが含まれていないため、CI でも検出できていない。

**推奨修正**:

以下のいずれか：

**案 A（推奨）**: `Cell.cellID(id)` を廃止し、`DSLSectionScope.cellID(id)` 一本化
- `CellModifiers.kt` の `Cell.cellID(id: Any)` 拡張関数を削除（または `@Deprecated`）
- Sample の `cell(...).cellID("static-a")` を `cell(...); cellID("static-a")` 形式に書き換え
- ドキュメント（`docs/declarative-dsl-guide.md`）も同じく更新

**案 B**: `Cell.cellID()` を Node 経路に乗せる
- `Cell.cellID(id)` で Cell に sentinel フィールド（`val pendingExplicitId: Any?` 等）を載せ、`DSLSectionScope.cell(cell)` でその sentinel を `identityHint` に転写してから `DSLCellNode` を構築する
- ただし `Cell` インターフェース変更が必要で侵襲が大きい

**案 C**: `Cell.cellID()` を「インスタンスフィールド + 専用スコープレジストリ」で繋ぐ
- DSLHintRegistry 風のサイドチャンネルを Compose 側にも持たせる（ただし iOS では UUID 自動採番なのに対し Compose は String の Cell.id で書き換え可能なため、現状の API 設計を尊重するなら案 A が最もシンプル）

**いずれの案でも**、`Compose DSLIntegrationTest.kt` に以下のケースを追加すること：

```kotlin
@Test
fun `cellID 明示指定で位置移動を跨いでも Cell ID が安定する`() {
    val first = evaluate {
        Section(header = "S") {
            cell(TestCell(id="noop", title="X")); cellID("cell-x")
        }
    }
    val second = evaluate {
        Section(header = "S") {
            cell(TestCell(id="noop", title="Y")); cellID("cell-y")
            cell(TestCell(id="noop", title="X")); cellID("cell-x")
        }
    }
    assertEquals(first[0].cells[0].id, second[0].cells[1].id)
}
```

（このテストは現状の `Cell.cellID()` パスが採用された場合に必ず失敗する。案 A で `DSLSectionScope.cellID()` を直接呼ぶ形に揃えた上で pass させる。）

### 🟡 Minor

#### [Minor-A] Compose `Cell.cellID()` を残す場合の Sample/ドキュメントの誤誘導

**該当箇所**: `samples/android/.../MainActivity.kt:143-144`、`docs/declarative-dsl-guide.md`（該当箇所未確認だが同様の記述がある可能性）

**問題点**:

Sample コメントには「`.cellID(...)` modifier または `forEach` の `key` lambda を使うのが本来の API 規約」と書かれているが、Major-A の通り `Cell.cellID()` は機能しない。コメントを信じて書いた利用者は「明示 ID を指定したつもりが、実は位置依存の動的 ID になっている」状態になる。

**推奨修正**: Major-A の修正に合わせてサンプル / ドキュメントも書き換える。

#### [Minor-B] `DSLDiffCalculator.swift` の `_ = newByID` が残存（前回 Minor-1 の積み残し）

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:80`

**問題点**:

前回 Minor-1 で「dead store の削除」を推奨したが、`_ = newByID` は依然残っている。コメントで「今後拡張時の対称参照用に残す」と説明があるため致命的ではないが、未使用変数のままにする意義は弱い（実装時に再生成すればよい）。

**推奨修正**:

- 削除する。将来必要になった時点で再追加すればよい
- もしくは `newByID` を実際に使う形（例えば「旧にあって新にない Section の Cell.removeCell を確実に発行する」等）にリファクタリングする

#### [Minor-C] iOS 側 `DSLBackedRepresentableView.init` で「初期 root を一度だけ評価」とコメントしつつ `builder()` を 2 回呼んでいる可能性

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:269-289`

**問題点**:

`init` 内で `builder()` を呼んで初期 root を構築している。SwiftUI ベストプラクティス的には View の `init` は何度も呼ばれる前提だが、`@StateObject` の `wrappedValue: ` は autoclosure で「初回のみ評価」される設計のはず。しかし現状の実装は `let initialNodes = builder()` を `init` の直接スコープで呼んでおり、View の再生成（親 View の再評価による `init` 再実行）ごとに毎回呼ばれる。

ただし `_bookkeeper = StateObject(wrappedValue: ...)` の引数は autoclosure ではなく eager 評価されているため、すでに評価した `DSLBookkeeper` インスタンスは StateObject のラッパ初期化条件に従って「初回のみ」採用されるが、不要な `builder()` 呼び出しのコストは発生する。

**推奨修正**:

- StateObject の autoclosure 内に builder 呼び出しを移すか、Bookkeeper を遅延初期化する形に変更する
- ただし本件は実害が小さく（builder は冪等な純粋関数想定）、性能課題が顕在化していないため低優先度

#### [Minor-D] 実機目視検証（タスク 22.2-22.6）が引き続き未完了

**該当箇所**: `openspec/changes/add-declarative-dsl/tasks.md:207-211`

**問題点**:

前回 Major-3 と同じく未消化。Critical-1 系の修正が入った今、安定 ID 経路が実機で意図通り動くこと（部分更新アニメーションが個別 Cell 単位で動く / 全削除→全挿入にならない）の最終確認が必要。

**推奨修正**: Major-A 修正後、Sample で 22.2-22.6 を実施する。

### 🔵 Suggestion

#### [Suggestion-A] iOS の `DSLHintRegistry` がプロセスローカル シングルトンであるため、複数 `KsSettingsView { ... }` を同一画面で並列描画する際にヒントが衝突する可能性

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift`

前回 Suggestion-1 と同じ。`DSLBackedRepresentable.evaluateAndApplyDiff()` の冒頭で `DSLHintRegistry.shared.reset()` を呼んでいるが、別の `KsSettingsView` がレジストリに記録したヒントを **誤って消す** リスクが残る。

**推奨修正**: 各 `DSLBookkeeper` インスタンスに非シングルトンの `HintRegistry` を持たせる、または ThreadLocal/TaskLocal に切り替える等。今は実害がないため低優先度だが、将来 TabView / SplitView 等でユースケースが増えると顕在化する。

#### [Suggestion-B] iOS / Android で `.cellID` の挙動を仕様レベルで揃える

iOS：DSLHintRegistry のサイドチャンネル経由で Node に転写される（メソッドチェーン形式 `cell.cellID("x")` で動作）
Compose：`DSLSectionScope.cellID("x")` 形式（直前 Cell に対するスコープ関数）が正規

API 形が異なるため、利用者がクロスプラットフォーム開発する際に混乱しやすい。Compose 側を iOS と同じメソッドチェーン形式に合わせるか、両 OS で「直前要素に対するスコープ関数」形式に揃えるかを検討するとよい。

### ✅ 解消した前回指摘（参考）

- Critical-1, Critical-2, Critical-3：上記サマリーの通り解消
- Major-1：tasks.md が実装状態と整合した
- Major-2：Sample に「Cell.id 引数は DSL 経路で rebind される」旨のコメントが追加
- Major-3：実機目視は未完了で Minor-D に格下げ
- Major-4：`DSLDiffCalculator.swift` の Cell 内容比較規約コメントが追加（Major レベルの設計指針が文章化された）
- Minor-1：部分的に整理（Minor-B に格下げ）
- Minor-2, Minor-3：本提案範囲外（`add-cell-types-*` 委譲）と整理
- Minor-4：本提案範囲外と整理
- Minor-5：Critical-1 の修正に伴い解消

## アクションプラン

優先度順：

1. **[Major-A]** Compose `Cell.cellID()` の動作不整合を修正
   - 推奨案 A：`Cell.cellID()` を削除し、`DSLSectionScope.cellID()` に一本化
   - Sample / ドキュメントの書き換え（Minor-A も同時解決）
   - 位置移動を跨ぐ cellID 安定性テストの追加（`DSLIntegrationTest.kt` に 1 ケース）
2. **[Minor-D]** 実機目視検証（22.2-22.6）を実施
3. **[Minor-B]** `_ = newByID` の整理（任意）
4. **[Minor-C]** `DSLBackedRepresentableView.init` の builder 呼び出し最適化（任意）
5. **[Suggestion-A]** `DSLHintRegistry` のシングルトン解消（将来課題）
6. **[Suggestion-B]** iOS / Compose の `.cellID` API 形の統一検討（将来課題）

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

理由：

- 前回 Critical 指摘 3 件はすべて構造的に解消された
- Major / Minor の大半も適切に対応された
- ただし新規に検出した **Major-A（Compose `Cell.cellID()` が仕様違反で機能しない）** が、Sample で利用されている経路かつ仕様 Scenario の THEN 句に直接違反する内容のため、マージ前に必ず修正が必要
- Critical レベルではないが、利用者が「明示 ID を付与したつもりが付与されていない」という silent な不整合は、本提案の中核価値「ID 採番をユーザーから隠蔽し、明示が必要な場合は `.cellID(_:)` を提供する」（Goals）を損なう

iOS の構造的な健全性は高く評価できるが、Compose の API 二重化バグが解消されれば次は APPROVED 相当の品質となる見込み。再レビュー必要。
