# レビュー結果: add-entrycell-placeholder-color (002 回目)

**日付**: 2026-08-27
**判定**: APPROVED

## サマリー

前サイクルで確定した 6 指摘 (Major 2 / Minor 3 / Suggestion 1) は**すべて解消**を確認した。修正は Android の 7 ファイルに閉じており、公開 API の引数順は既存 `componentN` を崩さない末尾追加へ戻され、valueText の最終段はホストテーマ (`android:textColorPrimary`) 解決へ是正され、抜けていた 2 テスト (差分判定・全段未指定) と 2 枚の視覚証跡が加わった。Android 全件テストは `./gradlew test --rerun-tasks` で **BUILD SUCCESSFUL / 2702 tests / 0 failures / 0 errors** (前回 2698 から新規 2 テスト × 2 variant 分の増加と一致)。iOS / MAUI のソースは review-001 実行時点から一切変更されていないため (全ファイルの mtime が review-001.md より前)、前回の実行結果 (iOS 606 tests / MAUI 472 tests、いずれも 0 failures) をそのまま持ち越す。

新規の Critical / Major は無い。残るのは deviation.md の `[波及]` 記録が影響 Cell 種別を過小に列挙している点 (Minor、1 行の修正) と、性能・再発防止に関する Suggestion 2 件のみ。

## 前サイクル指摘の解消状況

| # | 指摘 (出典) | 判定 | 確認内容 |
|---|---|---|---|
| (a) | Android 全段未指定時の入力文字色が platform default にならない (🟠 Major / 相方) | **解消** | `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:145-152` で `valueTextColorIsExplicit` を導入し、全段未指定時のみ `resolveDefaultTitleColor(context)` へ分岐。テスト `EntryCell の入力文字色は全段未指定ならホストテーマの文字色になる` が Material3 Dark 上で実測 (`InputCellsTest.kt`) |
| (b) | 新規引数の途中挿入によるソース互換破壊 (🟠 Major / 相方) | **解消** | `EntryCell.placeholderColor` は `isVisible` の後、`CellStyle.placeholderColor` は `accentColor` の後、`Theme.cellPlaceholderColor` は `sectionBorderColor` の後、Compose DSL 2 overload は `style` の後。いずれも既存パラメータの相対順と `componentN` を保つ末尾追加 |
| (c) | 差分判定 (再適用抑止) に対応テストが無い (🟡 Minor / ホスト) | **解消** | `InputCellsTest.kt` の `同一 Cell への再バインドで変化の無い placeholder 色を再適用しない` を追加。外から sentinel 色を書き込んでから同値再バインドし、sentinel が残ることを見る形で、早期 return を外すと必ず落ちる |
| (d) | `EffectiveStyle.placeholderColor` が未使用 (🟡 Minor / ホスト) | **解消** | data class からフィールド・`@property` 行とも削除済み。解決は companion の `effectivePlaceholderColor` 2 種 (`EffectiveStyle.kt:371` / `:385`) に一本化され、描画側 (`EntryCellViewHolder.kt:182-188`) はこれを直接呼ぶ |
| (e) | valueText 是正の視覚証跡と証跡範囲の明記が無い (🟡 Minor / ホスト) | **解消** | 証跡 2 枚 (`ui/verification/android-entry-valuetext-default.png` / `android-entry-valuetext-theme.png`) を実見し、`ui/brief.md` の記述と一致することを確認 (既定構成では入力済みテキストが従来の title 色解決値、`Theme.cellValueTextColor` 明示時は入力済みテキストのみ青へ変わり、行タイトル・placeholder 色・未入力行の placeholder は不変)。`ui/brief.md:41` に、画面証跡が届かない「全段未指定 → ホストテーマ既定」経路の証跡範囲外宣言と担保テストが明記された |
| (f) | ホスト hint 色 null 時の復元 no-op (🔵 Suggestion / ホスト) | **解消** | `EntryCellViewHolder.kt:323-337` で、`hostHintTextColors == null` のときは代入せず `placeholderColorApplied` も更新せずに戻る形にし、次回 bind で必ず再評価される (明示色が来れば確実に反映される)。前提 (`Theme.Material3.*` はホスト hint 色を必ず持つ) と、戻し先が無い構成では復元できないことは `:68-79` の doc に明記 |

## 指摘事項

### [🟡 Minor / 優先度: 低] deviation.md の `[波及]` が影響する Cell 種別を 2 つしか挙げていない

**該当箇所**: `kasane/changes/add-entrycell-placeholder-color/deviation.md:5` / 根拠は `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellBaseLayout.kt:547`

**問題点**:
記録は「`EffectiveStyle.valueTextColor` は全 Cell 共通のため、`EntryCell` だけでなく `LabelCell` / `CommandCell` の valueText も…色が変わる」と書いているが、実際に `applyCellBaseLayout` へ非 null の `valueText` を渡す ViewHolder は 11 種ある — `LabelCell` / `CommandCell` に加え `ButtonCell` / `SwitchCell` / `CheckboxCell` / `RadioCell` / `SimpleCheckCell` / `PickerCell` / `NumberPickerCell` / `DatePickerCell` / `TimePickerCell`。とくに Picker 系 3 種は選択値をこの valueText として表示するため、影響の見え方は `LabelCell` / `CommandCell` より大きい (提出済みの `android-entry-valuetext-theme.png` でも `PickerCell` の「ライト」が同じ色で描画されている)。

「全 Cell 共通」という前置きは正しいが、この記録はオーナーが波及の是非を判断するための材料であり、名指しされた 2 種だけを見て影響範囲を見積もられる余地がある。

**推奨修正**: 列挙を「`applyCellBaseLayout` の valueText を持つ全 Cell (LabelCell / CommandCell / ButtonCell / Switch・Checkbox・Radio・SimpleCheck / Picker・NumberPicker・DatePicker・TimePicker)」の趣旨へ 1 行で書き換える。オーナー確認へ回す前に直すのが望ましい。

### [🔵 Suggestion] 全段未指定時にホストテーマ解決が 1 bind で 2 回走る

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/EffectiveStyle.kt:111-115` と `:145-152`

**問題点**:
`titleColor` と `valueTextColor` がともに全段未指定のとき (= Theme を素で使う既定構成)、`resolveDefaultTitleColor(context)` が同一 `from()` 呼び出し中に 2 回走る。中身は `Theme.resolveAttribute` + `ContextCompat.getColorStateList` で、`from()` は Cell の bind ごとに呼ばれる (`EntryCellViewHolder.kt:141` ほか各 ViewHolder)。結果は必ず同値なので、2 回目は無駄。

**推奨修正**: 必須ではない。`from()` の先頭付近で「どちらかが未指定なら 1 回だけ解決する」ローカル変数を用意し、両分岐で共有する (3 行程度)。

### [🔵 Suggestion] 位置引数によるソース互換の再発防止策は未導入

**該当箇所**: `android/ks-settingsview-ui/src/main/kotlin/jp/kamusoft/kssettingsview/ui/CellStyle.kt` / `Theme.kt` / `EntryCell.kt`

**問題点**:
(b) の是正で今回の順序は直ったが、`CellStyle` は同型 (`Color?`) のフィールドが並ぶため、将来の途中挿入が再びコンパイル可能なまま意味だけ変わる事故を起こし得る。相方レビューが併記していた「位置引数で既存シグネチャを呼ぶコンパイルテストまたは API 互換性検査」は追加されていない。

**推奨修正**: 必須ではない。導入するなら本 change とは別に、`binary-compatibility-validator` 等の API dump 方式か、主要 data class を位置引数で構築するコンパイル専用テストのどちらを採るかを決めてからにするのが妥当 (本 change 内で決める設計判断ではない)。

## 確認した観点 (指摘に至らなかったもの)

- **(a) の修正方法の妥当性**: 相方の推奨は「valueText 固有値が無ければ解決済みの `titleColor` を使う」だったが、実装はそれを採らず解決チェーン (`CellStyle.valueTextColor` / `Theme.cellValueTextColor` / `Theme.cellTitleColor`) と厳密に一致する条件で分岐している。`titleColor` の再利用は `CellStyle.titleColor` を valueText チェーンへ引き込み、`kasane/concepts/core/styling/style-resolution.md` の「valueText は CellStyle → Theme valueText → Theme title → platform default」に反するため、**実装側の判断の方が正しい**。iOS (`ios/Sources/KsSettingsViewUI/EffectiveStyle.swift:158-160` の最終段 `UIColor.label`) とも同じ意味になった。
- **新規テストの検出力** (lessons/code-review L-001): 静的読解で結論が付く形になっているためミューテーション実測は行っていない。差分判定テストは「外部から書いた sentinel 色が残ること」を見るので早期 return を外せば必ず失敗し、全段未指定テストは事前アサーションで「ダークテーマの `textColorPrimary` が黒でない」ことを確かめてから比較するため、旧実装 (固定黒) では必ず失敗する。どちらも実装が誤ると落ちる。
- **companion アクセサとの二重管理**: `effectiveValueTextColor` の最終段は依然 `Theme.DEFAULT_CELL_TITLE_COLOR` (黒) を返し、実効値 (`from()`) と食い違う。ただしこれは `effectiveTitleColor` に元からある同型の構造 (Context を要する 4 段目だけ `from()` が持つ) の踏襲であり、両アクセサとも `from()` とテストからしか呼ばれない (`internal`)。本 change が持ち込んだ乖離ではないため指摘しない。
- **足場凍結**: `proposal.md` / `specs/` は無改変。`tasks.md` はチェックボックスのみ。`ui/brief.md` の追記は照合結果・トークン候補・証跡範囲という所定欄のみ。
- **deviation.md の付随修正 2 件**: 前回と同じく ksn-core の同梱条件内 (本務で触れたファイル内・コメント 1 行・分岐なし)。今サイクルで新たな無断逸脱は検出されなかった。撮影用に一時追加した `Theme.cellValueTextColor` は `samples/android/.../SampleTheme.kt` の diff に残っておらず (追加は `demoPlaceholderOrange` のみ)、brief の「撮影後に元へ戻している」と一致する。
- **placeholder 色まわりの回帰**: `reset()` はホスト既定 `ColorStateList` の復元と差分判定フラグの初期化を行い、再利用行に色が残らないこと・明示 → 未指定の遷移・無効状態での単色維持はいずれもテスト済み。Theme 追従は Robolectric の `EntryCellPlaceholderThemeRefreshTest` が実 View 経路で検証している。
- **輸送経路**: Android bridge の `KsBridgeEntryCell` / `KsBridgeTheme` は `var` プロパティ + 名前付き引数の `resolve()` のため、(b) の引数順問題の影響を受けない。
- **コメント規約**: `python3 scripts/comment-policy-lint.py --summary` は 681 ファイル / 禁止 0 件。今回追加された doc コメント (hint 色の前提・差分判定の理由・valueText 4 段目の分岐理由) はいずれも単独で読めて、外部 ID だけに依存した説明になっていない。
- **テスト実行** (`kasane/concepts/cross/conventions/test-execution.md` 準拠): Android は `cd android && ./gradlew test --rerun-tasks` で BUILD SUCCESSFUL、`build/test-results/test{Debug,Release}UnitTest/TEST-*.xml` 206 ファイルの集計で 2702 tests / 0 failures / 0 errors / 0 skipped。新規 2 テストが XML 上に存在し pass していることも個別に確認した。iOS / MAUI は今サイクルでソース無変更のため再実行していない (根拠: 変更ファイルの mtime がすべて review-001.md より前)。

## アクションプラン

1. `deviation.md` の `[波及]` の Cell 種別列挙を実態 (valueText を描画する全 Cell) に合わせて 1 行修正する — オーナー確認へ回す前 (Minor / 優先度: 低)
2. (任意) `EffectiveStyle.from()` のホストテーマ解決を 1 回に纏める (Suggestion)
3. (任意・本 change 外) 位置引数互換の再発防止策 (API dump またはコンパイル専用テスト) を採るか判断する (Suggestion)
