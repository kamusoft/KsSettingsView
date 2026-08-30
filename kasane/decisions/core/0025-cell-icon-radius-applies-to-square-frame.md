---
id: 0025
title: Cell icon の角丸は aspect fit の正方形枠に対して適用する
status: accepted
date: 2026-08-22
---

## Context

Cell の icon は `Theme.cellIconSize` / `CellStyle.iconSize` で解決した一辺の**正方形枠**に置き、`Theme.cellIconRadius` / `CellStyle.iconRadius` で角丸をかける公開契約を持つ (core/ADR-0009 の Theme / CellStyle 体系、解決規則は concepts/core/styling/style-resolution.md)。画像の配置は iOS が `UIImageView.contentMode = .scaleAspectFit`、Android が `ImageView` 既定の `FIT_CENTER` で、どちらも正方形枠へ aspect fit する。

この配置では非正方形画像の短辺側に透明の余白ができる。枠に角丸をかけると弧は余白の中で完結するため、余白が radius より大きい画像には角丸が効かず、radius が余白より大きいと画像の角が斜めに欠ける。つまり「非正方形画像 + 角丸」はどの radius でもきれいな結果にならない。

この論点は、Android が解決済みの icon size / radius を描画へ適用していなかった欠落 (`EffectiveStyle.effectiveIconSize` / `effectiveIconRadius` に呼び出し元が無く、`CellBaseLayout` が 24dp 固定) と、iOS で画像の intrinsic size がサイズ制約に勝つ欠陥 (SF Symbols の字形ごとに icon 列の幅が変わる) を両 OS 同時に直すにあたり、角丸を「どの矩形に対してかけるか」を両 OS で同じ答えにする必要から生じた。

## Decision

icon は解決済み icon size の正方形枠に aspect fit で収め、icon radius は**その正方形枠に対して**適用する。画像の描画矩形 (aspect fit 後の実寸) には追従しない。

- 非正方形画像では角丸が効かない (または radius が大きいと角が欠ける) ことを契約として明記し、角丸を効かせたい icon は正方形で用意してもらう。
- iOS は現行の `layer.cornerRadius` + `clipsToBounds` による枠 clip を維持する。Android は `iconView` の `clipToOutline` + `ViewOutlineProvider` (roundRect) で枠に対して clip し、iOS と対称にする。
- icon radius の意味は「設定アプリ風の角丸バッジ」であり、正方形 icon を前提とする機能として位置づける。

## Alternatives Considered

- **aspect fit 後の描画矩形に角丸を適用する**: 却下。両 OS で描画矩形を自前計算して mask / outline を bind ごとに更新する必要があり、iOS の既存の見た目も (非正方形 + 角丸指定の利用者に限り) 変わる。「非正方形画像に角丸をつけたい」需要は確認できておらず、実装コストに見合わない。
- **aspect fill で正方形に切り取る**: 却下。角丸は常に効くようになるが、利用者の画像を勝手に切り取る情報損失を伴い、iOS の aspect fit を捨てる破壊的変更になる。

## Consequences

- 正: 両 OS の角丸の適用先が「枠」で一致し、同じ Theme 値で同じ見た目になる。Android は outline を枠に張るだけで済み、iOS は無変更。
- 正: 契約が単純 (枠 = 正方形、角丸 = 枠に対して) で、利用者向けドキュメントに一文で書ける。
- 負: 非正方形画像に角丸を期待する利用者には効かない。契約として明記し、正方形 icon の用意を求める運用コストが残る。
- 負: radius が短辺側の余白より大きい非正方形画像では角が斜めに欠ける見た目を許容する (ライブラリ側で clamp や警告は行わない)。

出典: kasane/changes/fix-cell-icon-size-parity/exploration.md (検討した選択肢 / 決定事項) / 探索の会話中の議論 (2026-08-22)
