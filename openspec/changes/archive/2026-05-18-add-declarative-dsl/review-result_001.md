# レビュー結果 - add-declarative-dsl

**レビュー日時**: 2026年05月17日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-declarative-dsl

## サマリー

### 全体評価

iOS / Android 両方で「DSL 経路を提供する」骨格は実装されており、`SettingsRootBuilder` / `SectionBuilder` / 独自 `ForEach`（SwiftUI）と `DSLSettingsRootScope` / `forEach`（Compose）の DSL サーフェスが用意されている。`DSLDiffCalculator` も Section / Cell / Root H/F / Theme の各 Diff を網羅的に生成するロジックが実装され、単体テストが書かれている。テスト総数は iOS 109 / Android（gradle test）すべて成功。

しかし、**iOS 側の DSL 経路には設計コンセプトを実態として満たしていない Critical な実装欠陥** がある。仕様で MUST 規定された「同じ DSL 記述に対して body 再評価をまたいでも安定した ID を返さなければならない」を満たしておらず、サンプル画面で「項目追加」等の動的操作を行うと **全 Section / 全 Cell が remove → insert の Diff として流れる** 挙動になる。Compose 側はノード型を経由した安定 ID パス（`DSLRootTree.resolvedSections()`）が正しく接続されており、構造的には機能している。

加えて、両 OS で **`KsSettingsView { ... }` DSL 経路を実走させる Integration テストが事実上存在しない**（DSLDiffCalculator や ID 採番ユーティリティの単体テストはあるが、`builder` 経由でツリーを 2 回構築して Diff の安定性を検証するテストが無い）。タスク 8.10 / 16.6 が「Integration テスト」を要求しているが、実装上の安定 ID 接続バグを検出できる粒度に達していない。

タスク 21.5 / 22.2-22.6（目視確認）/ 24.3（validator）は未完了として正直に記録されている。タスク 24.2 は事実誤認の可能性がある（仕様レビューは「アーティファクト作成時点で完了済み」とあるが、当該レビュー結果ファイルは見当たらない）。

実装者が「未解決」として申告した backgroundColor / disabled / iconSize の no-op / 代替実装、および `KsSettingsViewStyleTests` の pre-existing main-actor 問題は、本提案範囲では受容できると判断する（spec は具象 Cell 提案 `add-cell-types-*` に責務を委譲している記述あり）。

### 判定

**CHANGES_REQUESTED**：iOS の Critical な ID 安定化バグと、それを検出できる Integration テストの欠落を修正する必要がある。Android は構造上は動くが、サンプルの利用パターンが「ID 隠蔽」の設計趣旨を活かしていない（Major）。両 OS で実機目視（タスク 22.2-22.6）が未実施のため、DSL 経路が実環境で正しく動くことの最終確認も求められる。

## 指摘事項

### 🔴 Critical

#### [Critical-1] iOS の DSL 経路で `DSLRootTree` / `DSLSectionNode` / `DSLCellNode` の安定 ID パスが接続されていない

**該当箇所**: 
- `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:236-339`（`DSLBackedView.evaluateAndApplyDiff()`）
- `ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift` （定義は存在するが呼び出し側なし）
- `ios/Sources/KsSettingsViewSwiftUI/SettingsRootBuilder.swift` （`[KsSettingsViewCore.Section]` を直接返す）

**問題点**:

`DSLBackedView.evaluateAndApplyDiff()` は `builder()` の戻り値 `[KsSettingsViewCore.Section]` を直接 `DSLDiffCalculator.ResolvedTree` に渡しており、`DSLNodes.swift` で定義された安定 ID 解決パイプライン（`DSLRootTree.resolvedSections()` → `DSLSectionNode.resolvedID(rootIdx:)` → `DSLCellNode.resolvedID(sectionID:indexInSection:)`）を **一切経由していない**。

`SettingsRootBuilder` / `SectionBuilder` の `buildExpression` も `[KsSettingsViewCore.Section]` / `[any KsCell]` のままで、Section / Cell インスタンスを `DSLSectionNode` / `DSLCellNode` に包む処理が存在しない。さらに `Section.init` は `id: UUID = UUID()` がデフォルトであるため、body 再評価のたびに新しい UUID が割り当てられる。

結果として:

1. `DSLDiffCalculator.compute(from: bookkeeper.lastTree, to: newResolved)` は `oldByID = Dictionary(... old.sections.map { ($0.id, $0) })` と `newByID = Dictionary(... new.sections.map { ($0.id, $0) })` で突合するが、**両者の Section.id 集合が完全に異なる** ため、毎回 `removeSection`（旧全件）+ `insertSection`（新全件）の Diff 列が発行される。
2. 仕様 `Section / Cell の同一性判定戦略`「同じ DSL 記述に対して body 再評価をまたいでも安定した ID を返さなければならない (MUST)」に **違反**。
3. 仕様 Scenario「完全静的構造での body 再評価耐性」「ForEach 配下の Cell ID 引き継ぎ」「ヘッダ文字列ベースの Section ID 安定性」「明示 .sectionID(_:) による動的追加の安定化」「Cell modifier 適用でも Cell ID が維持される」すべてが満たされていない。
4. サンプル `DSLDemoView` の「項目追加」ボタンを実機で押下した場合、全 Section / 全 Cell が「全削除 → 全挿入」アニメーションで一斉に再描画され、`add-partial-update-native` で整備した部分更新 API の恩恵がゼロになる（無限スクロール用途に近い性能特性）。

`ForEachDSL.swift` の `attachForEachHint(...)` は `section.id` / `cell.id` をキーにレジストリへヒントを記録するが、その後の Diff 経路では参照されない（dead code 化している）。`SectionModifiers.swift` / `CellModifiers.swift` の `.sectionID(_:)` / `.cellID(_:)` 拡張も同様。

**推奨修正**:

`DSLBackedView` で `builder()` を直接実行するのではなく、`@SettingsRootBuilder` の Component 型を `[DSLSectionNode]` ベースに作り変える、または以下のいずれかで DSL ノード経路を強制する：

**案 A（推奨）**: Builder の Component 型変更
- `SettingsRootBuilder` の Component を `[DSLSectionNode]` に変更（既存テストとの互換は `extension SettingsRoot { init }` のアダプタを残せば最小化できる）
- `SectionBuilder` の Component を `[DSLCellNode]` に変更
- `Section(...)` イニシャライザは `DSLSectionNode` を返す DSL ファクトリに置き換える
- ForEach 4 オーバーロードの戻り型を `[DSLSectionNode]` / `[DSLCellNode]` に変更し、`.forEach(itemID)` ヒントを node に保持
- `.sectionID(_:)` / `.cellID(_:)` modifier は node に `.explicit` ヒントを設定
- `DSLBackedView` は `DSLRootTree(sectionNodes: builder())` → `tree.resolvedSections()` で安定 ID 解決済みの Section 群を取得し、ResolvedTree に渡す

**案 B（次善）**: Builder Component を維持しつつ、`evaluateAndApplyDiff()` の中で「旧 Section.id 集合 と 新 Section.id 集合が一致するかを ID 採番ヒント逆引きで再計算する」アダプタを挟む。ただしレジストリのキーが `section.id`（新規 UUID）であるため body 再評価のたびにキーが変わり、機能しない。実質案 A 一択。

**いずれの案でも以下が必須**:
- `KsSettingsView { Section("見出し") { LabelCell("X") } }` を 2 回評価し、両者の resolved Section.id / Cell.id が一致することを検証する Integration テスト
- `KsSettingsView { ForEach(items) { ... } }` で items に append したときに既存 Cell の ID が不変であることを検証する Integration テスト

#### [Critical-2] DSL Integration テストが事実上存在しない（タスク 8.10 / 16.6 の偽完了）

**該当箇所**:
- `ios/Tests/KsSettingsViewSwiftUITests/`（DSL を経由した body 再評価の Integration テスト無し）
- `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt`（Store 方式と `settingsRoot { }` 純粋関数の検証のみ）
- `openspec/changes/add-declarative-dsl/tasks.md:83`（タスク 8.10）
- `openspec/changes/add-declarative-dsl/tasks.md:152`（タスク 16.6）

**問題点**:

タスク 8.10 は以下の Integration テストを要求している：
- DSL 方式での初回作成
- @State 変更による再描画
- Store 方式併存
- Root H/F modifier 反映
- Section H/F modifier 反映
- Cell modifier 反映
- ForEach 配下の動的追加

しかし `DSLDiffCalculatorTests.swift` / `DeclarativeDSLIdentityTests.swift` / `ForEachDSLTests.swift` / `SectionModifiersTests.swift` / `CellModifiersTests.swift` は **いずれも単体ユニットテスト** であり、`KsSettingsView { ... }` を 2 回評価して Diff の安定性を検証するテストは存在しない。`KsSettingsViewRepresentableTests.swift` は Store 方式のみ。

Compose 側も同様で、`KsSettingsViewComposeTest.kt` は Store 方式または `settingsRoot { }` 純粋関数の itemCount 検証のみ。

このため Critical-1 のバグがテスト上検出できず通っている。タスクがチェック済になっているのは事実誤認。

**推奨修正**:

1. タスク 8.10 / 16.6 のチェックを外し、以下の Integration テストを追加：
   - 静的 DSL（`KsSettingsView { Section("X") { LabelCell("A") } }`）を 2 回評価し、Diff Calculator が空 Diff を返すこと
   - `@State` で Cell タイトルだけ変更 → 該当 Cell の `.replaceCell` のみ発行されること
   - `ForEach(items)` で items を append → 既存 Cell ID 不変・新規のみ insert になること
   - `.sectionID("a")` で安定 ID 指定 → 動的追加で既存 Section が保持されること
2. Compose も同等の Integration テストを追加（Compose UI Test + Robolectric）

#### [Critical-3] `DSLBackedView.body` 内で副作用（applyDiff）を発生させている

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/KsSettingsView.swift:277-287`

**問題点**:

```swift
var body: some View {
    evaluateAndApplyDiff()  // ← body getter 内で @StateObject の state を変更
    return StoreBackedRepresentable(...)
}
```

SwiftUI の View `body` は **副作用なしの pure な計算プロパティ** であるべきという公式ガイドラインに違反している。`@StateObject` 内部 Store の状態を body 評価中に変更すると：

- 「Modifying state during view update, this will cause undefined behavior.」ランタイム警告がコンソールに連発する可能性
- ホットリロード / プレビュー時の挙動が不定になる
- 再帰的 body 評価が走るとループになる可能性
- SwiftUI の transaction 管理から外れ、アニメーションが正しく適用されない

Compose 側は `SideEffect { ... }` ブロック内で副作用を起こしており、これは Compose の推奨パターンに従っている（適切）。

**推奨修正**:

iOS 側でも以下のいずれかに変更：
- `body` 内で `evaluateAndApplyDiff()` を直接呼ぶのをやめ、SwiftUI の `onChange(of:)` / `task { ... }` / `Representable` の `updateUIViewController` 内に移動する
- `DSLBackedView` を `UIViewControllerRepresentable` 自体に作り変え、`updateUIViewController(_:context:)` 内で Diff 適用する（推奨）

### 🟠 Major

#### [Major-1] タスクの偽完了が複数（タスク自体は完了マークされているが実装が伴わない）

**該当箇所**: `openspec/changes/add-declarative-dsl/tasks.md`

**問題点**:

- タスク 2.4 「`AnyHashable` 受け入れ・`KsCellID` への変換ロジックを実装する」: 実コード上の変換ロジックは `KsCellID(cell:)` を呼んでいるだけで、`AnyHashable` ヒントからの ID 解決経路は未接続
- タスク 3.6 「`ForEach` 関数の中で各要素に **ID 採番ヒント** を埋め込む仕組み」: レジストリへの記録は実装されているが、Critical-1 の通り消費されない
- タスク 7.6 「Root H/F の比較で `updateAccessory` を生成」: 実装あり ✓（ただしテストではないが軽微）
- タスク 8.10 / 16.6: Critical-2 の通り Integration テスト欠如
- タスク 19.1: iOS / Android で「DSL 構造から生成される Diff 列を検証する」とあるが、実態は ResolvedTree を手で組み立てた単体テストのみで「同等 DSL 構造から」の検証になっていない
- タスク 24.2 「sdd-spec-reviewer 仕様レビューは APPROVED されている前提」: 該当 review ファイルが見当たらない

**推奨修正**:

各タスクのチェックを外し、実装または検証を追加する。仕様レビュー APPROVED の根拠は明示する。

#### [Major-2] Compose Sample の DSL 利用が「ID 隠蔽」設計趣旨を活かせていない

**該当箇所**: `samples/android/app/src/main/kotlin/jp/kamusoft/kssettingsview/samples/android/MainActivity.kt:129-134`

```kotlin
cell(SampleLabelCell(id = "static-a", title = "固定 Cell A"))
cell(SampleLabelCell(id = "static-b", title = "固定 Cell B"))
...
forEach(items.value, key = { it }) { name ->
    cell(SampleLabelCell(id = "dynamic-$name", title = name))
}
```

**問題点**:

仕様 Goals「Cell / Section の ID 採番をユーザーから隠蔽し、明示が必要な場合は `.cellID(_:)` / `.sectionID(_:)` modifier を提供する」とあるが、サンプルでは利用者が直接 Cell コンストラクタで `id = ...` を渡している。これは Compose `Cell` インターフェースが `val id: String` を要求しているため避けられないが、本来 DSL 経路では `DSLReidentifiableCell` 規約で id を rebind するため、利用者の入力 id は意味を持たない（捨てられる）。

サンプルが「ユーザーが id を意識する」記述になっているため、DSL の宣言的価値が伝わらない。

**推奨修正**:

- 後続 `add-cell-types-*` で具象 Cell が実装されるまでは PoC で代替するしかないが、ドキュメント / コメントで「Cell コンストラクタの id 引数は DSL 経路では無視され、DSL ノードで採番される」旨を明記する
- 可能なら Sample でも `cellID("dynamic-$name")` のような modifier 形式で明示する例にし、本来の規約を体現する

#### [Major-3] 実機目視検証（タスク 22.2-22.6）が未実施で動作確認の最終工程が不在

**該当箇所**: `openspec/changes/add-declarative-dsl/tasks.md:203-207`

**問題点**:

- 22.2: Sample アプリ起動・DSL デモ画面描画
- 22.3: DSL 動的追加・削除の部分更新アニメーション
- 22.4: Cell modifier 視覚確認
- 22.5: Root H/F 任意 View 描画
- 22.6: Section H/F 任意 View 描画

これらが未完了のままだと、Critical-1 のバグ（全削除→全挿入のアニメーション）が運用前に発覚しない可能性が高い。本提案の Migration Plan も「Sample で目視確認済」を完了条件としている。

**推奨修正**:

Critical-1 / Critical-3 を修正後、Sample で実機 / シミュレータ目視検証を実施し、22.2-22.6 を実態としてチェックする。

#### [Major-4] `DSLDiffCalculator.compute` の `cellLevelDiffs` で `KsCellID(cell:)` が誤った Cell から ID を取り出している

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:151-152`

```swift
for cell in old where !newIDs.contains(cell.id) {
    diffs.append(.removeCell(cellID: KsCellID(cell: cell)))
}
```

**問題点**:

`KsCellID(cell:)` のシグネチャと cell の id 表現が不一致だと、Diff 経路の Cell ID が Store / Controller 側の管理 ID と整合しない可能性がある。一見動いて見えるが、`add-partial-update-core` で導入された `KsCellID` 型 → UUID 変換ルールに依存している。

加えて、`AnyHashable(oldCell) != AnyHashable(cell)` という比較は `KsCell` の `Hashable` 実装に依存しているが、Cell 実装側で `id` を Hashable 計算に含めると「同 ID で内容違いの Cell」も `not equal` 判定となり一見正しく動くが、Cell 実装が `id` 以外のフィールドのみで Hashable を実装している場合は壊れる。

**推奨修正**:

`AnyHashable(cell)` ではなく `cell.isContentEqual(to:)` 等の意味的比較メソッドを `KsCell` プロトコル要件として追加するか、`Hashable` に含まれるフィールドの規約を仕様で明示する。

### 🟡 Minor

#### [Minor-1] `DSLDiffCalculator.swift` 内に dead store（`_ = oldByID` / `_ = newByID`）が残っている

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLDiffCalculator.swift:78-79`

```swift
_ = oldByID
_ = newByID
```

ループ内で `_ =` で受けるだけのコードが残っている。コンパイラ警告抑制目的だとしても、ループ外で 1 回だけ書けば良い。あるいは元々の意図が不明。

**推奨修正**: 削除する。

#### [Minor-2] `CellModifiers.swift` で `.backgroundColor(_:)` / `.disabled(_:)` が no-op 実装

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:136-148`

**問題点**:

仕様 `Cell の View modifier` 一覧で `.backgroundColor(_ color: KsColor)` / `.disabled(_ flag: Bool)` が明示されているが、`CellStyle` 側にフィールドが未追加のため no-op 実装。コメントで「`CellStyle` に backgroundColor が追加されたら反映する」と書かれているが、利用者は modifier を呼んでも視覚的に何も変わらず混乱の元。

**推奨修正**:

- 短期: `@available(*, unavailable, message: "...")` 等で deprecated 警告を出し、利用者に no-op だと明示する
- 中期: `CellStyle` に `backgroundColor: KsColor?` / `disabled: Bool` フィールドを追加する別変更提案を作成する（実装者報告通り `add-cell-types-*` 側で対応するなら、現提案では削除する）

#### [Minor-3] `iconSize(_:)` の引数型が仕様の `.icon(_ icon: KsIcon)` と異なる

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:80-94`

**問題点**:

仕様には `.icon(_ icon: KsIcon)` とあるが、`KsIcon` 型は未定義のため `iconSize(_ size: CGFloat)` で代替実装している。これは Cell modifier API の名前と意味が乖離しており、利用者から見て「`.icon(.system("person"))` という DSL Scenario が動かない」状態。

**推奨修正**:

- `KsIcon` 型の追加は `add-cell-types-*` の責務として仕様で明記し、本提案ではドキュメントで「`.icon(...)` は具象 Cell 提案実装後に有効化」と注記
- それまでは `.iconSize(_:)` でも良いが、命名が仕様と異なる旨をドキュメント / コメントに明記

#### [Minor-4] `KsSettingsViewStyleTests` の main-actor isolation エラー（実装者報告）

**該当箇所**: `ios/Tests/KsSettingsViewUITests/KsSettingsViewStyleTests.swift`

**問題点**:

実装者が「pre-existing」と申告しているが、レビュー時点でテストが全件パスしている（109 件成功）のは事実。問題はおそらく Swift 6 / SwiftTesting 移行時の警告レベル止まりか、または当該テストファイル側で `@MainActor` 注釈追加で解消済の可能性が高い。

**推奨修正**:

- 「pre-existing と主張」とあるが、本提案の作業範囲外なら別 Issue として分離し、本提案では触れない
- もし `_Bootstrap.swift` 等で `@testable import` を追加した結果として表面化したなら、本提案範囲で修正する

#### [Minor-5] iOS の Builder（タスク 4.1, 4.2）の `buildExpression` オーバーロード追加が空振り

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/SettingsRootBuilder.swift:40-44`

**問題点**:

`buildExpression(_ expression: [KsSettingsViewCore.Section]) -> [KsSettingsViewCore.Section]` は ForEach の戻り値を受けるためのオーバーロード（タスク 4.1 で要求）。実装はあるが、ForEach は同じ `[Section]` を返す関数なのでこのオーバーロードに乗る一方、ID ヒント伝播を含む `DSLSectionNode` レイヤを経由しないため、Critical-1 の通り意味を成さない。

**推奨修正**: Critical-1 の修正に合わせて、Component 型を `[DSLSectionNode]` に変更する。

### 🔵 Suggestion

#### [Suggestion-1] `DSLHintRegistry` のシングルトン設計は将来的にマルチ View / マルチスレッド競合の原因になる可能性

**該当箇所**: `ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift`

複数の `KsSettingsView { ... }` が同時に存在する画面で（例：TabView / Split View）、各 View の body 評価が交互に走ると、`reset()` で他 View のヒントを消してしまうリスクがある。スコープを `KsSettingsView` インスタンスごとの非シングルトンに変更することを検討。

#### [Suggestion-2] 案 A の Builder Component 型変更を採用する際は、既存 archive 済の `SettingsRoot { ... }` DSL（`settings-view-ios-ui` 初期 archive のテスト）との互換性を確認する

`extension SettingsRoot { init(theme:, @SettingsRootBuilder sections:) }` が `[Section]` を受け取っている前提なので、Component 型を変更すると壊れる。アダプタを残すか、`SettingsRoot` 初期化経路は別オーバーロードを追加するか方針決定が必要。

#### [Suggestion-3] Compose 側でも `DSLSectionScope.cell(cell: Cell)` で受け取った Cell の id を data class copy で安定 ID に rebind する経路が動いていることは正しいが、利用者向けドキュメントで「Cell の id 引数は DSL 経路では無視される」旨を明記すると親切

`docs/declarative-dsl-guide.md` のサンプルコードにも `id = ...` を渡す例があるため、混乱を避けるためコメントで補足する。

## アクションプラン

優先度順に対応すべき項目をリスト化：

1. **[Critical-1] iOS の DSL ノード経路接続**：`SettingsRootBuilder` / `SectionBuilder` の Component 型を `[DSLSectionNode]` / `[DSLCellNode]` に変更し、`DSLBackedView` で `DSLRootTree.resolvedSections()` を経由して安定 ID を解決した上で `DSLDiffCalculator` に渡す。`ForEach` / `.sectionID(_:)` / `.cellID(_:)` のヒント伝搬を node に保持する形に書き換える。
2. **[Critical-3] `DSLBackedView.body` 副作用の除去**：Diff 適用ロジックを `body` getter から外し、`UIViewControllerRepresentable.updateUIViewController` または `onChange` 経路に移動する。
3. **[Critical-2] DSL Integration テスト追加**：上記修正の正しさを検証するため、`KsSettingsView { ... }` を 2 回評価して以下を検証する：
   - 静的構造で空 Diff
   - Cell 内容変更で該当 Cell の `replaceCell` のみ
   - `ForEach(items)` 追加で既存 ID 不変・新規のみ `insertCell`
   - `.sectionID(_:)` で動的追加の安定化
   - `.rootHeader(_:)` 変更で Root H/F の `updateAccessory`
   - Compose も同等を追加
4. **[Major-1] タスク再点検**：実態に合わせて tasks.md のチェックを更新（偽完了のものを未完了に戻す）。仕様レビューの APPROVED 根拠を明示。
5. **[Major-3] 実機目視検証**：1-3 の修正後、Sample で 22.2-22.6 を実施。タスク 21.5 (Diff デバッグオーバーレイ) は任意なので保留可。
6. **[Major-4] Cell 比較規約の明確化**：`KsCell` プロトコルに「Cell の内容比較に id を含めるか」を仕様文書または規約コメントで明示する。
7. **[Major-2 / Suggestion-3] Sample / ドキュメントの DSL 規約整理**：Compose Sample の id 渡しと docs/declarative-dsl-guide.md のコメント補足。
8. **[Minor-1] DSLDiffCalculator のデッドコード削除**
9. **[Minor-2 / Minor-3] no-op modifier の deprecated 化または別提案へ分離**
10. **[Minor-5 / Suggestion-2] Builder Component 型変更時の archive 済 API 互換確認**
11. **[Suggestion-1] DSLHintRegistry のスコープ分離検討**

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

理由：

- Critical 指摘 3 件（iOS の安定 ID 経路欠如・body 副作用・Integration テスト欠如）が仕様 MUST 要件に直接違反している
- Major 指摘 4 件のうち 1 件（タスク偽完了）は仕様駆動開発のプロセス整合性に関わる重大事
- 既存単体テストは通過しているが、Critical-1 を検出するレベルのテストが構造的に欠落しているため、テスト成功は実装の正当性を担保していない
- 実機目視（22.2-22.6）も未実施のため、Critical-1 のバグが運用前に表面化しない可能性が高い

iOS の Critical-1 を修正しない限り、本提案の中核価値（「DSL の書き味」と「内部 Store + applyDiff の性能特性」の両立）は実態として実現されない。再レビュー必要。

---

## 訂正 (2026-05-18)

本レビューの本文 L123 周辺で「Compose 側は `SideEffect { ... }` ブロック内で副作用を起こしており、これは Compose の推奨パターンに従っている（適切）」と評価したが、これは **誤り** であることが Android Sample の動作確認で判明した。

**判明した問題**: DSL 方式 `KsSettingsView { ... }` Composable は、Composable 本体が外部 state（`mutableStateOf` 等）を直接読まず、`content: DSLSettingsRootScope.() -> Unit` lambda 内（`SideEffect` ブロックでの呼び出し時）でしか読まない。`SideEffect` ブロックは Snapshot 観測下で実行されないため、Compose runtime のスナップショット観測グラフに **外部 state → 当該 Composable の再評価依存** が登録されない。結果、親 Composable のリコンポーズに偶発的に巻き込まれた回（典型的には 1 回目）だけ `SideEffect` が走り、2 回目以降は再評価が起きず Diff が発行されない、というバグが顕在化した。

**症状**: Android Sample の DSL デモで「項目追加 / 末尾削除」ボタンが 1 回目だけ動作し、2 回目以降は全く反映されない。

**修正**: Diff 適用を `SideEffect { ... }` から `AndroidView { factory / update }` の `update` ブロックに移動した。`AndroidView.update` は Compose runtime がリコンポーズコミットごとに直接スケジュールするため、`SideEffect` のような skippable 判定の影響を受けず、外部 state の変化を確実に View 層へ伝播できる。iOS 側 `updateUIViewController(_:context:)` と同じセマンティクス。

**影響範囲**:

- 実装: `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposable.kt` の DSL 版を `AndroidView { factory / update }` 直接利用に書き換え。Store 方式版と共通の `bindAndroidView` ヘルパに切り出し。
- テスト: `KsSettingsViewComposeTest.kt` に「DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される」リグレッションテストを追加。
- 仕様（規範部）への影響: なし。`specs/settings-view-android-ui/spec.md` の Requirement / Scenario は実装機構を規定していない。むしろ L60-70 「DSL 方式での @State 変更による再描画」Scenario は本修正により実コードで初めて充足される。
- 仕様（非規範部）: `design.md` の擬似コードを `AndroidView.update` ベースに更新、`tasks.md` の 16.4 の実装機構記述を更新、`verification-report.md` の Android 副作用ポイント記述を更新。

**教訓**: `SideEffect` は「Compose state を非 Compose 管理オブジェクトと毎回の成功した recomposition で同期する」用途に限定される。当該 Composable がスナップショット観測グラフに登録されていない場合、リコンポーズ自体がスケジュールされず `SideEffect` も走らない。外部 state を直接読まない Composable で副作用を確実に走らせたい場合は、`AndroidView.update` ブロック（View interop）か、明示的に外部 state を `key` に取る `LaunchedEffect` を使うべきだった。
