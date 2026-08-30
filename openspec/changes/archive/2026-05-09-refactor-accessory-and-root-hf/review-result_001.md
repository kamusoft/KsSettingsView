# レビュー結果 - refactor-accessory-and-root-hf

**レビュー日時**: 2026年05月08日
**レビュワー**: sdd-reviewer
**変更提案ID**: refactor-accessory-and-root-hf

## サマリー

本変更提案は「`SectionAccessory` から Cell 概念を排除（`.custom(AnyCell)` → `.view(KsAnyView)`）」「`SettingsRoot` に Root H/F（`header` / `footer: RootAccessory?`）を追加」「`KsAnyView` 型消去ラッパを `settings-view-core` に新設」を Core 層に対して実装するものである。

レビューを実施した結果、以下の通り評価する：

- **proposal.md / design.md / specs/spec.md / tasks.md との整合性**: 要件・Decision・Scenarios のすべてが Core 層のドメインモデル（iOS Swift / Android Kotlin）で正しく実装されており、整合している。
- **タスクの達成状況**: tasks.md の全 28 タスク（1.1〜1.10、2.1〜2.8、3.1〜3.8、4.1〜4.2）が [x] でマークされ、内容も実装と一致している。
- **ビルド**: iOS（`swift build`）・Android（`./gradlew :ks-settingsview-core:assemble`）ともに BUILD SUCCESSFUL。
- **テスト**: iOS 46 tests / Android 46 tests（5+8+7+4+10+7+5）、両プラットフォームとも全件成功。0 failures / 0 errors。
- **openspec validate --strict**: 本提案・関連 in-progress 4 提案（ios-ui / android-ui / maui-bindings / cell-types-custom）すべて成功。
- **iOS / Android 整合性**: `KsAnyView` の二択 backing 戦略、`RootAccessory` / `SectionAccessory` の `view` ケース等価性扱い、`SettingsRoot` / `Section` の hash/equals 設計、ともにプラットフォーム差異の翻訳が一貫している。

設計判断は破壊的変更を含むが、Decision 1〜7 の根拠が明示され、影響範囲（in-progress 提案 4 件のアーティファクト）も探索段階で同期書き換え済み。Decision 3（`KsAnyView` の差分検出非対応）の意図は実装・テストともに明確に表現されている。

主要な指摘は Minor / Suggestion のみで、コメント記述の古さやテストの安定性に関する小さな改善余地に留まる。Critical / Major レベルの指摘はない。

**判定**: `APPROVED`

## 指摘事項

### 🟡 Minor

#### 🟡 Minor 1: 旧 `AnyCell` 参照のコメントが残存している

**該当箇所**:
- `ios/Sources/KsSettingsViewCore/KsCell.swift:14`
- `ios/Tests/KsSettingsViewCoreTests/DummyCells.swift:5`

**問題点**:
本提案で `AnyCell` 型は削除（tasks 1.10）されたが、以下の記述が更新されていない:

- `KsCell.swift:14`:
  ```swift
  /// `AnyCell.init` 等の利用側で改めて where 句を書く必要がなくなる。
  ```
  `AnyCell.init` はもう存在しない。

- `DummyCells.swift:5`:
  ```swift
  // AnyCell の異種コレクション挙動を検証するためにテスト内でのみ定義する。
  ```
  検証対象は現在 `[any KsCell]` のヘテロ配列であり、コメントが旧設計を指している。

これらは実装上の誤りではないが、コメントの整合性が損なわれており、今後コードを読む保守者が混乱する可能性がある。

**推奨修正**:

`KsCell.swift:14`:
```swift
// 例:
/// `[any KsCell]` の利用側（Section.cells など）で改めて where 句を書く必要がなくなる。
```

`DummyCells.swift:5`:
```swift
// 例:
// `[any KsCell]` の異種コレクション挙動を検証するためにテスト内でのみ定義する。
```

### 🔵 Suggestion

#### 🔵 Suggestion 1: `KsAnyViewTest.compose_hashcode_is_reference_identity` の不安定アサーション

**該当箇所**: `android/ks-settingsview-core/src/test/kotlin/jp/kamusoft/kssettingsview/core/KsAnyViewTest.kt:78`

**問題点**:
```kotlin
// 別インスタンスの hashCode は通常一致しない（identityHashCode が衝突する可能性は
// 極めて低いが理論的にはありうるため、`assertNotEquals` を強い保証として使う）
assertNotEquals(a.hashCode(), b.hashCode())
```

`Object.hashCode()` のデフォルト実装は `System.identityHashCode` ベースであり、別インスタンス同士でも理論上衝突しうる。コメントで「極めて低い」と免罪しているが、CI 上で稀にフレーキーになる可能性が皆無ではない。

「`equals` / `hashCode` を独自実装していない」ことを保証する目的なら、`KsAnyView` クラスのメソッドが `Any` から override されていないことを反射的に確認する方が安定する。

**推奨修正**（提案、必須ではない）:

```kotlin
@Test
@DisplayName("Compose クラスは equals / hashCode を override していない（Any デフォルト）")
fun compose_does_not_override_equals_or_hashcode() {
    val composeMethods = KsAnyView.Compose::class.java.declaredMethods
    // 自前で declaredMethods に含まれない（つまり Any から継承）ことを確認
    val overridden = composeMethods.map { it.name }.toSet()
    assertTrue("equals" !in overridden, "Compose は equals を override してはならない")
    assertTrue("hashCode" !in overridden, "Compose は hashCode を override してはならない")
}
```

ただし現行の `assertNotEquals(a.hashCode(), b.hashCode())` も実用上はほぼ問題なく、コメントで免罪している点も評価できるため、現状維持でも APPROVED の判定は変わらない。

#### 🔵 Suggestion 2: Android `SettingsRoot` / `Section` を `data class` 化して標準実装に寄せる選択肢

**該当箇所**:
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/SettingsRoot.kt`
- `android/ks-settingsview-core/src/main/kotlin/jp/kamusoft/kssettingsview/core/Section.kt`

**問題点**:
`SettingsRoot` / `Section` ともに `class` + 手動 `equals` / `hashCode` / `toString` を実装している。`SectionAccessory.View` / `RootAccessory.View` 側で「クラス一致のみで等価」を担保しているため、`SettingsRoot` / `Section` を `data class` 化しても Decision 3 の方針を満たせる（`data class` の自動 equals は各フィールドの equals を呼ぶだけ）。

Kotlin の慣習的書き方としては `data class` の方が定型コードを抑えられる。ただし、明示的な手動実装は意図の見える化にもなるため、現行のままでも問題はない（spec の「Hashable / equals 契約」は満たしている）。

**推奨修正**: 提案レベルの改善案。本マージとは別タイミングで検討して良い。

```kotlin
data class SettingsRoot(
    val sections: List<Section> = emptyList(),
    val theme: Theme = Theme(),
    val header: RootAccessory? = null,
    val footer: RootAccessory? = null,
)
```

### 🔵 Suggestion 3: `pinToVisibleBounds` の Open Question 対応の所在

**該当箇所**: `openspec/changes/refactor-accessory-and-root-hf/design.md:146`

**問題点**:
design.md の Open Questions に「`pinToVisibleBounds` をオプション化する API の形（`SettingsRoot.headerPinned: Bool?` / 各プラットフォームの style 引数で切り替え）」が残っている。本提案は Core 層のみのスコープで `pinToVisibleBounds = false` 固定（design Decision 4）として実装する明確な方針を取っているため、本提案範囲では問題なし。ただし、後続の iOS UI / Android UI 提案で再度この決定を取り上げ、最終結論を `add-settings-view-ios-ui` / `add-settings-view-android-ui` に明記する必要がある。

**推奨修正**: 後続提案レビュー時に Open Questions の決着を確認する。本提案範囲ではアクション不要。

## アクションプラン

優先度順:

1. （Minor 1）`KsCell.swift:14` / `DummyCells.swift:5` の旧 `AnyCell` 参照コメントを `[any KsCell]` ベースの記述に更新する。**マージ前の対応を推奨するが、blocker ではない。**
2. （Suggestion 1）Android `KsAnyViewTest.compose_hashcode_is_reference_identity` を `declaredMethods` ベースの安定アサーションに置き換える検討。後続改善で対応可。
3. （Suggestion 2）Kotlin `SettingsRoot` / `Section` の `data class` 化検討。後続改善で対応可。
4. （Suggestion 3）後続の iOS UI / Android UI 提案レビュー時に `pinToVisibleBounds` の Open Question 決着を確認。

## 判定結果

**ステータス**: `APPROVED`

**根拠**:
- proposal / design / spec / tasks のすべての要件・Decision・Scenario が iOS Swift / Android Kotlin の実装に正しく反映されている。
- ビルドが iOS / Android 両プラットフォームで成功している。
- テストが iOS 46 件 / Android 46 件すべて成功している。
- `openspec validate --strict` が本提案および関連 4 提案すべてで成功している。
- 破壊的変更（`SectionAccessory.custom(AnyCell)` 削除、`AnyCell` 削除）の影響範囲は探索段階で同期書き換え済み。
- 指摘事項はすべて Minor / Suggestion レベルで、Critical / Major はない。
- スキル参照: 本提案は OpenSpec の仕様駆動開発フローに沿って進行しており、`sdd-spec-review-skill` の規約に準拠している。

**マージ前推奨対応**:
- Minor 1（コメント整合性）の修正は推奨するが、blocker ではない。後続コミットで対応しても問題なし。
