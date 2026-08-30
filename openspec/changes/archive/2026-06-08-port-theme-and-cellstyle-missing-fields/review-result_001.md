# レビュー結果 - port-theme-and-cellstyle-missing-fields

**レビュー日時**: 2026年06月08日
**レビュワー**: sdd-reviewer
**変更提案ID**: port-theme-and-cellstyle-missing-fields

## サマリー

オリジナル `AiForms.Maui.SettingsView` から移植漏れだった「Cell 全体既定」フィールド群（`cellValueText*` / `cellDescription*` / `cellHint*` / `cellIcon*`）を `Theme` へ昇格し、`EffectiveStyle` の 3 段解決順序（`CellStyle.X → Theme.cellX → 既定`）と ButtonCell の 4 段解決を実装する変更提案。命名整合のため `viewBackgroundColor` / `titleColor` / `titleFont` を `backgroundColor` / `cellTitleColor` / `cellTitleFont` へ破壊的にリネームしている。

iOS / Android の `Theme` / `CellStyle` / `EffectiveStyle` のフィールド追加・リネーム・解決順序の中核は spec 通り正しく実装され、テストもアクセサ単位で網羅されている（iOS Core 83 / iOS UI 205 / Android 247 すべて成功）。サンプルアプリは旧 API への参照が残っておらず、`openspec validate --strict` も通る。**`Theme` と `CellStyle` の「Cell 系」フィールドおよび `EffectiveStyle` の解決順序に関しては、spec の MUST 要件をすべて満たしている**。

ただし、本 change で新規追加した **「Header / Footer 用」の `Theme.headerFont` / `Theme.footerFont` / `Theme.headerHeight` が、Theme 上にフィールドとして保持はされているが、レンダラから一切参照されていない**。spec はこれらについて単なる保持ではなく描画反映時の動作（`headerHeight` のフォールバック、`headerFontSize` 優先など）を MUST で記述しており、未実装は spec 違反である。テストもフィールド保持の確認のみで、描画反映の Scenario をカバーしていない。

加えて、`EffectiveStyle.effectiveButtonTitleColor`（既定 `.label`）を ButtonCellView から呼ばず、独自 `resolvedBaseColor`（既定 `.systemBlue`）を使う **二重実装** が iOS 側に存在する（テストは前者を、本番描画は後者を経由）。spec の ButtonCell 4 段解決 Scenario は明示指定がある限り両者で同値だが、保守性・テストカバレッジの観点で問題があり、ボタン慣習色（`.systemBlue`）を採るならば EffectiveStyle のアクセサ自体の既定値を仕様化して一本化すべき。

判定: **CHANGES_REQUESTED**（Critical 1 件 / Major 1 件 / Minor 数件）。

## 指摘事項

### 🔴 Critical: `Theme.headerHeight` / `Theme.headerFont` / `Theme.footerFont` / `Theme.headerFontSize` / `Theme.footerFontSize` の描画反映が未実装

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:444-493`（`makeHeaderBoundaryItem`）
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:759-792`, `1413` 周辺（Section Header / Footer / Root H/F の描画）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/SectionAccessoryViewHolders.kt:46-161`（Section / Root の Header / Footer 描画）

**問題点**:

iOS / Android の Section Header / Footer 描画コードは `theme.headerTextColor` / `footerTextColor` のみ参照しており、**`theme.headerFont` / `theme.footerFont` / `theme.headerFontSize` / `theme.footerFontSize` / `theme.headerHeight` を一切読まない**。新規追加された Theme フィールドは「保持されているだけ」で、利用者が `Theme(headerFont: bigFont, headerHeight: 80)` を指定しても見た目に反映されない。

iOS spec（`specs/settings-view-ios-style/spec.md`）の「Theme 型 (UI 層)」Requirement は以下を MUST と明記している：

> `headerFont: UIFont?` は Section Header の **全体既定フォント** ... 未指定のとき UI 層は既存 `headerFontSize` のみで描画する。`headerFontSize > 0` かつ `headerFont != nil` のとき、**`headerFontSize` を size として優先**する (MUST)。
> `footerFont: UIFont?` は Section Footer の **全体既定フォント** ... 挙動は `headerFont` と同じく `footerFontSize` 優先である (MUST)。
> `headerHeight: Double` は SettingsView 全体に適用される Section Header の **既定高さ** ... **Section ごとの `Section.headerHeight` が `-1.0` のときは本値を採用する** (MUST)。

Android spec も同等の MUST を持つ（`specs/settings-view-android-style/spec.md`）。

実コード `makeHeaderBoundaryItem` は `section.headerHeight > 0` のみで `.absolute(...)` を生成し、`-1.0` のときは `.estimated(20)` 固定で `theme.headerHeight` を考慮しない。iOS Section Header の `UILabel.font` 設定箇所も `theme.headerFont` を参照していない（`headerFontSize` ですら描画には反映されていない可能性がある）。

つまり、本 change が新規導入した 5 つの Header / Footer 関連 Theme フィールドのうち、**`headerHeight` / `headerFont` / `footerFont` 3 つが完全に dead field、`headerFontSize` / `footerFontSize` も Theme 値としては読まれていない**。テストも保持確認のみで、描画反映 Scenario は存在しない。

**推奨修正**:

選択肢 A（実装を補う、推奨）:
1. `KsSettingsViewController.makeHeaderBoundaryItem` を `theme` も受け取るように拡張し、`section.headerHeight` が `-1.0` のときに `theme.headerHeight` を fallback として使う。callers（`KsSettingsViewController.swift:467`, `586` 等）を更新する。
2. iOS Section / Root の Header / Footer 描画で `UILabel.font` 設定時に `theme.headerFont` / `theme.footerFont` を参照し、`headerFontSize > 0` なら `font.withSize(headerFontSize)` で size 上書きする（既存 `cellTitleFontSize` パターン踏襲）。
3. Android `SectionAccessoryViewHolders` の bind 内で `theme.headerFont` を `Typeface` 化して `textView.typeface` に設定、`theme.headerFontSize > 0` で `textView.textSize` を上書き。`Theme.headerHeight` のフォールバックは `KsSettingsView.kt` 側で Section の高さ確定ロジックに織り込む。
4. iOS / Android で対応する Scenario テストを追加（`Theme.headerHeight = 50` + `Section.headerHeight = -1` → 実 height が 50pt / 50dp となること）。

選択肢 B（本 change のスコープを縮小し、spec を緩める）:
- proposal/spec から「Header / Footer Font 系（新規追加）」と「`headerHeight` の SettingsView 全体既定」を **本 change の Goals から外し**、フィールドだけ持たせて MUST 文を「将来の Header/Footer リフレッシュ change で配線する」旨に書き換える。`headerFont` / `footerFont` / `headerHeight` の MUST から「上書き」「Section fallback」記述を削除し、Scenario も削除する。

ただし、選択肢 B は本 change の Why（「`HeaderHeight` 復活」「`headerFontFamily` / `headerFontAttributes` を `UIFont` / `TextStyle` 経由で持たせる」）と矛盾するため、**選択肢 A を強く推奨**。

---

### 🟠 Major: `EffectiveStyle.effectiveButtonTitleColor` ヘルパと `ButtonCellView.resolvedBaseColor` の二重実装、既定色不整合

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:239-248`（`effectiveButtonTitleColor`、既定 `.label`）
- `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:116-121`（`resolvedBaseColor`、既定 `.systemBlue`）
- `ios/Tests/KsSettingsViewUITests/EffectiveStyleResolutionTests.swift:250-257`（`test_effectiveButtonTitleColor_全てnilならlabel` — `.label` を期待）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:383-392`（同`effectiveButtonTitleColor`、既定 `DEFAULT_CELL_TITLE_COLOR = 黒`）
- `android/.../ButtonCellViewHolder.kt:43-51`（独自 4 段、既定 Material `colorPrimary` / `SYSTEM_BLUE`）

**問題点**:

spec の「EffectiveStyle の解決順序」Requirement は「ButtonCell.titleColor のみ特殊で、Cell 個別の `titleColor` フィールドを最優先とする 4 段解決を維持する」と書いており、iOS の `effectiveButtonTitleColor` ヘルパはこれを実装している。しかし **実プロダクションの ButtonCellView は `effectiveButtonTitleColor` を呼んでいない**。同じ 4 段解決を `ButtonCellView.resolvedBaseColor` 内に独自再実装し、加えて 4 段目の既定値を `.systemBlue`（`effectiveButtonTitleColor` は `.label`）にしている。

結果として：
1. `EffectiveStyle.effectiveButtonTitleColor` は本 change 内で **テストからしか参照されない dead code** に近い（`effectiveButtonTitleColor` の振る舞いが本番描画と乖離している）。
2. spec の「ButtonCell.titleColor の 4 段解決」Requirement に対する **真の adapter は `ButtonCellView.resolvedBaseColor` 側** なので、spec が要求する「EffectiveStyle のアクセサ関数を呼び出さなければならない (MUST)」を文字通りには満たさない。実態としての 4 段解決の意図（`Cell → Style → Theme → 既定`）は守られているため、機能要件としては成立。
3. Android も同様で、`ButtonCellViewHolder` は `EffectiveStyle.effectiveButtonTitleColor` を呼ばず、独自に `effective.titleColor` + `effective.titleColorIsExplicit` 経由で 4 段優先順位を再構築している。既定色は Material `colorPrimary`（fallback で `SYSTEM_BLUE = 0xFF007AFF`）。
4. 「ボタン平常時の慣習色を `.systemBlue` / `colorPrimary` にしたい」設計判断自体は妥当だが、spec 上「プラットフォーム既定」と書いて済ませているため、`effectiveButtonTitleColor` ヘルパの既定値（`.label` / 黒）と本番描画の既定値（`.systemBlue` / `colorPrimary`）が乖離しても spec 違反にはならない一方、**実体に対して spec の説明力が弱く、Test が本番描画の真の挙動を検証していない**。

**推奨修正**:

選択肢 A（ヘルパに揃える、推奨）:
1. iOS `EffectiveStyle.effectiveButtonTitleColor` の 4 段目を `.systemBlue` に変更し、`ButtonCellView.resolvedBaseColor` を削除して `effectiveButtonTitleColor` 呼び出しに置き換える。テストの `test_effectiveButtonTitleColor_全てnilならlabel` を `test_..._systemBlue` に書き換える。
2. Android も同様に、`EffectiveStyle.effectiveButtonTitleColor` の 4 段目を Material `colorPrimary` 取得経路に変更（または既定色を `SYSTEM_BLUE` 固定にする）し、`ButtonCellViewHolder` を `effectiveButtonTitleColor` 呼び出しへ統一。
3. spec の「ButtonCell.titleColor の 4 段解決」Scenario に「4 段目は ButtonCell 慣習色 `.systemBlue` / `colorPrimary`」を明記する（現状「プラットフォーム既定」と曖昧）。

選択肢 B（現状維持を spec で正当化）:
- `effectiveButtonTitleColor` を `internal` に格下げし「テスト専用 helper」とコメントで明示、spec の MUST から「EffectiveStyle のアクセサ関数から呼び出さなければならない」を ButtonCell について解除する。

選択肢 A の方が「ヘルパは本番描画と同一経路」を確立できるため強く推奨。

---

### 🟡 Minor: Android `EffectiveStyle` のロジック二重実装

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt`

**問題点**:

Android では ViewHolder が `EffectiveStyle.from(context, theme, cellStyle)` を呼んで `data class EffectiveStyle` を取得する経路と、新規追加された `Companion.effectiveTitleColor(cellStyle, theme)` 等の単純アクセサが二重に存在する。前者は `Color → ARGB Int` / `TextStyle → Typeface` 等の変換まで行い、後者は Compose 型のまま返す。

実態としては：
- ViewHolder（`LabelCellViewHolder` 等）は **`from()` のみ** を使い、Companion アクセサ群は呼ばない（`grep` で確認）。
- Companion アクセサ群はテスト（`EffectiveStyleResolutionTest`）からのみ呼ばれる。
- `from()` の中にも同じ「`CellStyle.X ?? Theme.cellX ?? 既定`」のロジックが手書きされている（valueText / description 等で同様）。

すなわち、spec の MUST「アクセサ関数を ... bind 処理から呼び出されなければならない」を文字どおりには満たしておらず、実体としては `from()` 内に重複実装している。バグ修正時に同じロジックを 2 箇所同期しないと不整合になり得る。

**推奨修正**:

- `from()` 内の各値解決を `effectiveTitleColor(cellStyle, theme).toArgb()` のように Companion アクセサ呼び出しへ統一する。`Typeface` / `sp Float` への変換だけが Android 固有なので、変換ヘルパ（`toTypeface` / `toSpFloatOrNull`）の呼び出しを直前で行えば、ロジック本体は単一の Companion アクセサに収まる。
- 結果として ViewHolder 群が直接 `effectiveTitleColor` を呼ぶ必要は依然なくても、spec の解決順序ロジックの SoT が 1 箇所に集約される。

---

### 🟡 Minor: `applyViewBackgroundColor` 関数名と関連コメントが旧名のまま

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:233, 254, 275, 281-287, 951`
- `ios/Tests/KsSettingsViewUITests/KsSettingsViewControllerTests.swift:295` のコメント

**問題点**:

`Theme.viewBackgroundColor` → `Theme.backgroundColor` の rename を行ったが、内部 helper `private func applyViewBackgroundColor(theme:)` および同関数を呼ぶ 3 箇所のコメントが旧名のまま残っている。同様に spec 言及コメント（`viewBackgroundColor のセクション間反映`）も旧名表記。テストコメントにも残存。

`private` メソッドなのでパブリック API への影響はないが、新規開発者が「Theme.viewBackgroundColor が削除されたのに、なぜ `applyViewBackgroundColor` という名前のメソッドがあるのか」を混乱しやすい。

**推奨修正**:

```swift
// before
private func applyViewBackgroundColor(theme: Theme) { ... }

// after
private func applyBackgroundColor(theme: Theme) { ... }
```

呼び出し元（3 箇所）と KDoc コメントも併せて更新。spec 言及コメントは「viewBackgroundColor のセクション間反映（旧 Requirement 名、現 `backgroundColor のセクション間反映`）」のように rename 後の Requirement 名へ整理する。

---

### 🟡 Minor: iOS / Android で `Theme.cellTitleColor` 未指定時の `EffectiveStyle.titleColorIsExplicit` 取得方法に整合性のずれ

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:77`（`titleColorIsExplicit = (cellStyle.titleColor != nil) || (theme.cellTitleColor != nil)`）
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:90-91`（同等）

**問題点**:

`titleColorIsExplicit` は ButtonCellView / ButtonCellViewHolder の「4 段目フォールバックをいつ採るか」判定に使われる。iOS / Android で意味は揃っているが、`ButtonCellView` 側で `effective.titleColorIsExplicit` を使う iOS と、`ButtonCellViewHolder` 側で同フラグ + 独自実装の Android で実装スタイルがやや非対称。本フラグの意義は「Theme.cellTitleColor / CellStyle.titleColor のどちらかで明示指定があれば `effective.titleColor` を信用する」だが、本 change が `effectiveButtonTitleColor` を導入した以上、このフラグ経由ではなくヘルパに統一する方がきれい（Major 指摘の修正と連動）。

**推奨修正**: Major 指摘で `effectiveButtonTitleColor` に統一すれば、`titleColorIsExplicit` フラグの存在意義が薄れる。統一後にフラグを削除するかどうかを検討。

---

### 🔵 Suggestion: `Theme.cellTitleFontSize` / `headerFontSize` / `footerFontSize` の "size 上書き" 挙動が `EffectiveStyle.effectiveTitleFont` に集約されている一方、Header / Footer 用には effectiveHeaderFont / effectiveFooterFont のアクセサが存在しない

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:115-135`（`effectiveTitleFont` は `cellTitleFontSize` 上書きを実装）
- 同ファイル全体に `effectiveHeaderFont` / `effectiveFooterFont` のアクセサが**ない**
- Android 側も同様

**問題点**:

spec は `headerFont` / `footerFont` について「`headerFontSize > 0` のとき size 上書き」を MUST と書いているが、その上書きロジックを集約する `effectiveHeaderFont(theme) -> UIFont?` / `effectiveFooterFont(theme) -> UIFont?` のようなヘルパが用意されていない。Critical 指摘の修正で描画コード側に上書きロジックを直書きするか、新たなアクセサ関数を切るかの選択になる。

**推奨修正**: Critical 指摘の対応と同時に、`EffectiveStyle` に `effectiveHeaderFont(theme:)` / `effectiveFooterFont(theme:)` を追加し、`headerFontSize > 0` で `withSize` する責務を集約。テストも追加する。

---

### 🔵 Suggestion: `Theme.headerHeight` の単位が iOS / Android 両方 `Double` で論理単位だが、iOS 側の解釈が pt と書かれているのに `Section.headerHeight` の `Int` / `Double` 混在と整合がない

**該当箇所**:
- `ios/Sources/KsSettingsViewUI/Theme.swift:83-85`（`headerHeight: Double`、pt と明記）
- `ios/Sources/KsSettingsViewCore/Section.swift:46`（`headerHeight: Double`）

**問題点**:

Section.headerHeight は既存実装で `Double` 受け取りなので整合 OK。一方、`Theme.rowHeight` は `Int`（既存）で、`Theme.headerHeight` は `Double` という不揃いがある。本 change のスコープではないが、将来「rowHeight も Double 化」する判断時に整合性を取りやすくしておくと良い。

**推奨修正**: Open Question として記録に残す。本 change のスコープ外なので変更不要。

---

## アクションプラン（優先度順）

1. **Critical の解消（最重要、必須）** — `Theme.headerFont` / `Theme.footerFont` / `Theme.headerHeight` / `Theme.headerFontSize` / `Theme.footerFontSize` の描画反映を iOS / Android で実装する。または本 change スコープから除外して spec を緩める判断を行う（NEEDS_DISCUSSION 級の設計判断）。実装する場合は対応する Scenario テストも追加（描画反映の e2e）。
2. **Major の解消** — `EffectiveStyle.effectiveButtonTitleColor` の 4 段目既定値を `.systemBlue` / `colorPrimary` に整え、`ButtonCellView.resolvedBaseColor` / `ButtonCellViewHolder` の独自 4 段ロジックをヘルパ呼び出しに置換。テストを更新。
3. **Minor: Android `EffectiveStyle.from()` のロジック重複解消** — Companion アクセサを `from()` 内で呼ぶ形に統一し、解決順序の SoT を 1 箇所にする。
4. **Minor: `applyViewBackgroundColor` のリネーム** — `applyBackgroundColor` へ private メソッド名変更 + コメント整理。
5. **Suggestion: `effectiveHeaderFont` / `effectiveFooterFont` アクセサ追加** — Critical 修正と同時に実装。
6. **Suggestion: `Theme.rowHeight` / `Theme.headerHeight` の型不揃いを Open Question として記録** — 別 change で扱う。

---

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

**理由**:
- spec の MUST 要件のうち、`Theme.headerFont` / `Theme.footerFont` / `Theme.headerHeight` の描画反映（Section header の font 切替、`Section.headerHeight = -1.0` 時の Theme fallback、`headerFontSize > 0` 優先）が未実装である（**Critical**）。テストもこれらの描画反映を一切カバーしていない。
- 機能上の重要な乖離（**Major**: ButtonCell 4 段解決のヘルパと本番描画の二重実装＋既定値不整合）が iOS / Android の両方に存在する。テストはヘルパ側のみを検証し、本番描画の真の既定値（`.systemBlue` / `colorPrimary`）を担保していない。
- リネーム・Cell 系フィールド追加・3 段解決の中核は spec 準拠で完成度が高く、テストカバレッジも良好。Critical / Major の 2 点を解消できれば全体として承認可能な完成度に達する。
