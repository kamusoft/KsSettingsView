# レビュー結果 - refine-basic-cells-style

**レビュー日時**: 2026年06月03日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-style

---

## サマリー

本 change は AiForms.Maui.SettingsView の見た目を再現するため、Theme / CellStyle / 各 Cell に多数のフィールドを追加し、iOS / Android 両プラットフォームのネイティブ描画ロジックを大幅に拡張する大規模な品質向上 change。`proposal.md` / `design.md` / `tasks.md` / 6 つの delta spec を精読した上で実装をレビューした結果、**全体として実装の質は非常に高く、Decision 1〜11 を忠実に再現** している。

### 検証済み事項

- iOS `swift test` → **145 / 145 PASS**
- Android `gradle test`（core / ui / compose）→ **BUILD SUCCESSFUL**
- iOS Sample `xcodebuild build` → **BUILD SUCCEEDED**
- Android Sample `:app:assembleDebug` → **BUILD SUCCESSFUL**
- Core 値型（Theme / CellStyle / CellTitleAlignment / 全 Cell の `isEnabled` / `ButtonCell.titleAlignment`）が両プラットフォームで spec MODIFIED / ADDED Requirements を満たす
- `EffectiveStyle` の `backgroundColor` / `accentColor` / `valueTextColor` / `valueTextFont` / `disabledTextColor` / `effectiveCellHeight` / `isFixedHeight` 合成が iOS / Android で一致
- `KsCellViewSupport` による `configurationUpdateHandler` ベースのタッチフィードバック（Decision 4）が iOS 全 Cell View にインストール済み
- 実効行高さ（固定 / 最低保証）が iOS は `heightAnchor` 制約のキャッシュ・差分更新、Android は `layoutParams.height` / `minimumHeight` 経路で適用
- Android `CheckboxCell` の `MaterialCheckBox` 置換 + padding 補正（Decision 5）
- `isEnabled` トグルが値型 equals に組み込まれ、`replaceCell`（reconfigure）経路を通る（Decision 7）
- `Theme.viewBackgroundColor` の反映が iOS（`UICollectionView.backgroundColor`）/ Android（`RecyclerView.setBackgroundColor`）双方で `viewDidLoad` / `setRootDirect` / `applyUpdateTheme` 経路に組み込まれている
- `ApplyDiff` の Theme 更新で全 Cell を `reconfigureItems`（iOS）/ `notifyDataSetChanged`（Android）で再 bind し新 Theme を反映
- プロジェクトメモリ（`Theme.Material3` 必須 / `onDrawOver` 罫線 / 複数セル一括更新）と整合
- 既存 `Cell.equals` への `isEnabled` 組込が `add-partial-update-core` の「差分検出は id 同一性のみ」規約と矛盾しない（replaceCell として処理）

### 判定

**ステータス: `CHANGES_REQUESTED`**

iOS Sample の **RadioCell セクションに Section footer が指定されておらず、`specs/samples-ios/spec.md` の MUST 条件「Section の `footer` テキストに `"You can select either TypeA or TypeB."` を指定」を満たしていない** という Major 不適合があるため、修正後再レビューを推奨する。それ以外は Minor / Suggestion レベルの軽微な指摘のみであり、いずれも spec 違反ではない。

---

## 指摘事項

### 🟠 Major

#### 🟠 Major-1: iOS Sample の RadioCell セクションに Section footer が指定されていない（spec 不適合）

**該当箇所**: `samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:131`

**問題点**:
`openspec/changes/refine-basic-cells-style/specs/samples-ios/spec.md` の Requirement「基本 Cell を含むデモ画面」の Section 構成 MUST 条件で、`RadioCell` 群について以下が明示されている：

> 4. `RadioCell` 群（`groupId = "type"`、`TypeA` / `TypeB` 2 件、Section の `footer` テキストに `"You can select either TypeA or TypeB."` を指定）

しかし実装は `Section("Type") { RadioCell(...) RadioCell(...) }` で footer 指定が欠落している。コメントには「Section(header:) のみのため description で代替表現する」と書かれているが、SwiftUI ラッパには既に `Section(_:footer:cells:)` イニシャライザ（`SectionBuilder.swift:111-119`）と `ksSection(_:footer:cells:)` トップレベル関数（同 85-93 行）が定義されており、`footer: String?` を直接渡せる。Android Sample 側は `Section(header = "Type", footer = "You can select either TypeA or TypeB.") { ... }`（`BasicCellsDemoScreen.kt:110-113`）で正しく実装されており、iOS だけ抜けている。

**推奨修正**:
`BasicCellsDemoView.swift` の RadioCell セクションを以下のように修正する（コメントも削除）：

```swift
// 4. RadioCell 群（TypeA / TypeB の 2 件、footer で説明文を表示）
Section("Type", footer: "You can select either TypeA or TypeB.") {
    RadioCell(
        title: "TypeA",
        groupId: "type",
        value: "TypeA",
        selectedValue: selectedType,
        onSelected: { v in
            selectedType = v
            lastTappedTitle = "Type → \(v)"
        }
    )
    RadioCell(
        title: "TypeB",
        groupId: "type",
        value: "TypeB",
        selectedValue: selectedType,
        onSelected: { v in
            selectedType = v
            lastTappedTitle = "Type → \(v)"
        }
    )
}
```

---

### 🟡 Minor

#### 🟡 Minor-1: Android `applyEffectiveHeight` で固定高さ時にも `minimumHeight` を上書きしている

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/LabelCellViewHolder.kt:346-349`

**問題点**:
spec（`specs/settings-view-android-ui/spec.md`「行高さ（RowHeight / HasUnevenRows）の適用」）の Scenario「固定高さ（HasUnevenRows = false）」は `container.layoutParams.height = effectiveHeightPx` のみを要求しており、`minimumHeight` の言及がない。一方「可変高さ（HasUnevenRows = true）」では `container.minimumHeight = effectiveHeightPx` を要求している。現状の `applyEffectiveHeight` は固定 / 可変いずれの場合も `minimumHeight = heightPx` を設定しており、固定高さモードでは意味が無い（実害もない）が、コード意図と spec の対応が分かりづらい。

**推奨修正**:
固定モードでは `minimumHeight` の書き込みをスキップする：

```kotlin
val targetMinHeight = if (effective.isFixedHeight) view.minimumHeight else heightPx
if (view.minimumHeight != targetMinHeight) {
    view.minimumHeight = targetMinHeight
    changed = true
}
```

または、現状のままでも実害はないため Suggestion 扱いで「固定高さ時は minimumHeight の指定は無意味（layoutParams.height で確定するため）だが安全側に倒している」とコメントを 1 行追加するだけでも可。

#### 🟡 Minor-2: iOS `KsSettingsViewController.loadView` で `cv.backgroundColor = .systemBackground` のハードコードが残存

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:212`

**問題点**:
`loadView` 内で `cv.backgroundColor = .systemBackground` を設定し、直後の `viewDidLoad` で `applyViewBackgroundColor(theme:)` が `Theme.viewBackgroundColor` を反映する。両者が呼ばれる順序上ユーザー目視には影響しないが、Theme 受け渡し前の極短時間に `.systemBackground` で表示される可能性がある。

**推奨修正**:
`loadView` 内で `cv.backgroundColor` の明示設定を削除（または `UIColor(ksColor: root.theme.viewBackgroundColor)` で初期化）。`view` 自体の背景はそのまま `.systemBackground` を維持。

```swift
let cv = UICollectionView(frame: .zero, collectionViewLayout: layout)
// Theme.viewBackgroundColor は viewDidLoad で applyViewBackgroundColor 経由で反映する
cv.backgroundColor = UIColor(ksColor: root.theme.viewBackgroundColor)
```

#### 🟡 Minor-3: iOS `RadioCellView` / `SimpleCheckCellView` の `checkmarkView.alpha = rc.isEnabled ? checkmarkView.alpha : 0.5` が冗長

**該当箇所**: `ios/Sources/KsSettingsViewUI/RadioCellView.swift:74`、`SimpleCheckCellView.swift:71`

**問題点**:
三項の `true` 分岐が `checkmarkView.alpha` 自身（変更なし）になっており、可読性を下げている。式の意図は「enabled なら現状維持、disabled なら 0.5」だが、`if !rc.isEnabled` ガードに展開した方が明示的。

**推奨修正**:
```swift
if !rc.isEnabled {
    checkmarkView.alpha = 0.5
}
```

---

### 🔵 Suggestion

#### 🔵 Suggestion-1: 内部コントロールに対する `alpha = 0.5` は spec の MUST NOT に抵触しないが、design 意図の明示があるとよい

**該当箇所**: `CheckboxCellView.swift:78`、`RadioCellView.swift:74`、`SimpleCheckCellView.swift:71`、`RadioCellViewHolder.kt:61`、`SimpleCheckCellViewHolder.kt:56`

**問題点**:
spec（`specs/settings-view-ios-ui/spec.md` / `specs/settings-view-android-ui/spec.md`）の "isEnabled 描画の反映" Requirement は「**Cell 全体への** alpha 適用や半透明化は行ってはならない (MUST NOT)」と規定している。現状の実装は「**内部のチェック表示 View（`KsCheckBoxView` / `KsCheckmarkAccessoryView` / `KsSimpleCheckView`）にのみ** `alpha = 0.5`」を適用しており Cell 全体ではないため **spec 違反ではない**。ただし `KsCheckBoxView` / `KsCheckmarkAccessoryView` は自前描画で UIKit 標準の disabled appearance を持たないため、視覚的 disabled を表現する代替手段として alpha を使う設計判断であることをコメントで明示するか、`KsCheckBoxView` 側に `isEnabled` 状態の描画を移譲することで「Cell 全体ではない」ことをコード上でより明確にできる。

**推奨修正**（任意）:
- 案 A: コメントを追加して「Cell 全体ではなく内部チェック表示の disabled 表現として alpha を使う」旨を明示する
- 案 B: `KsCheckBoxView` / `KsCheckmarkAccessoryView` 等に `isEnabled` プロパティを持たせ、内部で薄い描画に切り替える

#### 🔵 Suggestion-2: iOS `EffectiveStyle.titleColor` の Theme 補完経路が無いまま `.label` 固定

**該当箇所**: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:72-80`

**問題点**:
`Theme` には `titleColor` フィールドがないため、`CellStyle.titleColor == nil` のときシステム既定の `.label` にフォールバックしている。これは現行 spec と整合しているが、AiForms `Theme` の `CellTitleColor` 相当を Theme に追加する余地がある。本 change のスコープ外だが、後続 change の伏線として記載。

#### 🔵 Suggestion-3: iOS `ButtonCellView` の `baseColor` 決定ロジックが微妙に冗長

**該当箇所**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:64-71`

**問題点**:
```swift
if let kc = btn.titleColor {
    baseColor = UIColor(ksColor: kc)
} else if btn.style.titleColor != nil {
    baseColor = effective.titleColor
} else {
    baseColor = .systemBlue
}
```
`effective.titleColor` は `CellStyle.titleColor` を内部で解決済みなので、`btn.titleColor` 未指定時は `effective.titleColor` を直接使えば 1 段階で済む。ただし `effective.titleColor` は `CellStyle.titleColor == nil` のとき `.label` にフォールバックしているため、その場合に `.systemBlue` を採りたいために `style.titleColor != nil` で判定している。意図は分かるが、Cell 個別の `titleColor` / `CellStyle.titleColor` / システム既定 の 3 段階優先順位を 1 つのヘルパに抽出すると読みやすくなる。

---

## アクションプラン

優先度順：

1. **【必須・Major】** iOS Sample の RadioCell セクションに `footer: "You can select either TypeA or TypeB."` を追加し、`Section("Type", footer: ...)` の書式に書き換える（`samples/ios/KsSettingsViewSample/BasicCellsDemoView.swift:131-152`）。修正後、iOS Sample の再ビルドで動作確認。
2. （任意）Minor-1: `applyEffectiveHeight` で固定モード時の `minimumHeight` 上書きを止めるかコメントで意図を明示する。
3. （任意）Minor-2: `KsSettingsViewController.loadView` の `cv.backgroundColor` ハードコードを `Theme.viewBackgroundColor` ベースに書き換える。
4. （任意）Minor-3: `RadioCellView` / `SimpleCheckCellView` の三項表現を `if !isEnabled { alpha = 0.5 }` に整理する。
5. （任意）Suggestion 群はメモ程度。

---

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

- **❌ CHANGES_REQUESTED**: Major-1（iOS Sample の Section footer 仕様未充足）が存在する。MUST 条件違反のため修正必須。それ以外は Minor / Suggestion のみで重大な実装欠陥はない。
- Major-1 の修正は 1 行レベル（`Section("Type")` → `Section("Type", footer: "...")`）で完了する。orchestrator 側の判断でその場で修正依頼が可能。
- 修正後の再レビュー対象は当該 1 箇所のみ。テスト・ビルドへの影響は最小。

---

## 参考: 仕様カバレッジ確認サマリー

| 観点 | 確認結果 |
|------|----------|
| proposal.md "What Changes" 全項目 | ✅ 実装済 |
| design.md Decision 1（Theme.rowHeight / hasUnevenRows） | ✅ 実装済 |
| Decision 2（Theme 既定値はニュートラル維持） | ✅ 維持 |
| Decision 3（実効高さ合成） | ✅ 実装済 |
| Decision 4（iOS configurationUpdateHandler） | ✅ 実装済 |
| Decision 5（Android MaterialCheckBox 置換） | ✅ 実装済 |
| Decision 6（isEnabled = コントロール disabled + テキスト色置換、Cell 全体 alpha なし） | ✅ 実装済 |
| Decision 7（Cell.equals に isEnabled / replaceCell 経路） | ✅ 実装済 |
| Decision 8（ButtonCell.titleAlignment） | ✅ 実装済 |
| Decision 9（既存引数末尾追加 + デフォルト値） | ✅ 維持 |
| Decision 10（Sample 全文置換） | ✅ 実施済（ただし iOS 側 RadioCell footer 漏れあり） |
| Decision 11（Android Checkbox Requirement 追加） | ✅ 実装済 |
| tasks.md Phase 1-16 全タスク [x] | ✅ 全てチェック済 |
| iOS swift test 全 PASS | ✅ 145/145 |
| Android gradle test 全 PASS | ✅ |
| iOS sample build | ✅ |
| Android sample build | ✅ |
| プロジェクトメモリ整合（Material3 / onDrawOver / バッチ更新 / Sample Cell 置換方針） | ✅ |
