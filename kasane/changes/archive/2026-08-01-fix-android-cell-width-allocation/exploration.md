# Exploration: fix-android-cell-width-allocation

## 課題 / 動機

Android の EntryCell で、入力欄が「Title の残り幅」を割り当てられず画面の半分程度しか確保されない (パスワード欄も同様)。iOS と AiForms 版 Android は正常。期待挙動は「Title=コンテンツサイズ、入力欄=残り領域全部」。

調査で判明した構造的真因:

- 現行の共通行 (`CellBaseViews` / ConstraintLayout) には**幅配分の主体が存在しない**。title / valueTextView / accessoryHolder が独立アンカーの `WRAP_CONTENT` で浮いている。
- EntryCell は EditText を accessoryHolder に置き `minWidth = 160dp` の暫定ハックで幅を確保 (EntryCellViewHolder.kt:250 のコメントが暫定と自認)。160dp ≒ 412dp 幅端末の約4割 → 「半分くらい」の症状と符合。
- valueTextView も同じ弱点を持つ (長い valueText で title と衝突し得る潜在問題)。
- 原典 AiForms は `CellContentStack` (水平 LinearLayout、icon〜accessory 間の全幅占有) の **weight で残り幅を配分**。既定は title `0dp+weight=1` / value wrap。EntryCell は weight を付け替え title wrap / EditText `0dp+weight=1` で**行内に配置** (accessory ではない)。
- concepts/core/styling/cell-row-layout.md の「Android の EntryCell は入力フィールドを accessory 領域に置く既存配置を維持する」は**原典と乖離した記述**だった。

## 検討した選択肢 (却下案と理由を含む)

- **案A: EntryCell 固有の constraint 上書き** — 却下。共通行 (valueText 側) も同じ構造問題を抱えるため対症療法。オーナー指摘「元から治すべし」。
- **案2: ConstraintLayout 水平チェーンで weight 相当を再現** — 却下。chain + BASELINE + GONE の組み合わせは罠が多く、原典等価性の検証コストが高い。
- **案1 (採用): 本体行に AiForms 同型の水平 LinearLayout + weight を導入** — 原典と同じレイアウトエンジン挙動で再現が確実。root ConstraintLayout (icon / accessory / hint / MinHeight 保証) は維持し本体行だけ入れ子化。programmatic 構築のため「XML を使わない」方針とも整合。

## 決定事項

- 案1 で進める (オーナー確定 2026-08-01)。
- EntryCell の EditText は accessory から行内へ移設し、160dp minWidth ハックを撤去する。

## ADR 候補 (作成済み: android/ADR-0002 / 未起票: なし)

- [android/ADR-0002](../../decisions/android/0002-cell-row-width-allocation-linearlayout-weight.md) — 本探索の決定を起票し、2026-08-01 にオーナー承認済み (accepted)。

## 未決の論点

- BASELINE 紐付け (valueText ↔ title) と縦チェーン (title+description packed) が本体行の入れ子化後も成立するか (実装フェーズで検証)。
- valueText 系の既定配分が「title=残り幅 / value=wrap」(原典) となるため、既存スクリーンショットとの見た目差分が出ないかの確認。
- concepts/core/styling/cell-row-layout.md の accessory 配置記述の訂正 (蒸留時)。

## UI 素材 (ui/references/ の一覧と注釈)

- 未保存: 症状スクリーンショット2枚 (KsSettingsView 版=入力欄が半分程度 / AiForms 版=残り幅全部)。チャット添付のためバイナリ取り出し不可。実装フェーズの実機検証時に before/after として再取得する。

## 変更級の推奨: M (理由)

全 Cell が共有する共通行レイアウトの再構築で影響が広い (基本 Cell + 入力 Cell 全種の描画に波及)。公開 API 変更はないが、concepts の記述訂正を伴い、見た目のリグレッション検証が必要。
