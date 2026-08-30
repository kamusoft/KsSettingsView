# セカンドオピニオン: add-entrycell-placeholder-color (code-001)
**相方**: codex / **label**: so-code-add-entrycell-placeholder-color / **日付**: 2026-08-27 / **対象**: 本 change の未コミット作業ツリー変更全体 (iOS / Android / MAUI core・bridge・facade・サンプル・テスト)
---
# レビュー結果: add-entrycell-placeholder-color

**判定**: **CHANGES_REQUESTED**

## サマリー

placeholder 色の4段解決、Native Bridge／MAUI 輸送、表示中更新、3 platform のサンプル対称性は概ね仕様どおりです。承認済みの付随修正2件にも問題はありません。

ただし Android に利用者影響のある Major が2件あります。テスト結果はすべて成功していますが、該当ケースは現在のテストで検出できていません。

## 指摘事項

### [🟠 Major] Android の全段未指定時に入力文字色が platform default にならない

**該当箇所**:

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCellViewHolder.kt:217`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:146`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:325`
- `android/ks-settingsview-ui/src/test/kotlin/jp/kamusoft/kssettingsview/ui/InputCellsTest.kt:427`

**問題点**:
今回 `EntryCell` の入力色を `effective.titleColor` から `effective.valueTextColor` へ変更しています。しかし `effectiveValueTextColor()` は、`CellStyle.valueTextColor`、`Theme.cellValueTextColor`、`Theme.cellTitleColor` がすべて未指定の場合、ホストの `android:textColorPrimary` ではなく固定値 `Theme.DEFAULT_CELL_TITLE_COLOR`（黒）へ fallback します。

これはデルタスペックの「Theme title → platform default」と一致せず、ダークテーマでは入力文字が黒くなり視認性が落ちます。変更前の `effective.titleColor` は `android:textColorPrimary` を解決していたため、既定構成に対する回帰でもあります。

現在の fallback テストは `Theme.cellTitleColor` を明示しており、全段未指定の最終 fallback を検証していません。

**推奨修正**:
`EffectiveStyle.from()` では valueText 固有値がない場合、すでにホストテーマから解決済みの `titleColor` を使用してください。併せて Material3 のダークテーマ上で全関連値を未指定にし、`EditText.currentTextColor` がホストの `textColorPrimary` と一致するテストを追加してください。

### [🟠 Major] Android 公開 API の途中へ引数を挿入しておりソース互換性を破壊する

**該当箇所**:

- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EntryCell.kt:50`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt:47`
- `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/Theme.kt:111`
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:42`
- `android/ks-settingsview-compose/src/main/kotlin/jp/kamusoft/kssettingsview/compose/InputCellDsl.kt:80`
- `kasane/changes/add-entrycell-placeholder-color/proposal.md:23`

**問題点**:
新しい色引数が既存パラメータ列の途中へ挿入されています。名前付き引数では問題ありませんが、位置引数を使う既存 Kotlin コードはコンパイル不能または意味が変わります。

特に `CellStyle` は後続の `backgroundColor` と `accentColor` も同じ `Color?` 型なので、既存コードがコンパイルされたまま、従来の背景色が placeholder 色として解釈される可能性があります。また、data class の後続 `componentN` もずれるため、既存の分解宣言にも影響します。これは proposal の「ソース互換は保たれる」という合意と矛盾します。

**推奨修正**:
既存パラメータと `componentN` の順序を維持するため、次のように新規引数を末尾へ追加してください。

- `EntryCell`: `isVisible` の後
- `CellStyle`: `accentColor` の後
- `Theme`: `sectionBorderColor` の後
- Compose DSL 2 overload: `style` の後

位置引数で既存シグネチャを呼ぶコンパイルテスト、または API 互換性検査も追加すると再発防止になります。

## アクションプラン

1. Android の valueText 最終 fallback をホストテーマ色へ修正し、ダークテーマの全段未指定テストを追加する。
2. Android 公開 API の `placeholderColor`／`cellPlaceholderColor` を既存引数列の末尾へ移動する。
3. 全 platform のテスト・TFM ビルドを再実行する。

**件数**: Critical 0 / Major 2 / Minor 0 / Suggestion 0

---

## 突き合わせ結果 (ホスト review-001.md との照合、2026-08-27)

| 指摘 | 出典 | 採否 | 根拠 |
|---|---|---|---|
| Android 全段未指定時の入力文字色が platform default にならない (Major) | 相方のみ | **採用** (Major) | コードで実証: `EffectiveStyle.from()` の `titleColor` は未指定時に `android.R.attr.textColorPrimary` を解決する分岐を持つ (`EffectiveStyle.kt:113-114`) が、`effectiveValueTextColor()` の最終 fallback は固定値 `Theme.DEFAULT_CELL_TITLE_COLOR` (黒)。変更前の `effective.titleColor` 参照からのダークテーマ回帰であり、spec の「Theme title 既定 → platform default」とも不一致 |
| Android 公開 API の途中への引数挿入によるソース互換破壊 (Major) | 相方のみ | **採用** (Major) | diff で実証: `EntryCell.placeholderColor` は `placeholder`→`keyboardType` 間、`CellStyle.placeholderColor` は同型 `Color?` の `backgroundColor`/`accentColor` 直前、`Theme.cellPlaceholderColor`・DSL 2 overload も途中挿入。位置引数・分解宣言の既存コードを壊し、proposal の「ソース互換: 保たれる」と矛盾 |
| ホスト側 Minor 3 / Suggestion 2 (視覚証跡欠け・差分判定テスト欠け・未使用フィールド・hint 色 null 時の復元 no-op・ダーク面視認性) | ホストのみ | 確定 (ホスト判定のまま) | 相方と矛盾なし。修正サイクルで処理 |

- 未解決 (両者矛盾) の指摘: なし
- 採用 2 件はホスト側指摘と同格として修正サイクル (review cycle 2) へ
