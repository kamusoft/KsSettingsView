# 一致検証: fix-android-cell-width-allocation (002 回目)

**日付**: 2026-08-01
**判定**: VALID

前回 (`verify-001.md` / VALID) からの再検証。1 周目のレビュー指摘に対する修正 (A〜F) が入った後の状態で、
デルタスペックの全 Requirement / Scenario と実装・テストの対応を機械的に取り直した。

## テスト実行

```
cd android && ./gradlew test --rerun-tasks
→ BUILD SUCCESSFUL (166 actionable tasks: 166 executed)
```

- 実行テスト数: **535 件 / failures 0 / errors 0 / skipped 0** (debug variant。release variant も同数成功)
- `CellRowWidthAllocationTest`: **15 件すべて成功** (1 周目の 12 件から 3 件増)
- 注: 初回の `./gradlew test` は全タスク UP-TO-DATE で**実際には実行されなかった**ため、
  `--rerun-tasks` を付けて再実行した結果を上記の正とする。

## 対応表: settings-view-android-ui / Requirement「共通行の主行幅配分」

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| EntryCell の入力フィールドが残り幅全体を占める | `CellBaseLayout.kt:293-322` (`addFillingInlineTrailing`) / `EntryCellViewHolder.kt:245` | `CellRowWidthAllocationTest.kt:116` | ✅ 一致 |
| パスワード入力でも同じ配分になる | 同上 (`isPassword` は `inputType` のみ変更) | `CellRowWidthAllocationTest.kt:153` | ✅ 一致 |
| 入力フィールドの幅が固定の最低幅に依存しない | `EntryCellViewHolder.kt:237-246` (`minWidth = 160dp` ハック撤去) | `CellRowWidthAllocationTest.kt:186` (`minWidth == 0` + 幅 0 まで縮むこと) | ✅ 一致 |
| 行内 trailing がない場合は title が全幅を使う | `CellBaseLayout.kt:137-151` (title = `0dp + weight=1`) | `CellRowWidthAllocationTest.kt:261` | ✅ 一致 |
| valueText はコンテンツ幅で title が残り幅を占める | `CellBaseLayout.kt:166-178` (valueText = `wrap_content`) | `CellRowWidthAllocationTest.kt:286` | ✅ 一致 |
| 行幅を超える valueText は末尾省略される | `CellBaseLayout.kt:174-176` (`isSingleLine` + `ellipsize END`) | `CellRowWidthAllocationTest.kt:339` (`getEllipsisCount(0) > 0`) | ✅ 一致 |

Requirement 本文の「title と行内 trailing は互いの表示領域に重なってはならない」は
`CellRowWidthAllocationTest.kt:328-331` / `:546-548` / `:710-714` が `title.right <= trailing.left` で直接検証。

**1 周目からの変化**: 末尾省略 Scenario の検証が「構成 (maxLines / ellipsize) + 表示幅制約」から
`@GraphicsMode(NATIVE)` 下の `Layout.getEllipsisCount(0) > 0` (実レンダリング) へ**強化**された
(`CellRowWidthAllocationTest.kt:85-102`)。テストが成功していることで、この技術判断
(legacy graphics では `getEllipsisCount` が常に 0 / NATIVE では実省略が起きる) は成立していると確認した
— アサーションが `> 0` である以上、省略が起きなければ失敗する。

## 対応表: cell-types-basic / Requirement「Cell 級アクセサリと行内 trailing の 2 系統配置」(MODIFIED)

| Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| SwitchCell の description がアクセサリの下に回り込まない | `CellBaseLayout.kt:239` (`description.END = accessoryHolder.START`, 無変更) / `:253-256` (accessory 縦中央, 無変更) | **`CellRowWidthAllocationTest.kt:462` (新規)** | ✅ 一致 |
| Picker 系は valueText が行内・chevron が Cell 級 | valueText = `contentRow` の子 / chevron = `accessoryHolder` の子 | **`CellRowWidthAllocationTest.kt:507` (新規)** | ✅ 一致 |
| EntryCell の入力フィールドは行内に置かれる (Android) | `EntryCellViewHolder.kt:245` (`addFillingInlineTrailing`) | `CellRowWidthAllocationTest.kt:233` | ✅ 一致 |
| EntryCell の入力フィールドは行内のまま (iOS — 確認のみ) | `ios/Sources/KsSettingsViewUI/EntryCellView.swift:170` (`trailingViews: [fieldWrapper]`、**iOS に diff なし**) | 既存 `ios/Tests/KsSettingsViewUITests/InputCellsTests.swift:230-` | ✅ 一致 |

**1 周目からの変化**: 上 2 Scenario は verify-001 では「実装は共通制約により充足 / 直接の自動テストは
実機証跡のみ」だったが、今回**専用の Robolectric テストが 2 本追加**され、
`descriptionView.right <= accessoryHolder.left`・`contentRow.right <= accessoryHolder.left`・
accessory の縦中央 (±1px)・valueText と chevron の親の別 (`contentRow` / `accessoryHolder`) を
直接測るようになった。相方レビュー (`second-opinion-002.md`) の Minor 指摘が閉じている。

## 追加検査

### tasks.md の虚偽チェック検査

全 15 タスクが `[x]`。対応表と突き合わせた結果、**虚偽のチェックはない**。

| タスク | 根拠 |
|---|---|
| 1.1 / 1.2 | `CellBaseLayout.kt:127-178` (contentRow 導入 + 既定配分) |
| 1.3 | `CellBaseLayout.kt:132-134` (`isBaselineAligned`) / `:250-252` (縦チェーン head を `contentRow` へ) |
| 2.1 / 2.2 | `EntryCellViewHolder.kt:237-246` |
| 2.3 | `EntryCellViewHolder.bind` に差分なし。`InputCellsTest.kt` 全件成功 (差分ガード含む) |
| 2.4 | `ButtonCellViewHolder.kt:43-70` (ConstraintSet 対象を `contentRow` へ) |
| 3.1 / 3.2 / 3.3 | `CellRowWidthAllocationTest.kt` 15 件 |
| 3.4 | `BasicCellsTest.kt` / `InputCellsTest.kt` / `UnifyCellCommonFieldsTest.kt` の追随差分。全 535 件成功 |
| 4.1〜4.4 | `ui/references/current-kssettingsview.png` + `ui/verification/` 5 枚 + `ui/brief.md` の照合表 |

### 逆流検査 (足場アーティファクトの書き換え)

`git diff --stat` で確認:

- `proposal.md` / `exploration.md` / `specs/settings-view-android-ui/spec.md` / `specs/cell-types-basic/spec.md`
  → **差分なし** (逆流なし)
- `tasks.md` → チェックボックスのみ (規約どおり)
- `ui/brief.md` → 照合結果・実機実測・記述訂正の追記 (UI 記録として規約どおり)
- `deviation.md` → 新規 4 件 (すべてオーナー承認済みと明記)
- `review-001.md` / `verify-001.md` / `second-opinion-002.md` → 未変更

### 未記録乖離の洗い出し

対応表に ❌ なし。deviation.md 記録済みの 4 件 (title 1 行化 / ButtonCell の titleAlignment 実効化 /
6dp クリアランス / 原典期待スクリーンショット未取得) 以外に、spec からの逸脱は検出されなかった。

なお 6dp クリアランス追加によって「title 幅 + 行内 trailing 幅 = 主行幅」の等式が崩れていないことを
別途確認した (padding は View 幅に含まれる):

- 自動テスト: `CellRowWidthAllocationTest.kt:715-719` (`rowWidth == titleView.width + valueTextView.width`)
- 実機実測: `ui/brief.md` の照合表 (全行で title 幅 + 入力フィールド幅 = 996px)

### UI 変更の検査

- `ui/brief.md` に承認済み mock の記録あり (`mock/plan-a.html` → `approved.png`、2026-08-01 ユーザー承認)
- 合意済み妥協は `deviation.md` に集約され、brief.md は参照のみ (1 周目の指摘 F(2) が解消)
- 実機証跡 5 枚が `ui/verification/` に存在。うち `after-buttoncell-center-alignment-pixel6a.png` は
  deviation.md 記録の「ButtonCell の titleAlignment 実効化」の実描画証跡として有効であることを目視確認した
  (aux あり「登録」の title 領域内で中央に描画されている)
- 実機撮影のために一時変更したと報告されている `samples/` に**残存差分なし** (`git status` で確認)

## 判定

**VALID** — 全 10 Scenario が ✅ 一致。虚偽チェックなし、逆流なし、テスト 535 件全成功。
`verify-001.md` (VALID) から状態が悪化した点はなく、cell-types-basic の 2 Scenario の
テスト対応が「実機証跡のみ」から「自動テストあり」へ改善している。
