# レビュー結果 - add-declarative-dsl (4 回目 / Section 25 オーナーレビュー対応)

**レビュー日時**: 2026年05月18日
**レビュワー**: sdd-reviewer
**変更提案ID**: add-declarative-dsl
**前回レビュー**: `review-result_003.md`（APPROVED, ただし Minor-D / Suggestion-A は archive 前確認事項として継続）
**レビュー対象**: Section 25「オーナーレビュー対応：Android DSL を iOS と整合的にする」の実装

## サマリー

### 全体評価

Section 25 オーナーレビュー対応の実装は **意図された設計（design.md Decision 5 改訂・Section 25.x のタスク）と整合** しており、レビュー結果 001〜003 で確認された Critical 指摘の解消状態を維持したまま、Compose 側の DSL を iOS と完全並列な書き味（`Section("...") { LabelCell(...) }.sectionFooter("...")` 形式と Cell 直置き）に引き上げている。具体的には以下の 5 点が達成されている：

1. **25.0 循環依存回避**: `DSLReidentifiable` / `DSLStyleModifiable`（Swift）と `DSLReidentifiableCell` / `DSLStyleModifiableCell`（Kotlin）が両 OS とも Core モジュールへ移動済み。後続 `add-cell-types-*` 系の具象 Cell（`*-ui` 配置）が Core protocol/interface に依存することで `*-ui → *-compose / SwiftUI` の循環依存が回避される構造が確立された。
2. **25.1 SectionHandle / CellHandle**: `DSLSettingsRootScope.Section(...)` / `DSLSectionScope.cell(...)` の戻り値を `Unit` → `SectionHandle` / `CellHandle` に変更し、`internal constructor` で外部生成を禁止。`SectionHandle` には `.sectionHeader(text|content)` / `.sectionFooter(text|content)` / `.sectionID(id)` を、`CellHandle` には `.font(...)` / `.cellHeight(...)` / `.titleColor(...)` / `.backgroundColor(...)`（暫定 no-op）/ `.disabled(...)`（暫定 no-op）/ `.cellID(id)` を `@SettingsRootDsl` 付き拡張関数として提供。
3. **25.2 KsIdentifiable forEach**: `interface KsIdentifiable { val id: Any }` を Compose モジュールに新規追加。`DSLSettingsRootScope.forEach<T : KsIdentifiable>(items)` / `DSLSectionScope.forEach<T : KsIdentifiable>(items)` の `key` 省略版を `inline reified` で実装。
4. **25.3 Sample 拡張**: `SampleLabelCell.id` にデフォルト UUID 値、`SampleLabelCellDsl.kt`（`DSLSectionScope.SampleLabelCell(title:)` 拡張関数）、`DemoItem : KsIdentifiable`、`Section("...") { SampleLabelCell(...) }.sectionFooter("...")` chain を導入。iOS Sample と書き味が並列化された。
5. **25.4 テスト**: `DSLHandleTest.kt`（12 ケース）で SectionHandle/CellHandle 各 modifier・KsIdentifiable forEach（RootScope/SectionScope）・unaryPlus・デフォルト id 値の DSL rebind・後方互換性を網羅。`first[0].cells[0].id == second[0].cells[1].id` 等の位置移動を跨いだ ID 安定性も検証。

### ビルド・テスト結果

- **Android**: `./gradlew :ks-settingsview-core:testDebugUnitTest :ks-settingsview-compose:testDebugUnitTest` および `--rerun-tasks` 強制実行ともに BUILD SUCCESSFUL（63 actionable tasks all executed/up-to-date）。Section 25.4 で追加された `DSLHandleTest.kt` 12 ケースも含めて Pass。
- **iOS**: `swift test` 全 117 件 Pass、0 failures。

### 仕様（spec）整合性

`specs/settings-view-android-ui/spec.md` の Section 25 関連 Requirement / Scenario をすべて確認：

| Spec 要件 | 実装ファイル | 整合性 |
|----------|------------|--------|
| `Section(...): SectionHandle` 戻り値変更 | `DSLScope.kt:33-64` | ✅ 一致 |
| `cell(...): CellHandle` 戻り値変更 | `DSLScope.kt:158-168` | ✅ 一致 |
| `SectionHandle.sectionHeader/Footer/ID` chain | `DSLHandles.kt:44-75` | ✅ 一致 |
| `CellHandle.font/cellHeight/titleColor/cellID` chain | `DSLHandles.kt:83-127` | ✅ 一致 |
| `operator fun Cell.unaryPlus()` 追加 | `DSLScope.kt:176` | ✅ 一致 |
| `KsIdentifiable` marker interface 追加 | `KsIdentifiable.kt` | ✅ 一致 |
| `forEach<T : KsIdentifiable>` key 省略版 | `DSLScope.kt:264-279` | ✅ 一致（inline reified） |
| `DSLReidentifiableCell` / `DSLStyleModifiableCell` を Core 配置 | `ks-settingsview-core/.../DSLCellIdentity.kt` | ✅ 一致 |
| iOS `DSLReidentifiable` / `DSLStyleModifiable` を Core 配置 | `KsSettingsViewCore/DSLCellIdentity.swift` | ✅ 一致 |
| Cell コンストラクタの `id` デフォルト値規約 | `SampleLabelCell.kt:27` | ✅ 規約準拠（Sample で実例提示） |
| Compose Root H/F は引数指定維持 | `KsSettingsViewComposable.kt:93-103` | ✅ 一致（意図的非対称） |

`review-result_003.md` 時点で APPROVED されていた経路（`DSLExplicitIdCell` sentinel での `Cell.cellID(...)` 経路、iOS `DSLBackedRepresentableView` の副作用分離、AndroidView.update での Diff 適用）はすべて維持されており、退化（regression）なし。

### 判定

**APPROVED**：Section 25 の追加実装は spec / design / tasks と整合し、ビルド・テストとも成功している。指摘事項は **Minor 4 件** / **Suggestion 2 件** のみで、いずれもマージ阻害要因にはならない（うち 2 件は前回からの継続課題、2 件は今回新規）。

ただし `Minor-E`（コメント／ドキュメントの陳腐化）は **archive 前に必ず修正** すべきである（実装と説明の不一致は将来の保守者を誤誘導する）。

## 指摘事項

### 🟡 Minor

#### [Minor-E] iOS の `DSLNodes.swift` / `CellModifiers.swift` のヘッダコメントが Section 25.0 移動後も **陳腐化したまま** 残っている（新規）

**該当箇所**:
- `ios/Sources/KsSettingsViewSwiftUI/DSLNodes.swift:21-22`
- `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:18`
- 副次: `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift:156`（`fileprivate func mutateStyle` の docstring）

**問題点**:

Section 25.0 で `DSLReidentifiable` / `DSLStyleModifiable` protocol を `KsSettingsViewCore` モジュールに移動したが、ファイル先頭のヘッダコメントは依然として「本 SwiftUI モジュール内で `DSLReidentifiable` プロトコルを定義し」「本 SwiftUI モジュール内で `DSLStyleModifiable` プロトコルを定義し」と記述している。

```swift
// DSLNodes.swift L21-22
// `KsSettingsViewCore` 層は本提案の対象外（変更禁止）のため、Cell 側に re-id API を
// 標準で要求できない。本 SwiftUI モジュール内で `DSLReidentifiable` プロトコルを定義し、
// 具象 Cell が opt-in する形を採用する。
```

```swift
// CellModifiers.swift L18
// 本 SwiftUI モジュール内で `DSLStyleModifiable` プロトコルを定義し、
// 具象 Cell が opt-in する形を採用する。
```

実装本体（`import KsSettingsViewCore` 経由で型を参照）は正しく Core 経由になっており動作上の問題はないが、**コメントが実態と矛盾している** ため、将来の保守者が「protocol を SwiftUI モジュール内で再定義してよい」と誤解する可能性がある。また `25.0` の循環依存回避の動機（後続 `add-cell-types-*` 系の具象 Cell が `*-ui` 配置になることへの備え）が読み取れない。

**推奨修正**:

両ファイルのヘッダコメントを「`KsSettingsViewCore` モジュールに配置された `DSLReidentifiable` / `DSLStyleModifiable` protocol を import して利用する」旨に書き換える。同時に Core 配置の理由（循環依存回避）への参照（design.md Decision 4 / Section 25.0）を追記する。Android 側 `DSLCellIdentity.kt` のヘッダコメント（L10-15）が良い記述例。

具体的には `DSLNodes.swift` の L18-25 を以下に置換：

```swift
// `KsCell` プロトコルが要求する `id: UUID` は通常 `init(... id: UUID = UUID())` のように
// 値型ごとに自動採番される。DSL では body 再評価のたびに新規 `UUID` が生成されると
// Diff 同一性判定が破壊されるため、DSL ラッパが採番した「安定 UUID」で Cell の `id` を
// **書き換える** 必要がある。
//
// 本提案 Section 25.0 で、`DSLReidentifiable` プロトコルは `KsSettingsViewCore` モジュールに
// 移動された（後続 `add-cell-types-*` で具象 Cell が `KsSettingsViewUI` 配置になる際の
// `KsSettingsViewUI → KsSettingsViewSwiftUI` 循環依存回避が目的、design.md Decision 4 参照）。
// 具象 Cell が opt-in しない場合は利用者責任で `id` の安定性を確保してもらう。
```

#### [Minor-F] `SectionHandle` / `CellHandle` クラス本体に `@SettingsRootDsl` (`@DslMarker`) 注釈が **付与されていない**（spec L173 と微妙な乖離）（新規）

**該当箇所**: `android/ks-settingsview-compose/.../DSLHandles.kt:21,33`

**問題点**:

`specs/settings-view-android-ui/spec.md:173` は以下を要求している：

> `SectionHandle` / `CellHandle` は `internal constructor` + `@SettingsRootDsl` で外部生成不可とし、scope 越境を防ぐ

しかし実装では `internal constructor` のみ採用されており、クラス本体には `@SettingsRootDsl` 注釈がない：

```kotlin
// DSLHandles.kt L21-24
class SectionHandle internal constructor(
    internal val scope: DSLSettingsRootScope,
    internal val index: Int,
)

// DSLHandles.kt L33-36
class CellHandle internal constructor(
    internal val sectionScope: DSLSectionScope,
    internal val index: Int,
)
```

`@SettingsRootDsl` annotation は extension function 側（`fun SectionHandle.sectionHeader(...)` 等）には付与されているため、メソッドチェーン経路では DSL marker が機能する。しかし以下の 2 点が懸念：

1. **spec 文言との literal 不一致**: spec が「`@SettingsRootDsl` で外部生成不可とし」と書いているが、`@SettingsRootDsl` は `@DslMarker` であり、外部生成を制限する機能はもともと持たない。spec 文言が `internal constructor` と `@SettingsRootDsl` を並列に列挙しているのは混乱しているが、字面通り読むと **クラス本体への annotation 付与** を意図している可能性が高い。
2. **`@DslMarker` の入れ子誤用検出**: Kotlin の `@DslMarker` は **receiver の入れ子衝突** をコンパイル時に検出する仕組み。クラス本体に注釈がないと、`SectionHandle` を receiver にした拡張関数を別の DSL receiver（例: 別 `DSLSectionScope`）の中で呼び出した場合の検出が不完全になる。実害は限定的（`SectionHandle` 自体は member function を持たず、receiver として独立しているため）だが、spec 通り注釈すれば防御が一層堅くなる。

**推奨修正**:

両クラス本体に `@SettingsRootDsl` を付与する：

```kotlin
@SettingsRootDsl
class SectionHandle internal constructor(
    internal val scope: DSLSettingsRootScope,
    internal val index: Int,
)

@SettingsRootDsl
class CellHandle internal constructor(
    internal val sectionScope: DSLSectionScope,
    internal val index: Int,
)
```

これにより spec 文言と実装が一致し、Kotlin `@DslMarker` の入れ子検出が `SectionHandle` / `CellHandle` 自体にも適用される。

#### [Minor-G] `sectionID` / `cellID` の **3 系統 API 併存** が利用者にとって紛らわしい（spec 整合だが UX 上の弱点、新規）

**該当箇所**:
- `SectionHandle.sectionID(id: Any)` … `DSLHandles.kt:72` — **新規 chain 形式（推奨）**
- `DSLSettingsRootScope.sectionID(id: Any)` … `SectionModifiers.kt:22` — 旧 sentinel 形式（"直後 sectionID 呼び出し"）
- `Cell.cellID(id: Any)` … `CellModifiers.kt:78` — `DSLExplicitIdCell` sentinel ラップ形式
- `CellHandle.cellID(id: Any)` … `DSLHandles.kt:124` — **新規 chain 形式（推奨）**
- `DSLSectionScope.cellID(id: Any)` … `SectionModifiers.kt:29` — 旧 sentinel 形式

**問題点**:

Section 25.1 で `SectionHandle.sectionID` / `CellHandle.cellID` の chain 形式が「正規 API」として導入されたが、`review-result_002.md` Major-A 対応で導入された `Cell.cellID(...)` の sentinel ラップ形式（`DSLExplicitIdCell`）、および当初実装の `DSLSectionScope.cellID(...)` / `DSLSettingsRootScope.sectionID(...)` 形式が併存している。

- 各 API は **正しく動作する**（テストも `DSLHandleTest.kt` および `DSLIntegrationTest.kt` で並行検証されている）
- ただし `SectionModifiers.kt:15-16` のコメント「`Section { ... }.sectionID("...")` のメソッドチェーン形式は採用できない」は **25.1 で覆されている**（chain 形式は正規 API として実装されている）ため、コメントが陳腐化している
- 利用者からは「どれを使えばよいのか」が判然としない。`docs/declarative-dsl-guide.md` L165-172 でメソッドチェーン形式とスコープ関数形式の使い分けを「等価」と説明しているが、25.1 で chain 形式が **正規** になったことを反映していない

**推奨修正**:

1. `SectionModifiers.kt` の docstring と冒頭コメント（L10-21）を更新：
   - 「Compose の Section は値を返さない関数として呼ばれるため、メソッドチェーン形式は採用できない」という記述を削除（25.1 で chain 形式が正規化された）
   - 旧形式（`DSLSettingsRootScope.sectionID(...)` / `DSLSectionScope.cellID(...)`）を **DEPRECATED 候補** として位置づけ、`@Deprecated(message = "...", replaceWith = ...)` 付与を検討
   - もしくは「旧形式は 25.1 以前との後方互換のため残置するが、新規記述では `SectionHandle.sectionID(...)` / `CellHandle.cellID(...)` chain を推奨する」旨を明記
2. `docs/declarative-dsl-guide.md` の §4「ID 自動採番の仕組み」を更新：
   - chain 形式（`Section { ... }.sectionID("...")` / `cell(...).cellID("...")`）を **第一推奨**、`Cell.cellID(...)` sentinel 形式と旧スコープ関数形式を **後方互換** と明示
   - 関連: review-result_003 Suggestion-B「iOS / Compose の `.cellID` API 形を仕様レベルで揃える」の進捗。25.1 で chain 形式が追加されたことで iOS の `.cellID(_:)` メソッドチェーン形式と **書き味が並列化** されたため、利用者向けに「iOS と同じ chain 形式が Compose でも正規化された」旨をドキュメントに明記すると、クロスプラットフォーム開発者の混乱が減る

#### [Minor-H] 実機目視確認（22.2-22.6）が依然未完了（前回継続）

**該当箇所**: `openspec/changes/add-declarative-dsl/tasks.md:213-223`

**問題点**:

`review-result_003.md` の Minor-D に続き、`verification-report.md` の追補3 で「手動目視確認手順」として残された 22.2〜22.6 は **未消化** のまま。Section 25 で Android Sample の DSL 記述が新形式（`SampleLabelCell(...)` 直置き、`.sectionFooter(...)` chain、`KsIdentifiable` forEach）に書き換えられているため、特に以下の項目は最終確認が必要：

- 22.4: `SampleLabelCell(title = "Cellは Modifier で装飾できる").cellHeight(80.dp)` chain が実機で 80dp 高さで描画される
- 22.6: Section の `.sectionFooter("...")` chain が UI 層 `KsSettingsViewLayout` で footer accessory として描画される

ビルド・ユニットテストは全通過、Diff 算出ロジック・ID 採番ロジックは Integration テスト（`DSLIntegrationTest.kt` 8 ケース、`DSLHandleTest.kt` 12 ケース）で検証済みのため、実機確認は **「コード経路は連結されているが、UI 層から見た最終描画の最終受け入れ確認」** の位置づけ。archive 前のユーザー側確認事項として残す。

**推奨修正**:

archive 前に iOS Simulator / Android Emulator で `DSLDemoView` / `DSLDemoScreen` を実際に起動し、`verification-report.md` 追補3 の手順に従って 22.2〜22.6 を実施・チェックする。問題があれば後追い bug fix 提案を立てる。

### 🔵 Suggestion

#### [Suggestion-A] `DSLHintRegistry` プロセスローカル シングルトン（前回継続）

`review-result_003.md` Suggestion-A の通り、`ios/Sources/KsSettingsViewSwiftUI/DSLHintRegistry.swift` のシングルトン設計は将来 TabView / SplitView 等の並列描画ユースケースが顕在化したら見直し対象。本提案範囲では実害なし。

#### [Suggestion-D] iOS / Compose のメッセージ API 整合性向上（25.1 で改善されたが残課題、新規）

iOS は SwiftUI の View modifier として `KsSettingsView { ... }.rootHeader("...")` を提供しているのに対し、Compose は引数指定 `KsSettingsView(rootHeader = { ... })` のまま。Section 25.1 で **Section H/F と Cell modifier は両 OS で chain 形式に揃った** が、Root H/F は意図的に非対称のまま残っている（design.md Decision 5 で Compose イディオム尊重として確定済）。

利用者向けには `docs/declarative-dsl-guide.md` §12 で「意図的な非対称」として明示されており、本提案範囲では対応不要。後続提案で Compose の `KsSettingsView` を Builder パターン化する独立提案が立てられた場合、再検討の余地あり。

### ✅ 解消した／継承された前回指摘（参考）

| 前回指摘 | 状態 |
|---------|-----|
| Critical-1（iOS DSL 安定 ID 経路） | 既に解消（review-result_002 / 003 で確認） |
| Critical-2（DSL Integration テスト欠落） | 既に解消（`DSLIntegrationTest.kt` 8 ケース、25.4 で +12 ケース追加） |
| Critical-3（iOS body 副作用） | 既に解消（`updateUIViewController` 経路へ移行済） |
| Major-A（Compose `Cell.cellID()` 仕様違反） | 既に解消（`DSLExplicitIdCell` sentinel パターン） |
| Minor-D（実機目視） | 継続 → 本レビュー Minor-H に引き継ぎ |
| Suggestion-A（DSLHintRegistry シングルトン） | 継続 → 本レビュー Suggestion-A に引き継ぎ |
| Suggestion-B（iOS/Compose の `.cellID` API 整合） | 25.1 で chain 形式正規化により書き味は並列化。ドキュメント反映は Minor-G に含めて指摘 |
| Suggestion-C（具象 Cell の Reidentifiable 実装保証） | 後続 `add-cell-types-*` レビュー時の checklist 事項として残す |

## アクションプラン

優先度順：

1. **[Minor-E]** iOS `DSLNodes.swift` / `CellModifiers.swift` のヘッダコメントを更新（Core 配置の事実に整合させる）— **archive 前に修正推奨**
2. **[Minor-F]** Compose `SectionHandle` / `CellHandle` クラスに `@SettingsRootDsl` annotation を付与（spec L173 との literal 一致）— **archive 前に修正推奨**
3. **[Minor-G]** `SectionModifiers.kt` の陳腐化コメントと `docs/declarative-dsl-guide.md` §4 の chain 形式正規化を反映 — **archive 前に修正推奨**
4. **[Minor-H]** 実機目視（22.2-22.6）の実施 — **archive 前のユーザー確認事項**
5. **[Suggestion-A]** 将来 TabView / SplitView 等の並列ユースケース顕在化時に `DSLHintRegistry` シングルトン解消
6. **[Suggestion-D]** Compose Root H/F の chain 化は後続提案検討余地

## 判定結果

**ステータス**: `APPROVED`

理由：

- Section 25 で要求された 6 タスク群（25.0 / 25.1 / 25.2 / 25.3 / 25.4 / 25.6）はすべて実装・テストでカバーされており、spec の Requirement / Scenario と整合している。
- 前回 APPROVED 後に Compose 側が iOS と完全並列な書き味（`Section("...") { LabelCell(...) }.sectionFooter("...")` / Cell 直置き / `forEach(items)` key 省略）を獲得し、本提案の中核価値「DSL 方式の書き味」がプラットフォーム間で大きく改善された。
- ビルド・テストは両 OS で全通過（Android Compose 含む / iOS 117 件）、退化（regression）なし。
- 残る指摘は **Minor 4 件 / Suggestion 2 件** で、いずれも **コードの動作には影響しない**（コメント陳腐化、annotation 不足、ドキュメント整理、実機確認、API 整合）。Minor のうち 3 件（E / F / G）は archive 前に必ず修正すべきだが、修正自体は局所的でリスクなし。
- 循環依存回避（25.0）は後続 `add-cell-types-*` 系の archive 順序に直接寄与する重要な改善であり、構造的に正しい配置になっている。
- `DSLHandleTest.kt` の「デフォルト id 値の Cell でも DSL 経路で id が rebind される」「位置移動を跨いだ cellID 安定性」テストは regression 検出力が高い良いテスト群であり、Section 25 の新 API が **設計通り動作することを直接観測** できる。

archive 前に Minor-E / F / G の修正と Minor-H（実機目視）を完了すれば、本提案は完全に archive 可能な品質に達する。
