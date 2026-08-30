# Verify 001: fix-cell-accessory-vertical-fill

- 判定: **VALID**
- 対象: `specs/settings-view-ios-host/spec.md` (MODIFIED / 6 Scenario)、`specs/cell-types-basic/spec.md` (ADDED / 4 Scenario)
- deviation.md: 存在しない (未記録乖離の検出もなし)
- テスト: `xcodebuild test -scheme KsSettingsView-Package -destination 'platform=iOS Simulator,name=iPhone 17 Pro'` → **337 tests / 0 failures** (TEST SUCCEEDED)

---

## 1. 対応表: settings-view-ios-host (MODIFIED)

Requirement: 「KsListCellBase の自前 UIStackView 階層」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 初期化直後の subview hierarchy | `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:90-99` (accessoryHolder 定義・初期 `isHidden = true`) / `:139-199` (`installBaseLayout()` で `stackH = [iconImageView, stackV, accessoryHolder]`、`stackV = [contentStack, descriptionLabel]`、`contentStack = [titleLabel]`、stackH の 4 anchor を contentView とイコール) | `ios/Tests/KsSettingsViewUITests/UnifyCellCommonFieldsTests.swift:520` `test_KsListCellBase_subviewHierarchy_AiForms準拠` (arrangedSubviews の同一性・count 3・空 holder の isHidden まで assert) | ✅ 一致 |
| accessoryView が accessoryHolder に配置される | `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:150` (`listCell.setAccessoryView(accessoryView)`) / `KsListCellBase.swift:237-252` (`setAccessoryView`) / `CellBaseLayout.swift:65-66` (`contentConfiguration = nil` / `accessories = []`) | `UnifyCellCommonFieldsTests.swift:586` `test_applyCellBaseLayoutはaccessoryViewをaccessoryHolderに配置する` (holder に 1 個・`isHidden == false`・contentStack に含まれない・`contentConfiguration == nil` / `accessories.isEmpty` まで assert) | ✅ 一致 |
| accessoryView が nil なら accessoryHolder は空で隠れる | `KsListCellBase.swift:243-247` (nil 時に holder を空にして `isHidden = true`) | `UnifyCellCommonFieldsTests.swift:613` `test_applyCellBaseLayoutはaccessoryViewがnilならholderを空にして隠す` / `BasicCellsTests.swift:157` `test_CommandCellView_hideArrow_trueでdisclosure非表示` (renderer 経路での nil 系統) | ✅ 一致 |
| 再 render でアクセサリが蓄積しない | `KsListCellBase.swift:239-250` (旧内容を必ず除去、同一インスタンス再指定時は付け替えない) | `UnifyCellCommonFieldsTests.swift:634` `test_再renderでaccessoryHolderの内容は常に0個または1個` (A→B→nil、同一インスタンス連続、chevron の false→true→false 3 周を網羅) | ✅ 一致 |
| レイアウト後の幾何関係 (description とアクセサリの非交差・垂直センター) | `KsListCellBase.swift:106` (`stackH.alignment = .center`) / `:176-179` (accessoryHolder の Hugging・CCR を `.required` にして自然幅を保持) / `:184-189` (stackH anchors == contentView anchors) | `UnifyCellCommonFieldsTests.swift:687` `test_レイアウト後にdescriptionはアクセサリと交差せずアクセサリは垂直センター` (幅 320pt で `descFrame.maxX <= holderFrame.minX`、`holderFrame.midY ≈ contentView.midY` (±1.0)、長文が実際に複数行折り返すこと、nil 時に `stackV.maxX` が trailing margin まで広がることを assert) | ✅ 一致 |
| render 後の行内 trailing 配置 | `CellBaseLayout.swift:124` (`clearContentStackTrailingViews()`) / `:128-146` (valueLabel → trailingViews の順で `contentStack` へ addArrangedSubview) | `UnifyCellCommonFieldsTests.swift:168` `test_applyCellBaseLayoutは_contentStack並び順を正しく組む` (`[titleLabel, valueLabel, custom]`) / `:193` `test_applyCellBaseLayoutは値テキストのみの場合trailingViewsなし` | ✅ 一致 |
| prepareForReuse で行内 trailing と accessory が除去される | `KsListCellBase.swift:311-326` (`prepareForReuse` で text/image を nil クリア → `clearContentStackTrailingViews()` → `setAccessoryView(nil)`。first responder 保護は `:216-225` に既存のまま維持) | `UnifyCellCommonFieldsTests.swift:542` `test_prepareForReuseで_行内trailingとアクセサリが除去される` (contentStack が `[titleLabel]` に戻る・holder 空 + isHidden・text/image の nil クリア・`stackH` / `stackV` の恒常メンバー階層が非破壊であることまで assert) | ✅ 一致 |

### Requirement 本文の個別 MUST 検査 (Scenario 外)

| MUST 項目 | 実装 | 状態 |
|---|---|---|
| `accessoryHolder` は内容の自然幅を保ち伸縮しない | `KsListCellBase.swift:176-179` | ✅ |
| `hintLabel` は `contentView` ではなく `self` に addSubview し右上 float を維持 | `KsListCellBase.swift:279-287` (本 change で未変更) | ✅ |
| minHeight 制約の既存挙動維持 | `KsListCellBase.swift:117` の `stackHMinHeightConstraint` は本 change で未変更 | ✅ |
| 派生 renderer が恒常階層を除去・置換しない | 全 renderer は `applyCellBaseLayout` / `accessoryView` 経由のみ。`stackH` / `stackV` / `contentStack` から恒常メンバーを除去するコードは存在しない | ✅ |
| `contentConfiguration` を non-nil にしない / `accessories` を非空にしない | `CellBaseLayout.swift:65-66` で毎回リセット。renderer 側に設定箇所なし (`KsSettingsViewController.swift` の `accessoryView` は section supplementary の別概念で本 Requirement の対象外) | ✅ |

## 2. 対応表: cell-types-basic (ADDED)

Requirement: 「Cell 級アクセサリと行内 trailing の 2 系統配置」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| SwitchCell の description がアクセサリの下に回り込まない | `ios/Sources/KsSettingsViewUI/SwitchCellView.swift:54` (`accessoryView: toggle`) + 幾何は `KsListCellBase.swift:106,176-179` | `BasicCellsTests.swift:246` `test_SwitchCellView_bindで状態が反映される` (UISwitch が holder に入り contentStack には入らない) + `UnifyCellCommonFieldsTests.swift:687` (長文 description × UISwitch の非交差・垂直センターの幾何) | ✅ 一致 |
| Picker 系は valueText が行内・chevron が Cell 級 | `PickerCellView.swift:43-44` / `NumberPickerCellView.swift:70-71` / `TimePickerCellView.swift:65-66` / `DatePickerCellView.swift:76-77` (いずれも `valueLabelText:` + `accessoryView: makeChevronView()`)。`EmbeddedPickerHostField` 系の contentView 背面配置は未変更 | `InputCellsTests.swift:237` 共通ヘルパ `assertPickerValueInlineAndChevronInAccessory` + `:273` / `:280` / `:287` / `:295` の 4 種別テスト (value label が contentStack に 1 個・chevron が holder に 1 個・chevron が contentStack に無いことを assert) | ✅ 一致 |
| EntryCell の入力フィールドは行内のまま (iOS) | `EntryCellView.swift:170` (`trailingViews: [fieldWrapper]` のまま。本 change で未変更 — git status に現れない) | `InputCellsTests.swift:305` `test_EntryCellView_入力フィールドは行内でアクセサリ列は空` (`_textField.isDescendant(of: contentStack)`・holder 空 + isHidden) | ✅ 一致 |
| Android は既存構造で本要件を満たす | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:194` (`descriptionView.END = accessoryHolder.START`) / `:210-212` (`accessoryHolder` の TOP・BOTTOM = parent → 縦中央) / `SwitchCellViewHolder.kt:149` (MaterialSwitch を `accessoryHolder` へ addView)。Android ソースは本 change で 1 行も変更されていない (git status で確認) → spec の「実装変更は不要」の主張は妥当 | `android/.../test/kotlin/.../UnifyCellCommonFieldsTest.kt:396` `accessoryHolder はセル縦中央配置` (SwitchCell を 320dp で measure/layout し `centerY` 一致を ±1px で assert) | ✅ 一致 (下記 3-(2) の注記あり) |

### Requirement 本文の個別検査

| 項目 | 実装 | 状態 |
|---|---|---|
| Cell 級アクセサリ 9 種の振り分け | Switch (`SwitchCellView.swift:54`) / Checkbox (`CheckboxCellView.swift:56`) / Radio (`RadioCellView.swift:56`) / SimpleCheck (`SimpleCheckCellView.swift:52`) / Command (`CommandCellView.swift:25,37`) / Picker 4 種 — 計 9 種すべてが `accessoryView` 経由。`trailingViews:` 渡しは残っていない | ✅ |
| アクセサリ無し 3 種は無変更 | `ButtonCellView.swift` / `LabelCellView.swift` / `EntryCellView.swift` は git status に現れず未変更 (proposal の Non-Goals どおり) | ✅ |
| Android EntryCell は accessory 領域配置を維持 | `EntryCellViewHolder.kt:244` は変更なし | ✅ |

## 3. 追加検査

### (1) tasks.md の虚偽チェック

全 14 タスク (1.1-1.2 / 2.1-2.2 / 3.1-3.5 / 4.1-4.6 / 5.1-5.2) がチェック済み。上記対応表と突き合わせ、**未実装なのにチェック済みの項目は検出しなかった**。

- 4.6 「全件 `swift test` pass」: 本検証で `xcodebuild test` により 337 tests / 0 failures を実測し裏付け済み
- 5.1 / 5.2 (Simulator での視覚照合) は成果物アーティファクトが残らない性質のタスクだが、`review-001.md` にレビュアーが独立に Simulator で確認した記録があり、虚偽とは判定しない

### (2) 未記録乖離

**なし**。❌ 判定の Scenario がないため、deviation.md の不在は妥当。

参考 (乖離ではない観察):

- cell-types-basic の Scenario「Android は既存構造で本要件を満たす」のうち、`descriptionView.END = accessoryHolder.START` に対応する **幾何テストは Android 側に存在しない** (縦中央の方は `UnifyCellCommonFieldsTest.kt:396` にある)。ただし本 Scenario は「既存実装の挙動を契約として明文化する。実装変更は不要」と spec 自身が宣言しており、実装 (ConstraintSet の宣言そのもの) が契約を満たしているため一致検証としては ✅。テスト追加は品質上の任意事項 (ksn-review の領分)
- chevron が `contentStack` (spacing 6) から `accessoryHolder` (stackH spacing 16) へ移ったことによる CommandCell / Picker 系の間隔変化 (約 6pt → 16pt) は、デルタスペックが「spacing・margin 等の視覚パラメータは本 spec の対象外」と明記し `ui/brief.md:24` も mock の規範範囲を配置関係のみに限定しているため、仕様逸脱ではない

### (3) 逆流検査 (足場アーティファクトの書き換え)

- 足場 (`specs/` `proposal.md` `ui/` `exploration.md`) はいずれも提案コミット `3c24f3c` のまま。`git status` 上で変更されているのは `tasks.md` (チェック更新のみ) と実装・テストファイルだけ
- `3c24f3c` 以降に追加コミットはなく、実装期間中の足場書き換えは**なし**

### (4) UI 変更の検査

- `ui/brief.md:26-31` に承認モックの記録あり (`mock/plan-a.html` を採用、`approved.png`、2026-08-01 ユーザー承認)。plan-b 不採用理由も記録済み
- `ui/brief.md:24` に mock の規範範囲 (配置関係のみが規範、spacing・寸法・フォント・配色の生値は非規範) が明記されており、合意済み妥協として機能している

### (5) テスト実行

```
Test Suite 'All tests' passed
Executed 337 tests, with 0 failures (0 unexpected)
** TEST SUCCEEDED **
```

(macOS ホストの `swift test` では `#if canImport(UIKit)` ガードにより UI テストがコンパイル対象外となるため、iOS Simulator 宛の `xcodebuild test` で実行。Android は本 change で 1 行も変更していないため未実行)

---

## 4. 追加確認: オーケストレーターによる直接修正 1 箇所

対象: `ios/Sources/KsSettingsViewUI/CellBaseLayout.swift:6-8` — ファイル冒頭の恒常メンバー列挙に `accessoryHolder` を追加。

```
// `KsListCellBase` が `init(frame:)` で install した自前 UIStackView 階層
// （`stackH` / `stackV` / `contentStack` / `iconImageView` / `titleLabel` / `descriptionLabel` /
// `accessoryHolder`）の各 subview を更新する形で Cell の見た目を構成する。
```

### 観点 (1): 列挙の内容が `KsListCellBase` の実際の恒常メンバーと一致するか → **一致**

`KsListCellBase` が `init(frame:)` の `installBaseLayout()` で install する stack 階層メンバーは 7 個:
`iconImageView` (`:34`) / `titleLabel` (`:43`) / `descriptionLabel` (`:51`) / `contentStack` (`:64`) / `stackV` (`:74`) / `accessoryHolder` (`:90`) / `stackH` (`:103`)。
コメントの列挙 7 個と過不足なく一致する。

`hintLabel` (`:60`) が列挙にないのは正しい — `hintLabel` は `ensureHintLabel()` による lazy 生成で、かつ `contentView` ではなく `self` 直下に置かれる (UIStackView 階層の外) ため、「自前 UIStackView 階層」の列挙に含めるべきものではない。デルタスペックの階層図 (`specs/settings-view-ios-host/spec.md:11-22`) も `hintLabel` を階層外に置いている。

### 観点 (2): `kasane/concepts/cross/conventions/comment-policy.md` への違反 → **違反なし**

- 追加された `accessoryHolder` は**リポジトリ内のコード識別子**であり、規約が「外部参照ではなく自由に書いてよい」と定めるもの (comment-policy.md:17)
- 禁止参照 (変更提案 ID の裸参照 / Phase・Decision 通番 / アーカイブ文書パス / 拡張子なし裸参照)、禁止記述類型 (履歴記述 / 過去仕様説明 / デルタスペック構文キーワード / レビュー通番) のいずれにも該当しない
- 現在形の構造説明であり、規約が求める「そのファイルだけを読んでいる人にとって意味が通る」形式を満たす

なお、同ファイル `:9` および `:34` に残る `(MUST NOT)` はデルタスペック構文キーワードの混入 (comment-policy.md:38) にあたるが、これは本 change が持ち込んだ文言ではない既存違反であり、本検証のスコープ外として指摘に含めない。

---

## 5. 結論

**VALID** — 全 10 Scenario が「✅ 一致」。❌ は 0 件。虚偽チェックなし、足場の逆流なし、テスト 337 件全通過。オーケストレーターによる直接修正 1 箇所も内容・規約の両面で妥当。アーカイブ (蒸留) に進んで差し支えない状態と判定する。
