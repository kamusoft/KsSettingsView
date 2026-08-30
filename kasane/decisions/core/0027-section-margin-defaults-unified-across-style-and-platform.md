---
id: 0027
title: Section margin の既定は style 間・platform 間とも同値に統一する
status: accepted
date: 2026-08-25
---

## Context

Section 装飾の既定 `sectionMargin` は「Classic: 上下 0 / Modern: platform 所有の既定寸法」で、Modern の生値も platform 別 (iOS top 22 / bottom 0、Android 上下 12) だった。core/ADR-0024 は既定角丸を生値 26 へ統一した際、margin は「引き続き platform 所有」と据え置いている。

実機・シミュレータでのライブ調整 (change: adjust-section-spacing) で、(1) Android Modern の上下が iOS より詰まりすぎて見える、(2) Classic の既定が上下 0 のため Classic ⇔ Modern の切替で Section 間隔が大きく変わり違和感がある、という2つの問題をオーナーが確認した。

## Decision

既定 `sectionMargin` を次のとおり統一する:

- **platform 間**: iOS / Android とも同じ生値 top 22 / bottom 0 / 水平 16 (pt / dp) とする
- **style 間**: Classic の既定値 (`classicDefaultMargin` / `CLASSIC_DEFAULT_MARGIN`) は Modern の既定値への別名とし、完全同値とする
- Classic の実効値は既存の全幅契約のまま — `resolve` が水平成分を無視するため、上下のみ反映される。既定値に水平 16 を持たせるのは「水平は Classic では無視される」仕様を既定値の対称性ごと示すため
- Section Header ラベル下 / Footer ラベル上の text 間隔は 4pt/dp とし、Section の Header/Footer に限定する。Root Header / Footer は両 platform とも 0 (Root は利用者がカスタム View を設定する想定で、ライブラリ側の余白を入れない)

core/ADR-0024 の「margin の既定は引き続き platform 所有」という判断は本 ADR が置き換える (角丸の生値 26 統一は 0024 のまま有効)。

## Alternatives Considered

- **platform 別既定の維持 (現状維持)**: Android の詰まりが解消せず、未指定 Theme のクロスプラットフォーム表示差が残るため却下。
- **Classic は上下のみ Modern と同値・水平 0 を既定値に持つ**: 実効値は同じだが、既定値定数が非対称になり「水平は resolve が無視する」仕様が定数からは読み取れない。オーナー指示で完全同値 (水平 16 込み) を採用し却下。
- **Root Header / Footer にも 4pt/dp の text 間隔を入れる (iOS 実装当初の形)**: Root はカスタム View 前提でライブラリが余白を所有すべきでないとのオーナー判断で却下。両 platform 0 に統一。

## Consequences

- 正: 未指定 Theme の Section 間隔が platform 間・style 切替間で一致し、Classic ⇔ Modern 切替の違和感が消える。
- 正: 「Section Margin の上下は Classic にも反映・水平は無視」という契約を既定値の対称性が自己文書化する。
- 負: Classic の既定が上下 0 前提だった既存利用者は、更新後に Classic の Section 間隔が広がる (top +22)。従来の詰まった表示に戻すには `sectionMargin` の明示指定が必要。
- 負: 「全行が 1 画面に収まる」前提のテストは既定 margin の変動に影響される。幾何前提のテストは Theme に `sectionMargin = PaddingValues(0.dp)` 等を明示固定する。

出典: kasane/changes/archive/2026-08-25-adjust-section-spacing/summary.md (最終状態・採用値と根拠)
