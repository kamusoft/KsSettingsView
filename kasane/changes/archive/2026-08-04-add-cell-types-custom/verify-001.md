# Verify 001: add-cell-types-custom

- **判定: VALID**
- 検証日: 2026-08-04
- 検証対象: `specs/cell-types-custom/spec.md`（10 Requirement / 21 Scenario）
- 対象プラットフォーム: iOS / Android 両方
- ワークツリー: `.claude/worktrees/ksn-orchestrator-cell-types-custom-3ffa8f`

凡例: ✅ 一致 / ⚠️ deviation 記録済み（合意済み差分） / ❌ 欠落・乖離

パス表記はワークツリー相対。ファイルは以下の略号で示す。

| 略号 | パス |
|---|---|
| **iOS-T** | `ios/Tests/KsSettingsViewUITests/CustomCellTests.swift` |
| **iOS-DSL-T** | `ios/Tests/KsSettingsViewSwiftUITests/CustomCellDSLTests.swift` |
| **AND-T** | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellTest.kt` |
| **AND-R-T** | `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellRenderingTest.kt` |
| **AND-DSL-T** | `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/CustomCellDslTest.kt` |
| **AND-DSL-R-T** | `android/ks-settingsview-compose/src/test/kotlin/jp/kamusoft/kssettingsview/compose/CustomCellDslRenderingTest.kt` |

---

## 1. Requirement: CustomCell の定義と等価性 (ADDED)

### Scenario: builder だけが異なるインスタンスは等価

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `ios/Sources/KsSettingsViewUI/CustomCell.swift:203`（`static func ==` が `builder` / `onTap` を除外）/ `:213` `hash(into:)` | iOS-T:141 `test_builderだけが異なるインスタンスは等価` / iOS-T:151 `test_onTapだけが異なるインスタンスは等価` / iOS-DSL-T:109 `test_同値contentの再評価ではreplaceCellが発行されない`（差分検出が再バインドを起こさないことまで実証） | ✅ |
| Android | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCell.kt:90` `equals` / `:101` `hashCode` | AND-T:27 `builder だけが異なるインスタンスは等価` / AND-T:35 `onTap だけが異なるインスタンスは等価` / AND-DSL-T:136 `同値 content の再評価では内容更新が発生しない` | ✅ |

### Scenario: content が異なれば非等価

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCell.swift:207`（`content`）/ `:206`（`contentType`。`AnyHashable` の型跨ぎ等価を防ぐ追加ガード） | iOS-T:162 `test_contentが異なれば非等価` / iOS-T:176 `test_content値が同じでも実体型が異なれば非等価` / iOS-DSL-T:129 `test_contentの変化でreplaceCellが発行される` | ✅ |
| Android | `CustomCell.kt:95`（`content == other.content`） | AND-T:44 `content が異なれば非等価` / AND-T:81 `content 型が異なれば非等価` / AND-DSL-T:145 `content の変更で内容更新が発生する` | ✅ |

### Scenario: 表示に効くスカラーの変更も非等価

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCell.swift:203-211`（`id` / `style` / `showArrow` / `isEnabled` / `isVisible`） | iOS-T:218 `test_showArrowが異なれば非等価` / iOS-T:227 `test_isEnabledとisVisibleとstyleも等価判定に参加する` / iOS-DSL-T:189 | ✅ |
| Android | `CustomCell.kt:93-98` | AND-T:51 `showArrow だけが異なれば非等価` / AND-T:58 `id と style と isEnabled と isVisible も等価性に参加する` / AND-DSL-T:155 | ✅ |

**Requirement 本文の利用者契約**（content は値等価を持つ non-null 型 / Android は型制約で強制）: `CustomCell.kt:49` `class CustomCell<Content : Any>` で強制。iOS 側は `init<C: Hashable, V: View>`（`CustomCell.swift:119`）で `Hashable` を要求し、non-Optional は doc の契約として明記（`CustomCell.swift:45-47`）— spec 本文も「Android は型制約で強制」と限定しているため一致。

---

## 2. Requirement: 事前登録なしの描画 (ADDED)

### Scenario: Registry 未操作で表示できる

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `ios/Sources/KsSettingsViewUI/KsCellRegistry+CustomCell.swift:23` `registerCustomCell()` / `ios/Sources/KsSettingsViewUI/KsSettingsViewController.swift:190-192`（`autoRegisterCustomCell` 既定 `true` で shared registry へ自動登録） | iOS-T:293 `test_Registry未操作でもCustomCellがbuilderの出力で描画される`（`KsSettingsViewController` 実経路で `CustomCellView` が使われ probe が描画されることを実測）/ iOS-T:311 / iOS-T:321（content 実体型が違っても単一登録で解決） | ✅ |
| Android | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsCellRegistryCustomCell.kt:23` `registerCustomCell(context)` / `:31` `VIEW_TYPE_CUSTOM_CELL = 120` / `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/KsSettingsView.kt:224-227`（初期化時の sentinel 方式自動登録） | AND-R-T:629 `KsSettingsView 初期化で CustomCell が自動登録される`（**`strictMode == true` でも viewType が解決でき例外にならないことを明示的に検証**）/ AND-R-T:649 `Registry 未操作のまま CustomCell を含む root を表示できる` / AND-DSL-R-T:34（Compose ラッパ経由の end-to-end） | ✅ |

補足: 「strictMode でも例外にならない」は Android 側の Registry 概念であり（iOS の `KsCellRegistry` に strictMode は存在しない）、Android で明示検証されている。

---

## 3. Requirement: content 駆動の描画と再利用 (ADDED)

### Scenario: content の更新で表示が変わる

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `ios/Sources/KsSettingsViewUI/CustomCellView.swift:69-77`（bind 時に `custom.builder(custom.content)` を `UIHostingConfiguration` へ差し込む） | iOS-T:337 `test_bindでbuilder出力が行に描画される` / iOS-T:352 `test_contentの更新で表示がbuilderの新出力に変わる`（**実経路**: `applyDiff(.replaceCell)` → reconfigure → 再 render。旧 probe が消えることまで確認） | ✅ |
| Android | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CustomCellViewHolder.kt:53-66`（`cell.composeContent` を `setContent` へ）/ `CustomCell.kt:68` `composeContent` | AND-R-T:150 `bind すると builder の出力が composition に現れる` / AND-R-T:159 `content を更新すると builder の出力が入れ替わる` | ✅ |

### Scenario: 再利用時に前の内容が残らない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:105-109` `prepareForReuse()`（`tapHandler = nil` / `contentConfiguration = nil` で hosted View 階層と SwiftUI 購読を解放） | iOS-T:372 `test_prepareForReuseで前のcontentとlistenerが残らない`。**注**: 「別の Cell の行として再バインドされる」までを 1 テストで通す形にはなっていない（`prepareForReuse` 後の状態を検証）。スクロール再利用の実挙動は `ui/verification/ios-sim-iphone17-ios265-06-scroll-recycle-tap.png`（40 行往復スクロール後、タップした行にだけ ✓ が付く = listener の取り違えなし）で実証 | ✅ |
| Android | `CustomCellViewHolder.kt:98-106` `reset()`（listener / clickable / enabled / descendantFocusability を戻し `setContent {}` で content 参照を切る） | AND-R-T:177 `reset で前の content とタップ listener が残らない` / AND-R-T:193 `reset 後に別 Cell を bind すると新しい content だけが描画される`（`assertEquals(listOf("probe-B"), tags)` で残骸なしを実証）。実機証跡 `android-pixel6a-07-scroll-recycle-bottom-tap.png` | ✅ |

### Requirement 本文「content が等価のままの再構成では再バインドを要求しない」

| P | テスト | 状態 |
|---|---|---|
| iOS | iOS-T:247 `test_同値contentの再構成はAnyHashable比較でも等価` / iOS-DSL-T:109（`DSLDiffCalculator.compute` が空を返す） | ✅ |
| Android | AND-DSL-T:136 / AND-DSL-T:162（静的形も同様） | ✅ |

---

## 4. Requirement: 静的コンテンツの省略形 (ADDED)

### Scenario: content なしで生成・表示できる

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCell.swift:62` `EmptyContent` / `:152` content なし `init` | iOS-T:264 `test_contentなしで生成でき等価性はcontent以外の参加要素で決まる` / iOS-T:278 `test_contentなし省略形のbuilder出力が行に描画される` | ✅ |
| Android | `CustomCell.kt:122` `object CustomCellEmptyContent` / `:138` ファクトリ関数 `CustomCell(...)` | AND-T:90 `content なしの省略形は空 content を持つ` / AND-T:96 `省略形の等価性は content 以外の参加要素で決まる` / AND-R-T:210 `content なしの省略形も builder の出力を描画する` | ✅ |

---

## 5. Requirement: 行タップ (ADDED)

### Scenario: onTap 指定時に行タップで発火する

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:85` `tapHandler` / `KsSettingsViewController.swift:1862` `extension CustomCellView: TapNotifyingRenderer {}`（`didSelectItemAt` → `tapHandler` の標準経路に接続） | iOS-T:397 `test_onTap指定時に行タップで1回発火する`（**実経路** `controller.collectionView(_:didSelectItemAt:)` で 1 回だけ発火） | ✅ |
| Android | `CustomCellViewHolder.kt:71-76`（`setOnClickListener` を毎回上書き / 解除） | AND-R-T:259 `onTap 指定時に行タップで発火する`（実 `MotionEvent` を `dispatchTouchEvent`） | ✅ |

### Scenario: 既定では行タップ動作を持たない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:85`（`onTap == nil` なら `tapHandler` も nil。行タップ用 recognizer は追加しない） | iOS-T:416 `test_onTap未指定なら行タップハンドラを持たない`。**注**: 「コントロールの操作がそのまま機能する」は `isUserInteractionEnabled == true` の確認までで、実コントロールを駆動していない。実挙動は `ui/verification/ios-sim-iphone17-ios265-07-slider-enabled-dragged-disabled-blocked.png`（onTap を持たない SliderCell ラップ行のスライダーが 70→31 に追従）で実証 | ✅ |
| Android | `CustomCellViewHolder.kt:74`（listener 解除）/ `:81` `isClickable = isEnabled`（callback ではなく押下 feedback のための状態） | AND-R-T:293 `既定では行タップ動作を持たず content 内のコントロールが機能する`（listener 不在を確認したうえで実タップし子の callback が 1 回発火）/ AND-R-T:314（clickable と callback の分離を明文化） | ✅ |

### Scenario: 子要素の操作では行タップが発火しない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:79-85` コメントのとおり、行タップは `UICollectionView` の選択経路のみ。Cell 自身に gesture recognizer / target-action を追加しない | iOS-T:435 `test_行タップ用のgestureRecognizerをCell自身に追加しない`。**注**: 実ジェスチャ競合そのものではなく「二重発火し得る構造を作っていない」ことの検証（実装者が自己申告した単体テストの限界）。実挙動は `ui/verification/ios-sim-iphone17-ios265-05-child-tap-does-not-fire-row-ontap.png`（content 内 Button タップでカウンタが「0 回」= 行 onTap 未発火）で実証 | ✅ |
| Android | `CustomCellViewHolder.kt:68-70` コメント（content 側がタッチを消費すると View の click 経路まで届かない） | AND-R-T:271 `子要素の操作では行タップが発火しない`（実 `MotionEvent`。子 1 回・行 0 回を実測）。実機証跡 `android-pixel6a-06-child-tap-does-not-fire-row-ontap.png` | ✅ |

### Scenario: 無効時はタップが発火しない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:85` `custom.isEnabled ? custom.onTap : nil` | iOS-T:454 `test_isEnabledがfalseなら行タップは発火しない`（`tapHandler` nil に加え、実経路 `didSelectItemAt` でも 0 回） | ✅ |
| Android | `CustomCellViewHolder.kt:72-85`（listener 解除 + `isClickable = false` + `isEnabled = false`） | AND-R-T:325 `無効時は行タップが発火しない`。押下 feedback が出ないことは `android-pixel6a-11-press-feedback-disabled-none.png` | ✅ |

### Scenario: 無効時は content 内の操作も抑止される

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `ios/Sources/KsSettingsViewUI/CustomCellHostedContent.swift:44` `.disabled(!isEnabled)` + `KsCellViewSupport.setRenderState` 経由の `isUserInteractionEnabled = isEnabled`（`KsCellViewSupport.swift:69`） | iOS-T:482 `test_isEnabledがfalseならcontent内の操作も抑止される`（`isUserInteractionEnabled == false` と `hitTest(center) == nil` を実測。有効に戻すと成立する mutation probe 付き） | ✅ |
| Android | `CustomCellViewHolder.kt:146` `consumePointerInput`（Initial パスでポインタ消費）/ `:153` `blockDescendantActions`（`clearAndSetSemantics { disabled() }`）/ `:89-93` `FOCUS_BLOCK_DESCENDANTS` | AND-R-T:345 `無効時は content 内の操作も抑止される`（ポインタ経路）/ AND-R-T:367 `無効時は accessibility action 経由でも content 内の操作が発火しない`（semantics action 総当たりで 0 件・Slider 値不変）/ AND-R-T:413（有効時は発火する mutation probe）。**注**: 367 は Robolectric 上の semantics action 実行であり実 accessibility service そのものではない。実機の TalkBack ツリーは `android-pixel6a-14-a11y-tree-disabled-row.xml` / `android-pixel4a-04-a11y-tree-disabled-row.xml`（無効行に SeekBar もテキストも現れない）で実証 | ✅ |

### Requirement 本文「無効時の見た目の描き分けは利用者責務」

| P | 実装 | 状態 |
|---|---|---|
| iOS | `CustomCellHostedContent.swift:35` `opacity(isEnabled ? 1 : 0.38)` | ⚠️ **deviation 記録済み**（`deviation.md`「spec からの乖離 (オーナー指示)」）。spec は「利用者責務」としているが、オーナー指示で両プラットフォームとも content 全体を alpha 0.38 で淡色化する。iOS の標準コントロールが二重に薄くなる副作用も記録済み |
| Android | `CustomCellViewHolder.kt:152` `Modifier.alpha(DISABLED_CONTENT_ALPHA)`（`:235` = 0.38f） | ⚠️ 同上。あわせて `concepts/core/styling/cell-visual-states.md` との緊張関係も `deviation.md` に記録済み |

**無効時の TalkBack 読み上げ**（Android の content が読み上げ対象から外れ、iOS とは非対称）: ⚠️ `deviation.md`「アクセシビリティの扱い (オーナー判断)」に記録済み。

---

## 6. Requirement: Disclosure Indicator の表示 (ADDED)

### Scenario: showArrow で indicator が表示される

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellHostedContent.swift:37-39` / `:60-66` `chevron`。アセット・寸法・色・末端余白は `ios/Sources/KsSettingsViewUI/KsChevronAppearance.swift:19-47` に集約し、UIKit 経路の `makeChevronView()`（`CellBaseLayout.swift:176-186`）と同一定数を共有 | iOS-T:532 `test_showArrowでcontentの占有領域がindicator分だけ狭くなる`（content の占有幅が `trailingMargin` + chevron 幅ぶん狭くなることを実測）。**注**: 「標準 Cell と同一」の同一性は単体テストでは定数共有の構造として担保。ピクセル実測は `ui/verification/ios-sim-iphone17-ios265-03b-chevron-zoom.png` / `-11-post-fix-chevron-vs-commandcell.png`（CommandCell と bbox 20×35px・x 1136–1155・右端余白 50px が一致、最大画素差 6） | ✅ |
| Android | `CustomCellViewHolder.kt:157-168`（`R.drawable.ic_navigate_next` を共通定数 `CELL_DISCLOSURE_WIDTH_DP` / `CELL_DISCLOSURE_HEIGHT_DP` / `CELL_ROW_HORIZONTAL_PADDING_DP` で描画）。定数の切り出しは `CellBaseLayout.kt:87-99`、共通行側の参照は `CommandCellViewHolder.kt:71-72` | AND-R-T:221 `showArrow で Disclosure Indicator が合成される` / AND-R-T:243 `showArrow のとき content の占有幅は indicator の領域を除いた範囲になる`（`ROW_WIDTH_PX - (18+16)dp` の完全一致）。ピクセル実測は `android-pixel6a-04b-chevron-zoom.png`（CommandCell と bbox 18×30px・x 1014–1031・右端余白 48px が一致、最大画素差 10） | ✅ |

### Scenario: 既定では表示されない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellHostedContent.swift:37`（`showArrow` false なら chevron を組まない）/ `CustomCellView.swift:76-77` `.margins(.all, 0)` の full-bleed | iOS-T:513 `test_showArrow既定ではcontentが行全域を占有する`（probe の幅 == 行幅、誤差 0.5pt） | ✅ |
| Android | `CustomCellViewHolder.kt:157`（`if (showArrow)`） | AND-R-T:230 `既定では Disclosure Indicator は表示されず content が行全域を占有する`（`probe.size.width == ROW_WIDTH_PX`） | ✅ |

### Requirement 本文「showArrow は onTap と独立に指定できる」

| P | テスト | 状態 |
|---|---|---|
| iOS | iOS-T:569 `test_showArrowはonTapと独立に指定できる` | ✅ |
| Android | AND-DSL-T:53 `拡張関数の引数が CustomCell に渡る`（`showArrow = true` と `onTap` を独立に受ける）/ AND-R-T:221（onTap なしで indicator が出る） | ✅ |

---

## 7. Requirement: スタイルの適用範囲 (ADDED)

### Scenario: hasUnevenRows が true なら cellHeight は最低高として働く

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:64` `KsCellViewSupport.applyEffectiveHeight` / `:92-97` `preferredLayoutAttributesFitting` → `adjustedLayoutAttributes` | iOS-T:645 `test_hasUnevenRowsがtrueならcellHeightは最低高として働く`（cellHeight 60 / content 200 で行が 60 を超えることを実測） | ✅ |
| Android | `CustomCellViewHolder.kt:139-145` `Modifier.heightIn(min = heightDp.dp)` + `:95` `applyEffectiveHeight` | AND-R-T:435 `hasUnevenRows が true なら cellHeight は最低高として働く`（content 40dp → 100dp / content 200dp → 200dp の両方向を実測）。手段の追加は ⚠️ `deviation.md`「Decision 5 (Android の高さ適用)」に記録済み（design 乖離であり spec 挙動は一致） | ✅ |

### Scenario: hasUnevenRows が false なら cellHeight で固定できる

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | 同上（`EffectiveStyle` 経由） | iOS-T:662 `test_hasUnevenRowsがfalseならcellHeightで固定される`（cellHeight 90 / content 200 → 90pt に固定） | ✅ |
| Android | `CustomCellViewHolder.kt:140-142` `Modifier.height(heightDp.dp)` | AND-R-T:470 `hasUnevenRows が false なら cellHeight で固定できる`（layoutParams.height と measuredHeight の両方を実測） | ✅ |

### Scenario: テキスト系スタイルは content に影響しない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:47-64`（`EffectiveStyle` から参照するのは `cellBackgroundColor` と実効高さのみ。テキスト系項目は参照しない） | iOS-T:594 `test_テキスト系styleはcontentの見た目に影響しない`（titleColor / titleFont / descriptionColor / valueTextColor / hintTextColor を変えた 2 Cell の**レンダリング PNG が完全一致**。builder 側で色を変えると PNG が変わる mutation probe 付き） | ✅ |
| Android | `CustomCellViewHolder.kt:47-51`（`applyCellBackground` のみ。テキスト系項目は参照しない） | AND-R-T:597 `テキスト系スタイルは content の描画に影響しない`。**注**: composition の testTag 構成と probe の占有幅の一致までで、iOS のような画素比較ではない（Robolectric の限界）。実装が `EffectiveStyle` のテキスト系項目を一切参照していないコード事実と、`ui/verification/compare-01` の左右比較で補完 | ✅ |

### 行レベル項目（背景色）

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:53-55` | iOS-T:581 `test_style_backgroundColorが行に適用される` | ✅ |
| Android | `CustomCellViewHolder.kt:51` `applyCellBackground` | AND-R-T:583 `背景色は行レベルの style として適用される`（Ripple ラップを解いた実塗り色を実測） | ✅ |

### Requirement 本文「既存 DSL の style / cellHeight modifier チェーンからも同様に機能する」

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCell.swift:239` `withStyle(_:)`（`DSLStyleModifiable` 準拠） | iOS-DSL-T:62 `test_CustomCellにstyle_modifierチェーンが効く`（`.cellHeight(120).backgroundColor(.systemTeal)`） | ✅ |
| Android | `CustomCell.kt:72` `withDSLStyle`（`DSLStyleModifiableCell` 準拠） | AND-DSL-T:76 `CellHandle chain の cellHeight が CustomCell の style に反映される` / AND-T:152 | ✅ |

---

## 8. Requirement: 可視性フィルタへの参加 (ADDED)

### Scenario: isVisible=false で行が現れない

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCell.swift:57`（`VisibilityAware` 準拠）/ `:104` `isVisible` | iOS-T:838 `test_isVisibleがfalseなら行がsnapshotから除外され並びが詰まる`（data source snapshot の itemIdentifiers 順序まで確認）/ iOS-T:857 | ✅ |
| Android | `CustomCell.kt:58`（`VisibilityAware` 準拠）/ `:56` `isVisible` | AND-T:181 `isVisible false の CustomCell は visible projection から除外される`（`KsSettingsView.flatten` の結果が `["v", "after"]`）/ AND-T:174 | ✅ |

---

## 9. Requirement: 高さの自動追従 (ADDED)

### Scenario: content の展開で行高さが追従する

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCellView.swift:76` `UIHostingConfiguration`（self-sizing）/ `:92-97` `preferredLayoutAttributesFitting` / `ios/Sources/KsSettingsViewUI/CustomCellRowPlacement.swift:31`（遷移中の縦位置） | iOS-T:701 `test_content同値のままbuilder内部の状態変化で行高さが追従する`（**再バインド API を一切呼ばず**、builder 内 `ObservableObject` の変化だけで行高さが伸び・縮みすることを `cellForItem(at:).frame.height` で実測）/ iOS-T:678（content 自然高への追従）。**注**: 「後続の行の位置も更新される」は単体テストでは検証しておらず、`ui/verification/ios-sim-iphone17-ios265-02-dynamic-height-expanded.png` / `-10-post-fix-...` で実証 | ✅ |
| Android | `CustomCellViewHolder.kt:143` `heightIn(min=)` + `ComposeView` の `onMeasure` 委譲 | AND-R-T:552 `builder 内部の状態変化だけでも行高さが追従する`（`remember { mutableStateOf }` を外から toggle し measuredHeight が 80dp→240dp）/ AND-R-T:534。後続行の押し下がりは `android-pixel6a-02-dynamic-height-expanded.png` / `android-pixel4a-02-...` で実証 | ✅ |

### Requirement 本文「専用の再計測 API を要求しない」

両プラットフォームとも上記テストが「再計測 API を呼ばずに」高さが変わることを実証している。公開 API にも再計測用のメソッドは追加されていない（`CustomCell.swift` / `CustomCell.kt` に該当 API なし）。✅

**content が行に収まらないときの縦位置**（iOS `CustomCellRowPlacement` / Android `CenterOrTopVertically`）: ⚠️ `deviation.md`「spec 未規定事項の実装判断」に記録済み。テストは iOS-T:750 / iOS-T:775、AND-R-T:493。

---

## 10. Requirement: DSL による配置 (ADDED)

### Scenario: Android DSL で直置きできる

| 実装 | テスト | 状態 |
|---|---|---|
| `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/CustomCellDsl.kt:71`（content あり形）/ `:102`（content なし形）。いずれも `CellHandle` を返す | AND-DSL-T:29 `content ありの拡張関数で CustomCell が DSLCellNode に格納される` / AND-DSL-T:42（content なし形）/ AND-DSL-T:53（全引数の伝播）/ AND-DSL-T:76（`CellHandle.cellHeight` が効く）/ AND-DSL-R-T:34 `Registry 未操作でも DSL 直置きの CustomCell が行として反映される`（**行として表示される**ところまで end-to-end） | ✅ |

### Scenario: iOS DSL で直書きできる

| 実装 | テスト | 状態 |
|---|---|---|
| 専用コードなし（`CustomCell` が `KsCell` に準拠するため `KsSettingsViewBuilder` / `SectionBuilder` がそのまま受理する）。`CustomCell.swift:57` の `DSLReidentifiable` / `DSLStyleModifiable` 準拠が modifier チェーンと安定 ID を担保 | iOS-DSL-T:43 `test_SectionBuilderにCustomCellをイニシャライザ直書きで配置できる`（content あり / なしの両形が既存 Cell に挟まれて配置される）。「行が表示される」ところは iOS-T:293 が `KsSettingsViewController` 実経路で実証 | ✅ |

### Requirement 本文「id 省略時の同一性は既存 DSL 規約に従う」

| P | テスト | 状態 |
|---|---|---|
| iOS | iOS-DSL-T:82 `test_id省略時のCustomCellは2回評価で同じIDになる` | ✅ |
| Android | AND-DSL-T:103 `id 省略時は安定位置から採番される` / AND-DSL-T:86（`cellID` で位置移動を跨いで安定）/ AND-T:111 | ✅ |

### Requirement 本文「icon modifier は CustomCell に適用できない（型として非対応）」

| P | 実装 | テスト | 状態 |
|---|---|---|---|
| iOS | `CustomCell.swift:57`（`DSLIconModifiable` に準拠しない） | iOS-DSL-T:73 `test_CustomCellはDSLIconModifiableに準拠しない` | ✅ |
| Android | `CustomCell.kt:58`（`DSLIconModifiableCell` に準拠しない） | AND-T:164 / AND-DSL-T:116 `icon modifier は CustomCell に効かない` | ✅ |

---

## 追加検査

### tasks.md の完了状況と虚偽チェック

| 項目 | 結果 |
|---|---|
| 1.1〜1.6（iOS 実装） | ✅ 全て実体を確認（`CustomCell.swift` / `CustomCellView.swift` / `KsCellRegistry+CustomCell.swift` / `KsSettingsViewController.swift:190` / `CustomCellTests.swift`） |
| 2.1〜2.6（Android 実装） | ✅ 全て実体を確認（`CustomCell.kt` / `CustomCellViewHolder.kt` / `KsCellRegistryCustomCell.kt` / `KsSettingsView.kt:224` / `CustomCellDsl.kt` / 各テスト） |
| 3.1〜3.3（Sample デモ） | ✅ `samples/ios/KsSettingsViewSample/CustomCellDemoView.swift`（373 行・5 セクション）/ `samples/android/.../CustomCellDemoScreen.kt`（398 行）/ 両 `SampleSliderCell`。メニュー導線も `SampleScreen.swift:20,34,43,62` / `SampleScreen.kt:23,47` に存在 |
| 4.1 / 4.2（視覚照合・動的高さ） | ✅ `ui/verification/` に 40 件超の証跡と `index.md` のキャプション、`ui/brief.md` の照合結果を確認 |
| 4.3（本作業） | 未チェック — 本 verify がその実施 |

**未実装なのにチェック済みの虚偽は検出されなかった。**

### 逆流検査（足場アーティファクトの書き換え）

| 対象 | 結果 |
|---|---|
| `specs/cell-types-custom/spec.md` / `proposal.md` / `design.md` | ✅ **逆流なし**。`git status --short` に現れず、`git log` でも提案作成コミット `e214267` 以降の変更なし |
| `tasks.md` | チェックボックス更新と完了メモの追記のみ（1.2 の記述に「opacity 0.38 の淡色化」が追記されているが、これは deviation.md に記録済みのオーナー指示の反映。tasks.md は ksn-core の凍結対象（proposal / design / specs）に含まれないため違反ではない） |
| `ui/brief.md` | 「照合結果」「トークン候補」「合意済み妥協」の追記のみ（ksn-ui の正常な運用） |

### 未記録乖離

**なし。** ❌ 判定はゼロ。spec 本文と実装が異なる箇所（無効時の淡色化 / 無効時の TalkBack 読み上げ / content が行に収まらないときの縦位置 / iOS の Renderer 基底 / Android の高さ解決手段）はいずれも `deviation.md` に記録済み。

### UI 変更のゲート

- `ui/brief.md:32` に承認モックの記録あり（`mock/plan-a.html` を採用、`approved.png`、2026-08-03 ユーザー承認）。✅
- 合意済み妥協 6 件が `ui/brief.md`「合意済み妥協」節に記録済み。✅

### テスト実行（本 verify で再実行）

| 対象 | コマンド | 結果 |
|---|---|---|
| iOS 全体 | `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,id=<ios-simulator-udid>'` | **395 tests / 0 failures**（`** TEST SUCCEEDED **`） |
| iOS CustomCell 系 | 同上 + `-only-testing:KsSettingsViewUITests/CustomCellTests -only-testing:KsSettingsViewSwiftUITests/CustomCellDSLTests` | **36 + 8 = 44 tests / 0 failures**（`#if canImport(UIKit)` 配下がシミュレータ destination で実際に実行されていることを個別に確認） |
| Android 全体 | `./gradlew test --rerun-tasks`（キャッシュを避けるため強制再実行） | `BUILD SUCCESSFUL`。release variant 924 tests / 0 failures / 0 errors（debug variant と合わせて 1848） |
| Android CustomCell 系 | 上記結果の XML | `CustomCellTest` 15 / `CustomCellRenderingTest` 25 / `CustomCellDslTest` 11 / `CustomCellDslRenderingTest` 1 = **52 tests / 0 failures** |

---

## 判定: VALID

- 全 10 Requirement / 21 Scenario について、iOS / Android 双方の実装とテストの対応が取れている（❌ ゼロ）
- spec 本文からの差分はすべて `deviation.md` に記録済み（⚠️ 5 件）
- tasks.md の虚偽チェックなし、足場アーティファクトへの逆流なし
- テストは本 verify で再実行し全件成功

### 単体テストが代理検証にとどまり、実機・シミュレータ証跡で補完している Scenario（参考。判定には影響しない）

| Scenario | 単体テストの範囲 | 補完している証跡 |
|---|---|---|
| 子要素の操作では行タップが発火しない（iOS） | 「二重発火し得る構造がない」ことの構造検証 | `ios-sim-iphone17-ios265-05-child-tap-does-not-fire-row-ontap.png` |
| 既定では行タップ動作を持たない（iOS） | `tapHandler` nil / `isUserInteractionEnabled` true | `ios-sim-iphone17-ios265-07-slider-enabled-dragged-disabled-blocked.png` |
| 再利用時に前の内容が残らない（iOS） | `prepareForReuse` 後の解放状態 | `ios-sim-iphone17-ios265-06-scroll-recycle-tap.png` |
| 無効時は content 内の操作も抑止される（Android） | Robolectric 上の semantics action 実行 | `android-pixel6a-14-a11y-tree-disabled-row.xml` / `android-pixel4a-04-a11y-tree-disabled-row.xml`（実機 `uiautomator dump`） |
| テキスト系スタイルは content に影響しない（Android） | composition 構成と占有幅の一致 | `compare-01-disabled-dimming-and-slider-accent-ios-vs-android.png`（iOS 側は PNG 画素比較テストで担保） |
| showArrow で indicator が表示される（両） | 占有幅の減少 + 共有定数による構造担保 | `ios-...-03b-chevron-zoom.png` / `-11-...` / `android-pixel6a-04b-chevron-zoom.png`（CommandCell との bbox・余白・画素差の実測） |
| content の展開で行高さが追従する（両。後続行の位置更新） | 行高さの変化のみ | `ios-...-02` / `-10` / `android-pixel6a-02` / `android-pixel4a-02` |
