# UI Brief: android-numberpicker-modern-ui

## 画面と状態

構造階層 (ボトムシート):

```
BottomSheetDialog (ADR-0005 の PickerSelectionSheet と同系の器)
├── ドラッグハンドル
├── ヘッダー: キャンセル | タイトル (pickerTitle ?: title) | OK
│     (文字列は OS リソース: android.R.string.cancel / android.R.string.ok — 自前文字列の同梱なし)
└── ホイール (縦スクロールのスナップ式、候補は min..max の step 刻み)
    └── 行: 候補の表示文字列 (unit 適用後、例「15 px」)。中央の選択中行を強調
```

状態:

- **通常**: 現在値 (`value`) の候補が中央の選択中位置で開く。候補外の値なら先頭候補
- **候補多数**: ホイール自体がスクロール面。シート高は固定 (ホイールの可視行数分)
- **min > max**: シート自体を提示しない (警告ログのみ)
- **無効 (`isEnabled = false`)**: 行タップでシートを提示しない

## リファレンス注釈

- `references/current-kssettingsview-numberpicker.webp` — 現実装 AlertDialog + NumberPicker。**置き換え対象** (この見た目にしない)。単位表示がない点も不具合として解消する
- `references/aiforms-original-numberpicker.webp` — AiForms オリジナル (SettingsView.Maui)。**候補行の「15 px」形式 (値 + 半角スペース + 単位) を採用**。ダイアログの器・Holo 風罫線は対象外

## デザイントークン参照

- シート面: `Theme.cellBackgroundColor` / ヘッダー・行区切り: `Theme.separatorColor` ([list-appearance](../../../concepts/core/styling/list-appearance.md))
- 選択中候補の強調: `NumberPickerCell.accentColor`、未指定時は「CellStyle → Theme」の段階解決 ([style-resolution](../../../concepts/core/styling/style-resolution.md))
- ヘッダー構成 (キャンセル=低強調テキスト / OK=accent の filled pill) は android-picker-selection-sheet の承認モックの意匠を踏襲
- 生値はここに書かない。具体レイアウトは mock が正

## 検証条件 (動的挙動の判定基準)

視覚照合では静的な mock 照合に加えて以下を判定する:

- ホイールのスクロールを離すと必ずいずれか1候補が中央 (選択中位置) にスナップして静止する
- 選択中行は強調 (accent 色) され、周辺行は減衰表示になる
- 開いた時点で `value` の候補が選択中位置にある
- 確定 (OK) でのみ callback が発火し、キャンセル・外側タップ・Back・下スワイプでは発火しない

## 承認モック

mock/plan-a.html を採用 (approved.png、2026-08-02 オーナー承認)。

- 構成: ドラッグハンドル + ヘッダー (キャンセル=テキストボタン / タイトル中央 / OK=accent の filled pill) + スナップ式ホイール (可視5行)
- 選択中行の表現: accent 淡色の丸角帯を背後に敷き、行文字は accent 色・太字。周辺行は距離に応じて減衰 (フェード + 縮小)
- ホイール上下端はシート面色へのグラデーションでフェードアウト
- plan-b.html (上下罫線区切りのクラシックホイール風) は不採用の対案として保存

## 照合結果 (2026-08-02)

Pixel6a エミュレータ (Android 16 / 1080x2400) + samples/android の「入力 Cell 5 種デモ」で実機確認。
スクリーンショットは `verification/` に保存。

| 画像 | 内容 |
|---|---|
| `verification/sheet-open-initial-value-max.png` | 行タップで開いた直後 (value=30 = 末尾候補が中央) |
| `verification/wheel-after-swipe-snap-27px.png` | 候補領域を下方向スワイプ → 27 px にスナップ静止 (可視5行の減衰が見える状態) |
| `verification/cell-row-after-confirm.png` | OK 確定後の Cell 行と直近イベント行 |
| `verification/sheet-initial-27px.png` | 再オープン時に確定値 27 px が中央 (初期位置 = value) |

**静的照合 (approved.png との構造・トークン・意図)**: 一致。ドラッグハンドル / ヘッダー3要素の配置 /
区切り線 / 可視5行 + 中央 accent 淡色帯 / 選択中行の accent 太字 / 周辺行の減衰 / 上下端フェードが
モックどおり。色は Theme・CellStyle 由来の解決値で描画されている (モックの生値ではない)。

**検証条件 (動的挙動)**:

- スナップ静止: 指を離すと必ずいずれか1候補が中央帯に静止する — OK (27 px が帯にぴたりと収まる)
- 選択中行の強調と周辺の減衰: OK (中央=accent 太字・拡大、±1 行=フェード、±2 行=さらにフェードと縮小)
- 初期位置 = value: OK (初回 30 px / 確定後の再オープンで 27 px が中央)
- 確定でのみ callback 発火: OK (OK タップで「最後のイベント: サイズ → 27 px」、Cell 行が「27 px」へ更新)
- キャンセル: OK (候補を動かしてから取消 → 値・イベント行とも不変)
- 外側タップ: OK (同上)
- Back: OK (同上)
- 下方向スワイプ (ドラッグハンドル起点): OK (シートは閉じ、値・イベント行は不変)
- 候補領域の下方向操作で dismiss しない: OK (ホイール上の下スワイプは候補の遷移のみでシートは開いたまま)

**トークン候補**: なし (使用した生値はいずれも承認モックのレイアウト寸法 — 行高 44dp・可視5行・
帯のインセット 12dp と角丸 12dp・帯のアルファ 0.14・フェード高 60dp・減衰の倍率 — であり、
プロジェクトのデザイントークンに相当する概念ではないため、ホイール部品の定数として保持した)。

**合意済み妥協**: 0 件。

**サンプルでの未カバー**: 承認モック右フレームの「unit 未指定 (数値のみ)」は、サンプルアプリに
該当 Cell が無いため実機照合していない (候補文字列の生成は単体テストでカバー。
`NumberSelectionSheetTest#候補は step 刻みで昇順に列挙される` が unit なしの表示を検証)。
