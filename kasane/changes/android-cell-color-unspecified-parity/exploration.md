# Exploration: android-cell-color-unspecified-parity

## 課題 / 動機

docs-refresh (2026-09-06) の Android Skill 追従中に、サブエージェントの drift 所見として報告された。fix-default-colors-dark-appearance で Android の `Theme` / `CellStyle` の色フィールドは `Color` (未指定 = `Color.Unspecified`) に揃ったが、Cell 固有の色引数である `EntryCell` の `placeholderColor` は `Color?` (`null` が未指定) のまま残っている (`android/kssettingsview/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt`、DSL 側は `compose/InputCellDsl.kt`)。

style 型と Cell 固有の意味値で未指定の表し方が違うのは意図的とも読めるが、`core/styling/style-resolution.md` の「未指定の印は Android の色が `Color.Unspecified`」という記述からはこの使い分けが読み取れない。利用者向け文書 (skills) でも説明しにくい。

## 検討した選択肢 (却下案と理由を含む)

## 決定事項

## ADR 候補 (作成済み: なし / 未起票: なし)

## 未決の論点

- 未探索 (簡易起票)
- 方向: (a) `placeholderColor` も `Color` + `Color.Unspecified` に揃える (公開 API の破壊的変更。ライブラリ未配信のため凍結不要 — ADR-0030 前提と同じ) / (b) 現状を意図的な使い分けとして concepts (style-resolution.md) に一文足すだけにする
- 同型の Cell 固有色引数が他にないか (`accentColor` / `androidButtonColor` / `titleColor` 等) の棚卸し — fix-default-colors-dark-appearance の Non-Goal「色以外の nullable フィールド」とは別軸
- MAUI bridge の DTO → Cell 変換 (`null` を native に渡す契約) への影響

## UI 素材 (ui/references/ の一覧と注釈)

## 変更級の推奨: 未判定 ((b) なら concepts 追記のみで ksn-concept の領分、(a) なら S〜M 級の見込み)
