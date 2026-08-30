# Proposal: fix-android-cell-width-allocation

## Why

Android の EntryCell で、入力欄が「Title の残り幅」を割り当てられず画面の半分程度しか表示領域を持てない (パスワード欄も同様。iOS と原典 AiForms 版 Android は正常)。真因は、共通行レイアウト (`CellBaseViews`) に幅配分の主体が存在しないこと。title / valueTextView / accessoryHolder が独立アンカーの `WRAP_CONTENT` で浮いており、EntryCell は EditText を accessory 領域に置いて `minWidth = 160dp` の暫定ハックで幅を確保していた。原典 AiForms は水平 LinearLayout の weight で残り幅を配分しており、EntryCell の入力欄は accessory ではなく行内に置いている (android/ADR-0002 で決定済み)。

## What Changes

- `android/ks-settingsview-ui` の共通行構築 (`CellBaseLayout.kt`) に、原典 `CellContentStack` 相当の**本体行水平 LinearLayout** を導入する (root の ConstraintLayout と MinHeight 保証は維持し、本体行だけ入れ子化)。
- 幅配分を weight で表現する。既定 (valueText 系): title が残り幅 (`0dp + weight=1`)、valueText はコンテンツ幅 (singleLine + ellipsize END)。
- EntryCell: weight を付け替え、title をコンテンツ幅にし EditText を残り幅 (`0dp + weight=1`) で**行内に配置**する。accessoryHolder 配置と `minWidth = 160dp` ハックは撤去する。
- 影響 capability: `settings-view-android-ui` (Compose Bridge は View Host を包むため実装変更なし、挙動が追随する)。

## Non-Goals

- iOS / MAUI 側の変更 (iOS は現状正常)。
- Theme / CellStyle の解決規則、色・フォント・行高さの変更。
- accessoryHolder を使う他 Cell (Switch / checkbox / checkmark / chevron) の配置変更 (accessory 領域はそのまま)。
- RootHeaderFooterAdapter 等、Cell 行以外の領域。

## Impact

- 公開 API 変更なし。破壊的変更なし。
- 全 Cell の共通行に波及するため、基本 Cell + 入力 Cell 全種の視覚リグレッション検証が必要。特に `ButtonCellViewHolder` は titleView が root 直下である前提の ConstraintSet 切替を持つため、明示的な追随が必要。
- 技術リスク (ADR-0002 の検証事項): valueText ↔ title の BASELINE 揃えと、title+description 縦チェーン (packed) が本体行入れ子化後も成立するか。LinearLayout 内では baseline 揃えの手段が変わる可能性があり、実装時に Robolectric テストと実機で確認する。

## 級: M

全 Cell が共有する共通行レイアウトの再構築で影響が広いが、公開 API 変更はないため。

domain: cross
