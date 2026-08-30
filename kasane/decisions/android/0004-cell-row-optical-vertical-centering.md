---
id: 0004
title: Cell 行のテキストとアクセサリは幾何中央ではなく光学中央で揃える
status: accepted
date: 2026-08-02
---

## Context

Android 版で Cell のテキスト (title / valueText) と右端の chevron の垂直位置がズレて見え、iOS 版と比較して品質差があった (ユーザー報告)。Pixel 6a での画素測定の結果、レイアウト (ConstraintLayout の vertical chain + 縦中央 accessory) は幾何学的に正しく組まれているのに、**箱の中の描画だけが中心から外れている**ことが判明した:

- chevron drawable (`ic_navigate_next.xml`、AiForms 原典の忠実移植) はパス縦範囲 y=6..18 が 18x26 viewport の中心 (13) より 1 単位上寄りで、実機で約 1dp **上に浮く**
- テキストは Roboto の ascent/descent 非対称と CJK グリフの ink 中心のずれ (≒0.04em、16〜20sp で約 1dp) により幾何中央より**下に沈む** (Android の TextView 標準挙動)

両者が逆方向のため相対ズレは約 2.3dp に達し、目視で明確に分かる。

## Decision

幾何中央配置はそのままに、描画側を光学中央へ補正する:

1. `ic_navigate_next.xml` の pathData の絶対 y 座標を +1 し、パスを viewport 縦中央 (y=7..19、中心 13) へ補正する。**原典 AiForms からの意図的 deviation** であり、drawable 内コメントに明記する
2. `CellBaseLayout.kt` の contentRow (title + valueText + EntryCell の EditText を包む水平 LinearLayout) に `translationY = -1dp` (`opticalCenterOffsetY`) を掛け、テキストの見た目の沈みを打ち消す

補正後の実測 (Pixel 6a「テーマ」行、セル幾何中心との差): title +1.0px / value ±0.0px / chevron +0.5px、テキスト↔chevron 相対ズレ 6px → 0.5px。

## Alternatives Considered

- **TextView に `includeFontPadding = false`** — 却下。当初の主犯仮説だったが、実機検証でグリフ位置が 1px も動かなかった (TextView の箱が 1px 縮んだだけ = この端末・フォントではフォントパディングがほぼ上下対称)。**再提案禁止: 効果がないことは実測済み**
- **chevron をテキストの光学中心へ沈める (テキスト側は無補正)** — 却下。テキスト↔chevron のペアは揃うが「行全体が気持ち下」が残り、Switch 等の非 chevron アクセサリ行には効かない
- **`<group android:translateY="1">` で chevron を包む** — 却下 (オーナー指定)。変換レイヤを足すのではなく原データ (pathData) 側を直し、単一パスを維持する
- **テキストへの手動オフセット (無根拠のマジックナンバー)** — 探索初期に却下。ただし最終的に採用した -1dp は「無根拠」ではなく、フォントメトリクス由来の沈み量 (≒0.04em) の実測に基づく固定値であり、contentRow 一括適用でベースライン関係を崩さない形に限定した

## Consequences

- 正: 全 Cell 種でテキストとアクセサリが光学的に揃い、iOS 版と同等の見え方になる
- 正: translationY は描画時オフセットのため、レイアウト計算 (chain・最低行高保証・幅配分) に影響しない
- 負: `ic_navigate_next.xml` が原典 AiForms と差分を持つ。原典との機械的 diff で「ズレ」として検出されるが、これは意図的 deviation である
- 負: -1dp は 16〜20sp 帯の実測に基づく固定値のため、利用者が極端に大きいフォントサイズを設定すると補正不足になる (方向は安全側 = わずかな沈みに留まる)。実害が出たらフォントサイズ比例への切り替えを検討する
- 負: テキストの ink はセルの厳密な幾何中央には無い (光学中央を優先する意図的な選択)

出典: kasane/changes/archive/2026-08-02-fix-android-chevron-vertical-centering/exploration.md (検討した選択肢・決定事項・実測値) / 同 ui/references/ (実機証跡)
