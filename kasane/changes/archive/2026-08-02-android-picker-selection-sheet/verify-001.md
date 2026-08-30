# 検証結果: android-picker-selection-sheet (001 回目)

**日付**: 2026-08-02
**判定**: VALID

デルタスペック `specs/settings-view-android-ui/spec.md` の 6 Requirement / 16 Scenario と、実装 (`PickerSelectionSheet.kt` / `PickerCellViewHolder.kt`) およびテスト (`PickerSelectionSheetTest.kt` / `InputCellsTest.kt`) の対応を機械的に突き合わせた。

パス表記は以下の略記を用いる。

- `sheet` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheet.kt`
- `holder` = `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/PickerCellViewHolder.kt`
- `sheetTest` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/PickerSelectionSheetTest.kt`
- `inputTest` = `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt`

## 対応表 (ADDED Requirements)

### Requirement: PickerCell 選択面の提示

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| タイトルの解決 | `holder:66` (`cell.pageTitle ?: cell.title`) → `sheet:309-317` | `sheetTest:75` `タイトルは pageTitle を優先して解決する` / `sheetTest:87` `pageTitle が null のときタイトルは title を使う` | ✅ 一致 |
| キャンセルは callback を発火しない | `sheet:306` (`setOnClickListener { cancel() }`。callback 発火点は `sheet:343` と `sheet:592` の2箇所のみ) | `sheetTest:139` `キャンセルボタンでは callback を発火しない` | ✅ 一致 |
| 非確定 dismiss は経路によらず callback を発火しない | `sheet:216` (`setCanceledOnTouchOutside(true)`)、確定経路の閉包 (`sheet:342-345` / `sheet:591-594`) | `sheetTest:161` `選択面は外側タップで閉じられる設定になっている` / `sheetTest:167` `下方向スワイプ相当の非表示遷移で閉じても callback を発火しない` (`STATE_HIDDEN` 経由) / `sheetTest:199` `非確定 dismiss はどの経路でも callback を発火しない` (cancel / onBackPressed / dismiss) | ✅ 一致 |
| 候補の全件列挙と displayFormatter の適用 | `holder:61-63` (formatter 適用)、`sheet:733` (`getItemCount = displayItems.size`)、`sheet:720-726` (`bindRow`) | `sheetTest:93` `候補は items の順序どおり全件列挙され displayFormatter が適用される` | ✅ 一致 |
| 操作ラベルは OS リソースから解決される | `sheet:291` (`android.R.string.cancel`) / `sheet:320` (`android.R.string.ok`) | `sheetTest:115` `操作ラベルは OS の公開文字列リソースから解決される` | ✅ 一致 |

Requirement 本文の Scenario 化されていない条項:

- 「`isEnabled` な PickerCell の行タップで」 → 実装 `holder:40-48`、テスト `inputTest:566` `PickerCellViewHolder isEnabled = false のときタップは無効化される` ✅
- 「モーダル提示する」 → `PickerSelectionSheet` は `BottomSheetDialog` (`sheet:129`)。提示自体のテストは `inputTest:549` `PickerCellViewHolder single タップでボトムシートの選択面が表示される` ✅

### Requirement: 単一選択の即時確定

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 項目タップで即確定して閉じる | `sheet:591-594` (`onSingleSelected(index)` → `dismiss()`) | `sheetTest:470` `単一選択は項目タップで即確定して閉じる` (発火1回とシートの閉鎖を検証) | ✅ 一致 |

Requirement 本文「現在の `selectedIndex` に対応する項目へ選択印を表示する」 → 実装 `sheet:572-575` (`isCheckedAt`)、テスト `sheetTest:488` `単一選択は selectedIndex の項目にのみ選択印を表示する` ✅

### Requirement: 複数選択の確定・破棄と上限

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 確定操作で確定する | `sheet:342-345` (`onMultiConfirmed(workingSelection.toSet())` → `dismiss()`) | `sheetTest:513` `複数選択は確定操作で作業状態を1回だけ発火して閉じる` | ✅ 一致 |
| キャンセルで作業状態を破棄する | `sheet:132` (`workingSelection` はシート内ローカル)、`sheet:306` | `sheetTest:550` `複数選択のキャンセルは作業状態を破棄する` | ✅ 一致 |
| 上限到達時は新規チェックを無視して触覚フィードバックを要求 | `sheet:601-603` (上限判定 → `requestRejectFeedback`)、`sheet:620-623` | `sheetTest:568` `上限到達時は新規チェックを無視して拒否の触覚フィードバックを要求する` / `sheetTest:587` `拒否の触覚フィードバックが受け付けられなければ代替を要求する` / `sheetTest:603` `拒否の触覚フィードバックが受け付けられれば代替は要求しない` | ✅ 一致 |
| 上限到達時もチェック解除は可能 | `sheet:597-600` (解除分岐が上限判定より前) | `sheetTest:624` `上限到達時もチェック解除は可能` | ✅ 一致 |

Requirement 本文「候補項目のタップは作業状態のチェックをトグルするのみで callback を発火せず」 → テスト `sheetTest:532` `複数選択の項目タップは callback を発火せず作業状態のみ変える` ✅

### Requirement: 選択印の強調色

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Cell 固有値が最優先される | `sheet:64` (`cell.accentColor?.toArgb() ?: effective.accentColor`) → `sheet:558` | `sheetTest:687` `強調色は Cell 固有値を最優先で解決する` | ✅ 一致 |
| CellStyle へフォールバックする | 同上 (`effective.accentColor` = `CellStyle.accentColor` → `Theme.cellAccentColor` の既存解決) | `sheetTest:702` `強調色は Cell 固有値が無いとき CellStyle へフォールバックする` | ✅ 一致 |
| Theme の既定色へフォールバックする | 同上 | `sheetTest:716` `強調色は Cell 固有値も CellStyle も無いとき Theme へフォールバックする` | ✅ 一致 |

### Requirement: 候補行のアクセシビリティ状態

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 選択状態が公開される | `sheet:536-542` (`isCheckable` / `isChecked = host.isSelected`)、`sheet:723` (`contentDescription`)、`sheet:577-581` | `sheetTest:727` `候補行は表示名と選択状態を公開する` | ✅ 一致 |
| トグル後に公開状態が更新される | `sheet:577-581` (`applyChecked` が `root.isSelected` を更新) | `sheetTest:748` `候補行の公開される選択状態はトグル後に更新される` | ✅ 一致 |

### Requirement: モデル値の許容と非正規化

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| 範囲外 index を含む複数選択の確定 | `sheet:132` (初期集合をそのまま保持)、`sheet:343` (集合をそのまま渡す)、`sheet:601` (上限判定は `workingSelection.size`) | `sheetTest:644` `範囲外 index は作業状態に保持され確定 callback にも残る` | ✅ 一致 |
| 初期上限超過時も新規チェックは無視・解除は可能 | `sheet:596-608` (解除 → 上限 → 追加の順) | `sheetTest:663` `初期状態が上限超過でも新規チェックは無視され解除は可能` | ✅ 一致 |

Requirement 本文の追加条項:

- 「範囲外の `selectedIndex` には選択印を表示しない」 → `sheet:573`、テスト `sheetTest:502` `単一選択の範囲外 selectedIndex では選択印を表示しない` ✅
- 「`items` が空の場合も選択面は提示され、候補は0件となる」 → `sheet:733`、テスト `sheetTest:108` `items が空でも選択面は提示され候補は0件になる` ✅

**MODIFIED / REMOVED Requirements**: なし (デルタスペックは ADDED のみ)。

なお旧 `AlertDialog` 提示 (`showPickerDialog`) はコード・テストとも残骸なし (`grep showPickerDialog / setSingleChoiceItems / setMultiChoiceItems` → 0 件)。旧挙動を検証していた `inputTest` の該当テストはボトムシート提示の検証へ置き換わっている (`inputTest:549`)。

## 追加検査

| 検査項目 | 結果 |
|---|---|
| tasks.md の虚偽チェック | **なし**。チェック済みはグループ1〜3のみで、いずれも上記対応表の実装・テストで裏付けられる。グループ4 (4.1 視覚照合 / 4.2 実機確認) は未チェックのまま (後工程) |
| 逆流検査 (足場の書き換え) | **なし**。`proposal.md` / `specs/` / `ui/` は `3a8562e`(提案記録コミット) 以降の変更なし (`git log` で確認)。作業ツリーの `tasks.md` 差分はチェックボックスのみ。`kasane/decisions/android/0004-*.md` への追補はオーナー承認済みの決定記録であり足場ではない |
| 未記録乖離 | **なし** (対応表に ❌ が1件もないため) |
| deviation.md 記録済み乖離 | 初期スクロール (選択中項目が見える位置で開く)。spec には要求がないため対応表の判定には影響しない。実装 `sheet:465-469` / `sheet:674-676`、テスト `sheetTest:816` / `sheetTest:834` / `sheetTest:941` / `sheetTest:948` |
| UI 変更の承認モック記録 | `ui/brief.md` に承認モック (`mock/plan-b.html`、`approved.png`、2026-08-02 オーナー承認) と不採用案の記録あり。合意済み妥協の記録は現時点で brief 側になし (レビュー側の指摘として `review-003.md` に回す) |
| テスト全件成功 | **成功**。`cd android && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL。`build/test-results/*/TEST-*.xml` 集計で **1176 tests / 0 failures / 0 errors / 0 skipped** (ui 876 + compose 152 + core 148) |

## 判定

**VALID** — 6 Requirement / 16 Scenario (および Requirement 本文の非 Scenario 条項) のすべてに実装とテストの対応があり、❌ はゼロ。虚偽チェック・足場の逆流・テスト失敗のいずれもない。

補足: 本判定はデルタスペック (挙動の契約) との一致のみを対象とする。シートの高さ挙動・全展開の動的挙動は spec ではなく `ui/brief.md` の検証条件と ADR-0005 が正であり、そこに関する所見は `review-003.md` を参照。tasks グループ4 (視覚照合・実機証跡) は未実施のため、アーカイブ可否の最終判断はその完了後となる。
