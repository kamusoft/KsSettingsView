---
id: 0002
title: 共通行の幅配分は AiForms 同型の水平 LinearLayout + weight で行い、EntryCell の入力欄は行内に置く
status: accepted
date: 2026-08-01
---

## Context

Android の EntryCell で、入力欄 (EditText) が「Title の残り幅」を割り当てられず画面の半分程度しか確保されない不具合が発生した (パスワード欄も同様。iOS と AiForms 版 Android は正常)。

現行の共通行レイアウト (`CellBaseViews` / ConstraintLayout ベース) には幅配分の主体が存在しない。title / valueTextView / accessoryHolder がそれぞれ独立アンカーの `WRAP_CONTENT` で浮いており、「残り幅を誰かに割り当てる」構造がどこにも表現されていない。EntryCell は EditText を accessoryHolder (右端 FrameLayout) に置き、`minWidth = 160dp` の暫定ハックで幅を確保していた (EntryCellViewHolder.kt のコメント自体がこれを暫定と自認していた)。

移植元 AiForms.SettingsView の Android 実装を確認した結果:

- 行本体 `CellContentStack` (水平 LinearLayout) が icon と accessory の間の全幅を占有し (`toRightOf=CellIcon, toLeftOf=CellAccessoryView` + `match_parent`)、**LinearLayout の weight で残り幅を配分**している。
- 既定 (LabelCell 等の valueText): `CellTitle` が `0dp + weight=1` (title が残り幅)、ValueLabel は `wrap_content` (singleLine + ellipsize END)。
- EntryCell: コンストラクタで weight を付け替え (`TitleLabel.Weight = 0; Width = WrapContent`)、EditText を `(0dp, weight=1)` で **ContentStack (行内) に追加**する (`//remove weight and change width due to fill _EditText.`)。
- つまり原典の Android は EntryCell の入力欄を **accessory ではなく行内**に置いている。concepts の cell-row-layout.md にある「Android の EntryCell は入力フィールドを accessory 領域に置く既存配置を維持する」という記述は原典と乖離しており、accessory 配置自体が移植時の乖離、160dp ハックはその帳尻合わせだった。

## Decision

Android の共通行レイアウトに AiForms の `CellContentStack` 相当の**水平 LinearLayout を導入し、weight で残り幅を配分する**:

1. 本体行 (title + valueText / 入力欄) を水平 LinearLayout として構築する (programmatic 構築。root の ConstraintLayout — icon・accessory・hint の配置と MinHeight 保証 — は維持し、本体行だけ入れ子にする)。
2. 既定の配分は原典同型: title が `0dp + weight=1` (残り幅)、valueText は `wrap_content` (singleLine + ellipsize END)。
3. EntryCell は weight を付け替える: title を `wrap_content` にし、EditText を `(0dp, weight=1)` で**行内に配置**する。accessoryHolder 配置と `minWidth = 160dp` ハックは撤去する。

## Alternatives Considered

- **EntryCell 固有の constraint 上書き (accessoryHolder を width 0dp + START=title.END に張り替え)**: 却下。症状が出ているのは EntryCell だが、valueTextView も同じ構造的弱点 (残り幅配分の不在) を抱えており、共通行の問題を EntryCell だけ対症療法で塞ぐことになる。オーナー指摘により棄却 (「CellBaseLayout も同じ問題を抱えているなら元から治すべし」)。
- **ConstraintLayout 水平チェーンで weight 相当を再現**: 却下。View 階層は現状維持できるが、chain + BASELINE 紐付け + GONE メンバーの組み合わせは罠が多く、原典との等価性の検証コストが高い。原典と同じレイアウトエンジン挙動 (LinearLayout weight) を持ち込む方が「AiForms でできている配分」の再現が確実。
- **EditText の accessory 配置を維持したまま幅だけ直す**: 探索初期には concepts の「accessory 領域に置く既存配置を維持する」記述との整合から行内移設を避ける評価をしたが、原典確認で当該記述自体が原典と乖離していると判明したため、行内移設を採用する。

## Consequences

- 正: EntryCell の入力欄と valueText の幅配分が原典 AiForms と同型になり、「Title=コンテンツ幅、残りは値/入力欄」(EntryCell) と「値=コンテンツ幅、残りは Title」(valueText 系) が構造として保証される。
- 負: 本体行の View 階層が1段増える。
- 影響: 全 Cell の共通行に波及するため変更級は M。concepts/core/styling/cell-row-layout.md の「Android の EntryCell は入力フィールドを accessory 領域に置く既存配置を維持する」記述の訂正が必要 (蒸留時)。
- 検証: BASELINE 紐付け (valueText と title の同一ベースライン) が LinearLayout 内でも維持されるか、description との縦チェーン (packed + bias 0.5) が本体行の入れ子化後も成立するかを実装フェーズで確認する。

出典: fix-android-cell-width-allocation の探索会話 (2026-08-01)。AiForms.SettingsView 原典照合 (SettingsView/Resources/layout/CellBaseView.axml / Platforms/Android/Cells/EntryCellRenderer.cs:63-73 / LabelCellRenderer.cs:48-60)
