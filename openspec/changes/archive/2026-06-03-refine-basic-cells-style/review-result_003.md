# レビュー結果 - refine-basic-cells-style (3 回目)

**レビュー日時**: 2026年06月03日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-style
**前回レビュー**: `review-result_002.md`（APPROVED, Minor-3 / Suggestion-1〜3 はスキップ宣言）
**今回スコープ**: 残課題（Minor-3 / Suggestion-1〜3）の修正対応の妥当性確認

---

## サマリー

前回 APPROVED 後にスキップ宣言されていた Minor-3 / Suggestion-1〜3 が、すべて適切に実装されていることを確認した。
特に Suggestion-1（案 B 採用：内部チェック表示 View へ disabled 描画を移譲）は、
`design.md` Decision 6「全体半透明化はしない」の精神と、
`specs/cell-types-basic/spec.md` および `specs/settings-view-*-ui/spec.md` の
MUST NOT 条項（Cell 全体への alpha 適用や半透明化禁止）の両方と整合する形で
コード上の意図を明確化する変更となっており、Verification Report の SUGGESTION-1 も解消した。

ビルド・テストも iOS / Android / Sample すべてで成功し、新たな回帰やコメント以外の `alpha = 0.5` 直接代入は検出されなかった。
本変更提案は実装完了として最終承認できる。

### 検証済み事項

- iOS `swift test`: **145 / 145 PASS**（KsSettingsViewCoreTests / KsSettingsViewUITests / KsSettingsViewSwiftUITests / KsSettingsViewSwiftUIBridgeTests 含む）
  - `BasicCellsTests` に内部 View への isEnabled 委譲を確認する追加テストが含まれていることを確認:
    - `test_CheckboxCellView_disabledは内部KsCheckBoxViewのisEnabledに委譲される`
    - `test_CheckboxCellView_isEnabled_trueでは内部KsCheckBoxViewもenabled`
    - `test_RadioCellView_disabledは内部KsCheckmarkAccessoryViewのisEnabledに委譲される`
    - `test_RadioCellView_isEnabled_trueでは内部Checkmarkもenabled`
    - `test_SimpleCheckCellView_disabledは内部KsCheckmarkAccessoryViewのisEnabledに委譲される`
    - `test_KsCheckBoxView_isEnabled_を切替できる`
    - `test_KsCheckmarkAccessoryView_isEnabled_でtint色アルファが下がる`
- Android `./gradlew :ks-settingsview-ui:test --rerun-tasks`: **BUILD SUCCESSFUL**
  - `BasicCellsTest` 54 件含む合計 151 件全 PASS, 0 failures, 0 errors
  - 内部 View への isEnabled 委譲確認テストを含む（`RadioCellViewHolder disabled は内部 KsSimpleCheckView の isEnabled に委譲される`、`SimpleCheckCellViewHolder disabled は内部 KsSimpleCheckView の isEnabled に委譲される`、`CheckboxCellViewHolder disabled は内部 MaterialCheckBox の isEnabled に委譲される`、`KsSimpleCheckView の isEnabled は setEnabled で切替できる`）
- iOS Sample `xcodebuild -scheme KsSettingsViewSample -destination 'generic/platform=iOS Simulator' build`: **BUILD SUCCEEDED**
- Android Sample `./gradlew :app:assembleDebug`: **BUILD SUCCESSFUL**
- ソースコード grep（`*.alpha = ... 0.5` 系の代入）で実 `.swift` / `.kt` 実装側に該当箇所なし。残存しているのは下記のみで仕様違反に該当しない:
  - コメント内の自己言及（`KsCheckBoxView.swift:28` の「`checkBoxView.alpha = 0.5` を直接書く必要がなくなり」、`KsSimpleCheckView.kt:29` の同等記述）
  - テスト用 internal getter（`_checkBoxViewAlpha` / `_checkmarkViewAlpha`。むしろ「コンテナ自身の alpha が 1.0 で維持されること」を検証するために 1.0 期待値で使われている）
- 仕様ファイル（proposal.md / design.md / specs/*.md / tasks.md）は無変更を確認

### 判定

**ステータス: `APPROVED`**

---

## 前回スキップ宣言指摘の対応確認

### Suggestion-1 + Minor-3: 内部チェック表示 View へ disabled 表現を移譲（案 B）

**該当箇所**:
- iOS: `KsCheckBoxView.swift:57-66, 102-123, 145-166`、`KsCheckmarkAccessoryView.swift:28-45, 92-114`
- iOS 呼び出し側: `CheckboxCellView.swift:77-80`、`RadioCellView.swift:68-72`、`SimpleCheckCellView.swift:67-70`
- Android: `KsSimpleCheckView.kt:62-69, 79-81, 104-121`
- Android 呼び出し側: `RadioCellViewHolder.kt:60-63`、`SimpleCheckCellViewHolder.kt:56-59`

**確認内容**:

1. **内部 View への移譲が完了**
   - iOS `KsCheckBoxView` に `var isEnabled: Bool = true` プロパティを追加し、didSet で `applyBorderColor()` / `updateFill()` / `setNeedsDisplay()` を発火。`effectiveAccentColor()` で disabled 時にアルファ 0.5 を乗算した枠線・塗り色を返し、`draw(_:)` 内のチェックマーク stroke も `isEnabled` で `.white` / `.white.withAlphaComponent(0.5)` を分岐。
   - iOS `KsCheckmarkAccessoryView` に `var isEnabled: Bool = true` プロパティを追加し、`applyTintColor()` で `imageView.tintColor = lastAccent` / `lastAccent.withAlphaComponent(0.5)` を切替。
   - Android `KsSimpleCheckView` で `View` 標準 `setEnabled(Boolean)` をオーバーライドし、変化時に `invalidate()`。`onDraw` 内で `paint.color = if (isEnabled) color else applyDisabledAlpha(color)` で分岐。アルファ低下は `DISABLED_ALPHA_FACTOR = 0.5f` を ARGB の α 成分のみに乗算する形で実装（RGB はそのまま、`Color.argb` で再構築）。

2. **呼び出し側からの `alpha = 0.5` 直接代入が全廃**
   - `CheckboxCellView.swift:80` は `checkBoxView.isEnabled = cb.isEnabled` のみ。前バージョンにあった `checkBoxView.alpha = ... 0.5` は撤去済み。
   - `RadioCellView.swift:72` は `checkmarkView.isEnabled = rc.isEnabled` のみ（Minor-3 で指摘された冗長三項表現は本対応で自然解消）。`apply(selected:accent:animated:)` 内の `applyTintColor()` で disabled 反映が再評価される実装順序になっている。
   - `SimpleCheckCellView.swift:70`、`RadioCellViewHolder.kt:63`、`SimpleCheckCellViewHolder.kt:59` も同様。
   - `prepareForReuse` / `reset()` でも `isEnabled = true` / `true` にリセットされ、reuse 時のクロストークも防止されている。

3. **コード意図がコメントで明示**
   - `KsCheckBoxView.swift:25-30` / `KsCheckmarkAccessoryView.swift:38-39` / `KsSimpleCheckView.kt:26-31` に「内部チェック表示の disabled 表現として alpha を使い、Cell 全体ではないことをコード上で明示」「design.md Decision 6 と整合」「`UIView` 標準には `isEnabled` がないため独自定義」が記載されている。
   - 各 Cell View の bind 部に「視覚的 disabled は内部 View に委譲する」「Cell コンテナの alpha は変更しない」とのコメントが整備され、後続メンテナンス時に方針がぶれにくくなっている。

4. **仕様との整合**
   - `specs/cell-types-basic/spec.md:78` および `specs/settings-view-ios-ui/spec.md:116` / `specs/settings-view-android-ui/spec.md:107` の MUST NOT 条項「Cell 全体への `alpha` 適用や半透明化は行ってはならない」は引き続き満たされる（Cell コンテナの `alpha` は触らない）。
   - `design.md` Decision 6 の「alpha 0.5 などの全体半透明化はしない」も、今回の改修で「内部 View 部品の描画色アルファ調整に局所化」されたことが、コード・コメント・テストで明確化された。
   - Verification Report `verification-report.md` の SUGGESTION-1（「アクセサリ View への alpha 0.5 適用を `KsCheckBoxView` / `KsCheckmarkView` の描画ロジック内に移譲して disabledTextColor 相当に置換する形に改める」）の趣旨に沿った実装になっている。RGB の置換ではなくアルファ低下を採っている点はあるが、これは「accent カラーを保ったままで視認上 disabled」とする design 意図の延長線にあり、spec 違反ではない。

5. **テストでの担保**
   - iOS: 「内部 View に委譲」「コンテナ alpha は 1.0 のまま」を 6 件のテストで明確に確認している（テスト用アクセサ `_isCheckBoxEnabled` / `_isCheckmarkEnabled` / `_checkBoxViewAlpha` / `_checkmarkViewAlpha` で双方向に検証）。
   - Android: 同等の確認を `BasicCellsTest` で 4 件追加（MaterialCheckBox 側は Android 標準の `isEnabled` 経路を使う旨も別途検証）。
   - 単純な「`isEnabled` プロパティが切替可能」だけでなく、`KsCheckmarkAccessoryView` の `tintColor` のアルファ値が disabled 時に 0.5 になるところまでテストされており、内部実装の挙動も保護されている。

**判定**: spec / design いずれにも整合する正しい対応。アーカイブ可能。

### Suggestion-2: `Theme.titleColor` 未定義に関するコメント追記

**該当箇所**: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:72-74`

**確認内容**:

```swift
// タイトル: Theme は `titleColor` / `titleFont` を直接保持していないため、現状はシステム既定にフォールバック
// 補足: `Theme.titleColor` は未定義のため `.label` フォールバック（refine-basic-cells-style Suggestion-2）。
// 将来 AiForms `Theme.CellTitleColor` 相当を `Theme` に追加する場合は、`cellStyle.titleColor ?? theme.titleColor ?? .label` の 3 段階に変更する。
let defaultTitleColor: UIColor = .label
```

- 「なぜ `.label` 固定なのか」「将来 `Theme.titleColor` を追加する場合の差し替え方」が明確になり、後続 change の伏線として `Suggestion-2` がコード上で残されている。
- 仕様変更は伴わないため scope 外の指摘として適切に処理されている。

**判定**: 適切な対応。

### Suggestion-3: `ButtonCellView` の baseColor 決定ロジックをヘルパに抽出

**該当箇所**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:107-126`

**確認内容**:

```swift
internal static func resolvedBaseColor(for cell: ButtonCell, effective: EffectiveStyle) -> UIColor {
    if let kc = cell.titleColor {
        return UIColor(ksColor: kc)
    }
    if cell.style.titleColor != nil {
        return effective.titleColor
    }
    return .systemBlue
}
```

- 3 段階優先順位（Cell 個別 `titleColor` → `CellStyle.titleColor` → `.systemBlue`）が `static` ヘルパに抽出され、`render(cell:theme:)` 側は `let baseColor = Self.resolvedBaseColor(for: btn, effective: effective)` と 1 行に圧縮。
- KDoc 相当のドキュメントコメントで「`effective.titleColor` は `CellStyle.titleColor` 未指定時に `.label` を返すため、Button 用には `.systemBlue` をデフォルトにしたい意図がある」ことが明示されており、ヘルパの存在意義が理解しやすい。
- 振る舞いの変更はなし。テストへの影響もなし。
- `static` 関数なので副作用がなく、`internal` 可視性で同モジュールのユニットテストから直接呼び出して優先順位を検証することも将来的に容易（今は実装変更のみで追加テストはなし、振る舞い不変のため許容）。

**判定**: 可読性が向上し、デグレもない。適切な対応。

### Minor-3: 冗長な三項表現の解消

**該当箇所**: `ios/Sources/KsSettingsViewUI/RadioCellView.swift:72`、`SimpleCheckCellView.swift:70`

**確認内容**:
- Suggestion-1 の対応（`checkmarkView.isEnabled = cell.isEnabled` への置換）により、`checkmarkView.alpha = rc.isEnabled ? checkmarkView.alpha : 0.5` の冗長な三項式自体が削除された。前回指摘の自己代入的な三項分岐は完全に解消。
- 残された alpha 関連 API（`_checkmarkViewAlpha`）は「コンテナ alpha が 1.0 のまま」を検証する目的の test-only getter であり、プロダクションコードでの `alpha = ...` 代入には該当しない。

**判定**: 完全解消。

---

## 新規指摘事項

新規 Critical / Major / Minor 指摘なし。

### 軽微な気付き（情報共有のみ・対応不要）

#### 🔵 Info-1: 内部 View の `isEnabled` 機構が iOS / Android で「独自プロパティ」と「`View.setEnabled` オーバーライド」と分かれている

**該当箇所**: `KsCheckBoxView.swift:59-66`、`KsCheckmarkAccessoryView.swift:40-45`、`KsSimpleCheckView.kt:62-69`

**問題点**:
- iOS `KsCheckBoxView` / `KsCheckmarkAccessoryView` はどちらも `UIView` を直接継承しており、`UIView` には `isEnabled` プロパティが存在しないため独自プロパティとして定義している（コメントで明示済み）。
- Android `KsSimpleCheckView` は `android.view.View` 標準の `setEnabled(enabled: Boolean)` をオーバーライドして再描画する経路に乗っている。
- どちらも「外から `view.isEnabled = bool` で操作できる」「変化時のみ再描画」という挙動は揃っているため、プラットフォーム別の API 慣習に沿った妥当な実装。`UIControl` 系を継承する選択肢もあるが、現状の必要性に対して過剰なので採用しない判断は合理的。

**対応**: 不要。情報共有のみ。

#### 🔵 Info-2: Suggestion-2 のコメントで言及されている「将来の `Theme.titleColor` 追加」は別 change

**該当箇所**: `EffectiveStyle.swift:73-74`

**問題点**:
- 本変更提案のスコープではないが、コメントで伏線が残されている。実際に AiForms `Theme.CellTitleColor` 互換のフィールドを追加するかは別途決定する必要がある（YAGNI を踏まえた判断）。

**対応**: 不要。後続 change で扱う。

---

## アクションプラン

新規対応事項なし。本変更提案は最終的に実装完了。次の工程として：

1. （済）`sdd-verify` の `verification-report.md` 上の SUGGESTION-1 は本回の改修で解消（必要に応じて `openspec verify` を再実行して再評価）
2. アーカイブ準備（`openspec/changes/refine-basic-cells-style/` → `openspec/changes/archive/` 配下に移動 / `openspec archive refine-basic-cells-style`）

---

## 判定結果

**ステータス**: `APPROVED`

- **✅ APPROVED**: Critical / Major / 必須 Minor の指摘なし。
- 前回 APPROVED 時にスキップ宣言された Minor-3 / Suggestion-1〜3 がすべて適切に対応された。Suggestion-1 は案 B（内部 View 移譲）で実装され、`design.md` Decision 6 と spec の MUST NOT 条項の両方と整合する形になり、Verification Report の SUGGESTION-1 も解消した。
- ビルド・テスト（iOS swift test 145 / 145、Android :ks-settingsview-ui:test 151 / 151、iOS Sample / Android Sample build）すべて成功。回帰なし。
- 仕様ファイル（proposal.md / design.md / specs/*.md / tasks.md）の改変なし。
- 本変更提案は次工程（検証・アーカイブ）に進めて問題ない。

---

## 参考: レビュー観点別カバレッジ確認（3 回目）

| 観点 | 前回 (002) | 今回 (003) |
|------|-----------|-----------|
| Major-1（iOS Sample RadioCell footer） | ✅ 修正済 | ✅ 維持 |
| Minor-1（Android 固定高さ時の minimumHeight） | ✅ 修正済 | ✅ 維持 |
| Minor-2（iOS viewBackgroundColor チラつき） | ✅ 修正済 | ✅ 維持 |
| Minor-3（冗長三項表現） | ⏭️ Skip | ✅ Suggestion-1 対応で自然解消 |
| Suggestion-1（内部 View 移譲 or コメント） | ⏭️ Skip | ✅ 案 B 採用、テスト付き |
| Suggestion-2（`Theme.titleColor` コメント） | ⏭️ Skip | ✅ コメント追記 |
| Suggestion-3（ButtonCellView baseColor ヘルパ） | ⏭️ Skip | ✅ ヘルパ抽出 |
| verification-report.md SUGGESTION-1 | ⚠️ 残 | ✅ 解消（案 B により内部 View 描画に局所化） |
| iOS swift test | ✅ 145/145 | ✅ 145/145（追加テスト 5 件含む） |
| Android :ks-settingsview-ui:test | ✅ | ✅ 151/151（追加テスト含む） |
| iOS Sample build | ✅ | ✅ |
| Android Sample build | ✅ | ✅ |
| プロジェクトメモリ整合 | ✅ | ✅ |
| spec MUST NOT（Cell 全体 alpha 禁止） | ✅ | ✅ より明示的に担保 |
