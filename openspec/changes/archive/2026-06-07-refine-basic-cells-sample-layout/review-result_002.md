# レビュー結果 - refine-basic-cells-sample-layout (Phase 14 追加分)

**レビュー日時**: 2026年06月06日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-sample-layout
**対象範囲**: Phase 14（オーナー追加指摘 9 項目 + 罫線インセット方針変更）

---

## サマリー

### 評価概要

オーナー実機レビューに基づく Phase 14 の追加修正（9 項目 + 罫線インセット方針変更）について、仕様（proposal.md / design.md / 6 つの delta spec / tasks.md）と実装（iOS / Android）の整合性を網羅的に検証した。

**確認結果**：

1. **仕様の更新**: iOS 罫線インセットの方針変更（アイコン有り 52pt → 固定 16pt）は spec.md 上で適切に「アイコン有無に関わらず固定 16pt」と書き換えられており、Rationale で AiForms オリジナル参照の経緯も明記されている。
2. **iOS 実装**:
   - `titleLeadingPosition(for:)` は常に 16 を返す固定実装に簡素化済み（`KsSettingsViewController.swift:482-487`）
   - `separatorConfiguration(for:base:)` のセクション境界（端から端）と中間 Cell（16pt）の分岐は spec と一致
   - Header / Footer の不要余白削減（`directionalLayoutMargins = (2, 16, 2, 16)`）が `applyAccessoryToListCell` に実装済み
   - LabelCellView の description + valueText 並列描画（`subtitleCell + UICellAccessory.customView(.trailing())`）が実装され、4 パターン（両方/description のみ/valueText のみ/どちらもなし）が `switch` で網羅されている
   - Footer 文字色は呼び出し側で `theme.footerTextColor` を渡す経路に整理されている
3. **Android 実装**:
   - `SwitchCellViewHolder` の `trackTintList` を状態別 ColorStateList に変更済み（accent / outline）
   - `ClassicSectionDecoration.onDrawOver` でセクション最初 Cell の上端罫線を追加描画する `isSectionTop` 判定を追加
   - `separatorThicknessPx = 1.0f` の hairline 固定に変更済み
4. **Sample**:
   - iOS / Android の Cell タイプ別 7 セクションのテキストは一字一句一致（Notification の description / Agree to Terms 等含む）
   - `headerHeight = 40` の明示指定が CommandCell セクションに付与され、DSL（SectionBuilder / DSLScope）にも末尾 default 引数が追加されている
5. **テスト**:
   - iOS swift test: **154 passed**（titleLeadingPosition の固定 16pt 検証、混在セクションの固定 16pt 検証、separatorConfiguration の境界条件、footerTextColor の経路、LabelCellView の 4 パターンを網羅）
   - Android `:ks-settingsview-core/ui/compose:test`: **BUILD SUCCESSFUL**（trackTintList の状態別検証、thumbTintList の状態別検証を含む）
6. **`openspec validate refine-basic-cells-sample-layout --strict`**: valid

### 主要な所見（要対応）

- **🟠 Major-1**: iOS の Footer 文字色フォールバック規則が spec の `UIColor.secondaryLabel 相当のグレー` ではなく、`Theme.defaultFooterTextColor`（RGB 固定値 #6D6D72 相当）に依存している。spec に書かれた MUST 要件（「指定されていない（既定）場合、フォールバック色として `UIColor.secondaryLabel` 相当のグレーを使用しなければならない」）と実装の意味論が一致しない。
- **🟡 Minor-1**: ユーザーが指摘された通り、Android の罫線（セクション内 Cell 間）は `parent.paddingLeft` から端から端で描画されており、iOS の 16pt 固定インセットとクロスプラットフォームで一致していない。ただし Android 側 spec ではセクション内 Cell 間のインセット規則が明示されていないため、厳密には仕様違反ではない。AiForms オリジナルの Android スクリーンショットに 16pt 程度のインセットが見える点との突き合わせが望まれる。
- **🔵 Suggestion** 2 件（後述）

### 判定

**CHANGES_REQUESTED**

Major-1 について、現状の実装で「既定 Theme + Footer 表示」のときに secondaryLabel ではなく `#6D6D72` 固定色（ダークモード非対応）が使われることを許容するか、または spec の文言通り `UIColor.secondaryLabel` を返すロジックを iOS UI 側に追加するか、オーナー判断が必要。判定を `NEEDS_DISCUSSION` ではなく `CHANGES_REQUESTED` としているのは、spec の MUST 要件と実装が乖離している事実（仕様未充足）が明らかなため。

仮に Major-1 を「spec の表現を緩めて defaultFooterTextColor フォールバックでよい」とユーザーが判断する場合は、`spec.md` の Phase 14.3 Requirement の文言を「`Theme.defaultFooterTextColor` 相当（おおよそ secondaryLabel 色 #6D6D72）」と書き換える対応に切り替えても良い（その場合 APPROVED 相当）。

---

## 指摘事項

### 🟠 Major-1: iOS Footer 文字色フォールバックが secondaryLabel ではない

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:685` および `ios/Sources/KsSettingsViewCore/Theme.swift:147`

**問題点**:

`openspec/changes/refine-basic-cells-sample-layout/specs/settings-view-ios-ui/spec.md` の「Section Footer の文字色フォールバック」Requirement は次の MUST を含む。

> `Theme.footerTextColor` が指定されていない（既定）場合、フォールバック色として `UIColor.secondaryLabel` 相当のグレーを使用しなければならない (MUST)。

しかし `Theme.footerTextColor` は `KsColor` 非 Optional 型であり、既定値は `defaultFooterTextColor = KsColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)`（おおよそ `#6D6D72` の固定 RGB）である。実装は次のように、theme の値をそのまま使うだけで `secondaryLabel` への分岐がない。

```swift
// KsSettingsViewController.swift:684-687
textColor: isFooter
    ? UIColor(ksColor: root.theme.footerTextColor)
    : UIColor(ksColor: root.theme.headerTextColor)
```

問題は次の 2 点：

1. **ダークモード対応の欠落**: `UIColor.secondaryLabel` は dynamic color（light/dark 両対応）で、`#6D6D72` 固定 RGB はダークモードで読みにくくなる。iOS 17+ のシステム設定の見た目との一貫性が損なわれる。
2. **spec と実装の意味論差**: spec 上は「指定されていない場合は secondaryLabel」と書かれているが、実装では「指定されていなくても固定 RGB」になっており、Scenario 1（「footerTextColor 未指定時のグレーフォールバック」: 「Footer ラベルの `textColor` は `UIColor.secondaryLabel` 相当のグレーで描画される」）の検証ができない（実装は `UIColor.secondaryLabel` を一切返さない）。

また `SectionAccessoryRenderingTests.test_Footerの文字色はfooterTextColorが使われる` も「明示指定された色が使われる」ことを検証しているだけで、未指定時の `secondaryLabel` フォールバックは検証されていない。

**推奨修正**:

選択肢は 2 つ。

- **Option A（spec 通り実装）**: `Theme.footerTextColor` を Optional（`KsColor?`）化するか、または「`defaultFooterTextColor`（センチネル）」と比較するロジックを iOS UI 側に追加し、未指定時は `UIColor.secondaryLabel` を返すようにする。例：

  ```swift
  // KsSettingsViewController.swift sectionAccessoryView 内
  let footerColorUI: UIColor
  if root.theme.footerTextColor == Theme.defaultFooterTextColor {
      // Phase 14.3: 既定値の場合は dynamic な secondaryLabel をフォールバックとして使う。
      footerColorUI = .secondaryLabel
  } else {
      // 明示指定された値を優先する。
      footerColorUI = UIColor(ksColor: root.theme.footerTextColor)
  }
  ```

  ただし `Theme.defaultFooterTextColor` が外部から指定された場合と本当の「未指定」が区別できないという問題があるため、より厳密には `Theme.footerTextColor: KsColor?`（Optional 化）の方が望ましい。既存 API 互換性は init の default 引数を `nil` にすれば維持される。

- **Option B（spec を実装に揃える）**: `spec.md` の Phase 14.3 Requirement と Scenario 1 を `Theme.defaultFooterTextColor`（`#6D6D72` 相当のグレー）を返すように書き換える。AiForms オリジナル `UIColor.Gray` も dynamic ではなく固定 RGB なので、移植の趣旨としては Option B でも筋は通る。

オーナーの意図（クロスプラットフォーム + AiForms 移植）を踏まえると、**Option B の方が破壊変更が無く採用しやすい**と判断する。Option B を採る場合：

- spec.md の Requirement 文言を「`Theme.footerTextColor` の既定値 `defaultFooterTextColor`（`#6D6D72` 相当）はもともと secondaryLabel に近いグレーである。iOS UI 層は theme.footerTextColor をそのまま使えばよい」に書き換える
- Scenario 1 を「既定 Theme の場合は `KsColor(red: 0.43, green: 0.43, blue: 0.45, alpha: 1.0)` 相当のグレーで描画される」に書き換える
- 既存実装の修正は不要

---

### 🟡 Minor-1: Android セクション内 Cell 間の罫線インセットが iOS と非対称

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ClassicSectionDecoration.kt:84-94`

**問題点**:

iOS 実装は `separatorConfiguration(for:base:)` でセクション内中間 Cell の bottom separator を `leading = 16pt`（固定 16pt インセット）で描画する。これは spec で MUST。

Android 実装は `ClassicSectionDecoration.onDrawOver` で全 Cell の下端罫線を `parent.paddingLeft` から `parent.width - parent.paddingRight` まで（つまり端から端で）描画する。セクション最初 Cell の上端も同様に端から端。

両者でセクション内中間 Cell の罫線位置が異なる：iOS は 16pt インセット、Android は 0pt（端から端）。

ユーザーから「ユーザーが指摘」されている通り、AiForms オリジナルのスクリーンショットでは Android の Cell 間罫線にも 16pt 程度のインセットが見える可能性がある。**ただし、`settings-view-android-ui/spec.md` の「セクション罫線の描画位置と太さ」Requirement には Cell 間のインセット規則が明示されていない**（「セクション内 Cell 間 → 罫線を描画する (MUST)」とだけ書かれ、leading 値の指定がない）。そのため、現在の Android 実装は spec 違反ではない。

しかし以下の懸念がある：

1. **クロスプラットフォーム視覚一貫性**: iOS と Android で見た目が異なる（iOS は中間 Cell の罫線にインセット、Android はインセット無し）。仕様駆動開発の趣旨では UI ライブラリのクロスプラットフォーム整合性が重視される。
2. **AiForms オリジナルのスクリーンショット**: ユーザーが提示した AiForms オリジナルでは Android 側にも 16pt 相当のインセットが見えるとの指摘があり、iOS 14.1 の方針変更（アイコン有無関わらず固定 16pt）と整合させるには Android も同じ 16pt インセットにすべき可能性がある。

**推奨修正（採用可否は議論余地あり）**:

採用する場合、`ClassicSectionDecoration.onDrawOver` で「中間 Cell（`prevItem is CellListItem.CellRow && prevItem.sectionId == item.sectionId` かつ次 item も同一 section）」と判定したときの下端罫線の `left` 値を、`parent.paddingLeft + density.toPx(16)`（dp 換算した 16pt 相当）にする。セクション境界（最初 / 最後）の罫線は引き続き端から端を維持。

```kotlin
// ClassicSectionDecoration.kt onDrawOver 内（一例）
val nextItem = bindingAdapter.currentList.getOrNull(pos + 1)
val isSectionBottom = nextItem !is CellListItem.CellRow ||
    nextItem.sectionId != item.sectionId
val isSectionTop = prevItem !is CellListItem.CellRow ||
    prevItem.sectionId != item.sectionId

// 中間 Cell（セクション内中間） → 下端罫線は 16dp 相当インセット
// セクション最後 Cell → 下端罫線は端から端
val density = parent.resources.displayMetrics.density
val insetLeft = if (isSectionBottom) {
    parent.paddingLeft.toFloat()
} else {
    parent.paddingLeft.toFloat() + 16f * density
}
c.drawRect(insetLeft, bottom - separatorThicknessPx, right, bottom, paint)
```

同時に、`spec.md` の Android 側 「セクション罫線の描画位置と太さ」Requirement に次を追記する：

> - セクション内中間 Cell の下端 → `leading = 16dp` 相当のインセット（iOS と視覚的に揃える）

優先度: 低〜中（CHANGES_REQUESTED の必須項目ではない）。

**判断推奨**: 採用する。ただし spec の追記が伴うため、オーナーの最終判断を仰ぐ。Phase 14 の趣旨が「AiForms オリジナルに揃える」であれば、AiForms 実機 / スクリーンショットでの確認後に判断。

---

### 🔵 Suggestion-1: Phase 14.5 Track 色のオフ時グレーが spec の `Color.Argb(76, 117, 117, 117)` 相当ではない

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SwitchCellViewHolder.kt:87-93`

**問題点**:

`spec.md` Phase 14.5 の Requirement では：

> `state_checked = false` → 中間グレー（`Material colorOutline` 相当。AiForms オリジナルでは `Color.Argb(76, 117, 117, 117)` 相当のグレー透過）

実装は `MaterialColors.getColor(switchView, com.google.android.material.R.attr.colorOutline, Color.GRAY)` で `colorOutline` を解決して使用しているのみで、アルファ 76（30% 不透明）を乗せる処理は無い。

これは AiForms オリジナルの `Color.Argb(76, 117, 117, 117)`（α=0x4C=76 の半透明グレー）と完全には一致しないが、`colorOutline` のシステム既定値がそもそも半透明気味のため、視覚的な大差は出ない可能性がある。

ただし、ダークモード時に `colorOutline` が明るすぎる場合は AiForms の「アルファ 76 のグレー透過」より目立つことがある。Material 3 のテーマトークンを利用する設計上、`colorOutline` 単独で十分かどうかは AiForms オリジナル実機スクリーンショットとの比較が必要。

**推奨修正**:

優先度: 低。spec は「相当」と書かれており、`colorOutline` の解決値で意味論的には spec 準拠と判断できる。実機目視確認（13.2）で「オフ時の Track が目立たないグレーであること」を確認すれば足りる。気になる場合は、`ColorUtils.setAlphaComponent(outlineColor, 76)` でアルファを乗せて AiForms 互換にする選択肢もある。

---

### 🔵 Suggestion-2: Android 上端罫線描画の判定は ConcatAdapter Root H/F に対しても堅牢か

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/ClassicSectionDecoration.kt:97-104`

**問題点**:

セクション最初 Cell の判定は次のロジック：

```kotlin
val prevItem = bindingAdapter.currentList.getOrNull(pos - 1)
val isSectionTop = prevItem !is CellListItem.CellRow ||
    prevItem.sectionId != item.sectionId
```

これは `KsSettingsListAdapter.currentList` 内の前要素を見るため、Root Header（別 adapter）配下の状態は判定に入らない。これ自体は意図通りだが、`pos == 0` のときも上端罫線を描画することになる。

- ケース1: Root Header があり、その直下に最初の Section の CellRow → 上端罫線が描画される（OK、AiForms 互換）
- ケース2: Root Header が無く、画面トップに最初の Section の CellRow → 上端罫線が描画される（OK）
- ケース3: Section Header が存在（メインアダプタ内 `CellListItem.Header` か等価な行種）と、その直後に CellRow → 上端罫線が描画される（OK：Section Header と CellRow の境界に罫線が出る）

挙動は妥当だが、`CellListItem` の派生（`CellRow` 以外）が将来増えた場合、`prevItem !is CellListItem.CellRow` の判定が広すぎる可能性がある。たとえば将来 `CellListItem.Separator` のような種別が追加された場合、セパレータの上下両方に罫線が引かれる可能性がある。

**推奨修正**:

優先度: 低。現状の `CellListItem` 派生だと問題なし。将来の拡張時に再点検する旨をコメントで残しておくと良い。

```kotlin
// 直前 item が「同一 sectionId の CellRow」でなければセクション境界とみなす。
// 将来 CellListItem の派生が増えた場合（Separator 等）、ここの分岐に追加判定が必要になる
// 可能性がある。
val isSectionTop = prevItem !is CellListItem.CellRow ||
    prevItem.sectionId != item.sectionId
```

---

## アクションプラン

優先度順：

1. **🟠 Major-1（要対応）**: iOS Footer 文字色フォールバックの spec ⇔ 実装乖離を解消する。
   - **推奨**: Option B（spec の文言を実装に揃える）を採用し、`spec.md` の Phase 14.3 Requirement / Scenario 1 を `Theme.defaultFooterTextColor`（固定 RGB）に書き換える。実装は変更不要。
   - **代替**: Option A（spec 通り実装に変更）を採用する場合、`Theme.footerTextColor` を Optional 化し、UI 層で `nil → .secondaryLabel`、`非 nil → UIColor(ksColor:)` の分岐を入れる。テストも追加する。

2. **🟡 Minor-1（推奨対応）**: Android `ClassicSectionDecoration.onDrawOver` のセクション内中間 Cell の罫線に 16dp 相当のインセットを追加し、iOS と視覚的に揃える。同時に Android spec の「セクション罫線の描画位置と太さ」Requirement にインセット規則を追記する。AiForms オリジナルとの突き合わせ後に最終判断。

3. **🔵 Suggestion-1（必要時のみ対応）**: Android SwitchCell オフ時 Track 色を `colorOutline` のままにするか、AiForms 互換でアルファ 76 を乗せるかを実機目視で判断。

4. **🔵 Suggestion-2（コメント追記のみ）**: Android `ClassicSectionDecoration.onDrawOver` の `isSectionTop` 判定に将来拡張時の注意コメントを追記。

---

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

### 判定根拠

- ❌ Major-1: spec の MUST 要件（Footer 文字色フォールバック）と実装が一致していない。spec を書き換えるか、実装を直すかのオーナー判断が必要。これは仕様駆動開発のサイクル上、明示的に解消すべき。
- 🟡 Minor-1: クロスプラットフォーム視覚一貫性に課題あり。spec 違反ではないが、AiForms オリジナル準拠の趣旨に照らして対応が望まれる。
- ✅ Phase 14.1〜14.9 の他の項目は spec / 実装ともに整合している。
- ✅ Phase 14.1（iOS 罫線インセット固定 16pt）は spec.md（115 行目「セクション内中間 Cell の bottom separator のインセット幅は、**アイコンの有無に関わらず固定 16pt（標準左マージン）** とする (MUST)」）と実装（`titleLeadingPosition` 常に 16）が完全に一致。
- ✅ Phase 14.2（Header / Footer 不要余白削減）は `directionalLayoutMargins = (2, 16, 2, 16)` で実装され、spec の「上下とも 0〜2pt 程度に詰める」を満たしている。
- ✅ Phase 14.3（Footer 文字色 secondaryLabel）は呼び出し側で `theme.footerTextColor` を渡す経路に整理されており、Phase 14.4（description + valueText 並列描画）も 4 パターン分岐で `secondaryText` と `UICellAccessory.customView(.trailing())` の組合せで実装されている。
- ✅ Phase 14.5（SwitchCell Track 状態別 ColorStateList）は `ColorStateList(arrayOf(checkedStates, uncheckedStates), intArrayOf(accent, outlineColor))` で実装。
- ✅ Phase 14.6（Section 上端罫線）は `prevItem !is CellListItem.CellRow || prevItem.sectionId != item.sectionId` の判定で実装。
- ✅ Phase 14.7（1px hairline）は `separatorThicknessPx = 1.0f` で実装。
- ✅ Phase 14.8（Sample テキスト一字一句一致）は iOS / Android Sample のソース対照で確認済み。
- ✅ Phase 14.9（headerHeight サンプル化）は CommandCell セクションに `headerHeight = 40` 明示指定 + DSL（SectionBuilder / DSLScope）の末尾 default 引数追加が実装済み。
- ✅ iOS swift test: 154 passed
- ✅ Android gradle test: BUILD SUCCESSFUL
- ✅ `openspec validate refine-basic-cells-sample-layout --strict`: valid

### 次のステップ

1. **Major-1 への対応**を実施（Option A / B のいずれか）。
2. **Minor-1 の対応可否**をオーナーが判断し、対応する場合は実装と spec を同時に更新。
3. 上記対応後、Phase 12 相当のテスト・ビルドを再実行し、`openspec validate --strict` で valid を再確認する。
4. Phase 13（実機目視確認）を改めて実施する。

