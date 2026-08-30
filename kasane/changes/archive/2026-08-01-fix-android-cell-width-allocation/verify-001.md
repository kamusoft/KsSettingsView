# 一致検証結果: fix-android-cell-width-allocation (001 回目)

**日付**: 2026-08-01
**判定**: VALID

デルタスペック 2 capability・計 10 Scenario をすべて突き合わせた。❌ は 0 件。
tasks.md の虚偽チェックなし、足場 (proposal / specs) の逆流なし、テスト 382 件すべて成功。

## 実行したコマンド

- `./gradlew test` → BUILD SUCCESSFUL
- `./gradlew :ks-settingsview-ui:test --rerun-tasks` → BUILD SUCCESSFUL
  (`ks-settingsview-ui/build/test-results/testDebugUnitTest/*.xml` 集計: tests=382 skipped=0 failures=0 errors=0)

## 対応表

### specs/settings-view-android-ui — ADDED: 共通行の主行幅配分

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文: 既定 (valueText 系) は valueText コンテンツ幅 / title 残り幅 | `CellBaseLayout.kt:123-155` (`contentRow` = 水平 LinearLayout、`titleView` = `0dp + weight=1`、`valueTextView` = `wrap_content` + singleLine + ellipsize END) | `CellRowWidthAllocationTest.kt:276` | ✅ 一致 |
| Requirement 本文: EntryCell は title コンテンツ幅 / 入力フィールド残り幅 | `CellBaseLayout.kt:285-317` (`addFillingInlineTrailing`)、`EntryCellViewHolder.kt:234-249` | `CellRowWidthAllocationTest.kt:106` | ✅ 一致 |
| Requirement 本文: 行内 trailing なしなら title が全幅 | `CellBaseLayout.kt` (`valueTextView` GONE 時は weight=1 の title が `contentRow` 全幅) | `CellRowWidthAllocationTest.kt:251` | ✅ 一致 |
| Requirement 本文: title と行内 trailing は重ならない (MUST NOT) | LinearLayout の水平配置により構造的に保証 | `CellRowWidthAllocationTest.kt:313`, `:508` (`title.right <= value.left`) | ✅ 一致 |
| Scenario: EntryCell の入力フィールドが残り幅全体を占める | `CellBaseLayout.kt:285-317` | `CellRowWidthAllocationTest.kt:106` `EntryCell の入力フィールドは title の右端から主行の右端までを占める` | ✅ 一致 |
| Scenario: パスワード入力でも同じ配分になる | 同上 (配分は `isPassword` に非依存) | `CellRowWidthAllocationTest.kt:143` | ✅ 一致 |
| Scenario: 入力フィールドの幅が固定の最低幅に依存しない | `EntryCellViewHolder.kt:237-243` (`minWidth = 160dp` 撤去) | `CellRowWidthAllocationTest.kt:176` (`editText.minWidth == 0`、行幅を絞って幅 0 まで縮むことを確認) | ✅ 一致 |
| Scenario: 行内 trailing がない場合は title が全幅を使う | `CellBaseLayout.kt:216-224` (`contentRow` = MATCH_CONSTRAINT) | `CellRowWidthAllocationTest.kt:251` (CommandCell) | ✅ 一致 |
| Scenario: valueText はコンテンツ幅で title が残り幅を占める | `CellBaseLayout.kt:160-172` | `CellRowWidthAllocationTest.kt:276` | ✅ 一致 (注1) |
| Scenario: 行幅を超える valueText は末尾省略される | 同上 | `CellRowWidthAllocationTest.kt:324` | ✅ 一致 (注1) |

注1: Robolectric の legacy graphics は ellipsize を実行しない (`BoringLayout` になり `getEllipsisCount` が常に 0)。テストは「maxLines=1 + ellipsize=END + 表示幅 < 自然幅」の構成検証にとどめ、実際の "…" 描画は実機証跡 (`ui/verification/`) に委ねている。この制約はテスト本体の KDoc (`CellRowWidthAllocationTest.kt:71-79`) と brief.md に明記されており、**制約の認識は正しい**。

### specs/cell-types-basic — MODIFIED: Cell 級アクセサリと行内 trailing の 2 系統配置

| Requirement / Scenario | 実装 | テスト | 状態 |
|---|---|---|---|
| Requirement 本文: Cell 級アクセサリはセル全体に対し垂直センター | `CellBaseLayout.kt:246-249` (accessoryHolder の TOP/BOTTOM=parent。**未変更**) | `UnifyCellCommonFieldsTest.kt:396` `accessoryHolder はセル縦中央配置` | ✅ 一致 |
| Requirement 本文: 行内 trailing (valueText / EntryCell 入力フィールド) は主行内 | `CellBaseLayout.kt:195-197`, `:285-317` | `CellRowWidthAllocationTest.kt:223` | ✅ 一致 |
| Requirement 本文: description は Cell 級アクセサリと重ならない (MUST NOT) | `CellBaseLayout.kt:234` (`descriptionView.END = accessoryHolder.START`。**未変更**) | 直接の単体テストはなし。実機証跡 `ui/verification/after-basic-cells-pixel6a.png` (SwitchCell の description が Switch 左端で折り返し) | ✅ 一致 |
| Scenario: SwitchCell の description がアクセサリの下に回り込まない | 同上 (本 change で不変) | 実機証跡 + brief.md 照合結果 | ✅ 一致 |
| Scenario: Picker 系は valueText が行内・chevron が Cell 級 | valueText は `contentRow` の子、chevron は `accessoryHolder` (不変) | 実機証跡 `after-input-cells-pixel6a.png` (テーマ / 通知種別 / サイズ 行) | ✅ 一致 |
| Scenario: EntryCell の入力フィールドは行内に置かれる (Android) | `EntryCellViewHolder.kt:244-247` | `CellRowWidthAllocationTest.kt:223` `EntryCell の入力フィールドは本体行の子で accessoryHolder は空のまま` | ✅ 一致 |
| Scenario: EntryCell の入力フィールドは行内のまま (iOS — 既存挙動の確認のみ) | `ios/Sources/KsSettingsViewUI/KsListCellBase.swift:141-142` (`contentStack` に `titleLabel` を置き、行内 trailing を後続追加)。**実装変更なし** (proposal Non-Goals と整合) | 既存 iOS テスト | ✅ 一致 |

### ADR-0002 の実装フェーズ検証事項 (spec 外だが tasks 1.3 / 3.2 が要求)

| 検証事項 | 実装 | テスト | 状態 |
|---|---|---|---|
| valueText ↔ title のベースライン揃えが LinearLayout 内でも維持されるか | `CellBaseLayout.kt:130` (`isBaselineAligned = true` を明示) | `CellRowWidthAllocationTest.kt:360`, `:423`、既存 `UnifyCellCommonFieldsTest.kt:679` | ✅ 一致 (手段は BASELINE 制約 → `isBaselineAligned` に変更。brief.md に記録あり) |
| title+description の縦チェーン (packed + bias 0.5) が入れ子化後も成立するか | `CellBaseLayout.kt:241-243` (chain head を `titleView` → `contentRow` に付け替え) | `CellRowWidthAllocationTest.kt:382`、既存 `UnifyCellCommonFieldsTest.kt:594`, `:632` (root 座標系への換算ヘルパを追加) | ✅ 一致 |
| ButtonCell の ConstraintSet 切替の追随 | `ButtonCellViewHolder.kt:43-71` (対象を `titleView` → `contentRow` に変更) | `CellRowWidthAllocationTest.kt:445`, `:487` | ✅ 一致 |

## 追加検査

### tasks.md の完了状況

全 16 項目 (1.1〜4.4) が `[x]`。対応表と突き合わせた結果、**虚偽チェックは 0 件**。

- 1.1〜1.3 / 2.1〜2.2 / 2.4: 上表の実装列で確認。
- 2.3 (既存挙動の維持): `EntryCellViewHolder.bind` は KDoc 以外の差分なし。ADR-0001 の `inputType` 差分ガード (`EntryCellViewHolder.kt:96-98`) は無傷で、`InputCellsTest.kt:180` `EntryCell 同値 Cell の再 bind では inputType setter が呼ばれない` が新 View 階層 (`createEntryCellViewHolder` が `addFillingInlineTrailing` を使うよう追随済み) の下で成功している。payload 経路そのもの (`ContentUpdatePayloadTest`) は Adapter 層で View 階層に非依存、全件成功。
- 3.1〜3.4: `CellRowWidthAllocationTest.kt` (新規 11 テスト) と既存 3 ファイルの追随。
- 4.1〜4.4: ファイル mtime で順序を確認 — before `20:26:46` → 実装 `20:34:24` → after `20:48:23〜44`。**「実装着手前に before を取得」の要求は満たされている**。

### 逆流検査

`git status` の未コミット差分のうち `kasane/` 配下は `tasks.md` と `ui/brief.md` の 2 ファイルのみ。
`proposal.md` / `specs/settings-view-android-ui/spec.md` / `specs/cell-types-basic/spec.md` / `exploration.md` / `second-opinion-001.md` は**いずれも無変更**。逆流なし。

`ui/brief.md` の更新は tasks 4.1 (「原典期待の扱いを brief.md に明記する」) と 4.2 (「照合結果を brief.md に記録する」) が明示的に指示した作業であり、足場凍結違反にはあたらない。

### UI 変更の証跡

- `ui/brief.md` に承認済みモックの記録あり (`mock/plan-a.html` を採用、`approved.png`、2026-08-01 ユーザー承認)。
- 照合結果 (実機 bounds 実測付き) と、mock の規範範囲 (主行の幅配分の配置関係のみ) に対する一致表が記録されている。
- 実機未確認の 2 項目 (長い title の EntryCell / 長い title の valueText 行の末尾省略) は「Sample に該当行がないため Robolectric テストで検証」と明記され、実際に `CellRowWidthAllocationTest` が対応している。

### 未記録乖離

デルタスペックの Scenario に対する ❌ は 0 件のため、**spec からの未記録乖離はなし**。

ただし、**spec が沈黙している領域で利用者可視の挙動が 2 点変わっている**。これは Scenario 違反ではないため本検証では ❌ にしないが、蒸留前にオーナー判断が要る項目として申し送る (詳細と推奨は `review-001.md` の指摘 1・2 を参照):

1. `titleView` に `isSingleLine = true` + `ellipsize = END` を無条件適用 → 行内 trailing を持たない Cell の長い title が、折り返しから 1 行末尾省略に変わる。brief.md の「合意済み妥協 / 申し送り」に記載はあるが、本文は「→ オーナー確認事項」で**未合意**。
2. `titleView` が `0dp + weight=1` になった副作用で、`ButtonCell` の通常レイアウト (aux あり) の `titleAlignment` が初めて実効化する (既定値 `CENTER` の見た目が左寄せ→中央寄せに変わる)。brief.md にも tasks.md にも記載なし。

いずれも「実装を直すべきか deviation として合意すべきか」の見立ては review-001.md に記載した。
