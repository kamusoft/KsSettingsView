# レビュー結果 - purify-core-extract-style-to-ui-layer (iOS)

**レビュー日時**: 2026年06月07日
**レビュワー**: sdd-reviewer
**変更提案ID**: purify-core-extract-style-to-ui-layer
**スコープ**: iOS 側のみ（Android は別セッションで対応予定）

## サマリー

iOS 側の実装は、提案の核心目標である「Core 純化（`KsColor` / `KsFont` / `KsImage` / `Theme` / `CellStyle` の Core 削除）」「UI 層への再配置」「`SettingsRoot.theme` 削除」「`SettingsRootDiff.updateTheme` 削除」「`SettingsRootStore.applyTheme(_:)` 独立 API 化」をいずれも丁寧に達成しており、設計品質は総じて高い。Theme / CellStyle の Equatable 手動実装、`UIColor.isEqual(_:)` ベースの比較、`applyTheme` での同値抑制、`StateObject(wrappedValue:)` を用いた DSL Bookkeeper の初回限定構築など、SwiftUI モダン API のベストプラクティスに沿った実装も多く確認できる。

ビルド・テスト結果:
- `swift build`: 成功
- `swift test`: 83 件すべて成功
- `openspec validate purify-core-extract-style-to-ui-layer`: 成功

一方で以下の不整合・抜けが確認された:

1. **swiftui spec.md の DSL Modifier 一覧に `.icon(_ icon: KsImage)` が MUST として記載されているが、iOS 側に実装が存在しない（grep で `func icon` / `.icon(` の Cell modifier 定義はゼロ件）**。これは本提案で新たに「`.icon` の引数型は `KsSettingsViewUI` 所属の `KsImage` を受ける」と spec が明記しているにも関わらず、実装側に対応物がない状態。
2. **swiftui spec.md L88-91 の Requirement 「`DSLReidentifiable` / `DSLStyleModifiable` protocol の配置モジュール」は両 protocol を `KsSettingsViewCore` に置くと **MUST** で記述しているが、実装と tasks.md 6.17 は `DSLStyleModifiable` を `KsSettingsViewUI` に移動している**。tasks/実装側の方針が技術的に妥当（`CellStyle` が UI 層なので `DSLStyleModifiable` は Core に置けない）であるため、spec 側の Requirement 本文が修正漏れと判断できる。

これらは設計上の根本的破綻ではなく、修正範囲は限定的だが、spec と実装の食い違いは「OpenSpec の整合性」という本提案の主旨に直接かかわる箇所であるため修正必須として扱う。

**判定**: `CHANGES_REQUESTED`

## 指摘事項

### 🟠 Major-1: swiftui spec の `.icon` modifier MUST が実装されていない

**該当箇所**:
- spec: `openspec/changes/purify-core-extract-style-to-ui-layer/specs/settings-view-ios-swiftui/spec.md:82`
- 実装: `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift`（実装欠落）

**問題点**:
spec の "SwiftUI DSL" Requirement (L79-87) は Cell View modifier 群として以下を MUST 列挙している:

```
- `.titleColor(_ color: UIColor)`：タイトル色
- `.font(_ font: UIFont)`：フォント
- `.icon(_ icon: KsImage)`：アイコン（`KsImage` は `KsSettingsViewUI` 所属）
- `.cellHeight(_ height: CGFloat)`
- `.backgroundColor(_ color: UIColor)`
- `.disabled(_ flag: Bool)`
- `.cellID(_ id: AnyHashable)`
```

さらに L104-108 の Scenario 「.icon modifier の型」では「`.icon(_:)` は `KsImage`（`KsSettingsViewUI` 所属）を受ける。`KsSettingsViewCore` には `KsImage` が存在しないため、`import KsSettingsViewUI` が必要」と挙動を明記。

しかし `ios/Sources/KsSettingsViewSwiftUI/CellModifiers.swift` には `func icon` が定義されておらず、grep でも他ファイルに `.icon(` Cell modifier 実装は存在しない。`KsCell` extension としても `Self.icon(...)` を返すメソッドはなく、SwiftUI DSL で `LabelCell(title: "X").icon(.systemName("bell"))` と書くと **コンパイルエラー** になる。

なお、本提案の Sample 側（`BasicCellsDemoView.swift`）は `LabelCell(... , icon: KsImage.systemName(...))` のように **init 引数** で icon を直接渡しており、`.icon` modifier 経路を踏んでいないため、Sample は動作してしまうが spec 要件を満たしていない。

**推奨修正**:
以下のいずれかで対応する:

**案 A（推奨）**: `CellModifiers.swift` に `.icon(_:)` modifier を追加し、`CellStyle` 経路で持つか、または各具象 Cell に対するオーバーライド経路を新設する。`KsImage` は現状 `CellStyle` のフィールドにないため、もし modifier で icon を上書きしたい場合は具象 Cell 側に `withIcon(_:)` のような書き換え API を持たせ、SwiftUI extension からそれを呼ぶ実装になる。

```swift
// 例: KsSettingsViewSwiftUI/CellModifiers.swift
public protocol DSLIconModifiable: KsCell {
    func withIcon(_ icon: KsImage?) -> Self
}

extension KsCell {
    public func icon(_ icon: KsImage) -> Self {
        if let modifiable = self as? any DSLIconModifiable {
            if let rebuilt = applyIconIfMatching(modifiable, newIcon: icon) as? Self {
                return rebuilt
            }
        }
        return self
    }
}
```

そして `LabelCell` / `CommandCell` のみ `DSLIconModifiable` 準拠（`withIcon`）を実装する（他 Cell には icon フィールドがない）。

**案 B（暫定）**: spec の `.icon(_ icon: KsImage)` modifier 要求を削除する（spec 修正）。ただし spec の Scenario「.icon modifier の型」も同時に削除する必要がある。本提案の旨は「型の Native 化」であり、`.icon` modifier の有無自体は本提案範囲外として整理する判断もあり得る。

判断は本提案オーナーに委ねる。実装追加（案 A）が望ましいが、現行 Sample が init 引数経路を使うため、spec 側のスコープ縮小（案 B）でも整合性は取れる。

### 🟠 Major-2: swiftui spec の DSLStyleModifiable 配置 Requirement が実装と矛盾

**該当箇所**:
- spec: `openspec/changes/purify-core-extract-style-to-ui-layer/specs/settings-view-ios-swiftui/spec.md:88-91`
- 実装: `ios/Sources/KsSettingsViewUI/DSLStyleModifiable.swift`（UI 層配置）
- tasks: `openspec/changes/purify-core-extract-style-to-ui-layer/tasks.md:69, 166`（UI 層配置を明示）

**問題点**:
spec.md の SwiftUI DSL Requirement 本文 L88-91 は以下を **MUST** として規定している:

> - **`DSLReidentifiable` / `DSLStyleModifiable` protocol の配置モジュール**：
>   - これらの protocol は `KsSettingsViewCore` モジュールに定義しなければならない (MUST)
>   - 後続 `add-cell-types-*` 系で具象 Cell（`LabelCell` 等）が `KsSettingsViewUI` モジュールに配置されるため、`KsSettingsViewUI` の Cell が `DSLReidentifiable` を準拠できるよう、最下層 Core モジュールに置く

一方、tasks.md 6.17 / 6.17 注記、および実装は `DSLStyleModifiable` を `KsSettingsViewUI` に配置し、`DSLReidentifiable` のみ Core に残している。これは「`DSLStyleModifiable` が `CellStyle` を参照するが、本提案で `CellStyle` が Core から削除される」という技術的必然に基づく正しい判断だが、spec 本文の MUST 文言と矛盾する。

`DSLStyleModifiable.swift` の冒頭コメントにも「旧 `KsSettingsViewCore.DSLStyleModifiable` から UI 層に移動した（`CellStyle` の所属が Core → UI 層に変わったため）」と明記されており、実装側の判断は正当。

**推奨修正**:
spec.md L88-91 の Requirement 本文を以下のように修正する（**spec 修正**）:

```markdown
- **`DSLReidentifiable` / `DSLStyleModifiable` protocol の配置モジュール**：
  - `DSLReidentifiable` は `KsSettingsViewCore` モジュールに定義しなければならない (MUST)（`CellStyle` を参照しないため）。
  - `DSLStyleModifiable` は `KsSettingsViewUI` モジュールに定義しなければならない (MUST)（`CellStyle` が UI 層所属に変更されたため、Core から参照不可）。
  - `KsSettingsViewSwiftUI` モジュール内の DSL ロジック（`DSLNodes.swift` / `CellModifiers.swift` 等）は両モジュールを import して利用する。
```

この修正は本提案の Decision 2 / Decision 4 / tasks.md 6.17 と整合する。

### 🟡 Minor-1: Theme / CellStyle / KsImage の専用ユニットテストが存在しない

**該当箇所**:
- tasks: `openspec/changes/purify-core-extract-style-to-ui-layer/tasks.md:14-16` (Phase 2.6 / 2.7 / 2.8)
- spec: `openspec/changes/purify-core-extract-style-to-ui-layer/specs/settings-view-ios-style/spec.md:152-168` "Theme / CellStyle の Hashable / Equatable 契約"

**問題点**:
spec の Scenario「Theme の Equatable」「CellStyle の Equatable」「KsImage の Hashable 契約」は MUST レベルの挙動契約だが、専用テスト (`ThemeTests` / `CellStyleTests` / `KsImageTests`) は tasks.md 注記により「省略し SettingsRootStore / EffectiveStyle / BasicCells テストで間接的にカバーする」方針が選択されている。

確かに以下のテストで間接的にカバーされている:
- Theme の Equatable: `SettingsRootStoreTests.test_applyTheme_同値ならtheme通知を抑制する`（同値判定の正当性に依存）
- CellStyle の Equatable: `BasicCellsTests` の `Cell == Cell` 比較（各具象 Cell の `==` が `CellStyle ==` を呼ぶ）
- KsImage の Hashable: `LabelCellTests`（`icon: KsImage` を持つ LabelCell の `==` / hash を踏む）

ただし、**`UIColor.isEqual(_:)` を用いた手動 `==` が「明示的に異なる UIColor インスタンスでも `isEqual` で true 判定される」ケース**、**`uiImage` 派生の `ObjectIdentifier` ベース Hashable**、**`KsImage.systemName` vs `.uiImage` の cross-case 不等**などの境界条件を直接検証していない。回帰時の検出粒度が粗くなる。

**推奨修正**:
将来的な保守性のため、以下の最小テストを追加することを提案（必須ではない）:

```swift
// ios/Tests/KsSettingsViewUITests/ThemeEquatableTests.swift
final class ThemeEquatableTests: XCTestCase {
    func test_同値の_UIColor_インスタンスで_eq_が成立する() {
        let a = Theme(separatorColor: UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1))
        let b = Theme(separatorColor: UIColor(red: 0.5, green: 0.5, blue: 0.5, alpha: 1))
        XCTAssertEqual(a, b)  // UIColor.isEqual ベース
    }
    func test_異なる_UIColor_で_eq_が不成立() { ... }
}

// ios/Tests/KsSettingsViewUITests/KsImageHashableTests.swift
final class KsImageHashableTests: XCTestCase {
    func test_systemName_同値() { ... }
    func test_uiImage_同一インスタンスは等価_異なるインスタンスは不等() { ... }
    func test_systemName_vs_uiImage_cross_case_不等() { ... }
}
```

tasks.md 注記通り「間接カバーで足りる」という判断も妥当なため、本指摘は Minor。

### 🔵 Suggestion-1: Theme / CellStyle の `@unchecked Sendable` の妥当性メモを残す

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/Theme.swift:20`
- `ios/Sources/KsSettingsViewUI/CellStyle.swift:17`
- `ios/Sources/KsSettingsViewUI/KsImage.swift:25`

**問題点（懸念）**:
`Theme` / `CellStyle` / `KsImage` はそれぞれ `@unchecked Sendable` で宣言されている。これは `UIColor` / `UIFont` / `UIImage` が Swift 6.2 の strict concurrency では Sendable 適合していないため必要な対処だが、すべてのフィールドが `let` で immutable であり、UIKit の `UIColor` / `UIFont` / `UIImage` 自身は内部状態がほぼ immutable な事実上 thread-safe な型である点に依存している。

ただし、ユーザーが `UIImage(cgImage: mutableImage)` のような可変 backing を持つ `UIImage` インスタンスを `KsImage.uiImage(...)` に渡すと、`KsImage` を sendableに渡しても backing が変更され race condition を起こす可能性が理論上残る。

これは現実問題として発生しにくく、@unchecked Sendable で実用上問題ないが、後続のレビュアーや利用者のために短いコメントで前提を明記しておく方が安全。

**推奨修正**:
```swift
/// `@unchecked Sendable` の根拠:
/// - 全フィールドが `let`（immutable）。
/// - 構成要素の `UIColor` / `UIFont` は Apple 実装上、内部状態が事実上 immutable で thread-safe。
/// - `KsImage.uiImage` が保持する `UIImage` も、利用者が可変 backing を持たせない限り安全。
public struct Theme: Equatable, @unchecked Sendable { ... }
```

このコメントは spec の Scenario / 動作には影響しないため、推奨レベル。

### 🔵 Suggestion-2: tasks.md の完了条件チェックリスト整合

**該当箇所**: `openspec/changes/purify-core-extract-style-to-ui-layer/tasks.md:152-158`

**問題点（観察）**:
完了条件チェックボックスのうち、Android 関連が未チェックなのは想定通り（iOS 完了 → 停止方針）。`openspec validate` チェックは [x]、iOS の核心条件（Core 不在・SettingsRoot.theme 不在・KsCell.style 不在・SettingsRootDiff.updateTheme 不在）は [x] で正しい。

ただし「すべての Phase のチェックリストが完了している」「`./gradlew test` 全テスト緑」「Sample 7 種正常表示」が全体としての完了条件として `[ ]` のままなので、本提案を archive する際にはこれらが Android 完了とともに `[x]` にされる必要がある。本セッション（iOS 完了停止）では正しい状態。

**推奨修正**:
特に修正不要（観察事項として記録）。Android 着手時に上記が `[x]` 化される運用が想定通り。

## アクションプラン

優先度順:

1. **🟠 Major-2 を即修正**: `specs/settings-view-ios-swiftui/spec.md` L88-91 の `DSLReidentifiable` / `DSLStyleModifiable` 配置 Requirement を、`DSLStyleModifiable` が UI 層配置となるよう本文修正する。tasks.md / 実装と整合させる（実装変更不要）。
2. **🟠 Major-1 を判断・修正**: `.icon(_ icon: KsImage)` modifier を実装するか（案 A）、spec 側から削除するか（案 B）の判断を本提案オーナーに仰ぐ。Sample は init 引数経路を使うため最低限の動作には影響しないが、spec MUST が満たされていない状態は OpenSpec 規約上望ましくない。
3. **🟡 Minor-1（任意）**: Theme / CellStyle / KsImage 専用の Equatable / Hashable テストを追加（最小限）。本提案範囲外として後続で対応も可。
4. **🔵 Suggestion-1（任意）**: Theme / CellStyle / KsImage の `@unchecked Sendable` の根拠コメントを各ファイルに追記。
5. **🔵 Suggestion-2（観察）**: tasks.md の完了条件は Android 完了とともに `[x]` 化される運用で OK。

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

理由:
- Major-1（`.icon` modifier の spec ↔ 実装不整合）と Major-2（`DSLStyleModifiable` 配置の spec ↔ 実装矛盾）は、本提案の核心であるはずの「OpenSpec 仕様と実装の整合性」に直接関わる。
- いずれも修正範囲は spec 文言修正 or 小さな実装追加で済むため、修正後の再レビューは軽量。
- それ以外の核心実装（Core 純化、Theme / CellStyle / KsImage 再配置、`applyTheme` 独立 API、Sample の `UIColor` 直接構築化、テスト書き換え）は spec と整合し、品質も高い。
- iOS 側の swift build / swift test (83 件) は全緑であり、機能上のリグレッションは検出されていない。
- 本提案で Android 側修正が iOS Core 削除（物理ディレクトリ分離）の影響を受けないことも確認済み。
