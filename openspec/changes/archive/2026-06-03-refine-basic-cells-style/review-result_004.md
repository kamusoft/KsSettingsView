# レビュー結果 - refine-basic-cells-style (Phase 17)

**レビュー日時**: 2026年06月03日
**レビュワー**: sdd-reviewer
**変更提案ID**: refine-basic-cells-style
**レビュー対象範囲**: Phase 17（Theme.titleColor / Theme.titleFont 追加）のみ
**前提**: Phase 1〜16 は review-result_001〜003 / verification-report で承認・検証済みのため深掘り対象外。

## サマリー

Phase 17 で追加された「Theme.titleColor / Theme.titleFont（原典 AiForms `Theme.CellTitleColor` / `Theme.CellTitleFont` 相当）」の実装を、iOS / Android の Core / UI / テスト / Sample すべてにわたってレビューした。

仕様（proposal.md / design.md Decision 12 / specs/cell-types-basic / specs/settings-view-core / specs/settings-view-ios-ui / specs/settings-view-android-ui）に対する実装適合度は非常に高い。具体的には：

- Core: Theme に `titleColor: KsColor?` / `titleFont: KsFont?` をデフォルト `nil` / `null` で末尾追加し、API 互換性を維持している。既存呼び出しはコンパイル可能で、新フィールドのテストも追加されている。
- iOS EffectiveStyle: 3 段階優先順位（CellStyle → Theme → `.label` / `.preferredFont(.body)`）と `titleColorIsExplicit` フラグが spec 通り。
- iOS ButtonCellView: 4 段階優先順位（Cell 個別 → CellStyle → Theme → `.systemBlue`）と disabled 時の `disabledTextColor` 優先が正しく実装されている。
- Android EffectiveStyle / ButtonCellViewHolder: iOS と完全に対称。Material `colorPrimary` をフォールバック (3 段階目) に採用。
- 全 Cell（LabelCell / CommandCell / SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell / ButtonCell）が `effective.titleColor` を読む共通経路を持っており、spec の「全 Cell 共通の Theme.titleColor 反映」Requirement の実装根拠が担保されている。
- Sample（iOS / Android 両方）で MAUI 互換色 `#CC9900` を `Theme.titleColor` に渡すよう更新済み。

ただし、**Android `:ks-settingsview-compose:testReleaseUnitTest` の 1 件のテスト（`KsSettingsViewComposeTest > DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される`、line 223）が初回実行で FAIL し、2 回目実行で PASS する flaky 挙動を示している**。
- このテストは Phase 17 で追加・変更されたコードとは無関係（既存テスト、最終更新 commit `142e6ad`）。
- spec MUST 条件としては Phase 17 とは独立に存在していた既知問題と推定されるが、レビュアー手順の「テストが失敗してたら即 CHANGES_REQUESTED」規則に従う。

**判定**: `CHANGES_REQUESTED`
（理由: spec / 設計適合は問題なしだが、Android Compose ユニットテストが flaky であるため。Phase 17 スコープに対する追加修正は不要だが、flaky 解消もしくは正式な flaky 認定の判断をユーザに仰ぐべき）

## 指摘事項

### Critical

なし。

### Major

#### 🟠 M-1. Android Compose ユニットテストが flaky で再現性のある失敗を示す

**該当箇所**: `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/KsSettingsViewComposeTest.kt:223`

**問題点**:
- `./gradlew :ks-settingsview-core:test :ks-settingsview-ui:test :ks-settingsview-compose:test --rerun-tasks` を実行すると、`DSL 方式で外部 state を 2 回連続更新しても 2 回目の追加が反映される` テストが `expected:<3> but was:<2>` で AssertionError となった（`testReleaseUnitTest` タスク）。
- ただし直後の再実行（`./gradlew :ks-settingsview-compose:test --rerun-tasks`）では PASS。Compose の recomposition タイミングに依存する flaky テスト。
- Phase 17 の変更（`Theme.titleColor` / `titleFont` 追加と EffectiveStyle / ButtonCellView の 3〜4 段階化）はこのテストとは完全に無関係（state 同期ロジック / DSL 経路）。tasks.md の Phase 17.14（「`gradle test` 全件 PASS」）は決定的に PASS する状態を前提に [x] とされているが、実態は再実行で PASS する flaky 状態。
- レビュアー手順の禁止事項「テストの失敗を見過ごすことは禁止です」に該当するため記録。

**推奨修正**:
1. tasks.md の Phase 17.14 のチェックを下記いずれかで明確化する：
   - (a) flaky テスト 1 件を許容して Phase 17 範囲外として明記しつつ、別 change で `refactor-display-state-sync` 系の追加修正を提案する。
   - (b) `KsSettingsViewComposeTest.kt:192` のテストを安定化する（例: `composeTestRule.awaitIdle()` 追加、`waitForIdle()` の追加、`Snapshot.sendApplyNotifications()` の明示呼び出し、または該当ケース固有の同期待ち）。
2. Phase 17 スコープに対する追加修正は不要。

### Minor

#### 🟡 m-1. SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell の Theme.titleColor 反映テストが存在しない

**該当箇所**:
- iOS: `ios/Tests/KsSettingsViewUITests/BasicCellsTests.swift`（Phase 17 セクション、line 517〜）
- Android: `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/BasicCellsTest.kt:873〜`

**問題点**:
- spec `specs/cell-types-basic/spec.md` の「全 Cell 共通の Theme.titleColor / Theme.titleFont 反映」Requirement の Scenario「Theme.titleColor が全 Cell タイトル色に反映される」では `LabelCell` / `SwitchCell` / `CheckboxCell` が並ぶケースが要求されている。
- 実装は全 Cell ViewHolder / Cell View が `effective.titleColor` を共通経路で読むため (`applyLabelCellContents` 経由、または直接 `effective.titleColor` 参照)、機能としては保証されている。
- ただし、`Theme.titleColor` 反映を明示的に検証するテストは LabelCell / CommandCell の 2 ケースのみ。SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell については EffectiveStyle 単体テスト経由の間接担保のみ。
- レビュー観点チェックリスト「specに対応するテストが実装されているか」に厳密適合させるなら、これら 4 種についても直接の Theme.titleColor 反映テストを追加するのが望ましい。Phase 17 完了は妨げないが Suggestion レベルから Minor に格上げ。

**推奨修正**:
SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell について、`Theme(titleColor: ...)` を渡して bind した後、`title` 表示テキストの色（content configuration の textProperties.color / titleView.textColors.defaultColor）が指定色になることを検証するテストを 4 件追加する。LabelCell の既存テスト（`test_LabelCellView_Theme_titleColor_反映される` / `LabelCellViewHolder Theme titleColor が反映される`）と同型でコピペ可能。

例（iOS）:
```swift
func test_SwitchCellView_Theme_titleColor_反映される() {
    let view = SwitchCellView()
    let themeColor = KsColor(red: 0.4, green: 0.0, blue: 0.6, alpha: 1.0)
    view.render(cell: SwitchCell(title: "X", isOn: false, onValueChanged: { _ in }),
                theme: Theme(titleColor: themeColor))
    // content.textProperties.color を確認
}
```

#### 🟡 m-2. iOS `ButtonCellView` の `resolvedBaseColor` 内コメントで「3 段階目」の説明が `.label` に言及していない

**該当箇所**: `ios/Sources/KsSettingsViewUI/ButtonCellView.swift:114-118`

**問題点**:
```swift
/// `effective.titleColor` は明示指定なし時に `.label` を返すため、単にそれを使うと
/// 「3 段階目を `.label`」にすることになる。Button 用には平常時 `.systemBlue` を
/// デフォルトにしたい意図があり、それを 1 関数に集約する。
```
- コメントは正しく「4 段階目」の動作（3 段階目を `.systemBlue` にしたい意図）を説明しているが、`/// 優先順位:` の番号付き列挙（行 110-114）は 1, 2, 3 の 3 つで、説明文中の「3 段階目」表現と整合しているものの、実態は 4 段階優先順位（Cell 個別 / CellStyle / Theme / システム既定）であり、混乱する可能性がある。
- design.md Decision 12 と spec/cell-types-basic では「4 段階」と明記されているのに対し、コメント上で「3 段階」と書かれているのは Phase 17 の意図を読みづらくする。

**推奨修正**:
`resolvedBaseColor` の doc コメントを以下のように明確化する：

```swift
/// ボタン文字色の「平常時 baseColor」を 4 段階優先順位で決定する
/// （refine-basic-cells-style Phase 17 で `Theme.titleColor` を反映する 4 段階に拡張）。
///
/// 優先順位:
/// 1. **Cell 個別の `btn.titleColor`** が指定されていればそれを使う。
/// 2. **`CellStyle.titleColor`** が指定されていれば `effective.titleColor`（合成済み）を使う。
/// 3. **`Theme.titleColor`** が指定されていれば `effective.titleColor`（合成済み）を使う。
///    判定は `effective.titleColorIsExplicit`（`cellStyle.titleColor != nil || theme.titleColor != nil`）で 2 と 3 をまとめる。
/// 4. いずれも未指定なら `.systemBlue`（ButtonCell の慣習的なアクセント色）にフォールバック。
```

### Suggestion

#### 🔵 s-1. `EffectiveStyle.titleColor` の 3 段階目フォールバックを `.label` 固定で書く iOS と、`textColorPrimary` を Theme から動的解決する Android の対称性

**該当箇所**:
- iOS: `EffectiveStyle.swift:84` (`let defaultTitleColor: UIColor = .label`)
- Android: `EffectiveStyle.kt:163` (`resolveDefaultTitleColor(context)`)

**問題点**:
- iOS は `.label` を直接 hardcode し、Android は Context 経由で `android.R.attr.textColorPrimary` を解決している。spec はそれぞれ「iOS: `UIColor.label`」「Android: `TextView` 既定色」を要求しているため、これは正しい。
- ただし Android 側は ColorStateList を `.defaultColor` で抽出している（line 178）。`textColorPrimary` は dark/light mode で動的に変わるが、`getColorStateList(...).defaultColor` を 1 度だけ取得してキャッシュする現実装では、ダークモード遷移時にも色が静的になる可能性がある。
- これは Phase 17 のスコープ外（既定値解決の挙動として独立）かつ Suggestion レベル。今後ダークモード対応を強化する別 change で再検討する余地あり。

**推奨修正**:
- 現状コード変更不要。今後ダークモード切替対応の別 change で `textColorPrimary` を `ColorStateList` のまま `setTextColor(ColorStateList)` に渡す経路を検討する旨を、design.md のリスクトレードオフセクションにメモとして残すと良い。

#### 🔵 s-2. iOS `EffectiveStyle.swift` の Phase 17 コメントは網羅的だが、`titleFont` のフォールバック 3 段階目に対する言及が `headline` ではなく `.body` であることのドキュメント

**該当箇所**: `ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:80-85`

**問題点**:
- iOS では 3 段階目フォールバックを `UIFont.preferredFont(forTextStyle: .body)` としており、spec の `specs/settings-view-ios-ui/spec.md:53`（`UIFont.preferredFont(forTextStyle: .body)`）と一致している。OK。
- ただし AiForms 原典の `Theme.CellTitleFontSize` は概ね 17pt サイズだったため、テストで `pointSize 17pt` を期待値とする際に Dynamic Type 設定によっては動的にサイズが変わる可能性がある。`EffectiveStyleTests.swift:218` の `test_titleFont_両方未指定_preferredBodyFontが採用される` は `textStyle == .body` で検証しており妥当。問題なし。
- ドキュメント上、Android 側で「17sp 既定」と書いていてもう少し iOS 側のコメントで Dynamic Type 連動性を補足するとレビュー時の理解が早い。

**推奨修正**: 任意改善。Phase 17 のメリットを損なわないので保留可。

## アクションプラン

優先順位順：

1. **[Major M-1]** `KsSettingsViewComposeTest.kt:192` の flaky 挙動の取り扱いを明確化する。
   - 選択肢 A: 別 change で flaky 解消（推奨。`refactor-display-state-sync` の追加 phase など）。
   - 選択肢 B: 本 change のスコープ外として `notes.md` または `risks` セクションに既知問題として明記し、Phase 17.14 のチェック条件文を「初回 PASS は保証されないが Phase 17 範囲外」と明示。
2. **[Minor m-1]** SwitchCell / CheckboxCell / RadioCell / SimpleCheckCell の Theme.titleColor 反映テストを 4 件追加。iOS / Android 両方で計 8 件相当。
3. **[Minor m-2]** `ButtonCellView.swift` の `resolvedBaseColor` の doc コメントを「3 段階」→「4 段階」の表現に統一する。
4. **[Suggestion s-1, s-2]** 任意改善（今期は対応不要）。

## 仕様適合性チェック（要点）

| spec 要件 | iOS 実装 | Android 実装 | テスト |
|---|---|---|---|
| Theme.titleColor 末尾追加 / Optional / 既定 nil | OK (Theme.swift:77, init 107) | OK (Theme.kt:91) | OK (ThemeTests / ThemeTest) |
| Theme.titleFont 末尾追加 / Optional / 既定 nil | OK (Theme.swift:83, init 108) | OK (Theme.kt:99) | OK |
| EffectiveStyle titleColor 3 段階優先順位 | OK (EffectiveStyle.swift:87-93) | OK (EffectiveStyle.kt:82-88) | OK 4 ケース + Explicit フラグ |
| EffectiveStyle titleFont 3 段階優先順位 | OK (EffectiveStyle.swift:94-100) | OK (EffectiveStyle.kt:96-98) | OK |
| `titleColorIsExplicit` フラグ | OK (line 102) | OK (line 89-90) | OK |
| ButtonCell baseColor 4 段階 | OK (ButtonCellView.swift:119-127) | OK (ButtonCellViewHolder.kt:38-46) | OK (iOS 4 ケース / Android 4 ケース) |
| ButtonCell disabled 時 disabledTextColor 優先 | OK (ButtonCellView.swift:65) | OK (ButtonCellViewHolder.kt:48) | OK |
| 全 7 種 Cell で `effective.titleColor` 経路を共有 | OK (各 *CellView.swift) | OK (各 *CellViewHolder.kt / applyLabelCellContents) | LabelCell / CommandCell のみ直接テスト。残り 4 種は EffectiveStyle 経由の間接担保 |
| API 互換性（既存 Theme() 呼び出し可） | OK (デフォルト値付き末尾追加) | OK (data class デフォルト値付き) | OK (`test_API互換性_新フィールド未指定の既存呼び出しが動く`) |
| Sample に MAUI 互換 titleColor 渡し | OK (BasicCellsDemoView.swift:80) | OK (BasicCellsDemoScreen.kt:237) | (ビルド成功) |
| 旧 Suggestion-2 コメント置換 | OK (旧コメントの残骸なし) | OK | - |

## 判定結果

**ステータス**: `CHANGES_REQUESTED`

**判定理由**:
- Phase 17 の実装そのものは spec 完全準拠で、コードレビュー観点では問題ない（Critical なし、Major は flaky テスト 1 件のみ）。
- レビュアー手順の禁止事項「テストの失敗を見過ごすことは禁止です。テストが失敗してたら即 CHANGES_REQUESTED」に厳格に従い、`KsSettingsViewComposeTest:192` の Android Compose flaky test を「失敗」と扱う。
- Phase 17 の追加スコープに対するコード修正は **不要**。Major M-1 の取り扱い方針をユーザ判断（別 change での解消 or 本 change の completion 条件緩和）として明示し、Minor m-1 / m-2 を任意で追加修正することを推奨する。

判定: CHANGES_REQUESTED
