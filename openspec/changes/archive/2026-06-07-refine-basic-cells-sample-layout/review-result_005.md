# レビュー結果 - refine-basic-cells-sample-layout (Phase 15)

**レビュー日時**: 2026年06月06日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-sample-layout
**レビュー範囲**: Phase 15（オーナー二次実機目視 Image #8〜#11 対応）
- iOS Header / Footer の垂直配置 (Header = bottom / Footer = top)
- iOS CommandCell headerHeight = 60
- Android `Section.headerHeight` の UI 伝搬経路
- Android セル上下パディング 16dp → 4dp
- Android Header / Footer の Gravity 設定（BOTTOM / TOP）

## サマリー

Phase 15 で追加された 3 プラットフォーム横断の修正（垂直配置 / `Section.headerHeight` 伝搬 / 4dp パディング）は、
**AiForms.Maui.SettingsView オリジナル実装に忠実に追従しており、仕様（tasks / proposal / design / spec delta）と実装が完全に整合している**。

主要な確認結果：

- iOS: `applyAccessoryLabel` での `bottomAnchor` / `topAnchor` 制約 + priority 999 が AiForms `TextHeaderView.cs:42-46`（`c.Priority = 999f`）の意図を正確に踏襲。Header = `.bottom`、Footer = `.top` の `verticalAlignment` 分岐も `SetVerticalAlignment(LayoutAlignment.End)` 既定挙動（design Decision 15-1）に一致。
- Android: `Section.headerHeight` の伝搬経路（`Section` → `flatten()` → `CellListItem.SectionHeader.headerHeight` → `KsSettingsListAdapter.onBindViewHolder` → `SectionTextAccessoryViewHolder.bind(headerHeight=...)` → `layoutParams.height = headerHeight * density`）に**漏れなし**。Footer 経路には不要な伝搬を行わないようガード (`if (isHeader && headerHeight > 0.0)`) も適切。
- Android: 4dp パディング（`buildLabelCellViews` / `ButtonCellViewHolder.create`）は AiForms `cellbaseview.axml` の `paddingTop="4dp"` / `paddingBottom="4dp"`（オリジナルファイル確認済み）と一致。`applyEffectiveHeight()` で `minimumHeight = 44dp` が保証されるため、4dp 化で行高さが破綻しない。
- iOS / Android Sample の CommandCell セクション `headerHeight = 60` が両 OS で揃っている。
- 既存テスト 154 件（iOS）/ Android 全テスト PASS、Sample アプリのビルドも PASS、`openspec validate refine-basic-cells-sample-layout --strict` も valid。
- Phase 15.6 で新規追加された 4 件の Robolectric テスト（`Phase 15_6 Header bind で TextView gravity が BOTTOM_START`、`Phase 15_6 Footer bind で TextView gravity が TOP_START`、`Phase 15_3 Header bind で headerHeight 正値が layoutParams height に反映される`、`Phase 15_3 flatten で Section_headerHeight が CellListItem_SectionHeader に伝搬する` ほか）が**仕様 spec.md の Scenario と 1:1 対応**しており、テスト容易性も適切。

懸念点として、Suggestion レベルで 2 点ある（後述）が、いずれも Phase 15 のスコープを超えるか軽微な改善提案であり、本 Phase の判定は **APPROVED**。

**判定**: `APPROVED`

## 指摘事項

### iOS 実装

#### Suggestion-1 (iOS): `UIListContentConfiguration.cell()` ではなく `UIFont.preferredFont(forTextStyle: .footnote)` を直接指定している点の整合性

**該当箇所**: `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:811-813`

**問題点**:

`applyAccessoryLabel` で UILabel を生成する経路（`.bottom` / `.top` の場合）と、`UIListContentConfiguration.cell()` 経路（`.center` の場合）で、フォント解決手段が異なる。
コメントには「`UIListContentConfiguration.cell()` の text 既定相当に揃える」とあるが、`UIListContentConfiguration.cell()` の `textProperties.font` 既定は内部的に `.body` 相当（17pt 程度）であり、必ずしも `.footnote`（13pt）ではない。

Header / Footer のフォントサイズは `iOS UI / 質感` Requirement で明示的に定められていないため動作上の問題はないが、**実機目視で `.bottom` / `.top` 経路と `.center` 経路でフォントサイズに差が出る可能性**がある（Root accessory のみ `.center` 経路を通る）。

**推奨修正**:

(A) AiForms オリジナルの `PaddingLabel` は `SystemFontSize` 既定を使うため、本実装もシステム既定（`UIFont.preferredFont(forTextStyle: .body)`）と揃えるか、
(B) 逆に `.center` 経路でも明示的に `.footnote` に揃えるかのいずれかで、フォントサイズの一貫性を担保するとよい。

ただし、本指摘は Phase 15 の Scope（垂直配置）の直接の対象外であり、現状のテストもフォントサイズを検証していないため Suggestion レベルに留める。

---

### Android 実装

#### Suggestion-2 (Android): `SectionTextAccessoryViewHolder.createSectionTextView` の既存 setPadding が `headerHeight` 指定時に視覚的影響を残す可能性

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:218-220`

**問題点**:

```kotlin
val pad = (16 * resources.displayMetrics.density).toInt()
setPadding(pad, pad / 2, pad, pad / 2)  // 上下 8dp 相当
```

`SectionTextAccessoryViewHolder` の `createSectionTextView` は生成時に上下 8dp 相当（`pad / 2 = 16dp / 2`）のパディングを残す。
`headerHeight = 60` を指定したケースでは、`layoutParams.height = 60dp * density` で固定するが、内部の上下パディング 8dp は残るため、テキスト描画領域は **60dp - 16dp = 44dp**。
`gravity = Gravity.BOTTOM` でテキストは下端揃えになるが、ここでの「下端」は **パディングを除いた下端**（= contentView 下端から 8dp 上）。

AiForms オリジナル `TextHeaderView.cs:38-39` は `LabelTopAnchor`/`BottomAnchor` を `ContentView.TopAnchor`/`BottomAnchor` に **0pt インセットで**結ぶため、本実装より約 8dp 下に張り付く。

**推奨修正**:

仕様の Scenario「Header テキストは下端揃え」が成立するため動作要件は満たすが、実機目視で AiForms オリジナルと密度差が出る場合、`setPadding(pad, 0, pad, 0)` に変更すると AiForms オリジナルとより一致する。

ただし、現状の Header / Footer は単独行表示前提であり、8dp の差が視覚的に大きく問題化するとは限らないため Suggestion レベル。実機目視確認（Phase 13 の再実施）で問題視されれば修正する程度の優先度でよい。

---

### テスト

特になし。Phase 15.6 で追加された 4 件のテストは、仕様の Scenario（`Section Header の下揃え` / `Section Footer の上揃え` / `headerHeight 正値による固定高さ` / `headerHeight = -1 既定値の自動高さ`）と完全に 1:1 対応している。flatten 伝搬の単体テストも追加されており、データフローの責務境界が明確に検証されている。

### 仕様 / ドキュメント整合

- `proposal.md` の「What Changes」に Phase 15 修正項目（垂直配置・パディング 4dp・CommandCell headerHeight = 60）が記載されていることを確認（task 15.8 完了）。
- `design.md` の Decision 15-1〜15-3 は、AiForms オリジナルファイル（`TextHeaderView.cs`, `cellbaseview.axml`）の該当行を Rationale として正確に引用している。
- iOS / Android 各 spec delta（`specs/settings-view-ios-ui/spec.md` / `specs/settings-view-android-ui/spec.md`）に Requirement が ADDED されており、`openspec validate --strict` も valid。
- Android spec の「基本 Cell 共通の垂直パディング」 Requirement では `paddingTop == paddingBottom == (4 * density).toInt()` を MUST と明文化し、テストの `expected4 = (4 * density).toInt()` と整合。
- iOS spec の「Section Header / Footer の垂直配置」 Requirement では `UIView + UILabel + AutoLayout` での実装手段を明示し、Scenario でも `bottomAnchor` / `topAnchor` の制約検証を要求しており、実装・テストと整合。

## アクションプラン

| 優先度 | 項目 | 対応 |
| ---- | ---- | ---- |
| Suggestion | iOS フォントサイズの `.footnote` vs `.body` 整合 | Phase 13 の実機目視で問題視されれば対応。本 Phase ではマージ可。 |
| Suggestion | Android `SectionTextAccessoryViewHolder` の `setPadding` 縦 `pad / 2` を 0 に変更（AiForms 完全一致） | 同上、実機目視結果次第。本 Phase では Scope 外として可。 |
| —（情報） | Phase 13 再実施 | Phase 15 完了条件として tasks.md に明記済み。コードレビュー観点では完了済。 |

## 判定結果

**ステータス**: `APPROVED`

**理由**:

- Critical / Major 指摘なし。
- Phase 15 の tasks（15.1〜15.9）が漏れなく実装され、対応する spec.md の Requirement / Scenario が新規 ADDED されている。
- AiForms オリジナル `TextHeaderView.cs` / `TextFooterView.cs` / `cellbaseview.axml` と実装の挙動が完全に整合しており、design.md Decision 15-1〜15-3 の根拠も正確。
- 全プラットフォームでテスト PASS（iOS 154 / Android core+ui+compose 全）、Sample ビルド PASS、`openspec validate --strict` valid。
- 残る Suggestion 2 件は、いずれも Phase 13（実機目視確認）の結果次第で対応する性質のものであり、現時点で実装の差し戻しを要求するレベルではない。

Phase 13 の再実施（実機目視確認）で問題なければ、本 change はアーカイブ可能と判断する。
